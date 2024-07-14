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

package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.*;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.StockPriceRecord;
import com.aquarians.backtester.database.records.UnderlierRecord;

import java.util.*;

public class ValidateUnderliersJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ValidateUnderliersJob.class.getSimpleName());

    private static final int MAX_JUMPS = 10;
    private static final int MAX_FREEZES = 10;
    private static final double MAX_DEVS = 3.0;
    private static final double CALENDAR_DAYS_IN_YEAR = 365.25;

    private final DatabaseModule owner;
    private final Day startDay;
    private final Day endDay;
    private List<UnderlierRecord> underliers = new ArrayList<>();
    private final int maxHolidaysPerYear;
    private Set<Day> holidays = new TreeSet<>();

    private enum SourceOfUnderliers {
        Database,
        File,
        List
    }

    public ValidateUnderliersJob(DatabaseModule owner) {
        this.owner = owner;
        Properties props = Application.getInstance().getProperties();
        startDay = parseDay(props.getProperty("ValidateUnderliersJob.StartDay"));
        endDay = parseDay(props.getProperty("ValidateUnderliersJob.EndDay"));
        maxHolidaysPerYear = Integer.parseInt(props.getProperty("ValidateUnderliersJob.MaxHolidaysPerYear", "15"));
    }

    private static Day parseDay(String text) {
        if (null == text) {
            return null;
        }

        return new Day(text);
    }

    private void loadUnderliers() {
        // Since here we're not doing multithreaded operations, it doesn't matter which database connection we use
        DatabaseModule databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));

        String text = Application.getInstance().getProperties().getProperty("ValidateUnderliersJob.Underliers", "Database");
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

        int remaining = underliers.size();
        for (UnderlierRecord underlier : underliers) {
            remaining--;
            logger.debug("Validating underlier " + underlier.code + ", remaining: " + remaining);
            try {
                if (validate(underlier)) {
                    logger.debug("Validated underlier: " + underlier.code);
                } else {
                    logger.debug("Failed underlier: " + underlier.code);
                }
            } catch (Exception ex) {
                logger.warn("Validating underlier " + underlier.code, ex);
            }
        }
    }

    boolean validate(UnderlierRecord underlier) {
        Day startDay = this.startDay;
        Day endDay = this.endDay;

        if ((null == startDay) || (null == endDay)) {
            Pair<Day, Day> limits =  owner.getProcedures().stockPricesSelectMinMaxDate.execute(underlier.id);
            if (null == startDay) {
                startDay = limits.getKey();
            }
            if (null == endDay) {
                endDay = limits.getValue();
            }

            if ((null == startDay) || (null == endDay)) {
                logger.warn("Underlier " + underlier.code + ": no data");
                return false;
            }
        }

        List<StockPriceRecord> records = owner.getProcedures().stockPricesSelect.execute(underlier.id, startDay, endDay);
        if (records.size() < Util.TRADING_DAYS_IN_YEAR) {
            logger.warn("Underlier " + underlier.code + ": not enough data");
            return false;
        }

        int consecutiveIdenticalPrices = 0;
        ArrayList<StockPriceRecord> jumps = new ArrayList<>(Util.TRADING_DAYS_IN_WEEK);

        for (int i = 0; i < records.size(); i++) {
            StockPriceRecord record = records.get(i);
            StockPriceRecord prevRecord = (i > 0) ? records.get(i - 1) : null;

            // Price is zero
            if (record.close < Util.ZERO) {
                logger.warn("Underlier " + underlier.code + ", day: " + record.day + ", invalid price: " + Util.format(record.close));
                return false;
            }

            if (null == prevRecord) {
                continue;
            }

            // Price doesn't move
            if (Math.abs(record.close - prevRecord.close) < Util.ZERO) {
                consecutiveIdenticalPrices++;
            } else {
                consecutiveIdenticalPrices = 0;
            }
            if (consecutiveIdenticalPrices > Util.TRADING_DAYS_IN_WEEK) {
                logger.warn("Underlier " + underlier.code + ", day: " + record.day + ", stale data");
                return false;
            }

            // Gaps in the data
            if (prevRecord.day.countTradingDays(record.day) > Util.TRADING_DAYS_IN_WEEK) {
                logger.warn("Underlier " + underlier.code + ", day: " + record.day + ", gaps in the data");
                return false;
            }

            // Jumps
            if (Util.ratio(record.close, prevRecord.close) > 1.99) {
                jumps.add(record);
                if (jumps.size() == Util.TRADING_DAYS_IN_WEEK) {
                    StockPriceRecord first = jumps.get(0);
                    StockPriceRecord last = jumps.get(jumps.size() - 1);
                    if (first.day.countTradingDays(last.day) < Util.TRADING_DAYS_IN_MONTH) {
                        logger.warn("Underlier " + underlier.code + ", day: " + record.day + ", jumps in the data");
                        return false;
                    }

                    jumps.remove(0);
                }
            }
        }

        return true;
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

    private void identifyHolidays() {
        // Get the weekdays (non weekends) over the configured period
        Set<Day> weekdays = new TreeSet<>();
        for (Day day = startDay; day.compareTo(endDay) <= 0; day = day.nextTradingDay()) {
            weekdays.add(day);
        }

        Map<Day, Ref<Integer>> aggregatedHolidays = new TreeMap<>();
        for (UnderlierRecord underlier : underliers) {
            // Some are true holidays, some are just missing data, we don't know for sure
            List<Day> holidays = loadHolidays(underlier, weekdays);

            // Count how many times a holiday appeared across all underliers
            for (Day holiday : holidays) {
                Ref<Integer> count = aggregatedHolidays.get(holiday);
                if (null == count) {
                    count = new Ref<>(0);
                    aggregatedHolidays.put(holiday, count);
                }
                count.value++;
            }
        }
    }

    private List<Day> loadHolidays(UnderlierRecord underlier, Set<Day> weekdays) {
        List<StockPriceRecord> records = owner.getProcedures().stockPricesSelect.execute(underlier.id, startDay, endDay);
        if (records.size() < 1) {
            return new ArrayList<>();
        }

        Set<Day> tradeDays = new TreeSet<>();
        for (StockPriceRecord record : records) {
            tradeDays.add(record.day);
        }

        int years = endDay.getYear() - startDay.getYear();
        List<Day> holidays = new ArrayList<>((years + 1) * 20);
        StockPriceRecord first = records.get(0);
        StockPriceRecord last = records.get(records.size() - 1);
        for (Day weekday : weekdays) {
            if (weekday.compareTo(first.day) < 0) {
                continue;
            }
            if (weekday.compareTo(last.day) > 0) {
                break;
            }

            if (!tradeDays.contains(weekday)) {
                holidays.add(weekday);
            }
        }

        return holidays;
    }

    private void searchSplits() {
        for (UnderlierRecord underlier : underliers) {
            Pair<Day, Day> limits =  owner.getProcedures().stockPricesSelectMinMaxDate.execute(underlier.id);
            Map<Day, Double> splits = owner.getProcedures().stockSplitsSelect.execute(underlier.id, limits.getKey(), limits.getValue());
            if (splits.size() > 0) {
                logger.debug("Splits on " + underlier.code);
            }
        }
    }
}
