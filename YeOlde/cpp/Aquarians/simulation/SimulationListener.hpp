#ifndef __SIMULATION_SIMULATION_LISTENER_HPP
#define __SIMULATION_SIMULATION_LISTENER_HPP

#include "SimulationResult.hpp"

namespace simulation
{
    
class SimulationListener
{
protected:
    SimulationListener() {}
public:
    virtual ~SimulationListener() {}
    
    // During a simulation run, signals how much of it is completed trough percentRemaining
    // and at the end  (percentRemaining == 0) sends the result, which otherwise might be null.
    virtual void progressUpdate(double percentRemaining, const SimulationResultPtr &result) = 0;
};

} // namespace simulation

#endif // __SIMULATION_SIMULATION_LISTENER_HPP
