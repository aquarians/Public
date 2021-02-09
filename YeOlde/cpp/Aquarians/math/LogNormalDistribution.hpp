#ifndef __LOGNORMAL_DISTRIBUTION_HPP
#define __LOGNORMAL_DISTRIBUTION_HPP

#include "Calculus.hpp"

namespace math
{

// See http://en.wikipedia.org/wiki/Log-normal_distribution
class LogNormalDistribution
{
    static const double SQRT2PI;
    
    double mMean;
    double mDev;
    
public:
    LogNormalDistribution(double mean = 0.0, double dev = 1.0);
    
     // Probability density function
     double pdf(double x) const;
     
     // Computes the expected value of given f(x), when x follows this lognormal distribution
     double expectedValue(math::SISORealFunction *f, int steps = 100);
     
private:
    // Computes product of given distribution and function
    class Product: public math::SISORealFunction 
    {
        LogNormalDistribution *mDist;
        math::SISORealFunction *mF;
        
    public:
        Product(LogNormalDistribution *dist, math::SISORealFunction *f)
        {
            mDist = dist;
            mF = f;
        }
        
        double getSISORealValue(double x)
        {
            return mDist->pdf(x) * mF->getSISORealValue(x);
        }
    };
};

} // namespace math

#endif // __LOGNORMAL_DISTRIBUTION_HPP
