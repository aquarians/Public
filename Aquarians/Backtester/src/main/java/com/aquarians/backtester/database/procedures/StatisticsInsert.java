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

public class StatisticsInsert extends DbStatement {

    private static final String SQL_STATEMENT =
            "INSERT INTO statistics " +
                    "(underlier, day, spot_fwd_diff, parity_total, option_total)" +
                    "values (?, ?, ?, ?, ?)";

    private Long underlier;
    private Day day;
    private Double spot_fwd_diff;
    private Double parity_total;
    private Double option_total;

    public StatisticsInsert(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setLong(1, underlier);
        setDay(2, day);
        setDouble(3, spot_fwd_diff);
        setDouble(4, parity_total);
        setDouble(5, option_total);
    }

    public void execute(
            Long underlier,
            Day day,
            Double spot_fwd_diff,
            Double parity_total,
            Double option_total) {
        this.underlier = underlier;
        this.day = day;
        this.spot_fwd_diff = spot_fwd_diff;
        this.parity_total = parity_total;
        this.option_total = option_total;

        super.executeUpdate();
    }
}
