/*
    MIT License

    Copyright (c) 2024 Mihai Bunea

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

package com.aquarians.backtester.marketdata;

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.aqlib.CsvFileReader;
import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Period;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.UnderlierRecord;
import com.aquarians.backtester.marketdata.historical.MarketEventListener;

import java.util.*;

public abstract class MarketDataControl implements ApplicationModule {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(MarketDataControl.class);

    public static final String NAME = "MarketDataControl";

    private enum SourceOfUnderliers {
        Database,
        File,
        List
    }

    protected final Object lock = new Object();
    protected final Properties properties;

    protected final DatabaseModule databaseModule;

    protected List<UnderlierRecord> underliers = new ArrayList<>();
    protected List<MarketEventListener> marketEventListeners = new ArrayList<>();

    protected final List<Period> optionTerms = new ArrayList<>();

    public MarketDataControl() {
        properties = Application.getInstance().getProperties();

        // Get the global database module (with index zero)
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));

        loadOptionTerms();
    }

    public DatabaseModule getDatabaseModule() {
        return databaseModule;
    }

    public List<String> loadUnderlierCodes() {
        String text = Application.getInstance().getProperties().getProperty("MarketData.Underliers", "Database");
        String[] components = text.split(",");
        String sourceText = components[0];
        SourceOfUnderliers source = SourceOfUnderliers.valueOf(sourceText);
        switch (source) {
            case Database:
                return loadDatabaseUnderliers();
            case File:
                return loadFileUnderliers(components[1]);
            case List:
                return loadListUnderliers(components);
            default:
                break;
        }

        throw new RuntimeException("Unknown source of underliers: " + sourceText);
    }

    protected abstract List<String> loadDatabaseUnderliers();

    protected List<String> loadFileUnderliers(String filename) {
        Set<String> codes = new TreeSet<>();

        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(filename);
            String code = null;
            String[] record;
            while (null != (record = reader.readRecord())) {
                if (record.length < 1) {
                    continue;
                }

                code = record[0].trim();
                if (!code.isEmpty() && !code.startsWith("#")) {
                    codes.add(code);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return new ArrayList<>(codes);
    }

    protected List<String> loadListUnderliers(String[] codes) {
        Set<String> codesSet = new TreeSet<>();

        for (int i = 1; i < codes.length; i++) {
            String code = codes[i].trim();
            if (!code.isEmpty()) {
                codesSet.add(code);
            }
        }

        return new ArrayList<>(codesSet);
    }

    public void addMarketEventListener(MarketEventListener listener) {
        synchronized (lock) {
            marketEventListeners.add(listener);
        }
    }

    public void removeMarketEventListener(MarketEventListener listener) {
        synchronized (lock) {
            marketEventListeners.remove(listener);
        }
    }

    protected void notifyMarketEvent(MarketEventListener.MarketEvent event, Day day) {
        List<MarketEventListener> listeners = null;
        synchronized (lock) {
            listeners = new ArrayList<>(marketEventListeners);
        }

        for (MarketEventListener listener : listeners) {
            try {
                listener.processMarketEvent(event, day);
            } catch (Exception ex) {
                logger.warn(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public String getName() {
        return Application.buildModuleName(NAME);
    }

    private void loadOptionTerms() {
        String text = properties.getProperty("MarketData.OptionTerms", "").trim();
        if (text.isEmpty()) {
            return;
        }

        StringTokenizer tokenizer = new StringTokenizer(text, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            try {
                Period period = new Period(token);
                optionTerms.add(period);
            } catch (Exception ex) {
                logger.warn("Unrecognized period: " + token);
            }
        }
    }

    public List<Period> getOptionTerms() {
        return optionTerms;
    }
}
