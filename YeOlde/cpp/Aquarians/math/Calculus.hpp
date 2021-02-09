#ifndef __MATH_CALCULUS_HPP
#define __MATH_CALCULUS_HPP

namespace math
{

class SISORealFunction;

class Calculus
{
public:
    static int round(double value);

    // Computes integral from xmin to xmax of f(x) * dx
    static double integral(SISORealFunction *f, double xmin, double xmax, int steps = 100);
};

// Single input single output real function
class SISORealFunction
{
protected:
    SISORealFunction() {}
public:
    virtual ~SISORealFunction() {}    
    virtual double getSISORealValue(double param) = 0;
};

} // namespace math

#endif // __MATH_CALCULUS_HPP
