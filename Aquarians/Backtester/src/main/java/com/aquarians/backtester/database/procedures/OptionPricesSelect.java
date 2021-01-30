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
import com.aquarians.backtester.database.records.OptionPriceRecord;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OptionPricesSelect extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT day, code, is_call, strike, maturity, bid, ask " +
            "FROM option_prices " +
            "WHERE underlier = ? AND day >= ? AND day <= ?";

    // Input
    private Long underlier;
    private Day from;
    private Day to;

    // Output
    private List<OptionPriceRecord> records;

    public OptionPricesSelect(Connection connection) {
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
            String code = getString(results, 2);
            Boolean is_call = getBoolean(results, 3);
            Double strike = getDouble(results, 4);
            Day maturity = getDay(results, 5);
            Double bid = getDouble(results, 6);
            Double ask = getDouble(results, 7);
            records.add(new OptionPriceRecord(day, code, is_call, strike, maturity, bid, ask));
        }
    }

    public List<OptionPriceRecord> execute(Long underlier, Day from, Day to) {
        this.underlier = underlier;
        this.from = from;
        this.to = to;

        super.executeQuery();

        return records;
    }
}
