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

package com.aquarians.backtester.database.procedures;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;

public class StockPriceUpdate extends DbStatement {

    private static final String SQL_STATEMENT = "UPDATE stock_prices " +
            "SET implied = ?, volatility = ? " +
            "WHERE underlier = ? AND day = ?";

    private Long underlier;
    private Day day;
    private Double implied;
    private Double volatility;

    public StockPriceUpdate(Connection connection) {
        super(connection);
    }

    @Override
    protected String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    public void setParameters() throws Exception {
        setDouble(1, implied);
        setDouble(2, volatility);
        setLong(3, underlier);
        setDay(4, day);
    }

    public void execute(
            Long underlier,
            Day day,
            Double implied,
            Double volatility) {
        this.underlier = underlier;
        this.day = day;
        this.implied = implied;
        this.volatility = volatility;
        executeUpdate();
    }
}
