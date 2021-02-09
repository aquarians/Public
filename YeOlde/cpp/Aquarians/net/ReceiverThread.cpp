#include "ReceiverThread.hpp"
#include "Connection.hpp"
#include "../aqlib/Exception.hpp"
#include "../aqlib/Value.hpp"
#include "../aqlib/Timer.hpp"
#include "../aqlib/BinaryArchive.hpp"
#include "../aqlib/Serializable.hpp"
#include "../aqlib/ObjectFactory.hpp"
#include "../Application.hpp"
#include <sstream>
#include <string.h>
#include <memory>

namespace net
{

log4cxx::LoggerPtr ReceiverThread::logger(log4cxx::Logger::getLogger("net.ReceiverThread"));
const int ReceiverThread::RECEIVE_RETRY_WAIT_MILLISECONDS = 1000;

ReceiverThread::ReceiverThread(Connection *owner):
    Thread(std::string("RCV_") + aqlib::Value(Application::getInstance().getNextId())),
    mInstanceName(std::string("Rcv.") + aqlib::Value(owner->getId()))
{
    mOwner = owner;
    mStopRequested = false;    
}

void ReceiverThread::requestStop()
{
    LOG4CXX_INFO(logger, mInstanceName << " requesting stop");
    aqlib::Lock lock(mLock);
    mStopRequested = true;
    mLock.notify();
}

bool ReceiverThread::isStopRequested()
{    
    aqlib::Lock lock(mLock);
    return mStopRequested;
}

void ReceiverThread::run()
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
                mOwner->connectionClosed();
            }
            break;
        }
    }
    
    LOG4CXX_INFO(logger, mInstanceName << " stopped");
}

void ReceiverThread::receivePacket(char *buf, int len)
{
    int off = 0;
    while (len > 0)
    {
        LOG4CXX_INFO(logger, mInstanceName << " receivePacket, receiving len=" << len);
        int count = mOwner->getSocket()->recv(buf + off, len);
        LOG4CXX_INFO(logger, mInstanceName << " receivePacket, received count=" << count);
        if (0 == count)
        {
            // Connection was closed
            LOG4CXX_INFO(logger, mInstanceName << " : connection closed");
            requestStop();
            mOwner->connectionClosed();
            return;
        }
        
        off += count;
        len -= count;
    }    
}

void ReceiverThread::process()
{
    // Protocol: stream of encoded objects where an object is encoded as <size> followed by <data>

    // Receive size
    std::string header(sizeof(int32_t), 0);
    receivePacket((char *)header.data(), header.size());
    
    // Parse size
    aqlib::BinaryReadArchive sizeArchive(NULL, header.data(), header.size());
    int size = sizeArchive.readInt("");
    if ((size < 0) || (size > aqlib::BinaryWriteArchive::MAXIMUM_ARCHIVE_SIZE))
    {
        std::stringstream msgStrm;
        msgStrm << mInstanceName << ": invalid size: " << size;
        throw aqlib::Exception(msgStrm.str());
    }    
    
    // Allocate buffer for the object (including the already received header)
    std::string packet(header.size() + size, 0);
    
    // Copy the already received part
    for (int i = 0; i < header.size(); ++i) 
    {
        packet[i] = header[i];
    }
    
    // Receive data    
    receivePacket((char *)packet.data() + header.size(), size);    
    LOG4CXX_DEBUG(logger, mInstanceName << " received Message: " << aqlib::Value::toHexString(std::string(packet.data(), packet.size())));
    
    // Parse message
    aqlib::BinaryReadArchive messageArchive(Application::getInstance().getObjectFactory(), packet.data(), packet.size());
    aqlib::SerializablePtr message;
    try
    {    
        message = aqlib::SerializablePtr(messageArchive.readObject(""));
    } 
    catch (std::exception &ex)
    {
        LOG4CXX_WARN(logger, mInstanceName << ": " << ex.what());
        return;
    }
    
    if (NULL == message.get())
    {
        LOG4CXX_WARN(logger, mInstanceName << " Received NULL message");
        return;
    }

    // Notify owner
    try 
    {    
        mOwner->messageReceived(message);
    }
    catch (std::exception &ex)
    {
        LOG4CXX_WARN(logger, mInstanceName << ": message type: " << message->getClassHierarchy() << ": " << ex.what());
    }
}

} // namespace net
