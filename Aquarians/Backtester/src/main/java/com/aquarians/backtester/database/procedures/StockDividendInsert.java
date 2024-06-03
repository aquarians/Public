package com.aquarians.backtester.database.procedures;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;

public class StockDividendInsert extends DbStatement {

    private static final String SQL_STATEMENT = "INSERT INTO stock_dividends (underlier, date, dividend) VALUES (?, ?, ?)";

    private Long underlier;
    private Day day;
    private Double dividend;

    public StockDividendInsert(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    public void setParameters() throws Exception {
        setLong(1, underlier);
        setDay(2, day);
        setDouble(3, dividend);
    }

    public void execute(Long underlier, Day day, Double dividend) {
        this.underlier = underlier;
        this.day = day;
        this.dividend = dividend;
        super.executeUpdate();
    }

}
