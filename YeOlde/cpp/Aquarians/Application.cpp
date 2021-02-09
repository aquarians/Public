#include "Application.hpp"
#include "aqlib/DefaultObjectFactory.hpp"
#include "aqlib/Exception.hpp"
#include "net/Module.hpp"
#include "simulation/Module.hpp"
#include "simulation/BlackScholes.hpp"
#include "net/SimulationRequestMessage.hpp"
#include "net/SimulationResultMessage.hpp"
#include "simulation/SimulationResult.hpp"
#include <time.h>
#include <sys/time.h>
#include <signal.h>
#include <stdlib.h>

log4cxx::LoggerPtr Application::logger(log4cxx::Logger::getLogger("Application"));
Application *Application::gInstance = NULL;

Application::Application(const PropertiesPtr &properties)
{
    // Set the global instance pointer
    gInstance = this;
    
    mStopRequested = false;
    mProperties = properties;  
    mIdCounter = 0;
    mObjectFactory = aqlib::ObjectFactoryPtr(new aqlib::DefaultObjectFactory(""));
}

Application& Application::getInstance()
{
    return *gInstance;
}

aqlib::ObjectFactory* Application::getObjectFactory()
{
    return mObjectFactory.get();
}

int Application::getNextId()
{
    aqlib::Lock lock(mLock);
    mIdCounter++;
    return mIdCounter;
}

const PropertiesPtr& Application::getProperties() const
{
    return mProperties;
}

std::string Application::getProperty(const std::string &name, const std::string &defaultValue) const
{
    std::string value = mProperties->get(name);
    return value.size() ? value : defaultValue;
}

void Application::addModule(const aqlib::ModulePtr &module)
{
    mModules.push_back(module);
    mModulesIndex[module->getModuleName()] = module;
}

void Application::init()
{
    // Install signal handlers
    signal(SIGINT, interruptHandler);
    signal(SIGSEGV, segmentationHandler);
    std::set_unexpected(terminateHandler);
    std::set_terminate(terminateHandler);
    
    // Initialize static data
    aqlib::Exception::staticInit();
    
    // Initialize the random number generator
    srand((unsigned)time(NULL));    
    
    // Register object prototypes
    registerObjectFactoryPrototypes();
    
    // Create application modules
    addModule(aqlib::ModulePtr(new net::Module()));
    addModule(aqlib::ModulePtr(new simulation::Module()));
    
    // Init modules
    for (Modules::iterator it = mModules.begin(); it != mModules.end(); ++it)
    {
        const aqlib::ModulePtr &module = *it;
        try
        {
            module->init();
        }
        catch (std::exception &ex)
        {
            LOG4CXX_WARN(logger, "Failed initializing module: " << module->getModuleName() << ": " << ex.what())
        }
    }
}

void Application::registerObjectFactoryPrototypes()
{
    // At this point, aqlib::SerializablePrototypeRepository contains the list of prototypes, filled during application static init phase
    const std::list<aqlib::SerializablePtr> &prototypes = aqlib::SerializablePrototypeRepository::instance().prototypes;
    for (std::list<aqlib::SerializablePtr>::const_iterator it = prototypes.begin(); it != prototypes.end(); ++it)
    {
        const aqlib::SerializablePtr &prototype = *it;
        mObjectFactory->registerPrototype(prototype.get());
    }
}

void Application::run()
{
    aqlib::Lock lock(mLock);
    while (!mStopRequested)
    {
        mLock.wait();
    }
}

void Application::cleanup()
{
    // Cleanup modules
    for (Modules::iterator it = mModules.begin(); it != mModules.end(); ++it)
    {
        const aqlib::ModulePtr &module = *it;
        try
        {
            module->cleanup();
        }
        catch (std::exception &ex)
        {
            LOG4CXX_WARN(logger, "Failed cleaning up module: " << module->getModuleName() << ": " << ex.what())
        }
    }
}

void Application::requestStop()
{
    LOG4CXX_INFO(logger, "Stop requested")
    aqlib::Lock lock(mLock);
    mStopRequested = true;
    mLock.notify();
}

void Application::interruptHandler(int param)
{
    gInstance->requestStop();
}

void Application::segmentationHandler(int param)
{
    // Log the stack trace
    try
    {
        throw aqlib::Exception("Segmentation fault!");
    }
    catch (std::exception &ex)
    {
        LOG4CXX_FATAL(logger, ex.what());
    }
    
    // Terminate the process
    ::sleep(2);
    abort();
}

void Application::terminateHandler()
{
    // Log the stack trace
    try
    {
        throw aqlib::Exception("Terminated!");
    }
    catch (std::exception &ex)
    {
        LOG4CXX_FATAL(logger, ex.what());
    }
    
    // Terminate the process
    ::sleep(2);
    abort();    
}

aqlib::Module* Application::getModule(const std::string &name) const
{
    ModulesIndex::const_iterator it = mModulesIndex.find(name);
    if (mModulesIndex.end() == it)
    {
        return NULL;
    }

    const aqlib::ModulePtr &module = it->second;
    return module.get();
}
