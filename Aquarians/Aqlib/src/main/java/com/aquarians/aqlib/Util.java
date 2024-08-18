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

package com.aquarians.aqlib;

import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.math.Distance;
import com.aquarians.aqlib.math.Function;
import com.aquarians.aqlib.math.PriceRecord;
import com.sun.org.apache.xpath.internal.objects.XNull;

import java.text.DecimalFormat;
import java.util.*;

public class Util {

    public static final double MINIMUM_PRICE = 0.01;
    public static final double ZERO = 1e-12;
    public static final double ONE = 1.0 - 1e-12;
    public static final double INFINITY = 1.0 / ZERO;
    public static final int TRADING_DAYS_IN_YEAR = 252;
    public static final int CALENDAR_DAYS_IN_YEAR = 365;
    public static final int TRADING_DAYS_IN_WEEK = 5;
    public static final int CALENDAR_DAYS_IN_WEEK = 7;
    public static final int TRADING_DAYS_IN_MONTH = 20;
    public static final int DEFAULT_HEDGE_FREQUENCY = 1;
    public static final DecimalFormat DOUBLE_DIGIT_FORMAT = new DecimalFormat("#0.00");
    public static final DecimalFormat FOUR_DIGIT_FORMAT = new DecimalFormat("#0.0000");
    public static final DecimalFormat SIX_DIGIT_FORMAT = new DecimalFormat("#0.000000");

    public static boolean safeEquals(Object left, Object right) {
        if ((null == left) || (null == right)) {
            return false;
        }

        return left.equals(right);
    }

