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

import java.text.DecimalFormat;

public class Instrument {

    private static final DecimalFormat DOUBLE_DIGIT_FORMAT = new DecimalFormat("###.##");

    public enum Type {
        STOCK,
        OPTION
    }

    private final Type type;
    private final String code;
    private final Boolean call;
    private final Day maturity;
    private final Double strike;

    private Double bidPrice;
    private Double askPrice;

    public Instrument(Type type, String code, Boolean isCall, Day maturity, Double strike) {
        this.type = type;
        this.code = code;
        this.call = isCall;
        this.maturity = maturity;
        this.strike = strike;
    }

    // Just for quick examination in the debugger
    public String toString() {
        String text = "";
        if (type.equals(Type.STOCK)) {
            text += "STK";
        } else if (type.equals(Type.OPTION)) {
            text += "OPT";
        }
        if (code != null) {
            text += " " + code;
        }
        text += " B: " + (bidPrice != null ? DOUBLE_DIGIT_FORMAT.format(bidPrice) : "-");
        text += " A: " + (askPrice != null ? DOUBLE_DIGIT_FORMAT.format(askPrice) : "-");
        return text;
    }

    public Type getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public Boolean isCall() {
        return call;
    }

    public Double getStrike() {
        return strike;
    }

    public Day getMaturity() {
        return maturity;
    }

    public Double getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(Double bidPrice) {
        this.bidPrice = bidPrice;
    }

    public Double getAskPrice() {
        return askPrice;
    }

    public Double getSpread() {
        if ((null == askPrice) || (null == bidPrice)) {
            return null;
        }

        return (askPrice - bidPrice);
    }

    public void setAskPrice(Double askPrice) {
        this.askPrice = askPrice;
    }

    public Double getPrice() {
        if ((null != bidPrice) && (null != askPrice)) {
            return (bidPrice + askPrice) / 2.0;
        } else if (null != bidPrice) {
            return bidPrice;
        } else if (null != askPrice) {
            return askPrice;
        }

        return null;
    }

    public Double getMidPrice() {
        if ((null != bidPrice) && (null != askPrice)) {
            return (bidPrice + askPrice) / 2.0;
        }

        return null;
    }

    public void setPrice(double price) {
        setBidPrice(price);
        setAskPrice(price);
    }
}
