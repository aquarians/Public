#include "Module.hpp"
#include "../Application.hpp"
#include "../aqlib/Value.hpp"

namespace simulation
{

log4cxx::LoggerPtr Module::logger(log4cxx::Logger::getLogger("simulation.Module"));
const std::string Module::NAME = "simulation.Module";
    
Module::Module()
{
    mProcessorThread = boost::shared_ptr<ProcessorThread>(new ProcessorThread(this));
    mShutdownRequested = false;
    mSimulationStopRequested = false;
}

Module::ProcessorThread::ProcessorThread(Module *owner):
    Thread("SIM")
{
    mOwner = owner;
}

void Module::ProcessorThread::run()
{
    mOwner->run();
}

const std::string& Module::getModuleName() const
{
    return NAME;
}

void Module::init()
{
    mProcessorThread->start();
}

void Module::cleanup()
{
    requestSimulationStop();
    requestShutdown();
    mProcessorThread->join();
}

void Module::run()
{
    LOG4CXX_INFO(logger, NAME << " : running");
    
    while (!isShutdownRequested())
    {
        try
        {
            process();
        }
        catch (std::exception &ex)
        {
            LOG4CXX_ERROR(logger, NAME << " : " << ex.what());
        }
    }
    
    LOG4CXX_INFO(logger, NAME << " : stopped");
}

void Module::process()
{
    // Wait simulation or stop request
    SimulationRequestPtr request = waitSimulationRequest();
    if (NULL == request.get())
    {
        return;
    }
    
    // Run simulation
    LOG4CXX_INFO(logger, NAME << " started simulation");
    request->simulate(this);
    LOG4CXX_INFO(logger, NAME << " completed simulation");    
    
    // Clear the road for another simulation
    aqlib::Lock lock(mLock);
    mSimulationRequest = SimulationRequestPtr();    
}

bool Module::isShutdownRequested()
{
    aqlib::Lock lock(mLock);
    return mShutdownRequested;
}

void Module::requestShutdown()
{
    LOG4CXX_INFO(logger, NAME << " application shutdown requested");
    aqlib::Lock lock(mLock);
    mShutdownRequested = true;
    mLock.notify();
}

bool Module::isSimulationStopRequested()
{
    aqlib::Lock lock(mLock);
    return mSimulationStopRequested;
}

void Module::requestSimulationStop()
{
    LOG4CXX_INFO(logger, NAME << " simulation stop requested");
    aqlib::Lock lock(mLock);
    mSimulationStopRequested = true;
}

void Module::requestSimulation(const SimulationRequestPtr &request)
{
    aqlib::Lock lock(mLock);
    if (NULL != mSimulationRequest.get())
    {
        LOG4CXX_WARN(logger, NAME << " : cannot request a simulation while another one is running");
        return;
    }
    
    mSimulationRequest = request;
    mLock.notify();
}

SimulationRequestPtr Module::waitSimulationRequest()
{
    aqlib::Lock lock(mLock);
    while ((NULL == mSimulationRequest.get()) && (!mShutdownRequested))
    {
        mLock.wait();
    }
    
    if (mShutdownRequested)
    {
        return SimulationRequestPtr();
    }
    
    mSimulationStopRequested = false;
    return mSimulationRequest;
}

void Module::addSimulationListener(SimulationListener *listener)
{
    aqlib::Lock lock(mLock);
    mSimulationListeners.insert(listener);    
}

void Module::removeSimulationListener(SimulationListener *listener)
{
    aqlib::Lock lock(mLock);
    mSimulationListeners.erase(listener);
}

void Module::notifySimulationProgressUpdate(double percentRemaining, const SimulationResultPtr &result)
{
    LOG4CXX_INFO(logger, NAME << " simulation progress: percentRemaining=" << percentRemaining);
    aqlib::Lock lock(mLock);
    
    for (SimulationListeners::iterator it = mSimulationListeners.begin(); it != mSimulationListeners.end(); ++it)
    {
        SimulationListener *listener = *it;
        try
        {
            listener->progressUpdate(percentRemaining, result);
        }
        catch (std::exception &ex)
        {
            LOG4CXX_ERROR(logger, NAME << " : " << ex.what());
        }
    }
}

} // namespace simulation
