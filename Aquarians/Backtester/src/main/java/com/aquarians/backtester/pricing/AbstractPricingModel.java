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
import com.aquarians.aqlib.models.PricingResult;
import com.aquarians.aqlib.models.VolatilitySurface;

public abstract class AbstractPricingModel implements PricingModel {

    protected double interestRate = 0.0;
    protected double dividendYield = 0.0;

    protected Double spot;

    protected Day today;

    @Override
    public void fit() {
    }

    public PricingResult price(Instrument instrument) {
        if (instrument.getType().equals(Instrument.Type.STOCK)) {
            PricingResult result = new PricingResult(spot, 1.0);
            result.pnlDev = 0.0;
            result.day = today;
            return result;
        }

        throw new RuntimeException("Unknown instrument type: " + instrument.getType().name());
    }

    @Override
    public VolatilitySurface getSurface() {
        return null;
    }

    public Double getSpot() {
        return spot;
    }

    public Day getToday() {
        return today;
    }

    @Override
    public Double getForward(Day maturity) {
        return null;
    }

    public Double getVolatility() {
        return null;
    }

    public void setSpot(Double spot) {
        this.spot = spot;
    }

    public void setToday(Day today) {
        this.today = today;
    }

}
