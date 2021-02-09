#ifndef __GBM_FITTER_HPP
#define __GBM_FITTER_HPP

#include "Types.hpp"
#include "MertonModel.hpp"

// Fitter for Geometric Brownian Motion process
class GBMFitter
{
    // Sampling time
    double mTime;
  
    // Samples of the log-process
    dblvect_t mLogSamples;    

    // Results
    double mRealizedMean;
    double mRealizedVariance;
    double mRealizedGrowth;
    double mRealizedVolatility;
    
public:
    GBMFitter(double samplingTime, const dblvect_t &logSamples);

    void compute();

    // Accessors
    double getRealizedMean() const { return mRealizedMean; }
    double getRealizedVariance() const { return mRealizedVariance; }
    double getRealizedGrowth() const { return mRealizedGrowth; }
    double getRealizedVolatility() const { return mRealizedVolatility; }

    // Computes theoretical mean
    static double computeTheoreticalMean(
                const BatesModel &physicalModel,
                double samplingTime);

    // Computes theoretical variance
    static double computeTheoreticalVariance(
                const BatesModel &physicalModel,
                double samplingTime);

    double computeAnnualizedVariance() const;
    
private:
    // Computes samples mean
    void computeRealizedMean();

    // Computes samples variance
    void computeRealizedVariance();

    // Estimates the growth rate
    void computeRealizedGrowth();

    // Estimates the volatility
    void computeRealizedVolatility();
};

class GBMFitter2
{
public:  
    struct Sample
    {
        double time;
        double value;        
        Sample(double time = 0.0, double value = 0.0);
    };
    
    GBMFitter2();
    
    void setOffset(double value) { mOffset = value; }
    void add(const Sample &sample);
    void compute();
    
    double getGrowthRate() const { return mGrowthRate; }
    double getVolatility() const { return mVolatility; }
    
private:
  // Compute initial estimate for growth and vol
  void computeInitialEstimate();

private:
    int mOffset;
    typedef std::vector<Sample> Samples;
    Samples mSamples;
    
    double mGrowthRate;
    double mVolatility;
};

#endif