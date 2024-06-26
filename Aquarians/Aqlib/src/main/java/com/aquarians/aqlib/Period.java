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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Period {

    final Pattern PATTERN = Pattern.compile("([0-9]+)([YMWD])");

    private final int days;

    public Period(int days) {
        this.days = days;
    }

    public Period(String text) {
        text = text.toUpperCase();
        Matcher matcher = PATTERN.matcher(text);
        int days = 0;
        while (matcher.find()) {
            String countText = matcher.group(1);
            String unitText = matcher.group(2);

            int count = Integer.parseInt(countText);
            if (unitText.equals("Y")) {
                days = count * Util.TRADING_DAYS_IN_YEAR;
            } else if (unitText.equals("M")) {
                days = count * Util.TRADING_DAYS_IN_MONTH;
            } else if (unitText.equals("W")) {
                days = count * Util.TRADING_DAYS_IN_WEEK;
            } else if (unitText.equals("D")) {
                days = count;
            } else {
                throw new RuntimeException("Unknown unit: " + unitText);
            }
        }

        this.days = days;
    }

    @Override
    public String toString() {
        return Integer.toString(days) + "D";
    }

    public int getDays() {
        return days;
    }

}
