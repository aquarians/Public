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

package com.aquarians.aqlib.positions;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;

import java.text.DecimalFormat;

public class Trade {

    public Long id;
    public Day executionDay;
    public Instrument instrument;
    public double quantity;
    public double price; // market price
    public double tv; // theoretical value
    public Double commission;
    public String label;
    public boolean isStatic = false; // static trades are not delta-hedged

    public Trade(Day executionDay, Instrument instrument, double quantity, double price, double tv) {
        this.executionDay = executionDay;
        this.instrument = instrument;
        this.quantity = quantity;
        this.price = price;
        this.tv = tv;
    }

    public static Trade createTrade(Day executionDay, Instrument instrument, double quantity, double theoreticalValue) {
        if (null == instrument) {
            return null;
        }

        // Buy at ask, sell at bid
        Double marketPrice = (quantity > 0.0) ? instrument.getAskPrice() : instrument.getBidPrice();
        if (null == marketPrice) {
            return null;
        }

        return new Trade(executionDay, instrument, quantity, marketPrice, theoreticalValue);
    }

    public static Trade createTrade(Day executionDay, Instrument instrument, double quantity) {
        if (null == instrument) {
            return null;
        }

        // Buy at ask, sell at bid
        Double marketPrice = (quantity > 0.0) ? instrument.getAskPrice() : instrument.getBidPrice();
        if (null == marketPrice) {
            return null;
        }

        return new Trade(executionDay, instrument, quantity, marketPrice, marketPrice);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(instrument.toString());

        builder.append(" E:");
        builder.append(executionDay);

        builder.append(" Q:");
        builder.append(new DecimalFormat("###.##").format(quantity));

        builder.append(" P:");
        builder.append(new DecimalFormat("###.##").format(price));

        if (instrument.getType().equals(Instrument.Type.OPTION)) {
            builder.append(" V:");
            builder.append(new DecimalFormat("###.##").format(tv));
        }

        return builder.toString();
    }

    public double profit() {
        double cost = quantity * (price - tv);
        return -cost;
    }

}
