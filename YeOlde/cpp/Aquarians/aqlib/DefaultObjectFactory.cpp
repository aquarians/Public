#include "DefaultObjectFactory.hpp"
#include "Serializable.hpp"
#include "Exception.hpp"
#include "StringTokenizer.hpp"

namespace aqlib
{
    
log4cxx::LoggerPtr DefaultObjectFactory::logger(log4cxx::Logger::getLogger("DefaultObjectFactory"));

DefaultObjectFactory::DefaultObjectFactory(const std::string &instanceName):
    mInstanceName(instanceName)
{
}

void DefaultObjectFactory::registerPrototype(const Serializable *prototype)
{
    // Given a C++ class names list, replace the namespace separator "::" with a dot "." (Java style).
    // For instance "varieties::GrannySmith,plants::fruits::Apple" becomes "varieties.GrannySmith,plants.fruits.Apple".
    std::string hierarchy = boost::regex_replace(prototype->getClassHierarchy(), boost::regex("::"), ".");
    if (hierarchy.size() == 0)
    {
        throw aqlib::Exception("Invalid class hierarchy");
    }
    
    LOG4CXX_DEBUG(logger, mInstanceName << " registerPrototype " << hierarchy);
    
    StringTokenizer tokenizer(hierarchy, ",");
    while (tokenizer.hasMoreTokens())
    {
        const std::string type = tokenizer.nextToken();
        if (type.size() > 0)
        {
            mPrototypes[type] = prototype;
        }
    }
}

Serializable* DefaultObjectFactory::createObject(const std::string &classHierarchy) const
{
    //LOG4CXX_DEBUG(logger, mInstanceName << " createObject " << classHierarchy);
    
    // Lookup prototype for this message
    StringTokenizer tokenizer(classHierarchy, ",");
    while (tokenizer.hasMoreTokens())
    {
        const std::string type = tokenizer.nextToken();
        Prototypes::const_iterator it = mPrototypes.find(type);
        if (mPrototypes.end() == it)
        {
            continue;
        }

        // Ask prototype for a new instance of it's kind
        const Serializable *prototype = it->second;
        return prototype->createInstance();
    }

    // Type hierarchy is unknown
    return NULL;
}

} // namespace aqlib
