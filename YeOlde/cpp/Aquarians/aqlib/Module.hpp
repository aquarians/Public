#ifndef __AQLIB_MODULE_HPP
#define __AQLIB_MODULE_HPP

#include <string>
#include <boost/shared_ptr.hpp>

namespace aqlib
{

// An application module. For instance, net, database , pricing, positions module etc.
class Module
{
protected:
    Module() {}
public:
    virtual ~Module() {}
    
    virtual void init() = 0;
    virtual void cleanup() = 0;

    virtual const std::string& getModuleName() const = 0;
};

typedef boost::shared_ptr<Module> ModulePtr;

} // namespace aqlib

#endif // __AQLIB_MODULE_HPP
