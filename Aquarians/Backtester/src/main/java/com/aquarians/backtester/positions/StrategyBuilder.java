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
import com.aquarians.aqlib.positions.Strategy;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.pricing.PricingModel;
import com.aquarians.backtester.pricing.PricingModule;

import java.util.List;

public abstract class StrategyBuilder {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(StrategyBuilder.class.getSimpleName());

    protected final PositionsModule owner;

    protected final PositionsControl positionsControl;

    // When entering trades sequentially in time, what percent of the total capital to allocate on a trade
    // In percents, 0.5 means 50%
    protected double positionSize = 1.0;

    // If true, will trade floating point quantities
    // If false will trade real (integer) quantities, according to contract size (ex: 1 option contract = 100x underlier)
    protected boolean fractionalTrades = true;

    protected int hedgeFrequency = 0;

    protected StrategyBuilder(PositionsModule owner) {
        this.owner = owner;
        positionsControl = (PositionsControl) Application.getInstance().getModule(Application.buildModuleName(PositionsControl.NAME));
    }

    public abstract Strategy createStrategy();

    public abstract String getType();

    public PositionsModule getOwner() {
        return owner;
    }

    public int getHedgeFrequency() {
        return hedgeFrequency;
    }

    public CapitalAllocationController getCapitalAllocationController() {
        return positionsControl.getCapitalAllocationController(getType());
    }

    public boolean hasTradingCapital() {
        CapitalAllocationController controller = getCapitalAllocationController();
        if (null == controller) {
            return false;
        }

        Long underlier = owner.getPricingModule().getUnderlier().id;
        return controller.hasTradingCapital(underlier);
    }

    public double getTradingCapital() {
        CapitalAllocationController controller = getCapitalAllocationController();
        if (null == controller) {
            return 0.0;
        }

        Long underlier = owner.getPricingModule().getUnderlier().id;
        return controller.getTradingCapital(underlier);
    }

    public static Strategy selectBestStrategy(List<Strategy> strategies) {
        Strategy best = null;
        Double maxRelative = null;

        for (Strategy strategy : strategies) {
            double relative = strategy.expectedPnlMean / strategy.expectedPnlDev;
            if ((null == maxRelative) || (relative > maxRelative)) {
                best = strategy;
                maxRelative = relative;
            }
        }

        return best;
    }

    public void rebalance(Portfolio portfolio) {
        // By default, delta-hedge the position
        portfolio.rebalance();

        // Mark-to-market
        portfolio.saveMtm();

        // Update NAV
        Double nav = portfolio.computeNav();
        if (nav != null) {
            updateCapital(nav);
        } else  {
            logger.warn("Null NAV on day " + portfolio.getPricingModel().getToday());
        }
    }

    public boolean closeExpired(Portfolio portfolio) {
        if (!portfolio.isExpired()) {
            return false;
        }

        portfolio.close();
        freeCapital(portfolio.computeNav());

        return true;
    }

    public void updateCapital(Double amount) {
        CapitalAllocationController controller = getCapitalAllocationController();
        if (null == controller) {
            return;
        }

        Long underlier = owner.getPricingModule().getUnderlier().id;
        controller.updateCapital(underlier, amount);
    }

    public PricingModel getPricingModel() {
        return getPricingModel(null);
    }

    public PricingModel getPricingModel(Strategy strategy) {
        return owner.getPricingModule().getPricingModel();
    }

    public void freeCapital(Double amount) {
        CapitalAllocationController controller = getCapitalAllocationController();
        if (null == controller) {
            return;
        }

        Long underlier = owner.getPricingModule().getUnderlier().id;
        controller.freeCapital(underlier, amount);
    }

    public double getMultiplier(Strategy strategy) {
        double capital = strategy.capital * positionSize;
        double multiplier = capital / strategy.executionSpot;
        if (!fractionalTrades) {
            long multiple = Math.round(multiplier / PositionsModule.CONTRACT_SIZE);
            multiplier = multiple * PositionsModule.CONTRACT_SIZE;
        }
        return multiplier;
    }

    public Strategy allocateCapital(double capital, Strategy strategy) {
        // Allocate capital to the strategy
        strategy.capital = capital;

        // Compute how much we can trade
        strategy.multiplier = getMultiplier(strategy);
        if (strategy.multiplier < Util.ZERO) {
            return null;
        }

        return strategy;
    }

    public boolean hasFractionalTrades() {
        return fractionalTrades;
    }

    public Instrument getUnderlier() {
        return owner.getPricingModule().getStock();
    }

    public DatabaseModule getDatabaseModule() {
        return owner.getDatabaseModule();
    }

    public PricingModule getPricingModule() {
        return owner.getPricingModule();
    }

    public Portfolio createPortfolio(Strategy strategy) {
        return new Portfolio(strategy,
                owner.getPricingModule().getUnderlierInstrument(),
                owner.getDatabaseModule(),
                owner.getPricingModule(),
                getPricingModel());
    }
}
