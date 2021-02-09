#include "DistributionHistogram.hpp"
#include <map>
#include <sstream>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

namespace math
{

log4cxx::LoggerPtr DistributionHistogram::logger(log4cxx::Logger::getLogger("math.DHist"));
    
DistributionHistogram::DistributionHistogram()
{
    mMean = 0.0;
    mDeviation = 0.0;
}

void DistributionHistogram::add(double value)
{
    mValues.push_back(value);
}

void DistributionHistogram::compute()
{
    mMean = computeMean();
    mDeviation = computeDeviation(mMean);
}

void DistributionHistogram::computeHistogram(std::map<double, double> &distribution, int intervals)
{
    std::map<int, int> histogram;
    for (int i = 0; i < intervals; ++i)
    {
        histogram[i] = 0;
    }

    double vmin = +1e16;
    double vmax = -1e16;
    
    for (Values::const_iterator it = mValues.begin(); it != mValues.end(); ++it)
    {
        double value = *it;
        vmin = std::min(vmin, value);
        vmax = std::max(vmax, value);
    }
    
    double distance = vmax - vmin;
    double step = distance / intervals;
    
    for (Values::const_iterator it = mValues.begin(); it != mValues.end(); ++it)
    {
        double value = *it;        
        double position = (value - vmin) / distance;        
        int interval = (int)(position * intervals);
        histogram[interval]++;
    }

    // Latex bar chart coordinates: (0,3) (1,2) (2,4) (3,1) (4,2)
    std::stringstream strmLaTeX;
    for (int i = 0; i < intervals; ++i)
    {
        double value = vmin + i * step + step * 0.5;
        int frequency = histogram[i];
        
        //LOG4CXX_DEBUG(logger, "DHIST value=" << value << " freq=" << frequency << " p=" << ((double)frequency / mValues.size()));
        //if (((double)frequency / mValues.size()) < 0.01) continue;

        distribution[value] = frequency;
        strmLaTeX << "(" << value << "," << frequency << ") ";
    }
    LOG4CXX_DEBUG(logger, "LATEX " << strmLaTeX.str());
}

void DistributionHistogram::print(int intervals, int stars) const
{
    std::map<int, int> histogram;
    for (int i = 0; i < intervals; ++i)
    {
        histogram[i] = 0;
    }

    double vmin = +1e16;
    double vmax = -1e16;
    
    for (Values::const_iterator it = mValues.begin(); it != mValues.end(); ++it)
    {
        double value = *it;
        vmin = std::min(vmin, value);
        vmax = std::max(vmax, value);
    }
    
    double distance = vmax - vmin;
    double step = distance / intervals;
    
    for (Values::const_iterator it = mValues.begin(); it != mValues.end(); ++it)
    {
        double value = *it;        
        double position = (value - vmin) / distance;        
        int interval = (int)(position * intervals);
        histogram[interval] ++;
    }

    for (int i = 0; i < intervals; ++i)
    {
        double start = vmin + i * step;
        double stop = start + step;                
        
        double density = (double)histogram[i] / (double)mValues.size();
        density = round(density * 100.0) / 100.0; // round to 2 decimals        

        std::stringstream strm; 
        if (stars)
        {
            for (int k = 0; k < density * stars; ++k)
            {
                strm << "*";
            }
        }
        
        LOG4CXX_INFO(logger, ""
            << "\t" << toDouble(start)
            << "\t" << toDouble(stop)
            << "\t" << toDouble(density)
            << "\t" << strm.str());
    }
}

std::string DistributionHistogram::toDouble(double value) const
{
    char szValue[64];
    sprintf(szValue, "%.4f", value);
    return szValue;
}

double DistributionHistogram::computeMean() const
{
    double total = 0.0;
    for (Values::const_iterator it = mValues.begin(); it != mValues.end(); ++it)
    {
        double value = *it;
        total += value;
    }
    total /= std::max((int)mValues.size() - 1 , 1);
    return total;
}

double DistributionHistogram::computeDeviation(double mean) const
{
    double total = 0.0;
    for (Values::const_iterator it = mValues.begin(); it != mValues.end(); ++it)
    {
        double value = *it - mean;
        total += value * value;
    }
    total /= std::max((int)mValues.size() - 1 , 1);
    return sqrt(total);
}

} // namespace math
