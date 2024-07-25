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

import com.aquarians.backtester.marketdata.historical.HistoricalDataControl;
import com.aquarians.backtester.marketdata.historical.HistoricalMarketDataModule;

public class MarketDataModuleFactory {

    private static final MarketDataModuleFactory INSTANCE = new MarketDataModuleFactory();

    private MarketDataModuleFactory() {

    }

    public static MarketDataModuleFactory getInstance() {
        return INSTANCE;
    }

    public MarketDataModule buildMarketDataModule(String type, int index) {
        if (type.equals(HistoricalMarketDataModule.TYPE)) {
            return new HistoricalMarketDataModule(index);
        }

        throw new RuntimeException("Unknown market data module type: " + type);
    }

    public MarketDataControl buildMarketDataControl(String type) {
        if (type.equals(HistoricalMarketDataModule.TYPE)) {
            return new HistoricalDataControl();
        }

        throw new RuntimeException("Unknown market data control type: " + type);
    }
}
