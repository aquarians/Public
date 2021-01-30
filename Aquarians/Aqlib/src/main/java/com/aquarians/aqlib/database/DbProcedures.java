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
import java.util.ArrayList;

public abstract class DbProcedures {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(DbProcedures.class);

    private final Connection connection;
    private final ArrayList<DbStatement> procedures = new ArrayList<>();

    public final SequenceNextVal sequenceNextVal;

    public DbProcedures(Connection connection) {
        this.connection = connection;
        sequenceNextVal = addProcedure(new SequenceNextVal(connection));
    }

    public <T extends DbStatement> T addProcedure(T procedure) {
        procedures.add(procedure);
        return procedure;
    }

    public void init() {
        for (DbStatement procedure : procedures) {
            try {
                procedure.init();
            } catch (Exception ex) {
                logger.warn("Initializing procedure: " + procedure.getClass().getSimpleName(), ex);
            }
        }
    }

    public void cleanup() {
        for (DbStatement procedure : procedures) {
            try {
                procedure.cleanup();
            } catch (Exception ex) {
                logger.warn("Cleaning procedure: " + procedure.getClass().getSimpleName(), ex);
            }
        }
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void commit() {
        try {
            connection.commit();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}
