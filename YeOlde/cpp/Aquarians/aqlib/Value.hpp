#ifndef __AQLIB_VALUE_HPP
#define __AQLIB_VALUE_HPP

#include <string>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <stdint.h>
#include <log4cxx/logger.h>

namespace aqlib
{

class Value: public std::string
{
    static log4cxx::LoggerPtr logger;
    
    // Used to detect the precision of boost's time_duration class, fractional_seconds member
    class BoostFractionalSecondsPrecision
    {
        long mPrecision;

    public:
        BoostFractionalSecondsPrecision();
        
        long getPrecision() const { return mPrecision; }
    };
    
    static BoostFractionalSecondsPrecision gBoostFSP;
    
public:
    static const std::string WHITECHARS;

    Value(const std::string &value);
    Value(const char *text);
    Value(int32_t value);
    Value(int64_t value);
    Value(bool value);
    Value(double value, int precision = 0);
    Value(const boost::posix_time::ptime &value);
    Value(const boost::gregorian::date &value);
    Value(const boost::posix_time::time_duration &value);
    
    std::string& operator=(const std::string &value);
    
    static std::string toString(int32_t value);
    static std::string toString(int64_t value);
    static std::string toString(bool value);
    static std::string toString(double value, int precision = 0);
    static std::string toString(const boost::posix_time::ptime &value);
    static std::string toString(const boost::gregorian::date &value);
    static std::string toString(const boost::posix_time::time_duration &value);
    static std::string toHexString(const std::string &data);
    
    static int getMonthIndex(const std::string &name);

    int32_t toInt() const;
    int64_t toLong() const;
    double toDouble() const;
    bool toBool() const;
    boost::posix_time::ptime toTime() const;
    boost::gregorian::date toDate() const;
    boost::posix_time::time_duration toDuration() const;
        
    static bool isWhiteChar(char value);
    static void trim(std::string &text, const std::string &whitechars = WHITECHARS);
    static void toLower(std::string &text);
        
    Value trim() const;
    Value toLower() const;
        
    // Format must contain the regular expression for selecting the year, month and day
    static boost::gregorian::date parseDate(
                const std::string &text, 
                const std::string &format = "([0-9]{4})([0-9]{2})([0-9]{2})");
                
    // Format must contain the regular expression for selecting the hour, minute, second and miliseconds
    static boost::posix_time::time_duration parseClock(
                const std::string &text, 
                const std::string &format = "([0-9]{2}):([0-9]{2}):([0-9]{2}).([0-9]{3})");
};

} // namespace aqlib

#endif // __AQLIB_VALUE_HPP
