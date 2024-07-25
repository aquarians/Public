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

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.positions.Position;
import com.aquarians.aqlib.positions.Strategy;
import com.aquarians.aqlib.positions.Trade;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.procedures.StrategyGet;
import com.aquarians.backtester.database.procedures.TradeGet;
import com.aquarians.backtester.pricing.*;
import com.aquarians.backtester.database.Procedures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PositionsModule implements ApplicationModule, PricingListener {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(PositionsModule.class);

    public static final String NAME = "Positions";

    public static final double CONTRACT_SIZE = 100.0;

    private final int index;
    private final DatabaseModule databaseModule;
    private final PricingModule pricingModule;
    private final boolean autoTrade;
    private final Map<String, StrategyBuilder> strategyBuilders = new TreeMap<>();

    private List<Portfolio> portfolios = new ArrayList<>();

    public PositionsModule(int index) {
        this.index = index;
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, index));
        pricingModule = (PricingModule) Application.getInstance().getModule(Application.buildModuleName(PricingModule.NAME, index));
        autoTrade = Boolean.parseBoolean(Application.getInstance().getProperties().getProperty("Positions.AutoTrade", "false"));

        // Create builders for strategies
        String text = Application.getInstance().getProperties().getProperty("Positions.StrategyBuilders", "");
        String[] types = text.split(",");
        for (String type : types) {
            StrategyBuilder builder = createStrategyBuilder(type);
            if (null != builder) {
                strategyBuilders.put(builder.getType(), builder);
            }
        }
    }

    private StrategyBuilder createStrategyBuilder(String type) {
        return new StrategyBuilderFactory().build(type, this);
    }

    @Override
    public void init() {
        pricingModule.addListener(this);
    }

    @Override
    public void cleanup() {
        pricingModule.removeListener(this);
    }

    @Override
    public String getName() {
        return Application.buildModuleName(NAME, index);
    }

    private void addNewPosition(StrategyBuilder builder) {
        Strategy strategy = builder.createStrategy();
        if (null == strategy) {
            return;
        }

        try {
            if (!addToDatabase(strategy)) {
                builder.freeCapital(null);
                return;
            }

            portfolios.add(builder.createPortfolio(strategy));
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
    }

    private boolean addToDatabase(Strategy strategy) {
        logger.info("AUTOTRADE day=" + pricingModule.getToday() +
                " underlier=" + pricingModule.getUnderlier().code +
                " strategy=" + strategy.number +
                " type=" + strategy.type +
                " maturity=" + strategy.maturityDay +
                " vol=" + Application.DOUBLE_DIGIT_FORMAT.format(strategy.volatility * 100.0) + "%" +
                " spot=" + Application.DOUBLE_DIGIT_FORMAT.format(strategy.executionSpot) +
                " expectedMean=" + Application.FOUR_DIGIT_FORMAT.format(strategy.expectedPnlMean * 100.0) + "%" +
                " expectedDev=" + Application.DOUBLE_DIGIT_FORMAT.format(strategy.expectedPnlDev));

        boolean succeeded = false;
        databaseModule.setAutoCommit(false);
        try {
            strategy.id = databaseModule.getProcedures().sequenceNextVal.execute(Procedures.SQ_STRATEGIES);
            databaseModule.getProcedures().strategyInsert.execute(
                    strategy.id ,
                    strategy.type,
                    strategy.number,
                    strategy.multiplier,
                    strategy.underlier,
                    strategy.executionDay,
                    strategy.maturityDay,
                    strategy.volatility,
                    strategy.executionSpot,
                    strategy.expectedPnlMean,
                    strategy.expectedPnlDev,
                    strategy.capital,
                    strategy.data);

            for (Trade trade : strategy.trades) {
                Long tradeId = databaseModule.getProcedures().sequenceNextVal.execute(Procedures.SQ_TRADES);
                databaseModule.getProcedures().tradeInsert.execute(
                        tradeId,
                        strategy.id,
                        pricingModule.getToday(),
                        trade.instrument.getType().name(),
                        trade.instrument.getCode(),
                        trade.instrument.isCall(),
                        trade.instrument.getMaturity(),
                        trade.instrument.getStrike(),
                        trade.quantity,
                        trade.price,
                        trade.tv,
                        trade.commission,
                        trade.label,
                        trade.isStatic);
            }

            databaseModule.commit();
            succeeded = true;
        } catch (Exception ex) {
            databaseModule.rollback();
            logger.warn(ex.getMessage(), ex);
        } finally {
            databaseModule.setAutoCommit(true);
        }

        return succeeded;
    }

    public DatabaseModule getDatabaseModule() {
        return databaseModule;
    }

    public PricingModule getPricingModule() {
        return pricingModule;
    }

    private void loadPositions() {
        portfolios.clear();

        Long underlier = pricingModule.getUnderlier().id;
        List<Long> ids = databaseModule.getProcedures().strategiesSelectIdsByUnderlier.execute(underlier);
        for (Long id : ids) {
            try {
                loadStrategy(id);
            } catch (Exception ex) {
                logger.warn("Loading strategy " + id, ex);
            }
        }
    }

    private void loadStrategy(Long strategyId) {
        StrategyGet.Record record = databaseModule.getProcedures().strategyGet.execute(strategyId);

        Strategy strategy = new Strategy();
        strategy.id = record.id;
        strategy.type = record.type;
        strategy.number = record.number;
        strategy.multiplier = record.multiplier;
        strategy.underlier = record.underlier;
        strategy.executionDay = record.execution_day;
        strategy.maturityDay = record.maturity_day;
        strategy.volatility = record.volatility;
        strategy.executionSpot = record.execution_spot;
        strategy.expectedPnlMean = record.expected_pnl_mean;
        strategy.expectedPnlDev = record.expected_pnl_dev;
        strategy.realizedPnl = (record.realized_pnl != null) ? record.realized_pnl : 0.0;
        strategy.capital = record.capital;
        strategy.data = record.data;

        List<Long> tradeIds = databaseModule.getProcedures().tradesSelectByStrategy.execute(strategyId);
        strategy.trades = new ArrayList<>(tradeIds.size());
        for (Long tradeId : tradeIds) {
            TradeGet.Record tradeRecord = databaseModule.getProcedures().tradeGet.execute(tradeId);
            Instrument instrument = new Instrument(
                    Instrument.Type.valueOf(tradeRecord.instr_type),
                    tradeRecord.instr_code,
                    tradeRecord.instr_is_call,
                    tradeRecord.instr_maturity,
                    tradeRecord.instr_strike);
            Trade trade = new Trade(tradeRecord.execution_day, instrument, tradeRecord.quantity, tradeRecord.price, tradeRecord.tv);
            trade.id = tradeId;
            trade.commission = tradeRecord.commission;
            trade.label = tradeRecord.label;
            trade.isStatic = Util.safeEquals(tradeRecord.is_static, true);
            strategy.trades.add(trade);
        }

        StrategyBuilder builder = strategyBuilders.get(record.type);
        if (null == builder) {
            logger.warn("Builder not found for strategy " + strategyId);
            return;
        }

        portfolios.add(builder.createPortfolio(strategy));
    }

    private void rebalancePositions() {
        for (Portfolio portfolio : portfolios) {
            try {
                StrategyBuilder builder = strategyBuilders.get(portfolio.getStrategy().type);
                builder.rebalance(portfolio);
            } catch (Exception ex) {
                logger.warn("Day: " + pricingModule.getToday() +
                        " Underlier: " + pricingModule.getUnderlier().code +
                        " Portfolio: " + portfolio.getId(), ex);
            }
        }
    }

    private void closeExpiredPositions() {
        List<Portfolio> expired = new ArrayList<>(portfolios.size());
        for (Portfolio portfolio : portfolios) {
            try {
                if (closeExpired(portfolio)) {
                    expired.add(portfolio);
                }
            } catch (Exception ex) {
                logger.warn("Day: " + pricingModule.getToday() +
                        " Underlier: " + pricingModule.getUnderlier().code +
                        " Position: " + portfolio.getId(), ex);
            }
        }

        // Remove those who expired
        for (Portfolio portfolio : expired) {
            portfolios.remove(portfolio);
        }
    }

    private boolean closeExpired(Portfolio portfolio) {
        boolean expired = false;
        try {
            StrategyBuilder builder = strategyBuilders.get(portfolio.getStrategy().type);
            expired = builder.closeExpired(portfolio);
        } catch (Exception ex) {
            logger.warn("Day: " + pricingModule.getToday() +
                    " Underlier: " + pricingModule.getUnderlier().code +
                    " Position: " + portfolio.getId(), ex);
        }

        return expired;
    }

    private void addNewPositions() {
        for (StrategyBuilder builder : strategyBuilders.values()) {
            try {
                addNewPosition(builder);
            } catch (Exception ex) {
                logger.warn("Day: " + pricingModule.getToday() +
                        " Underlier: " + pricingModule.getUnderlier().code +
                        " Strategy: " + builder.getType(), ex);
            }
        }
    }

    @Override
    public void processPricingUpdate() {
        if (!autoTrade) {
            return;
        }

        loadPositions();
        closeExpiredPositions();
        addNewPositions();
        rebalancePositions();
        closeExpiredPositions(); // Rebalancing may cause additional expiries
    }
}
