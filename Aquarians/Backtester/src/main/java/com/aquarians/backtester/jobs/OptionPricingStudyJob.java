package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.Util;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.models.BlackScholes;
import com.aquarians.aqlib.models.NormalProcess;
import com.aquarians.backtester.Application;

public class OptionPricingStudyJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(OptionPricingStudyJob.class.getSimpleName());

    public OptionPricingStudyJob() {
    }

    private void priceMonteCarlo() {
        int SIMULATIONS = 10000;
        double growth = 0.0;
        double volatility = 0.25;
        int days = Util.TRADING_DAYS_IN_MONTH;
        double spot = 100.0;
        double strike = 100.0;
        boolean isCall = true;

        NormalProcess process = new NormalProcess(growth, volatility);

        DefaultProbabilityFitter forwards = new DefaultProbabilityFitter(SIMULATIONS);
        DefaultProbabilityFitter values = new DefaultProbabilityFitter(SIMULATIONS);
        DefaultProbabilityFitter pnls = new DefaultProbabilityFitter(SIMULATIONS);

        BlackScholes pricer = new BlackScholes(isCall, spot, strike, Util.yearFraction(days), 0.0, 0.0, volatility);
        double price = pricer.price();

        for (int sim = 0; sim < SIMULATIONS; sim++) {
            double forward = process.generateForwardAtMaturity(days, spot);
            forwards.addSample(forward);

            double value = new BlackScholes(isCall, forward, strike, 0.0, 0.0, 0.0, volatility).valueAtExpiration();
            values.addSample(value);

            // Buy at price, sell at value
            double pnl = value - price;
            pnls.addSample(pnl);
        }

        String cwd = Application.getInstance().getProperties().getProperty(Application.WORK_FOLDER_PROPERTY);

        forwards.saveHistogram(cwd + "/forwards.csv");
        values.saveHistogram(cwd + "/values.csv");
        pnls.saveHistogram(cwd + "/pnls.csv");

        logger.debug("Fair value: " + Util.DOUBLE_DIGIT_FORMAT.format(price));

        Util.logStatistics(logger, "FORWARDS", forwards);
        Util.logStatistics(logger, "VALUES", values);
        Util.logStatistics(logger, "PNLS", pnls);
    }

    @Override
    public void run() {
        priceMonteCarlo();
    }
}
