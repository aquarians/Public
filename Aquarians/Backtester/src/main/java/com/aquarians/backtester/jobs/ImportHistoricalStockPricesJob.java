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
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.Procedures;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First time import from files, imports all the available historical data.
 * This contrasts with EOD import where only last trading day's data is imported.
 */
public class ImportHistoricalStockPricesJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ImportHistoricalStockPricesJob.class.getSimpleName());

    private static final long BULK_IMPORT_COUNT = 10000;

    private final DatabaseModule owner;
    private final String folder;
    private final String regex;
    private Set<String> underliersFilter = new TreeSet<>();

    private long importedCount = 0;

    public ImportHistoricalStockPricesJob(DatabaseModule owner) {
        this.owner = owner;
        folder = Application.getInstance().getFolderProperty("ImportHistoricalStockPricesJob.Folder");
        regex = Application.getInstance().getProperties().getProperty("ImportHistoricalStockPricesJob.Regex");

        String[] underliers = Application.getInstance().getProperties().getProperty("ImportHistoricalStockPricesJob.Underliers" , "").split(",");
        for (String underlier : underliers) {
            underlier = underlier.trim();
            if (underlier.length() == 0) {
                continue;
            }

            underliersFilter.add(underlier);
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
            internalRun();
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
        logger.info("Finished");
    }

    private void internalRun() {
        // List the files
        File fldr = new File(folder);
        File[] files = fldr.listFiles();
        if (null == files) {
            logger.info("No files");
            return;
        }

        // Sort by year
        Pattern pattern = Pattern.compile(regex);
        Map<Integer, String> paths = new TreeMap<>();
        for (File file : files) {
            String name = file.getName();
            Matcher matcher = pattern.matcher(name);
            if (!matcher.find()) {
                continue;
            }

            String yearText = matcher.group(1);
            int year = Integer.parseInt(yearText);
            paths.put(year, file.getPath());
        }

        // Enable bulk operations
        owner.setAutoCommit(false);

        // Go from past to present
        for (Map.Entry<Integer, String> entry : paths.entrySet()) {
            String path = entry.getValue();
            try {
                loadFile(path);
            } catch (Exception ex) {
                logger.warn("Reading file: " + path, ex);
            }
        }

        // Disable bulk operations
        owner.setAutoCommit(true);
    }

    private void loadFile(String path) {
        logger.info("Loading file: " + path);

        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(path);

            String[] columns = null;
            Day prevDay = null;
            while (null != (columns = reader.readRecord())) {
                String symbol = columns[0];
                if ((underliersFilter.size() > 0) && (!underliersFilter.contains(symbol))) {
                    continue;
                }

                Day day = new Day(columns[1], Day.US_FORMAT);
                double open = Double.parseDouble(columns[2]);
                double high = Double.parseDouble(columns[3]);
                double low = Double.parseDouble(columns[4]);
                double close = Double.parseDouble(columns[5]);
                double adjusted = Double.parseDouble(columns[6]);
                long volume = Long.parseLong(columns[7]);

                Long underlier = owner.getProcedures().underlierSelect.execute(symbol);
                if (null == underlier) {
                    underlier = owner.getProcedures().sequenceNextVal.execute(Procedures.SQ_UNDERLIERS);
                    owner.getProcedures().underlierInsert.execute(underlier, symbol);
                    logger.info("Created underlier: " + symbol + " with id: " + underlier);
                }

                if ((null == prevDay) || (!day.equals(prevDay))) {
                    logger.debug("Importing day: " + day);
                    prevDay = day;
                }

                if (!owner.getProcedures().stockPriceExists.execute(underlier, day)) {
                    owner.getProcedures().stockPriceInsert.execute(underlier, day, open, high, low, close, adjusted, volume);
                }

                importedCount++;
                if (importedCount % BULK_IMPORT_COUNT == 0) {
                    owner.commit();
                    logger.debug("Imported: " + importedCount + " records");
                }
            }

        } finally {
            if (null != reader) {
                reader.close();
            }
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
