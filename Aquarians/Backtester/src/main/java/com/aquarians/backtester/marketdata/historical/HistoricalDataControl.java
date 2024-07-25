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
import com.aquarians.aqlib.Util;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.records.UnderlierRecord;
import com.aquarians.backtester.marketdata.MarketDataControl;
import sun.security.krb5.internal.APRep;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HistoricalDataControl extends MarketDataControl implements GuiDataControl {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(HistoricalDataControl.class);

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
    private List<HistoricalMarketDataModule> modules = new ArrayList<>();
    private int waitingCount;
    private Listener listener;

    public HistoricalDataControl() {
        startDay = new Day(properties.getProperty("MarketData.StartDay"));
        endDay = new Day(properties.getProperty("MarketData.EndDay"));
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

    private void loadUnderliers() {
        List<String> codes = loadUnderlierCodes();
        underliers = new ArrayList<>(codes.size());
        for (String code : codes) {
            Long id = databaseModule.getProcedures().underlierSelect.execute(code);
            if (id != null) {
                underliers.add(new UnderlierRecord(id, code));
            }
        }
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
            notifyMarketEvent(MarketEventListener.MarketEvent.StartOfBatch, currentDay);
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
            notifyMarketEvent(MarketEventListener.MarketEvent.StartOfDay, currentDay);

            // Dispatch market data for each underlier on this day
            dispatch();

            // Day ends for all underliers
            notifyMarketEvent(MarketEventListener.MarketEvent.EndOfDay, currentDay);
        }

        if (currentDay.equals(endDay)) {
            notifyMarketEvent(MarketEventListener.MarketEvent.EndOfBatch, currentDay);
        }
    }

    private boolean hasMoreDays() {
        // Check if we reached end of playback or were signalled to stop
        synchronized (lock) {
            return !(stopRequested || shutdownRequested || (currentDay.compareTo(endDay) > 0));
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
                HistoricalMarketDataModule module = modules.get(k);
                int pos = i + k;

                // At the very end of the list we may have more processing threads than remaining underliers to process
                if (pos >= underliers.size()) {
                    finishedDispatching();
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

    public void register(HistoricalMarketDataModule module) {
        synchronized (lock) {
            modules.add(module);
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

    @Override
    protected List<String> loadDatabaseUnderliers() {
        List<UnderlierRecord> records = databaseModule.getProcedures().underliersSelectAll.execute();
        Set<String> codes = new TreeSet<>();
        for (UnderlierRecord record : records) {
            codes.add(record.code);
        }
        return new ArrayList<>(codes);
    }
}
