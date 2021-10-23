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

    public final UnderlierSelect underlierSelect;
    public final UnderlierInsert underlierInsert;
    public final UnderlierGet underlierGet;
    public final StockPriceExists stockPriceExists;
    public final StockPriceInsert stockPriceInsert;
    public final StockPriceUpdate stockPriceUpdate;
    public final OptionPriceInsert optionPriceInsert;
    public final UnderliersSelectAll underliersSelectAll;
    public final StockPricesDeleteAll stockPricesDeleteAll;
    public final StockPricesSelect stockPricesSelect;
    public final OptionPricesSelect optionPricesSelect;
    public final OptionPricesDeleteAll optionPricesDeleteAll;
    public final StockSplitInsert stockSplitInsert;
    public final StockSplitsSelect stockSplitsSelect;
    public final ForwardTermsSelect forwardTermsSelect;

    public Procedures(Connection connection) {
        super(connection);

        underlierSelect = addProcedure(new UnderlierSelect(connection));
        underlierInsert = addProcedure(new UnderlierInsert(connection));
        underlierGet = addProcedure(new UnderlierGet(connection));
        stockPriceExists = addProcedure(new StockPriceExists(connection));
        stockPriceInsert = addProcedure(new StockPriceInsert(connection));
        stockPriceUpdate = addProcedure(new StockPriceUpdate(connection));
        optionPriceInsert = addProcedure(new OptionPriceInsert(connection));
        underliersSelectAll = addProcedure(new UnderliersSelectAll(connection));
        stockPricesSelect = addProcedure(new StockPricesSelect(connection));
        stockPricesDeleteAll = addProcedure(new StockPricesDeleteAll(connection));
        optionPricesSelect = addProcedure(new OptionPricesSelect(connection));
        optionPricesDeleteAll = addProcedure(new OptionPricesDeleteAll(connection));
        stockSplitInsert = addProcedure(new StockSplitInsert(connection));
        stockSplitsSelect = addProcedure(new StockSplitsSelect(connection));
        forwardTermsSelect = addProcedure(new ForwardTermsSelect(connection));
    }
}
