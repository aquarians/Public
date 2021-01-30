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

import com.aquarians.aqlib.math.Function;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Util {

    public static final double MINIMUM_PRICE = 0.01;
    public static final double ZERO = 1e-12;
    public static final double ONE = 1.0 - 1e-12;
    public static final double INFINITY = 1.0 / ZERO;
    public static final int TRADING_DAYS_IN_YEAR = 252;
    public static final int TRADING_DAYS_IN_WEEK = 5;
    public static final int TRADING_DAYS_IN_MONTH = 20;
    public static final int DEFAULT_HEDGE_FREQUENCY = 1;
    public static final int HEDGING_DISABLED = 0;

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

    public static Double maxRatio(Double first, Double second) {
        if ((null == first) || (null == second)) {
            return null;
        }

        first = Math.abs(first);
        second = Math.abs(second);

        double factor = Math.max(1.0 / first, 1.0 / second);
        if (factor > 1.0) {
            first *= factor;
            second *= factor;
        }

        double min = Math.min(first, second);
        double max = Math.max(first, second);

        double ret = max / Math.max(min, Util.ZERO);
        return ret;
    }

    public static List<String> toString(List objects) {
        List<String> res = new ArrayList<String>(objects.size());
        for (Object obj : objects) {
            res.add(obj.toString());
        }
        return res;
    }

    // S <= U
    public static Double getParitySpotUpperBound(Instrument call, Instrument put) {
        if ((null == call) || (null == put)) {
            return null;
        }

        if (!doubleEquals(call.getStrike(), put.getStrike())) {
            return null;
        }

        Double cp = call.getAskPrice();
        Double pp = put.getBidPrice();
        if ((null == cp) || (null == pp)) {
            return null;
        }

        double u = call.getStrike() + cp - pp;
        return u;
    }

    // S >= L
    public static Double getParitySpotLowerBound(Instrument call, Instrument put) {
        if ((null == call) || (null == put)) {
            return null;
        }

        if (!doubleEquals(call.getStrike(), put.getStrike())) {
            return null;
        }

        Double cp = call.getBidPrice();
        Double pp = put.getAskPrice();
        if ((null == cp) || (null == pp)) {
            return null;
        }

        double u = call.getStrike() + cp - pp;
        return u;
    }

}
