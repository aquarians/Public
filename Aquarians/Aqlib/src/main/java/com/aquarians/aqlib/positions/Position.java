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

import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;

public class Position {

    public final Instrument instrument;

    private double totalQuantity;
    private double totalCost;

    public Position() {
        this.instrument = null;
    }

    public Position(Instrument instrument) {
        this.instrument = instrument;
    }

    public Position(Position copy) {
        this.instrument = copy.instrument;
        this.totalQuantity = copy.totalQuantity;
        this.totalCost = copy.totalCost;
    }

    public void reset() {
        totalQuantity = 0.0;
        totalCost = 0.0;
    }

    public void add(double quantity, double price) {
        double cost = quantity * price;
        totalQuantity += quantity;
        totalCost += cost;
    }

    public Double close(Double bid, Double ask) {
        double quantity = -totalQuantity;
        Double price = quantity > 0 ? ask : bid;
        if (null == price) {
            return null;
        }

        close(price);
        return profit();
    }

    public Double evaluateClose(Double price) {
        return evaluateClose(price, price);
    }

    public Double evaluateClose(Double bid, Double ask) {
        Position copy = new Position(this);
        return copy.close(bid, ask);
    }

    public double close(double price) {
        add(-totalQuantity, price);
        return profit();
    }

    public double getTotalQuantity() {
        return totalQuantity;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double profit() {
        return -totalCost;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (instrument != null) {
            builder.append(instrument.getCode());
            builder.append(" : ");
        }
        builder.append("Q=");
        builder.append(Util.DOUBLE_DIGIT_FORMAT.format(totalQuantity));
        builder.append(" P=");
        builder.append(Util.DOUBLE_DIGIT_FORMAT.format(totalCost));

        return builder.toString();
    }
}
