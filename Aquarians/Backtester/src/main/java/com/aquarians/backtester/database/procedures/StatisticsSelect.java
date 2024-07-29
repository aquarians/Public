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

package com.aquarians.backtester.database.procedures;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StatisticsSelect extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT day, spot_fwd_diff, parity_total, arbitrage_total " +
            "FROM statistics " +
            "WHERE underlier = ? AND day >= ? AND day <= ?";

    public static final class Record {
        final Day day;
        final Double spot_fwd_diff;
        final Double parity_total;
        final Double option_total;

        public Record(Day day, Double spotFwdDiff, Double parityTotal, Double optionTotal) {
            this.day = day;
            spot_fwd_diff = spotFwdDiff;
            parity_total = parityTotal;
            option_total = optionTotal;
        }
    }

    // Input
    private Long underlier;
    private Day from;
    private Day to;

    // Output
    private List<Record> records;

    public StatisticsSelect(Connection connection) {
        super(connection);
    }

    @Override
    protected String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    public void setParameters() throws Exception {
        setLong(1, underlier);
        setDay(2, from);
        setDay(3, to);
    }

    @Override
    public void process(ResultSet results) throws Exception {
        records = new ArrayList<>();
        while (results.next()) {
            Day day = getDay(results, 1);
            Double spot_fwd_diff = getDouble(results, 2);
            Double parity_total = getDouble(results, 3);
            Double option_total = getDouble(results, 4);
            records.add(new Record(day, spot_fwd_diff, parity_total, option_total));
        }
    }

    public List<Record> execute(Long underlier, Day from, Day to) {
        this.underlier = underlier;
        this.from = from;
        this.to = to;

        super.executeQuery();

        return records;
    }
}
