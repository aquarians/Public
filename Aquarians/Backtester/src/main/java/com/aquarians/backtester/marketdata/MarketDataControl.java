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

import com.aquarians.aqlib.CsvFileReader;
import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Util;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.UnderlierRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MarketDataControl {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(MarketDataControl.class);

    private static MarketDataControl INSTANCE;

    public enum PlaybackMode {
        SingleStep("Single Step"),
        Continuous("Continuous");

        public final String caption;

        PlaybackMode(String caption) {
            this.caption = caption;
        }
    }

    private enum SourceOfUnderliers {
        Database,
        File,
        List
    }

    // For GUI control
    public interface Listener {
        void playbackStarted();
        void playbackRunning(Day day);
        void playbackEnded();
    }

    private final Object lock = new Object();
    private boolean initialized;
    private final Day startDay;
    private final Day endDay;
    private Day currentDay;
    private PlaybackMode playbackMode = PlaybackMode.SingleStep;
    private final Thread processorThread;
    private boolean shutdownRequested;
    private boolean startRequested;
    private boolean nextRequested;
    private boolean stopRequested;
    private List<MarketDataModule> modules = new ArrayList<>();
    private int waitingCount;
    private List<UnderlierRecord> underliers = new ArrayList<>();
    private Listener listener;
    private List<MarketEventListener> marketEventListeners = new ArrayList<>();

    private MarketDataControl() {
        startDay = new Day(Application.getInstance().getProperties().getProperty("MarketData.StartDay"));
        endDay = new Day(Application.getInstance().getProperties().getProperty("MarketData.EndDay"));
        currentDay = startDay;
        processorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    process();
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        }, "MDCTRL");
    }

    public void init() {
        if (initialized) {
            return;
        }

        processorThread.start();
        initialized = true;
    }

    public void cleanup() {
        if (!initialized) {
            return;
        }

        requestShutdown();
        Util.safeJoin(processorThread);
        initialized = false;
    }

    public void requestShutdown() {
        synchronized (lock) {
            shutdownRequested = true;
            lock.notifyAll();
        }
    }

    public void requestNext() {
        synchronized (lock) {
            nextRequested = true;
            lock.notifyAll();
        }
    }

    public Day getStartDay() {
        return startDay;
    }

    public Day getEndDay() {
        return endDay;
    }

    public Day getCurrentDay() {
        synchronized (lock) {
            return currentDay;
        }
    }

    public void setCurrentDay(Day currentDay) {
        synchronized (lock) {
            this.currentDay = currentDay;
        }
    }

    public PlaybackMode getPlaybackMode() {
        synchronized (lock) {
            return playbackMode;
        }
    }

    public void setPlaybackMode(PlaybackMode playbackMode) {
        synchronized (lock) {
            this.playbackMode = playbackMode;
        }
    }


    public void requestStart() {
        synchronized (lock) {
            startRequested = true;
            lock.notifyAll();
        }
    }

    public boolean isStartRequested() {
        synchronized (lock) {
            return startRequested;
        }
    }

    private void process() {
        loadUnderliers();

        while (true) {
            // Wait for signal to start playing market data or to shutdown the application
            synchronized (lock) {
                stopRequested = false;
                nextRequested = false;
                while (!(startRequested || shutdownRequested)) {
                    Util.safeWait(lock);
                }

                if (shutdownRequested) {
                    break;
                }

                // Notify start
                if (null != listener) {
                    listener.playbackStarted();
                }
            }

            iterateDays();

            // Notify end
            synchronized (lock) {
                startRequested = false;
                if (null != listener) {
                    listener.playbackEnded();
                }
            }
        }
    }

    private void iterateDays() {
        if (currentDay.equals(startDay)) {
            notifyMarketEvent(MarketEventListener.MarketEvent.StartOfBatch);
        }

        boolean first = true;
        while (hasMoreDays()) {
            // Increment day
            synchronized (lock) {
                if (!first) {
                    currentDay = currentDay.nextTradingDay();
                } else {
                    first = false;
                }

                // Notify running
                if (null != listener) {
                    listener.playbackRunning(currentDay);
                }
            }

            // Day starts for all underliers
            notifyMarketEvent(MarketEventListener.MarketEvent.StartOfDay);

            // Dispatch market data for each underlier on this day
            dispatch();

            // Day ends for all underliers
            notifyMarketEvent(MarketEventListener.MarketEvent.EndOfDay);
        }

        if (currentDay.equals(endDay)) {
            notifyMarketEvent(MarketEventListener.MarketEvent.EndOfBatch);
        }
    }

    private boolean hasMoreDays() {
        // Check if we reached end of playback or were signalled to stop
        synchronized (lock) {
            return !(stopRequested || shutdownRequested || (currentDay.compareTo(endDay) >= 0));
        }
    }

    private void dispatch() {
        logger.debug("Play day: " + currentDay);

        // Forall underliers
        for (int i = 0; i < underliers.size(); i += modules.size()) {
            // Reset the flag used by modules to acknowledge having processed the data
            synchronized (lock) {
                if (stopRequested || shutdownRequested) {
                    break;
                }
                waitingCount = modules.size();
                logger.debug("Waiting count: " + waitingCount);
            }

            // Dispatch signal to modules to process the current day, splitting the underliers among them
            for (int k = 0; k < modules.size(); k++) {
                MarketDataModule module = modules.get(k);
                int pos = i + k;
                if (pos >= underliers.size()) {
                    synchronized (lock) {
                        waitingCount--;
                        logger.debug("Waiting count: " + waitingCount);
                    }
                    continue;
                }

                UnderlierRecord underlier = underliers.get(pos);
                try {
                    module.requestDispatch(currentDay, underlier);
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }

            // Wait until all modules have processed the signal
            synchronized (lock) {
                while (!((waitingCount <= 0) || stopRequested || shutdownRequested)) {
                    Util.safeWait(lock);
                }
            }

            // In stepping (debug) mode, wait for user input before proceeding further
            if (playbackMode.equals(PlaybackMode.SingleStep)) {
                waitRequestToContinue();
            }
        } // End forall underliers
    }

    private void waitRequestToContinue() {
        synchronized (lock) {
            while (!(nextRequested || stopRequested || shutdownRequested)) {
                Util.safeWait(lock);
            }
            nextRequested = false;
        }
    }

    public void finishedDispatching() {
        synchronized (lock) {
            waitingCount--;
            logger.debug("Waiting count: " + waitingCount);
            lock.notifyAll();
        }
    }

    public void requestStop() {
        synchronized (lock) {
            stopRequested = true;
            lock.notifyAll();
        }
    }

    public void register(MarketDataModule module) {
        synchronized (lock) {
            modules.add(module);
        }
    }

    private void loadUnderliers() {
        // Since here we're not doing multithreaded operations, it doesn't matter which database connection we use
        DatabaseModule databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));

        String text = Application.getInstance().getProperties().getProperty("MarketData.Underliers", "Database");
        String[] components = text.split(",");
        String sourceText = components[0];
        SourceOfUnderliers source = SourceOfUnderliers.valueOf(sourceText);
        switch (source) {
            case Database:
                loadDatabaseUnderliers(databaseModule);
                break;
            case File:
                loadFileUnderliers(databaseModule, components[1]);
                break;
            case List:
                loadListUnderliers(databaseModule, components);
                break;
            default:
                throw new RuntimeException("Unknown source of underliers: " + sourceText);
        }

        ensureUniqueUnderliers();
        logger.info("Loaded " + underliers.size() + " underliers");
    }

    private void loadDatabaseUnderliers(DatabaseModule databaseModule) {
        underliers = databaseModule.getProcedures().underliersSelectAll.execute();
    }

    private void loadFileUnderliers(DatabaseModule databaseModule, String filename) {
        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(filename);
            String code = null;
            while (null != (code = reader.readLine())) {
                Long id = databaseModule.getProcedures().underlierSelect.execute(code);
                if (null != id) {
                    underliers.add(new UnderlierRecord(id, code));
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void loadListUnderliers(DatabaseModule databaseModule, String[] codes) {
        for (int i = 1; i < codes.length; i++) {
            String code = codes[i];
            Long id = databaseModule.getProcedures().underlierSelect.execute(code);
            if (null != id) {
                underliers.add(new UnderlierRecord(id, code));
            }
        }
    }

    public void setListener(Listener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    public void resetListener() {
        synchronized (lock) {
            this.listener = null;
        }
    }

    public static void createInstance() {
        if (null != INSTANCE) {
            return;
        }

        INSTANCE = new MarketDataControl();
    }

    private void ensureUniqueUnderliers() {
        Map<String, UnderlierRecord> underliersMap = new TreeMap<>();
        for (UnderlierRecord underlier : underliers) {
            underliersMap.put(underlier.code, underlier);
        }

        underliers = new ArrayList<>(underliersMap.size());
        for (Map.Entry<String, UnderlierRecord> entry : underliersMap.entrySet()) {
            underliers.add(entry.getValue());
        }
    }

    public static MarketDataControl getInstance() {
        return INSTANCE;
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

    private void notifyMarketEvent(MarketEventListener.MarketEvent event) {
        List<MarketEventListener> listeners = null;
        synchronized (lock) {
            listeners = new ArrayList<>(marketEventListeners);
        }

        for (MarketEventListener listener : listeners) {
            try {
                listener.processMarketEvent(event, currentDay);
            } catch (Exception ex) {
                logger.warn(ex.getMessage(), ex);
            }
        }
    }

}
