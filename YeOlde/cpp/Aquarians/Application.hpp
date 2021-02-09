#ifndef __APPLICATION_HPP
#define __APPLICATION_HPP

#include "aqlib/ObjectFactory.hpp"
#include "aqlib/Thread.hpp"
#include "aqlib/Module.hpp"
#include "Types.hpp"
#include <log4cxx/logger.h>

class Application
{
    static log4cxx::LoggerPtr logger;
    
protected:
    static Application *gInstance;
    
    aqlib::Monitor mLock;
    bool mStopRequested;
    PropertiesPtr mProperties;
    int mIdCounter;
    aqlib::ObjectFactoryPtr mObjectFactory;

    // Modules, in creation (and initialization) order
    typedef std::list<aqlib::ModulePtr> Modules;
    Modules mModules;
    
    // Modules, indexed by name
    typedef std::map<std::string, aqlib::ModulePtr> ModulesIndex;
    ModulesIndex mModulesIndex;
    
public:
    Application(const PropertiesPtr &properties);
    
    // Singleton access
    static Application& getInstance();
    
    void init();
    void run();
    void cleanup();
    
    void requestStop();
    
    // Access to the application wide one and only object factory
    aqlib::ObjectFactory* getObjectFactory();
    
    // Returns a new id
    int getNextId();
    
    const PropertiesPtr& getProperties() const;
    std::string getProperty(const std::string &name, const std::string &defaultValue = "") const;
    
    aqlib::Module* getModule(const std::string &name) const;
    
private:
    void addModule(const aqlib::ModulePtr &module);    
    void registerObjectFactoryPrototypes();
    
private:
    static void interruptHandler(int param);
    static void segmentationHandler(int param);
    static void terminateHandler();
};

#endif // __APPLICATION_HPP
