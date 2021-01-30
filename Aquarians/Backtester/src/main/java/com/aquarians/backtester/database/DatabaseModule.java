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

package com.aquarians.backtester.database;

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.backtester.Application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseModule implements ApplicationModule {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(DatabaseModule.class);

    public static final String NAME = "Database";

    private final int index;
    private final String url;
    private final String user;
    private final String password;

    private Connection connection;
    private Procedures procedures;

    public DatabaseModule(int index) {
        this.index = index;
        Properties properties = Application.getInstance().getProperties();
        url = properties.getProperty("Database.URL");
        user = properties.getProperty("Database.User");
        password = properties.getProperty("Database.Password");
    }

    @Override
    public void init() {
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        procedures = new Procedures(connection);
        procedures.init();
    }

    @Override
    public void cleanup() {
        try {
            procedures.cleanup();
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }

        try {
            connection.close();
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
    }

    @Override
    public String getName() {
        return Application.buildModuleName(NAME, index);
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
    }

    public void commit() {
        try {
            connection.commit();
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
    }

    public Procedures getProcedures() {
        return procedures;
    }
}
