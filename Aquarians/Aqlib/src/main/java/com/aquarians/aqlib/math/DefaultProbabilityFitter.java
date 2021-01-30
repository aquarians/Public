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
import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Util;
import org.apache.commons.math3.distribution.NormalDistribution;
import sun.text.resources.th.FormatData_th;

import java.text.DecimalFormat;
import java.util.*;

public class DefaultProbabilityFitter {

    public static final double MAD_TO_DEV_FACTOR = 1.4826022185056023;
    public static final double IQR_RANGE_FACTOR = 2.0; // 1.5 is the accepted "outliers" factor but 2.0 gives a better fit for a normal distribution
    public static final double INLIER_RANGE_FACTOR = 0.02;
    private static final int MIN_SAMPLES_SIZE = 32;
    private static final double OUTLIERS_PROBABILITY = 0.01;

    public enum eOutliersMode {
        outliersModeInterquartile,
        outliersModeProbability
    }

    private eOutliersMode outliersMode = eOutliersMode.outliersModeProbability;
    private double outliersProbability = OUTLIERS_PROBABILITY;

    public List<Double> samples;
    boolean isSorted;
    public Double min = null;
    public Double max = null;
    public Double total = null;
    public int inliers = 0;

    private double mean = 0.0;
    private double dev = 0.0;

    private Double iqrMin;
    private Double iqrMax;

    private double medianMean;
    private double medianDev; // Standard deviation computed using robust method (median rather than mean)

    private final Random random = new Random();

    public Integer skew;
    public double xmin;
    public double xmax;
    public double pmin;
    public double pmax;

    public DefaultProbabilityFitter() {
        samples = new ArrayList<>();
    }

    public DefaultProbabilityFitter(DefaultProbabilityFitter copy) {
        samples = new ArrayList<>(copy.samples);
    }

    public DefaultProbabilityFitter(List<Double> samples) {
        this.samples = new ArrayList<>(samples.size());
        for (double sample : samples) {
            addSample(sample);
        }
    }

    public void setOutliersMode(eOutliersMode outliersMode) {
        this.outliersMode = outliersMode;
    }

    public void setOutliersProbability(double outliersProbability) {
        this.outliersProbability = outliersProbability;
    }

    public void clear() {
        samples.clear();
        min = null;
        max = null;
        total = null;
    }

    public List<Double> getSamples() {
        return samples;
    }

    public DefaultProbabilityFitter(int estimatedSize) {
        samples = new ArrayList<Double>(estimatedSize);
    }

    public double getVol(double dt) {
        return  dev / Math.sqrt(dt);
    }

    public double getGrowth(double dt) {
        double vol = getVol(dt);
        return  (mean / dt) + vol * vol * 0.5;
    }

    public DefaultProbabilityFitter clone() {
        return new DefaultProbabilityFitter(samples);
    }

