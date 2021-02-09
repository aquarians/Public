#ifndef __NET_SIMULATION_SERVICE_HPP
#define __NET_SIMULATION_SERVICE_HPP

#include "../simulation/SimulationListener.hpp"
#include "../simulation/SimulationRequest.hpp"

namespace simulation
{
class Module;
}

namespace net
{
    
class Session;

class SimulationService: public simulation::SimulationListener
{
    Session *mOwner;
    simulation::Module *mSimulationModule;

public:
    SimulationService(Session *owner);
    
    void init();
    void cleanup();
    
    simulation::Module* getSimulationModule();
    
    // Implement the SimulationListener interface
    void progressUpdate(double percentRemaining, const simulation::SimulationResultPtr &result);
};

} // namespace net

#endif // __NET_SIMULATION_SERVICE_HPP
