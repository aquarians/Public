#ifndef __SIMULATION_MODULE_HPP
#define __SIMULATION_MODULE_HPP

#include "../aqlib/Module.hpp"
#include "../aqlib/Thread.hpp"
#include <log4cxx/logger.h>
#include <boost/shared_ptr.hpp>
#include "SimulationRequest.hpp"
#include "SimulationListener.hpp"
#include <set>

namespace simulation
{
    
class ConnectionManager;
class ConnectionListener;

class Module: public aqlib::Module
{
    static log4cxx::LoggerPtr logger;    
    
    aqlib::Monitor mLock;
    // Set to true when application is shutting down
    bool mShutdownRequested;
    // Reset to false at the beginning of a simulation,
    // set to true by the client if he wants a long running simulation interrupted.
    // The simulations should poll this flag and stop if it was requested
    bool mSimulationStopRequested;
    
    class ProcessorThread: public aqlib::Thread
    {
        Module *mOwner;
    public:
        ProcessorThread(Module *owner);
        void run();
    };
    boost::shared_ptr<ProcessorThread> mProcessorThread;
    
    // Only one simulation runs at a time
    SimulationRequestPtr mSimulationRequest;
    
    typedef std::set<SimulationListener *> SimulationListeners;
    SimulationListeners mSimulationListeners;
    
public:
    static const std::string NAME;
    Module();
    
    // Implement the Module interface
    const std::string& getModuleName() const;
    void init();
    void cleanup();
    
    void run();
    void process();
    
    bool isShutdownRequested();
    void requestShutdown();

    bool isSimulationStopRequested();
    void requestSimulationStop();
    
    // Runs this simulation
    void requestSimulation(const SimulationRequestPtr &request);    
    
    void addSimulationListener(SimulationListener *listener);
    void removeSimulationListener(SimulationListener *listener);

    void notifySimulationProgressUpdate(double percentRemaining, const SimulationResultPtr &result);
    
private:
    SimulationRequestPtr waitSimulationRequest();    
};

} // namespace simulation

#endif // __SIMULATION_MODULE_HPP
