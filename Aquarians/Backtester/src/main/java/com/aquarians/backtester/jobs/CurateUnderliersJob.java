/*
    MIT License

    Copyright (c) 2020 Mihai Bunea

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

package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.CsvFileReader;
import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.StockPriceRecord;
import com.aquarians.backtester.database.records.UnderlierRecord;

import java.util.ArrayList;
import java.util.List;

// The code is horribly inneficient but it's only run once
    public class CurateUnderliersJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(CurateUnderliersJob.class.getSimpleName());

    private static final int MAX_JUMPS = 10;
    private static final int MAX_FREEZES = 10;
    private static final double MAX_DEVS = 3.0;

    private final DatabaseModule owner;
    private final Day startDay;
    private final Day endDay;
    private List<UnderlierRecord> underliers = new ArrayList<>();

    private enum SourceOfUnderliers {
        Database,
        File,
        List
    }

    public CurateUnderliersJob(DatabaseModule owner) {
        this.owner = owner;
        startDay = new Day(Application.getInstance().getProperties().getProperty("CurateUnderliersJob.StartDay"));
        endDay = new Day(Application.getInstance().getProperties().getProperty("CurateUnderliersJob.EndDay"));
    }

    private void loadUnderliers() {
        // Since here we're not doing multithreaded operations, it doesn't matter which database connection we use
        DatabaseModule databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));

        String text = Application.getInstance().getProperties().getProperty("CurateUnderliersJob.Underliers", "Database");
        String[] components = text.split(",");
        String sourceText = components[0];
        SourceOfUnderliers source = SourceOfUnderliers.valueOf(sourceText);
        switch (source) {
            case Database:
                loadDatabaseUnderliers(databaseModule);
                break;
            case File:
                loadFileUnderliers(databaseModule, components[1]);
                break;
            case List:
                loadListUnderliers(databaseModule, components);
                break;
            default:
                throw new RuntimeException("Unknown source of underliers: " + sourceText);
        }

        logger.info("Loaded " + underliers.size() + " underliers");
    }

    private void loadDatabaseUnderliers(DatabaseModule databaseModule) {
        underliers = databaseModule.getProcedures().underliersSelectAll.execute();
    }

    private void loadFileUnderliers(DatabaseModule databaseModule, String filename) {
        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(filename);
            String code = null;
            while (null != (code = reader.readLine())) {
                Long id = databaseModule.getProcedures().underlierSelect.execute(code);
                if (null != id) {
                    underliers.add(new UnderlierRecord(id, code));
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void loadListUnderliers(DatabaseModule databaseModule, String[] codes) {
        for (int i = 1; i < codes.length; i++) {
            String code = codes[i];
            Long id = databaseModule.getProcedures().underlierSelect.execute(code);
            if (null != id) {
                underliers.add(new UnderlierRecord(id, code));
            }
        }
    }
    @Override
    public void run() {
        loadUnderliers();

        // Select underliers that have data through all test period (weren't delisted)
        ensureDataThroughAllPeriod();

        // Reject underliers that have too many jumps or frozen data
        rejectJumpsAndFreezes();

        // Compare stock close with the value implied from option prices
        ensureCloseAndImpliedAgreement();

        for (UnderlierRecord underlier : underliers) {
            logger.info("CURATED: " + underlier.code);
        }
    }

    private void ensureCloseAndImpliedAgreement() {
        List<UnderlierRecord> selected = new ArrayList<>(underliers.size());
        for (UnderlierRecord underlier : underliers) {
            try {
                if (!hasCloseAndImpliedAgreement(underlier)) {
                    logger.info("REJECTED: close and implied disagree: " + underlier.code);
                    continue;
                }

                selected.add(underlier);
            } catch (Exception ex) {
                logger.warn(underlier.code, ex);
            }
        }

        underliers = selected;
    }

    private boolean hasCloseAndImpliedAgreement(UnderlierRecord underlier) {
        List<StockPriceRecord> records = owner.getProcedures().stockPricesSelect.execute(underlier.id, startDay, endDay);

        DefaultProbabilityFitter fitter = new DefaultProbabilityFitter(records.size());
        StockPriceRecord prev = null;
        for (StockPriceRecord curr : records) {
            if (curr.close < Util.ZERO) {
                prev = null;
                continue;
            }

            if (null != prev) {
                double ret = Math.log(curr.close / prev.close);
                fitter.addSample(ret);
            }

            prev = curr;
        }
        fitter.compute(true);

        for (StockPriceRecord record : records) {
            if ((null == record.implied) || (record.close < Util.ZERO)) {
                continue;
            }

            double ret = Math.log(record.implied / record.close);
            double std = (ret - fitter.getMean() / fitter.getDev());
            if (Math.abs(std) > MAX_DEVS) {
                return false;
            }
        }

        return true;
    }

    private void rejectJumpsAndFreezes() {
        List<UnderlierRecord> selected = new ArrayList<>(underliers.size());
        for (UnderlierRecord underlier : underliers) {
            try {
                if (!hasJumpsAndFreezes(underlier)) {
                    logger.info("REJECTED: Has too many jumps and freezes: " + underlier.code);
                    continue;
                }

                selected.add(underlier);
            } catch (Exception ex) {
                logger.warn(underlier.code, ex);
            }
        }

        underliers = selected;
    }

    private boolean hasJumpsAndFreezes(UnderlierRecord underlier) {
        List<StockPriceRecord> records = owner.getProcedures().stockPricesSelect.execute(underlier.id, startDay, endDay);
        for (int i = Util.TRADING_DAYS_IN_YEAR; i < records.size(); i += Util.TRADING_DAYS_IN_MONTH) {
            List<StockPriceRecord> window = records.subList(i - Util.TRADING_DAYS_IN_YEAR, i);

            // how many times price doubles of halves
            int jumps = 0;
            // how many times price doesn't move at all
            int freezes = 0;
            StockPriceRecord prev = null;
            for (StockPriceRecord curr : window) {
                if (curr.close < Util.ZERO) {
                    freezes++;
                    prev = null;
                }

                if (prev != null) {
                    // price halves or doubles
                    if ((curr.close < prev.close * 0.5) || (curr.close > prev.close * 2.0)) {
                        jumps++;
                    }

                    // price doesn't move at all
                    if (Math.abs(curr.close - prev.close) < Util.ZERO) {
                        freezes++;
                    }
                }

                prev = curr;
            }

            if ((jumps > MAX_JUMPS) || (freezes > MAX_FREEZES)) {
                return false;
            }
        }

        return true;
    }

    private void ensureDataThroughAllPeriod() {
        List<UnderlierRecord> selected = new ArrayList<>(underliers.size());
        for (UnderlierRecord underlier : underliers) {
            try {
                if (!hasDataThroughAllPeriod(underlier)) {
                    logger.info("REJECTED: Has no data though all period: " + underlier.code);
                    continue;
                }

                selected.add(underlier);
            } catch (Exception ex) {
                logger.warn(underlier.code, ex);
            }
        }

        underliers = selected;
    }

    private boolean hasDataThroughAllPeriod(UnderlierRecord underlier) {
        // Must have data at start
        List<StockPriceRecord> startRecords = owner.getProcedures().stockPricesSelect.execute(underlier.id, startDay, startDay.addTradingDays(Util.TRADING_DAYS_IN_WEEK));
        if (startRecords.size() < 1) {
            return false;
        }

        // Must have data at end
        List<StockPriceRecord> endRecords = owner.getProcedures().stockPricesSelect.execute(underlier.id, endDay.addTradingDays(-Util.TRADING_DAYS_IN_WEEK), endDay);
        if (endRecords.size() < 1) {
            return false;
        }

        // Must have data in the middle
        List<StockPriceRecord> records = owner.getProcedures().stockPricesSelect.execute(underlier.id, startDay, endDay);
        int days = startDay.countTradingDays(endDay);
        if (records.size() < days / 2) {
            return false;
        }

        return true;
    }
}
