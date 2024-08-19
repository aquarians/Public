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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

public class VolatilitySurface implements Volatility {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(VolatilitySurface.class);

    private static final double FIT_VOL_MAX_DEVS = 1.5;

    private final TreeMap<Integer, StrikeVols> maturities = new TreeMap<>();
    private Double spot;
    private Double error;

    public VolatilitySurface() {}

    public TreeMap<Integer, StrikeVols> getMaturities() {
        return maturities;
    }

    public Integer getFirstMaturity() {
        if (maturities.size() == 0) {
            return null;
        }

        return maturities.firstKey();
    }

    public void add(int maturity, double strike, double vol) {
        StrikeVols strikeVols = maturities.get(maturity);
        if (null == strikeVols) {
            strikeVols = new StrikeVols();
            maturities.put(maturity, strikeVols);
        }

        strikeVols.put(strike, vol);
    }

    @Override
    public Double getSpot() {
        return spot;
    }

    public void setSpot(Double spot) {
        this.spot = spot;
    }

    // Find highest maturity <= given maturity
    public Map.Entry<Integer, StrikeVols> getLowerBound(int maturity) {
        Map.Entry<Integer, StrikeVols> lowerEntry = null;

        for (Map.Entry<Integer, StrikeVols> entry : maturities.entrySet()) {
            if (entry.getKey() > maturity) {
                break;
            }

            lowerEntry = entry;
        }

        return lowerEntry;
    }

    // Find highest maturity <= given maturity
    public Map.Entry<Integer, StrikeVols> getUpperBound(int maturity) {
        Map.Entry<Integer, StrikeVols> upperEntry = null;

        for (Map.Entry<Integer, StrikeVols> entry : maturities.descendingMap().entrySet()) {
            if (entry.getKey() < maturity) {
                break;
            }

            upperEntry = entry;
        }

        return upperEntry;
    }

    // Strike needs to be expressed in standard deviations of volatility, see conversion function toDevStr()
    @Override
    public Double getVolatility(int maturity, double strike) {
        if (maturities.isEmpty() || (maturity < 1)) {
            return null;
        }

        // Try exact maturity
        StrikeVols strikeVols = maturities.get(maturity);
        if (null != strikeVols) {

            // Try exact strike
            Double vol = strikeVols.get(strike);
            if (null != vol) {
                return vol;
            }

            // Interpolate strike
            return strikeVols.interpolate(strike);
        }

        // Interpolate variance
        Map.Entry<Integer, StrikeVols> lowerEntry = getLowerBound(maturity);
        if (null == lowerEntry) {
            return maturities.firstEntry().getValue().interpolate(strike);
        }

        Map.Entry<Integer, StrikeVols> upperEntry = getUpperBound(maturity);
        if (null == upperEntry) {
            return maturities.lastEntry().getValue().interpolate(strike);
        }

        int lowerMaturity = lowerEntry.getKey();
        int upperMaturity = upperEntry.getKey();

        Double lowerVol = lowerEntry.getValue().interpolate(strike);
        Double upperVol = upperEntry.getValue().interpolate(strike);
        if ((null == lowerVol) || (null == upperVol)) {
            return null;
        }

        double lowerVar = lowerVol * lowerVol * Util.yearFraction(lowerMaturity);
        double upperVar = upperVol * upperVol * Util.yearFraction(upperMaturity);
        double var = Util.interpolate(lowerMaturity, lowerVar, upperMaturity, upperVar, maturity);
        double vol = Math.sqrt(var / Util.yearFraction(maturity));
        return vol;
    }

    public static final class StrikeVols extends TreeMap<Double, Double> {

        public Double forward;
        public Double interest;

        public StrikeVols() {}

        public StrikeVols(StrikeVols copy) {
            super(copy);
        }

        // Find highest strike <= given strike
        public Map.Entry<Double, Double> getLowerBound(double strike) {
            Map.Entry<Double, Double> lowerEntry = null;

            for (Map.Entry<Double, Double> entry : entrySet()) {
                if (entry.getKey() > strike) {
                    break;
                }

                lowerEntry = entry;
            }

            return lowerEntry;
        }

        // Find lowest strike >= given strike
        public Map.Entry<Double, Double> getUpperBound(double strike) {
            Map.Entry<Double, Double> upperEntry = null;

            for (Map.Entry<Double, Double> entry : descendingMap().entrySet()) {
                if (entry.getKey() < strike) {
                    break;
                }

                upperEntry = entry;
            }

            return upperEntry;
        }

