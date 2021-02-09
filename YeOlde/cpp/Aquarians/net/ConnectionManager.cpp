#include "ConnectionManager.hpp"
#include "Connection.hpp"
#include "../aqlib/Exception.hpp"
#include "../aqlib/Value.hpp"
#include <sstream>

namespace net
{
    
log4cxx::LoggerPtr ConnectionManager::logger(log4cxx::Logger::getLogger("net.ConnectionManager"));

ConnectionManager::ConnectionManager():
    Thread("CMGR"),
    mInstanceName("ConMngr"),
    mStopRequested(false)
{
}


ConnectionManager::AddedEvent::AddedEvent(const ConnectionPtr &connection)
{
    mConnection = connection;
}

void ConnectionManager::AddedEvent::process(ConnectionManager *manager)
{
    manager->processAdded(mConnection);
}

ConnectionManager::RemovedEvent::RemovedEvent(int connectionId)
{
    mConnectionId = connectionId;
}

void ConnectionManager::RemovedEvent::process(ConnectionManager *manager)
{
    manager->processRemoved(mConnectionId);
}

void ConnectionManager::requestStop()
{
    aqlib::Lock lock(mLock);
    mStopRequested = true;
    mLock.notify();
}

bool ConnectionManager::isStopRequested()
{
    aqlib::Lock lock(mLock);
    return mStopRequested;
}

void ConnectionManager::add(const ConnectionPtr &connection)
{
    aqlib::Lock lock(mLock);    
    EventPtr event(new AddedEvent(connection));
    mEvents.push_back(event);
    mLock.notify();
}

void ConnectionManager::remove(int connectionId)
{
    aqlib::Lock lock(mLock);    
    EventPtr event(new RemovedEvent(connectionId));
    mEvents.push_back(event);
    mLock.notify();
}

void ConnectionManager::run()
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


void ConnectionManager::process()
{
    // Wait events or stop request
    Events events;
    waitEvents(events);
    if (0 == events.size())
    {
        return;
    }

    LOG4CXX_INFO(logger, mInstanceName << " processing " << events.size() << " events");
    for (Events::iterator it = events.begin(); it != events.end(); ++it)
    {
        try
        {
            const EventPtr &event = *it;
            event->process(this);
        }
        catch (std::exception &ex)
        {
            LOG4CXX_INFO(logger, mInstanceName << " : " << ex.what());
        }
    }    
}

void ConnectionManager::waitEvents(ConnectionManager::Events &events)
{
    aqlib::Lock lock(mLock);
    while ((!mStopRequested) && (0 == mEvents.size()))
    {
        LOG4CXX_INFO(logger, mInstanceName << " waitEvents, waiting");
        mLock.wait();
        LOG4CXX_INFO(logger, mInstanceName << " waitEvents, woke up");
    }
    
    if (mStopRequested)
    {
        return;
    }
    
    events = mEvents;
    mEvents.clear();
}

void ConnectionManager::processAdded(const ConnectionPtr &connection)
{
    LOG4CXX_INFO(logger, mInstanceName << " adding connection: " << connection->getId());
    
    try
    {
        connection->init();
        mConnections.push_back(connection);
    }
    catch (std::exception &ex)
    {
        LOG4CXX_ERROR(logger, mInstanceName << " adding connection: " << connection->getId() << ": " << ex.what());
        try {
            connection->cleanup();
        } catch (std::exception &ignored) {}
    }
}

void ConnectionManager::processRemoved(int connectionId)
{
    LOG4CXX_INFO(logger, mInstanceName << " removing connection: " << connectionId);
    
    try
    {
        ConnectionPtr connection;
        for (Connections::iterator it = mConnections.begin(); it != mConnections.end(); ++it)
        {
            const ConnectionPtr &contained = *it;
            if (contained->getId() != connectionId)
            {
                continue;
            }
            
            connection = contained;
            mConnections.erase(it);            
            break;
        }
        
        if (NULL != connection.get())
        {
            connection->cleanup();
        }
    }
    catch (std::exception &ex) 
    {
        LOG4CXX_ERROR(logger, mInstanceName << " removing connection: " << connectionId << ": " << ex.what());
    }   
}

void ConnectionManager::init()
{
    start();    
}

void ConnectionManager::cleanup()
{
    requestStop();
    join();
    
    while (mConnections.size() != 0)
    {
        const ConnectionPtr &connection = *(mConnections.begin());
        processRemoved(connection->getId());
    }
}

} // namespace net
