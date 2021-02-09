#include "Module.hpp"
#include "ConnectionManager.hpp"
#include "ConnectionListener.hpp"
#include "../Application.hpp"
#include "../aqlib/Value.hpp"

namespace net
{

log4cxx::LoggerPtr Module::logger(log4cxx::Logger::getLogger("net.Module"));
const std::string Module::NAME = "net.Module";
    
Module::Module()
{
    std::string host = Application::getInstance().getProperty("net.host", "0.0.0.0");
    aqlib::Value port = Application::getInstance().getProperty("net.port", "12345");    
    LOG4CXX_INFO(logger, NAME << " host=" << host << " port=" << port);
    
    mConnectionManager = boost::shared_ptr<ConnectionManager>(new ConnectionManager());
    mConnectionListener = boost::shared_ptr<ConnectionListener>(new ConnectionListener(mConnectionManager.get(), host, port.toInt()));
}

const std::string& Module::getModuleName() const
{
    return NAME;
}

void Module::init()
{
    mConnectionManager->init();
    mConnectionListener->init();
}

void Module::cleanup()
{
    mConnectionManager->cleanup();
    mConnectionListener->cleanup();
}

} // namespace net
