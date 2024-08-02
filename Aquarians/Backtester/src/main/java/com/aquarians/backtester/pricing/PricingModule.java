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

package com.aquarians.backtester.pricing;

import com.aquarians.aqlib.*;
import com.aquarians.aqlib.models.VolatilitySurface;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.UnderlierRecord;
import com.aquarians.backtester.marketdata.MarketDataListener;
import com.aquarians.backtester.marketdata.MarketDataModule;

import java.util.*;

public class PricingModule implements ApplicationModule, MarketDataListener {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(PricingModule.class);

    public static final String NAME = "Pricing";

    private final Object lock = new Object();
    private int index;
    private final MarketDataModule marketDataModule;

    private List<PricingListener> listeners = new ArrayList<>();

    private final DatabaseModule databaseModule;
    private final TreeMap<Day, OptionTerm> optionTerms = new TreeMap<>();
    private PricingModel.Type activeModel;
    private final double borrowRate;
    private final boolean validatePrices;
    private final boolean ensureFullSpread;
    private List<PricingModel> pricingModels = new ArrayList<>();
    private Map<String, Instrument> instruments = new TreeMap<>();
    private Map<Day, Double> interestRates = new HashMap<>();

    private Day today;
    private UnderlierRecord underlier;
    private Instrument stock;

