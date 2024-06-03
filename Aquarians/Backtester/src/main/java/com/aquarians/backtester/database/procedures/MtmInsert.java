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

public class MtmInsert extends DbStatement {

    private static final String SQL_STATEMENT = "INSERT INTO mtm (id, strategy, day, delta_position, spot_price, volatility, " +
            "market_profit, theoretical_profit) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    public MtmInsert(Connection connection) {
        super(connection);
    }

    private Long id;
    private Long strategy;
    private Day day;
    private Double delta_position;
    private Double spot_price;
    private Double volatility;
    private Double market_profit;
    private Double theoretical_profit;

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setLong(1, id);
        setLong(2, strategy);
        setDay(3, day);
        setDouble(4, delta_position);
        setDouble(5, spot_price);
        setDouble(6, volatility);
        setDouble(7, market_profit);
        setDouble(8, theoretical_profit);
    }

    public void execute(
            Long id,
            Long strategy,
            Day day,
            Double delta_position,
            Double spot_price,
            Double volatility,
            Double market_profit,
            Double theoretical_profit) {
        this.id = id;
        this.strategy = strategy;
        this.day = day;
        this.delta_position = delta_position;
        this.spot_price = spot_price;
        this.volatility = volatility;
        this.market_profit = market_profit;
        this.theoretical_profit = theoretical_profit;

        super.executeUpdate();
    }

}
