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

import com.aquarians.aqlib.*;

import java.sql.*;

public abstract class DbStatement {

    private final Connection connection;
    private PreparedStatement statement;

    protected DbStatement(Connection connection) {
        this.connection = connection;
    }

    /**
     * An SQL statement with parameter placeholders, like "select x, y from t where z = ? and w < ?"
     */
    protected abstract String getSqlStatement();

    /**
     * Called before execution, moves params from DbStatement to the PreparedStatement
     */
    protected void setParameters() throws Exception {}

    /**
     * Called for queries which return a recordset. Moved data from the recordset to the DbStatement.
     */
    protected void process(ResultSet results) throws Exception {}

    public void init() {
        try {
            statement = connection.prepareStatement(getSqlStatement());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void cleanup() {
        try {
            if (null != statement) {
                statement.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    protected void executeUpdate() {
        try {
            setParameters();
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    protected void executeQuery() {
        try {
            setParameters();
            ResultSet resultSet = statement.executeQuery();
            process(resultSet);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public Blob createBlob(byte[] data) {
        try {
            Blob blob = connection.createBlob();
            blob.setBytes(0, data);
            return blob;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void freeBlob(Blob blob) {
        try {
            blob.free();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void setInt(int parameterIndex, Integer value) throws Exception {
        if (null != value) {
            statement.setInt(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.INTEGER);
        }
    }

    public void setLong(int parameterIndex, Long value) throws Exception {
        if (null != value) {
            statement.setLong(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.BIGINT);
        }
    }

    public void setDouble(int parameterIndex, Double value) throws Exception {
        if (null != value) {
            statement.setDouble(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.DOUBLE);
        }
    }

    public void setString(int parameterIndex, String value) throws Exception {
        if (null != value) {
            statement.setString(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.VARCHAR);
        }
    }

    public void setDay(int parameterIndex, Day value) throws Exception {
        if (null != value) {
            statement.setTimestamp(parameterIndex, new Timestamp(value.toCalendar().getTimeInMillis()), AqCalendar.UTC_CALENDAR);
        } else {
            statement.setNull(parameterIndex, Types.DATE);
        }
    }

    public void setTime(int parameterIndex, com.aquarians.aqlib.Time value) throws Exception {
        if (null != value) {
            statement.setTime(parameterIndex, new java.sql.Time(value.millis()));
        } else {
            statement.setNull(parameterIndex, Types.TIME);
        }
    }

    public void setTimestamp(int parameterIndex, AqCalendar value) throws Exception {
        if (null != value) {
            statement.setTimestamp(parameterIndex, new Timestamp(value.getTimeInMillis()), AqCalendar.UTC_CALENDAR);
        } else {
            statement.setNull(parameterIndex, Types.TIMESTAMP);
        }
    }

    public void setBlob(int parameterIndex, Blob value) throws Exception {
        if (null != value) {
            statement.setBlob(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.BLOB);
        }
    }

    public void setBoolean(int parameterIndex, Boolean value) throws Exception {
        if (null != value) {
            statement.setBoolean(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.BOOLEAN);
        }
    }

    public static Integer getInt(ResultSet results, int parameterIndex) throws Exception {
        int value = results.getInt(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        return value;
    }

    public static Long getLong(ResultSet results, int parameterIndex) throws Exception {
        long value = results.getLong(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        return value;
    }

    public static Double getDouble(ResultSet results, int parameterIndex) throws Exception {
        double value = results.getDouble(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        return value;
    }

    public static Boolean getBoolean(ResultSet results, int parameterIndex) throws Exception {
        boolean value = results.getBoolean(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        return value;
    }

    public static String getString(ResultSet results, int parameterIndex) throws Exception {
        String value = results.getString(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        return value;
    }

    public static Day getDay(ResultSet results, int parameterIndex) throws Exception {
        Timestamp value = results.getTimestamp(parameterIndex, AqCalendar.UTC_CALENDAR);
        if (results.wasNull()) {
            return null;
        }
        return new Day(new AqCalendar(value.getTime()));
    }

    public static com.aquarians.aqlib.Time getTime(ResultSet results, int parameterIndex) throws Exception {
        java.sql.Time value = results.getTime(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        return new com.aquarians.aqlib.Time(new AqCalendar(value.getTime()));
    }

    public static AqCalendar getTimestamp(ResultSet results, int parameterIndex) throws Exception {
        Timestamp value = results.getTimestamp(parameterIndex, AqCalendar.UTC_CALENDAR);
        if (results.wasNull()) {
            return null;
        }
        return new AqCalendar(value.getTime());
    }

    public static byte[] getBlob(ResultSet results, int parameterIndex) throws Exception {
        Blob value = results.getBlob(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        byte[] data = value.getBytes(0, (int) value.length());
        value.free();
        return data;
    }

    public static Boolean geBoolean(ResultSet results, int parameterIndex) throws Exception {
        Boolean value = results.getBoolean(parameterIndex);
        if (results.wasNull()) {
            return null;
        }
        return value;
    }

}
