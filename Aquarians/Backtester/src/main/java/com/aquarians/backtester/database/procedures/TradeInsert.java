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

public class TradeInsert extends DbStatement {

    private static final String SQL_STATEMENT = "INSERT INTO trades " +
            "(id, strategy, execution_day, instr_type, instr_code, instr_is_call, " +
            "instr_maturity, instr_strike, quantity, price, tv, commission, label, is_static) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private Long id;
    private Long strategy;
    private Day execution_day;
    private String instr_type;
    private String instr_code;
    private Boolean instr_is_call;
    private Day instr_maturity;
    private Double instr_strike;
    private Double quantity;
    private Double price;
    private Double tv;
    private Double commission;
    private String label;
    private Boolean is_static;

    public TradeInsert(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setLong(1, id);
        setLong(2, strategy);
        setDay(3, execution_day);
        setString(4, instr_type);
        setString(5, instr_code);
        setBoolean(6, instr_is_call);
        setDay(7, instr_maturity);
        setDouble(8, instr_strike);
        setDouble(9, quantity);
        setDouble(10, price);
        setDouble(11, tv);
        setDouble(12, commission);
        setString(13, label);
        setBoolean(14, is_static);
    }

    public void execute(
            Long id,
            Long strategy,
            Day execution_day,
            String instr_type,
            String instr_code,
            Boolean instr_is_call,
            Day instr_maturity,
            Double instr_strike,
            Double quantity,
            Double price,
            Double tv,
            Double commission,
            String label,
            Boolean is_static) {
        this.id = id;
        this.strategy = strategy;
        this.execution_day = execution_day;
        this.instr_type = instr_type;
        this.instr_code = instr_code;
        this.instr_is_call = instr_is_call;
        this.instr_maturity = instr_maturity;
        this.instr_strike = instr_strike;
        this.quantity = quantity;
        this.price = price;
        this.tv = tv;
        this.commission = commission;
        this.label = label;
        this.is_static = is_static;

        executeUpdate();
    }
}
