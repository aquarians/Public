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

import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.Function;
import org.apache.commons.math3.distribution.NormalDistribution;

public class BlackScholes {

    public static final double MIN_VOL = 0.1 / 100.0; // 0.1%
    public static final double MAX_VOL = 1000.0 / 100.0; // 1000%
    public static final int VOL_STEPS = Util.getBinarySearchSteps(MIN_VOL, MAX_VOL, Util.ZERO);

    public static final double MIN_INTEREST = -10.0;
    public static final double MAX_INTEREST = 10.0;

    private final NormalDistribution ndist = new NormalDistribution(0.0, 1.0);

    protected boolean isCall;
    protected double spotPrice;
    protected double strikePrice;
    protected double timeToExpiration;
    protected double interestRate;
    protected double dividendYield;
    protected double volatility;

    protected boolean isBlack = false;

    public BlackScholes(
            boolean isCall,
            double spotPrice,
            double strikePrice,
            double timeToExpiration,
            double interestRate,
            double dividendYield,
            double volatility) {
        this.isCall = isCall;
        this.spotPrice = spotPrice;
        this.strikePrice = strikePrice;
        this.timeToExpiration = timeToExpiration;
        this.interestRate = interestRate;
        this.dividendYield = dividendYield;
        this.volatility = volatility;
    }

    public void setBlack(boolean black) {
        isBlack = black;
    }

    public static BlackScholes copy(BlackScholes original) {
        return new BlackScholes(original.isCall,
                original.spotPrice,
                original.strikePrice,
                original.timeToExpiration,
                original.interestRate,
                original.dividendYield,
                original.volatility);
    }

    // When You Cannot Hedge Continuously - Emanuel Derman
    // Standard deviation of the PNL when doing N rebalances until expiry
    public double theoreticalPnlDev(int n) {
        double k = analyticVega() * 100.0;
        double dev = Math.sqrt(Math.PI * 0.25) * k * volatility / Math.sqrt(n);
        return dev;
    }

    public double forward(double probability) {
        double mean = (interestRate - dividendYield - volatility * volatility * 0.5) * timeToExpiration;
        double dev = volatility * Math.sqrt(timeToExpiration);
        NormalDistribution dist = new NormalDistribution(mean, dev);
        probability = Util.limitProbability(probability);
        double x = dist.inverseCumulativeProbability(probability);
        double fwd = spotPrice * Math.exp(x);
        return fwd;
    }

    /**
     * Computes option value at expiration given spot price at expiration
     * @return option value
     */
    public double valueAtExpiration() {
        double sign = isCall ? 1.0 : -1.0;
        double value = Math.max(sign * (spotPrice - strikePrice), 0.0);
        return value;
    }

    public double price() {
        double s = spotPrice;
        double x = strikePrice;
        double t = timeToExpiration;
        double r = interestRate;
        double q = dividendYield;
        double v = volatility;
        double vsqrt = v * Math.sqrt(t);
        double discount = Math.exp(-r * t);
        double f = isBlack ? s : s * Math.exp((r - q) * t);

        double value = 0.0;
        if (vsqrt < Util.ZERO) {
            // Intrisic value
            value = valueAtExpiration();
        } else {
            double d1 = (Math.log(f / x) + (0.5 * v * v) * t) / vsqrt;
            double d2 = d1 - vsqrt;
            double sign = isCall ? 1.0 : -1.0;
            double nd1 = ndist.cumulativeProbability(sign * d1);
            double nd2 = ndist.cumulativeProbability(sign * d2);
            value = sign * discount * (f * nd1 - x * nd2);
        }

        return value;
    }

    public Double impliedVolatility(double price) {
        final BlackScholes copy = BlackScholes.copy(this);
        Function priceFunction = new Function() {
            @Override
            public double value(double volatility) {
                copy.volatility = volatility;
                return copy.price();
            }
        };

        double volatility = Util.binarySearch(priceFunction, price, MIN_VOL, MAX_VOL, VOL_STEPS, Util.ZERO);
        if ((volatility < MIN_VOL + Util.ZERO) || (volatility > MAX_VOL - Util.ZERO)) {
            return null;
        }

        return volatility;
    }

    public Double impliedInterestRate(double price) {
        final BlackScholes copy = BlackScholes.copy(this);
        Function priceFunction = new Function() {
            @Override
            public double value(double interestRate) {
                copy.interestRate = interestRate;
                return copy.price();
            }
        };

        double interest = Util.binarySearch(priceFunction, price, MIN_INTEREST, MAX_INTEREST, 60, Util.ZERO);
        if ((interest < MIN_INTEREST + Util.ZERO) || (interest > MAX_INTEREST - Util.ZERO)) {
            return null;
        }

        return interest;
    }

    public double impliedSpot(double price) {
        final BlackScholes copy = BlackScholes.copy(this);
        Function priceFunction = new Function() {
            @Override
            public double value(double spot) {
                copy.spotPrice = spot;
                return copy.price();
            }
        };

        double spotPrice = Util.binarySearch(priceFunction, price, strikePrice / 10.0, strikePrice * 10.0, VOL_STEPS, Util.ZERO);
        return spotPrice;
    }

    public double impliedStrike(double price) {
        final BlackScholes copy = BlackScholes.copy(this);
        Function priceFunction = new Function() {
            @Override
            public double value(double strike) {
                copy.strikePrice = strike;
                return copy.price();
            }
        };

        double strikePrice = Util.binarySearch(priceFunction, price, spotPrice * 0.1, spotPrice * 3.0, VOL_STEPS, Util.ZERO);
        return strikePrice;
    }

