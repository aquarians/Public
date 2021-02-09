#include "SimulationRequest.hpp"
#include "../aqlib/Exception.hpp"

namespace simulation
{

// Implement the Serializable interface except for classReadFrom)() and classWriteTo().
// Notice using the full class name, "aqlib::DemoSerializableWithMacros" instead of just "DemoSerializableWithMacros".
AQLIB_IMPLEMENT_SERIAL(simulation::SimulationRequest, aqlib::Serializable);

void SimulationRequest::classReadFrom(aqlib::ReadArchive &archive) 
{
}

void SimulationRequest::classWriteTo(aqlib::WriteArchive &archive) const
{
}

void SimulationRequest::simulate(Module *owner)
{
    throw aqlib::Exception("Not implemented");
}

} // namespace simulation
