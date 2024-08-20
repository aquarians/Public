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

package com.aquarians.backtester.pricing;

import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.models.MonteCarloPricer;
import com.aquarians.aqlib.models.NormalProcess;
import com.aquarians.aqlib.models.PricingResult;

public class MonteCarloPricingModel extends AbstractPricingModel {

    private final NormalProcess process;

    public MonteCarloPricingModel(NormalProcess process) {
        this.process = process;
    }

    @Override
    public Type getType() {
        return Type.MonteCarlo;
    }

    @Override
    public PricingResult price(Instrument instrument) {
        if (instrument.getType().equals(Instrument.Type.STOCK)) {
            return super.price(instrument);
        }

        if (!instrument.getType().equals(Instrument.Type.OPTION)) {
            throw new RuntimeException("Unknown instrument type: " + instrument.getType().name());
        }

        int days = Util.maturity(today, instrument.getMaturity());
        double yf = Util.yearFraction(days);
        MonteCarloPricer pricer = new MonteCarloPricer(process, instrument.isCall(), spot, instrument.getStrike(), yf);

        if (days < 1) {
            return new PricingResult(pricer.valueAtExpiration(), 0.0);
        }

        return new PricingResult(pricer.price(), pricer.delta());
    }
}
