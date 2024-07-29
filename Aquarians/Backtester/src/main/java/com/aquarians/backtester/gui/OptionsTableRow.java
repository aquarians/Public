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

package com.aquarians.backtester.gui;

import java.awt.*;

public class OptionsTableRow {

    private final Double strike;
    private final Double callValue;
    private final Double callBid;
    private final Double callAsk;
    private final Double putValue;
    private final Double putBid;
    private final Double putAsk;
    private final Color backgroundColor;
    private final boolean atm;
    private final double parityPrice;

    public OptionsTableRow(Double strike, Double callValue, Double callBid, Double callAsk, Double putValue,
                           Double putBid, Double putAsk, Color backgroundColor, boolean atm, double parityPrice) {
        this.strike = strike;
        this.callValue = callValue;
        this.callBid = callBid;
        this.callAsk = callAsk;
        this.putValue = putValue;
        this.putBid = putBid;
        this.putAsk = putAsk;
        this.backgroundColor = backgroundColor;
        this.atm = atm;
        this.parityPrice = parityPrice;
    }

    public Double getStrike() {
        return strike;
    }

    public Double getCallValue() {
        return callValue;
    }

    public Double getCallBid() {
        return callBid;
    }

    public Double getCallAsk() {
        return callAsk;
    }

    public Double getPutValue() {
        return putValue;
    }

    public Double getPutBid() {
        return putBid;
    }

    public Double getPutAsk() {
        return putAsk;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public boolean isAtm() {
        return atm;
    }

    public double getParityPrice() {
        return parityPrice;
    }
}
