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

public class ValueColumn extends OptionsTableColumn {

    private final boolean isCall;

    public enum Type {
        WholeValue,
        ExtrinsicValue
    }

    private Type type = Type.WholeValue;

    public ValueColumn(boolean isCall) {
        super(getColumnName(isCall, Type.WholeValue));
        this.isCall = isCall;
    }

    private static String getColumnName(Boolean isCall, Type type) {
        if (type.equals(Type.WholeValue)) {
            return (isCall ? "Call" : "Put") + " Value";
        } else if (type.equals(Type.ExtrinsicValue)) {
            return (isCall ? "Call" : "Put") + " Ext";
        }

        return null;
    }

    public Object getValue(OptionsTableRow row) {
        if (type.equals(Type.WholeValue)) {
            Double value = isCall ? row.getCallValue() : row.getPutValue();
            if (null == value) {
                return null;
            }

            return OptionsFrame.PRICE_FORMAT.format(value);
        }

        if (type.equals(Type.ExtrinsicValue)) {
            Double value = isCall ? row.getCallExtrinsicValue() : row.getPutExtrinsicValue();
            if (null == value) {
                return null;
            }

            return OptionsFrame.PRICE_FORMAT.format(value);
        }

        return null;
    }

    public Color getBackgroundColor(OptionsTableRow row) {
        return OptionsFrame.VALUE_COLUMN_BACKGROUND_COLOR;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return getColumnName(isCall, type);
    }

    public void toggleType() {
        switch (type) {
            case WholeValue:
                type = Type.ExtrinsicValue;
                break;
            case ExtrinsicValue:
                type = Type.WholeValue;
                break;
        }
    }
}
