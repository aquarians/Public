#ifndef __NET_SENDER_THREAD_HPP
#define __NET_SENDER_THREAD_HPP

#include "../aqlib/Thread.hpp"
#include <list>
#include <string>
#include <boost/shared_ptr.hpp>
#include <log4cxx/logger.h>

namespace net
{
    
class Connection;

class SenderThread: public aqlib::Thread
{
    static log4cxx::LoggerPtr logger;
    
    aqlib::Monitor mLock;
    const std::string mInstanceName;
    Connection *mOwner;    
    bool mStopRequested;

    // The packets queued for sending
    typedef boost::shared_ptr<std::string> Packet;
    typedef std::list<Packet> Packets;
    Packets mPackets;
    
    // Current size of the queue
    int mQueueSize;
    
    // Maximum size of the packet queue, in bytes
    static const int MAX_QUEUE_SIZE;
    
    // Time to wait between two send attempts when socket send() returned 0
    static const int SEND_RETRY_WAIT_MILLISECONDS;
    
public:
    SenderThread(Connection *owner);
    
    // Implement the Thread interface
    void run();

    void requestStop();
    
    // Queues data for sending
    void post(const char *data, int length);

private:
    bool isStopRequested();
    
    void sendPacket(const char *buf, int len);
    
    void process();
    
    Packet getNextPacket();
};

} //namespace net

#endif // __NET_SENDER_THREAD_HPP
