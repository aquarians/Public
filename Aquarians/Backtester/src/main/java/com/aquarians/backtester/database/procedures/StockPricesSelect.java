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
import com.aquarians.backtester.database.records.StockPriceRecord;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class StockPricesSelect extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT day, open, high, low, close, adjusted, implied, volume, volatility " +
            "FROM stock_prices " +
            "WHERE underlier = ? AND day >= ? AND day <= ? " +
            "ORDER BY day";

    // Input
    private Long underlier;
    private Day from;
    private Day to;

    // Output
    private List<StockPriceRecord> records;

    public StockPricesSelect(Connection connection) {
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
            Double open = getDouble(results, 2);
            Double high = getDouble(results, 3);
            Double low = getDouble(results, 4);
            Double close = getDouble(results, 5);
            Double adjusted = getDouble(results, 6);
            Double implied = getDouble(results, 7);
            Long volume = getLong(results, 8);
            Double volatility = getDouble(results, 9);
            records .add(new StockPriceRecord(day, open, high, low, close, adjusted, implied, volume, volatility));
        }
    }

    public List<StockPriceRecord> execute(Long underlier, Day from, Day to) {
        this.underlier = underlier;
        this.from = from;
        this.to = to;

        super.executeQuery();

        return records;
    }
}
