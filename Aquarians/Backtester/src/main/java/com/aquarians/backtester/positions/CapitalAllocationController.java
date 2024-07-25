/*
    MIT License

    Copyright (c) 2024 Mihai Bunea

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/

package com.aquarians.backtester.positions;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Util;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.procedures.NavGetDay;
import com.aquarians.backtester.database.records.NavRecord;
import com.aquarians.backtester.database.records.UnderlierRecord;

import java.util.*;

// Coordinates allocation of capital across multiple underliers (possibly processed on multiple threads)
public class CapitalAllocationController {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(CapitalAllocationController.class.getSimpleName());

    private final Object lock = new Object();

    private final String strategyType;

    // The amount of capital to start with
    private final double startingCapital;

    // Given an amount of capital, over how many trades to split it
    // Group size = 0 signifies independent strategy on a single underlier, don't care about the others,
    // So initial capital = $1,000,000 and one trades 100 underliers, each takes $1,000,000 (so there's 100x$1MM money involvec)
    //
    // Group size >= 1 splits the position capital available for a sequential time frame into one or more trades.
    // Ex: group size = 2m, initial capital = $1,000,000 position size = 50%, first time frame gets $500,000 allocated
    // group size = 2 means the $500,000 is split among a maximum of two trades, each taking $250,000
    private final int groupSize;

    private final DatabaseModule databaseModule;

    private Map<Long, NavRecord> navRecordsMap = new HashMap<>();
    private double availableCapital = 0.0;
    private int availableAllocations = 0;

    // Custom data
    private String data;

    public CapitalAllocationController(String strategyType) {
        this.strategyType = strategyType;

        Properties properties = Application.getInstance().getProperties();
        startingCapital = Double.parseDouble(properties.getProperty("Strategy." + strategyType + ".StartingCapital", "100.0"));
        groupSize = Integer.parseInt(properties.getProperty("Strategy." + strategyType + ".GroupSize", "0"));

        logger.debug("CapitalAllocation strategyType=" + strategyType +
                " startingCapital=" + startingCapital + " groupSize=" + groupSize);

        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));
    }

    public void load(Day today) {
        // Load NAV of previous trading day
        Day yesterday = today.previousTradingDay();
        List<NavRecord> navRecordsList = databaseModule.getProcedures().navGetDay.execute(strategyType, yesterday);
        if (navRecordsList.size() == 0) {
            // Try earlier
            Day lastDay = databaseModule.getProcedures().navSelectLastDay.execute(strategyType);
            if (lastDay != null) {
                navRecordsList = databaseModule.getProcedures().navGetDay.execute(strategyType, lastDay);
            }
        }

        // Process underlier allocations (if any)
        navRecordsMap.clear();
        availableAllocations = groupSize;
        availableCapital = startingCapital;
        for (NavRecord record : navRecordsList) {
            if (null == record.underlier) {
                // Capital that's not assigned to a specific underlier is available for the group
                availableCapital = record.available;
                continue;
            }

            navRecordsMap.put(record.underlier, record);

            // In case of group trading
            if (groupSize > 0) {
                // An amount of capital is allocated to this underlier, so this leaves one less available allocation for the group
                availableAllocations--;
                // Safety check
                if (availableAllocations < 0) {
                    throw new RuntimeException("Capital allocations cannot be negative");
                }
            }
        }
    }

    public void save(Day today) {
        // Set the per-underlier allocation
        double allocated = 0.0;
        for (Map.Entry<Long, NavRecord> entry : navRecordsMap.entrySet()) {
            NavRecord record = entry.getValue();
            allocated += record.allocated;
            try {
                databaseModule.getProcedures().navInsert.execute(today, strategyType, record.underlier, record.available, record.allocated);
            } catch (Exception ex) {
                logger.warn("Day: " + today + " Underlier: " + entry.getKey(), ex);
            }
        }

        // Set the group overall
        if (groupSize > 0) {
            databaseModule.getProcedures().navInsert.execute(today, strategyType, null, availableCapital, allocated);
        }
    }

    // The "has" function checks if capital is available without actually requesting it
    // See the "get" function for actually requesting capital allocation
    public boolean hasTradingCapital(Long underlier) {
        double amount = internalGetTradingCapital(underlier, false);
        return (amount > 0.0);
    }

    // The "get" function requests the capital and if successful, capital is allocated and can't be used by other trades
    // See the "has" function for querying capital availability without actually requesting it
    public double getTradingCapital(Long underlier) {
        return internalGetTradingCapital(underlier, true);
    }

    // If AllocationRequest flag is false, will only check if capital is available (and return the amount) without actually allocating it
    private double internalGetTradingCapital(Long underlier, boolean isAllocationRequest) {
        // If group size is zero, underliers are independent and each trades their own capital
        if (0 == groupSize) {
            return getIndependentTradingCapital(underlier, isAllocationRequest);
        }

        return getGroupTradingCapital(underlier, isAllocationRequest);
    }

    private double getIndependentTradingCapital(Long underlier, boolean isAllocationRequest) {
        synchronized (lock) {
            NavRecord navRecord =  navRecordsMap.get(underlier);
            if (null == navRecord) {
                // No trades have been placed yet, we have the starting capital amount
                navRecord = new NavRecord(null, underlier, startingCapital, 0.0);
                navRecordsMap.put(underlier, navRecord);
            }

            // If capital has already been allocated to a trade, there's no more left
            // until the respective trades expires and capital gets freed (with profit or loss)
            double available = navRecord.available;
            if (0.0 == available) {
                return 0.0;
            }

            // Allocate the whole capital
            if (isAllocationRequest) {
                navRecord.allocated = available;
                navRecord.available = 0.0;
            }

            return available;
        }
    }

    private double getGroupTradingCapital(Long underlier, boolean isAllocationRequest) {
        synchronized (lock) {
            // Check if capital was already allocated to this underlier
            if (navRecordsMap.containsKey(underlier)) {
                // Deny further allocation until capital is released
                return 0.0;
            }

            if (availableAllocations < 1) {
                return 0.0;
            }

            // Amount per trade
            double allocationAmount = availableCapital / availableAllocations;
            if (!isAllocationRequest) {
                return allocationAmount;
            }

            availableCapital -= allocationAmount;
            availableAllocations--;
            // Safety check
            if (availableAllocations < 0) {
                throw new RuntimeException("Capital allocations cannot get negative");
            }

            // Allocate to the underlier
            navRecordsMap.put(underlier, new NavRecord(null, underlier, 0.0, allocationAmount));
            return allocationAmount;
        }
    }

    public void freeCapital(Long underlier, Double amount) {
        if (0 == groupSize) {
            freeIndependentCapital(underlier, amount);
            return;
        }

        freeGroupCapital(underlier, amount);
    }

    private void freeIndependentCapital(Long underlier, Double amount) {
        synchronized (lock) {
            NavRecord navRecord =  navRecordsMap.get(underlier);
            if ((null == navRecord) || (navRecord.available > 0.0)) {
                // Should not happen
                throw new RuntimeException("Capital not allocated for underlier: " + underlier);
            }

            navRecord.available = (null != amount) ? amount : navRecord.allocated;
            navRecord.allocated = 0.0;
        }
    }

    private void freeGroupCapital(Long underlier, Double amount) {
        synchronized (lock) {
            NavRecord navRecord =  navRecordsMap.get(underlier);
            if (null == navRecord) {
                // Should not happen
                throw new RuntimeException("Capital not allocated for underlier: " + underlier);
            }

            navRecordsMap.remove(underlier);
            availableCapital += (null != amount) ? amount : navRecord.allocated;
            availableAllocations++;
            // Safety check
            if (availableAllocations > groupSize) {
                throw new RuntimeException("Capital allocations cannot exceed group size");
            }
        }
    }

    // New net-asset-value after mark-to-market
    public void updateCapital(Long underlier, Double amount) {
        synchronized (lock) {
            NavRecord navRecord =  navRecordsMap.get(underlier);
            if ((null == navRecord) || (navRecord.available > 0.0)) {
                // Should not happen
                throw new RuntimeException("Capital not allocated for underlier: " + underlier);
            }

            navRecord.allocated = amount;
        }
    }

    public String getTagValue(String tag) {
        Map<String, String> tags = Util.loadTags(data);
        return tags.get(tag);
    }

    public void setTagValue(String tag, String value) {
        Map<String, String> tags = Util.loadTags(data);
        if (value != null) {
            tags.put(tag, value);
        } else {
            tags.remove(tag);
        }
        data = Util.saveTags(tags);
    }
}
