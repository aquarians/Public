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

package com.aquarians.backtester.jobs;

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.aqlib.Util;
import com.aquarians.backtester.Application;
import com.aquarians.backtester.database.DatabaseModule;

import java.util.ArrayList;
import java.util.List;

public class JobsModule implements ApplicationModule {

    public static final String JOBS_PROPERTY = "Jobs.Jobs";

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(JobsModule.class);

    public static final String NAME = "Jobs";

    private final Object lock = new Object();
    private final DatabaseModule databaseModule;
    private final Thread processorThread;
    private final List<String> jobNames = new ArrayList<>();
    boolean stopRequested;

    public JobsModule() {
        databaseModule = (DatabaseModule) Application.getInstance().getModule(Application.buildModuleName(DatabaseModule.NAME, 0));
        processorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    process();
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        }, "JOBS");

        String jobsText = Application.getInstance().getProperties().getProperty(JOBS_PROPERTY , "");
        String[] jobNames = jobsText.split(",");
        for (String jobName : jobNames) {
            this.jobNames.add(jobName.trim());
        }
    }

    private Runnable createJob(String name) {
        if (name.equals(ImportHistoricalStockPricesJob.class.getSimpleName())) {
            return new ImportHistoricalStockPricesJob(databaseModule);
        } else if (name.equals(ImportHistoricalOptionPricesJob.class.getSimpleName())) {
            return new ImportHistoricalOptionPricesJob(databaseModule);
        } else if (name.equals(CurateUnderliersJob.class.getSimpleName())) {
            return new CurateUnderliersJob(databaseModule);
        } else if (name.equals(ImportStockSplitsJob.class.getSimpleName())) {
            return new ImportStockSplitsJob(databaseModule);
        } else if (name.equals(GenerateTestDataJob.class.getSimpleName())) {
            return new GenerateTestDataJob(databaseModule);
        } else if (name.equals(ImportYahooStockPricesJob.class.getSimpleName())) {
            return new ImportYahooStockPricesJob(databaseModule);
        } else if (name.equals(GeometricBrownianMotionStudyJob.class.getSimpleName())) {
            return new GeometricBrownianMotionStudyJob(databaseModule);
        } else if (name.equals(OptionPricingStudyJob.class.getSimpleName())) {
            return new OptionPricingStudyJob();
        }

        throw new RuntimeException("Unknown job: " + name);
    }

    private void process() {
        for (String name : jobNames) {
            if (isStopRequested()) {
                break;
            }

            try {
                Runnable job = createJob(name);
                job.run();
            } catch (Exception ex) {
                logger.warn("Running job: " + name, ex);
            }
        }

        // Terminate application when jobs finish
        Application.getInstance().requestStop();
    }

    @Override
    public void init() {
        processorThread.start();
    }

    @Override
    public void cleanup() {
        requestStop();
        Util.safeJoin(processorThread);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void requestStop() {
        synchronized (lock) {
            stopRequested = true;
            lock.notifyAll();
        }
    }

    public boolean isStopRequested() {
        synchronized (lock) {
            return stopRequested;
        }
    }

}
