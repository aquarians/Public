#include "SenderThread.hpp"
#include "Connection.hpp"
#include "../aqlib/Exception.hpp"
#include "../aqlib/Value.hpp"
#include "../aqlib/Timer.hpp"
#include "../Application.hpp"
#include <sstream>
#include <string.h>
#include <memory>

namespace net
{

log4cxx::LoggerPtr SenderThread::logger(log4cxx::Logger::getLogger("net.SenderThread"));
const int SenderThread::MAX_QUEUE_SIZE = 10 * 1024 * 1024; // 10 Mb
const int SenderThread::SEND_RETRY_WAIT_MILLISECONDS = 1000;

SenderThread::SenderThread(Connection *owner):
    Thread(std::string("SND_") + aqlib::Value(Application::getInstance().getNextId())),
    mInstanceName(std::string("Snd.") + aqlib::Value(owner->getId()))
{
    mOwner = owner;
    mStopRequested = false;
    mQueueSize = 0;
}

void SenderThread::requestStop()
{
    LOG4CXX_INFO(logger, mInstanceName << " requesting stop");
    aqlib::Lock lock(mLock);
    mStopRequested = true;
    mLock.notify();
}

bool SenderThread::isStopRequested()
{
    aqlib::Lock lock(mLock);
    return mStopRequested;
}

void SenderThread::run()
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

void SenderThread::post(const char *data, int length)
{
    if (length <= 0)
    {
        std::stringstream msgStrm;
        msgStrm << mInstanceName << ": invalid length: " << length;
        throw aqlib::Exception(msgStrm.str());
    }
    
    // Get the lock
    aqlib::Lock lock(mLock);

    // Wait until queue is ready
    while ((mQueueSize + length > MAX_QUEUE_SIZE) && (!mStopRequested)) 
    {
        LOG4CXX_INFO(logger, mInstanceName << " queuePacket, waiting");
        mLock.wait();
        LOG4CXX_INFO(logger, mInstanceName << " queuePacket, woke up");
    }
    
    // Check for stop request
    if (mStopRequested) 
    {
        return;
    }
    
    // Push packet
    Packet packet = Packet(new std::string(data, length)); 
    mPackets.push_back(packet);
    mQueueSize += length;
    //LOG4CXX_DEBUG(logger, mInstanceName << " posted packet of length " << length);

    // Wake up processor thread
    mLock.notify();
}

void SenderThread::sendPacket(const char *buf, int len)
{
    int off = 0;
    while (len > 0)
    {
        int count = mOwner->getSocket()->send(buf + off, len);
        if (0 == count)
        {
            LOG4CXX_INFO(logger, mInstanceName << " sendPacket, waiting");
            aqlib::Lock lock(mLock);
            aqlib::Timer timer;
            while ((!mStopRequested) && (timer.getElapsedMilliseconds() < SEND_RETRY_WAIT_MILLISECONDS))
            {
                mLock.wait(SEND_RETRY_WAIT_MILLISECONDS);
            }
            if (mStopRequested)
            {
                return;
            }
            LOG4CXX_INFO(logger, mInstanceName << " sendPacket, woke up");
        }

        off += count;
        len -= count;
    }    
}

void SenderThread::process()
{
    Packet packet = getNextPacket();
    if (NULL == packet.get())
    {
        return;
    }

    sendPacket(packet->data(), packet->size());
}

SenderThread::Packet SenderThread::getNextPacket()
{
    // Get the lock
    aqlib::Lock lock(mLock);
    
    // Wait until a packet is available or stop was requested
    while ((0 == mPackets.size()) && (!mStopRequested)) 
    {
        LOG4CXX_INFO(logger, mInstanceName << " getNextPacket, waiting");
        mLock.wait();
        LOG4CXX_INFO(logger, mInstanceName << " getNextPacket, woke up");
    }
    
    // Check for stop request
    if (mStopRequested) 
    {
        return SenderThread::Packet();
    }
    
    // Pop packet
    Packet packet = mPackets.front();
    mPackets.pop_front();
    mQueueSize -= packet->length();    
    return packet;
}

} //namespace net
