#pragma once

#include <math.h>
#include <map>
#include <set>
#include <unordered_map>
#include <unordered_set>


#include "session.h"

struct ClickStats {
    int pos;
    int clicks;
    int sat_clicks;
    int ssat_clicks;
    int shows;
    long long dwell;
    int last_in_query;
    int last_in_session;
    //long long time2click;

    ClickStats():
        pos(0),
        clicks(0),
        sat_clicks(0),
        ssat_clicks(0),
        shows(0),
        dwell(0),
        last_in_query(0),
        last_in_session(0)
        //time2click(0)
    {
    }

    void addClick(const Session& session, int query_index, int click_index) {
        const Query& q = session.queries[query_index];
        const Click& c = q.clicks[click_index];
        ++clicks;
        dwell += c.dwell;
        if (c.dwell >= 50)
            ++sat_clicks;
        if (c.dwell >= 400)
            ++ssat_clicks;
        last_in_query += (click_index == q.clicks.size() - 1);
        last_in_session += ((click_index == q.clicks.size() - 1) &&
            (query_index == session.queries.size() - 1));
        //time2click += c.time_passed;
    }

    void addShow(int pos) {
        ++shows;
        this->pos += pos;
    }
};

struct DocumentStats {
    ClickStats overall;
    std::map<int, ClickStats> query_clicks;
    std::map<int, ClickStats> term_clicks;
    std::map<int, ClickStats> prev_query_clicks;
    std::map<int, ClickStats> coclicks;
};

struct PosClickStats {
    int clicks;
    int posclicks[10];

    PosClickStats() {
        clicks = 0;
        posclicks[0] = 0;
        posclicks[1] = 0;
        posclicks[2] = 0;
        posclicks[3] = 0;
        posclicks[4] = 0;
        posclicks[5] = 0;
        posclicks[6] = 0;
        posclicks[7] = 0;
        posclicks[8] = 0;
        posclicks[9] = 0;
    }

    double getEntropy() {
        double res = 0;
        for (int i = 0; i < 10; ++i)
            res += (1.0 * posclicks[i] / (clicks + 1)) * log(1.0 * (posclicks[i]) / (clicks + 1) + 0.0000001);
        return res;
    }
};

struct SessionStats {
    std::map<int, DocumentStats> url_stats;
    std::map<int, DocumentStats> domain_stats;
};


class StatsCollector {
public:
    SessionStats overall_stats;
    std::map<int, SessionStats> user_stats;

    std::map<int, PosClickStats> query_entropy;
    std::map<int, PosClickStats> user_entropy;

    std::set<int> terms_filter;
    std::set<int> query_filter;
    std::set<int> url_filter;
    std::set<int> domain_filter;
    std::set<int> user_filter;

    void buildFilter(const Session& session);

    void processSession(const Session& session);
};
