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

package com.aquarians.aqlib;

public class OptionPair implements Comparable<OptionPair> {

    public final Double strike;
    public Instrument call;
    public Instrument put;

    public OptionPair(Double strike) {
        this.strike = strike;
    }

    @Override
    public int compareTo(OptionPair that) {
        return this.strike.compareTo(that.strike);
    }

    public void set(Instrument instrument) {
        if (instrument.isCall()) {
            call = instrument;
        } else {
            put = instrument;
        }
    }

    public boolean isEmpty() {
        Double callPrice = (call != null) ? call.getPrice() : null;
        Double putPrice = (put != null) ? put.getPrice() : null;
        if ((callPrice != null) || (putPrice != null)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        String text = Double.toString(strike);
        if ((null != call) && (call.getPrice() != null)) {
            text += " C: " + call.getPrice();
        }
        if ((null != put) && (put.getPrice() != null)) {
            text += " P: " + put.getPrice();
        }
        return text;
    }

    public Double getImpliedSpot() {
        if ((null == call) || (null == put)) {
            return null;
        }

        // Bounds for spot price
        Double spotLow = Util.getParitySpotLowerBound(call, put);
        Double spotHigh = Util.getParitySpotUpperBound(call, put);
        if ((null == spotLow) || (null == spotHigh)) {
            return null;
        }

        double impliedSpot = (spotLow + spotHigh) / 2.0;
        return impliedSpot;
    }

    public boolean hasFullSpread() {
        // Both call and put must have valid bid and ask
        Double callSpread = Util.getSafeSpread(call);
        Double putSpread = Util.getSafeSpread(put);
        return (null != callSpread) && (null != putSpread);
    }

    public OptionPair clone() {
        OptionPair clone = new OptionPair(strike);
        clone.call = (this.call != null) ? this.call.clone() : null;
        clone.put = (this.put != null) ? this.put.clone() : null;
        return clone;
    }

}
