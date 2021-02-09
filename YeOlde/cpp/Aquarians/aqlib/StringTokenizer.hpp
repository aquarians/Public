#ifndef __AQLIB_STRING_TOKENIZER_HPP
#define __AQLIB_STRING_TOKENIZER_HPP

#include <string>
#include <vector>

namespace aqlib
{

// See java.util.StringTokenizer
class StringTokenizer
{
public:
    StringTokenizer(const std::string &text, const std::string &delimiter);

    bool hasMoreTokens() const;
    const std::string& nextToken();

private:
    int mPos;
    std::vector <std::string> mTokens;    
};

} // namespace aqlib

#endif // __AQLIB_STRING_TOKENIZER_HPP
