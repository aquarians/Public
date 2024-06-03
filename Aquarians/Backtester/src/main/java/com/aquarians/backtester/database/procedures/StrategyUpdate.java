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

import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;

public class StrategyUpdate extends DbStatement {

    private static final String SQL_STATEMENT = "UPDATE strategies SET realized_pnl = ?, commission = ?, data = ? WHERE id = ?";

    private Long id;
    private Double realized_pnl;
    private Double commission;
    private String data;

    public StrategyUpdate(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setDouble(1, realized_pnl);
        setDouble(2, commission);
        setString(3, data);
        setLong(4, id);
    }

    public void execute(Long id, Double realized_pnl, Double commission, String data) {
        this.id = id;
        this.realized_pnl = realized_pnl;
        this.commission = commission;
        this.data = data;
        executeUpdate();
    }

}
