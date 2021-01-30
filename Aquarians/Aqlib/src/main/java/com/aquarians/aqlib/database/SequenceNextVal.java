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

package com.aquarians.aqlib.database;

import java.sql.Connection;
import java.sql.ResultSet;

public class SequenceNextVal extends DbStatement {

    private static final String SQL_STATEMENT = "SELECT nextval(?)";

    // Input
    private String name;

    // Output
    private Long id;

    public SequenceNextVal(Connection connection) {
        super(connection);
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        setString(1, name);
    }

    @Override
    public void process(ResultSet results) throws Exception {
        results.next();
        id = getLong(results, 1);
    }

    public Long execute(String name) {
        this.name = name;
        super.executeQuery();
        return id;
    }

}
