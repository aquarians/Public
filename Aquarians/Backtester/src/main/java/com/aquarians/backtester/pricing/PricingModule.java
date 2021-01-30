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
    private List<PricingModel> pricingModels = new ArrayList<>();
    private Map<String, Instrument> instruments = new TreeMap<>();

    private Day today;
    private UnderlierRecord underlier;
    private Instrument stock;

    public PricingModule(int index) {
        this.index = index;
        marketDataModule = (MarketDataModule) Application.getInstance().getModule(Application.buildModuleName(MarketDataModule.NAME, index));

        // Because we do multithreaded operations, each pricing module needs other modules of the same index
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, index));
        activeModel = PricingModel.Type.valueOf(Application.getInstance().getProperties().getProperty("Pricing.ActiveModel", PricingModel.Type.Market.name()));

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
        fitModels();
        notifyListeners();
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
                    term = new OptionTerm(today, instrument.getMaturity());
                    optionTerms.put(instrument.getMaturity(), term);
                }

                term.add(instrument);
            }
        }
    }

    public Day getToday() {
        return today;
    }

    public UnderlierRecord getUnderlier() {
        return underlier;
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

        computeModelError();
    }

    public static final class ModelError {
        double totalError;
        int pricesCount;
        int errorsCount;
    }

    private void computeModelError() {
        PricingModel model = getPricingModel();
        if (null == model) {
            return;
        }

        ModelError error = new ModelError();
        for (Map.Entry<Day, OptionTerm> entry : optionTerms.entrySet()) {
            OptionTerm term = entry.getValue();
            term.computeModelError(model, error);
        }

        double average = error.totalError / Math.max(1, error.errorsCount);
        double percent = (0.0 + error.errorsCount) / Math.max(1, error.pricesCount);
        double weighted = average * percent;
        logger.debug("MODELFIT underlier=" + underlier.code + " day=" + today + " err=" + Application.DOUBLE_DIGIT_FORMAT.format(weighted * 100.0));
    }

    public TreeMap<Day, OptionTerm> getOptionTerms() {
        return optionTerms;
    }

    public Double getImpliedSpotPrice() {
        Double spot = getMarketSpotPrice();
        if (null == spot) {
            return null;
        }

        OptionTerm term = null;
        for (Map.Entry<Day, OptionTerm> entry : optionTerms.entrySet()) {
            term = entry.getValue();
            break;
        }
        if (null == term) {
            return null;
        }

        OptionPair atmPair = term.getClosestStrike(spot);
        if (null == atmPair) {
            return null;
        }

        // Bounds for spot price
        Double spotLow = Util.getParitySpotLowerBound(atmPair.call, atmPair.put);
        Double spotHigh = Util.getParitySpotUpperBound(atmPair.call, atmPair.put);
        if ((null == spotLow) || (null == spotHigh)) {
            return null;
        }

        // Use the mid instead of sampled spot, otherwise it's a sampling error (wouldn't happen in a HFT setting)
        Double impliedSpot = (spotLow + spotHigh) / 2.0;
        double ratio = Util.maxRatio(spot, impliedSpot);
        if (ratio > 1.1) {
            // Error in data
            return null;
        }

        return impliedSpot;
    }

    public Double getMarketSpotPrice() {
        if (null == stock) {
            return null;
        }

        return stock.getPrice();
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

}
