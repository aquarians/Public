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

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportStockSplitsJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ImportStockSplitsJob.class);

    private static final Pattern SPLIT_PATTERN = Pattern.compile("([0-9]*\\.?[0-9]+)[ ]*[a-zA-Z]+[ ]*([0-9]*\\.?[0-9]+)");
    private static final double TOLERANCE = 0.01;

    private final DatabaseModule databaseModule;
    private final String file;

    public ImportStockSplitsJob(DatabaseModule databaseModule) {
        this.databaseModule = databaseModule;
        file = Application.getInstance().getProperties().getProperty("ImportStockSplitsJob.File");
    }

    @Override
    public void run() {
        logger.info("STOCK SPLITS IMPORT running");

        CsvFileReader reader = new CsvFileReader(file);
        String[] records = null;
        while (null != (records = reader.readRecord())) {
            if (records.length < 4) {
                logger.warn("INVALID FORMAT: " + Util.toString(Arrays.asList(records)));
                continue;
            }

            String underlierCode = records[0];
            Day day = new Day(records[1], Day.US_FORMAT);

            // ex: 0.17 for a "1 for 6" split
            String ratioText = records[2];

            // ex: "1 for 6"
            String splitText = records[3];

            double ratio = Double.parseDouble(ratioText);
            Double split = parseSplit(splitText);
            if (null == split) {
                logger.warn("UNKNOWN FORMAT: " + underlierCode + " day=" + day + " text=[" + splitText + "]");
                split = 1.0;
            }

            // Verify that ratio rounds to roundedRatio
            if (Math.abs(ratio - split) > TOLERANCE) {
                logger.warn("MISMATCH: " + underlierCode + " day=" + day + " ratio=" + Util.round(ratio, 4) + " split=" + Util.round(split, 4));
            }

            // Use whatever is more different from 1
            double diffRatio = Math.abs(ratio - 1.0);
            double diffSplit = Math.abs(split - 1.0);
            double value = (diffRatio > diffSplit) ?  ratio : split;

            try {
                process(underlierCode, day, value);
            } catch (Exception ex) {
                logger.warn(underlierCode + " " + day, ex);
            }
        }
    }

    private static Double parseSplit(String split) {
        Matcher matcher = SPLIT_PATTERN.matcher(split);
        if (!matcher.find()) {
            return null;
        }

        String fromText = matcher.group(1);
        String toText = matcher.group(2);

        double from = Double.parseDouble(fromText);
        double to = Double.parseDouble(toText);
        if (to < 0.01) {
            to = 1.0;
        }
        double ratio = from / to;
        return ratio;
    }

    private void process(String underlierCode, Day day, double ratio) {
        Long underlierId = databaseModule.getProcedures().underlierSelect.execute(underlierCode);
        if (null == underlierId) {
            return;
        }

        Map<Day, Double> splits = databaseModule.getProcedures().stockSplitsSelect.execute(underlierId, day, day);
        if (splits.size() > 0) {
            return;
        }

        databaseModule.getProcedures().stockSplitInsert.execute(underlierId, day, ratio);
        logger.info("Inserted split for " + underlierCode + " on " + day + " at " + Util.round(ratio, 4));
    }
}
