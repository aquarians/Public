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
import java.util.*;

public class AqCalendar extends GregorianCalendar {

    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS zzz";

    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");
    public static final Calendar UTC_CALENDAR = now();

    public AqCalendar(long timestamp, TimeZone timeZone) {
        super(timeZone, Locale.getDefault());
        setTimeInMillis(timestamp);
    }

    public AqCalendar(long timestamp) {
        super(UTC_TIMEZONE, Locale.getDefault());
        setTimeInMillis(timestamp);
    }

    public AqCalendar(String text) {
        this(text, UTC_TIMEZONE);
    }

    public AqCalendar(String text, TimeZone timeZone) {
        super(timeZone, Locale.getDefault());
        SimpleDateFormat df = new SimpleDateFormat(DEFAULT_FORMAT);
        df.setTimeZone(timeZone);
        try {
            Date timestamp = df.parse(text);
            setTimeInMillis(timestamp.getTime());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public AqCalendar(Day day) {
        this(day, new Time(0, 0, 0, 0), UTC_TIMEZONE);
    }

    public AqCalendar(Calendar calendar) {
        this(calendar.getTime().getTime(), calendar.getTimeZone());
    }

    public AqCalendar(Day day, Time time) {
        this(day, time, UTC_TIMEZONE);
    }

    public AqCalendar(Day day, Time time, TimeZone timeZone) {
        super(timeZone, Locale.getDefault());
        set(Calendar.YEAR, day.getYear());
        set(Calendar.MONTH, day.getMonth() - 1);
        set(Calendar.DATE, day.getDay());
        set(Calendar.HOUR_OF_DAY, time.getHour());
        set(Calendar.MINUTE, time.getMinute());
        set(Calendar.SECOND, time.getSecond());
        set(Calendar.MILLISECOND, time.getMillisecond());
    }

    public String format(String formatText) {
        SimpleDateFormat df = new SimpleDateFormat(formatText);
        if (null != getTimeZone()) {
            df.setTimeZone(getTimeZone());
        }
        return df.format(getTime());
    }

    public String toString() {
        return format(DEFAULT_FORMAT);
    }

    public static AqCalendar parseCalendar(String text, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date;
        try {
            date = df.parse(text);
        } catch (Exception ex) {
            return null;
        }

        AqCalendar calendar = new AqCalendar(date.getTime(), UTC_TIMEZONE);
        return calendar;
    }

    public AqCalendar add(Period period) {
        Calendar output = (Calendar) clone();
        output.add(Calendar.YEAR, period.getYears());
        output.add(Calendar.MONTH, period.getMonths());
        output.add(Calendar.DATE, period.getDays());
        return new AqCalendar(output);
    }

    public static Calendar addHours(Calendar input, int hours) {
        Calendar output = (Calendar) input.clone();
        output.add(Calendar.HOUR_OF_DAY, hours);
        return new AqCalendar(output);
    }

    public static final AqCalendar now() {
        Calendar tm = Calendar.getInstance(UTC_TIMEZONE);
        return new AqCalendar(tm);
    }
}
