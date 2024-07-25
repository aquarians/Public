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

package com.aquarians.backtester.database.records;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.math.PriceRecord;

import java.util.ArrayList;
import java.util.List;

public class StockPriceRecord implements Comparable<StockPriceRecord> {

    public final Day day;
    public final Double open;
    public final Double high;
    public final Double low;
    public final Double close;
    public final Double adjusted;
    public final Double implied;
    public final Long volume;
    public final Double volatility;

    public StockPriceRecord(Day day, Double price) {
        this.day = day;
        this.open = null;
        this.high = null;
        this.low = null;
        this.close = price;
        this.adjusted = null;
        this.implied = null;
        this.volume = null;
        this.volatility = null;
    }

    public StockPriceRecord(Day day, Double open, Double high, Double low, Double close, Double adjusted, Double implied, Long volume, Double volatility) {
        this.day = day;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjusted = adjusted;
        this.implied = implied;
        this.volume = volume;
        this.volatility = volatility;
    }

    @Override
    public String toString() {
        return day.toString() + ": " + Double.toString(close);
    }

    @Override
    public int compareTo(StockPriceRecord that) {
        return this.day.compareTo(that.day);
    }

    public static List<PriceRecord> toPriceRecords(List<StockPriceRecord> stocks) {
        List<PriceRecord> prices = new ArrayList<>(stocks.size());
        for (StockPriceRecord stock : stocks) {
            prices.add(new PriceRecord(stock.day, stock.close));
        }
        return prices;
    }

    public Instrument buildStock(String code) {
        Double price = close;
        if (null == price) {
            return null;
        }

        Instrument instrument = new Instrument(Instrument.Type.STOCK, code, false, null, null);
        instrument.setBidPrice(price);
        instrument.setAskPrice(price);

        return instrument;
    }

}
