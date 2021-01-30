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
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.models.PricingResult;

// No valuation, just market prices
public class MarketPricingModel extends AbstractPricingModel {

    private final PricingModule owner;

    private Day today;
    private Double spot;

    public MarketPricingModel(PricingModule owner) {
        this.owner = owner;
    }

    public Type getType() {
        return Type.Market;
    }

    public void fit() {
        today = owner.getToday();
        spot = owner.getSpotPrice();
    }

    public Day getToday() {
        return today;
    }

    public Double getSpot() {
        return spot;
    }

    @Override
    public PricingResult price(Instrument instrument) {
        if (null == instrument) {
            return null;
        }

        Double bid = instrument.getBidPrice();
        Double ask = instrument.getAskPrice();
        if ((null != bid) && (null != ask)) {
            return new PricingResult((bid + ask) / 2.0, null);
        } else if (null != bid) {
            return new PricingResult(bid + Util.MINIMUM_PRICE * 0.5, null);
        } else if (null != ask) {
            return new PricingResult(ask - Util.MINIMUM_PRICE * 0.5, null);
        }

        return null;
    }

}
