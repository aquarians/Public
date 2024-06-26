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
import com.aquarians.aqlib.Pair;
import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StockDividendsSelect extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT date, dividend FROM stock_dividends WHERE underlier = ? AND date >= ? AND date <= ?";

    private Long underlier;
    private TreeMap<Day, Double> dividends;
    private Day from;
    private Day to;

    public StockDividendsSelect(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
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
        dividends = new TreeMap<>();
        while (results.next()) {
            Day day = getDay(results, 1);
            Double ratio = getDouble(results, 2);
            dividends.put(day, ratio);
        }
    }

    public TreeMap<Day, Double> execute(Long underlier) {
        this.underlier = underlier;
        from = new Day("1900-Jan-01");
        to = new Day("2100-Jan-01");
        executeQuery();
        return new TreeMap<>(dividends);
    }

    public TreeMap<Day, Double> execute(Long underlier, Day from, Day to) {
        this.underlier = underlier;
        this.from = from;
        this.to = to;
        executeQuery();
        return new TreeMap<>(dividends);
    }

    public boolean hasSplits(Long underlier, Day from, Day to) {
        this.underlier = underlier;
        this.from = from;
        this.to = to;
        executeQuery();
        return (dividends.size() > 0);
    }

    public List<Pair<Day, Double>> executeList(Long underlier, Day from, Day to) {
        this.underlier = underlier;
        this.from = from;
        this.to = to;
        executeQuery();
        List<Pair<Day, Double>> records = new ArrayList<>(dividends.size());
        for (Map.Entry<Day, Double> entry : dividends.entrySet()) {
            records.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        return records;
    }

}
