#ifndef __SIMULATION_SIMULATION_REQUEST_HPP
#define __SIMULATION_SIMULATION_REQUEST_HPP

#include "../aqlib/Serializable.hpp"
#include "../aqlib/Thread.hpp"

namespace simulation
{
    
class Module;

class SimulationRequest: public aqlib::Serializable
{        
protected:
    SimulationRequest() {}
public:
    virtual ~SimulationRequest() {}
    
    // Use Module's notifySimulationProgressUpdate() to dispatch progress and results.
    // Periodically check Module's isSimulationStopRequested() to interrupt a long-running simulation.
    //virtual void simulate(Module *owner) = 0;
    virtual void simulate(Module *owner);
    
    // Declare the Serializable interface
    AQLIB_DECLARE_SERIAL;    
};

typedef boost::shared_ptr<SimulationRequest> SimulationRequestPtr;

} // namespace simulation

#endif // __SIMULATION_SIMULATION_REQUEST_HPP