    public double impliedStrikeFromDelta(double delta) {
        final BlackScholes copy = BlackScholes.copy(this);
        Function priceFunction = new Function() {
            @Override
            public double value(double strike) {
                copy.strikePrice = strike;
                return copy.analyticDelta();
            }
        };

        double strikePrice = Util.binarySearch(priceFunction, delta, spotPrice * 0.9, spotPrice * 1.1, VOL_STEPS, Util.ZERO);
        return strikePrice;
    }

    public double analyticDelta() {
        if (timeToExpiration < Util.ZERO) {
            if (isCall) {
                return spotPrice <= strikePrice ? 0.0 : 1.0;
            }

            return spotPrice >= strikePrice ? 0.0 : -1.0;
        }

        double s = spotPrice;
        double x = strikePrice;
        double t = timeToExpiration;
        double r = interestRate;
        double q = dividendYield;
        double v = volatility;
        double vsqrt = v * Math.sqrt(t);
        double d1 = (Math.log(s / x) + (r - q + 0.5 * v * v) * t) / vsqrt;
        double sign = isCall ? +1.0 : -1.0;
        double value = sign * Math.exp(-dividendYield * timeToExpiration) * ndist.cumulativeProbability(sign * d1);
        return value;
    }

    public double analyticTheta() {
        double s = spotPrice;
        double x = strikePrice;
        double t = timeToExpiration;
        double r = interestRate;
        double q = dividendYield;
        double v = volatility;
        double vsqrt = v * Math.sqrt(t);
        double d1 = (Math.log(s / x) + (r - q + 0.5 * v * v) * t) / vsqrt;
        double d2 = d1 - vsqrt;

        double value = 0.0;
        if (isCall) {
            value = -Math.exp(-q * t) * s * ndist.density(d1) * v / (2.0 * Math.sqrt(t)) -
                    r * x * Math.exp(-r * t) * ndist.cumulativeProbability(d2) +
                    q * s * Math.exp(-q * t) * ndist.cumulativeProbability(d1);
        } else {
            value = -Math.exp(-q * t) * s * ndist.density(d1) * v / (2.0 * Math.sqrt(t)) +
                    r * x * Math.exp(-r * t) * ndist.cumulativeProbability(-d2) -
                    q * s * Math.exp(-q * t) * ndist.cumulativeProbability(-d1);

        }

        return value;
    }

    public double analyticGamma() {
        double s = spotPrice;
        double x = strikePrice;
        double t = timeToExpiration;
        double r = interestRate;
        double q = dividendYield;
        double v = volatility;
        double vsqrt = v * Math.sqrt(t);
        vsqrt = Math.max(vsqrt, Util.ZERO);
        double d1 = (Math.log(s / x) + (r - q + 0.5 * v * v) * t) / vsqrt;
        double value = Math.exp(-dividendYield * timeToExpiration) * ndist.density(d1) / (s * vsqrt);
        return value;
    }

    public double analyticSpeed() {
        double s = spotPrice;
        double x = strikePrice;
        double t = timeToExpiration;
        double r = interestRate;
        double q = dividendYield;
        double v = volatility;
        double vsqrt = v * Math.sqrt(t);
        vsqrt = Math.max(vsqrt, Util.ZERO);
        double d1 = (Math.log(s / x) + (r - q + 0.5 * v * v) * t) / vsqrt;
        double value = -(analyticGamma() / spotPrice) * (d1 / vsqrt + 1);
        return value;
    }

    public double analyticVega() {
        double s = spotPrice;
        double x = strikePrice;
        double t = timeToExpiration;
        double r = interestRate;
        double q = dividendYield;
        double v = volatility;
        double vsqrt = v * Math.sqrt(t);
        vsqrt = Math.max(vsqrt, Util.ZERO);
        double d1 = (Math.log(s / x) + (r - q + 0.5 * v * v) * t) / vsqrt;
        double value = s * Math.exp(-q * t) * ndist.density(d1) * Math.sqrt(t);
        value /= 100.0;
        return value;
    }

    public double theta() {
        BlackScholes next = BlackScholes.copy(this);
        next.timeToExpiration = Util.yearFraction((int)timeToExpiration - 1);

        double thisPrice = this.price();
        double nextPrice = next.price();

        double dp = nextPrice - thisPrice;
        return dp;
    }

    public double numericVega() {
        BlackScholes next = BlackScholes.copy(this);
        next.volatility = volatility + 0.01;

        double thisPrice = this.price();
        double nextPrice = next.price();

        double dp = nextPrice - thisPrice;
        return dp;
    }

    public double gamma() {
        return analyticGamma();
    }

    public double delta() {
        return analyticDelta();
    }

    public static boolean isValidVol(Double vol) {
        if (null == vol) {
            return false;
        }

        if (vol < MIN_VOL + Util.ZERO) {
            return false;
        }

        if (vol > MAX_VOL - Util.ZERO) {
            return false;
        }

        return true;
    }

    public double getStrikePrice() {
        return strikePrice;
    }

    public static double limitVol(double vol) {
        if (vol < MIN_VOL) {
            return MIN_VOL;
        }

        if (vol > MAX_VOL) {
            return MAX_VOL;
        }

        return vol;
    }
}
