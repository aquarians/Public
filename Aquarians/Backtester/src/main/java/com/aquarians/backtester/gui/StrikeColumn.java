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
import java.text.DecimalFormat;

public class StrikeColumn extends OptionsTableColumn {

    private static final DecimalFormat FORMAT = new DecimalFormat("###.####");

    private static final String STRIKE = "Strike";
    private static final String PARITY = "Parity";

    public enum Type {
        Strike,
        Parity
    }

    private Type type = Type.Strike;

    public StrikeColumn() {
        super(STRIKE);
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getValue(OptionsTableRow row) {
        if (type.equals(Type.Strike)) {
            return FORMAT.format(row.getStrike());
        }

        if (type.equals(Type.Parity)) {
            return FORMAT.format(row.getParityPrice());
        }

        return "";
    }

    public Color getBackgroundColor(OptionsTableRow row) {
        if (row.isAtm()) {
            return OptionsFrame.SPOT_ROW_BACKGROUND_COLOR;
        }

        if (row.getParityPrice() > 0.0) {
            return OptionsFrame.PRICE_HIGHIGHT_BACKGROUND_COLOR;
        }
        return OptionsFrame.STRIKE_COLUMN_BACKGROUND_COLOR;
    }

    public String getName() {
        switch (type) {
            case Strike:
                return STRIKE;
            case Parity:
                return PARITY;
        }

        return super.getName();
    }

    public void toggleType() {
        switch (type) {
            case Strike:
                type = Type.Parity;
                break;
            case Parity:
                type = Type.Strike;
                break;
        }
    }
}
