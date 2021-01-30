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

package com.aquarians.backtester.marketdata;

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.OptionPriceRecord;
import com.aquarians.backtester.database.records.StockPriceRecord;
import com.aquarians.backtester.database.records.UnderlierRecord;

import java.util.ArrayList;
import java.util.List;

public class MarketDataModule implements ApplicationModule {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(MarketDataModule.class);

    public static final String NAME = "MarketData";

    private final int index;
    private final Object lock = new Object();
    private final Thread processorThread;
    private final DatabaseModule databaseModule;
    private boolean shutdownRequested;
    private boolean dispatchRequested;
    private Day day;
    private UnderlierRecord underlier;
    private List<MarketDataListener> listeners = new ArrayList<>();

    public MarketDataModule(int index) {
        this.index = index;
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
        // Because we do multithreaded operations, each market data module works with it's corresponding database connection
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, index));

        // Create singleton
        MarketDataControl.createInstance();
    }

    @Override
    public void init() {
        // Init singleton
        MarketDataControl.getInstance().init();

        MarketDataControl.getInstance().register(this);
        processorThread.start();
    }

    @Override
    public void cleanup() {
        requestShutdown();
        Util.safeJoin(processorThread);

        // Cleanup singleton
        MarketDataControl.getInstance().cleanup();
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

    @Override
    public String getName() {
        return Application.buildModuleName(NAME, index);
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
            MarketDataControl.getInstance().finishedDispatching();
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

        Double price = record.close;
        if (null == price) {
            return false;
        }

        Instrument instrument = new Instrument(Instrument.Type.STOCK, underlier.code, false, null, null);
        instrument.setBidPrice(price);
        instrument.setAskPrice(price);

        instruments.add(instrument);
        return true;
    }

    private int loadOptions(Day day, UnderlierRecord underlier, List<Instrument> instruments) {
        int count = 0;
        List<OptionPriceRecord> records = databaseModule.getProcedures().optionPricesSelect.execute(underlier.id, day, day);
        for (OptionPriceRecord record : records) {
            // Ignore if expiring today
            if (record.maturity.compareTo(day) <= 0) {
                continue;
            }

            Day maturity = record.maturity;
            if (null != maturity) {
                // Roll to previous trading day
                maturity = maturity.rollToTradingDay(false);
            }

            Instrument instrument = new Instrument(Instrument.Type.OPTION, record.code, record.is_call, maturity, record.strike);
            instrument.setBidPrice(record.bid);
            instrument.setAskPrice(record.ask);
            if ((instrument.getBidPrice() != null) && (instrument.getBidPrice() < Util.ZERO)) {
                instrument.setBidPrice(null);
            }
            if ((instrument.getAskPrice() != null) && (instrument.getAskPrice() < Util.ZERO)) {
                instrument.setAskPrice(null);
            }
            if (null != instrument.getPrice()) {
                instruments.add(instrument);
                count++;
            }
        }
        return count;
    }

}
