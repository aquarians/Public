#ifndef __NORMAL_DISTRIBUTION_HPP
#define __NORMAL_DISTRIBUTION_HPP

namespace math
{

class NormalDistribution
{
public:
    NormalDistribution(double mean = 0.0, double deviation = 1.0);

    /**
     * Generates a normally-distributed random number X ~ N(m,v^2)
     * @param m mean
     * @param v standard deviation
     * @return a random number of given mean and standard deviation
     */
     double rnd();
     
     // Probability density function
     double pdf(double x) const;

     // Cummulative distribution function
     double cdf(double x);

     // Inverse cummulative distribution function
     double icdf(double probability);
     
     double min() const { return mMin; }
     double max() const { return mMax; }

private:
    double stdnormInverseCDF(double p);

private:
    double mMean;
    double mDeviation;
    double mVariance;
    
    double mMin;
    double mMax;
};

} // namespace math

#endif // __NORMAL_DISTRIBUTION_HPP
