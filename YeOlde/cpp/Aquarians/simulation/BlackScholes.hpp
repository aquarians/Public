#ifndef __SIMULATION_BLACK_SCHOLES_HPP
#define __SIMULATION_BLACK_SCHOLES_HPP

#include "SimulationRequest.hpp"
#include <log4cxx/logger.h>
#include <vector>

namespace simulation
{
    
class Module;

class BlackScholes: public SimulationRequest
{
    static log4cxx::LoggerPtr logger;
    
    static const double MIN_TIME_TO_MATURITY;
    static const int INTEGRAL_STEPS; // For numerical calculation of option price        
    
public:    
    static const int MODEL_BLACK_SCHOLES_SPOT;
    static const int MODEL_BLACK_SCHOLES_FUTURE_TRUNCATION;
    static const int MODEL_BLACK_SCHOLES_FUTURE_APPROXIMATION;
    static const int MODEL_BLACK;
    static const int MODEL_BUNEA;
    
    // Option pricing params
    bool mIsCall;
    double mSpotPrice;
    double mStrikePrice;
    double mTimeToOptionExpiration;
    double mTimeToFutureExpiration;
    double mGrowthRate; // Growth rate of the spot price in the physical measure
    double mInterestRate;
    double mDividendYield;
    double mVolatility;
        
    // Number of steps to divide the [0, mTimeToExpiration] into
    int mReplicationSteps;
    
    // Number of simulations to perform
    int mSimulationsCount;
    
    // Option pricing model
    int mPricingModel;
    
public:    
    BlackScholes();
    
    // Declare the Serializable interface
    AQLIB_DECLARE_SERIAL;

    // Implement the SimulationRequest interface
    void simulate(Module *owner);
    
    // Compute option price
    double price() const;
    
    double priceBlackScholes() const;
    double priceBlack() const;
    double priceBunea() const;

    // Compute option delta
    double delta() const;
    
    // Simulate replication of the option price on a random path of the underlier
    // Return the difference between replication portfolio and option value at expiration
    double simulateReplication() const;
    
    // Simulate a random walk of the spot price
    void simulatePath(std::vector<double> &spot, std::vector<double> &time) const;
    
private:
    class ExpirationValue;
};

typedef boost::shared_ptr<SimulationRequest> SimulationRequestPtr;

} // namespace simulation

#endif // __SIMULATION_BLACK_SCHOLES_HPP
