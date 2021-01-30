/*
    MIT License

    Copyright (c) 2017 Mihai Bunea

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

public class StockPriceInsert extends DbStatement {

    private static final String SQL_STATEMENT = "INSERT INTO stock_prices" +
            " (underlier, day, open, high, low, close, adjusted, volume)" +
            "  values (?, ?, ?, ?, ?, ?, ?, ?)";

    private Long underlier;
    private Day day;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private double adjusted;

    public StockPriceInsert(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setLong( 1, underlier);
        setDay( 2, day);
        setDouble( 3, open);
        setDouble( 4, high);
        setDouble( 5, low);
        setDouble( 6, close);
        setDouble( 7, adjusted);
        setLong( 8, volume);
    }

    public void execute(
            Long underlier,
            Day day,
            double open,
            double high,
            double low,
            double close,
            double adjusted,
            long volume) {
        this.underlier = underlier;
        this.day = day;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjusted = adjusted;
        this.volume = volume;

        super.executeUpdate();
    }
}
