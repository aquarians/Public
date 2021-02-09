#ifndef __AQLIB_BINARY_ARCHIVE_HPP
#define __AQLIB_BINARY_ARCHIVE_HPP

#include "Archive.hpp"
#include <log4cxx/logger.h>

namespace aqlib
{

// Stores data in binary encoding, in a streaming way: order of storage matters.
// For supporting all-ways compatibility, older members should be written and read first.
// Same goes for class hierarchy: base classes should be written and read first.
class BinaryWriteArchive: public WriteArchive
{
    static log4cxx::LoggerPtr logger;
    static const int INITIAL_BUFFER_SIZE;

    char *mData;
    int mLength;
    int mPosition;
    
public:
    BinaryWriteArchive();
    ~BinaryWriteArchive();

    static const int MAXIMUM_ARCHIVE_SIZE;
        
    // Implement the WriteArchive interface
    WriteArchive* createInstance() const;
    std::string getData() const;
    void writeInt(const std::string &name, int32_t value);
    void writeFloat(const std::string &name, float value);
    void writeBool(const std::string &name, bool value);
    void writeString(const std::string &name, const std::string &value);
    void writeObject(const std::string &name, const Serializable *value);
    void writeTime(const std::string &name, const boost::posix_time::ptime &value);

    const char* data() const { return mData; }
    const int size() const { return mPosition; }    

private:
    void growBuffer(const std::string &name);
    void writeChar(const std::string &name, char value);
    static int floatToRawIntBits(float x);
};

// Stores data in binary encoding, in a streaming way: order of storage matters.
// For supporting all-ways compatibility, older members should be written and read first.
// Same goes for class hierarchy: base classes should be written and read first.
class BinaryReadArchive: public ReadArchive
{
    static log4cxx::LoggerPtr logger;
    const ObjectFactory *mFactory;
    
    const char *mData;
    int mLength;
    int mPosition;
    
public:
    BinaryReadArchive(const ObjectFactory *factory, const char *data, int length);
    
    int32_t available() const { return mLength - mPosition; }
    bool hasMoreData(const std::string &name) { return available() > 0; }
    
    // Implement the ReadArchive interface
    ReadArchive* createInstance(const char *data, int length) const;
    int32_t readInt(const std::string &name);
    float readFloat(const std::string &name);
    bool readBool(const std::string &name);
    std::string readString(const std::string &name);
    Serializable* readObject(const std::string &name);
    boost::posix_time::ptime readTime(const std::string &name);
    std::string readObjectType(const std::string &name);
    
    void printCurrentState();

private:
    char readChar(const std::string &name);
    static float intBitsToFloat(int32_t x);
};

} // namespace aqlib

#endif // __AQLIB_BINARY_ARCHIVE_HPP
