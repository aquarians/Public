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

package com.aquarians.aqlib.positions;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Strategy {

    public Long id;
    public String type;
    public int number;
    public Double multiplier;
    public Long underlier;
    public Day executionDay;
    public Day maturityDay;
    public Double volatility;
    public Double executionSpot;
    public Double expectedPnlMean;
    public Double expectedPnlDev;
    public double realizedPnl; // Accumulated PNL, historical PNL
    public List<Trade> trades;
    // How much capital was allocated to this strategy (ex: $1000)
    public Double capital;
    // Custom data
    public String data;

    public Map<String, String> customValues = new TreeMap<>();

    public Strategy() {
    }

    public double cost() {
        double total = 0.0;
        for (Trade trade : trades) {
            total += trade.price * trade.quantity;
        }
        return total;
    }

    public double profit() {
        double total = 0.0;
        for (Trade trade : trades) {
            total += trade.profit();
        }
        return total;
    }

    public Double computeCommission() {
        boolean hasCommission = false;
        double total = 0.0;
        for (Trade trade : trades) {
            if (trade.commission != null) {
                total += trade.commission;
                hasCommission = true;
            }
        }

        if (!hasCommission) {
            return null;
        }

        return total;
    }
    public String getTagValue(String tag) {
        Map<String, String> tags = Util.loadTags(data);
        return tags.get(tag);
    }

    public void setTagValue(String tag, String value) {
        Map<String, String> tags = Util.loadTags(data);
        tags.put(tag, value);
        data = Util.saveTags(tags);
    }

    public Position getPosition(String code) {
        Position position = new Position();
        for (Trade trade : trades) {
            if (trade.instrument.getCode().equals(code)) {
                position.add(trade.quantity, trade.price);
            }
        }
        return position;
    }

    public Map<String, Position> getPositions() {
        Map<String, Position> positions = new TreeMap<>();

        for (Trade trade : trades) {
            Position position = positions.get(trade.instrument.getCode());
            if (null == position) {
                position = new Position(trade.instrument);
                positions.put(trade.instrument.getCode(), position);
            }
            position.add(trade.quantity, trade.price);
        }

        return positions;
    }

    public Map<String, Position> getNonZeroPositions() {
        Map<String, Position> positions = getPositions();
        Map<String, Position> nonZeroPositions = new TreeMap<>();

        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            Position position = entry.getValue();
            if (Math.abs(position.getTotalQuantity()) < Util.ZERO) {
                continue;
            }

            nonZeroPositions.put(entry.getKey(), position);
        }

        return nonZeroPositions;
    }

    public void putCustomValue(String tag, double value) {
        customValues.put(tag, Double.toString(value));
    }

    public Double getCustomDoubleValue(String tag) {
        String value = customValues.get(tag);
        if (null == value) {
            return null;
        }

        return Double.parseDouble(value);
    }

}