    public static void safeJoin(Thread thread) {
        try {
            thread.join();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static void safeWait(Object monitor) {
        try {
            monitor.wait();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static void safeWait(Object monitor, long millis) {
        if (millis <= 0) {
            return;
        }

        try {
            monitor.wait(millis);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static double limitProbability(double p) {
        p = Math.max(p, Util.ZERO);
        p = Math.min(p, Util.ONE);
        return p;
    }

    /**
     * Returns an index pointing to the first element of given list which does not compare less than given key
     * @param list list of elements, sorted in ascending order
     * @param key searched element
     * @param <T> elements type
     * @return index of first element such that list(index) >= key, or list.size() if no such element exists
     */
    public static <T> int lowerBound(List<? extends Comparable<? super T>> list, T key) {
        // The index of the search key, if it is contained in the list; otherwise (-(insertion_point) - 1).
        // The insertion point is defined as the point at which the key would be inserted into the list:
        // the index of the first element greater than the key, or list.size() if all elements in the
        // list are less than the specified key.
        int index = Collections.binarySearch(list, key);
        if (index >= 0) {
            // Found exact key but it may not be the first if there are duplicates
            int previousIndex = index - 1;
            while (previousIndex > -1 && list.get(previousIndex).compareTo(key) == 0) {
                index = previousIndex;
                previousIndex--;
            }
            return index;
        }

        // index = -position - 1 => position = - (index + 1)
        int position = -(index + 1);
        return position;
    }

    /**
     * Builds an array from a list of values
     */
    public static  <T> ArrayList<T> newArrayList(T... values) {
        ArrayList<T> array = new ArrayList<T>(values.length);
        for (int i = 0; i < values.length; ++i) {
            array.add(values[i]);
        }
        return array;
    }

    public static int daysToExpiry(double yearFraction) {
        return (int) Math.round(yearFraction * Util.TRADING_DAYS_IN_YEAR);
    }

    public static double yearFraction(int daysToExpiry) {
        return (0.0 + daysToExpiry) / Util.TRADING_DAYS_IN_YEAR;
    }

    // Number of steps required for a binary search to go from start to end with an accuracy of precision
    public static int getBinarySearchSteps(double start, double end, double precision) {
        return 1 + (int)Math.round(Math.log(Math.abs(end - start) / precision) / Math.log(2.0));
    }

    public static double binarySearch(
            Function function,
            double targetValue,
            double minArgument,
            double maxArgument,
            int steps,
            double precision) {
        return binarySearch(function, targetValue, minArgument, maxArgument, steps, precision, null);
    }

    public static double binarySearch(
            Function function,
            double targetValue,
            double minArgument,
            double maxArgument,
            int steps,
            double precision,
            Ref<Boolean> successStatus) {

        double minValue = function.value(minArgument);
        if (Math.abs(minValue - targetValue) <= precision)
        {
            if (null != successStatus) successStatus.value = true;
            return minArgument;
        }

        double maxValue = function.value(maxArgument);
        if (Math.abs(maxValue - targetValue) <= precision)
        {
            if (null != successStatus) successStatus.value = true;
            return maxArgument;
        }

        // Check if solvable
        double bestArgument = (minArgument + maxArgument) * 0.5;
        if ((targetValue - minValue) * (targetValue - maxValue) > 0.0)
        {
            // Find which argument is closer
            double diffMinArg = Math.abs(targetValue - minValue);
            double diffMaxArg = Math.abs(targetValue - maxValue);
            if (null != successStatus) successStatus.value = false;
            bestArgument = diffMinArg < diffMaxArg ? minArgument : maxArgument;
            return bestArgument;
        }

        for (int i = 0; i < steps; i++)
        {
            bestArgument = (minArgument + maxArgument) * 0.5;
            double curValue = function.value(bestArgument);
            double diff = Math.abs(targetValue - curValue);
            if (diff <= precision)
            {
                if (null != successStatus) successStatus.value = true;
                return bestArgument;
            }

            // Check if targetValue lies inside the interval  [minArgument, curArgument]
            if ((targetValue - minValue) * (targetValue - curValue) <= 0.0)
            {
                // Use [minArgument, curArgument] interval in the next step
                maxValue = curValue;
                maxArgument = bestArgument;
            }
            else
            {
                // Use [curArgument, maxArgument] interval in the next step
                minValue = curValue;
                minArgument = bestArgument;
            }
        }

        if (null != successStatus) successStatus.value = false;
        return bestArgument;
    }

    /**
     * Linear interpolation of y = f(x) on interval [x1, x2]
     * @param x1 x1
     * @param y1 f(x1)
     * @param x2 x2
     * @param y2 f(x2)
     * @param x any x
     * @return linearly interpolated f(x)
     */
    public static double interpolate(double x1, double y1, double x2, double y2, double x) {
        double y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
        return y;
    }

    /**
     * Given a discretized function y = f(x) given at discrete points, interpolates value at real x
     * @param x argument x
     * @param xs discretization of arguments: x0, x1, ... xN
     * @param ys discretization of values: y0, y1, ... yN
     * @return f(x)
     */
    public static double interpolate(double x, List<Double> xs, List<Double> ys) {
        if (0 == xs.size()) {
            return 0.0;
        }

        int posUp = Util.lowerBound(xs, x);
        if (xs.size() == posUp) {
            return ys.get(ys.size() - 1);
        }

        double xup = xs.get(posUp);
        double yup = ys.get(posUp);
        if (Util.doubleEquals(x, xup)) {
            return yup;
        }

        // Interpolate
        int posDown = posUp - 1;
        if (posDown < 0) {
            return yup;
        }

        double xdown = xs.get(posDown);
        double ydown = ys.get(posDown);
        double y = interpolate(xdown, ydown, xup, yup, x);
        return y;
    }

    /**
     * Creates an {@code ArrayList} instance of given size initialized with given value.
     * @param size array size
     * @param value value to fill the array with
     * @return a new {@code ArrayList} of given size filled with given value
     */
    public static <E> ArrayList<E> newInitializedArrayList(int size, E value) {
        ArrayList<E> list = new ArrayList<E>(size);
        for (int i = 0; i < size; ++i) {
            list.add(value);
        }
        return list;
    }

    /**
     * Rounds given value to requested number of decimals
     * @param value value to round, ex: 1.2345
     * @param decimals number of decimals, ex: 2
     * @return rounding result, ex: 1.23 for value=1.2345 and decimals=2
     */
    public static double round(double value, int decimals) {
        if (decimals < 0) {
            throw new RuntimeException("Decimals must be positive or zero");
        }

        double powerOfTen = Math.pow(10.0, decimals);
        double multipliedByPower = value * powerOfTen;
        long rounded = Math.round(multipliedByPower);
        double divided = rounded / powerOfTen;
        return divided;
    }

    public static Double min(Double ... values) {
        Double minValue = null;

        for (int i = 0; i < values.length; ++i) {
            Double value = values[i];
            if (null == value) {
                continue;
            }

            if ((null == minValue) || (value < minValue)) {
                minValue = value;
            }
        }

        return minValue;
    }


    public static Double max(Double ... values) {
        Double maxValue = null;

        for (int i = 0; i < values.length; ++i) {
            Double value = values[i];
            if (null == value) {
                continue;
            }

            if ((null == maxValue) || (value > maxValue)) {
                maxValue = value;
            }
        }

        return maxValue;
    }

    public static Double addToSum(Double sum, Double value) {
        if (null == value) {
            return sum;
        }

        if (null == sum) {
            return value;
        }

        return sum + value;
    }

    public static boolean doubleEquals(Double left, Double right) {
        if ((null == left) || (null == right)) {
            return false;
        }

        return Math.abs(left - right) < ZERO;
    }

    public static boolean doubleGE(Double left, Double right) {
        if ((null == left) || (null == right)) {
            return false;
        }

        return left >= right;
    }

    public static String safeFormat(DecimalFormat format, Object value) {
        if (null == value) {
            return "";
        }

        return format.format(value);
    }

    public static double marketVolToVol(double marketVol) {
        return marketVol / 100.0;
    }

    public static double volToMarketVol(double vol) {
        return vol * 100.0;
    }

    public static double marketVolToVar(double marketVol) {
        double vol = marketVol / 100.0;
        return vol * vol;
    }

    public static double varToMarketVol(double var) {
        double vol = Math.sqrt(var);
        return vol * 100.0;
    }

    public static Double ratio(Double first, Double second) {
        if ((null == first) || (null == second)) {
            return null;
        }

        first = Math.abs(first);
        second = Math.abs(second);

        double min = Math.min(first, second);
        double max = Math.max(first, second);

        double ratio = max / Math.max(min, Util.ZERO);
        return ratio;
    }

    public static List<String> toString(List objects) {
        List<String> res = new ArrayList<String>(objects.size());
        for (Object obj : objects) {
            res.add(obj.toString());
        }
        return res;
    }

    // When buying the call and selling the put, parity relationship dictates spot being lower than an upper bound
    public static Double getParitySpotUpperBound(Instrument call, Instrument put) {
        if ((null == call) || (null == put)) {
            return null;
        }

        if (!doubleEquals(call.getStrike(), put.getStrike())) {
            return null;
        }

        // Buy the call
        Double cp = call.getAskPrice();
        // Sell the put
        Double pp = put.getBidPrice();
        if ((null == cp) || (null == pp)) {
            return null;
        }

        double u = call.getStrike() + cp - pp;
        return u;
    }

    // When selling the call and buying the put, parity relationship dictates spot being higher than a lower bound
    public static Double getParitySpotLowerBound(Instrument call, Instrument put) {
        if ((null == call) || (null == put)) {
            return null;
        }

        if (!doubleEquals(call.getStrike(), put.getStrike())) {
            return null;
        }

        // Sell the call
        Double cp = call.getBidPrice();
        // Buy the put
        Double pp = put.getAskPrice();
        if ((null == cp) || (null == pp)) {
            return null;
        }

        double u = call.getStrike() + cp - pp;
        return u;
    }

    // Fits line y(x) = alpha + beta * x
    public static void fitRegressionLine(Points points, Ref<Double> alpha, Ref<Double> beta) {
        double ysum = 0.0;
        double xsum = 0.0;
        double xysum = 0.0;
        double xxsum = 0.0;
        for (int i = 0; i < points.size(); i++) {
            double x = points.x(i);
            double y = points.y(i);
            xsum += x;
            xxsum += x * x;
            ysum += y;
            xysum += x * y;
        }

        double n = Math.max(points.size(), 1);
        beta.value = (n * xysum - xsum * ysum) / (n * xxsum - xsum * xsum);

        double xavg = xsum / n;
        double yavg = ysum / n;
        alpha.value = yavg - beta.value * xavg;
    }

    public static void logStatistics(org.apache.log4j.Logger logger, String tag, DefaultProbabilityFitter fitter) {
        fitter.computeStatistics();

        logger.debug(tag + " statistics: count=" + fitter.size() +
                " mean=" + Util.DOUBLE_DIGIT_FORMAT.format(fitter.getMean()) +
                " dev=" + Util.DOUBLE_DIGIT_FORMAT.format(fitter.getDev()) +
                " min=" + Util.DOUBLE_DIGIT_FORMAT.format(fitter.getMin()) +
                " max=" + Util.DOUBLE_DIGIT_FORMAT.format(fitter.getMax()));
    }

    public static String saveTags(Map<String, String> tags) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            builder.append(first ? "" : ",").append(entry.getKey()).append("=").append(entry.getValue());
            if (first) {
                first = false;
            }
        }

        return builder.toString();
    }

    public static Map<String, String> loadTags(String data) {
        Map<String, String> tags = new TreeMap<>();
        if (null == data) {
            return tags;
        }

        String[] tagValues = data.split(",");
        for (String tagValue : tagValues) {
            int pos = tagValue.indexOf("=");
            if (pos < 0) {
                continue;
            }

            String tag = tagValue.substring(0, pos);
            String value = tagValue.substring(pos + 1);
            tags.put(tag, value);
        }

        return tags;
    }

    public static String format(Double value) {
        if (null == value) {
            return "null";
        }

        return DOUBLE_DIGIT_FORMAT.format(value);
    }

    public static String format(Boolean value) {
        if (null == value) {
            return "null";
        }

        return value ? "true" : "false";
    }

    public static String cwd() {
        return System.getProperty("user.dir");
    }

    public static void plot(String filename, Points ... plots) {
        Set<Double> xs = new TreeSet<>();
        for (Points plot : plots) {
            xs.addAll(plot.getXs());
        }

        CsvFileWriter writer = null;
        try {
            writer = new CsvFileWriter(filename);
            for (double x : xs) {
                List<String> values = new ArrayList<>(plots.length + 1);
                values.add(FOUR_DIGIT_FORMAT.format(x));
                for (Points plot : plots) {
                    double y = plot.value(x);
                    values.add(y != 0.0 ? FOUR_DIGIT_FORMAT.format(y) : "");
                }
                writer.write(values);
            }
        } finally {
            if (null != writer) {
                writer.close();
            }
        }
    }

    public static List<PriceRecord> adjustForSplits(List<PriceRecord> records, Map<Day, Double> splits) {
        if (records.size() < 1) {
            return new ArrayList<>();
        }

        List<PriceRecord> adjustedRecords = new ArrayList<>(records.size());
        double totalRatio = 1.0;
        for (PriceRecord record : records) {
            Double currentRatio = splits.get(record.day);
            if (currentRatio != null) {
                totalRatio *= currentRatio;
            }

            PriceRecord adjustedRecord = new PriceRecord(record.day, record.price * totalRatio);
            adjustedRecords.add(adjustedRecord);
        }

        return adjustedRecords;
    }

    public static Double getSafeSpread(Instrument instrument) {
        if (null == instrument) {
            return null;
        }

        Double spread = instrument.getSpread();
        if (null == spread) {
            return null;
        }

        if (spread < Util.ZERO) {
            return null;
        }

        return spread;
    }

    public static <T> T getClosestValue(Collection<T> values, T targetValue, Distance<T> evaluator) {
        if (null == targetValue) {
            return null;
        }

        T closestValue = null;
        Double minDistance = null;

        Iterator<T> it = values.iterator();
        while (it.hasNext()) {
            T value = it.next();
            double distance = evaluator.distance(value, targetValue);
            if ((null == minDistance) || (distance < minDistance)) {
                minDistance = distance;
                closestValue = value;
            }
        }

        return closestValue;
    }

    public static <K, V> V getClosestValue(Map<K, V> map, K targetKey, Distance<K> evaluator) {
        if (null == targetKey) {
            return null;
        }

        V closestValue = null;
        Double minDistance = null;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            double distance = evaluator.distance(entry.getKey(), targetKey);
            if ((null == minDistance) || (distance < minDistance)) {
                minDistance = distance;
                closestValue = entry.getValue();
            }
        }

        return closestValue;
    }

    public static Double extrinsicValue(Instrument option, Double value, Double forward) {
        if ((null == forward) || (null == value)) {
            return null;
        }

        double intrinsicValue = option.isCall() ? forward - option.getStrike() : option.getStrike() - forward;
        if (intrinsicValue < 0.0) {
            return value;
        }

        return value - intrinsicValue;
    }

}