        public Double interpolate(Double strike) {
            if (size() == 0) {
                return null;
            }

            // Try exact value
            Double vol = get(strike);
            if (null != vol) {
                return vol;
            }

            Map.Entry<Double, Double> lowerEntry = getLowerBound(strike);
            if (null == lowerEntry) {
                return firstEntry().getValue();
            }

            Map.Entry<Double, Double> upperEntry = getUpperBound(strike);
            if (null == upperEntry) {
                return lastEntry().getValue();
            }

            vol = Util.interpolate(lowerEntry.getKey(), lowerEntry.getValue(), upperEntry.getKey(), upperEntry.getValue(), strike);
            return vol;
        }
    }

    public boolean isEmpty() {
        return maturities.isEmpty();
    }

    public void setForward(int maturity, double forward) {
        StrikeVols strikeVols = maturities.get(maturity);
        if (null == strikeVols) {
            strikeVols = new StrikeVols();
            maturities.put(maturity, strikeVols);
        }

        strikeVols.forward = forward;
    }

    public Double getInterest(int maturity) {
        if (0 == maturities.size()) {
            return null;
        }

        // Try exact
        StrikeVols strikeVols = maturities.get(maturity);
        if (null != strikeVols) {
            return strikeVols.interest;
        }

        // Find the closest maturity
        Integer minDistance = null;
        for (Map.Entry<Integer, StrikeVols> entry : maturities.entrySet()) {
            int distance = Math.abs(entry.getKey() - maturity);
            if ((null == minDistance) || (distance < minDistance)) {
                minDistance = distance;
                strikeVols = entry.getValue();
            }
        }
        if (strikeVols != null) {
            return strikeVols.interest;
        }

        return null;
    }

    @Override
    public Double getForward(int maturity) {
        if (0 == maturities.size()) {
            return null;
        }

        // Try exact
        StrikeVols strikeVols = maturities.get(maturity);
        if (null != strikeVols) {
            return strikeVols.forward;
        }

        return null;
    }

    public void add(int maturity, VolatilitySurface.StrikeVols vols) {
        maturities.put(maturity, vols);
    }

    public Double getError() {
        return error;
    }

    public void setError(Double error) {
        this.error = error;
    }

    public VolatilitySurface computeFittedVol(Double refVol) {
        if ((null == spot) || (null == refVol)) {
            return null;
        }

        VolatilitySurface resultSurface = new VolatilitySurface();
        resultSurface.setSpot(spot);

        NormalDistribution stdDist = new NormalDistribution();

        for (Map.Entry<Integer, StrikeVols> maturityEntry : maturities.entrySet()) {
            Integer maturity = maturityEntry.getKey();
            StrikeVols strikeVols = maturityEntry.getValue();

            double yf = Util.yearFraction(maturity);
            double dev = refVol * Math.sqrt(yf);

            double minVol = refVol;
            double maxVol = refVol;
            List<WeightedObservedPoint> points = new ArrayList<>(strikeVols.size());
            for (Map.Entry<Double, Double> volEntry : strikeVols.entrySet()) {
                Double strike = volEntry.getKey();
                Double vol = volEntry.getValue();
                minVol = Math.min(minVol, vol);
                maxVol = Math.max(maxVol, vol);

                double x = Math.log(strike / spot);
                double z = x / dev; // standard devs
                if (Math.abs(z) > FIT_VOL_MAX_DEVS) {
                    continue;
                }

                double weight = stdDist.density(z);
                Double y = volEntry.getValue();
                points.add(new WeightedObservedPoint(weight, z, y));
            }

            // Fit polynomial of degree 2
            double [] c = null;
            if (points.size() > 3) {
                PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
                c = fitter.fit(points);
            }

            // Compute fitted vols
            StrikeVols fitted = new StrikeVols();
            for (Map.Entry<Double, Double> volEntry : strikeVols.entrySet()) {
                Double strike = volEntry.getKey();
                Double vol = volEntry.getValue();

                if (c != null) {
                    double x = Math.log(strike / spot);
                    double z = x / dev; // standard devs
                    if (Math.abs(z) < FIT_VOL_MAX_DEVS) {
                        vol = c[0] + c[1] * z + c[2] * z * z;
                        vol = Math.max(vol, minVol);
                        vol = Math.min(vol, maxVol);
                    }

                }
                fitted.put(strike, vol);
            }

            resultSurface.maturities.put(maturity, fitted);
        }

        return resultSurface;
    }

    public Pair<Double, Double> getForwardVol() {
        for (StrikeVols strikeVols : maturities.values()) {
            if (null == strikeVols.forward) {
                continue;
            }

            Double vol =  strikeVols.interpolate(strikeVols.forward);
            if (null == vol) {
                continue;
            }

            return new Pair<>(strikeVols.forward, vol);
        }

        return null;
    }
}
