package com.aquarians.backtester.database.procedures;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Pair;
import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;
import java.sql.ResultSet;

public class StockPricesSelectMinMaxDate extends DbStatement {
    private static final String SQL_STATEMENT = "SELECT min(day), max(day) " +
            "FROM stock_prices " +
            "WHERE underlier = ?";

    // Input
    private Long underlier;

    // Output
    private Day min;
    private Day max;

    public StockPricesSelectMinMaxDate(Connection connection) {
        super(connection);
    }

    @Override
    protected String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    public void setParameters() throws Exception {
        setLong(1, underlier);
    }

    @Override
    public void process(ResultSet results) throws Exception {
        min = null;
        max = null;
        if (results.next()) {
            min = getDay(results, 1);
            max = getDay(results, 2);
        }
    }

    public Pair<Day, Day> execute(Long underlier) {
        this.underlier = underlier;

        super.executeQuery();

        return new Pair<>(min, max);
    }
}
