#ifndef __NET_SESSION
#define __NET_SESSION

#include "Connection.hpp"
#include "SimulationService.hpp"

namespace net
{

// A client session. Holds the application-level objects.
// At some point the Connection class might be moved to a library,
// while the session remains at application level.
class Session: public Connection
{
    boost::shared_ptr<SimulationService> mSimulationService;
    
public:
    Session(ConnectionManager *manager, const SocketPtr &socket);

    // Override the Connection interface
    void init();
    void cleanup();
    
    SimulationService* getSimulationService();
};

} // namespace net

#endif // __NET_SESSION
