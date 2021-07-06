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

package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.CsvFileReader;
import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Util;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.Procedures;
import com.aquarians.backtester.database.records.StockPriceRecord;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportHistoricalOptionPricesJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ImportHistoricalOptionPricesJob.class.getSimpleName());

    private static final long BULK_IMPORT_COUNT = 10000;

    private enum DataFormat {
        Default,
        Orats
    }

    private enum Source {
        Composite("*"), // Asterisk means it is a composite price, including all option exchanges
        NonStandard("N"), // This is a non-standard, or special settlement option situations, and not used in back testing
        Binary("B"), // Binary option
        Weekly("W"), // Weekly option
        Quarterly("Q"), // Quarterly option
        PmSettled("P"), // SPX PM Settled. Usually SPX is AM settled
        TenShares("T"), // Ten Shares per contract ("mini" options)
        Jumbo("J"); // Jumbo (1000 shares per contract)

        public final String code;

        Source(String code) {
            this.code = code;
        }

        public static Source parseSource(String code) {
            for (Source type : Source.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }

            return null;
        }
    }

    private final DatabaseModule owner;
    private final String folder;
    private final String regex;
    private Set<Source> sources = new TreeSet<>();
    private Map<String, String> aliases = new TreeMap<>();
    private Set<String> underliersFilter = new TreeSet<>();
    private final boolean clearPreviousData;
    private final boolean importStockPrice;
    private final DataFormat dataFormat;

    private long importedCount = 0;

    public ImportHistoricalOptionPricesJob(DatabaseModule owner) {
        this.owner = owner;
        folder = Application.getInstance().getFolderProperty("ImportHistoricalOptionPricesJob.Folder");
        regex = Application.getInstance().getProperties().getProperty("ImportHistoricalOptionPricesJob.Regex");
        clearPreviousData = Boolean.parseBoolean(Application.getInstance().getProperties().getProperty(
                "ImportHistoricalOptionPricesJob.ClearPreviousData", "false"));
        importStockPrice = Boolean.parseBoolean(Application.getInstance().getProperties().getProperty(
                "ImportHistoricalOptionPricesJob.ImportStockPrice", "false"));
        dataFormat = DataFormat.valueOf(Application.getInstance().getProperties().getProperty("ImportHistoricalOptionPricesJob.DataFormat"));

        String[] sources = Application.getInstance().getProperties().getProperty("ImportHistoricalOptionPricesJob.Sources").split(",");
        for (String source : sources) {
            this.sources.add(Source.valueOf(source));
        }

        String aliases = Application.getInstance().getProperties().getProperty("ImportHistoricalOptionPricesJob.Aliases");
        if (null != aliases) {
            String[] mappings = aliases.split(",");
            for (String mapping : mappings) {
                String[] tagvalue = mapping.split(":");
                this.aliases.put(tagvalue[0], tagvalue[1]);
                logger.debug("Alias: " + tagvalue[0] + " mapped to " + tagvalue[1]);
            }
        }

        String[] underliers = Application.getInstance().getProperties().getProperty("ImportHistoricalOptionPricesJob.Underliers" , "").split(",");
        for (String underlier : underliers) {
            underlier = underlier.trim();
            if (underlier.length() == 0) {
                continue;
            }

            underliersFilter.add(underlier.trim());
        }

        String underliersFile = Application.getInstance().getProperties().getProperty("ImportHistoricalStockPricesJob.UnderliersFile");
        if (null != underliersFile) {
            loadUnderliersFromFile(underliersFile);
        }
    }

    @Override
    public void run() {
        logger.info("Running");
        try {
            doClearPreviousData();
            internalRun();
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
        logger.info("Finished");
    }

    private void doClearPreviousData() {
        if (!clearPreviousData) {
            return;
        }

        for (String code : underliersFilter) {
            Long id = owner.getProcedures().underlierSelect.execute(code);
            if (null == id) {
                continue;
            }

            logger.info("Clearing data for underlier: " + code);
            owner.getProcedures().optionPricesDeleteAll.execute(id);
            logger.info("Delete of data completed");
        }
    }

    public void internalRun() {
        File fldr = new File(folder);
        File[] files = fldr.listFiles();
        if (null == files) {
            logger.info("No files");
            return;
        }

        // Sort by day
        Pattern pattern = Pattern.compile(regex);
        Map<Day, String> paths = new TreeMap<>();
        for (File file : files) {
            String name = file.getName();
            Matcher matcher = pattern.matcher(name);
            if (!matcher.find()) {
                continue;
            }

            String dayText = matcher.group(1);
            Day day = new Day(dayText, Day.FORMAT_YYYYMMDD);
            paths.put(day, file.getPath());
        }

        // Enable bulk operations
        owner.setAutoCommit(false);

        // Go from past to present
        for (Map.Entry<Day, String> entry : paths.entrySet()) {
            Day day = entry.getKey();
            String path = entry.getValue();
            try {
                int count = loadFile(day, path);
                logger.debug("Imported count=" + count + " options for day=" + day);
            } catch (Exception ex) {
                logger.warn("Reading file: " + path, ex);
            }
        }

        // Disable bulk operations
        owner.setAutoCommit(true);
    }

    private static final class FileRecord {
        String underlyingSymbol;
        Double stockPrice;
        Source source;
        String optionSymbol;
        Boolean isCall;
        Day expiration;
        Day recordDay;
        Double strike;
        Double bid;
        Double ask;
    }

    // https://www.historicaloptiondata.com/content/historical-options-data-file-structures-0
    private static FileRecord parseDefaultFileRecord(String[] columns) {
        // 0 UnderlyingSymbol
        // 1 UnderlyingPrice
        // 2 Flags
        // 3 OptionSymbol
        // 4 Blank
        // 5 Type
        // 6 Expiration
        // 7 DataDate
        // 8 Strike
        // 9 Last
        // 10 Bid
        // 11 Ask
        // 12 Volume
        // 13 OpenInterest
        // 14 T1OpenInterest
        FileRecord record = new FileRecord();

        record.underlyingSymbol = columns[0];
        record.stockPrice = Double.parseDouble(columns[1]);
        record.source = Source.parseSource(columns[2]);
        record.optionSymbol = columns[3];
        String optionType = columns[5].toLowerCase();
        record.isCall = optionType.equals("call") || optionType.equals("c");
        record.expiration = new Day(columns[6], Day.US_FORMAT);
        record.recordDay = new Day(columns[7], Day.US_FORMAT);
        record.strike = Double.parseDouble(columns[8]);
        record.bid = Double.parseDouble(columns[10]);
        if (record.bid < Util.ZERO) {
            record.bid = null;
        }
        record.ask = Double.parseDouble(columns[11]);
        if (record.ask < Util.ZERO) {
            record.ask = null;
        }

        return record;
    }

    // https://docs.orats.io/datav2-api-guide/definitions.html
    private static FileRecord parseOratsFileRecord(String[] columns, boolean isCall) {
        // 0 ticker
        // 1 stkPx
        // 2 expirDate
        // 3 yte
        // 4 strike
        // 5 cVolu
        // 6 cOi
        // 7 pVolu
        // 8 pOi
        // 9 cBidPx
        // 10 cValue
        // 11 cAskPx
        // 12 pBidPx
        // 13 pValue
        // 14 pAskPx
        // ...
        // 35 spot_px
        // 36 trade_date
        FileRecord record = new FileRecord();

        record.underlyingSymbol = columns[0];
        record.stockPrice = Double.parseDouble(columns[1]);
        record.expiration = new Day(columns[2], Day.US_FORMAT);
        record.strike = Double.parseDouble(columns[4]);
        record.source = Source.Composite;
        record.isCall = isCall;
        record.optionSymbol = (isCall ? "C" : "P") + " " + record.expiration.toString() + " " + Application.DOUBLE_DIGIT_FORMAT.format(record.stockPrice);
        record.recordDay = new Day(columns[36], Day.US_FORMAT);

        if (isCall) {
            record.bid = Double.parseDouble(columns[9]);
            record.ask = Double.parseDouble(columns[11]);
        } else {
            record.bid = Double.parseDouble(columns[12]);
            record.ask = Double.parseDouble(columns[14]);
        }

        if (record.bid < Util.ZERO) {
            record.bid = null;
        }
        if (record.ask < Util.ZERO) {
            record.ask = null;
        }

        return record;
    }

    public int loadFile(Day refDay, String path) {
        logger.info("Loading file: " + path + " for day:" + refDay);

        int row = -1;
        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(path);
            String[] columns = null;
            while (null != (columns = reader.readRecord())) {
                row++;
                if (0 == row) {
                    // First row is the header, skip it
                    continue;
                }

                if (dataFormat.equals(DataFormat.Default)) {
                    FileRecord record = parseDefaultFileRecord(columns);
                    importRecord(refDay, record);
                } else if (dataFormat.equals(DataFormat.Orats)) {
                    FileRecord callRecord = parseOratsFileRecord(columns, true);
                    FileRecord putRecord = parseOratsFileRecord(columns, false);
                    importRecord(refDay, callRecord);
                    importRecord(refDay, putRecord);
                } else {
                    throw new RuntimeException("Unknown data format: " + dataFormat.name());
                }
            }

        } finally {
            if (null != reader) {
                reader.close();
            }
        }

        return row;
    }

    private void importRecord(Day refDay, FileRecord record) {
        String alias = aliases.get(record.underlyingSymbol);
        if (alias != null) {
            record.underlyingSymbol = alias;
        }

        if ((underliersFilter.size() > 0) && (!underliersFilter.contains(record.underlyingSymbol))) {
            return;
        }

        // Ignore garbage data
        if ((null == record.bid) && (null == record.ask)) {
            return;
        }

        if (!record.recordDay.equals(refDay)) {
            throw new RuntimeException("DATE mismatch: refDay=" + refDay + " recordDay=" + record.recordDay);
        }

        // Only allowed types
        if ((null == record.source) || (!sources.contains(record.source))) {
            return;
        }

        Long underlier = owner.getProcedures().underlierSelect.execute(record.underlyingSymbol);
        if (null == underlier) {
            underlier = owner.getProcedures().sequenceNextVal.execute(Procedures.SQ_UNDERLIERS);
            owner.getProcedures().underlierInsert.execute(underlier, record.underlyingSymbol);
            logger.info("Created underlier: " + record.underlyingSymbol + " with id: " + underlier);
        }

        if (importStockPrice) {
            List<StockPriceRecord> stockPrices = owner.getProcedures().stockPricesSelect.execute(underlier, refDay, refDay);
            if (stockPrices.size() == 0) {
                owner.getProcedures().stockPriceInsert.execute(underlier, refDay, record.stockPrice);
            } else {
                owner.getProcedures().stockPriceUpdate.execute(underlier, refDay, record.stockPrice, null);
            }
        }

        owner.getProcedures().optionPriceInsert.execute(
                underlier,
                record.optionSymbol,
                refDay,
                record.isCall,
                record.strike,
                record.expiration,
                record.bid,
                record.ask);

        importedCount++;
        if (importedCount % BULK_IMPORT_COUNT == 0) {
            owner.commit();
            logger.debug("Imported: " + importedCount + " records");
        }
    }

    private void loadUnderliersFromFile(String filename) {
        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(filename);
            String code = null;
            while (null != (code = reader.readLine())) {
                code = code.trim();
                if (code.length() == 0) {
                    continue;
                }

                underliersFilter.add(code);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}
