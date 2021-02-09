#include "StringTokenizer.hpp"
#include "Exception.hpp"

namespace aqlib
{

StringTokenizer::StringTokenizer(const std::string &text, const std::string &delimiter)
{
    mPos = 0;
    
    std::size_t pos = 0;
    std::size_t idx = text.find(delimiter, pos);
    
    while ((pos < text.size()) && (idx != std::string::npos))
    {
        std::string token = text.substr(pos, idx - pos);
        mTokens.push_back(token);
        pos = idx + delimiter.size();
        idx = text.find(delimiter, pos);
    }
    
    if (pos < text.size())
    {
        std::string token = text.substr(pos);
        mTokens.push_back(token);
    }
}

bool StringTokenizer::hasMoreTokens() const
{
    return (mPos < mTokens.size());
}

const std::string& StringTokenizer::nextToken()
{
    if (mTokens.size() == mPos)
    {
        throw aqlib::Exception("No more tokens");
    }
    
    return mTokens[mPos++];
}

} // namespace aqlib
