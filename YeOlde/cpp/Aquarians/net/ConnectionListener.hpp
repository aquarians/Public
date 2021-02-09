#ifndef __NET_CONNECTION_LISTENER_HPP
#define __NET_CONNECTION_LISTENER_HPP

#include "../aqlib/Thread.hpp"
#include "Socket.hpp"

namespace net
{
    
class ConnectionManager;

class ConnectionListener: public aqlib::Thread
{
    static log4cxx::LoggerPtr logger;
    
    ConnectionManager *mManager;
    const std::string mInstanceName;
    Socket mSocket;
    aqlib::Monitor mLock;
    bool mStopRequested;
    const std::string mHost;
    const int mPort;
    
public:
    ConnectionListener(ConnectionManager *manager, const std::string &host, int port);
    
    void requestStop();
    
    // Implement the Thread interface
    void run();
    
    void init();
    void cleanup();
    
private:
    void process();
    
    bool isStopRequested();
};

} // namespace net

#endif // __NET_CONNECTION_LISTENER_HPP
