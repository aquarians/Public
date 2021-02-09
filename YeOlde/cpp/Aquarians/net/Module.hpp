#ifndef __NET_MODULE_HPP
#define __NET_MODULE_HPP

#include "../aqlib/Module.hpp"
#include <log4cxx/logger.h>

namespace net
{
    
class ConnectionManager;
class ConnectionListener;

class Module: public aqlib::Module
{
    static log4cxx::LoggerPtr logger;
    static const std::string NAME;
    
    boost::shared_ptr<ConnectionManager> mConnectionManager;
    boost::shared_ptr<ConnectionListener> mConnectionListener;
    
public:
    Module();
    
    // Implement the Module interface
    const std::string& getModuleName() const;
    void init();
    void cleanup();
};

} // namespace net

#endif // __NET_MODULE_HPP
