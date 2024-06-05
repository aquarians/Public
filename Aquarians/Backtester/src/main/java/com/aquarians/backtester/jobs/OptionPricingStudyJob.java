package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.math.LinearIterator;
import com.aquarians.aqlib.models.BlackScholes;
import com.aquarians.aqlib.models.NormalProcess;
import com.aquarians.backtester.Application;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Iterator;

public class OptionPricingStudyJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(OptionPricingStudyJob.class.getSimpleName());

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

    @Override
    public void run() {
        priceMonteCarlo();
    }
}
