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

package com.aquarians.aqlib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DayTest {

    private String buildErrorMessage(Day start, Day end) {
        StringBuilder builder = new StringBuilder();
        builder.append("Start: ").append(start);
        builder.append(", End: ").append(end);
        return builder.toString();
    }

    @Test
    public void testCountCalendarDays() {
        int[] daysToAdd = {0, 1, 2, 3, 4, 5, 6, 7, 10, 14, 20, 60, 90, 365, 550, 730};

//        int[] daysToAdd = new int[Util.TRADING_DAYS_IN_YEAR * 2];
//        for (int i = 0; i < daysToAdd.length; i++) {
//            daysToAdd[i] = 0;
//        }

        for (int i = 0; i < 7; i++) {
            Day start = Day.now().ensureTradingDay().addTradingDays(i);

            for (int days : daysToAdd) {
                Day end = start.addTradingDays(days);

                int count = start.countTradingDays(end);
                assertEquals(buildErrorMessage(start, end) + ": Computed days should equal added days", days, count);

                int reverseCount = end.countTradingDays(start);
                assertEquals(buildErrorMessage(start, end) + ": Reverse count should be negative of count", -count, reverseCount);
            }
        }
    }

    @Test
    public void testAddCalendarDays() {
        int[] daysToAdd = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 20, 60, 90, 365, 550, 730};

        Day st = new Day("2022-Oct-03");
        st.addTradingDays(3);

        for (int i = 0; i < 7; i++) {
            Day start = Day.now().ensureTradingDay().addTradingDaysSlow(i);

            for (int days : daysToAdd) {
                Day expectedEnd = start.addTradingDaysSlow(days);
                Day computedEnd = start.addTradingDaysFast(days);

                assertEquals("Days not equal: expectedEnd=" + expectedEnd + " computedEnd=" + computedEnd +
                        " start=" + start + " days=" + days, expectedEnd, computedEnd);

                int count = start.countTradingDays(expectedEnd);
                assertEquals(buildErrorMessage(start, expectedEnd) + ": Added days should equal computed", days, count);
            }
        }
    }

}