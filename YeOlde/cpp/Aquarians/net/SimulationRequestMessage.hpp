#ifndef __NET_SIMULATION_SUBSCRIBE_MESSAGE_HPP
#define __NET_SIMULATION_SUBSCRIBE_MESSAGE_HPP

#include "Message.hpp"
#include "../simulation/SimulationRequest.hpp"

namespace net
{
    
class SimulationRequestMessage: public Message
{
    bool mStopRequested;
    simulation::SimulationRequestPtr mRequest;
    
public:
    SimulationRequestMessage();
    
    // Declare the Serializable interface
    AQLIB_DECLARE_SERIAL;
    
    // Implement the Message interface
    void execute(Connection *connection);
};

} // namespace net

#endif // __NET_SIMULATION_SUBSCRIBE_MESSAGE_HPP
