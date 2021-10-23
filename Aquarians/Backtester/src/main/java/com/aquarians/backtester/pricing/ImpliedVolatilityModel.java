package com.aquarians.backtester.pricing;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.OptionPair;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.models.BlackScholes;
import com.aquarians.aqlib.models.PricingResult;
import com.aquarians.aqlib.models.VolatilitySurface;
import com.aquarians.backtester.Application;

import java.util.Map;

public class ImpliedVolatilityModel extends AbstractPricingModel {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ImpliedVolatilityModel.class);

    private PricingModule owner;
    private VolatilitySurface surface;
    private Day today;
    private Double spotPrice;
    private final Integer volatilityRounding;
    private int hedgeFrequency = Util.DEFAULT_HEDGE_FREQUENCY;

    public ImpliedVolatilityModel(PricingModule owner) {
        this.owner = owner;

        String volatilityRoundingText = Application.getInstance().getProperties().getProperty("ImpliedVolatilityModel.VolatilityRounding");
        if (volatilityRoundingText != null) {
            volatilityRounding = Integer.parseInt(volatilityRoundingText);
        } else {
            volatilityRounding = null;
        }
    }

    public Day getToday() {
        return today;
    }

    public Type getType() {
        return Type.Implied;
    }

    @Override
    public PricingResult price(Instrument instrument) {
        if ((null == today) || (null == surface)) {
            return null;
        }

        int maturity = today.countTradingDays(instrument.getMaturity());
        Double vol = surface.getVolatility(maturity, instrument.getStrike());
        if (null == vol) {
            return null;
        }

        double yf = Util.yearFraction(maturity);
        Double forward = surface.getForward(maturity);
        if (null == forward) {
            forward = surface.getSpot();
        }

        if (instrument.getType().equals(Instrument.Type.STOCK)) {
            PricingResult result = new PricingResult(forward, 1.0);
            result.pnlDev = 0.0;
            result.day = today;
            return result;
        }

        if (maturity < 1) {
            double sign = instrument.isCall() ? 1.0 : -1.0;
            double value = Math.max(sign * (forward - instrument.getStrike()), 0.0);
            PricingResult result = new PricingResult(value, 0.0);
            result.pnlDev = 0.0;
            result.day = today;
            return result;
        }

        BlackScholes pricer = new BlackScholes(instrument.isCall(), forward, instrument.getStrike(), yf, interestRate, dividendYield, vol);
        double price = pricer.price();
        double delta = pricer.analyticDelta();
        PricingResult result = new PricingResult(price, delta);

        int hedges = maturity;
        if (hedgeFrequency > 0) {
            hedges = Math.max(1, maturity / hedgeFrequency);
        }
        result.pnlDev = pricer.theoreticalPnlDev(hedges);
        result.day = today;

        return result;
    }

    @Override
    public void fit() {
        today = owner.getToday();
        spotPrice = owner.getSpotPrice();

        surface = new VolatilitySurface();
        surface.setSpot(spotPrice);

        for (Map.Entry<Day, OptionTerm> entry : owner.getOptionTerms().entrySet()) {
            OptionTerm term = entry.getValue();
            try {
                computeImpliedVol(term);
            } catch (Exception ex) {
                logger.warn("Underlier: " + owner.getStock().getCode() + " day: " + today + " term: " + term.maturity, ex);
            }
        }
    }

    void computeImpliedVol(OptionTerm term) {
        for (Map.Entry<Double, OptionPair> entry : term.getStrikes().entrySet()) {
            Double strike = entry.getKey();
            OptionPair pair = entry.getValue();

            // Use OTM option
            Instrument option = (strike < spotPrice) ? pair.put : pair.call;
            if (null == option) {
                continue;
            }

            // Must have both bid and ask
            Double price = option.getMidPrice();
            if (null == price) {
                continue;
            }

            Double forward = spotPrice;
            Double parityForward = term.computeParityForwardPrice();

            BlackScholes pricer = new BlackScholes(option.isCall(), spotPrice, pair.strike, term.yf, interestRate, dividendYield, 0.0);
            Double vol = pricer.impliedVolatility(price);
            if (null == vol) {
                continue;
            }

            if (volatilityRounding != null) {
                vol = Util.round(vol, volatilityRounding);
            }

            surface.add(term.daysToExpiry, strike, vol);
        }
    }

    @Override
    public VolatilitySurface getSurface() {
        return surface;
    }

    public Double getSpot() {
        return spotPrice;
    }

    public Double getVolatility() {
        return surface.getVolatility(Util.TRADING_DAYS_IN_MONTH, surface.getSpot());
    }

    public int getHedgeFrequency() {
        return hedgeFrequency;
    }

    public void setHedgeFrequency(int hedgeFrequency) {
        this.hedgeFrequency = hedgeFrequency;
    }
}
