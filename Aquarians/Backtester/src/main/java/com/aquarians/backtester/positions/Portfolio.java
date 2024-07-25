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

import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.models.CommissionBuilder;
import com.aquarians.aqlib.models.PricingResult;
import com.aquarians.aqlib.models.VolatilitySurface;
import com.aquarians.aqlib.positions.Position;
import com.aquarians.aqlib.positions.Strategy;
import com.aquarians.aqlib.positions.Trade;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.Procedures;
import com.aquarians.backtester.pricing.PricingModel;
import com.aquarians.backtester.pricing.PricingModule;

import java.util.Map;

public class Portfolio {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(Portfolio.class.getSimpleName());
    
    protected final Strategy strategy;
    protected final Instrument underlier;
    protected final DatabaseModule databaseModule;
    protected final PricingModule pricingModule;
    protected final PricingModel pricingModel;
    
    private int hedgeFrequency = 1;

    // Non null if the position was closed
    private Double realizedProfit = null;
    
    public Portfolio(Strategy strategy,
                     Instrument underlier,
                     DatabaseModule databaseModule,
                     PricingModule pricingModule,
                     PricingModel pricingModel) {
        this.strategy = strategy;
        this.underlier = underlier;
        this.databaseModule = databaseModule;
        this.pricingModule = pricingModule;
        this.pricingModel = pricingModel;
    }

    public void setHedgeFrequency(int hedgeFrequency) {
        this.hedgeFrequency = hedgeFrequency;
    }

    public boolean isExpired() {
        if (realizedProfit != null) {
            return true;
        }

        if (null == strategy.maturityDay) {
            return false;
        }

        return (pricingModel.getToday().compareTo(strategy.maturityDay) >= 0);
    }

    PricingModel getPricingModel() {
        return pricingModel;
    }

    public Double getRealizedProfit() {
        return realizedProfit;
    }
    
    public Long getId() {
        return strategy.id;
    }

    public double getUnderlierPosition() {
        return getInstrumentPosition(underlier.getCode());
    }

    public double getInstrumentPosition(String code) {
        Position position = new Position();

        for (Trade trade : strategy.trades) {
            if ((trade.instrument.getCode().equals(code)) && (!Util.safeEquals(trade.isStatic, true))) {
                position.add(trade.quantity, trade.price);
            }
        }

        return position.getTotalQuantity();
    }

    public void rebalance() {
        // Check if already closed or hedging is disabled
        if (null != realizedProfit) {
            return;
        }

        // Does it ever expire?
        if (null == strategy.maturityDay) {
            return;
        }

        // Expired?
        int remainingDays = pricingModel.getToday().countTradingDays(strategy.maturityDay);
        if (remainingDays < 1) {
            close();
            return;
        }

        // Check if hedging is disabled
        if (!needsRebalancing()) {
            return;
        }

        // HedgePos + OptionPos = 0
        double optionsPos = computeOptionsDeltaPosition();
        double oldHedgePos = getUnderlierPosition();
        double newHedgePos = 0.0 - optionsPos;
        double quantity = (newHedgePos - oldHedgePos);
        if (Math.abs(quantity) > Util.ZERO) {
            Double underlierPrice = underlier.getPrice();
            double tradePrice = (underlierPrice != null) ? underlierPrice : pricingModel.getSpot();
            saveTrade(underlier, quantity, tradePrice, null, false);
        }
    }

    public boolean needsRebalancing() {
        // Check if hedging is disabled
        if (hedgeFrequency < 1) {
            return false;
        }

        int elapsedDays = strategy.executionDay.countTradingDays(pricingModel.getToday());
        boolean isNeeding = (0 == elapsedDays) || (0 == elapsedDays % hedgeFrequency);
        return isNeeding;
    }

    public double computeOptionsDeltaPosition() {
        double total = 0.0;

        for (Trade trade : strategy.trades) {
            if (trade.isStatic) {
                continue;
            }

            if (!(trade.instrument.getType().equals(Instrument.Type.OPTION))) {
                continue;
            }

            PricingResult pricing = pricingModel.price(trade.instrument);
            double delta = pricing.delta * trade.quantity;
            total += delta;
        }

        return total;
    }

    public boolean isClosed() {
        return (realizedProfit != null);
    }

