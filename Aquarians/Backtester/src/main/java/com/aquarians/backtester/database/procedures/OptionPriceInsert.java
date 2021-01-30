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

public class OptionPriceInsert extends DbStatement {

    private static final int CODE_COLUMN_LENGTH = 64;

    private static final String SQL_STATEMENT =
            "INSERT INTO option_prices " +
                    "(underlier, code, day, is_call, strike, maturity, bid, ask)" +
                    "values (?, ?, ?, ?, ?, ?, ?, ?)";

    private Long underlier;
    private String code;
    private Day day;
    private Boolean is_call;
    private Double strike;
    private Day maturity;
    private Double bid;
    private Double ask;

    public OptionPriceInsert(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setLong(1, underlier);
        setString(2, code);
        setDay(3, day);
        setBoolean(4, is_call);
        setDouble(5, strike);
        setDay(6, maturity);
        setDouble(7, bid);
        setDouble(8, ask);
    }

    public void execute(
            Long underlier,
            String code,
            Day day,
            Boolean is_call,
            Double strike,
            Day maturity,
            Double bid,
            Double ask) {
        this.underlier = underlier;
        this.code = code.substring(0, Math.min(CODE_COLUMN_LENGTH, code.length()));
        this.day = day;
        this.is_call = is_call;
        this.strike = strike;
        this.maturity = maturity;
        this.bid = bid;
        this.ask = ask;

        super.executeUpdate();
    }

}
