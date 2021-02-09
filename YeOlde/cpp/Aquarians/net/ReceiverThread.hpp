#ifndef __NET_RECEIVER_THREAD_HPP
#define __NET_RECEIVER_THREAD_HPP

#include "../aqlib/Thread.hpp"

namespace net
{
    
class Connection;

class ReceiverThread: public aqlib::Thread
{
    static log4cxx::LoggerPtr logger;
    
    aqlib::Monitor mLock;
    const std::string mInstanceName;
    Connection *mOwner;
    bool mStopRequested;

    // Time to wait between two send attempts when socket recv() returned 0
    static const int RECEIVE_RETRY_WAIT_MILLISECONDS;
    
public:
    ReceiverThread(Connection *owner);
    
    void run();

    void requestStop();

private:    
    bool isStopRequested();
    
    void receivePacket(char *buf, int len);
    
    void process();    
};

} // namespace net

#endif // __NET_RECEIVER_THREAD_HPP
