#pragma once

#include <istream>
#include <string>
#include <vector>

class StringUtils {
public:
    static std::vector<std::string> splitString(const std::string& str, char delim);
};