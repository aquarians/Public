package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.CsvFileReader;
import com.aquarians.aqlib.Day;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.Procedures;

import java.io.File;

public class ImportYahooStockPricesJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ImportYahooStockPricesJob.class.getSimpleName());

    private static final long BULK_IMPORT_COUNT = 100000;

    private final DatabaseModule owner;
    private final String folder;

    private long importedCount = 0;

    public ImportYahooStockPricesJob(DatabaseModule owner) {
        this.owner = owner;
        folder = Application.getInstance().getFolderProperty("ImportYahooStockPricesJob.Folder");
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

        // Enable bulk operations
        owner.setAutoCommit(false);

        // Go from past to present
        for (File file : files) {
            try {
                loadFile(file.getPath(), file.getName());
            } catch (Exception ex) {
                logger.warn("Reading file: " + file.getPath(), ex);
            }
        }

        // Disable bulk operations
        owner.setAutoCommit(true);
    }

    private void loadFile(String path, String symbol) {
        symbol = symbol.replace(".csv", "");
        logger.info("Loading file: " + path + ", symbol: " + symbol);

        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(path);

            //Date,Open,High,Low,Close,Adj Close,Volume
            //1986-01-06,13053.790039,13053.790039,13053.790039,13053.790039,13053.790039,0
            String[] columns = null;
            Day prevDay = null;
            int line = 0;
            while (null != (columns = reader.readRecord())) {
                line++;

                try {
                    Day day = new Day(columns[0], Day.FORMAT_YYYY_MM_DD);
                    double open = Double.parseDouble(columns[1]);
                    double high = Double.parseDouble(columns[2]);
                    double low = Double.parseDouble(columns[3]);
                    double close = Double.parseDouble(columns[4]);
                    double adjusted = Double.parseDouble(columns[5]);
                    long volume = Long.parseLong(columns[6]);

                    Long underlier = owner.getProcedures().underlierSelect.execute(symbol);
                    if (null == underlier) {
                        underlier = owner.getProcedures().sequenceNextVal.execute(Procedures.SQ_UNDERLIERS);
                        owner.getProcedures().underlierInsert.execute(underlier, symbol);
                        logger.info("Created underlier: " + symbol + " with id: " + underlier);
                    }

                    if ((null == prevDay) || (!day.equals(prevDay))) {
                        logger.debug("Importing day: " + day + ", symbol: " + symbol);
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
                } catch (Exception ex) {
                    logger.debug("Importing symbol " + symbol + " : ignoring line " + line);
                }
            }

        } finally {
            if (null != reader) {
                reader.close();
            }
        }
    }


}
