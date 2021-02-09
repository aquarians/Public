#ifndef __NET_SIMULATION_RESULT_MESSAGE_HPP
#define __NET_SIMULATION_RESULT_MESSAGE_HPP

#include "../aqlib/Serializable.hpp"
#include "../simulation/SimulationResult.hpp"
#include <log4cxx/logger.h>

namespace net
{
    
class SimulationResultMessage: public aqlib::Serializable
{
    static log4cxx::LoggerPtr logger;
    
    double mPercentRemaining;
    simulation::SimulationResultPtr mResult;
    
public:
    SimulationResultMessage();
    
    void setPercentRemaining(double percentRemaining);
    void setSimulationResult(const simulation::SimulationResultPtr &result);
    
    // Declare the Serializable interface
    AQLIB_DECLARE_SERIAL;
};

} // namespace net

#endif // __NET_SIMULATION_RESULT_MESSAGE_HPP
