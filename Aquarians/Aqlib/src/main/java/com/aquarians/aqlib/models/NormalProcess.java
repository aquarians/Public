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

package com.aquarians.aqlib.models;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Pair;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.math.PriceRecord;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.List;

public class NormalProcess {

    public static double OUTLIERS_PROBABILITY = 0.01;

    public final double growth;
    public final double vol;

    private final NormalDistribution distribution = new NormalDistribution();

    public NormalProcess(double growth, double vol) {
        this.growth = growth;
        this.vol = vol;
    }

    public double forwardMean(double spot, double time) {
        return spot * Math.exp(growth * time);
    }

    public double forwardDev(double spot, double time) {
        double var = Math.exp(2.0 * growth * time) * (Math.exp(vol * vol * time) - 1.0);
        return spot * Math.sqrt(var);
    }

    public NormalDistribution getDistribution(double yf) {
        double mean = (growth - vol * vol * 0.5) * yf;
        double dev = vol * Math.sqrt(yf);
        return new NormalDistribution(mean, dev);
    }

    private Double generateProcess(int count, double spot, double dt, List<Double> values) {
        double mean = (growth - vol * vol * 0.5) * dt;
        double dev = vol * Math.sqrt(dt);
        for (int i = 0; i <= count; i++) {
            if (null != values) {
                values.add(spot);
            }
            double z = distribution.sample();
            spot *= Math.exp(mean + dev * z);
            if (spot < Util.MINIMUM_PRICE) {
                return null;
            }
        }
        return spot;
    }

    public double generateForward(int count, double spot, double dt) {
        return generateProcess(count, spot, dt, null);
    }

    public List<Double> generatePath(int count, double spot, double dt) {
        List<Double> values = new ArrayList<>(count + 1);
        Double forward = generateProcess(count, spot, dt, values);
        if (null == forward) {
            return null;
        }
        return values;
    }

    public List<PriceRecord> generatePath(Day startDay, double spot, int maturity, int count) {
        return generatePath(startDay, startDay.ensureTradingDay().addTradingDays(count), spot, maturity);
    }

    public List<PriceRecord> generatePath(Day startDay, Day endDay, double spot, int maturity) {
        int size = startDay.countCalendarDays(endDay);
        List<PriceRecord> records = new ArrayList<>(size + 1);

        double dt = Util.yearFraction(maturity);
        double mean = (growth - vol * vol * 0.5) * dt;
        double dev = vol * Math.sqrt(dt);
        for (Day day = startDay.ensureTradingDay(); day.compareTo(endDay) <= 0; day = day.nextTradingDay()) {
            records.add(new PriceRecord(day, spot));

            double z = distribution.sample();
            spot *= Math.exp(mean + dev * z);

            // Don't let the price fall to zero
            spot = Math.max(spot, Util.MINIMUM_PRICE);
        }

        return records;
    }

    public List<Pair<Double, Double>> generateCorelatedPath(int count, double spot, double dt, NormalProcess process2, double spot2, double correlation) {
        List<Pair<Double, Double>> values = new ArrayList<>(count + 1);
        double icorr = Math.sqrt(1.0 - correlation * correlation);
        double mean = (growth - vol * vol * 0.5) * dt;
        double dev = vol * Math.sqrt(dt);
        double mean2 = (process2.growth - process2.vol * process2.vol * 0.5) * dt;
        double dev2 = process2.vol * Math.sqrt(dt);
        for (int i = 0; i <= count; i++) {
            if (null != values) {
                values.add(new Pair<>(spot, spot2));
            }
            double z = distribution.sample();
            spot *= Math.exp(mean + dev * z);
            double z2 = z * correlation + distribution.sample() * icorr;
            spot2 *= Math.exp(mean2 + dev2 * z2);
            if (spot < Util.MINIMUM_PRICE) {
                spot = Util.MINIMUM_PRICE;
            }
            if (spot2 < Util.MINIMUM_PRICE) {
                spot2 = Util.MINIMUM_PRICE;
            }
        }
        return values;
    }

    public static NormalProcess parseNormalProcess(List<Double> prices, double dt, boolean excludeOutliers) {

        DefaultProbabilityFitter fitter = DefaultProbabilityFitter.computeReturnsFromPrices(prices);
        fitter.setOutliersProbability(OUTLIERS_PROBABILITY);
        fitter.compute(excludeOutliers);

        double vol = fitter.getDev() / Math.sqrt(dt);
        double growth = vol * vol * 0.5 + fitter.getMean() / dt;
        return new NormalProcess(growth, vol);
    }
}
