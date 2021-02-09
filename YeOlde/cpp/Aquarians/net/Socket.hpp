#ifndef __NET_SOCKET_HPP
#define __NET_SOCKET_HPP

#include <string>
#include <log4cxx/logger.h>
#include <boost/shared_ptr.hpp>

namespace net
{

class Socket
{
    static log4cxx::LoggerPtr logger;
    
    int mHandle;
    std::string mInstanceName;

public:
    // Wraps an existing socket handle or creates a new one if -1
    Socket(int handle = -1);    
    ~Socket();
        
    void shutdown();
    
    // Underlier handle access
    int getHandle() const;

    // Connect to given address
    void connect(const std::string &host, int port);

    // Listen on given interface
    void listen(const std::string &host, int port);
    
    // Returns the number of characters sent (can be zero) or throws if an error occurs
    int send(const char *buf, int len);

    // Returns the number of characters received (can be zero) or throws if an error occurs
    int recv(char *buf, int len);

    // Accepts a connection, returns the socket
    boost::shared_ptr<Socket> accept();
    
private:
    // Create a new socket handle
    static int create();
    // Close the socket handle
    void close();
};

typedef boost::shared_ptr<Socket> SocketPtr;

} // namespace net

#endif // __NET_SOCKET_HPP
