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

public class BidColumn extends OptionsTableColumn {

    private final boolean isCall;

    public BidColumn(boolean isCall) {
        super((isCall ? "Call" : "Put") + " Bid");
        this.isCall = isCall;
    }

    public Object getValue(OptionsTableRow row) {
        Double price = isCall ? row.getCallBid() : row.getPutBid();
        if (null == price) {
            return null;
        }

        return OptionsFrame.PRICE_FORMAT.format(price);
    }

    public Color getBackgroundColor(OptionsTableRow row) {
        Double price = isCall ? row.getCallBid() : row.getPutBid();
        if (null == price) {
            // Show default color
            return super.getBackgroundColor(row);
        }

        Double value = isCall ? row.getCallValue() : row.getPutValue();
        if (null == value) {
            // Show default color
            return super.getBackgroundColor(row);
        }

        // Check if there's an arbitrage opportunity
        if (price > value) {
            return OptionsFrame.PRICE_HIGHIGHT_BACKGROUND_COLOR;
        }

        // Show default color
        return super.getBackgroundColor(row);
    }

}