    public int size() {
        return samples.size();
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Double getAbsMax() {
        if ((null == min) || (null == max)) {
            return null;
        }

        return Math.max(Math.abs(min), Math.abs(max));
    }

    public int icdf_pos(double prob) {
        prob = Util.limitProbability(prob);
        int length = samples.size() - 1;
        int pos = (int) Math.round(length * prob);
        return pos;
    }

    public DefaultProbabilityFitter slice(double prob) {
        DefaultProbabilityFitter sliced = new DefaultProbabilityFitter(samples.size());
        for (int i = 0; i < samples.size(); i++) {
            double p = (0.0 + i) / samples.size();
            if (p > prob) {
                break;
            }

            sliced.addSample(samples.get(i));
        }

        return sliced;
    }

    public double average(double prob) {
        double total = 0.0;
        int count = samples.size();
        for (int i = 0; i < samples.size(); i++) {
            double p = (0.0 + i) / samples.size();
            if (p > prob) {
                count = i;
                break;
            }

            total += samples.get(i);
        }

        return total / count;
    }

    public Double icdf(double prob) {
        if (!isSorted) {
            Collections.sort(samples);
            isSorted = true;
        }

        int pos = icdf_pos(prob);
        if (pos >= samples.size()) {
            return null;
        }

        double x = samples.get(pos);
        return x;
    }

    public double getPdf(double x) {
        Double p = null;
        for (int i = 0; i < samples.size(); ++i) {
            double sample = samples.get(i);
            if (sample < x) {
                continue;
            }

            if (null == p) {
                p = (i + 0.0) / samples.size(); // CDF(x)
                continue;
            }

            double psample = (i + 0.0) / samples.size(); // CDF(sample)
            double dp = psample - p;
            if (dp < 0.01) {
                continue;
            }

            double density = dp / (sample - x);
            return density;
        }


        return 0.0;
    }

    public double getCdf(double x) {
        int i = 0;
        for (i = 0; i < samples.size(); ++i) {
            double sample = samples.get(i);
            if (sample >= x) {
                break;
            }
        }

        double p = ((double) i) / samples.size();
        return p;
    }

    public void addSample(Double value) {
        if (null == value) {
            return;
        }

        samples.add(value);
        min = (null != min) ? Math.min(min, value) : value;
        max = (null != max) ? Math.max(max, value) : value;
        if (null == total) total = 0.0;
        total += value;
        isSorted = false;
    }

    public void multiply(double factor) {
        min = null;
        max = null;
        for (int i = 0; i < samples.size(); i++) {
            double x = samples.get(i) * factor;
            samples.set(i, x);
            min = (null != min) ? Math.min(min, x) : x;
            max = (null != max) ? Math.max(max, x) : x;
        }
    }

    public double getTotal() {
        return total;
    }

    public int[] buckets;

    public double getX(int bucket) {
        double dx = (max - min) / buckets.length;
        double x = min + dx * bucket;
        return x;
    }

    public double getDx() {
        double dx = (max - min) / buckets.length;
        return dx;
    }

    public double density(double x, double dprob) {
        int pos = Util.lowerBound(samples, x);
        if (pos >= samples.size()) {
            return 0.0;
        }

        double count = samples.size() - 1;
        double probPos = pos / count;
        double probDown = probPos - dprob;
        double probUp = probPos + dprob;
        int down = (int) Math.round(probDown * count);
        if (down < 0) {
            return 0.0;
        }
        int up = (int) Math.round(probUp * count);
        if (up > count) {
            return 0.0;
        }

        double xdown = samples.get(down);
        double xup = samples.get(up);
        double dx = xup - xdown;
        double pdf = dprob / dx;
        return pdf;
    }

    public int bucketsSize() {
        return buckets.length;
    }

    public double bucketX(int index) {
        return getX(index);
    }

    public double bucketY(int index) {
        double dx = (max - min) / buckets.length;
        double dp = (0.0 + buckets[index]) / samples.size();
        double pdf = dp / dx;
        return pdf;
    }

    public double bucketYHistogram(int index) {
        return buckets[index];
    }

    // Computes the cumulative distribution function between [X_min, X_max]
    public void compute(int count) {
        compute();
        buckets = new int[count + 1];
        for (int i = 0; i < count; ++i) {
            buckets[i] = 0;
        }
        double dx = (max - min) / count;
        for (int i = 0; i < samples.size(); ++i) {
            double x = samples.get(i);
            int bucket = (int)Math.round((x - min) / dx);
            if ((bucket < 0) || (bucket > count))
            {
                continue;
            }

            buckets[bucket]++;
        }
    }

    public boolean isOutlier(double sample) {
        if ((null == iqrMin) || (null == iqrMax)) {
            return false;
        }

        return (sample < iqrMin) || (sample > iqrMax);
    }

    private void computeTotal(boolean excludeOutliers) {
        total = 0.0;
        inliers = 0;
        min = null;
        max = null;
        for (double x : samples) {
            if (excludeOutliers && isOutlier(x)) {
                continue;
            }

            inliers++;
            total += x;
            min = (null != min) ? Math.min(min, x) : x;
            max = (null != max) ? Math.max(max, x) : x;
        }
    }

    public DefaultProbabilityFitter compute() {
        compute(false);
        return this;
    }

    public DefaultProbabilityFitter compute(boolean excludeOutliers) {
        if (excludeOutliers) {
            computeOutliers();
        }
        computeTotal(excludeOutliers);
        mean = total / Math.max(inliers, 1);
        computeDev(excludeOutliers);
        return this;
    }

    private void computeDev(boolean excludeOutliers) {
        double var = 0.0;
        for (Double value : samples) {
            if (excludeOutliers && isOutlier(value)) {
                continue;
            }

            double delta = value - mean;
            var += delta * delta;
        }

        var = var / Math.max(inliers, 1);
        dev = Math.sqrt(var);
    }

    public void computeMeanAndDev() {
        mean = total / samples.size();
        double var = var(samples, mean);
        dev = Math.sqrt(var);
    }

    public double getMedianMean() {
        return medianMean;
    }

    public double getMedianDev() {
        return medianDev;
    }

    public static double mean(List<Double> values) {
        if (values.size() < 1) {
            return Double.NaN;
        }

        double total = 0.0;
        for (double value : values) {
            total += value;
        }

        total /= values.size();
        return total;
    }

    public static double dev(List<Double> values, Double mean) {
        return Math.sqrt(var(values, mean));
    }

    public static double var(List<Double> values, Double mean) {
        if (values.size() < 2) {
            return Double.NaN;
        }

        double total = 0.0;
        for (double value : values) {
            double delta = value - mean;
            total += delta * delta;
        }

        int n = Math.max(values.size() - 1, 1);
        total = total / n;
        return total;
    }

    public double getMean() {
        return mean;
    }

    public double getDev() {
        return dev;
    }

    public void computeMedians() {
        Collections.sort(samples);
        computeMedianMean();
        computeMedianDev();
    }

    private void computeMedianMean() {
        if (samples.size() < 2) {
            medianMean = 0.0;
            return;
        }

        // When the samples number is odd there's a mid sample. Ex: [-1, 0, 1]), mid sample = 0.
        // When even there's no mid sample but two of them. Ex: [-1, 1]), mid lower samples = -1, mid upper sample = 1
        int medianUpperPos = samples.size() / 2;
        int medianLowerPos = (0 != samples.size() % 2) ? medianUpperPos : medianUpperPos - 1;
        medianMean = (samples.get(medianUpperPos) + samples.get(medianLowerPos)) / 2.0;
    }

    // See http://en.wikipedia.org/wiki/Median_absolute_deviation
    private void computeMedianDev() {
        if (samples.size() < 2) {
            medianDev = 0.0;
            return;
        }

        List<Double> deviations = new ArrayList<Double>();
        for (double x : samples) {
            double dev = Math.abs(x - medianMean);
            deviations.add(dev);
        }
        Collections.sort(deviations);
        double mad = deviations.get(deviations.size() / 2);
        medianDev = MAD_TO_DEV_FACTOR * mad;
    }

    public double rnd() {
        double u = random.nextDouble();
        int pos = (int) Math.round((samples.size() - 1) * u);
        double x = samples.get(pos);
        return x;
    }

    public double rnd(int n) {
        double x = rnd();
        for (int i = 1; i <= n; ++i) {
            double y = rnd();
            x += y;
        }
        return x;
    }

    public static DefaultProbabilityFitter extractPrices(List<PriceRecord> prices) {
        DefaultProbabilityFitter fitter = new DefaultProbabilityFitter(prices.size());
        for (PriceRecord price : prices) {
            fitter.addSample(price.price);
        }
        return fitter;
    }

    public static List<Double> computeReturnsRaw(List<PriceRecord> prices) {
        List<Double> returns = new ArrayList<Double>(prices.size());

        PriceRecord previous = null;
        for (PriceRecord current : prices) {
            if (current.price < Util.ZERO) {
                previous = null;
                continue;
            }

            if (null != previous) {
                double ret = Math.log(current.price / previous.price);
                returns.add(ret);
            }

            previous = current;
        }

        return returns;
    }

    public static DefaultProbabilityFitter computeReturns(List<PriceRecord> prices) {
        return new DefaultProbabilityFitter(computeReturnsRaw(prices));
    }

    public static DefaultProbabilityFitter computeReturnsFromPrices(List<Double> prices) {
        List<Double> returns = new ArrayList<Double>(prices.size());

        Double previous = null;
        for (Double current : prices) {
            if (current < Util.ZERO) {
                previous = null;
                continue;
            }

            if (null != previous) {
                double ret = Math.log(current / previous);
                returns.add(ret);
            }

            previous = current;
        }

        return new DefaultProbabilityFitter(returns);
    }

    public DefaultProbabilityFitter squared() {
        List<Double> squares = new ArrayList<Double>(samples.size());
        for (Double sample : samples) {
            squares.add(sample * sample);
        }
        return new DefaultProbabilityFitter(squares);
    }

    public void sort() {
        Collections.sort(samples);
        isSorted = true;
        if (samples.size() > 0) {
            min = samples.get(0);
            max = samples.get(samples.size() - 1);
        }
    }

    public double sample(int pos) {
        return samples.get(pos);
    }

    public double normalizeRegular(double sample) {
        return (sample - mean) / dev;
    }

    public double normalizeMedian(double sample) {
        return (sample - medianMean) / medianDev;
    }

    public void normalize() {
        for (int i = 0; i < samples.size(); i++) {
            double sample = samples.get(i);
            sample = normalizeRegular(sample);
            samples.set(i, sample);
        }
    }

    public static List<PriceRecord> generatePrices(int size, double vol) {
        List<PriceRecord> prices = new ArrayList<PriceRecord>(size + 1);

        double yf = Util.yearFraction(1);
        NormalDistribution dist = new NormalDistribution(vol * vol * 0.5 * yf, vol * Math.sqrt(yf));
        double spot = 100.0;
        Day day = Day.now().nextTradingDay();
        for (int i = 0; i <= size; i++) {
            prices.add(new PriceRecord(day, spot));
            double x = dist.sample();
            spot *= Math.exp(x);
            day = day.nextTradingDay();
        }

        return prices;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public void setDev(double dev) {
        this.dev = dev;
    }

    public void save(String file) {
        CsvFileWriter writer = null;
        try {
            writer = new CsvFileWriter(file);
            for (int i = 0; i < bucketsSize(); i++) {
                writer.write(Util.newArrayList(Double.toString(bucketX(i)), Double.toString(bucketY(i))));
            }
        } finally {
            if (null != writer) {
                writer.close();
            }
        }
    }

    public void saveHistogram(String file) {
        CsvFileWriter writer = null;
        try {
            writer = new CsvFileWriter(file);
            for (int i = 0; i < bucketsSize(); i++) {
                writer.write(Util.newArrayList(Double.toString(bucketX(i)), Double.toString(bucketYHistogram(i))));
            }
        } finally {
            if (null != writer) {
                writer.close();
            }
        }
    }

    public void print() {
        for (int i = 0; i < bucketsSize(); i++) {
            System.out.println(Double.toString(bucketX(i)) + ","  + Double.toString(bucketY(i)));
        }
    }

    public void limitOutliers() {
        if (samples.size() < 10) {
            return;
        }

        List<Double> sortedSamples = new ArrayList<Double>(samples);
        Collections.sort(sortedSamples);

        // Interquantile range
        int q1Pos = (int) Math.round(sortedSamples.size() * 0.25);
        int q3Pos = (int) Math.round(sortedSamples.size() * 0.75);
        double q1 = sortedSamples.get(q1Pos);
        double q3 = sortedSamples.get(q3Pos);
        double iqr = q3 - q1;
        double factor = 1.5;
        double xmin = q1 - iqr * factor;
        double xmax = q3 + iqr * factor;

        for (int i = 0; i < samples.size(); i++) {
            double x = samples.get(i);
            x = Math.max(x, xmin);
            x = Math.min(x, xmax);
            samples.set(i, x);
        }
    }

    public DefaultProbabilityFitter filterOutliers() {
        return filterOutliers(IQR_RANGE_FACTOR);
    }

    public void computeOutliers() {
        if (eOutliersMode.outliersModeInterquartile == outliersMode) {
            computeOutliersInterquartile();
        } else if (eOutliersMode.outliersModeProbability == outliersMode) {
            computeOutliersProbability();
        } else {
            throw new RuntimeException("Unknown outliers mode: " + outliersMode.name());
        }
    }

    private void computeOutliersProbability() {
        if (samples.size() < 2) {
            return;
        }

        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);

        double count = samples.size() * outliersProbability;
        int minpos = (int)Math.round(count);
        int maxpos = (int)Math.round(samples.size() - 1 - count);

        iqrMin = sorted.get(minpos);
        iqrMax = sorted.get(maxpos);
    }

    private void computeOutliersInterquartile() {
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int q1pos = (int)Math.round(samples.size() * 0.25);
        int q3pos = (int)Math.round(samples.size() * 0.75);
        double q1 = sorted.get(q1pos);
        double q3 = sorted.get(q3pos);
        double iqr = (q3 - q1);
        iqrMin = q1 - iqr * IQR_RANGE_FACTOR;
        iqrMax = q3 + iqr * IQR_RANGE_FACTOR;
    }

    public DefaultProbabilityFitter filterOutliers(double factor) {
        if (samples.size() < 10) {
            return new DefaultProbabilityFitter(samples);
        }

        List<Double> sorted = new ArrayList<Double>(samples);
        Collections.sort(sorted);

        // Interquantile range
        int q1Pos = (int) Math.round(sorted.size() * 0.25);
        int q3Pos = (int) Math.round(sorted.size() * 0.75);
        double q1 = sorted.get(q1Pos);
        double q3 = sorted.get(q3Pos);
        double iqr = (q3 - q1) * factor;
        double xmin = q1 - iqr;
        double xmax = q3 + iqr;

        List<Double> filtered = new ArrayList<Double>(samples.size());
        for (double x : samples) {
            if (x < xmin) {
                x = xmin;
            }
            if (x > xmax) {
                x = xmax;
            }

            filtered.add(x);
        }

        return new DefaultProbabilityFitter(filtered);
    }

    public DefaultProbabilityFitter filterInliers(double factor) {
        if (samples.size() < 10) {
            return new DefaultProbabilityFitter(samples);
        }

        List<Double> sorted = new ArrayList<Double>(samples);
        Collections.sort(sorted);

        int mid = sorted.size() / 2;
        double xmid = sorted.get(mid);

        // Interquantile range
        int q1Pos = (int) Math.round(sorted.size() * 0.49);
        int q3Pos = (int) Math.round(sorted.size() * 0.51);
        double q1 = sorted.get(q1Pos);
        double q3 = sorted.get(q3Pos);
        double xmin = q1;
        double xmax = q3;

        List<Double> filtered = new ArrayList<Double>(samples.size());
        for (double x : samples) {
            if ((x > xmin) && (x < xmax)) {
                continue;
            }

            filtered.add(x);
        }

        return new DefaultProbabilityFitter(filtered);
    }
    public void adjustPnlDistribution(double probability) {
        if (samples.size() < 10) {
            return;
        }

        sort();
        probability = Util.limitProbability(probability);
        int startPos = (int) Math.round(samples.size() * probability);
        int endPos = (int) Math.round((samples.size() - 1) * (1.0 - probability));
        double xmin = samples.get(0);
        double xmax = samples.get(endPos);
        for (int i = 0; i < samples.size(); i++) {
            if (i < startPos) {
                samples.set(i, xmin);
            } else if (i > endPos) {
                samples.set(i, xmax);
            }
        }
    }

    /**
     * Skew direction
     * @return -1 for going down, 0 for staying the same, 1 for going up
     */
    public void computeSkew() {
        skew = null;
        sort();
        if (samples.size() < 64) {
            return;
        }

        // Interquantile range
        int q1Pos = (int) Math.round(samples.size() * 0.25);
        int q3Pos = (int) Math.round(samples.size() * 0.75);
        double q1 = samples.get(q1Pos);
        double q3 = samples.get(q3Pos);
        double iqr = q3 - q1;
        double factor = 1.5;
        xmin = q1 - iqr * factor;
        xmax = q3 + iqr * factor;

        pmin = getCdf(xmin);
        pmax = 1.0 - getCdf(xmax);

        double total = (pmin * xmin + pmax * xmax) * 10000.0;
        skew = (int) Math.signum(Math.round(total));
    }

    public DefaultProbabilityFitter abs() {
        List<Double> absolutes = new ArrayList<Double>(samples.size());
        for (double x : samples) {
            absolutes.add(Math.abs(x));
        }
        return new DefaultProbabilityFitter(absolutes);
    }

    public DefaultProbabilityFitter sum(int count) {
        List<Double> totals = new ArrayList<Double>(1 + samples.size() / count);
        for (int i = 0; i < samples.size() - count; i += count) {
            double total = 0.0;
            for (int k = 0; k < count; k++) {
                total += samples.get(i + k);
            }
            totals.add(total);
        }
        return new DefaultProbabilityFitter(totals);
    }

    public void addAll(Collection<Double> values) {
        for (Double value : values) {
            addSample(value);
        }
    }

    public double getRet() {
        return mean * Util.TRADING_DAYS_IN_YEAR;
    }

    public double getVol() {
        return dev * Math.sqrt(Util.TRADING_DAYS_IN_YEAR);
    }

    public double getSharpe() {
        return getSharpe(size());
    }

    public double getSharpe(int n) {
        return mean * Math.sqrt(n) / dev;
    }

    @Override
    public String toString() {
        DecimalFormat FORMAT = new DecimalFormat("###.##");
        return "Mean=" + FORMAT.format(mean) + " Dev=" + FORMAT.format(dev);
    }

    public static double computeSharpe(List<Double> prices) {
        DefaultProbabilityFitter fitter = computeReturnsFromPrices(prices);
        fitter.compute();
        return fitter.getMean() * Math.sqrt(Util.TRADING_DAYS_IN_YEAR) / fitter.getDev();
    }
}
