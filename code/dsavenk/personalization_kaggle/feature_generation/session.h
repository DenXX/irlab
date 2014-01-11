#pragma once

#include <vector>
#include <unordered_map>
#include <istream>
#include <ostream>

struct Click {
public:
    int url_id;
    int time_passed;
    int dwell;
};

struct Query {
public:
    bool is_test;
    int id;
    int serp_id;
    std::vector<int> terms;
    int time_passed;
    std::vector<std::pair<int, int> > docs;
    std::unordered_map<int, int> urlid2pos;
    std::vector<Click> clicks;
};

struct Session {
public:
    int id;
    char day;
    int user_id;
    std::vector<Query> queries;

    static bool readSession(std::istream& stream, Session& session, bool skipQC);

    void writeTest(std::ostream& out);
};