#ifndef __AQLIB_ARCHIVE_HPP
#define __AQLIB_ARCHIVE_HPP

#include <string>
#include <stdint.h>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace aqlib
{

class Serializable;
class ObjectFactory;

// Generic write archive. Projected implementations: binary and XML.
class WriteArchive
{
protected:
    WriteArchive() {}
public:
    virtual ~WriteArchive() {}
    
    // Prototype-method: create a new object of this class, using the default constructor.
    virtual WriteArchive* createInstance() const = 0;
    
    // Return the stored data of this archive
    virtual std::string getData() const = 0;

    virtual void writeInt(const std::string &name, int32_t value) = 0;
    virtual void writeFloat(const std::string &name, float value) = 0;
    virtual void writeBool(const std::string &name, bool value) = 0;
    virtual void writeString(const std::string &name, const std::string &value) = 0;
    virtual void writeObject(const std::string &name, const Serializable *value) = 0;
    virtual void writeTime(const std::string &name, const boost::posix_time::ptime &value) = 0;
};

// Generic read archive. Projected implementations: binary and XML.
class ReadArchive
{
protected:
    ReadArchive() {}
public:
    virtual ~ReadArchive() {}
    
    // Prototype-method: create a new object of this class, using the default constructor.
    virtual ReadArchive* createInstance(const char *data, int length) const = 0;
    
    // If data is stored in a streaming way, then it returns true if reading hasn't yet reached end of stream.
    // For instance storing two integers in a binary archive stream might look like: [<value1><value2>]. 
    // The order of storing/reading data matters. Integers are stored/retrieved as in order, first one, followed by the then second.
    // After reading <value1>, hasMoreData() returns true. After reading <value2> too, hasMoreData() returns false.
    // Storing the same two integers in an XML might look like [<name1 value='value1' /><name2 value='value2' />]. 
    // Reading the XML archive could make use of the stored names instead of storing order.
    virtual bool hasMoreData(const std::string &name) = 0;
    
    virtual int32_t readInt(const std::string &name) = 0;
    virtual float readFloat(const std::string &name) = 0;
    virtual bool readBool(const std::string &name) = 0;
    virtual std::string readString(const std::string &name) = 0;
    virtual Serializable* readObject(const std::string &name) = 0;
    virtual boost::posix_time::ptime readTime(const std::string &name) = 0;
    
    // Reads only the class-type information of the encoded object
    virtual std::string readObjectType(const std::string &name) = 0;
};

} // namespace aqlib

#endif // __AQLIB_ARCHIVE_HPP
