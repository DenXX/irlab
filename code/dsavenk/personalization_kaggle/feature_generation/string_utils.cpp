#include <sstream>
#include <iostream>

#include "string_utils.h"

std::vector<std::string> StringUtils::splitString(const std::string& str, char delim) {
    std::stringstream stream(str);
    std::vector<std::string> fields;
    std::string field;
    while(std::getline(stream, field, delim)) {
        fields.push_back(field);
    }
    return fields;
}
