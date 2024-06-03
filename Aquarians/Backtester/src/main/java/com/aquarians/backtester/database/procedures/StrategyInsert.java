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

import com.aquarians.aqlib.database.DbStatement;
import com.aquarians.aqlib.Day;

import java.sql.Connection;

public class StrategyInsert extends DbStatement {

    private static final String SQL_STATEMENT = "INSERT INTO strategies " +
            "(id, type, number, multiplier, underlier, execution_day, maturity_day, " +
            "volatility, execution_spot, expected_pnl_mean, expected_pnl_dev, capital, data) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private Long id;
    private String type;
    private Integer number;
    private Double multiplier;
    private Long underlier;
    private Day execution_day;
    private Day maturity_day;
    private Double volatility;
    private Double execution_spot;
    private Double expected_pnl_mean;
    private Double expected_pnl_dev;
    private Double capital;
    private String data;

    public StrategyInsert(Connection connection) {
        super(connection);
    }

    @Override
    protected String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setLong(1, id);
        setString(2, type);
        setInt(3, number);
        setDouble(4, multiplier);
        setLong(5, underlier);
        setDay(6, execution_day);
        setDay(7, maturity_day);
        setDouble(8, volatility);
        setDouble(9, execution_spot);
        setDouble(10, expected_pnl_mean);
        setDouble(11, expected_pnl_dev);
        setDouble(12, capital);
        setString(13, data);
    }

    public void execute(
            Long id,
            String type,
            Integer number,
            Double multiplier,
            Long underlier,
            Day execution_day,
            Day maturity_day,
            Double volatility,
            Double executionSpot,
            Double expectedPnlMean,
            Double expectedPnlDev,
            Double capital,
            String data) {
        this.id = id;
        this.type = type;
        this.number = number;
        this.multiplier = multiplier;
        this.underlier = underlier;
        this.execution_day = execution_day;
        this.maturity_day = maturity_day;
        this.volatility = volatility;
        this.execution_spot = executionSpot;
        this.expected_pnl_mean = expectedPnlMean;
        this.expected_pnl_dev = expectedPnlDev;
        this.capital = capital;
        this.data = data;
        super.executeUpdate();
    }
}
