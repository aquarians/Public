#ifndef __NET_MESSAGE_HPP
#define __NET_MESSAGE_HPP

#include "../aqlib/Serializable.hpp"

namespace net
{
    
class Connection;

// A message carried over the net
// Aditionally to serializing it's data, it also provides remote procedure call
class Message: public aqlib::Serializable
{
protected:
    Message() {}
public:
    virtual ~Message() {}
    
    // After deserialization, this procedure is called.
    // The connection is where this message came from.
    virtual void execute(Connection *connection) = 0;
};

} // namespace net

#endif // __NET_MESSAGE_HPP
