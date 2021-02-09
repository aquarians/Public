#ifndef __AQLIB_DEFAULT_OBJECT_FACTORY_HPP
#define __AQLIB_DEFAULT_OBJECT_FACTORY_HPP

#include "ObjectFactory.hpp"
#include <log4cxx/logger.h>
#include <map>

namespace aqlib
{

// A default implementation of an object factory
class DefaultObjectFactory: public ObjectFactory
{
    static log4cxx::LoggerPtr logger;
    
    const std::string mInstanceName;
    
    // Map class path to prototype for creating a new instance of the respective type
    // Class path is mapped "as is", for instance a class Human in namespace Biology.Animals.Primates 
    // is mapped as "Biology.Animals.Primates.Human".
    // For large number of classes and namespaces it might be faster to keep a tree
    // of namespaces and only map the class name at the leaf namespace level.
    typedef std::map<std::string, const Serializable *> Prototypes;
    Prototypes mPrototypes;
    
public:
    DefaultObjectFactory(const std::string &instanceName);
    
    // Implement the ObjectFactory interface
    void registerPrototype(const Serializable *prototype);

    // Implement the ObjectFactory interface
    Serializable* createObject(const std::string &classHierarchy) const;
};

} // namespace aqlib

#endif // __AQLIB_DEFAULT_OBJECT_FACTORY_HPP
