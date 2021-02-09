#ifndef __AQLIB_THREAD_HPP
#define __AQLIB_THREAD_HPP

#include <pthread.h>
#include <map>
#include <string>
#include <log4cxx/logger.h>

namespace aqlib
{

class Mutex
{
public:
    Mutex();
    virtual ~Mutex();

    void lock();
    void unlock();

protected:
    pthread_mutex_t mMutex;
};

class Lock
{
public:
    Lock(Mutex &mutex);
    virtual ~Lock();

private:
    Mutex &mMutex;
};

class Monitor: public Mutex
{
public:
    Monitor();
    virtual ~Monitor();
    
    void wait(long milliseconds = 0);
    void notify();

protected:
    pthread_cond_t mCondition;
};

class Thread
{
    static log4cxx::LoggerPtr logger;

public:
    Thread(const std::string &name);
    virtual ~Thread();

    void start();
    void join();

    virtual void run() = 0;

private:
    const std::string mName;
    pthread_t mThreadId;
    static void* myStartRoutine(void *param);
};

} // namespace aqlib

#endif // __AQLIB_THREAD_HPP
