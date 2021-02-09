#ifndef __NET_CONNECTION_HPP
#define __NET_CONNECTION_HPP

#include "Socket.hpp"
#include "../aqlib/Thread.hpp"
#include "../aqlib/Exception.hpp"
#include "../aqlib/Serializable.hpp"
#include <map>
#include <log4cxx/logger.h>
#include <boost/shared_ptr.hpp>

namespace net
{

class ReceiverThread;
class SenderThread;
class ConnectionManager;

class Connection
{
    static log4cxx::LoggerPtr logger;

    ConnectionManager *mManager;

    SocketPtr mSocket;
    const std::string mInstanceName;

    boost::shared_ptr<ReceiverThread> mReceiverThread;
    boost::shared_ptr<SenderThread> mSenderThread;
 
    aqlib::Monitor mLock;
    bool mStopRequested;
    
public:
    Connection(ConnectionManager *manager, const SocketPtr &socket);
    virtual ~Connection();
    
    const std::string& instanceName() const { return mInstanceName; }
    
    int getId() const { return mSocket->getHandle(); }
    const SocketPtr& getSocket() const { return mSocket; }
    
    void sendMessage(const aqlib::Serializable *message);
    
    virtual void init();
    virtual void cleanup();
    
    // Called when the connection was closed
    virtual void connectionClosed();

    // Called when a message was received
    void messageReceived(const aqlib::SerializablePtr &message);
};

typedef boost::shared_ptr<Connection> ConnectionPtr;

} // namespace net

#endif // __CLIENT_CONNECTION_HPP
