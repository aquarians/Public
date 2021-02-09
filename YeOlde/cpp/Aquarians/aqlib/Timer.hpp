#ifndef __AQLIB_TIMER_HPP
#define __AQLIB_TIMER_HPP

namespace aqlib
{

// Computes elapsed time
class Timer
{
    long mStartMicroseconds;

public:
    Timer();   

    long getElapsedMicroseconds() const;
    long getElapsedMilliseconds() const;
    long getElapsedSeconds() const;
    
private:
    long getMicroseconds() const;
};

} // namespace aqlib

#endif // __AQLIB_TIMER_HPP
