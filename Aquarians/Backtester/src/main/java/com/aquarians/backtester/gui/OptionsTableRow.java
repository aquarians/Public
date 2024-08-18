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
    // Amount of PNL if an arbitrage opportunity exists
    private final double callBidPnl;
    private final double callAskPnl;
    private final double putBidPnl;
    private final double putAskPnl;
    private final Double callExtrinsicValue;
    private final Double callExtrinsicBid;
    private final Double callExtrinsicAsk;
    private final Double putExtrinsicValue;
    private final Double putExtrinsicBid;
    private final Double putExtrinsicAsk;

    public OptionsTableRow(Double strike, Double callValue, Double callBid, Double callAsk, Double putValue,
                           Double putBid, Double putAsk, Color backgroundColor, boolean atm, double parityPrice,
                           double callBidPnl, double callAskPnl, double putBidPnl, double putAskPnl,
                           Double callExtrinsicValue, Double callExtrinsicBid, Double callExtrinsicAsk,
                           Double putExtrinsicValue, Double putExtrinsicBid, Double putExtrinsicAsk) {
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
        this.callBidPnl = callBidPnl;
        this.callAskPnl = callAskPnl;
        this.putBidPnl = putBidPnl;
        this.putAskPnl = putAskPnl;
        this.callExtrinsicValue = callExtrinsicValue;
        this.callExtrinsicBid = callExtrinsicBid;
        this.callExtrinsicAsk = callExtrinsicAsk;
        this.putExtrinsicValue = putExtrinsicValue;
        this.putExtrinsicBid = putExtrinsicBid;
        this.putExtrinsicAsk = putExtrinsicAsk;
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

    public double getCallBidPnl() {
        return callBidPnl;
    }

    public double getCallAskPnl() {
        return callAskPnl;
    }

    public double getPutBidPnl() {
        return putBidPnl;
    }

    public double getPutAskPnl() {
        return putAskPnl;
    }

    public Double getCallExtrinsicValue() {
        return callExtrinsicValue;
    }

    public Double getPutExtrinsicValue() {
        return putExtrinsicValue;
    }

    public Double getCallExtrinsicBid() {
        return callExtrinsicBid;
    }

    public Double getCallExtrinsicAsk() {
        return callExtrinsicAsk;
    }

    public Double getPutExtrinsicBid() {
        return putExtrinsicBid;
    }

    public Double getPutExtrinsicAsk() {
        return putExtrinsicAsk;
    }
}
