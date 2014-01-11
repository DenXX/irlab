
#include <assert.h>
#include <algorithm>
#include <stdlib.h>
#include <unordered_map>
#include <string>
#include <sstream>
#include <iostream>
#include <vector>

#include "session.h"
#include "string_utils.h"

bool Session::readSession(std::istream& stream, Session& session, bool skipQC) {
    std::string line;
    std::getline(stream, line);
    std::vector<std::string> fields = StringUtils::splitString(line, '\t');
    // Asserts
    if (stream.eof() || fields.size() == 0) return false;
    assert(fields.size() == 4);
    assert(fields[1] == "M");

    session.id = atoll(fields[0].c_str());
    session.day = atoi(fields[2].c_str());
    session.user_id = atoll(fields[3].c_str());
    
    std::unordered_map<int, int> serp2index;

    bool stop = false;
    while(stream.good() && !stop) {
        std::getline(stream, line);
        fields = StringUtils::splitString(line, '\t');
        if (stream.eof() || fields.size() == 0) break;
        if (fields[1] == "M") {
            stop = true;
            stream.seekg(-line.length()-1, stream.cur);
        }
        else {
            if (!skipQC && (fields[2] == "Q" || fields[2] == "T")) {
                Query q;
                q.is_test = fields[2] == "T";
                q.time_passed = atoi(fields[1].c_str());
                q.serp_id = atoll(fields[3].c_str());
                q.id = atoll(fields[4].c_str());
                std::vector<std::string> terms = StringUtils::splitString(fields[5], ',');
                for (auto term = terms.begin(); term != terms.end(); ++term)
                    q.terms.push_back(atoll(term->c_str()));
                // Read urls and domains
                for (int i = 6; i < fields.size(); ++i) {
                    std::vector<std::string> doc = StringUtils::splitString(fields[i], ',');
                    q.docs.push_back(std::make_pair(atoll(doc[0].c_str()), atoll(doc[1].c_str())));
                    q.urlid2pos[q.docs.back().first] = q.docs.size() - 1;
                }
                serp2index[q.serp_id] = session.queries.size();
                session.queries.push_back(q);
            } else if (!skipQC && fields[2] == "C") {
                Click c;
                c.time_passed = atoi(fields[1].c_str());
                c.url_id = atoll(fields[4].c_str());
                int serpid = atoll(fields[3].c_str());

                // Get dwell time
                std::getline(stream, line);
                fields = StringUtils::splitString(line, '\t');
                if (fields.size() == 0 || fields[1] == "M") c.dwell = -1;
                else c.dwell = atoi(fields[1].c_str()) - c.time_passed;
                stream.seekg(-line.length()-1, stream.cur);

                session.queries[serp2index[serpid]].clicks.push_back(c);
            }
        }
    };

    return stream.good();
}

void Session::writeTest(std::ostream& out) {
    out << this->id << "\tM\t" << int(this->day) << "\t" << this->user_id << '\n';
    std::vector<std::pair<int, std::string> > lines;

    for (int i = 0; i < this->queries.size(); ++i) {
        const Query& q = this->queries[i];
        std::stringstream str;
        char tp = q.is_test ? 'T' : 'Q';
        str << this->id << "\t" << q.time_passed << "\t" << tp << "\t"
            << q.serp_id << "\t" << q.id << "\t";
        for (int j = 0; j < q.terms.size(); ++j) {
            if (j > 0) str << ",";
            str << q.terms[j];
        }
        
        for (int j = 0; j < q.docs.size(); ++j) {
            str << "\t";
            str << q.docs[j].first << "," << q.docs[j].second;
        }
        str << '\n';
        lines.push_back(std::make_pair(q.time_passed, str.str()));

        for (int j = 0; j < q.clicks.size(); ++j) {
            str.str(std::string());
            const Click& c = q.clicks[j];
            str << this->id << "\t" << c.time_passed << "\tC\t" << q.serp_id 
                << "\t" << c.url_id << '\n';
            lines.push_back(std::make_pair(c.time_passed, str.str()));
        }
    }

    std::sort(lines.begin(), lines.end());
    for (int i = 0; i < lines.size(); ++i)
        out << lines[i].second;
}