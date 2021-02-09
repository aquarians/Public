#include "SimulationResult.hpp"
#include "../aqlib/Exception.hpp"
#include "../aqlib/BinaryArchive.hpp"
#include "../Application.hpp"

namespace simulation
{

// Implement the Serializable interface
AQLIB_IMPLEMENT_SERIAL(simulation::SimulationResult, aqlib::Serializable);

void SimulationResult::add(const Graph &graph)
{
    mGraphs.push_back(graph);
}

void SimulationResult::classWriteTo(aqlib::WriteArchive &archive) const
{
    archive.writeInt("", mGraphs.size());

    for (std::size_t i = 0; i < mGraphs.size(); ++i)
    {
        const Graph &graph = mGraphs[i];
        
        archive.writeInt("", graph.size());
        
        for (Graph::const_iterator it = graph.begin(); it != graph.end(); ++it)
        {
            archive.writeFloat("", it->first);
            archive.writeFloat("", it->second);
        }
    }
}

void SimulationResult::classReadFrom(aqlib::ReadArchive &archive)
{
    throw aqlib::Exception("Not implemented!");
}

} // namespace simulation
