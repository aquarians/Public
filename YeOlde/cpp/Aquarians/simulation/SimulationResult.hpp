#ifndef __SIMULATION_SIMULATION_RESULT_HPP
#define __SIMULATION_SIMULATION_RESULT_HPP

#include "../aqlib/Serializable.hpp"

namespace simulation
{
    
class SimulationResult: public aqlib::Serializable
{
public:    
    typedef std::map<double, double> Graph;
    typedef std::vector<Graph> Graphs;
    
    SimulationResult() {}
    virtual ~SimulationResult() {}
    
    // Declare the Serializable interface
    AQLIB_DECLARE_SERIAL;
    
    void add(const Graph &graph);
    
private:
    Graphs mGraphs;    
};

typedef boost::shared_ptr<SimulationResult> SimulationResultPtr;

} // namespace simulation

#endif // __SIMULATION_SIMULATION_RESULT_HPP
