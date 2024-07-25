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

package com.aquarians.backtester.positions;

import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.models.VolatilitySurface;
import com.aquarians.aqlib.positions.Strategy;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.pricing.PricingModule;

public class DataValidationStrategy extends StrategyBuilder {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(DataValidationStrategy.class.getSimpleName());

    public static final String TYPE = "DataValidation";

    private final DatabaseModule databaseModule;
    private final PricingModule pricingModule;

    public DataValidationStrategy(PositionsModule owner) {
        super(owner);
        databaseModule = owner.getDatabaseModule();
        pricingModule = owner.getPricingModule();
    }

    @Override
    public Strategy createStrategy() {
        VolatilitySurface surface = pricingModule.getVolatilitySurface();
        if (null == surface) {
            return null;
        }

        VolatilitySurface.StrikeVols term = surface.getFirstTerm();
        if (null == term.forward) {
            return null;
        }

        Double vol = term.interpolate(term.forward);

        databaseModule.getProcedures().stockPriceUpdate.execute(pricingModule.getUnderlier().id, pricingModule.getToday(), term.forward, vol);
        logger.debug("Update day=" + pricingModule.getToday() +
                " und=" + pricingModule.getUnderlier().code +
                " fwd=" + Util.format(term.forward) +
                " vol=" + Util.format(vol != null ? vol * 100.0 : null) + "%");

        return null;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
