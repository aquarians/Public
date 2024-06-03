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
import com.aquarians.backtester.database.records.NavRecord;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class NavSelectByUnderlier extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT day, available, allocated FROM nav WHERE strategy_type = ? AND underlier = ? ORDER BY day";

    // Input
    private String strategy_type;
    private Long underlier;

    private List<NavRecord> records;

    public NavSelectByUnderlier(Connection connection) {
        super(connection);
    }

    @Override
    protected String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    public void setParameters() throws Exception {
        setString(1, strategy_type);
        setLong(2, underlier);
    }

    @Override
    public void process(ResultSet results) throws Exception {
        records = new ArrayList<>();
        while (results.next()) {
            Day day = getDay(results, 1);
            Double available = getDouble(results, 2);
            Double allocated = getDouble(results, 3);
            records.add(new NavRecord(day, underlier, available, allocated));
        }
    }

    public List<NavRecord> execute(String strategy_type, Long underlier) {
        this.strategy_type = strategy_type;
        this.underlier = underlier;

        super.executeQuery();

        return new ArrayList<>(records);
    }
}
