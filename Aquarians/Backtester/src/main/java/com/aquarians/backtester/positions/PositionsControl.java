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

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.aqlib.Day;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.marketdata.MarketDataControl;
import com.aquarians.backtester.marketdata.historical.MarketEventListener;

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Singleton instance for positions.
 * There can be several PositionsModule instances, on different threads.
 * Some operations require coordination among all threads, so they are placed here.
 */
public class PositionsControl implements MarketEventListener, ApplicationModule {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(PositionsControl.class);

    public static final String NAME = "PositionsControl";

    private Object lock = new Object();
    private boolean initialized = false;
    private Map<String, CapitalAllocationController> capitalAllocationControllers = new TreeMap<>();
    private final boolean clearDatabaseOnBatchStart;
    private final DatabaseModule databaseModule;
    private final boolean autoTrade;
    private final MarketDataControl dataControl;

    public PositionsControl() {
        // Create controllers for strategies
        Properties properties = Application.getInstance().getProperties();
        String text = properties.getProperty("Positions.StrategyBuilders", "");
        String[] types = text.split(",");
        for (String type : types) {
            try {
                CapitalAllocationController controller = new CapitalAllocationController(type);
                capitalAllocationControllers.put(type, controller);
            } catch (Exception ex) {
                logger.warn("Creating controller for " + type, ex);
            }
        }

        clearDatabaseOnBatchStart = Boolean.parseBoolean(properties.getProperty("Positions.ClearDatabaseOnBatchStart", "false"));
        logger.info("ClearDatabaseOnBatchStart: " + clearDatabaseOnBatchStart);

        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME));
        autoTrade = Boolean.parseBoolean(Application.getInstance().getProperties().getProperty("Positions.AutoTrade", "false"));
        dataControl = (MarketDataControl) Application.getInstance().getModule(Application.buildModuleName(MarketDataControl.NAME));
    }

    public void init() {
        synchronized (lock) {
            if (initialized) {
                return;
            }

            initialized = true;
        }

        dataControl.addMarketEventListener(this);
    }

    public void cleanup() {
        synchronized (lock) {
            if (!initialized) {
                return;
            }

            initialized = false;
        }

        dataControl.removeMarketEventListener(this);
    }

    @Override
    public String getName() {
        return Application.buildModuleName(NAME);
    }

    @Override
    public void processMarketEvent(MarketEvent event, Day day) {
        if (!autoTrade) {
            return;
        }

        if (MarketEvent.StartOfBatch == event) {
            if (clearDatabaseOnBatchStart) {
                clearDatabase();
            }
        } else if (MarketEvent.StartOfDay == event) {
            loadCapitalAllocation(day);
        } else if (MarketEvent.EndOfDay == event) {
            saveCapitalAllocation(day);
        }
    }

    void loadCapitalAllocation(Day day) {
        for (Map.Entry<String, CapitalAllocationController> entry : capitalAllocationControllers.entrySet()) {
            CapitalAllocationController controller = entry.getValue();
            try {
                controller.load(day);
            } catch (Exception ex) {
                logger.warn("Day: " + day + " controller: " + entry.getKey(), ex);
            }
        }
    }

    void saveCapitalAllocation(Day day) {
        for (Map.Entry<String, CapitalAllocationController> entry : capitalAllocationControllers.entrySet()) {
            CapitalAllocationController controller = entry.getValue();
            try {
                controller.save(day);
            } catch (Exception ex) {
                logger.warn("Day: " + day + " controller: " + entry.getKey(), ex);
            }
        }
    }

    public CapitalAllocationController getCapitalAllocationController(String strategyType) {
        return capitalAllocationControllers.get(strategyType);
    }

    private void clearDatabase() {
        if (!autoTrade) {
            return;
        }

        databaseModule.getProcedures().navDelete.execute();
        databaseModule.getProcedures().mtmDelete.execute();
        databaseModule.getProcedures().tradesDelete.execute();
        databaseModule.getProcedures().strategiesDelete.execute();
        databaseModule.getProcedures().statisticsDelete.execute();
    }
}
