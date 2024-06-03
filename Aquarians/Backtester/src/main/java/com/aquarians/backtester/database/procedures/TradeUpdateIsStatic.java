package com.aquarians.backtester.database.procedures;

import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;

public class TradeUpdateIsStatic extends DbStatement {

    private static final String SQL_STATEMENT = "UPDATE trades SET is_static = ? WHERE id = ?";

    private Long id;
    private Boolean is_static;

    public TradeUpdateIsStatic(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setBoolean(1, is_static);
        setLong(2, id);
    }

    public void execute(Long id, Boolean is_static) {
        this.id = id;
        this.is_static = is_static;

        executeUpdate();
    }
}
