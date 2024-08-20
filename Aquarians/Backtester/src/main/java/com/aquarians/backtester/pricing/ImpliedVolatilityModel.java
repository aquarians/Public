package com.aquarians.backtester.pricing;

import com.aquarians.aqlib.*;
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
    private int hedgeFrequency = Util.DEFAULT_HEDGE_FREQUENCY;

    public ImpliedVolatilityModel(PricingModule owner) {
        this.owner = owner;
    }

    public Day getToday() {
        return today;
    }

    public Type getType() {
        return Type.Implied;
    }

    @Override
    public PricingResult price(Instrument instrument) {
        if (instrument.getType().equals(Instrument.Type.STOCK)) {
            return super.price(instrument);
        } else if (instrument.getType().equals(Instrument.Type.PARITY)) {
            return priceParity(instrument);
        } else if (!instrument.getType().equals(Instrument.Type.OPTION)) {
            throw new RuntimeException("Unknown instrument type: " + instrument.getType().name());
        }

        if ((null == today) || (null == surface)) {
            return null;
        }

        int maturity = Util.maturity(today, instrument.getMaturity());
        Double vol = surface.getVolatility(maturity, instrument.getStrike());
        if (null == vol) {
            return null;
        }

        Double forward = surface.getForward(maturity);
        if (null == forward) {
            return null;
        }

        Double interest = surface.getInterest(maturity);
        if (null == interest) {
            interest = interestRate;
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

        // Use Black model where dividends are implied by the forward price
        double yf = Util.yearFraction(maturity);
        BlackScholes pricer = new BlackScholes(instrument.isCall(), forward, instrument.getStrike(), yf, interest, 0.0, vol);
        pricer.setBlack(true);
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

        for (OptionTerm term : owner.getOptionTerms().values()) {
            try {
                computeImpliedVol(term);
            } catch (Exception ex) {
                logger.warn("Underlier: " + owner.getStock().getCode() + " day: " + today + " term: " + term.maturity, ex);
            }
        }
    }

    void computeImpliedVol(OptionTerm term) {
        double interestRate = owner.getInterestRate(today);
        // Calculate forward doing sanity checks on its validity
        Double forwardPrice = term.computeParityForwardPrice(interestRate, true);
        if (null == forwardPrice) {
            return;
        }

        VolatilitySurface.StrikeVols strikeVols = computeImpliedVol(term, forwardPrice, interestRate);
        surface.add(term.daysToExpiry, strikeVols);
    }

    private VolatilitySurface.StrikeVols computeImpliedVol(OptionTerm term, double forward, double interest) {
        VolatilitySurface.StrikeVols strikeVols = new VolatilitySurface.StrikeVols();
        strikeVols.forward = forward;
        strikeVols.interest = interest;

        for (Map.Entry<Double, OptionPair> entry : term.getStrikes().entrySet()) {
            Double strike = entry.getKey();
            OptionPair pair = entry.getValue();

            // Use OTM option
            Instrument option = (strike < forward) ? pair.put : pair.call;
            if (null == option) {
                continue;
            }

            Double price = option.getMidPrice();
            if (null == price) {
                continue;
            }

            // Use Black model where dividend yield is implied by forward price
            BlackScholes pricer = new BlackScholes(option.isCall(), forward, strike, term.yf, interest, 0.0, 0.0);
            pricer.setBlack(true);
            Double vol = pricer.impliedVolatility(price);
            if (null == vol) {
                continue;
            }

            strikeVols.put(strike, vol);
        }

        return strikeVols;
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

    private PricingResult priceParity(Instrument instrument) {
        if ((null == today) || (null == surface)) {
            return null;
        }

        int maturity = Util.maturity(today, instrument.getMaturity());
        VolatilitySurface.StrikeVols strikeVols = surface.getMaturities().get(maturity);
        if ((null == strikeVols) || (null == strikeVols.forward)) {
            return null;
        }

        double interestRate = (strikeVols.interest != null) ? strikeVols.interest : 0.0;
        double totalRate = interestRate + owner.getBorrowRate();

        OptionTerm term = owner.getOptionTerms().get(instrument.getMaturity());
        if (null == term) {
            return null;
        }

        double borrow = strikeVols.forward * (Math.exp(totalRate * term.yf) - 1.0);

        OptionPair pair = term.strikes.get(instrument.getStrike());
        if ((null == pair) || (null == pair.call) || (null == pair.put)) {
            return null;
        }

        double pnl = pair.getParityArbitragePnl(strikeVols.forward, borrow);
        return new PricingResult(pnl, 1.0);
    }

    @Override
    public Double getForward(Day maturity) {
        return surface.getForward(Util.maturity(today, maturity));
    }

}
