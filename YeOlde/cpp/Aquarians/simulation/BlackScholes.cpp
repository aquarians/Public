#include "BlackScholes.hpp"
#include "Module.hpp"
#include <math.h>
#include "../aqlib/Exception.hpp"
#include "../math/LogNormalDistribution.hpp"
#include "../math/NormalDistribution.hpp"
#include "../math/DistributionHistogram.hpp"
#include "../aqlib/TextFile.hpp"
#include "../aqlib/Value.hpp"
#include "../aqlib/BinaryArchive.hpp"

namespace simulation
{

// Implement the Serializable interface
//AQLIB_IMPLEMENT_SERIAL(simulation::BlackScholes, simulation::SimulationRequest);
AQLIB_IMPLEMENT_SERIAL(simulation::BlackScholes, aqlib::Serializable);
log4cxx::LoggerPtr BlackScholes::logger(log4cxx::Logger::getLogger("simulation.BlackScholes"));

// About 1 minute
const double BlackScholes::MIN_TIME_TO_MATURITY = 1.0 / (365.0 * 24.0 * 60.0);
const int BlackScholes::INTEGRAL_STEPS = 1000;

const int BlackScholes::MODEL_BLACK_SCHOLES_SPOT = 1;
const int BlackScholes::MODEL_BLACK_SCHOLES_FUTURE_TRUNCATION = 2;
const int BlackScholes::MODEL_BLACK_SCHOLES_FUTURE_APPROXIMATION = 3;
const int BlackScholes::MODEL_BLACK = 4;
const int BlackScholes::MODEL_BUNEA = 5;

BlackScholes::BlackScholes()
{
    mIsCall = false;
    mStrikePrice = 0.0;
    mTimeToOptionExpiration = 0.0;
    mTimeToFutureExpiration = 0.0;
    mGrowthRate = 0.0;
    mInterestRate = 0.0;
    mDividendYield = 0.0;
    mVolatility = 0.0;
    mReplicationSteps = 0;
    mSimulationsCount = 0;
    mPricingModel = 0;
}

void BlackScholes::classReadFrom(aqlib::ReadArchive &archive)
{
    LOG4CXX_DEBUG(logger, "classReadFrom enter");
    (dynamic_cast<aqlib::BinaryReadArchive *>(&archive))->printCurrentState();
    
    mIsCall = archive.readBool("IsCall");
    LOG4CXX_DEBUG(logger, "Read mIsCall=" << mIsCall);
    (dynamic_cast<aqlib::BinaryReadArchive *>(&archive))->printCurrentState();
    
    mSpotPrice = archive.readFloat("SpotPrice");
    mStrikePrice = archive.readFloat("StrikePrice");
    mTimeToOptionExpiration = archive.readFloat("TimeToOptionExpiration ");
    mTimeToFutureExpiration = archive.readFloat("TimeToFutureExpiration ");
    mGrowthRate = archive.readFloat("GrowthRate");
    mInterestRate = archive.readFloat("InterestRate");
    mDividendYield = archive.readFloat("DividendYield");
    mVolatility = archive.readFloat("Volatility");
    mReplicationSteps = archive.readInt("ReplicationSteps");
    mSimulationsCount = archive.readInt("SimulationsCount");
    mPricingModel = archive.readInt("mPricingModel");
    
    
    LOG4CXX_DEBUG(logger, "Read mSpotPrice=" << mSpotPrice);
    LOG4CXX_DEBUG(logger, "Read mStrikePrice=" << mStrikePrice);
    LOG4CXX_DEBUG(logger, "Read mTimeToOptionExpiration=" << mTimeToOptionExpiration);
    LOG4CXX_DEBUG(logger, "Read mTimeToFutureExpiration=" << mTimeToFutureExpiration);
    LOG4CXX_DEBUG(logger, "Read mGrowthRate=" << mGrowthRate);
    LOG4CXX_DEBUG(logger, "Read mInterestRate=" << mInterestRate);
    LOG4CXX_DEBUG(logger, "Read mDividendYield=" << mDividendYield);
    LOG4CXX_DEBUG(logger, "Read mVolatility=" << mVolatility);
    LOG4CXX_DEBUG(logger, "Read mReplicationSteps=" << mReplicationSteps);
    LOG4CXX_DEBUG(logger, "Read mSimulationsCount=" << mSimulationsCount);
    LOG4CXX_DEBUG(logger, "Read mPricingModel=" << mPricingModel);
}

void BlackScholes::classWriteTo(aqlib::WriteArchive &archive) const
{
    throw aqlib::Exception("Not iplemented!");
}

/*
void BlackScholes::simulate(Module *owner)
{
    LOG4CXX_INFO(logger, "simulation started");
    
    int steps = 10;
    while (steps--)
    {
        double percentRemaining = (double)steps / 10.0;
        owner->notifySimulationProgressUpdate(percentRemaining, SimulationResultPtr());
        LOG4CXX_INFO(logger, "remaining simulation steps: " << steps);
        ::sleep(1);
        if (owner->isSimulationStopRequested())
        {
            LOG4CXX_INFO(logger, "simulation interrupted");
            owner->notifySimulationProgressUpdate(0.0, SimulationResultPtr());
            break;
        }
    }
    
    SimulationResultPtr result(new SimulationResult());
    std::map<double, double> graph;
    for (int i = 0; i < 100; ++i)
    {
        double x = i;
        double y = i < 50 ? i : 100 - i;
        graph[x] = y;
    }
    result->add(graph);
    owner->notifySimulationProgressUpdate(0.0, result);
    
    LOG4CXX_INFO(logger, "simulation ended");
}
*/

void BlackScholes::simulate(Module *owner)
{
    LOG4CXX_INFO(logger, "simulation started");
    
    math::DistributionHistogram dhist;
    for (int i = mSimulationsCount; i > 0; i--)
    {
        double percentRemaining = (double)i / mSimulationsCount;
        if (0 == (i % 100))
        {        
            owner->notifySimulationProgressUpdate(percentRemaining, SimulationResultPtr());
        }        
        
        double pnl = simulateReplication();        
        dhist.add(pnl);
        
        LOG4CXX_INFO(logger, "remaining simulation steps: " << i << " pnl=" << pnl);

        if (owner->isSimulationStopRequested())
        {
            LOG4CXX_INFO(logger, "simulation interrupted");
            owner->notifySimulationProgressUpdate(0.0, SimulationResultPtr());
            break;
        }
    }

    dhist.compute();
    LOG4CXX_DEBUG(logger, "HEDGE price=" << priceBlackScholes() << " mean=" << dhist.getMean() << " dev=" << dhist.getDeviation());
    dhist.print();

    SimulationResultPtr result(new SimulationResult());
    std::map<double, double> graph;
    dhist.computeHistogram(graph);
    result->add(graph);
    owner->notifySimulationProgressUpdate(0.0, result);
    
    LOG4CXX_INFO(logger, "simulation ended");
}

struct MyExpirationValue: public math::SISORealFunction
{
    bool mIsCall;
    double mStrikePrice;
    
