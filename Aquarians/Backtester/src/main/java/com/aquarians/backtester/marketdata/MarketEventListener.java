package com.aquarians.backtester.marketdata;

import com.aquarians.aqlib.Day;

public interface MarketEventListener {

    enum MarketEvent {
        StartOfBatch,
        StartOfDay,
        EndOfDay,
        EndOfBatch
    }

    void processMarketEvent(MarketEvent event, Day day);

}
