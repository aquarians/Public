package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.Day;
import com.aquarians.aqlib.Instrument;
import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.math.LinearIterator;
import com.aquarians.aqlib.math.PriceRecord;
import com.aquarians.aqlib.models.BlackScholes;
import com.aquarians.aqlib.models.MonteCarloPricer;
import com.aquarians.aqlib.models.NormalProcess;
import com.aquarians.aqlib.models.PricingResult;
import com.aquarians.aqlib.positions.Strategy;
import com.aquarians.aqlib.positions.Trade;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.positions.Portfolio;
import com.aquarians.backtester.pricing.MonteCarloPricingModel;
import com.aquarians.backtester.pricing.NormalDistributionModel;
import com.aquarians.backtester.pricing.PricingModel;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Iterator;
import java.util.List;

public class OptionPricingStudyJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(OptionPricingStudyJob.class.getSimpleName());

    private Instrument stock = new Instrument(Instrument.Type.STOCK, "stock", null, null, null);

    public OptionPricingStudyJob() {
    }

    private void priceMonteCarlo() {
        int SIMULATIONS = 10000;
        double growth = 0.0;
        double volatility = 0.25;
        int maturity = Util.TRADING_DAYS_IN_MONTH;
        double spot = 100.0;
        double strike = 100.0;
        boolean isCall = true;

        NormalDistribution std = new NormalDistribution();

        DefaultProbabilityFitter forwards = new DefaultProbabilityFitter(SIMULATIONS);
        DefaultProbabilityFitter values = new DefaultProbabilityFitter(SIMULATIONS);
        DefaultProbabilityFitter pnls = new DefaultProbabilityFitter(SIMULATIONS);

        // Black-Scholes theoretical value of the option
        double t = Util.yearFraction(maturity);
        BlackScholes pricer = new BlackScholes(isCall, spot, strike, t, 0.0, 0.0, volatility);
        double tv = pricer.price();

        Iterator<Double> it = new LinearIterator(0.0, 1.0, SIMULATIONS);
        while (it.hasNext()) {
            double probability = Util.limitProbability(it.next());
            double z = std.inverseCumulativeProbability(probability);

            // Forward price
            double forward = spot * Math.exp((growth - volatility * volatility * 0.5) * t + volatility * Math.sqrt(t) * z);
            forwards.addSample(forward);

            // Option value
            double value = Math.max((isCall ? 1.0 : -1.0) * (forward - strike), 0.0);
            values.addSample(value);

            // Profit and loss when buying at tv, sell at market value
            double pnl = value - tv;
            pnls.addSample(pnl);
        }

        forwards.saveHistogram(Util.cwd() + "/forwards.csv");
        values.saveHistogram(Util.cwd() + "/values.csv");
        pnls.saveHistogram(Util.cwd() + "/pnls.csv");

        Util.logStatistics(logger, "FORWARDS", forwards);
        Util.logStatistics(logger, "VALUES", values);
        Util.logStatistics(logger, "PNLS", pnls);

        logger.debug("Black Scholes TV=" + Util.DOUBLE_DIGIT_FORMAT.format(tv));
        logger.debug("Simulated value=" + Util.DOUBLE_DIGIT_FORMAT.format(values.getMean()));
    }

    private void priceOptionsAsInsurance() {
        int samples = 10000;
        double volatility = 0.25;
        boolean isCall = true;
        double spot = 100.0;
        double strike = spot;
        double yf = Util.yearFraction(Util.TRADING_DAYS_IN_YEAR);

        NormalProcess processGrowth = new NormalProcess(0.1, volatility);
        NormalProcess processStagnation = new NormalProcess(0.0, volatility);
        NormalProcess processDecline = new NormalProcess(-0.1, volatility);

        MonteCarloPricer pricerGrowth = new MonteCarloPricer(processGrowth, isCall, spot, strike, yf);
        MonteCarloPricer pricerStagnation = new MonteCarloPricer(processStagnation, isCall, spot, strike, yf);
        MonteCarloPricer pricerDecline = new MonteCarloPricer(processDecline, isCall, spot, strike, yf);

        DefaultProbabilityFitter forwardsGrowth = processGrowth.simulateForwards(spot, yf, samples);
        DefaultProbabilityFitter forwardsStagnation = processStagnation.simulateForwards(spot, yf, samples);
        DefaultProbabilityFitter forwardsDecline = processDecline.simulateForwards(spot, yf, samples);

        forwardsGrowth.saveHistogram(Util.cwd() + "/forwardsGrowth.csv");
        forwardsStagnation.saveHistogram(Util.cwd() + "/forwardsStagnation.csv");
        forwardsDecline.saveHistogram(Util.cwd() + "/forwardsDecline.csv");
        Util.logStatistics(logger, "forwardsGrowth", forwardsGrowth);
        Util.logStatistics(logger, "forwardsStagnation", forwardsStagnation);
        Util.logStatistics(logger, "forwardsDecline", forwardsDecline);

        logger.debug("Growth price=" + Util.format(pricerGrowth.price()) + " delta=" + Util.format(pricerGrowth.delta()));
        logger.debug("Stagnation price=" + Util.format(pricerStagnation.price()) + " delta=" + Util.format(pricerStagnation.delta()));
        logger.debug("Decline price=" + Util.format(pricerDecline.price()) + " delta=" + Util.format(pricerDecline.delta()));
    }

    private void simulateReplication() {
        int SIMULATIONS = 100;
        double CAPITAL = 100.0;
        double MARKET_GROWTH = -0.1;

        double volatility = 0.25;
        double spot = 100.0;
        int maturity = Util.TRADING_DAYS_IN_YEAR;
        int hedgeFrequency = Util.TRADING_DAYS_IN_MONTH;
        Day now = Day.now();

        NormalProcess marketProcess = new NormalProcess(MARKET_GROWTH, volatility);

        DefaultProbabilityFitter pnlsBlackScholes = new DefaultProbabilityFitter(SIMULATIONS);
        DefaultProbabilityFitter pnlsGrowth = new DefaultProbabilityFitter(SIMULATIONS);
        DefaultProbabilityFitter pnlsStagnation = new DefaultProbabilityFitter(SIMULATIONS);
        DefaultProbabilityFitter pnlsDecline = new DefaultProbabilityFitter(SIMULATIONS);

        for (int sim = 0; sim < SIMULATIONS; sim++) {
            logger.debug("Running simulation " + sim + ", remaining: " + (SIMULATIONS - sim));

            NormalDistributionModel modelBlackScholes = new NormalDistributionModel(volatility);
            MonteCarloPricingModel modelGrowth = new MonteCarloPricingModel(new NormalProcess(0.1, volatility));
            MonteCarloPricingModel modelStagnation = new MonteCarloPricingModel(new NormalProcess(0.0, volatility));
            MonteCarloPricingModel modelDecline = new MonteCarloPricingModel(new NormalProcess(-0.1, volatility));

            double navBlackScholes = CAPITAL;
            double navGrowth = CAPITAL;
            double navStagnation = CAPITAL;
            double navDecline = CAPITAL;

            boolean created = false;
            Strategy strategyBlackScholes = null;
            Strategy strategyGrowth = null;
            Strategy strategyStagnation = null;
            Strategy strategyDecline = null;

            Portfolio portfolioBlackScholes = null;
            Portfolio portfolioGrowth = null;
            Portfolio portfolioStagnation = null;
            Portfolio portfolioDecline = null;

            List<PriceRecord> stockTrajectory = marketProcess.generatePath(now, spot, maturity);
            for (PriceRecord record : stockTrajectory) {

                stock.setPrice(record.price);

                modelBlackScholes.setSpot(record.price);
                modelGrowth.setSpot(record.price);
                modelStagnation.setSpot(record.price);
                modelDecline.setSpot(record.price);

                modelBlackScholes.setToday(record.day);
                modelGrowth.setToday(record.day);
                modelStagnation.setToday(record.day);
                modelDecline.setToday(record.day);

                if (!created) {
                    created = true;
                    strategyBlackScholes = createStrategy(CAPITAL, record, maturity, modelBlackScholes);
                    strategyGrowth = createStrategy(CAPITAL, record, maturity, modelGrowth);
                    strategyStagnation = createStrategy(CAPITAL, record, maturity, modelStagnation);
                    strategyDecline = createStrategy(CAPITAL, record, maturity, modelDecline);

                    portfolioBlackScholes = new Portfolio(strategyBlackScholes, stock, null, null, modelBlackScholes);
                    portfolioGrowth = new Portfolio(strategyGrowth, stock, null, null, modelGrowth);
                    portfolioStagnation = new Portfolio(strategyStagnation, stock, null, null, modelStagnation);
                    portfolioDecline = new Portfolio(strategyDecline, stock, null, null, modelDecline);

                    portfolioBlackScholes.setHedgeFrequency(hedgeFrequency);
                    portfolioGrowth.setHedgeFrequency(hedgeFrequency);
                    portfolioStagnation.setHedgeFrequency(hedgeFrequency);
                    portfolioDecline.setHedgeFrequency(hedgeFrequency);
                }

                portfolioBlackScholes.rebalance();
                portfolioGrowth.rebalance();
                portfolioStagnation.rebalance();
                portfolioDecline.rebalance();

//                navBlackScholes = portfolioBlackScholes.computeNav();
//                navGrowth = portfolioGrowth.computeNav();
//                navStagnation = portfolioStagnation.computeNav();
//                navDecline = portfolioDecline.computeNav();
//                logger.debug("NAV sim=" + sim + " day=" + record.day + " spot=" + Util.format(record.price) +
//                        " navBlackScholes=" + Util.format(navBlackScholes) +
//                        " navGrowth=" + Util.format(navGrowth) +
//                        " navStagnation=" + Util.format(navStagnation) +
//                        " navDecline=" + Util.format(navDecline));
//                logger.debug("NAVCSV," + sim + "," + record.day.format(Day.US_FORMAT) + "," + Util.format(record.price) +
//                        "," + Util.format(navBlackScholes) +
//                        "," + Util.format(navGrowth) +
//                        "," + Util.format(navStagnation) +
//                        "," + Util.format(navDecline));
            }

                logger.debug("PNL sim=" + sim +
                        " BlackScholes=" + Util.format(portfolioBlackScholes.getRealizedProfit()) +
                        " Growth=" + Util.format(portfolioGrowth.getRealizedProfit()) +
                        " Stagnation=" + Util.format(portfolioStagnation.getRealizedProfit()) +
                        " Decline=" + Util.format(portfolioDecline.getRealizedProfit()));

            pnlsBlackScholes.addSample(portfolioBlackScholes.getRealizedProfit());
            pnlsGrowth.addSample(portfolioGrowth.getRealizedProfit());
            pnlsStagnation.addSample(portfolioStagnation.getRealizedProfit());
            pnlsDecline.addSample(portfolioDecline.getRealizedProfit());
        }

        logger.debug("=============== STATISTICS ===============");
        Util.logStatistics(logger, "BlackScholes", pnlsBlackScholes);
        Util.logStatistics(logger, "Growth", pnlsGrowth);
        Util.logStatistics(logger, "Stagnation", pnlsStagnation);
        Util.logStatistics(logger, "Decline", pnlsDecline);
    }

    Strategy createStrategy(double nav, PriceRecord record, int maturity, PricingModel model) {
        Strategy strategy = new Strategy();
        strategy.executionDay = record.day;
        strategy.executionSpot = record.price;
        strategy.maturityDay = record.day.addTradingDays(maturity);
        strategy.multiplier = nav / record.price;
        strategy.capital = nav;

        // ATM call option
        Instrument option = new Instrument(Instrument.Type.OPTION, "option", true, strategy.maturityDay, record.price);

        PricingResult pricing = model.price(option);

        // Buy option
        Trade trade = new Trade(record.day, option, 1.0, pricing.price, pricing.price);

        // Sell option
        //Trade trade = new Trade(record.day, option, -1.0, pricing.price, pricing.price);

        strategy.trades = Util.newArrayList(trade);

        return strategy;
    }

    @Override
    public void run() {
        //priceMonteCarlo();
        //priceOptionsAsInsurance();
        simulateReplication();
    }
}
