#include "Socket.hpp"
#include "../aqlib/Exception.hpp"
#include "../aqlib/Value.hpp"
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sstream>
#include <errno.h>
#include <netdb.h>
#include <string.h>

namespace net
{
    
log4cxx::LoggerPtr Socket::logger(log4cxx::Logger::getLogger("net.Socket"));

Socket::Socket(int handle)
{
    mHandle = handle < 0 ? create() : handle;
    mInstanceName = std::string("Socket.") + aqlib::Value(mHandle);
}

Socket::~Socket()
{
    close();
}

int Socket::getHandle() const 
{ 
    return mHandle; 
}

void Socket::close()
{
    // Check if the handle is valid
    if (mHandle < 0)
    {
        return;
    }
    
    // Close handle
    if (0 != ::close(mHandle))
    {
        std::stringstream msgStrm;
        msgStrm << "close error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }
    
    // Mark the handle as invalid
    mHandle = -1;
}

void Socket::shutdown()
{
    // Check if the handle is valid
    if (mHandle < 0)
    {
        return;
    }
    
    // Close handle
    if (0 != ::shutdown(mHandle, SHUT_RDWR))
    {
        std::stringstream msgStrm;
        msgStrm << "shutdown error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }
    
    // Mark the handle as invalid
    mHandle = -1;
}

int Socket::create()
{
    int handle = ::socket(PF_INET, SOCK_STREAM, 0);
    if (handle < 0)
    {
        std::stringstream msgStrm;
        msgStrm << "socket error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }
    
    return handle;
}

void Socket::connect(const std::string &host, int port)
{
    hostent *server = ::gethostbyname(host.c_str());
    if (NULL == server)
    {
        std::stringstream msgStrm;
        msgStrm << "gethostbyname error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }

    sockaddr_in serv_addr;
    memset(&serv_addr, 0, sizeof(sockaddr_in));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);
    serv_addr.sin_addr.s_addr = ((in_addr *)(server->h_addr))->s_addr;
    if (::connect(mHandle, (sockaddr *) &serv_addr, sizeof(sockaddr_in)) < 0)
    {
        std::stringstream msgStrm;
        msgStrm << "connect error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }
}

int Socket::send(const char *buf, int len)
{
    int count = ::send(mHandle, buf, len, 0);
    if (count < 0)
    {
        std::stringstream msgStrm;
        msgStrm << "send error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }

    return count;
}

int Socket::recv(char *buf, int len)
{
    int count = ::recv(mHandle, buf, len, 0);
    if (count < 0)
    {
        std::stringstream msgStrm;
        msgStrm << "recv error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }

    return count;
}

void Socket::listen(const std::string &host, int port)
{    
    hostent *server = ::gethostbyname(host.c_str());
    if (NULL == server)
    {
        std::stringstream msgStrm;
        msgStrm << "gethostbyname error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }

    // set SO_REUSEADDR on the socket to true
    int optval = 1;
    if (::setsockopt(mHandle, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof optval) < 0)
    {
        std::stringstream msgStrm;
        msgStrm << "setsockopt error: " << errno;
        throw aqlib::Exception(msgStrm.str());        
    }

    sockaddr_in serv_addr;
    memset(&serv_addr, 0, sizeof(sockaddr_in));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);
    serv_addr.sin_addr.s_addr = ((in_addr *)(server->h_addr))->s_addr;

    if (::bind(mHandle, (sockaddr *) &serv_addr, sizeof(sockaddr_in)) < 0)
    {                
        std::stringstream msgStrm;
        msgStrm << "bind error: " << errno;
        throw aqlib::Exception(msgStrm.str());
    }

    if (0 != ::listen(mHandle, 1))
    {
        std::stringstream msgStrm;
        msgStrm << "listen error: " << errno;            
        throw new aqlib::Exception(msgStrm.str());
    }
    
    LOG4CXX_INFO(logger, mInstanceName << " Listening for connections on " << host << " : " << port)
}

boost::shared_ptr<Socket> Socket::accept()
{
    sockaddr_in cln_addr;
    memset(&cln_addr, 0, sizeof(sockaddr_in));
    socklen_t addr_len = sizeof(sockaddr_in);
    int handle = ::accept(mHandle, (sockaddr *) &cln_addr, &addr_len);
    if (handle < 0)
    {
        std::stringstream msgStrm;
        msgStrm << "accept error: " << errno;
        throw aqlib::Exception(msgStrm.str());        
    }

    std::string host = ::inet_ntoa(cln_addr.sin_addr);
    int port = ntohs(cln_addr.sin_port);
    LOG4CXX_INFO(logger, mInstanceName << " Accepted " << handle << " connection from " << host << " : " << port)

    return boost::shared_ptr<Socket>(new Socket(handle));
}

} // namespace net

