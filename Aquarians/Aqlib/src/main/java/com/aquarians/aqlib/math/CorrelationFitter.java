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

package com.aquarians.aqlib.math;

import com.aquarians.aqlib.CsvFileWriter;
import com.aquarians.aqlib.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CorrelationFitter {

    private final List<Double> xs;
    private final List<Double> ys;

    public CorrelationFitter(List<Double> xs, List<Double> ys) {
        if (xs.size() != ys.size()) {
            throw new RuntimeException("Size mimatch!");
        }

        this.xs = xs;
        this.ys = ys;
    }

    public int size() {
        return xs.size();
    }

    public static final class Builder {
        private final List<Double> xs = new LinkedList<Double>();
        private final List<Double> ys = new LinkedList<Double>();

        public int size() {
            return xs.size();
        }

        public void add(double x, double y) {
            xs.add(x);
            ys.add(y);
        }

        public List<Double> getXs() {
            return xs;
        }

        public List<Double> getYs() {
            return ys;
        }

        public CorrelationFitter build() {
            return new CorrelationFitter(xs, ys);
        }
    }

    private double computeCovariance(DefaultProbabilityFitter xs, DefaultProbabilityFitter ys, boolean excludeOutliers) {
        double mean = 0.0;
        int count = 0;
        for (int i = 0; i < xs.size(); ++i) {
            double x = xs.sample(i);
            double y = ys.sample(i);
            if (excludeOutliers && (xs.isOutlier(x) || ys.isOutlier(y))) {
                continue;
            }
            double dx = x - xs.getMean();
            double dy = y - ys.getMean();
            mean += dx * dy;
            count++;
        }
        mean /= Math.max(count, 1);
        return mean;
    }

    public double computeCorrelation() {
        return computeCorrelation(false);
    }

    public double computeCorrelation(boolean excludeOutliers) {
        DefaultProbabilityFitter xs = new DefaultProbabilityFitter(this.xs);
        DefaultProbabilityFitter ys = new DefaultProbabilityFitter(this.ys);
        xs.compute(excludeOutliers);
        ys.compute(excludeOutliers);

        double covariance = computeCovariance(xs, ys, excludeOutliers);
        double variance = xs.getDev() * ys.getDev();
        if (variance < Util.ZERO) {
            return 0.0;
        }
        double correlation = covariance / variance;
        return correlation;
    }

    public double getCorrelation() {
        return computeCorrelation();
    }

    private static CorrelationFitter filterOutliers(List<Double> xs, List<Double> ys) {
        List<Double> original_xs = new ArrayList<Double>(xs);
        Collections.sort(original_xs);

        // Interquantile range
        int q1Pos = (int) Math.round(original_xs.size() * 0.25);
        int q3Pos = (int) Math.round(original_xs.size() * 0.75);
        double q1 = original_xs.get(q1Pos);
        double q3 = original_xs.get(q3Pos);
        double iqr = (q3 - q1) * DefaultProbabilityFitter.IQR_RANGE_FACTOR;
        double xmin = q1 - iqr;
        double xmax = q3 + iqr;

        List<Double> filtered_xs = new ArrayList<Double>(xs.size());
        List<Double> filtered_ys = new ArrayList<Double>(ys.size());
        for (int i = 0; i < xs.size(); i++) {
            double x = xs.get(i);
            double y = ys.get(i);
            if ((x < xmin) || (x > xmax)) {
                continue;
            }

            filtered_xs.add(x);
            filtered_ys.add(y);
        }

        return new CorrelationFitter(filtered_xs, filtered_ys);
    }

    public CorrelationFitter filterOutliers() {
        CorrelationFitter filtered = filterOutliers(xs, ys);
        CorrelationFitter result = filterOutliers(filtered.ys, filtered.xs);
        return result;
    }

    public void save(String file) {
        CsvFileWriter writer = null;
        try {
            writer = new CsvFileWriter(file);
            for (int i = 0; i < xs.size(); i++) {
                writer.write(Util.newArrayList(Integer.toString(i), Double.toString(xs.get(i)), Double.toString(ys.get(i))));
            }
        } finally {
            writer.close();
        }
    }

    public double computeRobustCorrelation() {
        if (xs.size() != ys.size()) {
            throw new RuntimeException("Size mismatch!");
        }

        if (xs.size() < 4) {
            return 0.0;
        }

        return filterOutliers().getCorrelation();
    }

    public List<Double> getXs() {
        return xs;
    }

    public List<Double> getYs() {
        return ys;
    }
}
