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

package com.aquarians.backtester;

import com.aquarians.aqlib.ApplicationModule;
import com.aquarians.backtester.database.DatabaseModule;
import com.aquarians.backtester.gui.GuiModule;
import com.aquarians.backtester.jobs.JobsModule;
import com.aquarians.backtester.marketdata.MarketDataModule;
import com.aquarians.backtester.pricing.PricingModule;

import java.sql.DriverManager;
import java.text.DecimalFormat;
import java.util.*;

public class Application {
    
    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(Application.class);

    private static Application INSTANCE;

    public static final String HOME_FOLDER_PROPERTY = "user.home";
    public static final DecimalFormat DOUBLE_DIGIT_FORMAT = new DecimalFormat("###.##");
    public static final DecimalFormat FOUR_DIGIT_FORMAT = new DecimalFormat("###.####");

    private final Object lock = new Object();
    private final Properties properties;
    private boolean stopRequested = false;
    private int idCounter = 0;

    // Modules of the application, in required initialization order (some may need to be initialized before others)
    private List<ApplicationModule> modules = new ArrayList<ApplicationModule>();

    public Application(Properties properties) {
        INSTANCE = this;
        this.properties = properties;
        createModules();
    }

    private void createModules() {
        // Jobs don't need all modules
        if (properties.getProperty(JobsModule.JOBS_PROPERTY) != null) {
            modules.add(new DatabaseModule(0));
            modules.add(new JobsModule());
        } else {
            createRegularModules();
        }
    }

    private void createRegularModules() {
        int threads = Integer.parseInt(properties.getProperty("Modules.Threads", "1"));
        for (int index = 0; index < threads; index++) {
            modules.add(new DatabaseModule(index));
            modules.add(new MarketDataModule(index));
            modules.add(new PricingModule(index));
        }

        modules.add(new GuiModule());
    }

    public static final String buildModuleName(String baseName, int index) {
        return baseName + "." + index;
    }

    public ApplicationModule getModule(String name) {
        for (ApplicationModule module : modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }

        return null;
    }

    public static void main(String[] args) {
        // Load the properties file
        Properties properties = null;
        try {
            String name = args.length > 0 ? args[0] : "config.properties";
            properties = new Properties();
            properties.load(new java.io.FileInputStream(name));
        } catch (Exception ex) {
            System.out.println("Error loading properties file: " + ex);
            return;
        }

        // Set home folder if not specified
        if (null == properties.getProperty(HOME_FOLDER_PROPERTY)) {
            properties.setProperty(HOME_FOLDER_PROPERTY, System.getProperty(HOME_FOLDER_PROPERTY));
        }

        // Initialize the logging system
        try {
            // Override the log file name adding the date, for instance "quant_" becomes "quant_20130605.log"
            String logFileName = properties.getProperty("log4j.appender.F1.File");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
            logFileName += sdf.format(java.util.Calendar.getInstance().getTime()) + ".log";
            properties.setProperty("log4j.appender.F1.File", logFileName);
            org.apache.log4j.PropertyConfigurator.configure(properties);
        } catch (Exception ex) {
            System.out.println("Error initializing logging system: " + ex);
            return;            
        }

        // Add command line arguments to properties
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            String[] tokens = arg.split("=");
            if (tokens.length < 1) {
                continue;
            }

            String tag = tokens[0].trim();
            String value = "";
            if (tokens.length > 1) {
                value = tokens[1].trim();
            }
            properties.setProperty(tag, value);
            logger.info("Command line property: " + tag + "=" + value);
        }

        // Start the application
        Application application = null;
        try {
            application = new Application(properties);
            application.init();
        } catch (Exception ex) {
            logger.error("Error starting application", ex);
            return;
        }
        
        if (logger.isInfoEnabled()) logger.info("APPLICATION STARTED");

        // Run the application
        try {
            application.run();
        } catch (Throwable ex) {
            logger.warn("Problem running application", ex);
            return;            
        }                

        // Stop the application
        try {
            application.cleanup();
        } catch (Throwable ex) {
            logger.warn("Problem stopping application", ex);
            return;            
        }                
        
        if (logger.isInfoEnabled()) logger.info("APPLICATION STOPPED");
    }
    
    public void requestStop() {
        if (logger.isInfoEnabled()) logger.info("stop requested");
        synchronized (lock) {
            stopRequested = true;
            lock.notifyAll();
        }
    }        
    
    public void init() {
        try {
            DriverManager.registerDriver(new org.postgresql.Driver());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        // Don't catch any exception here, if one module fails, the whole application fails
        for (ApplicationModule module : modules) {
            module.init();
        }
    }

    public void cleanup() {
        for (ApplicationModule module : modules) {
            try {
                module.cleanup();
            } catch (Exception ex) {
                logger.warn("Module: " + module.getName(), ex);
            }
        }
    }

    public String getFolderProperty(String name) {
        String value = properties.getProperty(name);
        if (null == value) {
            return null;
        }

        if (value.indexOf("~") != 0) {
            return value;
        }

        String home = properties.getProperty(HOME_FOLDER_PROPERTY);
        if (null == home) {
            home = ".";
        }

        // Remove terminating "/", if any
        if (home.lastIndexOf("/") == home.length() - 1) {
            home = home.substring(0, home.length() - 1);
        }

        return home + value.substring(1);
    }

    public static Application getInstance() {
        return INSTANCE;
    }

    public Properties getProperties() {
        return properties;
    }

    public void run() throws Exception {
        synchronized (lock) {
            while (!stopRequested) {
                lock.wait();
            }
        }
    }

    public int getNextId() {
        synchronized (lock) {
            return (++idCounter);
        }
    }

}