    public void close() {
        if (realizedProfit != null) {
            return;
        }

        realizedProfit = 0.0;

        Map<String, Position> positions = getPositions();
        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            Position position = entry.getValue();

            double quantity = -position.getTotalQuantity();
            if (Math.abs(quantity) > Util.ZERO) {
                PricingResult pricing = pricingModel.price(position.instrument);
                position.add(quantity, pricing.price);
                saveTrade(position.instrument, quantity, pricing.price, null, true);
            }

            double pnl = position.profit();
            realizedProfit += pnl;
        }

        if (null != databaseModule) {
            databaseModule.getProcedures().strategyUpdate.execute(strategy.id, realizedProfit, 0.0, strategy.data);
        }
    }

    protected Map<String, Position> getPositions() {
        return strategy.getPositions();
    }

    public Double evaluateCloseAtMarket() {
        double totalProfit = 0.0;

        Map<String, Position> positions = getPositions();
        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            Position position = entry.getValue();
            Instrument instrument = (pricingModule != null) ? pricingModule.getInstrument(position.instrument.getCode()) : null;
            if (null == instrument) {
                // No liquidity
                return null;
            }

            Double profit = position.evaluateClose(instrument.getBidPrice(), instrument.getAskPrice());
            if (null == profit) {
                // No liquidity
                return null;
            }

            totalProfit += profit;
        }

        return totalProfit;
    }

    public Double evaluateCloseAtTv() {
        double totalProfit = 0.0;

        Map<String, Position> positions = getPositions();
        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            Position position = entry.getValue();
            PricingResult pricingResult = pricingModel.price(position.instrument);
            if ((null == pricingResult) || (null == pricingResult.price)) {
                return null;
            }

            double pnl = position.evaluateClose(pricingResult.price);
            totalProfit += pnl;
        }

        return totalProfit;
    }

    public Double computePnl() {
        return computeNav() - strategy.capital;
    }

    public Double computeNav() {
        if (null == strategy.capital) {
            return null;
        }

        if (null != realizedProfit) {
            return strategy.capital + strategy.realizedPnl + realizedProfit * strategy.multiplier;
        }

        Double mtmProfit = evaluateCloseAtTv();
        if (null == mtmProfit) {
            mtmProfit = 0.0;
        }

        return strategy.capital + strategy.realizedPnl + mtmProfit * strategy.multiplier;
    }

    public void saveTrade(Instrument instrument, double quantity, double price) {
        saveTrade(instrument, quantity, price, null, false);
    }

    public void saveTrade(Instrument instrument, double quantity, double price, String label, Boolean isStatic) {
        if (Math.abs(quantity) < Util.ZERO) {
            return;
        }

        // Only "true" is true, anything else is false
        isStatic = Util.safeEquals(isStatic, true);

        Long id = null;
        Double commission = null;
        if (databaseModule != null) {
            id = databaseModule.getProcedures().sequenceNextVal.execute(Procedures.SQ_TRADES);
            commission = CommissionBuilder.computeCommission(instrument, quantity, price);
            databaseModule.getProcedures().tradeInsert.execute(
                    id,
                    strategy.id,
                    pricingModel.getToday(),
                    instrument.getType().name(),
                    instrument.getCode(),
                    instrument.isCall(),
                    instrument.getMaturity(),
                    instrument.getStrike(),
                    quantity,
                    price,
                    price,
                    commission,
                    label,
                    isStatic);
        }

        Trade trade = new Trade(pricingModel.getToday(), instrument, quantity, price, price);
        trade.id = id;
        trade.commission = commission;
        trade.label = label;
        trade.isStatic = isStatic;
        strategy.trades.add(trade);
    }

    public void saveMtm() {
        if ((null == databaseModule) || (null == pricingModule)) {
            return;
        }

        Double deltaPos = computeOptionsDeltaPosition();

        Double volatility = null;
        VolatilitySurface surface = pricingModule.getVolatilitySurface();
        if ((pricingModel.getSpot() != null) && (surface != null) && (!surface.isEmpty())) {
            volatility = surface.getVolatility(surface.getFirstMaturity(), pricingModel.getSpot());
        }

        Double marketProfit = evaluateCloseAtMarket();
        Double theoreticalProfit = evaluateCloseAtTv();

        Long id = databaseModule.getProcedures().sequenceNextVal.execute(Procedures.SQ_MTMS);
        databaseModule.getProcedures().mtmInsert.execute(
                id,
                strategy.id,
                pricingModel.getToday(),
                deltaPos,
                pricingModel.getSpot(),
                volatility,
                marketProfit,
                theoreticalProfit);
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public Instrument getUnderlier() {
        return underlier;
    }
    
}
