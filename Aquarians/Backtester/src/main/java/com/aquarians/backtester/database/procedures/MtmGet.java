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
import java.sql.ResultSet;

public class MtmGet extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT strategy, day, delta_position, spot_price, volatility, market_profit, theoretical_profit " +
            "FROM mtm WHERE id = ?";

    private Long id;
    private Record record;

    public static final class Record {
        public final Long strategy;
        public final Day day;
        public final Double delta_position;
        public final Double spot_price;
        public final Double volatility;
        public final Double market_profit;
        public final Double theoretical_profit;

        public Record(Long strategy,
                      Day day,
                      Double delta_position,
                      Double spot_price,
                      Double volatility,
                      Double market_profit,
                      Double theoretical_profit) {
            this.strategy = strategy;
            this.day = day;
            this.delta_position = delta_position;
            this.spot_price = spot_price;
            this.volatility = volatility;
            this.market_profit = market_profit;
            this.theoretical_profit = theoretical_profit;
        }
    }

    public MtmGet(Connection connection) {
        super(connection);
    }

    @Override
    protected String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    public void setParameters() throws Exception {
        setLong(1, id);
    }

    @Override
    public void process(ResultSet results) throws Exception {
        if (results.next()) {
            Long strategy = getLong(results, 1);
            Day day = getDay(results, 2);
            Double delta_position = getDouble(results, 3);
            Double spot_price = getDouble(results, 4);
            Double volatility = getDouble(results, 5);
            Double market_profit = getDouble(results, 6);
            Double theoretical_profit = getDouble(results, 7);
            record = new Record(strategy, day, delta_position, spot_price, volatility, market_profit, theoretical_profit);
        }
    }

    public Record execute(Long id) {
        this.id = id;
        record = null;
        executeQuery();
        return record;
    }
}
