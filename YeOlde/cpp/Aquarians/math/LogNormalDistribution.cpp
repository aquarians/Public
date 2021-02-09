#include "LogNormalDistribution.hpp"
#include <math.h>

namespace math
{

const double LogNormalDistribution::SQRT2PI = sqrt(2 * M_PI);

LogNormalDistribution::LogNormalDistribution(double mean, double dev)
{
    mMean = mean;
    mDev = dev;
}

double LogNormalDistribution::pdf(double x) const
{
    double z = log(x) - mMean;
    return exp(-(z * z) / (2 * mDev * mDev)) / (x * mDev * SQRT2PI);
}

double LogNormalDistribution::expectedValue(math::SISORealFunction *f, int steps)
{
    // A lognormal random variable X is defined as X = exp(mean + dev * Z) where Z is a standard-normally-distributed random variable
    // One knows that 99.7% of Z lye within 3 standard-normal deviations, therefore in the [-3, 3] interval
    // Be a bit more generous and use [-5, 5] as the limits
    double xmin = exp(mMean - 5.0 * mDev);
    double xmax = exp(mMean + 5.0 * mDev);    
    Product product(this, f);
    return Calculus::integral(&product, xmin, xmax, steps);
}

} // namespace math
