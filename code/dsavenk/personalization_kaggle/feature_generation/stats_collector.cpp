
#include <iostream>

#include "stats_collector.h"

void StatsCollector::buildFilter(const Session& session) {
    user_filter.insert(session.user_id);
    for (int i = 0; i < session.queries.size(); ++i) {
        const Query& q = session.queries[i];
        query_filter.insert(q.id);
        for (int j = 0; j < q.docs.size(); ++j) {
            url_filter.insert(q.docs[j].first);
            domain_filter.insert(q.docs[j].second);
        }
        for (int j = 0; j < q.terms.size(); ++j)
            terms_filter.insert(q.terms[j]);
    }
}

void StatsCollector::processSession(const Session& session) {
    std::unordered_set<int> queries;
    std::unordered_set<int> urls;
    std::unordered_set<int> domains;
    for (int i = 0; i < session.queries.size(); ++i) {
        const Query& q = session.queries[i];
        for (int j = 0; j < q.docs.size(); ++j) {
            // Overall stats
            {
                // URL shows
                if (url_filter.find(q.docs[j].first) != url_filter.end()) {
                    overall_stats.url_stats[q.docs[j].first].overall.addShow(j);
                    if (query_filter.find(q.id) != query_filter.end())
                        overall_stats.url_stats[q.docs[j].first].query_clicks[q.id].addShow(j);

                    for (int k = 0; k < q.terms.size(); ++k)
                        if (terms_filter.find(q.terms[k]) != terms_filter.end())
                            overall_stats.url_stats[q.docs[j].first].term_clicks[q.terms[k]].addShow(j);

                    for (auto it = queries.begin(); it != queries.end(); ++it)
                        overall_stats.url_stats[q.docs[j].first].prev_query_clicks[*it].addShow(j);

                    for (auto it = urls.begin(); it != urls.end(); ++it)
                        overall_stats.url_stats[q.docs[j].first].coclicks[*it].addShow(j);
                }
                // Domain shows
                if (domain_filter.find(q.docs[j].second) != domain_filter.end()) {
                    overall_stats.domain_stats[q.docs[j].second].overall.addShow(j);
                    if (query_filter.find(q.id) != query_filter.end())
                        overall_stats.domain_stats[q.docs[j].second].query_clicks[q.id].addShow(j);

                    for (int k = 0; k < q.terms.size(); ++k)
                         if (terms_filter.find(q.terms[k]) != terms_filter.end())
                             overall_stats.domain_stats[q.docs[j].second].term_clicks[q.terms[k]].addShow(j);

                    for (auto it = queries.begin(); it != queries.end(); ++it)
                        overall_stats.domain_stats[q.docs[j].second].prev_query_clicks[*it].addShow(j);
                    
                    for (auto it = domains.begin(); it != domains.end(); ++it)
                        overall_stats.domain_stats[q.docs[j].second].coclicks[*it].addShow(j);
                }
            }

            // Per user stats
            if (user_filter.find(session.user_id) != user_filter.end())
            {
                // URL shows
                if (url_filter.find(q.docs[j].first) != url_filter.end()) {
                    user_stats[session.user_id].url_stats[q.docs[j].first].overall.addShow(j);
                    if (query_filter.find(q.id) != query_filter.end())
                        user_stats[session.user_id].url_stats[q.docs[j].first].query_clicks[q.id].addShow(j);

                    for (int k = 0; k < q.terms.size(); ++k)
                        if (terms_filter.find(q.terms[k]) != terms_filter.end())
                            user_stats[session.user_id].url_stats[q.docs[j].first].term_clicks[q.terms[k]].addShow(j);

                    for (auto it = queries.begin(); it != queries.end(); ++it)
                        user_stats[session.user_id].url_stats[q.docs[j].first].prev_query_clicks[*it].addShow(j);

                    for (auto it = urls.begin(); it != urls.end(); ++it)
                        user_stats[session.user_id].url_stats[q.docs[j].first].coclicks[*it].addShow(j);
                }
                // Domain shows
                if (domain_filter.find(q.docs[j].second) != domain_filter.end()) {
                    user_stats[session.user_id].domain_stats[q.docs[j].second].overall.addShow(j);
                    if (query_filter.find(q.id) != query_filter.end())
                        user_stats[session.user_id].domain_stats[q.docs[j].second].query_clicks[q.id].addShow(j);

                    // TODO: reenable
                    for (int k = 0; k < q.terms.size(); ++k)
                         if (terms_filter.find(q.terms[k]) != terms_filter.end())
                             user_stats[session.user_id].domain_stats[q.docs[j].second].term_clicks[q.terms[k]].addShow(j);

                    for (auto it = queries.begin(); it != queries.end(); ++it)
                        user_stats[session.user_id].domain_stats[q.docs[j].second].prev_query_clicks[*it].addShow(j);

                    for (auto it = domains.begin(); it != domains.end(); ++it)
                        user_stats[session.user_id].domain_stats[q.docs[j].second].coclicks[*it].addShow(j);
                }
            }
        }

        for (int j = 0; j < q.clicks.size(); ++j) {
            const Click& c = q.clicks[j];
            int click_pos = q.urlid2pos.at(c.url_id);
            int domain_id = q.docs[click_pos].second;
            // Overall stats
            {
                // URL clicks
                overall_stats.url_stats[c.url_id].overall.addClick(session, i, j);
                if (query_filter.find(q.id) != query_filter.end()) {
                    ++query_entropy[q.id].clicks;
                    ++query_entropy[q.id].posclicks[click_pos];
                    overall_stats.url_stats[c.url_id].query_clicks[q.id].addClick(session, i, j);
                }

                for (int k = 0; k < q.terms.size(); ++k)
                    if (terms_filter.find(q.terms[k]) != terms_filter.end())
                        overall_stats.url_stats[q.docs[j].second].term_clicks[q.terms[k]].addClick(session, i, j);

                for (auto it = queries.begin(); it != queries.end(); ++it)
                    overall_stats.url_stats[c.url_id].prev_query_clicks[*it].addClick(session, i, j);
                
                for (auto it = urls.begin(); it != urls.end(); ++it)
                    overall_stats.url_stats[c.url_id].coclicks[*it].addClick(session, i, j);

                // Domain clicks
                overall_stats.domain_stats[domain_id].overall.addClick(session, i, j);
                if (query_filter.find(q.id) != query_filter.end())
                    overall_stats.domain_stats[domain_id].query_clicks[q.id].addClick(session, i, j);

                for (int k = 0; k < q.terms.size(); ++k)
                    if (terms_filter.find(q.terms[k]) != terms_filter.end())
                         overall_stats.domain_stats[domain_id].term_clicks[q.terms[k]].addClick(session, i, j);

                for (auto it = queries.begin(); it != queries.end(); ++it)
                    overall_stats.domain_stats[domain_id].prev_query_clicks[*it].addClick(session, i, j);

                for (auto it = domains.begin(); it != domains.end(); ++it)
                    overall_stats.domain_stats[domain_id].coclicks[*it].addClick(session, i, j);
            }

            // Per user stats
            if (user_filter.find(session.user_id) != user_filter.end())
            {
                ++user_entropy[q.id].clicks;
                ++user_entropy[q.id].posclicks[click_pos];

                // URL clicks
                if (url_filter.find(c.url_id) != url_filter.end()) {
                    user_stats[session.user_id].url_stats[c.url_id].overall.addClick(session, i, j);
                    if (query_filter.find(q.id) != query_filter.end())
                        user_stats[session.user_id].url_stats[c.url_id].query_clicks[q.id].addClick(session, i, j);

                    for (int k = 0; k < q.terms.size(); ++k)
                        if (terms_filter.find(q.terms[k]) != terms_filter.end())
                            user_stats[session.user_id].url_stats[q.docs[j].second].term_clicks[q.terms[k]].addClick(session, i, j);

                    for (auto it = queries.begin(); it != queries.end(); ++it)
                        user_stats[session.user_id].url_stats[c.url_id].prev_query_clicks[*it].addClick(session, i, j);

                    for (auto it = urls.begin(); it != urls.end(); ++it)
                        user_stats[session.user_id].url_stats[c.url_id].coclicks[*it].addClick(session, i, j);
                }
                // Domain clicks
                if (domain_filter.find(domain_id) != domain_filter.end()) {
                    user_stats[session.user_id].domain_stats[domain_id].overall.addClick(session, i, j);
                    if (query_filter.find(q.id) != query_filter.end())
                        user_stats[session.user_id].domain_stats[domain_id].query_clicks[q.id].addClick(session, i, j);

                    // TODO: try enabling
                    for (int k = 0; k < q.terms.size(); ++k)
                         if (terms_filter.find(q.terms[k]) != terms_filter.end())
                             user_stats[session.user_id].domain_stats[domain_id].term_clicks[q.terms[k]].addClick(session, i, j);

                    for (auto it = queries.begin(); it != queries.end(); ++it)
                        user_stats[session.user_id].domain_stats[domain_id].prev_query_clicks[*it].addClick(session, i, j);

                    for (auto it = domains.begin(); it != domains.end(); ++it)
                        user_stats[session.user_id].domain_stats[domain_id].coclicks[*it].addClick(session, i, j);
                }
            }
            if (url_filter.find(c.url_id) != url_filter.end())
                urls.insert(c.url_id);
            if (domain_filter.find(domain_id) != domain_filter.end())
                domains.insert(domain_id);
        }

        if (query_filter.find(q.id) != query_filter.end())
            queries.insert(q.id);
    }
}
