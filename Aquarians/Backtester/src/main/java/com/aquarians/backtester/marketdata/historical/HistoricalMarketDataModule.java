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

package com.aquarians.backtester.marketdata.historical;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Period;
import com.aquarians.aqlib.Util;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.OptionPriceRecord;
import com.aquarians.backtester.database.records.StockPriceRecord;
import com.aquarians.backtester.database.records.UnderlierRecord;
import com.aquarians.backtester.marketdata.MarketDataControl;
import com.aquarians.backtester.marketdata.MarketDataListener;
import com.aquarians.backtester.marketdata.MarketDataModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HistoricalMarketDataModule extends MarketDataModule {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(HistoricalMarketDataModule.class);

    public static final String TYPE = "Historical";

    private final Thread processorThread;
    private final DatabaseModule databaseModule;
    private boolean shutdownRequested;
    private boolean dispatchRequested;
    private Day day;
    private UnderlierRecord underlier;
    private final HistoricalDataControl dataControl;

    public HistoricalMarketDataModule(int index) {
        super(index);
        processorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    process();
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        }, "MDATA_" + index);

        Application app = Application.getInstance();
        databaseModule = (DatabaseModule) app.getModule(Application.buildModuleName(DatabaseModule.NAME, index));

        dataControl = (HistoricalDataControl) app.getModule(Application.buildModuleName(MarketDataControl.NAME));
        dataControl.register(this);
    }

    @Override
    public void init() {
        processorThread.start();
    }

    @Override
    public void cleanup() {
        requestShutdown();
        Util.safeJoin(processorThread);
    }

    public void requestShutdown() {
        synchronized (lock) {
            shutdownRequested = true;
            lock.notifyAll();
        }
    }

    public void requestDispatch(Day day, UnderlierRecord underlier) {
        synchronized (lock) {
            this.day = day;
            this.underlier = underlier;
            dispatchRequested = true;
            lock.notifyAll();
        }
    }

    private void process() {
        while (true) {
            Day day;
            UnderlierRecord underlier;
            synchronized (lock) {
                while (!(dispatchRequested || shutdownRequested)) {
                    Util.safeWait(lock);
                }

                if (shutdownRequested) {
                    break;
                }

                day = this.day;
                underlier = this.underlier;
                dispatchRequested = false;
            }

            dispatchUpdate(day, underlier);

            // Notify controller that we finished processing the day
            dataControl.finishedDispatching();
        }
    }

    private void dispatchUpdate(Day day, UnderlierRecord underlier) {
        // Load data from the database
        List<Instrument> instruments = new ArrayList<>();
        loadStock(day, underlier, instruments);
        loadOptions(day, underlier, instruments);
        if (0 == instruments.size()) {
            logger.debug("Dispatching day: " + day + " for underlier: " + underlier.code + " no instruments");
            return;
        }

        // Dispatch to listeners
        logger.debug("Dispatching day: " + day + " for underlier: " + underlier.code + " instruments count: " + instruments.size());
        if (instruments.size() > 0) {
            for (MarketDataListener listener : listeners) {
                try {
                    listener.processMarketDataUpdate(day, underlier, instruments);
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        }
    }

    public void addListener(MarketDataListener listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    public void removeListener(MarketDataListener listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    private boolean loadStock(Day day, UnderlierRecord underlier, List<Instrument> instruments) {
        List<StockPriceRecord> records = databaseModule.getProcedures().stockPricesSelect.execute(underlier.id, day, day);
        if (records.size() == 0) {
            return false;
        }

        StockPriceRecord record = records.get(0);
        Instrument instrument = record.buildStock(underlier.code);
        if (null == instrument) {
            return false;
        }

        instruments.add(instrument);
        return true;
    }

    private int loadOptions(Day day, UnderlierRecord underlier, List<Instrument> instruments) {
        int count = 0;

        List<OptionPriceRecord> records = databaseModule.getProcedures().optionPricesSelect.execute(underlier.id, day, day);

        Set<Day> allowedTerms = null;

        List<Period> configuredTerms = dataControl.getOptionTerms();
        if (configuredTerms.size() > 0) {
            // Allow only the configured terms and exclude the others
            Set<Day> allTerms = new TreeSet<>();
            for (OptionPriceRecord record : records) {
                allTerms.add(record.maturity);
            }

            allowedTerms = new TreeSet<>();
            for (Period period : configuredTerms) {
                Day maturity = day.add(period);
                Day term = Util.getClosestValue(allTerms, maturity, (left, right) -> Math.abs(left.countTradingDays(right)));
                if (term != null) {
                    allowedTerms.add(term);
                }
            }
        }

        for (OptionPriceRecord record : records) {
            Instrument instrument = record.buildOption();
            if (null == instrument) {
                continue;
            }

            if ((allowedTerms != null) && (!allowedTerms.contains(instrument.getMaturity()))) {
                continue;
            }

            instruments.add(instrument);
            count++;
        }

        return count;
    }

}
