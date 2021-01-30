/*
    MIT License

    Copyright (c) 2017 Mihai Bunea

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

/**
 * Market data quote (bid, ask, etc).
 */
public class QuoteData {

    private final Double[] values = new Double[QuoteField.FIELDS_COUNT];

    public QuoteData(Double[] values) {
        for (int i = 0; i < values.length; i++) {
            this.values[i] = limitPrice(values[i]);
        }
    }

    private static Double limitPrice(Double price) {
        if (null == price) {
            return null;
        }

        if (price < 0.01) {
            return null;
        }

        return price;
    }

    public Double getBid() {
        return getValue(QuoteField.BID_PRICE);
    }

    public Double getAsk() {
        return getValue(QuoteField.ASK_PRICE);
    }

    public Double getValue(QuoteField field) {
        Double value = values[field.getIndex()];
        return value;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (QuoteField field : QuoteField.values()) {
            Double value = getValue(field);
            if (null == value) {
                continue;
            }

            count++;
            if (count > 1) {
                builder.append(", ");
            }
            builder.append(field.getName() + ":" + value);
        }

        if (0 == count) {
            return null;
        }

        return builder.toString();
    }

    public Double getMidPrice() {
        Double bid = getValue(QuoteField.BID_PRICE);
        if (null == bid) {
            return null;
        }

        Double ask = getValue(QuoteField.ASK_PRICE);
        if (null == ask) {
            return null;
        }

        return (bid + ask) / 2.0;
    }

    public Double getPrice() {
        Double mid = getMidPrice();
        if (null != mid) {
            return mid;
        }

        return getValue(QuoteField.LAST_TRADE_PRICE);
    }

}
