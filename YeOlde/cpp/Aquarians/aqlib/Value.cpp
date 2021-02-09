#include "Value.hpp"
#include "Exception.hpp"
#include <sstream>
#include <stdlib.h>
#include <iostream>
#include <boost/concept_check.hpp>
#include <locale>
#include <boost/date_time/date_facet.hpp>
#include <boost/date_time/time_facet.hpp>
#include <boost/regex.hpp>
#include <boost/date_time/gregorian/gregorian.hpp>

namespace aqlib
{

log4cxx::LoggerPtr Value::logger(log4cxx::Logger::getLogger("Value"));
const std::string Value::WHITECHARS = " \t\r\n";

Value::BoostFractionalSecondsPrecision Value::gBoostFSP;

Value::Value(const std::string &value): std::string(value)
{
    //LOG4CXX_INFO(logger, "Value(const std::string &value) value=" << value)
}

Value::Value(const char *text): std::string(text)
{
    //LOG4CXX_INFO(logger, "Value(const char *text) text=" << text)
}

Value::Value(int32_t value): std::string(toString(value))
{
}

Value::Value(int64_t value): std::string(toString(value))
{
}

Value::Value(bool value): std::string(toString(value))
{
}

Value::Value(double value, int precision): std::string(toString(value, precision))
{
}

Value::Value(const boost::posix_time::ptime &value): std::string(toString(value))
{
}

Value::Value(const boost::gregorian::date &value): std::string(toString(value))
{
}

Value::Value(const boost::posix_time::time_duration &value): std::string(toString(value))
{
}

bool Value::isWhiteChar(char value)
{
    for (int i = 0; i < WHITECHARS.size(); ++i)
    {
        if (WHITECHARS[i] == value)
        {
            return true;
        }
    }
    
    return false;
}

std::string Value::toString(int32_t value)
{
    std::stringstream valueStrm;
    valueStrm << value;
    return valueStrm.str();
}

std::string Value::toString(int64_t value)
{
    std::stringstream valueStrm;
    valueStrm << value;
    return valueStrm.str();
}

std::string Value::toString(bool value)
{
    return value ? "true" : "false";
}

std::string Value::toString(double value, int precision)
{
    std::stringstream valueStrm;
    valueStrm << std::setprecision(precision > 0 ? precision : 9) << value;
    return valueStrm.str();
}

std::string Value::toString(const boost::posix_time::ptime &value)
{
    // To YYYY-mmm-DD HH:MM:SS.fffffffff string where mmm 3 char month name. Fractional seconds only included if non-zero.
    return boost::posix_time::to_simple_string(value);
}

std::string Value::toString(const boost::gregorian::date &value)
{
    // To YYYY-MM-DD where all components are integers, ex: "2002-01-31"
    return boost::gregorian::to_iso_extended_string(value);
}

std::string Value::toString(const boost::posix_time::time_duration &value)
{
    // To hh:mm:ss.fff...    
    char buf[128];
    long fs = value.fractional_seconds();    
    switch (gBoostFSP.getPrecision())
    {
        // Microsecond precision
        case 1000000:
            sprintf(buf, "%02d:%02d:%02d.%06d", 
                    value.hours(), value.minutes(), value.seconds(), value.fractional_seconds());
            break;
        // Nanosecond precision
        case 1000000000:
            sprintf(buf, "%02d:%02d:%02d.%09d", 
                    value.hours(), value.minutes(), value.seconds(), value.fractional_seconds());
            break;
        default:
            throw Exception(
                std::string("Cannot handle boost fractional_seconds precision=") + 
                Value((int64_t)gBoostFSP.getPrecision()));
    }
    return buf;
}

int32_t Value::toInt() const
{
    int32_t value = 0;
    std::stringstream valueStrm(*this);    
    valueStrm >> value;
    
    if (valueStrm.eof())
    {
        return value;
    }
    
    throw Exception(std::string("Invalid int: ") + (*this));
}

int64_t Value::toLong() const
{
    int64_t value = 0;
    std::stringstream valueStrm(*this);    
    valueStrm >> value;
    
    if (valueStrm.eof())
    {
        return value;
    }
    
    throw Exception(std::string("Invalid long: ") + (*this));
}

double Value::toDouble() const
{
    double value = 0;
    std::stringstream valueStrm(*this);    
    valueStrm >> value;
    
    if (valueStrm.eof())
    {
        return value;
    }
    
    throw Exception(std::string("Invalid double: ") + (*this));
}

bool Value::toBool() const
{
    static const int lValuesCount = 3;
    static const std::string lTrueValues[] = {"true", "t", "1"};
    static const std::string lFalseValues[] = {"false", "f", "0"};
    
    for (int i = 0; i < lValuesCount; ++i)
    {
        if ((*this) == lTrueValues[i])
        {
            return true;
        }
        
        if ((*this) == lFalseValues[i])
        {
            return false;
        }
    }    

    throw Exception(std::string("Invalid bool: ") + (*this));
}

boost::posix_time::ptime Value::toTime() const
{
    return boost::posix_time::time_from_string(*this);
}

boost::gregorian::date Value::toDate() const
{
    //  From delimited date string where with order year-month-day eg: 2002-1-25
    return boost::gregorian::from_string(*this);
}

boost::posix_time::time_duration Value::toDuration() const
{
    return parseClock(*this);
}

void Value::trim(std::string &text, const std::string &whitechars)
{
    // Find the first non-whitechar character
    int first = 0;
    while (first < text.size())
    {
        char value = text[first];
        if (isWhiteChar(value))
        {
            first++;
            continue;
        }
        
        break;
    }
    
    // Check if after the end of string
    if (first >= text.size())
    {
        return;
    }
    
    // Find the last non-whitechar character
    int last = text.size() - 1;
    while (last > -1)
    {
        char value = text[last];
        if (isWhiteChar(value))
        {
            last--;
            continue;
        }
        
        break;
    }
    
    // Check if before the begin of string
    if (last < 0)
    {
        return;
    }
    
    // Extract the part between first and last
    text = text.substr(first, last - first + 1);
}

void Value::toLower(std::string &text)
{
    char offset = ('a' - 'A');
    for (int i = 0; i < text.size(); ++i)
    {
        if ((text[i] < 'A') || (text[i] > 'Z'))
        {
            continue;
        }
        
        text[i] += offset;
    }
}

Value Value::trim() const
{
    std::string value = *this;
    trim(value);
    return value;
}

Value Value::toLower() const
{
    std::string value = *this;
    toLower(value);
    return value;
}

std::string& Value::operator=(const std::string &value)
{
    return std::string::operator=(value);
}

boost::gregorian::date Value::parseDate(const std::string &text, const std::string &format)
{
    boost::regex expression(format);
    boost::smatch matches;
    if (!boost::regex_search(text.begin(), text.end(), matches, expression))
    {
        throw Exception(std::string("Invalid date: ") + text + " format: " + format);
    }
    
    Value yearText = matches[1].str();
    Value monthText = matches[2].str();
    Value dayText = matches[3].str();
    
    boost::gregorian::date result(
        yearText.toInt(), monthText.toInt(), dayText.toInt());
        
    return result;
}

boost::posix_time::time_duration Value::parseClock(const std::string &text, const std::string &format)
{
    boost::regex expression(format);
    boost::smatch matches;
    if (!boost::regex_search(text.begin(), text.end(), matches, expression))
    {
        throw Exception(std::string("Invalid time: ") + text + " format: " + format);
    }
    
    Value hourText = matches[1].str();
    Value minuteText = matches[2].str();
    Value secondText = matches[3].str();
    Value milisecondText = matches[4].str();
    
    int hour = hourText.toInt();
    int minute = minuteText.toInt();
    int second = secondText.toInt();
    long fraction = milisecondText.toInt();
    switch (gBoostFSP.getPrecision())
    {
        // Microsecond precision
        case 1000000:
            fraction *= 1000;
            break;
        // Nanosecond precision
        case 1000000000:
            fraction *= 1000000;
            break;
        default:
            throw Exception(
                std::string("Cannot handle boost fractional_seconds precision=") + 
                Value((int64_t)gBoostFSP.getPrecision()));
    }
     
    boost::posix_time::time_duration result(hour, minute, second, fraction);
    return result;
}

Value::BoostFractionalSecondsPrecision::BoostFractionalSecondsPrecision()
{
    mPrecision = 1;
    for (int i = 0; i < 12; ++i)
    {
        boost::posix_time::time_duration td(0, 0, 0, mPrecision);
        if (td.fractional_seconds() == mPrecision)
        {
            mPrecision *= 10;
            continue;
        }
        
        break;
    }
}

int Value::getMonthIndex(const std::string &name)
{
    std::string lowerName = name;
    boost::algorithm::to_lower(lowerName);
    
    boost::gregorian::greg_month::month_map_ptr_type months = boost::gregorian::greg_month::get_month_map_ptr();
    boost::gregorian::greg_month::month_map_type::iterator it = months->find(lowerName);
    if (months->end() == it)
    {
        throw Exception(std::string("Invalid month: ") + name);
    }
    
    return it->second;
}

std::string Value::toHexString(const std::string &input)
{
    static const char* const hextable = "0123456789ABCDEF";
    size_t len = input.length();

    std::string output;
    output.reserve(2 * len);
    for (size_t i = 0; i < len; ++i)
    {
        const unsigned char c = input[i];
        output.push_back(hextable[c >> 4]);
        output.push_back(hextable[c & 15]);
    }
    return output;
}

} // namespace aqlib
