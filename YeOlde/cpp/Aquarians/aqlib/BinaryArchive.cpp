#include "BinaryArchive.hpp"
#include "Serializable.hpp"
#include "ObjectFactory.hpp"
#include "Exception.hpp"
#include "StringTokenizer.hpp"
#include <string.h>
#include <sstream>
#include <memory>
#include "../aqlib/Value.hpp"

namespace aqlib
{

///////////////////////// BinaryWriteArchive /////////////////////////
log4cxx::LoggerPtr BinaryWriteArchive::logger(log4cxx::Logger::getLogger("aqlib.BinaryWriteArchive"));
const int BinaryWriteArchive::MAXIMUM_ARCHIVE_SIZE = 1 * 1024 * 1024; // 1 Mb
const int BinaryWriteArchive::INITIAL_BUFFER_SIZE = 1 * 1024; // 1 kB

BinaryWriteArchive::BinaryWriteArchive()
{
    mData = new char[INITIAL_BUFFER_SIZE];
    mLength = INITIAL_BUFFER_SIZE;
    mPosition = 0;
}

BinaryWriteArchive::~BinaryWriteArchive()
{
    delete[] mData;    
}

WriteArchive* BinaryWriteArchive::createInstance() const
{
    return new BinaryWriteArchive();
}

std::string BinaryWriteArchive::getData() const
{
    return std::string(data(), size());
}

void BinaryWriteArchive::writeChar(const std::string &name, char value)
{
    // Allocate space for data
    while (mPosition >= mLength)
    {
        growBuffer(name);
    }
    
    mData[mPosition] = value;
    mPosition++;
}

void BinaryWriteArchive::writeString(const std::string &name, const std::string &value)
{
    LOG4CXX_DEBUG(logger, "writeString enter name=" << name << " size=" << value.size() << " pos=" << mPosition);
    
    // Write size
    writeInt(name, value.size());
    
     // Allocate space for data
    while (mPosition + value.size() >= mLength)
    {
        growBuffer(name);
    }
    
    // Write data and adjust stream position
    memcpy(mData + mPosition, value.data(), value.size());
    mPosition += value.size();
    LOG4CXX_DEBUG(logger, "writeString leave name=" << name << " size=" << value.size() << " pos=" << mPosition);
}

void BinaryWriteArchive::growBuffer(const std::string &name)
{
    // Check if there's any space left
    if (mLength >= MAXIMUM_ARCHIVE_SIZE)
    {
        std::stringstream msgStrm;
        msgStrm << "Writing: " << name << ". Size too big: " << mLength;
        throw Exception(msgStrm.str());
    }
    
    // Compute length of the new buffer: double the current size if possible or use max buffer size
    int length = (mLength < MAXIMUM_ARCHIVE_SIZE / 2) ? mLength * 2 : MAXIMUM_ARCHIVE_SIZE;
    
    // Alloc new data
    char *data = new char[length];
    if (NULL == data)
    {
        std::stringstream msgStrm;
        msgStrm << "Writing: " << name << ". Out of memory"; 
        throw Exception(msgStrm.str());
    }
    
    // Copy existing data
    memcpy(data, mData, mLength);
    
    // Delete old data
    delete[] mData;
    
    // Store new data
    mData = data;
    mLength = length;
}

void BinaryWriteArchive::writeInt(const std::string &name, int32_t value)
{
    LOG4CXX_DEBUG(logger, "writeInt enter name=" << name << " value=" << value << " pos=" << mPosition);
    
    char a = (char)((int)0xff & (value >> 24));
    char b = (char)((int)0xff & (value >> 16));
    char c = (char)((int)0xff & (value >> 8));
    char d = (char)((int)0xff & value);

    writeChar(name, a);
    writeChar(name, b);
    writeChar(name, c);
    writeChar(name, d);
    
    LOG4CXX_DEBUG(logger, "writeInt leave name=" << name << " value=" << value << " pos=" << mPosition);
}

// code taken from http://http.developer.nvidia.com/Cg/floatToRawIntBits.html
int BinaryWriteArchive::floatToRawIntBits(float  x)
{  
    union 
    {
        float f;  // assuming 32-bit IEEE 754 single-precision
        int32_t i; // assuming 32-bit 2's complement int
    } u;

    u.f = x;
    return u.i;
}

void BinaryWriteArchive::writeFloat(const std::string &name, float value)
{
    LOG4CXX_DEBUG(logger, "writeFloat enter name=" << name << " value=" << value << " pos=" << mPosition);
    
    writeInt(name, floatToRawIntBits(value));
    
    LOG4CXX_DEBUG(logger, "writeFloat leave name=" << name << " value=" << value << " pos=" << mPosition);
}

void BinaryWriteArchive::writeObject(const std::string &name, const Serializable *value)
{
    LOG4CXX_DEBUG(logger, "writeObject enter name=" << name << " pos=" << mPosition);
    
    // Store the object in a brand new archive
    BinaryWriteArchive archive;
    
    if (NULL != value) 
    {
        // Write type hierarchy
        std::string type = value->getClassHierarchy();
        LOG4CXX_DEBUG(logger, "writeObject writing type=" << type);
        archive.writeString(name, type);

        // Write value
        LOG4CXX_DEBUG(logger, "writeObject writing data");
        value->writeTo(archive);
    }

    // Store the archive
    std::string data(archive.data(), archive.size());
    writeString(name, data);
    
    LOG4CXX_DEBUG(logger, "writeObject leave name=" << name << " pos=" << mPosition);
}

void BinaryWriteArchive::writeBool(const std::string &name, bool value)
{
    writeInt(name, value ? 1 : 0);
}

void BinaryWriteArchive::writeTime(const std::string &name, const boost::posix_time::ptime &value)
{
    writeInt(name, value.date().year());
    writeInt(name, value.date().month());
    writeInt(name, value.date().day());
    writeInt(name, value.time_of_day().hours());
    writeInt(name, value.time_of_day().minutes());
    writeInt(name, value.time_of_day().seconds());
    writeInt(name, value.time_of_day().fractional_seconds());
}

///////////////////////// BinaryReadArchive /////////////////////////
log4cxx::LoggerPtr BinaryReadArchive::logger(log4cxx::Logger::getLogger("aqlib.BinaryReadArchive"));
BinaryReadArchive::BinaryReadArchive(const ObjectFactory *factory, const char *data, int length)
{
    if (length < 0)
    {
        std::stringstream msgStrm;
        msgStrm << "Invalid length: " << length;
        throw Exception(msgStrm.str());
    }
    
    mFactory = factory;
    mData = data;
    mLength = length;
    mPosition = 0;
}

ReadArchive* BinaryReadArchive::createInstance(const char *data, int length) const
{
    return new BinaryReadArchive(mFactory, data, length);
}

char BinaryReadArchive::readChar(const std::string &name)
{
    if (mPosition >= mLength)
    {
        std::stringstream msgStrm;
        msgStrm << "Reading: " << name << ". End of stream: pos=" << mPosition << " len=" << mLength;
        throw Exception(msgStrm.str());
    }
    
    char value = mData[mPosition];
    mPosition++;
    return value;
}

std::string BinaryReadArchive::readString(const std::string &name)
{    
    // Read size
    int length = readInt(name);
    
    // Read data
    if (mPosition + length > mLength)
    {
        std::stringstream msgStrm;
        msgStrm << "Reading: " << name << ". End of stream: pos=" << mPosition << " len=" << mLength << " bytes=" << length;
        throw Exception(msgStrm.str());        
    }
    
    std::string data;
    data.resize(length);
    memcpy((char *)data.data(), mData + mPosition, length);
    mPosition += length;
    
    return data;
}

void BinaryReadArchive::printCurrentState()
{
    LOG4CXX_DEBUG(logger, "CurState=" << aqlib::Value::toHexString(std::string(mData + mPosition, mLength - mPosition)));
}

Serializable* BinaryReadArchive::readObject(const std::string &name)
{    
    // Read object's data
    std::string data = readString(name);
    if (0 == data.size()) 
    {
        // Null object
        return NULL;
    }
    
    // Wrap data in an archive
    BinaryReadArchive archive(mFactory, data.data(), data.size());
    
    // Read type hierarchy
    std::string type = archive.readString(name);
    LOG4CXX_DEBUG(logger, "readObject name=" << name << " type=" << type);

    // Create object instance
    Serializable *instance = mFactory->createObject(type);
    if (NULL == instance) 
    {
        std::stringstream msgStrm;
        msgStrm << "Reading: " << name << ". Unknown type: " << type;
        throw Exception(msgStrm.str());
    }
    

    // Ask object to read it's data
    instance->readFrom(archive);
    return instance;
}

std::string BinaryReadArchive::readObjectType(const std::string &name)
{
    // Object serialization format: [DATA]
    // [DATA]: [<SIZE><OBJECT>]
    // [OBJECT]: [<TYPE><CONTENTS>]
    
    // Read DATA.SIZE (integer type)
    readInt(name);
    
    // Read DATA.OBJECT.TYPE (string type)
    std::string type = readString(name);
    return type;
}

bool BinaryReadArchive::readBool(const std::string &name)
{
    int value = readInt(name);
    return value ? true : false;
}

boost::posix_time::ptime BinaryReadArchive::readTime(const std::string &name)
{
    int year = readInt(name);
    int month = readInt(name);
    int day = readInt(name);
    int hours = readInt(name);
    int minutes = readInt(name);
    int seconds = readInt(name);
    int fractional_seconds = readInt(name);
 
    boost::gregorian::date bday(year, month, day);
    boost::posix_time::time_duration clock(hours, minutes, seconds, fractional_seconds);
    boost::posix_time::ptime timestamp(bday, clock);

    return timestamp;
}

// code taken from http://http.developer.nvidia.com/Cg/intBitsToFloat.html
float BinaryReadArchive::intBitsToFloat(int32_t x)
{
    union 
    {
        float f; // assuming 32-bit IEEE 754 single-precision
        int i; // assuming 32-bit 2's complement int
    } u;

    u.i = x;
    return u.f;
}

float BinaryReadArchive::readFloat(const std::string &name)
{
    return intBitsToFloat(readInt(name));
}

int32_t BinaryReadArchive::readInt(const std::string &name)
{
    char a = readChar(name);
    char b = readChar(name);
    char c = readChar(name);
    char d = readChar(name);
    return 
    (
        (((int32_t)a & (int32_t)0xff) << 24) | 
        (((int32_t)b & (int32_t)0xff) << 16) | 
        (((int32_t)c & (int32_t)0xff) << 8) | 
        ((int32_t)d & (int32_t)0xff)
    );
}

} // namespace aqlib
