#ifndef __AQLIB_TEXT_FILE_HPP
#define __AQLIB_TEXT_FILE_HPP

#include <string>
#include <vector>

namespace aqlib
{

// Wrapper for reading a text file
class TextFile
{
    static const int BUFFER_SIZE;
    
    const std::string mInstanceName;
    void *mHandle;
    std::string mBuffer;
    std::string mLine;
    int mCount; // Count of data in the buffer
    int mPos; // Position in the buffer
    
public:
    TextFile(const std::string &filename);
    virtual ~TextFile();
    
    static bool exists(const std::string &filename);
    
    /**
     * Reads next line of text. Returns true on succes, false on end of file.
     */
    bool readLine(std::string &line);
    
private:
    void readFile();
};

// Wrapper for writing a text file
class TextFileWriter
{
    const std::string mInstanceName;
    void *mHandle;
    
    static const std::string NEWLINE;
    
public:
    TextFileWriter(const std::string &filename);
    virtual ~TextFileWriter();
    
    // Writes a line of text
    bool writeLine(const std::string &line);
    
    // Flushes the file
    virtual void flush();
    
    // Closes the file
    virtual void close();
};

class CsvFileWriter: public TextFileWriter
{
public:
    CsvFileWriter(const std::string &filename);
    ~CsvFileWriter();

    typedef std::vector<std::string> Record;
    void addRecord(const Record &record);

    void flush();

    void writeRecord(const Record &record);

private:
    typedef std::vector<Record> Records;
    Records mRecords;
};

class CsvFileReader
{
    TextFile mTextFile;
    
public:
    CsvFileReader(const std::string &fileName);

    typedef std::vector<std::string> Record;
    bool readRecord(Record &record);
};

} // namespace aqlib

#endif // __AQLIB_TEXT_FILE_HPP
