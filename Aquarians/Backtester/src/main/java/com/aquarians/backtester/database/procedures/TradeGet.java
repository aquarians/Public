/*
    MIT License

    Copyright (c) 2017 Mihai Bunea

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
import java.sql.ResultSet;

public class TradeGet extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT " +
            "execution_day, instr_type, instr_code, instr_is_call, instr_maturity, instr_strike, " +
            "quantity, price, tv, commission, label, is_static " +
            "FROM trades WHERE id = ?";

    private Long id;
    private Record record;

    public static final class Record {
        public final Day execution_day;
        public final String instr_type;
        public final String instr_code;
        public final Boolean instr_is_call;
        public final Day instr_maturity;
        public final Double instr_strike;
        public final double quantity;
        public final double price;
        public final double tv;
        public final Double commission;
        public final String label;
        public final Boolean is_static;

        public Record(Day execution_day,
                      String instr_type,
                      String instr_code,
                      Boolean instr_is_call,
                      Day instr_maturity,
                      Double instr_strike,
                      double quantity,
                      double price,
                      double tv,
                      Double commission,
                      String label,
                      Boolean is_static) {
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
        }
    }

    public TradeGet(Connection connection) {
        super(connection);
    }

    @Override
    protected String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    public void setParameters() throws Exception {
        setLong(1, id);
    }

    @Override
    public void process(ResultSet results) throws Exception {
        if (results.next()) {
            Day execution_day = getDay(results, 1);
            String instr_type = getString(results, 2);
            String instr_code = getString(results, 3);
            Boolean instr_is_call = getBoolean(results, 4);
            Day instr_maturity = getDay(results, 5);
            Double instr_strike = getDouble(results, 6);
            Double quantity = getDouble(results, 7);
            Double price = getDouble(results, 8);
            Double tv = getDouble(results, 9);
            Double commission = getDouble(results, 10);
            String label = getString(results, 11);
            Boolean is_static = getBoolean(results, 12);
            record = new Record(execution_day, instr_type, instr_code, instr_is_call, instr_maturity, instr_strike, quantity, price, tv, commission, label, is_static);
        }
    }

    public Record execute(Long id) {
        this.id = id;
        record = null;
        executeQuery();
        return record;
    }

}
