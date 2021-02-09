#include "Thread.hpp"
#include "Exception.hpp"
#include <sstream>
#include <errno.h>
#include <log4cxx/mdc.h>

namespace aqlib
{

/////////////////////////////////////
// Mutex class
/////////////////////////////////////
Mutex::Mutex()
{
    // Recursive mutex
    pthread_mutexattr_t type;
    pthread_mutexattr_init(&type);
    pthread_mutexattr_settype(&type, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&mMutex, &type);
}

Mutex::~Mutex()
{
    pthread_mutex_destroy(&mMutex);
}

void Mutex::lock()
{
    pthread_mutex_lock(&mMutex);
}

void Mutex::unlock()
{
    pthread_mutex_unlock(&mMutex);
}

/////////////////////////////////////
// Lock class
/////////////////////////////////////
Lock::Lock(Mutex &mutex):
    mMutex(mutex)
{
    mMutex.lock();
}

Lock::~Lock()
{
    mMutex.unlock();
}

/////////////////////////////////////
// Monitor class
/////////////////////////////////////
Monitor::Monitor()
{
    pthread_cond_init(&mCondition, NULL);
}

Monitor::~Monitor()
{
    int retval = pthread_cond_destroy(&mCondition);
    if (0 != retval)
    {
        std::stringstream msgStrm;
        msgStrm << "Error destrying condition: " << retval;
        throw Exception(msgStrm.str());
    }
}

void Monitor::wait(long milliseconds)
{
    if (milliseconds <= 0)
    {
        pthread_cond_wait(&mCondition, &mMutex);
        return;
    }
    
    timespec duration;
    duration.tv_sec = milliseconds / (long)1000;
    duration.tv_nsec = (milliseconds % (long)1000) * (long)1000000;
    pthread_cond_timedwait(&mCondition, &mMutex, &duration);
}

void Monitor::notify()
{
    pthread_cond_broadcast(&mCondition); 
}

/////////////////////////////////////
// Thread class
/////////////////////////////////////
log4cxx::LoggerPtr Thread::logger(log4cxx::Logger::getLogger("Thread"));

Thread::Thread(const std::string &name):
    mName(name)
{
}

Thread::~Thread()
{
}

void Thread::start()
{
    // Create thread
    if (0 != pthread_create(&mThreadId, NULL, myStartRoutine, this))
    {
        std::stringstream msgStrm;
        msgStrm << "Error creating thread: " << mName << ": " << errno;
        throw Exception(msgStrm.str());
    }
}

void* Thread::myStartRoutine(void *param)
{
    Thread *thread = (Thread *)param;
    
    // Set the thread name
    log4cxx::MDC::put("tid", thread->mName);
    
    // Run the thread routine
    try
    {
        thread->run();
    }
    catch (std::exception &ex)
    {
        LOG4CXX_WARN(logger, "Running thread: " << thread->mName << ": " << ex.what())
    }    
    
    return NULL;
}

void Thread::join()
{
    if (0 != pthread_join(mThreadId, NULL))
    {
        std::stringstream msgStrm;
        msgStrm << "Error joining thread: " << mName;
        throw Exception(msgStrm.str());
    }
}

} // namespace aqlib
