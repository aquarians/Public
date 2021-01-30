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

import com.aquarians.aqlib.Util;

import java.util.Iterator;

/**
 * Intended to iterate between (0, inf).
 * Neither zero nor infinity are actually produced by next(), one can only go close to them.
 */
public class LogarithmicZeroToInfinityIterator implements Iterator<Double> {

    private final double sign;
    private final double logzero;
    private final double loginfinity;
    private final int count;
    private final double step;
    private int position;

    /**
     * Goes from zero to infinity in logarithmic steps.
     * @param zero can be very small but must be greater than zero
     * @param infinity any value greater than start
     * @param count number of iteration steps
     */
    public LogarithmicZeroToInfinityIterator(double zero, double infinity, int count) {
        zero = Math.signum(zero) * Math.max(Math.abs(zero), Util.ZERO); // Limit in absolute value to ZERO
        if (zero * infinity < 0.0) {
            throw new RuntimeException("Zero and infinity limits must have the same sign");
        }
        sign = (zero < 0.0) ? -1.0 : 1.0;
        logzero = Math.log(sign * zero);
        loginfinity = Math.log(sign * infinity);
        this.count = count;
        step = (loginfinity - logzero) / count;
        position = 0;
    }

    @Override
    public boolean hasNext() {
        return position > count ? false : true;
    }

    @Override
    public Double next() {
        double logcurrent = logzero + position * step;
        position++;
        double value = sign * Math.exp(logcurrent);
        return value;
    }

    @Override
    public void remove() {}
}
