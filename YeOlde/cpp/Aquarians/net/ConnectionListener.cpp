#include "ConnectionListener.hpp"
#include "Session.hpp"
#include "ConnectionManager.hpp"
#include "../aqlib/Exception.hpp"
#include "../aqlib/Value.hpp"
#include "../Application.hpp"

namespace net
{
    
log4cxx::LoggerPtr ConnectionListener::logger(log4cxx::Logger::getLogger("net.ConnectionListener"));

ConnectionListener::ConnectionListener(ConnectionManager *manager, const std::string &host, int port):
    Thread(std::string("CLSN_") + aqlib::Value(Application::getInstance().getNextId())),
    mManager(manager),
    mInstanceName("Clsn." + host + "." + aqlib::Value(port)),
    mStopRequested(false),
    mHost(host),
    mPort(port)
{
}

void ConnectionListener::requestStop()
{
    LOG4CXX_INFO(logger, mInstanceName << " requesting stop");
    aqlib::Lock lock(mLock);
    mStopRequested = true;
    mLock.notify();
    
    // Wake up processor thread blocked in I/O operation
    try
    {
        mSocket.shutdown();
    }
    catch (std::exception &ex)
    {
        LOG4CXX_WARN(logger, mInstanceName << ": " << ex.what());
    }
}

void ConnectionListener::run()
{
    LOG4CXX_INFO(logger, mInstanceName << " running");
    
    while (!isStopRequested())
    {
        try
        {
            process();
        }
        catch (std::exception &ex)
        {
            if (!isStopRequested())
            {
                LOG4CXX_ERROR(logger, mInstanceName << ": " << ex.what());
                break;
            }
        }
    }
    
    LOG4CXX_INFO(logger, mInstanceName << " stopped");
}

bool ConnectionListener::isStopRequested()
{
    aqlib::Lock lock(mLock);
    return mStopRequested;
}

void ConnectionListener::process()
{
    LOG4CXX_INFO(logger, mInstanceName << " listening");
    SocketPtr socket = mSocket.accept();
    LOG4CXX_INFO(logger, mInstanceName << " accepted: " + aqlib::Value(socket->getHandle()));
    
    ConnectionPtr connection;
    try
    {
        // If moving the connection stuff to a library, 
        // the Session should be created by the application
        // and hold the objects necessary for communicating with the client.
        connection = ConnectionPtr(new Session(mManager, socket));
    }
    catch (std::exception &ex)
    {
        LOG4CXX_ERROR(logger, mInstanceName << " failed creating session for: " << aqlib::Value(socket->getHandle()));
        return;
    }
    
    mManager->add(connection);
}

void ConnectionListener::init()
{
    mSocket.listen(mHost, mPort);    
    start();
}

void ConnectionListener::cleanup()
{
    requestStop();
    join();
}

} // namespace net
