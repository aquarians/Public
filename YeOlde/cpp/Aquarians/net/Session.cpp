#include "Session.hpp"

namespace net
{
    
Session::Session(ConnectionManager *manager, const SocketPtr &socket):
    Connection(manager, socket)
{
    mSimulationService = boost::shared_ptr<SimulationService>(new SimulationService(this));
}

SimulationService* Session::getSimulationService()
{
    return mSimulationService.get();
}

void Session::init()
{    
    Connection::init();
    mSimulationService->init();
}

void Session::cleanup()
{
    mSimulationService->cleanup();    
    Connection::cleanup();
}

} // namespace net
