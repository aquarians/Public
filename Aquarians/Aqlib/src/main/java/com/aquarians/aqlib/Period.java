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

    final Pattern PATTERN = Pattern.compile("([0-9]+)([YMD])");

    public static final Period ONE_DAY = new Period(0, 0, 1);
    public static final Period ONE_MONTH = new Period(0, 1, 0);
    public static final Period ONE_YEAR = new Period(1, 0, 0);

    private final int years;
    private final int months;
    private final int days;

    public Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public Period(String text) {
        int years = 0;
        int months = 0;
        int days = 0;
        text = text.toUpperCase();
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            String countText = matcher.group(1);
            String unitText = matcher.group(2);

            int count = Integer.parseInt(countText);
            if (unitText.equals("Y")) {
                years = count;
            } else if (unitText.equals("M")) {
                months = count;
            } else if (unitText.equals("D")) {
                days = count;
            } else {
                throw new RuntimeException("Unknown unit: " + unitText);
            }
        }

        this.years = years;
        this.months = months;
        this.days = days;
    }

    public Period negative() {
        return new Period(-years, -months, -days);
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

}
