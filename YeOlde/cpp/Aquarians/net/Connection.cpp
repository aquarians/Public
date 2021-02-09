#include "Connection.hpp"
#include "ConnectionManager.hpp"
#include "ReceiverThread.hpp"
#include "SenderThread.hpp"
#include "Message.hpp"
#include "../aqlib/Value.hpp"
#include "../aqlib/StringTokenizer.hpp"
#include "../aqlib/BinaryArchive.hpp"
#include "../aqlib/Serializable.hpp"
#include "../Application.hpp"
#include <sstream>
#include <string.h>

namespace net
{

log4cxx::LoggerPtr Connection::logger(log4cxx::Logger::getLogger("net.Connection"));

Connection::Connection(ConnectionManager *manager, const SocketPtr &socket):
    mManager(manager),
    mSocket(socket),
    mInstanceName(std::string("Conn.") + aqlib::Value(socket)),
    mStopRequested(false)
{    
    mReceiverThread = boost::shared_ptr<ReceiverThread>(new ReceiverThread(this));
    mSenderThread = boost::shared_ptr<SenderThread>(new SenderThread(this));
    
    LOG4CXX_INFO(logger, mInstanceName << " : Created.");
}

Connection::~Connection()
{
    LOG4CXX_INFO(logger, mInstanceName << " : Destroyed.");
}

void Connection::init()
{
    mReceiverThread->start();
    mSenderThread->start();    
}

void Connection::cleanup()
{
    LOG4CXX_ERROR(logger, mInstanceName << " cleaning up");
    
    mReceiverThread->requestStop();
    mSenderThread->requestStop();
    
    // Wake up processor threads blocked in I/O operation
    try 
    {
        mSocket->shutdown();
    } 
    catch (aqlib::Exception ex) 
    {
        LOG4CXX_ERROR(logger, mInstanceName << ": " << ex.toString());
    }
    
    mReceiverThread->join();
    mSenderThread->join();
    
    LOG4CXX_ERROR(logger, mInstanceName << " cleaned up");
}

void Connection::connectionClosed()
{
    mManager->remove(getId());
}

void Connection::sendMessage(const aqlib::Serializable *message)
{
    // Protocol: stream of encoded objects
    aqlib::BinaryWriteArchive archive;
    archive.writeObject("message", message);
    mSenderThread->post(archive.data(), archive.size());
    LOG4CXX_DEBUG(logger, mInstanceName << " sendMessage: " << aqlib::Value::toHexString(std::string(archive.data(), archive.size())));
}

void Connection::messageReceived(const aqlib::SerializablePtr &message)
{
    LOG4CXX_DEBUG(logger, mInstanceName << " Received message of type: " << message->getClassHierarchy());
    
    // Remote procedure call: ask object to do it's job
    Message *netMessage = dynamic_cast<Message *>(message.get());
    if (NULL != netMessage)
    {
        netMessage->execute(this);
    }
}

} // namespace net
