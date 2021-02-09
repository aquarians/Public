#include "Exception.hpp"
#include <execinfo.h>
#include <unistd.h>
#include <bfd.h>
#include <vector>
#include <stdexcept>
#include <sstream>
#include <stdlib.h>
#include <stdio.h>
#include <cxxabi.h>

namespace aqlib
{

log4cxx::LoggerPtr Exception::logger(log4cxx::Logger::getLogger("aqlib.Exception"));
const std::string Exception::CLASS_NAME("Exception");
const int Exception::MAX_STACKTRACE_DEPTH = 100;
Exception::BfdData* Exception::gBfdData = NULL;

struct Exception::BfdData
{
    bfd *abfd;
    std::vector<char> syms;
    asection *text;
    
    BfdData()
    {
        char ename[1024];
        int l = readlink("/proc/self/exe", ename, sizeof(ename));
        if (l == -1) 
        {
            throw std::runtime_error("readlink('/proc/self/exe') failed");
        }
        ename[l] = 0;

        bfd_init();

        abfd = bfd_openr(ename, NULL);
        if (NULL == abfd) 
        {
            throw std::runtime_error("bfd_openr failed");
        }

        // Oddly, this is required for it to work...
        bfd_check_format(abfd, bfd_object);

        int storage_needed = bfd_get_symtab_upper_bound(abfd);
        if (storage_needed < 0)
        {
            throw std::runtime_error("bfd_get_symtab_upper_bound failed");
        }
        
        syms.resize(storage_needed);
        int scount = bfd_canonicalize_symtab(abfd, (asymbol **)(&syms[0]));
        if (scount < 0)
        {
            throw std::runtime_error("bfd_canonicalize_symtab failed");
        }

        text = bfd_get_section_by_name(abfd, ".text");
    }

    bool resolve(Exception::Frame &frame)
    {
        char *frmAdr = (char *) frame.addr;
        char *txtAdr = (char *) text->vma;
        if (frmAdr < txtAdr)
        {
            return false;
        }

        // http://sourceware.org/ml/binutils/2005-07/msg00285.html
        // A bfd_vma is defined as whatever C type can hold an unsigned 64-bit value.
        // Thus on a 32-bit host it will be a "unsigned long long" and on a 64-bit host it will be an "unsigned long".
        // Either way a bfd_vma is a 64-bit unsigned value.
        bfd_vma offset = frmAdr - txtAdr;

        const char *file = NULL;
        const char *func = NULL;
        unsigned line;
        if (!bfd_find_nearest_line(abfd, text, (asymbol **)(&syms[0]), offset, &file, &func, &line))
        {
            return false;
        }
        
        if ((NULL == file) || (NULL == func))
        {
            return false;
        }

        frame.file = file;
        if (frame.func.size() == 0) frame.func = func;
        frame.line = line;
        return true;
    }
    
};

void Exception::staticInit()
{
    gBfdData = new BfdData();
}

Exception::Frame::Frame()
{
    addr = NULL;
    line = -1;
}

Exception::Exception(const std::string &message)
{
    // Get the backtrace
    std::vector<void *> addresses(MAX_STACKTRACE_DEPTH);
    int depth = backtrace(&addresses[0], addresses.size());
    
    // Get the frames
    mFrames.resize(depth);
    char **syms = backtrace_symbols(&addresses[0], depth);
    for (int i = 0; i < depth; ++i)
    {
        Frame &frame = mFrames[i];
        
        // Raw data
        frame.addr = addresses[i];
        frame.sym = syms[i];
        
        // Module and function
        parseBacktraceSymbol(frame);
        
        // File and line
        if (NULL != gBfdData)
        {
            gBfdData->resolve(frame);
        }
        
        // Demangle function name
        int demangleStatus = 0;
        char *demangledFunc = abi::__cxa_demangle(frame.func.c_str(), NULL, NULL, &demangleStatus);
        if (NULL != demangledFunc)
        {
            frame.func = demangledFunc;
            free(demangledFunc);
        }
    }
    free(syms);
    
    mMessage = message;
    mWhat = Exception::toString();
}

// Expect the form: "module(function+0xOFFSET) [0xADDR]" or "module() [0xADDR]"
void Exception::parseBacktraceSymbol(Frame &frame)
{
    std::size_t startPos = frame.sym.find("(");
    if (std::string::npos == startPos)
    {
        return;
    }
    
    frame.module = frame.sym.substr(0, startPos);
    
    startPos++;
    std::size_t endPos = frame.sym.find("+0x", startPos);
    if (std::string::npos == endPos)
    {
        return;
    }
            
    frame.func = frame.sym.substr(startPos, endPos - startPos);
}

const std::string& Exception::getClassName() const
{
    return CLASS_NAME;
}

const std::string& Exception::getMessage() const
{
    return mMessage;
}

int Exception::getFrameCount() const
{
    return mFrames.size();
}

const Exception::Frame& Exception::getFrame(int index) const
{
    return mFrames[index];
}

std::string Exception::toString() const
{
    std::stringstream strm;
    strm << getClassName() << ": " << mMessage;
    
    std::string module;
    for (int i = 0; i < getFrameCount(); ++i)
    {
        const Frame &frame = getFrame(i);
        
        // Ignore first frame as it's the Exception() constructor, where backtrace() is called
        if (0 == i)
        {
            continue;
        }
        
        // Last frame seems to be just the module name
        if ((getFrameCount() - 1 == i) && (0 == frame.func.size()))
        {
            continue;
        }
        
        strm << std::endl;
        
        if (module != frame.module)
        {
            module = frame.module;
            strm << " in " << module << std::endl;
        }
        
        strm << "  at " << frame.func;
        
        if (frame.file.size()) 
        {
            strm << "(" << frame.file;
            if (frame.line) strm << ": " << frame.line;
            strm << ")";
        }
    }

    return strm.str();
}

const char* Exception::what() const throw()
{
    return mWhat.c_str();
}

} // namespace aqlib
