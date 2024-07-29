/*
    MIT License

    Copyright (c) 2017 Mihai Bunea

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

package com.aquarians.backtester.database;

import com.aquarians.aqlib.database.DbProcedures;
import com.aquarians.backtester.database.procedures.*;

import java.sql.Connection;

public class Procedures extends DbProcedures {

    public static final String SQ_UNDERLIERS = "sq_underliers";
    public static final String SQ_STRATEGIES = "sq_strategies";
    public static final String SQ_TRADES = "sq_trades";
    public static final String SQ_MTMS = "sq_mtm";

    public final UnderlierSelect underlierSelect;
    public final UnderlierInsert underlierInsert;
    public final UnderlierGet underlierGet;
    public final StockPriceExists stockPriceExists;
    public final StockPriceInsert stockPriceInsert;
    public final StockPriceUpdate stockPriceUpdate;
    public final OptionPriceInsert optionPriceInsert;
    public final OptionPriceBulkInsert optionPriceBulkInsert;
    public final UnderliersSelectAll underliersSelectAll;
    public final StockPricesDeleteAll stockPricesDeleteAll;
    public final StockPricesSelect stockPricesSelect;
    public final OptionPricesSelect optionPricesSelect;
    public final OptionPricesDeleteAll optionPricesDeleteAll;
    public final StockSplitInsert stockSplitInsert;
    public final StockSplitsSelect stockSplitsSelect;
    public final StockDividendInsert stockDividendInsert;
    public final StockDividendsSelect stockDividendsSelect;
    public final ForwardTermsSelect forwardTermsSelect;
    public final StockPricesSelectMinMaxDate stockPricesSelectMinMaxDate;
    public final StrategiesSelectIdsByUnderlier strategiesSelectIdsByUnderlier;
    public final TradesSelectByStrategy tradesSelectByStrategy;
    public final TradeGet tradeGet;
    public final TradesDelete tradesDelete;
    public final StrategyGet strategyGet;
    public final TradeInsert tradeInsert;
    public final TradeUpdateIsStatic tradeUpdateIsStatic;
    public final StrategyInsert strategyInsert;
    public final StrategiesSelectUnderliers strategiesSelectUnderliers;
    public final StrategyUpdate strategyUpdate;
    public final StrategyUpdateData strategyUpdateData;
    public final StrategiesSelectRealizedIds strategiesSelectRealizedIds;
    public final StrategiesSelectTypes strategiesSelectTypes;
    public final StrategiesDelete strategiesDelete;
    public final MtmInsert mtmInsert;
    public final MtmsSelectByStrategy mtmsSelectByStrategy;
    public final MtmGet mtmGet;
    public final MtmDelete mtmDelete;
    public final NavSelectLastDay navSelectLastDay;
    public final NavGetDay navGetDay;
    public final NavInsert navInsert;
    public final NavDelete navDelete;
    public final NavSelectUnderliers navSelectUnderliers;
    public final NavSelectByUnderlier navSelectByUnderlier;
    public final NavSelectByNullUnderlier navSelectByNullUnderlier;
    public final StatisticsInsert statisticsInsert;
    public final StatisticsSelect statisticsSelect;
    public final StatisticsDelete statisticsDelete;

    public Procedures(Connection connection) {
        super(connection);

        underlierSelect = addProcedure(new UnderlierSelect(connection));
        underlierInsert = addProcedure(new UnderlierInsert(connection));
        underlierGet = addProcedure(new UnderlierGet(connection));
        stockPriceExists = addProcedure(new StockPriceExists(connection));
        stockPriceInsert = addProcedure(new StockPriceInsert(connection));
        stockPriceUpdate = addProcedure(new StockPriceUpdate(connection));
        optionPriceInsert = addProcedure(new OptionPriceInsert(connection));
        optionPriceBulkInsert = addProcedure(new OptionPriceBulkInsert(connection));
        underliersSelectAll = addProcedure(new UnderliersSelectAll(connection));
        stockPricesSelect = addProcedure(new StockPricesSelect(connection));
        stockPricesDeleteAll = addProcedure(new StockPricesDeleteAll(connection));
        optionPricesSelect = addProcedure(new OptionPricesSelect(connection));
        optionPricesDeleteAll = addProcedure(new OptionPricesDeleteAll(connection));
        stockSplitInsert = addProcedure(new StockSplitInsert(connection));
        stockSplitsSelect = addProcedure(new StockSplitsSelect(connection));
        stockDividendInsert = addProcedure(new StockDividendInsert(connection));
        stockDividendsSelect = addProcedure(new StockDividendsSelect(connection));
        forwardTermsSelect = addProcedure(new ForwardTermsSelect(connection));
        stockPricesSelectMinMaxDate = addProcedure(new StockPricesSelectMinMaxDate(connection));
        strategiesSelectIdsByUnderlier = addProcedure(new StrategiesSelectIdsByUnderlier(connection));
        tradesSelectByStrategy = addProcedure(new TradesSelectByStrategy(connection));
        tradeGet = addProcedure(new TradeGet(connection));
        tradesDelete = addProcedure(new TradesDelete(connection));
        strategyGet = addProcedure(new StrategyGet(connection));
        tradeInsert = addProcedure(new TradeInsert(connection));
        tradeUpdateIsStatic = addProcedure(new TradeUpdateIsStatic(connection));
        strategyInsert = addProcedure(new StrategyInsert(connection));
        strategiesSelectUnderliers = addProcedure(new StrategiesSelectUnderliers(connection));
        strategyUpdate = addProcedure(new StrategyUpdate(connection));
        strategyUpdateData = addProcedure(new StrategyUpdateData(connection));
        strategiesSelectRealizedIds = addProcedure(new StrategiesSelectRealizedIds(connection));
        strategiesSelectTypes = addProcedure(new StrategiesSelectTypes(connection));
        strategiesDelete = addProcedure(new StrategiesDelete(connection));
        mtmInsert = addProcedure(new MtmInsert(connection));
        mtmsSelectByStrategy = addProcedure(new MtmsSelectByStrategy(connection));
        mtmGet = addProcedure(new MtmGet(connection));
        mtmDelete = addProcedure(new MtmDelete(connection));
        navSelectLastDay = addProcedure(new NavSelectLastDay(connection));
        navGetDay = addProcedure(new NavGetDay(connection));
        navInsert = addProcedure(new NavInsert(connection));
        navDelete = addProcedure(new NavDelete(connection));
        navSelectUnderliers = addProcedure(new NavSelectUnderliers(connection));
        navSelectByUnderlier = addProcedure(new NavSelectByUnderlier(connection));
        navSelectByNullUnderlier = addProcedure(new NavSelectByNullUnderlier(connection));
        statisticsInsert = addProcedure(new StatisticsInsert(connection));
        statisticsSelect = addProcedure(new StatisticsSelect(connection));
        statisticsDelete = addProcedure(new StatisticsDelete(connection));
    }
}
