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

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.OptionPair;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.Function;
import com.aquarians.aqlib.math.LinearIterator;
import com.aquarians.aqlib.math.PriceRecord;
import com.aquarians.aqlib.models.BlackScholes;
import com.aquarians.aqlib.models.NormalProcess;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.Procedures;

import java.util.*;

public class GenerateTestDataJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(GenerateTestDataJob.class.getSimpleName());

    private static final int STOCK_PRICE_ROUNDING = 2;
    private static final int OPTION_PRICE_ROUNDING = 4;
    private static final double OPTION_PRICE_TICK = 1.0 / Math.pow(10.0, OPTION_PRICE_ROUNDING);

    private final DatabaseModule owner;
    private final String underlierCode;
    private final double spotPrice;
    private final double growthRate;
    private final double volatility;
    private final VolFunction volFunction;
    private final double strikeDevs;
    private final int strikeCount;
    private final boolean weeklies;
    private final boolean monthlies;
    private final boolean quarterlies;
    private final Day startDay;
    private final Day optionsStartDay;
    private final Day endDay;

    private Long underlier;
    private List<PriceRecord> stockRecords;
    private TreeMap<Day, TermData> terms = new TreeMap<>();

    public GenerateTestDataJob(DatabaseModule owner) {
        this.owner = owner;
        Properties properties = Application.getInstance().getProperties();
        underlierCode = properties.getProperty("GenerateTestDataJob.UnderlierCode",  "TESTDATA");
        spotPrice = Double.parseDouble(properties.getProperty("GenerateTestDataJob.SpotPrice", "100.0"));
        growthRate = Double.parseDouble(properties.getProperty("GenerateTestDataJob.GrowthRate", "0.0"));
        volatility = Double.parseDouble(properties.getProperty("GenerateTestDataJob.Volatility", "0.25"));
        strikeDevs = Double.parseDouble(properties.getProperty("GenerateTestDataJob.StrikeDevs", "2.0"));
        volFunction = parseVolFunction(properties, strikeDevs);
        strikeCount = Integer.parseInt(properties.getProperty("GenerateTestDataJob.StrikeCount", "16"));
        weeklies = Boolean.parseBoolean(properties.getProperty("GenerateTestDataJob.Weeklies", "true"));
        monthlies = Boolean.parseBoolean(properties.getProperty("GenerateTestDataJob.Monthlies", "true"));
        quarterlies = Boolean.parseBoolean(properties.getProperty("GenerateTestDataJob.Quarterlies", "true"));
        startDay = Day.parseDay(properties.getProperty("GenerateTestDataJob.StartDay",  "2010-Jan-01"));
        if (null != properties.getProperty("GenerateTestDataJob.OptionsStartDay")) {
            optionsStartDay = Day.parseDay(properties.getProperty("GenerateTestDataJob.OptionsStartDay"));
        } else {
            optionsStartDay = startDay;
        }
        endDay = Day.parseDay(properties.getProperty("GenerateTestDataJob.EndDay",  "2020-Dec-31"));
    }

    private static VolFunction parseVolFunction(Properties properties, double strikeDevs) {
        String text = properties.getProperty("GenerateTestDataJob.VolSkew");
        if (null == text) {
            return null;
        }

        List<Double> ys = new ArrayList<>(3);
        String[] values = text.split(",");
        for (String value : values) {
            Double y = Double.parseDouble(value);
            ys.add(y);
        }

        if (ys.size() != 3) {
            return null;
        }

        // Deduce parabola of equation y = a * x^2 + b * x + c that passes through
        // (-strikeDevs, y[0]), (0, y[1]), (strikeDevs, y[2])

        // Mid is for x1 = 0, therefore c = y1
        double c = ys.get(1);

        double x = strikeDevs;
        double a = (ys.get(0) + ys.get(2) - 2.0 * c) / (2.0 * x * x);
        double b = (ys.get(2) - ys.get(0)) / (2.0 * x);
        return new VolFunction(a, b, c);
    }

    private static final class TermData {
        TreeMap<Double, OptionPair> strikes = new TreeMap<>();

        public Double getClosestStrike(double target) {
            Double result = null;
            double minDistance = 0.0;

            for (Double strike : strikes.keySet()) {
                double distance = Math.abs(strike - target);
                if ((null == result) || (distance < minDistance)) {
                    result = strike;
                    minDistance = distance;
                }
            }

            return result;
        }
    }

    private double computeVol(double spot, double strike, double yf) {
        if (null == volFunction) {
            return volatility;
        }

        double mean = (growthRate - volatility * volatility * 0.5) * yf;
        double dev = volatility * Math.sqrt(yf);
        double x = Math.log(strike / spot);
        double z = (x - mean) / dev;
        double vol = volFunction.value(z);
        return BlackScholes.limitVol(vol);
    }

    @Override
    public void run() {
        // Get the ID of the underlier
        underlier = owner.getProcedures().underlierSelect.execute(underlierCode);
        if (null == underlier) {
            // Create underlier
            logger.debug("Creating new underlier with code: " + underlierCode);
            underlier = owner.getProcedures().sequenceNextVal.execute(Procedures.SQ_UNDERLIERS);
            owner.getProcedures().underlierInsert.execute(underlier, underlierCode);
        }

        // Clear it's data
        logger.debug("Clearing old data...");
        owner.getProcedures().optionPricesDeleteAll.execute(underlier);
        owner.getProcedures().stockPricesDeleteAll.execute(underlier);
        logger.debug("Old data cleared");

        // Generate stock prices
        generateStockPrices();

        // Generate for each day
        for (PriceRecord stockRecord : stockRecords) {
            generate(stockRecord);
        }
    }

    private void generateStockPrices() {
        // Generate theoretical values
        NormalProcess process = new NormalProcess(growthRate, volatility);
        List<PriceRecord> path = process.generatePath(startDay, spotPrice, startDay.countTradingDays(endDay));

        // Round to market prices
        stockRecords = new ArrayList<>(path.size());
        for (PriceRecord record : path) {
            PriceRecord stockRecord = new PriceRecord(record.day, Util.round(record.price, STOCK_PRICE_ROUNDING));
            stockRecords.add(stockRecord);
        }
    }

    private void generate(PriceRecord stockRecord) {
        // Stock
        logger.debug("Day: " + stockRecord.day + " stock price: " + Application.DOUBLE_DIGIT_FORMAT.format(stockRecord.price));
        owner.getProcedures().stockPriceInsert.execute(
                underlier,
                stockRecord.day,
                stockRecord.price, // open
                stockRecord.price, // high
                stockRecord.price, // low
                stockRecord.price, // close
                stockRecord.price, // adjusted
                1L // volume
        );

        // Options
        if (stockRecord.day.compareTo(optionsStartDay) < 0) {
            return;
        }

        generateOptions(stockRecord);
        priceAndSaveOptions(stockRecord);
        deleteExpiredMaturities(stockRecord);
    }

    // Generate option maturities and strikes
    private void generateOptions(PriceRecord stockRecord) {
        Set<Day> maturities = generateMaturities(stockRecord);
        for (Day maturity : maturities) {
            generateStrikes(stockRecord, maturity);
        }
    }

    // Remove expired maturities
    private void deleteExpiredMaturities(PriceRecord stockRecord) {
        List<Day> expired = new ArrayList<>();
        for (Day maturity : terms.keySet()) {
            if (stockRecord.day.compareTo(maturity) >= 0) {
                expired.add(maturity);
            }
        }
        for (Day maturity : expired) {
            terms.remove(maturity);
        }
    }

    private static boolean isLastDayOfTheMonth(Day day) {
        return (day.nextTradingDay().getMonth() != day.getMonth());
    }

    private static boolean isQuarterlyExpiration(Day day) {
        if (!isLastDayOfTheMonth(day)) {
            return false;
        }

        int month = day.getCalendarMonth();
        return  (
                (Calendar.MARCH == month) ||
                (Calendar.JUNE == month) ||
                (Calendar.SEPTEMBER == month) ||
                (Calendar.DECEMBER == month)
        );
    }

    private Set<Day> generateMaturities(PriceRecord stockRecord) {
        Set<Day> maturities = new TreeSet<>();

        if (weeklies) {
            // Fridays every week
            Day day = stockRecord.day;
            if (day.getDayOfWeek() == Calendar.FRIDAY) {
                maturities.add(day);
            }

            day = day.nextTradingDay();
            while (day.getDayOfWeek() != Calendar.FRIDAY) {
                day = day.nextTradingDay();
            }
            maturities.add(day);
        }

        if (monthlies) {
            // Last trading day of each month
            Day day = stockRecord.day;
            if (isLastDayOfTheMonth(day)) {
                maturities.add(day);
            }

            day = day.nextTradingDay();
            while (!isLastDayOfTheMonth(day)) {
                day = day.nextTradingDay();
            }
            maturities.add(day);
        }

        if (quarterlies) {
            // Last trading day of every 3rd month
            Day day = stockRecord.day;
            if (isQuarterlyExpiration(day)) {
                maturities.add(day);
            }

            day = day.nextTradingDay();
            while (!isQuarterlyExpiration(day)) {
                day = day.nextTradingDay();
            }
            maturities.add(day);
        }

        return maturities;
    }

    private void generateStrikes(PriceRecord stockRecord, Day maturity) {
        TermData term = terms.get(maturity);
        if (null == term) {
            term = new TermData();
            terms.put(maturity, term);
        }

        // Generate strikes around current spot
        int remainingDays = Util.maturity(stockRecord.day, maturity);
        if (remainingDays < 1) {
            return;
        }

        double yf = Util.yearFraction(remainingDays);
        double mean = (growthRate - volatility * volatility * 0.5) * yf;
        double dev = volatility * Math.sqrt(yf);
        LinearIterator itDevs = new LinearIterator(-strikeDevs, strikeDevs, strikeCount);
        while (itDevs.hasNext()) {
            double z = itDevs.next();
            double x = mean + dev * z;
            double strike = stockRecord.price * Math.exp(x);
            strike = Util.round(strike, STOCK_PRICE_ROUNDING);

            // Find closest strike already in market data
            Double closestStrike = term.getClosestStrike(strike);
            if (closestStrike != null) {
                // Convert to standard devs
                double closestX = Math.log(closestStrike / stockRecord.price);
                double closestZ = (closestX - mean) / dev;

                // Ignore current strike if closest already existing strike is closer than the strike increment
                double dz = Math.abs(z - closestZ);
                double step = itDevs.getStep() * 0.75;
                if (dz < step) {
                    continue;
                }
            }

            // Generate strike
            OptionPair pair = new OptionPair(strike);
            term.strikes.put(strike, pair);

            String code = maturity.toString() + "_" + Application.DOUBLE_DIGIT_FORMAT.format(strike);
            pair.call = new Instrument(Instrument.Type.OPTION, "C_" + code, true, maturity, strike);
            pair.put = new Instrument(Instrument.Type.OPTION, "P_" + code, false, maturity, strike);
        }
    }

    private static final class VolFunction implements Function {
        final double a;
        final double b;
        final double c;

        private VolFunction(double a, double b, double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public double value(double x) {
            return a * x * x + b * x + c;
        }
    }

    private void priceAndSaveOptions(PriceRecord stockRecord) {
        for (Map.Entry<Day, TermData> termEntry : terms.entrySet()) {
            Day maturity = termEntry.getKey();
            int remainingDays = Util.maturity(stockRecord.day, maturity);
            double yf = Util.yearFraction(remainingDays);

            TermData termData = termEntry.getValue();
            for (OptionPair pair : termData.strikes.values()) {
                double volatility = computeVol(stockRecord.price, pair.strike, yf);

                BlackScholes callPricer = new BlackScholes(true, stockRecord.price, pair.strike, yf, 0.0, 0.0, volatility);
                double callPrice = callPricer.price();
                callPrice = Util.round(callPrice, OPTION_PRICE_ROUNDING);

                Double callBid = callPrice - OPTION_PRICE_TICK;
                if (callBid < OPTION_PRICE_TICK) {
                    callBid = null;
                }
                double callAsk = callPrice + OPTION_PRICE_TICK;

                owner.getProcedures().optionPriceInsert.execute(underlier, pair.call.getCode(), stockRecord.day, true, pair.strike, maturity, callBid, callAsk);
                logger.debug("Day: " + stockRecord.day + " Call: " + pair.call.getCode() + " Price: " + Application.FOUR_DIGIT_FORMAT.format(callPrice));

                BlackScholes putPricer = new BlackScholes(false, stockRecord.price, pair.strike, yf, 0.0, 0.0, volatility);
                double putPrice = putPricer.price();
                putPrice = Util.round(putPrice, OPTION_PRICE_ROUNDING);

                Double putBid = putPrice - OPTION_PRICE_TICK;
                if (putBid < OPTION_PRICE_TICK) {
                    putBid = null;
                }
                double putAsk = putPrice + OPTION_PRICE_TICK;

                owner.getProcedures().optionPriceInsert.execute(underlier, pair.put.getCode(), stockRecord.day, false, pair.strike, maturity, putBid, putAsk);
                logger.debug("Day: " + stockRecord.day + " Put: " + pair.call.getCode() + " Price: " + Application.FOUR_DIGIT_FORMAT.format(putPrice));
            }
        }
    }
}
