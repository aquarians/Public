#include <iostream>
#include <log4cxx/logger.h>
#include <log4cxx/helpers/fileinputstream.h>
#include <log4cxx/helpers/fileinputstream.h>
#include <log4cxx/propertyconfigurator.h>
#include <log4cxx/mdc.h>
#include "Types.hpp"
#include <string>
#include <stdio.h>
#include "aqlib/StringTokenizer.hpp"
#include "aqlib/BinaryArchive.hpp"
#include "aqlib/Serializable.hpp"
#include "aqlib/DefaultObjectFactory.hpp"
#include "aqlib/Thread.hpp"
#include "aqlib/Value.hpp"
#include "Application.hpp"
#include "simulation/BlackScholes.hpp"
#include "math/DistributionHistogram.hpp"

static log4cxx::LoggerPtr logger(log4cxx::Logger::getLogger("Main"));
void initializeLogging(const PropertiesPtr &properties);

int main(int argc, char **argv)
{
    // Load the properties file
    PropertiesPtr properties;
    try
    {
        std::string name = (argc > 1) ? argv[1] : "aquarians.properties";
        properties = PropertiesPtr(new log4cxx::helpers::Properties());
        log4cxx::helpers::FileInputStreamPtr stream(new log4cxx::helpers::FileInputStream(name));
        properties->load(stream);
    }
    catch(std::exception &ex)
    {
        std::cout << "Error loading properties file: " << ex.what() << std::endl;
        return 1;
    }
    
    // Initialize the logging system
    try 
    {
        initializeLogging(properties);
    }
    catch(std::exception &ex)
    {
        std::cout << "Error initializing logging system: " << ex.what() << std::endl;
        return 1;
    }
    
    // Start the application
    boost::shared_ptr<Application> application;
    try
    {
        application = boost::shared_ptr<Application>(new Application(properties));
        application->init();
    }
    catch(std::exception &ex)
    {
        LOG4CXX_ERROR(logger, "Error starting application: " << ex.what());
        return 1;
    }

    LOG4CXX_INFO(logger, "Application started")

    // Run the application
    try
    {
        application->run();
    }
    catch(std::exception &ex)
    {
        LOG4CXX_WARN(logger, "Problem running application: " << ex.what());
    }
    
    // Stop the application
    try
    {
        application->cleanup();
    }
    catch(std::exception &ex)
    {
        LOG4CXX_WARN(logger, "Problem stopping application: " << ex.what());
    }
    
    LOG4CXX_INFO(logger, "Application stopped")
    return 0;
}

void initializeLogging(const PropertiesPtr &properties)
{
    // Override the log file name adding the date, for instance "quant_" becomes "quant_20130605.log"
    struct timeval timeOfDay;
    gettimeofday(&timeOfDay, NULL);
    struct tm localTime;
    localtime_r(&timeOfDay.tv_sec, &localTime);
    
    std::string prefix = properties->getProperty("log4j.appender.F1.File");
     
    char fileName[256];
    snprintf(fileName, 256, "%s%04d%02d%02d.log",
            prefix.c_str(),
            localTime.tm_year + 1900,
            localTime.tm_mon + 1,
            localTime.tm_mday);
            
    properties->put("log4j.appender.F1.File", fileName);
    
    // Configure the logger
    log4cxx::PropertyConfigurator::configure(*properties.get());
    log4cxx::MDC::put("tid", "MAIN");    
}
