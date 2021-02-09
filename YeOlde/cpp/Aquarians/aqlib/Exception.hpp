#ifndef __EXCEPTION_HPP
#define __EXCEPTION_HPP

#include <exception>
#include <string>
#include <vector>
#include <log4cxx/logger.h>

namespace aqlib
{

// Offers the functionality to print the stack trace at runtime.
// As there's no C/C++ standard or at least established library for this, 
// had to implement it from scratch using GCC specific API.
class Exception: public std::exception
{
    static log4cxx::LoggerPtr logger;
    
public:
    Exception(const std::string &message);
    ~Exception() throw() {}
    
    // Exception class name
    virtual const std::string& getClassName() const;
    
    // Error message as provided in the constructor
    const std::string& getMessage() const;

    // Call once, on application startup
    static void staticInit();
    
    // Access to stack trace frames
    struct Frame
    {
        void *addr; // Frame address
        std::string sym; // Raw data as outputted by backtrace_symbols()
        std::string module; // Name of the application or library
        std::string file; // Name of the source file
        std::string func; // Function signature
        int line; // Line in the source file
        Frame();
    };    
    int getFrameCount() const;
    const Frame& getFrame(int index) const;

    // Conversion to string
    virtual std::string toString() const;
    
    // Override the std::exception interface
    virtual const char* what() const throw();
    
private:
    // Parse text outputted by backtrace_symbols()    
    static void parseBacktraceSymbol(Frame &frame);    
    
private:
    static const std::string CLASS_NAME;
    static const int MAX_STACKTRACE_DEPTH;
    
    // Use BFD library for (file, line number) information
    // Adapted from http://en.wikibooks.org/wiki/Linux_Applications_Debugging_Techniques/The_call_stack
    struct BfdData;
    static BfdData *gBfdData;

    typedef std::vector<Frame> Frames;
    Frames mFrames;
    
    std::string mMessage;
    std::string mWhat;
};

} // namespace aqlib

#endif // __EXCEPTION_HPP
