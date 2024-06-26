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

package com.aquarians.aqlib.models;

import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.LinearIterator;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Iterator;

public class MonteCarloPricer {

    private static final int SAMPLES = 10000;

    private final NormalProcess process;
    private final  boolean isCall;
    private final  double spotPrice;
    private final  double strikePrice;
    private final  double timeToExpiration;
    private int samples = SAMPLES;

    public MonteCarloPricer(NormalProcess process, boolean isCall, double spotPrice, double strikePrice, double timeToExpiration) {
        this.process = process;
        this.isCall = isCall;
        this.spotPrice = spotPrice;
        this.strikePrice = strikePrice;
        this.timeToExpiration = timeToExpiration;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    private double price(double spot) {
        NormalDistribution dist = process.getDistribution(timeToExpiration);

        double total = 0.0;
        Iterator<Double> it = new LinearIterator(0.0, 1.0, samples);
        while (it.hasNext()) {
            double p = Util.limitProbability(it.next());
            double x = dist.inverseCumulativeProbability(p);
            double forward = spot * Math.exp(x);
            double value = valueAtExpiration(forward);
            total += value;
        }

        double average = total / (samples + 1);
        return average;
    }

    private double valueAtExpiration(double forward) {
        double sign = isCall ? 1.0 : -1.0;
        double value = Math.max(sign * (forward - strikePrice), 0.0);
        return value;
    }

    public double price() {
        return price(spotPrice);
    }

    public double delta() {
        double p0 = price(spotPrice);
        double h = spotPrice * 0.01;
        double p1 = price(spotPrice + h);
        double derivative = (p1 - p0) / h;
        return derivative;
    }

    public double valueAtExpiration() {
        double sign = isCall ? 1.0 : -1.0;
        double value = Math.max(sign * (spotPrice - strikePrice), 0.0);
        return value;
    }
}
