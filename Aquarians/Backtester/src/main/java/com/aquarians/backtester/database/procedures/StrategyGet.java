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
import java.sql.ResultSet;

public class StrategyGet extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT " +
            "type, number, multiplier, underlier, execution_day, maturity_day, volatility, execution_spot, " +
            "expected_pnl_mean, expected_pnl_dev, realized_pnl, capital, data, commission " +
            "FROM strategies WHERE id = ?";

    private Long id;
    private Record record;

    public static final class Record {
        public final Long id;
        public final String type;
        public final Integer number;
        public final Double multiplier;
        public final Long underlier;
        public final Day execution_day;
        public final Day maturity_day;
        public final Double volatility;
        public final Double execution_spot;
        public final Double expected_pnl_mean;
        public final Double expected_pnl_dev;
        public final Double realized_pnl;
        public final Double capital;
        public final String data;
        public final Double commission;

        public Record(Long id,
                      String type,
                      Integer number,
                      Double multiplier,
                      Long underlier,
                      Day execution_day,
                      Day maturity_day,
                      Double volatility,
                      Double execution_spot,
                      Double expected_pnl_mean,
                      Double expected_pnl_dev,
                      Double realized_pnl,
                      Double capital,
                      String data,
                      Double commission) {
            this.id = id;
            this.type = type;
            this.number = number;
            this.multiplier = multiplier;
            this.underlier = underlier;
            this.execution_day = execution_day;
            this.maturity_day = maturity_day;
            this.volatility = volatility;
            this.execution_spot = execution_spot;
            this.expected_pnl_mean = expected_pnl_mean;
            this.expected_pnl_dev = expected_pnl_dev;
            this.realized_pnl = realized_pnl;
            this.capital = capital;
            this.data = data;
            this.commission = commission;
        }
    }

    public StrategyGet(Connection connection) {
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
            String type = getString(results, 1);
            Integer number = getInt(results, 2);
            Double multiplier = getDouble(results, 3);
            Long underlier = getLong(results, 4);
            Day execution_day = getDay(results, 5);
            Day maturity_day = getDay(results, 6);
            Double volatility = getDouble(results, 7);
            Double execution_spot = getDouble(results, 8);
            Double expected_pnl_mean = getDouble(results, 9);
            Double expected_pnl_dev = getDouble(results, 10);
            Double realized_pnl = getDouble(results, 11);
            Double capital = getDouble(results, 12);
            String data = getString(results, 13);
            Double commission = getDouble(results, 14);
            record = new Record(id, type, number, multiplier, underlier, execution_day,
                    maturity_day, volatility, execution_spot, expected_pnl_mean,
                    expected_pnl_dev, realized_pnl, capital, data, commission);
        }
    }

    public Record execute(Long id) {
        this.id = id;
        record = null;
        executeQuery();
        return record;
    }

}
