package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.*;
import com.aquarians.aqlib.math.DefaultProbabilityFitter;
import com.aquarians.aqlib.math.LinearIterator;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.database.records.StockPriceRecord;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Iterator;
import java.util.List;

public class GeometricBrownianMotionStudyJob implements Runnable {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(GeometricBrownianMotionStudyJob.class.getSimpleName());

    private final DatabaseModule database;

    private static final int MAX_SAMPLING_DAYS = 32;

    public GeometricBrownianMotionStudyJob(DatabaseModule database) {
        this.database = database;
    }

    static boolean isNullOrZero(Double value) {
        if ((null == value) || (value < Util.ZERO)) {
            return true;
        }

        return false;
    }

    private DefaultProbabilityFitter collectLogReturns(String code, int days) {
        Long id = database.getProcedures().underlierSelect.execute(code);
        if (null == id) {
            return new DefaultProbabilityFitter();
        }

        Pair<Day, Day> interval = database.getProcedures().stockPricesSelectMinMaxDate.execute(id);
        Day startDay = interval.getKey();
        Day endDay = interval.getValue();

        List<StockPriceRecord> records =  database.getProcedures().stockPricesSelect.execute(id, startDay, endDay);

        // Collect log returns
        DefaultProbabilityFitter rets = new DefaultProbabilityFitter(records.size());

        for (int i = days; i < records.size(); i += days) {
            StockPriceRecord curr = records.get(i);
            StockPriceRecord prev = records.get(i - days);
            if (isNullOrZero(curr.close) || isNullOrZero(prev.close)) {
                continue;
            }

            double ret = Math.log(curr.close / prev.close);
            rets.addSample(ret);
        }

        return rets;
    }

    private void computeHistogram(String code) {
        // Collect daily log returns
        DefaultProbabilityFitter rets = collectLogReturns(code, 1);

        // Calculate and print histogram in the log
        rets.computeHistogram(20);
        rets.save("out.csv");
    }

    private interface ParameterAccessor {
        double getParam(DefaultProbabilityFitter samples);
    }

    private void computeParameter(String code, ParameterAccessor accessor) {
        // Collect samples with varying frequency
        Points.Builder builder = new Points.Builder();
        for (int days = 1; days < MAX_SAMPLING_DAYS; days++) {
            double x = Util.yearFraction(days);

            // Calculate variance
            DefaultProbabilityFitter rets = collectLogReturns(code, days);
            rets.compute();
            double y = accessor.getParam(rets);

            builder.add(x, y);
        }
        Points points = builder.build();

        // Least-squares fit of straight line through sampling points
        Ref<Double> alpha = new Ref<>();
        Ref<Double> beta = new Ref<>();
        Util.fitRegressionLine(points, alpha, beta);

        // Print results in the log for later visual display
        logger.debug("CSV,time,var,fit");
        for (int i = 0; i < points.size(); i++) {
            double x = points.x(i);
            double y = points.y(i);
            double yy = alpha.value + beta.value * x;
            logger.debug("CSV," + Util.SIX_DIGIT_FORMAT.format(x) +
                    "," + Util.SIX_DIGIT_FORMAT.format(y) +
                    "," + Util.SIX_DIGIT_FORMAT.format(yy));
        }

        logger.debug("Fitted parameters: alpha=" + Util.FOUR_DIGIT_FORMAT.format(alpha.value) +
                " beta=" + Util.FOUR_DIGIT_FORMAT.format(beta.value));
    }

    private void solutionOfGbmSde() {
        int days = Util.TRADING_DAYS_IN_YEAR;
        //int days = 1;
        double t = Util.yearFraction(days);
        double spot = 100.0;
        double mu = 0.05;
        double sigma = 0.35;

        NormalDistribution std = new NormalDistribution();

        DefaultProbabilityFitter zs = new DefaultProbabilityFitter();
        DefaultProbabilityFitter forward1s = new DefaultProbabilityFitter();
        DefaultProbabilityFitter forward2s = new DefaultProbabilityFitter();

        Iterator<Double> it = new LinearIterator(0.0, 1.0, 100000);
        while (it.hasNext()) {
            double probability = Util.limitProbability(it.next());
            double z = std.inverseCumulativeProbability(probability);
            zs.addSample(z);

            double forward1 = spot + spot * (mu * t + sigma * Math.sqrt(t) * z);
            forward1s.addSample(forward1);

            double forward2 = spot * Math.exp((mu - sigma * sigma * 0.5) * t + sigma * Math.sqrt(t) * z);
            forward2s.addSample(forward2);
        }

        zs.saveHistogram(Util.cwd() + "/zs.csv", 100);
        forward1s.saveHistogram(Util.cwd() + "/forward1s.csv", 100);
        forward2s.saveHistogram(Util.cwd() + "/forward2s.csv", 100);
        Util.plot(Util.cwd() + "/forwards.csv", forward1s.computeHistogram(100), forward2s.computeHistogram(100));

        forward1s.compute();
        logger.debug("forward1s mean=" + forward1s.getMean());

        forward2s.compute();
        logger.debug("forward2s mean=" + forward2s.getMean());
    }

    @Override
    public void run() {
        String code = "SPY";

        // Histogram of daily returns
        //computeHistogram(code);

        // Normal distribution variance
        //computeParameter(code, samples -> samples.getDev() * samples.getDev());

        // Normal distribution mean
        //computeParameter(code, samples -> samples.getMean());

        // Solution of GBM SDE
        solutionOfGbmSde();
    }
}
