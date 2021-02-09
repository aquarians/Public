#include <math.h>
#include "Calculus.hpp"

namespace math
{


int Calculus::round(double value)
{
    int idxFloor = floor(value);
    int idxCeil = ceil(value);
    double distFloor = fabs(value - idxFloor);
    double distCeil = fabs(idxCeil - value);
    int pos = distFloor < distCeil ? idxFloor : idxCeil;
    return pos;
}

// See http://en.wikipedia.org/wiki/Trapezoidal_rule
double Calculus::integral(SISORealFunction *f, double xmin, double xmax, int steps)
{
    double h = (xmax - xmin) / steps;
    double sum = 0.0;
    for (int i = 0; i <= steps; ++i)
    {
        double x = xmin + h * i;
        double m = ((0 == i) || (steps == i)) ? 1 : 2;
        sum += m * f->getSISORealValue(x);
    }
    return sum * h * 0.5;
}

} // namespace math
