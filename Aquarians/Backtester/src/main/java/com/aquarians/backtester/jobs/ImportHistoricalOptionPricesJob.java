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

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportHistoricalOptionPricesJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ImportHistoricalOptionPricesJob.class.getSimpleName());

    private static final long BULK_IMPORT_COUNT = 10000;

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

    private long importedCount = 0;

    public ImportHistoricalOptionPricesJob(DatabaseModule owner) {
        this.owner = owner;
        folder = Application.getInstance().getFolderProperty("ImportHistoricalOptionPricesJob.Folder");
        regex = Application.getInstance().getProperties().getProperty("ImportHistoricalOptionPricesJob.Regex");
        clearPreviousData = Boolean.parseBoolean(Application.getInstance().getProperties().getProperty(
                "ImportHistoricalOptionPricesJob.ClearPreviousData", "false"));

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

    public int loadFile(Day refDay, String path) {
        logger.info("Loading file: " + path + " for day:" + refDay);

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

        Set<Day> maturities = new TreeSet<>();

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

                importedCount++;
                if (importedCount % BULK_IMPORT_COUNT == 0) {
                    owner.commit();
                    logger.debug("Imported: " + importedCount + " records");
                }

                String underlyingSymbol = columns[0];
                String alias = aliases.get(underlyingSymbol);
                if (alias != null) {
                    underlyingSymbol = alias;
                }

                if ((underliersFilter.size() > 0) && (!underliersFilter.contains(underlyingSymbol))) {
                    continue;
                }

                Source source = Source.parseSource(columns[2]);
                String optionSymbol = columns[3];
                String optionType = columns[5].toLowerCase();
                boolean isCall = optionType.equals("call") || optionType.equals("c");
                Day expiration = new Day(columns[6], Day.US_FORMAT);
                Day recordDay = new Day(columns[7], Day.US_FORMAT);
                Double strike = Double.parseDouble(columns[8]);
                Double bid = Double.parseDouble(columns[10]);
                if (bid < Util.ZERO) {
                    bid = null;
                }
                Double ask = Double.parseDouble(columns[11]);
                if (ask < Util.ZERO) {
                    ask = null;
                }

                maturities.add(expiration);

                // Ignore garbage data
                if ((null == bid) && (null == ask)) {
                    continue;
                }

                if (!recordDay.equals(refDay)) {
                    throw new RuntimeException("DATE mismatch: refDay=" + refDay + " recordDay=" + recordDay);
                }

                // Only allowed types
                if ((null == source) || (!sources.contains(source))) {
                    continue;
                }

                Long underlier = owner.getProcedures().underlierSelect.execute(underlyingSymbol);
                if (null == underlier) {
                    continue;
                }

                owner.getProcedures().optionPriceInsert.execute(
                        underlier,
                        optionSymbol,
                        refDay,
                        isCall,
                        strike,
                        expiration,
                        bid,
                        ask);
            }

        } finally {
            if (null != reader) {
                reader.close();
            }
        }

        return row;
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
