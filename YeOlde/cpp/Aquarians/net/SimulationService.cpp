#include "SimulationService.hpp"
#include "../simulation/Module.hpp"
#include "../Application.hpp"
#include "../aqlib/Exception.hpp"
#include "SimulationResultMessage.hpp"
#include "Session.hpp"

namespace net
{
    
SimulationService::SimulationService(Session *owner)
{
    mOwner = owner;
    mSimulationModule = dynamic_cast<simulation::Module *>(Application::getInstance().getModule(simulation::Module::NAME));
    if (NULL == mSimulationModule)
    {
        throw aqlib::Exception("Simulation module not available");
    }    
}

simulation::Module* SimulationService::getSimulationModule()
{
    return mSimulationModule;
}

void SimulationService::init()
{
    mSimulationModule->addSimulationListener(this);    
}

void SimulationService::cleanup()
{
    mSimulationModule->removeSimulationListener(this);
}

void SimulationService::progressUpdate(double percentRemaining, const simulation::SimulationResultPtr &result)
{
    SimulationResultMessage message;
    message.setPercentRemaining(percentRemaining);
    message.setSimulationResult(result);
    mOwner->sendMessage(&message);
}

} // namespace net
