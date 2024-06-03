package com.aquarians.backtester.database.procedures;

import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;

public class StrategyUpdateData extends DbStatement {

    private static final String SQL_STATEMENT = "UPDATE strategies SET data = ? WHERE id = ?";

    private Long id;
    private String data;

    public StrategyUpdateData(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setString(1, data);
        setLong(2, id);
    }

    public void execute(Long id, String data) {
        this.id = id;
        this.data = data;
        executeUpdate();
    }

}
