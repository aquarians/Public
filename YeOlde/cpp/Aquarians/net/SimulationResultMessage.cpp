#include "SimulationResultMessage.hpp"
#include "Session.hpp"
#include "../simulation/Module.hpp"
#include "../simulation/BlackScholes.hpp"
#include "../aqlib/BinaryArchive.hpp"
#include "../Application.hpp"

namespace net
{

log4cxx::LoggerPtr SimulationResultMessage::logger(log4cxx::Logger::getLogger("net.SimulationResultMessage"));
// Implement the Serializable interface
AQLIB_IMPLEMENT_SERIAL(net::SimulationResultMessage, aqlib::Serializable);

SimulationResultMessage::SimulationResultMessage()
{
    mPercentRemaining = 0.0;
}

void SimulationResultMessage::setPercentRemaining(double percentRemaining)
{
    mPercentRemaining = percentRemaining;
}

void SimulationResultMessage::setSimulationResult(const simulation::SimulationResultPtr &result)
{
    mResult = result;
}

void SimulationResultMessage::classReadFrom(aqlib::ReadArchive &archive)
{
    mPercentRemaining = archive.readFloat("PercentRemaining");
    mResult = simulation::SimulationResultPtr(dynamic_cast<simulation::SimulationResult *>(archive.readObject("Result")));
}

void SimulationResultMessage::classWriteTo(aqlib::WriteArchive &archive) const
{
    archive.writeFloat("PercentRemaining", mPercentRemaining);    
    archive.writeObject("Result", mResult.get());
}

} // namespace net
