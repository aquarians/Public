package com.aquarians.backtester.database.procedures;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.database.DbStatement;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class OptionPriceBulkInsert extends DbStatement {

    public static final int CODE_COLUMN_LENGTH = 64;
    public static final int RECORDS = 10;

    private static final String SQL_STATEMENT =
            "INSERT INTO option_prices " +
                    "(underlier, code, day, is_call, strike, maturity, bid, ask)" +
                    "values" +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)," +
                    " (?, ?, ?, ?, ?, ?, ?, ?)";

    private final List<Record> records = new ArrayList<>(RECORDS);

    public OptionPriceBulkInsert(Connection connection) {
        super(connection);

        for (int i = 0; i < RECORDS; i++) {
            records.add(new Record());
        }
    }

    public static final class Record {
        public Long underlier;
        public String code;
        public Day day;
        public Boolean is_call;
        public Double strike;
        public Day maturity;
        public Double bid;
        public Double ask;
    }

    @Override
    public String getSqlStatement() {
        return SQL_STATEMENT;
    }

    @Override
    protected void setParameters() throws Exception {
        int index = 0;
        for (int pos = 0; pos < RECORDS; pos++) {
            Record record = records.get(pos);
            index++;
            setLong(index, record.underlier);
            index++;
            setString(index, record.code);
            index++;
            setDay(index, record.day);
            index++;
            setBoolean(index, record.is_call);
            index++;
            setDouble(index, record.strike);
            index++;
            setDay(index, record.maturity);
            index++;
            setDouble(index, record.bid);
            index++;
            setDouble(index, record.ask);
        }
    }

    public void execute(List<Record> records) {
        if (records.size() != RECORDS) {
            throw new RuntimeException("Expecting " + RECORDS + " records");
        }

        for (int i = 0; i < records.size(); i++) {
            Record thisRecord = this.records.get(i);
            Record record = records.get(i);

            thisRecord.underlier = record.underlier;
            thisRecord.code = record.code;
            thisRecord.day = record.day;
            thisRecord.is_call = record.is_call;
            thisRecord.strike = record.strike;
            thisRecord.maturity = record.maturity;
            thisRecord.bid = record.bid;
            thisRecord.ask = record.ask;
        }

        super.executeUpdate();
    }

}
