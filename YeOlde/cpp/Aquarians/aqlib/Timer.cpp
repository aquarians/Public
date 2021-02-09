#include "Timer.hpp"
#include <sys/time.h>
#include <iostream>

namespace aqlib
{

Timer::Timer()
{
    mStartMicroseconds = getMicroseconds();
}

long Timer::getMicroseconds() const
{
    struct timeval timeOfDay;
    gettimeofday(&timeOfDay, NULL);
    long current = (long)timeOfDay.tv_usec + (long)1000000 * (long)timeOfDay.tv_sec;
    return current;
}

long Timer::getElapsedMicroseconds() const
{
    long endMicroseconds = getMicroseconds();
    long elapsed = endMicroseconds - mStartMicroseconds;
    return elapsed;
}

long Timer::getElapsedMilliseconds()  const
{ 
    return getElapsedMicroseconds() / (long)1000; 
}

long Timer::getElapsedSeconds()  const
{ 
    return getElapsedMicroseconds() / (long)1000000; 
}

} // namespace aqlib