    double getSISORealValue(double spotPrice)
    {
        double side = mIsCall ? 1.0 : -1.0;
        return std::max((spotPrice - mStrikePrice) * side, 0.0);
    }
};

class BlackScholes::ExpirationValue: public math::SISORealFunction
{
    bool mIsCall;
    double mStrikePrice;
    
public:
    ExpirationValue(bool isCall, double strikePrice)
    {
        mIsCall = isCall;
        mStrikePrice = strikePrice;
    }
    
    double getSISORealValue(double spotPrice)
    {
        double side = mIsCall ? 1.0 : -1.0;
        return std::max((spotPrice - mStrikePrice) * side, 0.0);
    }
};

double BlackScholes::price() const
{
    switch (mPricingModel)
    {
        case MODEL_BLACK_SCHOLES_SPOT:
        case MODEL_BLACK_SCHOLES_FUTURE_TRUNCATION:
        case MODEL_BLACK_SCHOLES_FUTURE_APPROXIMATION:
            return priceBlackScholes();
        case MODEL_BLACK:
            return priceBlack();
        case MODEL_BUNEA:
            return priceBunea();
    }
    
    throw aqlib::Exception(std::string("Unknown PricingModel: ") + aqlib::Value(mPricingModel));
}

double BlackScholes::priceBlackScholes() const
{
    math::NormalDistribution ndist;
    
    double s = mSpotPrice;
    double x = mStrikePrice;
    double t = mTimeToOptionExpiration;
    double r = mInterestRate;
    double q = mDividendYield;
    double v = mVolatility;
 
    if (t < MIN_TIME_TO_MATURITY)
    {
        return std::max(0.0, (s - x) * (mIsCall ? 1.0 : -1.0));
    }
    
    double vsqrt = v * sqrt(t);
    double d1 = (log(s / x) + (r - q + (double)0.5 * v * v) * t) / vsqrt;
    double d2 = d1 - vsqrt;
    
    // Check if call option
    if (mIsCall)
    {
        return (s * exp(-q * t) * ndist.cdf(d1) - x * exp(-r * t) * ndist.cdf(d2));
    }
    
    // Put option
    return (-s * exp(-q * t) * ndist.cdf(-d1) + x * exp(-r * t) * ndist.cdf(-d2));
}

double BlackScholes::priceBlack() const
{
    math::NormalDistribution ndist;
    
    double s = mSpotPrice;
    double x = mStrikePrice;
    double t = mTimeToOptionExpiration;
    double r = mInterestRate;
    double q = mInterestRate;
    double v = mVolatility;
 
    if (t < MIN_TIME_TO_MATURITY)
    {
        return std::max(0.0, (s - x) * (mIsCall ? 1.0 : -1.0));
    }
    
    double vsqrt = v * sqrt(t);
    double d1 = (log(s / x) + (r - q + (double)0.5 * v * v) * t) / vsqrt;
    double d2 = d1 - vsqrt;
    
    // Check if call option
    if (mIsCall)
    {
        return (s * exp(-q * t) * ndist.cdf(d1) - x * exp(-r * t) * ndist.cdf(d2));
    }
    
    // Put option
    return (-s * exp(-q * t) * ndist.cdf(-d1) + x * exp(-r * t) * ndist.cdf(-d2));
}

double BlackScholes::priceBunea() const
{
    math::NormalDistribution ndist;
    
    double f = mSpotPrice * exp((mInterestRate - mDividendYield) * mTimeToOptionExpiration);
    double k = mStrikePrice;
    double t = mTimeToOptionExpiration;
    double r = mInterestRate;
    double q = mDividendYield;
    double v = mVolatility;    
    double discount = exp(-(r - q) * (mTimeToFutureExpiration - t));
    double fd = f * discount;
    //LOG4CXX_INFO(logger, "priceBunea f=" << f << " fd=" << fd);
    
    double vsqrt = v * sqrt(t);
    double d1 = (log(fd / k) + (r + 0.5 * v * v) * t) / vsqrt;
    double d2 = d1 - vsqrt;

    // Check if call option
    if (mIsCall > 0)
    {
        return (fd * ndist.cdf(d1) - k * exp(-r * t) * ndist.cdf(d2));
    }
    
    // Put option
    return (-fd * ndist.cdf(-d1) + k * exp(-r * t) * ndist.cdf(-d2));
}
// Computed numerically by a central difference formula, see http://en.wikipedia.org/wiki/Finite_difference
double BlackScholes::delta() const
{
    BlackScholes higher = *this;
    BlackScholes lower = *this;    
    double h = mSpotPrice * 0.01;
    higher.mSpotPrice += h;
    lower.mSpotPrice -= h;    
    double priceHigh = higher.price();
    double priceLow = lower.price();
    double derivative = (priceHigh - priceLow) / (h * 2.0);
    return derivative;
}

void BlackScholes::simulatePath(std::vector<double> &spot, std::vector<double> &time) const
{
    spot.resize(mReplicationSteps + 1);
    time.resize(mReplicationSteps + 1);
    
    double s = mSpotPrice;
    double dt = mTimeToOptionExpiration / mReplicationSteps;
    double sqdt = sqrt(dt);
    double v = mVolatility;
    math::NormalDistribution ndist;
    
    for (int i = 0; i <= mReplicationSteps; ++i)
    {
        double t = std::max(mTimeToOptionExpiration - dt * i, 0.0);
        spot[i] = s;
        time[i] = t;                
        
        // See http://en.wikipedia.org/wiki/Geometric_Brownian_motion
        double eps = ndist.rnd();
        s += s * (mGrowthRate * dt + v * eps * sqdt);
    }
}

double BlackScholes::simulateReplication() const
{
    double qs = 0.0; // quantity in the asset
    double qb = 0.0; // quantity in the bank
    
    std::vector<double> spot;
    std::vector<double> time;
    spot.resize(mReplicationSteps + 1);
    time.resize(mReplicationSteps + 1);
    
    BlackScholes model = *this;
    simulatePath(spot, time);
    
    /*
    spot.clear();
    time.clear();
    aqlib::CsvFileReader reader("/home/administrator/Projects/cpp/Heston/path.csv");
    aqlib::CsvFileWriter::Record record;
    while (reader.readRecord(record))
    {
        double t = aqlib::Value(record[0]).toDouble();
        double s = aqlib::Value(record[1]).toDouble();
        //LOG4CXX_INFO(logger, "LOAD t=" << t << " s=" << s);
        time.push_back(t);
        spot.push_back(s);        
    }
    */
    
    double dtFutOpt = mTimeToFutureExpiration - mTimeToOptionExpiration;
    for (int i = 0; i < spot.size(); ++i)
    {
        model.mSpotPrice = spot[i];
        model.mTimeToOptionExpiration = time[i];
        model.mTimeToFutureExpiration = time[i] + dtFutOpt;
        //LOG4CXX_INFO(logger, "SET i=" << i << " t=" << model.mTimeToOptionExpiration << " s=" << model.mSpotPrice);
        
        double p = model.price();
        double d = model.delta();        
        double dt = (i > 0) ? time[i - 1] - time[i] : 0.0;
        double f = model.mSpotPrice * exp((model.mInterestRate - model.mDividendYield) * model.mTimeToOptionExpiration);

         // Add interest
        qb *= exp(mInterestRate * dt);
        
        double underlierPrice = 0.0;
        double underlierQuantity = 0.0;
        double dividendYield = 0.0;
        
        if (MODEL_BLACK_SCHOLES_SPOT == mPricingModel)
        {
            // Underlier is the spot
            underlierPrice = model.mSpotPrice;

            // Has dividends
            dividendYield = mDividendYield;
            
            // No delta adjustment
            underlierQuantity = d;
        }
        else if (MODEL_BLACK_SCHOLES_FUTURE_TRUNCATION == mPricingModel)
        {
            // Underlier is the future
            underlierPrice = f;
            
            // No dividends
            dividendYield = 0.0;
            
            // No delta adjustment
            underlierQuantity = d;    
        }
        else if (MODEL_BLACK_SCHOLES_FUTURE_APPROXIMATION == mPricingModel)
        {
            // Underlier is the future
            underlierPrice = f;
            
            // No dividends
            dividendYield = 0.0;
            
            // Delta adjustment
            underlierQuantity = d * exp(-(model.mInterestRate - model.mDividendYield) * model.mTimeToOptionExpiration);
        }
        else if (MODEL_BLACK == mPricingModel)
        {
            // Underlier is the future
            underlierPrice = f;
            
            // No dividends
            dividendYield = 0.0;
            
            // No delta adjustment
            underlierQuantity = d;
        }
        else if (MODEL_BUNEA == mPricingModel)
        {
            // Underlier is the future
            underlierPrice = f;
            
            // No dividends
            dividendYield = 0.0;
            
            // No delta adjustment
            underlierQuantity = d;
        }
        
        // Add dividends
        qs *= exp(dividendYield * dt);
        
        // Compute replicated price
        double hedge = qb + qs * underlierPrice;
        
        if (0 == i) // Enter position
        {
            qs = underlierQuantity; // If selling a call option, buy stock
            qb = p - qs * underlierPrice; // Borrow missing money
        } 
        else if (i < (spot.size() - 1)) // Balance position
        {            
            double dqs = underlierQuantity - qs; // Amount we need to further buy stock
            qs = underlierQuantity;
            double cost = dqs * underlierPrice;
            qb -= cost; // Bank account is a debt so substract the asset plus
        } 
        else // Close position
        {
            double amount = qs * underlierPrice; // Sell the stock we have and cash the resulting amount (or expense if qs < 0)
            amount -= p; // Pay off the option
            qb += amount; // Return the loan: ideally we should get qb = 0 at this point
        }
        
        /*
        LOG4CXX_INFO(logger, "REPL"
            << " i=" << i 
            << " t=" << model.mTimeToOptionExpiration
            << " s=" << model.mSpotPrice
            << " f=" << f
            << " up=" << underlierPrice
            << " uq=" << underlierQuantity
            << " ud=" << dividendYield
            << " p=" << p
            << " h=" << hedge
            << " d=" << d
            << " qs=" << qs
            << " qb=" << qb);
            */
    }
    
    return qb;
}

} // namespace simulation
