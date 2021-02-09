#include "TextFile.hpp"
#include "Exception.hpp"
#include "StringTokenizer.hpp"
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sstream>

namespace aqlib
{
    
const int TextFile::BUFFER_SIZE = 256 * 1024;  // 256 Kb

TextFile::TextFile(const std::string &filename):
    mInstanceName(std::string("File.") + filename)
{
    mHandle = fopen(filename.c_str(), "r");
    if (NULL == mHandle)
    {
        throw Exception(std::string("Failed to open file: ") + filename);
    }
    
    mBuffer.resize(BUFFER_SIZE);
    
    // Read initial data chunk into the buffer
    readFile();
}

TextFile::~TextFile()
{
    fclose((FILE *)mHandle);
}

bool TextFile::exists(const std::string &filename)
{
    struct stat attributes;
    bool success = (0 == stat(filename.c_str(), &attributes));
    return success;
}

bool TextFile::readLine(std::string &line)
{
    if (mCount <= 0)
    {
        return false;
    }
    
    // Read buffer
    while (mPos < mCount)
    {
        char value = mBuffer[mPos];
        mPos++;
        
        // Look for newline character
        if (value != '\n')
        {
            // Add to current line
            mLine += value;
            if (mLine.length() > BUFFER_SIZE)
            {
                // It's weird not to have encountered a line until now
                throw Exception("Buffer overfow!");
            }
            continue;
        }
        
        // Copy current line result and prepare reading the next line
        line = mLine;
        mLine.clear();

        // Remove eventual DOS newline terminal character
        if ((line.size() > 0) && ('\r' == line[line.size() - 1]))
        {
            line = line.substr(0, line.size() - 1);
        }
        
        // Succeeded
        return true;
    }
    
    // Read next data chunk into the buffer
    readFile();
    return readLine(line);
}

void TextFile::readFile()
{
    FILE *file = (FILE *)mHandle;
    void *data = (void *)mBuffer.data();
    mCount = fread(data, 1, BUFFER_SIZE, file);
    mPos = 0;
}

/////////////////////////////////////////////////////
const std::string TextFileWriter::NEWLINE("\n");

TextFileWriter::TextFileWriter(const std::string &filename):
    mInstanceName(std::string("FileWriter.") + filename)
{
    mHandle = fopen(filename.c_str(), "w");
    if (NULL == mHandle)
    {
        throw Exception(std::string("Failed to open file: ") + filename);
    }    
}

TextFileWriter::~TextFileWriter()
{
    flush();
    close();
}

void TextFileWriter::close()
{
    fclose((FILE *)mHandle);
    mHandle = NULL;
}

void TextFileWriter::flush()
{
    fflush((FILE *)mHandle);
}

bool TextFileWriter::writeLine(const std::string &line)
{
    // Write line data
    int count = fwrite(line.data(), 1, line.size(), (FILE *)mHandle);
    if (line.size() != count)
    {
        return false;
    }
    
    // Write newline character
    count = fwrite(NEWLINE.data(), 1, NEWLINE.size(), (FILE *)mHandle);
    if (NEWLINE.size() != count)
    {
        return false;
    }
    
    return true;
}

CsvFileWriter::CsvFileWriter(const std::string &filename):
    TextFileWriter(filename)
{
}

CsvFileWriter::~CsvFileWriter()
{
    flush();
}

void CsvFileWriter::addRecord(const Record &record)
{
    mRecords.push_back(record);
}

void CsvFileWriter::flush()
{
    for (Records::const_iterator itRecord = mRecords.begin(); itRecord != mRecords.end(); ++itRecord)
    {
        const Record &record = *itRecord;
        writeRecord(record);
    }

    mRecords.clear();
}

void CsvFileWriter::writeRecord(const Record &record)
{
    std::stringstream strm;
    for (Record::const_iterator it = record.begin(); it != record.end(); ++it)
    {
        const std::string &item = *it;
        strm << (record.begin() != it ? "," : "") << item;
    }

    writeLine(strm.str());
}

CsvFileReader::CsvFileReader(const std::string &fileName):
    mTextFile(fileName)
{
}

bool CsvFileReader::readRecord(Record &record)
{
    record.clear();
    
    std::string line;
    if (!mTextFile.readLine(line))
    {
        return false;
    }

    StringTokenizer tokenizer(line, ",");
    while (tokenizer.hasMoreTokens())
    {
        std::string token = tokenizer.nextToken();
        record.push_back(token);
    }

    return true;
}

} // namespace aqlib
