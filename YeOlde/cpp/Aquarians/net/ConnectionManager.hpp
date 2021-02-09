#ifndef __CLIENT_CONNECTION_MANAGER_HPP
#define __CLIENT_CONNECTION_MANAGER_HPP

#include <list>
#include "../aqlib/Thread.hpp"
#include "Connection.hpp"
#include <boost/shared_ptr.hpp>

namespace net
{

class Connection;

class ConnectionManager: public aqlib::Thread
{
    static log4cxx::LoggerPtr logger;
    
    const std::string mInstanceName;
    aqlib::Monitor mLock;
    bool mStopRequested;    
    
    typedef std::list<ConnectionPtr> Connections;
    Connections mConnections;
    
    class Event 
    {
    protected:
        Event() {}
    public:
        virtual ~Event() {}
        virtual void process(ConnectionManager *manager) = 0;
    };
    typedef boost::shared_ptr<Event> EventPtr;
    typedef std::list<EventPtr> Events;
    Events mEvents;

public:
    ConnectionManager();
    
    void add(const ConnectionPtr &connection);
    void remove(int connectionId);
    
    // Implement the Thread interface
    void run();
    
    void requestStop();
    bool isStopRequested();
    
    void init();
    void cleanup();
    
private:
    void process();
    
    void waitEvents(Events &events);
    
    void processAdded(const ConnectionPtr &connection);
    void processRemoved(int connectionId);
    
    class AddedEvent: public Event
    {
        ConnectionPtr mConnection;
    public:
        AddedEvent(const ConnectionPtr &connection);
        void process(ConnectionManager *manager);
    };
    friend class RemovedEvent;
    
    class RemovedEvent: public Event
    {
        int mConnectionId;
    public:
        RemovedEvent(int connectionId);
        void process(ConnectionManager *manager);
    };
    friend class RemovedEvent;    
};

} // namespace net

#endif // __CLIENT_CONNECTION_MANAGER_HPP
