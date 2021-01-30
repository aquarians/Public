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

public class Time implements Comparable<Time> {

    public static final String DEFAULT_FORMAT = "HH:mm:ss.SSS";

    public static final int MILLISECONDS_PER_HOUR = 3600 * 1000;
    public static final int MILLISECONDS_PER_DAY = 24 * MILLISECONDS_PER_HOUR;

    public static final Time START_OF_DAY = new Time(0, 0, 0, 0);
    public static final Time MID_DAY = new Time(12, 0, 0, 0);
    public static final Time END_OF_DAY = new Time(23, 59, 59, 999);

    private final int hour;
    private final int minute;
    private final int second;
    private final int millisecond;

    public Time(int hour, int minute, int second, int millisecond) {
        this(hour, minute, second, millisecond, true);
    }

    private Time(int hour, int minute, int second, int millisecond, boolean validate) {
        if (validate) {
            if ((hour < 0) || (hour > 23)) {
                throw new RuntimeException("Invalid hour:" + hour);
            }
            if ((minute < 0) || (minute > 59)) {
                throw new RuntimeException("Invalid minute:" + minute);
            }
            if ((second < 0) || (second > 59)) {
                throw new RuntimeException("Invalid second:" + second);
            }
            if ((millisecond < 0) || (millisecond > 999)) {
                throw new RuntimeException("Invalid millisecond:" + millisecond);
            }
        }

        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
    }

    public Time(String text) {
        this(text, DEFAULT_FORMAT);
    }

    public Time(String text, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            sdf.setTimeZone(AqCalendar.UTC_TIMEZONE);
            Date date = sdf.parse(text);
            Calendar calendar = new AqCalendar(date.getTime(), AqCalendar.UTC_TIMEZONE);
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);
            second = calendar.get(Calendar.SECOND);
            millisecond = calendar.get(Calendar.MILLISECOND);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public Time(Calendar calendar) {
        this(calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND));
    }

    public String toString() {
        String text = String.format("%02d", hour) + ":" +
                String.format("%02d", minute) + ":" +
                String.format("%02d", second) + "." +
                String.format("%03d", millisecond);
        return text;
    }

    @Override
    public int compareTo(Time that) {
        Integer thisMillis = this.millis();
        Integer thatMillis = that.millis();
        return thisMillis.compareTo(thatMillis);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Time)) {
            return false;
        }

        Time that = (Time) other;
        return (this.compareTo(that) == 0);
    }

    public int millis() {
        return (hour * 3600 + minute * 60 + second) * 1000 + millisecond;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getMillisecond() {
        return millisecond;
    }

    public Time add(Time duration) {
        Calendar calendar = Calendar.getInstance(AqCalendar.UTC_TIMEZONE);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);

        calendar.add(Calendar.MILLISECOND, duration.millis());

        return new Time(calendar);
    }

    public static Time now() {
        return new Time(AqCalendar.now());
    }
}
