#include "SimulationRequestMessage.hpp"
#include "Session.hpp"
#include "../simulation/Module.hpp"
#include "../simulation/BlackScholes.hpp"
#include "../aqlib/BinaryArchive.hpp"
#include "../Application.hpp"

namespace net
{

// Implement the Serializable interface
AQLIB_IMPLEMENT_SERIAL(net::SimulationRequestMessage, net::Message);

SimulationRequestMessage::SimulationRequestMessage()
{
    mRequest = simulation::SimulationRequestPtr(new simulation::BlackScholes());
}

void SimulationRequestMessage::classReadFrom(aqlib::ReadArchive &archive)
{
    mStopRequested = archive.readBool("StopRequested");
    mRequest = simulation::SimulationRequestPtr(dynamic_cast<simulation::SimulationRequest *>(archive.readObject("Request")));
}

void SimulationRequestMessage::classWriteTo(aqlib::WriteArchive &archive) const
{
    throw aqlib::Exception("Not iplemented!");
    //archive.writeBool("StopRequested", mStopRequested);
    //archive.writeObject("Result", mResult.get());
}

void SimulationRequestMessage::execute(Connection *connection)
{
    Session *session = dynamic_cast<Session *>(connection);
    if (NULL == session)
    {
        return;
    }
    
    simulation::Module *module = session->getSimulationService()->getSimulationModule();
    if (!mStopRequested)
    {
        module->requestSimulation(mRequest);
    }
    else
    {
        module->requestSimulationStop();
    }
}

} // namespace net
