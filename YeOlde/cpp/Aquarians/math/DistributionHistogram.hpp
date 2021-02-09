#ifndef __DISTRIBUTION_HISTOGRAM_HPP_INCLUDED
#define __DISTRIBUTION_HISTOGRAM_HPP_INCLUDED

#include <log4cxx/logger.h>

namespace math
{

#include <vector>
#include <string>
#include <map>

class DistributionHistogram
{
    static log4cxx::LoggerPtr logger;
    
public:
    DistributionHistogram();
    
    void add(double value);
    
    void print(int intervals = 21, int stars = 100) const;
    
    void computeHistogram(std::map<double, double> &distribution, int intervals = 21);

    double computeMean() const;
    double computeDeviation(double mean) const;

    void compute();

    double getMean() const { return mMean; }
    double getDeviation() const { return mDeviation; }    
    
    typedef std::vector<double> Values;
    const Values& getValues() const { return mValues; }

private:    
    Values mValues;
    double mMean;
    double mDeviation;    
    
    std::string toDouble(double value) const;
};

} // namespace math

#endif // __DISTRIBUTION_HISTOGRAM_HPP_INCLUDED
