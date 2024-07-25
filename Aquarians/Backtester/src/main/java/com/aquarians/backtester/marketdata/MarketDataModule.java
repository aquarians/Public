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
import com.aquarians.backtester.Application;

import java.util.ArrayList;
import java.util.List;

public abstract class MarketDataModule implements ApplicationModule {

    public static final String NAME = "MarketData";

    protected final Object lock = new Object();
    protected final int index;
    protected List<MarketDataListener> listeners = new ArrayList<>();

    public MarketDataModule(int index) {
        this.index = index;
    }

    @Override
    public String getName() {
        return Application.buildModuleName(NAME, index);
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
}