    public PricingModule(int index) {
        this.index = index;
        marketDataModule = (MarketDataModule) Application.getInstance().getModule(Application.buildModuleName(MarketDataModule.NAME, index));

        // Because we do multithreaded operations, each pricing module needs other modules of the same index
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, index));
        activeModel = PricingModel.Type.valueOf(Application.getInstance().getProperties().getProperty("Pricing.ActiveModel", PricingModel.Type.Market.name()));
        validatePrices = Boolean.parseBoolean(Application.getInstance().getProperties().getProperty("Pricing.ValidatePrices", "false"));
        ensureFullSpread = Boolean.parseBoolean(Application.getInstance().getProperties().getProperty("Pricing.EnsureFullSpread", "false"));
        borrowRate = Double.parseDouble(Application.getInstance().getProperties().getProperty("Pricing.BorrowRate", "0"));

        loadInterestRates();
        createPricingModels();
    }

    private void createPricingModels() {
        PricingModelFactory factory = new PricingModelFactory();
        String[] types = Application.getInstance().getProperties().getProperty("Pricing.Models", "").split(",");
        for (String type : types) {
            type = type.trim();
            PricingModel model = factory.build(type, this);
            if (model != null) {
                pricingModels.add(model);
            }
        }
    }

    @Override
    public void init() {
        marketDataModule.addListener(this);
    }

    @Override
    public void cleanup() {
        marketDataModule.removeListener(this);
    }

    @Override
    public String getName() {
        return Application.buildModuleName(NAME, index);
    }

    public void processMarketDataUpdate(Day day, UnderlierRecord underlier, List<Instrument> instruments) {
        this.today = day;
        this.underlier = underlier;
        addInstruments(instruments);
        recalculateAndNotify();
    }

    @Override
    public void quotesUpdated() {
        recalculateAndNotify();
    }

    private void recalculateAndNotify() {
        fitModels();
        notifyListeners();

        // HACK
        logger.debug("getMaxParityArbitrageReturn=" + Util.format(getMaxParityArbitrageReturn()));
        logger.debug("getMaxOptionArbitrageReturn=" + Util.format(getMaxOptionArbitrageReturn()));
    }

    public Instrument getInstrument(String code) {
        return instruments.get(code);
    }

    public void addListener(PricingListener listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    public void removeListener(PricingListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        List<PricingListener> listeners;
        synchronized (lock) {
            if (this.listeners.size() == 0) {
                return;
            }
            listeners = new ArrayList<>(this.listeners);
        }

        for (PricingListener listener : listeners) {
            try {
                listener.processPricingUpdate();
            } catch (Exception ex) {
                logger.warn(ex.getMessage(), ex);
            }
        }
    }

    private void addInstruments(List<Instrument> instruments) {
        this.instruments.clear();
        stock = null;
        optionTerms.clear();

        for (Instrument instrument : instruments) {
            this.instruments.put(instrument.getCode(), instrument);

            if (instrument.getType().equals(Instrument.Type.STOCK)) {
                stock = instrument;
            } else if (instrument.getType().equals(Instrument.Type.OPTION)) {
                OptionTerm term = optionTerms.get(instrument.getMaturity());
                if (null == term) {
                    term = new OptionTerm(this, today, instrument.getMaturity());
                    optionTerms.put(instrument.getMaturity(), term);
                }

                term.add(instrument);
            }
        }

        validatePrices();
        ensureFullSpread();
    }

    void ensureFullSpread() {
        if (!ensureFullSpread) {
            return;
        }

        for (OptionTerm term : optionTerms.values()) {
            term.ensureFullSpread();
        }
    }

    private void validatePrices() {
        if (!validatePrices) {
            return;
        }

        for (OptionTerm term : optionTerms.values()) {
            term.validatePrices();
        }
    }

    public Day getToday() {
        return today;
    }

    public UnderlierRecord getUnderlier() {
        return underlier;
    }

    public Instrument getUnderlierInstrument() {
        return getStock();
    }

    public PricingModel getPricingModel() {
        for (PricingModel model : pricingModels) {
            if (model.getType().equals(activeModel)) {
                return model;
            }
        }

        return null;
    }

    private void fitModels() {
        for (PricingModel model : pricingModels) {
            try {
                model.fit();
            } catch (Exception ex) {
                logger.warn("Underlier: " + underlier.code + " day: " + today + " model: " + model.getType(), ex);
            }
        }
    }

    public TreeMap<Day, OptionTerm> getOptionTerms() {
        return optionTerms;
    }

    public Double getSpotPrice() {
        if (null == stock) {
            return null;
        }

        return stock.getPrice();
    }

    public DatabaseModule getDatabaseModule() {
        return databaseModule;
    }

    public VolatilitySurface getVolatilitySurface() {
        PricingModel model = getPricingModel();
        if ((null != model) && (null != model.getSurface())) {
            return model.getSurface();
        }

        return null;
    }

    public PricingModel getPricingModel(PricingModel.Type type) {
        for (PricingModel model : pricingModels) {
            if (model.getType().equals(type)) {
                return model;
            }
        }

        return null;
    }

    public Instrument getStock() {
        return stock;
    }

    OptionTerm findClosestTerm(int maturity) {
        Integer minDistance = null;
        OptionTerm selectedTerm = null;
        for (Map.Entry<Day, OptionTerm> entry : optionTerms.entrySet()) {
            OptionTerm term = entry.getValue();
            int distance = Math.abs(term.daysToExpiry - maturity);
            if ((null == minDistance) || (distance < minDistance)) {
                minDistance = distance;
                selectedTerm = entry.getValue();
            }
        }
        return selectedTerm;
    }

    private void loadInterestRates() {
        String file = Application.getInstance().getProperties().getProperty("Pricing.Rates.File");
        if (null == file) {
            return;
        }

        Map<Day, Double> rates = new HashMap<>();
        Day startDay = null;
        Day endDay = null;

        CsvFileReader reader = null;
        try {
            reader = new CsvFileReader(file);

            String[] record;
            int line = 0;
            while (null != (record = reader.readRecord())) {
                line++;

                // Header: date,value
                if (1 == line) {
                    continue;
                }

                // Example: 2001-01-01,5.4100
                if (record.length < 2) {
                    continue;
                }

                try {
                    Day day = new Day(record[0], Day.FORMAT_YYYY_MM_DD);
                    Double rate = Double.parseDouble(record[1]) / 100.0;
                    if (null == startDay) {
                        startDay = day;
                    }
                    endDay = day;
                    rates.put(day, rate);
                } catch (Exception ex) {
                    logger.warn("Line " + line + ": " + Arrays.toString(record), ex);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        // Some days might be missing, fill them with the previous day's rate
        if (null != startDay) {
            Double prevRate = null;
            for (Day day = startDay.ensureTradingDay(); day.compareTo(endDay) <= 0; day = day.nextTradingDay()) {
                Double currRate = rates.get(day);
                if (currRate != null) {
                    prevRate = currRate;
                }
                if (prevRate != null) {
                    interestRates.put(day, prevRate);
                }
            }
        }
    }

    public double getInterestRate(Day day) {
        Double rate = interestRates.get(day);
        if (rate != null) {
            return rate;
        }

        return 0.0;
    }

    public void restoreBackup() {
        for (OptionTerm term : optionTerms.values()) {
            term.restoreBackup();
        }
    }

    public double getMaxParityArbitrageReturn() {
        double maxRet = 0.0;

        PricingModel model = getPricingModel(PricingModel.Type.Implied);
        if (null == model) {
            return maxRet;
        }

        for (OptionTerm term : optionTerms.values()) {
            maxRet = Math.max(maxRet, term.getMaxParityArbitrageReturn(model));
        }

        return maxRet * 100.0;
    }

    public double getMaxOptionArbitrageReturn() {
        double maxRet = 0.0;

        PricingModel model = getPricingModel();
        if (null == model) {
            return maxRet;
        }

        for (OptionTerm term : optionTerms.values()) {
            maxRet = Math.max(maxRet, term.getMaxOptionArbitrageReturn(model));
        }

        return maxRet * 100.0;
    }

    // Returns the amount of positive PNL expected for bid (sell at bid) respectively ask (buy at ask) given the theoretical value of the option
    public Pair<Double, Double> getExpectedPnl(Instrument option, double tv) {
        Double spot = getSpotPrice();
        if (null == spot) {
            return new Pair<>(0.0, 0.0);
        }

        // Cost of buying or shorting one share of the stock
        double yf = Util.yearFraction(today.countTradingDays(option.getMaturity()));
        double totalRate = getInterestRate(today) + getBorrowRate();
        double borrowCost = spot * (Math.exp(totalRate * yf) - 1.0);

        double bidPnl = -borrowCost;
        if (option.getBidPrice() != null) {
            // Buy at TV, sell at bid
            bidPnl += option.getBidPrice() - tv;
        }

        double askPnl = -borrowCost;
        if (option.getAskPrice() != null) {
            // Buy at ask, sell at TV
            askPnl += tv - option.getAskPrice();
        }

        return new Pair<>(Math.max(bidPnl, 0.0), Math.max(askPnl, 0.0));
    }

    public Double getForward(int maturity) {
        VolatilitySurface surface = getVolatilitySurface();
        if (null == surface) {
            return null;
        }

        VolatilitySurface.StrikeVols strikeVols = surface.getMaturities().get(maturity);
        if (null == strikeVols) {
            return null;
        }

        return strikeVols.forward;
    }

    double getBorrowRate() {
        return borrowRate;
    }

}
