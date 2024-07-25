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

package com.aquarians.backtester.pricing;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.math.PriceRecord;
import com.aquarians.aqlib.models.BlackScholes;
import com.aquarians.aqlib.models.PricingResult;
import com.aquarians.aqlib.models.VolatilitySurface;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.StockPriceRecord;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class NormalDistributionModel extends AbstractPricingModel {

    public static final PricingModel.Type TYPE = PricingModel.Type.Normal;

    private final PricingModule owner;
    private Double volatility;
    private List<StockPriceRecord> records;
    private double growthRate = 0.0;
    private int hedgeFrequency = Util.DEFAULT_HEDGE_FREQUENCY;

    public NormalDistributionModel() {
        this(null);
    }

    public NormalDistributionModel(PricingModule owner) {
        this.owner = owner;
    }

    public NormalDistributionModel(double volatility) {
        owner = null;
        this.volatility = volatility;
    }

    public Type getType() {
        return Type.Normal;
    }

    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
    }

    public List<StockPriceRecord> getRecords() {
        return records;
    }

    public void setVolatility(Double volatility) {
        this.volatility = volatility;
    }

    public Day getToday() {
        return today;
    }

    public Double getSpot() {
        return spot;
    }

    public void setRecords(List<StockPriceRecord> records) {
        this.records = records;
    }

    public void setHedgeFrequency(int hedgeFrequency) {
        this.hedgeFrequency = hedgeFrequency;
    }

    public Double impliedVolatility(Instrument instrument) {
        int days = today.countTradingDays(instrument.getMaturity());
        double yf = Util.yearFraction(days);
        BlackScholes pricer = new BlackScholes(instrument.isCall(), spot, instrument.getStrike(), yf, 0.0, 0.0, 0.0);
        Double price = instrument.getPrice();
        if (null == price) {
            return null;
        }

        return pricer.impliedVolatility(instrument.getPrice());
    }

    @Override
    public PricingResult price(Instrument instrument) {
        if (instrument.getType().equals(Instrument.Type.STOCK)) {
            return super.price(instrument);
        }

        if (!instrument.getType().equals(Instrument.Type.OPTION)) {
            throw new RuntimeException("Unknown instrument type: " + instrument.getType().name());
        }

        if (null == volatility) {
            return null;
        }

        int days = today.countTradingDays(instrument.getMaturity());
        double yf = Util.yearFraction(days);
        BlackScholes pricer = new BlackScholes(instrument.isCall(), spot, instrument.getStrike(), yf, 0.0, 0.0, volatility);

        if (days < 1) {
            PricingResult result = new PricingResult(pricer.valueAtExpiration(), 0.0);
            result.pnlDev = 0.0;
            result.day = today;
            return result;
        }

        PricingResult result = new PricingResult(pricer.price(), pricer.analyticDelta());
        int hedges = days;
        if (hedgeFrequency > 0) {
            hedges = Math.max(1, days / hedgeFrequency);
        }
        result.pnlDev = pricer.theoreticalPnlDev(hedges);
        result.day = today;
        return result;
    }

    @Override
    public void fit() {
        today = owner.getToday();
        spot = owner.getSpotPrice();

        // Load from the database if not provided externally
        List<StockPriceRecord> records = this.records;
        if (null == records) {
            Day from = today.addDays(-Util.CALENDAR_DAYS_IN_YEAR);
            DatabaseModule databaseModule = owner.getDatabaseModule();
            records = databaseModule.getProcedures().stockPricesSelect.execute(owner.getUnderlier().id, from, today);
        }

        Double prevPrice = null;
        DefaultProbabilityFitter fitter = new DefaultProbabilityFitter(records.size());
        for (StockPriceRecord record : records) {
            Double currPrice = record.close;
            if ((null == currPrice) || (currPrice < Util.ZERO)) {
                prevPrice = null;
                continue;
            }

            if (null != prevPrice) {
                double ret = Math.log(currPrice / prevPrice);
                fitter.addSample(ret);
            }

            prevPrice = currPrice;
        }

        // Check if we got enough samples
        if (fitter.size() < Util.TRADING_DAYS_IN_YEAR / 2) {
            volatility = null;
            return;
        }

        fitter.compute();
        volatility = fitter.getDev() * Math.sqrt(Util.TRADING_DAYS_IN_YEAR);
    }

    public Double getVolatility() {
        return volatility;
    }

    @Override
    public VolatilitySurface getSurface() {
        return null;
    }

    public NormalDistribution getDistribution(int maturity) {
        double yf = Util.yearFraction(maturity);
        return getDistribution(yf);
    }

        public NormalDistribution getDistribution(double yf) {
        double vol = getVolatility();
        double mean = (growthRate - vol * vol * 0.5) * yf;
        double dev = vol * Math.sqrt(yf);
        return new NormalDistribution(mean, dev);
    }

    public List<PriceRecord> generatePath(int days) {
        List<PriceRecord> prices = new ArrayList<PriceRecord>(days + 1);

        double spot = getSpot();
        Day now = getToday();
        NormalDistribution dist = getDistribution(1);

        for (int day = 0; day <= days; day++) {
            prices.add(new PriceRecord(now, Util.round(spot, 4)));

            double x = dist.sample();
            now = now.nextTradingDay();
            spot *= Math.exp(x);
            if (spot < 1.0) {
                // Too small prices don't go well with two digit rounding
                // And the jump in price will be filtered out anyways by the fitter
                spot = 100.0;
            }
        }

        return prices;
    }

}
