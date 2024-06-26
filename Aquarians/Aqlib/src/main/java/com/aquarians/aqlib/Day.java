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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Day implements Comparable<Day> {

    public static final String DEFAULT_FORMAT = "yyyy-MMM-dd";
    public static final String US_FORMAT = "MM/dd/yyyy";
    public static final String FORMAT_YYYYMMDD = "yyyyMMdd";
    public static final String FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final long MILLIS_IN_DAY = 24 * 3600 * 1000;

    private final int year;
    private final int month;
    private final int day;

    // Distance between trading days (ex: from friday to monday = 1 day)
    // Indexes as follows:
    // Calendar.MONDAY -> 0
    // Calendar.TUESDAY -> 1
    // Calendar.WEDNESDAY -> 2
    // Calendar.THURSDAY -> 3
    // Calendar.FRIDAY -> 4
    private static final int[][] WEEKDAYS_DISTANCE = {
            {0, 1, 2, 3, 4},
            {4, 0, 1, 2, 3},
            {3, 4, 0, 1, 2},
            {2, 3, 4, 0, 1},
            {1, 2, 3, 4, 0}
    };

    // How many calendar days to add to a weekday
    private static final int[][] CALENDARDAYS_TO_ADD = {
            {0, 1, 2, 3, 4},
            {0, 1, 2, 3, 6},
            {0, 1, 2, 5, 6},
            {0, 1, 4, 5, 6},
            {0, 3, 4, 5, 6}
    };

    public Day(Day copy) {
        this(copy.year, copy.month, copy.day);
    }

    public Day(int year, int month, int day) {
        this.year = year;
        if (month < 1 || month > 12) {
            throw new RuntimeException("Invalid month");
        }
        this.month = month;
        this.day = day;
    }

    public Day(Calendar calendar) {
        this(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));
    }

    public static Day now() {
        return new Day(AqCalendar.now());
    }

    public Day(String text) {
        this(text, DEFAULT_FORMAT);
    }

    public Day(String text, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            sdf.setTimeZone(AqCalendar.UTC_TIMEZONE);
            Date date = sdf.parse(text);
            Calendar calendar = new AqCalendar(date.getTime(), AqCalendar.UTC_TIMEZONE);
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH) + 1;
            day = calendar.get(Calendar.DATE);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public int compareTo(Day that) {
        if (this.year < that.year) {
            return -1;
        }
        if (this.year > that.year) {
            return 1;
        }

        if (this.month < that.month) {
            return -1;
        }
        if (this.month > that.month) {
            return 1;
        }

        if (this.day < that.day) {
            return -1;
        }
        if (this.day > that.day) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Day)) {
            return false;
        }

        Day that = (Day) other;
        return (0 == this.compareTo(that));
    }

    public String toString() {
        Calendar calendar = toCalendar();
        SimpleDateFormat df = new SimpleDateFormat(DEFAULT_FORMAT);
        df.setTimeZone(AqCalendar.UTC_TIMEZONE);
        String text = df.format(calendar.getTime());
        return text;
    }

    public String format(String formatText) {
        Calendar calendar = toCalendar();
        SimpleDateFormat df = new SimpleDateFormat(formatText);
        df.setTimeZone(AqCalendar.UTC_TIMEZONE);
        String text = df.format(calendar.getTime());
        return text;
    }

    public Calendar toCalendar() {
        AqCalendar calendar = new AqCalendar(this, Time.START_OF_DAY, AqCalendar.UTC_TIMEZONE);
        return calendar;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getCalendarMonth() {
        return month - 1;
    }

    public int getDay() {
        return day;
    }

    public int subtract(Day that) {
        return countCalendarDays(that);
    }

    // Counts calendar days from this day to given day
    public int countCalendarDays(Day to) {
        Calendar thisCalendar = this.toCalendar();
        Calendar thatCalendar = to.toCalendar();
        long millis = thatCalendar.getTimeInMillis() - thisCalendar.getTimeInMillis();
        long days = millis / MILLIS_IN_DAY;
        return (int) days;
    }

    // Counts trading days from this day to given day
    public int countTradingDaysSlow(Day that) {
        if (this.compareTo(that) > 0) {
            return -that.countTradingDaysSlow(this);
        }

        int days = 0;
        Day now = new Day(year, month, day);
        while (now.compareTo(that) < 0) {
            if (!now.isWeekend()) {
                days++;
            }
            now = now.next();
        }

        return days;
    }

    public int countTradingDays(Day that) {
        return countTradingDaysFast(that);
        //return countTradingDaysSlow(that);
    }

    public int countTradingDaysFast(Day that) {
        return this.ensureTradingDay().countTradingDaysFastInternal(that.ensureTradingDay());
    }

    public int countTradingDaysFastInternal(Day that) {
        int comparison = that.compareTo(this);
        if (0 == comparison) {
            return 0;
        } else if (comparison < 0) {
            return -that.countTradingDays(this);
        }

        int total = this.countCalendarDays(that);
        int weeks = total / 7;

        int thisIndex = this.getDayOfWeek() - Calendar.MONDAY;
        int thatIndex = that.getDayOfWeek() - Calendar.MONDAY;

        int days = WEEKDAYS_DISTANCE[thisIndex][thatIndex];

        int count = weeks * Util.TRADING_DAYS_IN_WEEK + days;
        return count;
    }

    public Day next() {
        return addCalendarDays(1);
    }

    public Day ensureTradingDay() {
        if (isWeekend()) {
            return nextTradingDay();
        }

        return this;
    }

    public Day nextTradingDay() {
        Day result = next();
        while (result.isWeekend()) {
            result = result.next();
        }
        return result;
    }

    public Day previous() {
        return addCalendarDays(-1);
    }

    public Day previousTradingDay() {
        Day result = previous();
        while (result.isWeekend()) {
            result = result.previous();
        }
        return result;
    }

    public Day previousTradingDays(int days) {
        Day result = new Day(this);
        for (int i = 0; i < days; ++i) {
            result = result.previousTradingDay();
        }
        return result;
    }

    public Day addDays(int days) {
        return addCalendarDays(days);
    }

    public Day addCalendarDays(int days) {
        Calendar calendar = toCalendar();
        calendar.add(Calendar.DATE, days);
        return new Day(calendar);
    }

    public Day addMonths(int months) {
        return add(new Period(months * Util.TRADING_DAYS_IN_MONTH));
    }

    public Day addYears(int years) {
        return add(new Period(years * Util.TRADING_DAYS_IN_YEAR));
    }

    public Day addTradingDaysFast(int days) {
        if (isWeekend()) {
            throw new RuntimeException("Cannot start in a weekend: " + this);
        }

        int thisIndex = getDayOfWeek() - Calendar.MONDAY;

        int weeks = days / Util.TRADING_DAYS_IN_WEEK;
        int thatIndex = days % Util.TRADING_DAYS_IN_WEEK;
        int weekDays = CALENDARDAYS_TO_ADD[thisIndex][thatIndex];
        int calendarDays = weeks * Util.CALENDAR_DAYS_IN_WEEK + weekDays;

        Calendar calendar = toCalendar();
        calendar.add(Calendar.DATE, calendarDays);

        return new Day(calendar);
    }

    public Day addTradingDays(int days) {
        //return addTradingDaysSlow(days);
        return addTradingDaysFast(days);
    }

    public Day addTradingDaysSlow(int days) {
        int sign = (int) Math.signum(days);
        days = Math.abs(days);

        Day day = new Day(this);
        while (day.isWeekend()) {
            day = sign > 0 ? day.next() : day.previous();
        }

        for (int i = 0; i < days; ++i) {
            day = sign > 0 ? day.next() : day.previous();
            while (day.isWeekend()) {
                day = sign > 0 ? day.next() : day.previous();
            }
        }

        return day;
    }

    public Day add(Period period) {
        return addTradingDays(period.getDays());
    }

    public boolean isWeekend() {
        Calendar calendar = toCalendar();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if ((dayOfWeek == Calendar.SATURDAY) || (dayOfWeek == Calendar.SUNDAY)) {
            return true;
        }

        return false;
    }

    public int getDayOfWeek() {
        Calendar calendar = toCalendar();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek;
    }

    public Day rollToTradingDay(boolean isFollowing) {
        Day result = this;
        while (result.isWeekend()) {
            if (isFollowing) {
                result = result.next();
            } else {
                result = result.previous();
            }
        }
        return result;
    }

    public String getDayName() {
        switch (toCalendar().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "MONDAY";

            case Calendar.TUESDAY:
                return "TUESDAY";

            case Calendar.WEDNESDAY:
                return "WEDNESDAY";

            case Calendar.THURSDAY:
                return "THURSDAY";

            case Calendar.FRIDAY:
                return "FRIDAY";

            case Calendar.SATURDAY:
                return "SATURDAY";

            case Calendar.SUNDAY:
                return "SUNDAY";
        }

        throw new RuntimeException("Unknown day name for " + toString());
    }

    public static Day parseDay(String text) {
        return new Day(text);
    }

}
