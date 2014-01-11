
#include "feature_generator.h"

Features::Features() {
    u_shows = 0;
    u_clicks = 0;
    u_ctr = 0;
    u_ctr2 = 0;
    u_satctr = 0;
    u_ssatctr = 0;
    u_avedwell = 0;
    qu_shows = 0;
    qu_clicks = 0;
    qu_ctr = 0;
    qu_ctr2 = 0;
    qu_satctr = 0;
    qu_ssatctr = 0;
    qu_avedwell = 0;
    d_shows = 0;
    d_clicks = 0;
    d_ctr = 0;
    d_ctr2 = 0;
    d_satctr = 0;
    d_ssatctr = 0;
    d_avedwell = 0;
    qd_shows = 0;
    qd_clicks = 0;
    qd_ctr = 0;
    qd_ctr2 = 0;
    qd_satctr = 0;
    qd_ssatctr = 0;
    qd_avedwell = 0;
    u_avepos = 0;
    qu_avepos = 0;
    d_avepos = 0;
    qd_avepos = 0;
    u_lastinq_clicks = 0;
    u_lastinq_ctr = 0;
    u_lastinsession_clicks = 0;
    u_lastinsession_ctr = 0;
    qu_lastinq_clicks = 0;
    qu_lastinq_ctr = 0;
    qu_lastinsession_clicks = 0;
    qu_lastinsession_ctr = 0;
    d_lastinq_clicks = 0;
    d_lastinq_ctr = 0;
    d_lastinsession_clicks = 0;
    d_lastinsession_ctr = 0;
    qd_lastinq_clicks = 0;
    qd_lastinq_ctr = 0;
    qd_lastinsession_clicks = 0;
    qd_lastinsession_ctr = 0;

    max_u_prevq_ctr = 0;
    max_u_prevu_ctr = 0;
    max_d_prevq_ctr = 0;
    max_d_prevd_ctr = 0;

    have_user_history = 0;
    u_shows_user = 0;
    u_clicks_user = 0;
    u_ctr_user = 0;
    u_ctr2_user = 0;
    u_satctr_user = 0;
    u_ssatctr_user = 0;
    u_avedwell_user = 0;
    qu_shows_user = 0;
    qu_clicks_user = 0;
    qu_ctr_user = 0;
    qu_ctr2_user = 0;
    qu_satctr_user = 0;
    qu_ssatctr_user = 0;
    qu_avedwell_user = 0;
    d_shows_user = 0;
    d_clicks_user = 0;
    d_ctr_user = 0;
    d_ctr2_user = 0;
    d_satctr_user = 0;
    d_ssatctr_user = 0;
    d_avedwell_user = 0;
    qd_shows_user = 0;
    qd_clicks_user = 0;
    qd_ctr_user = 0;
    qd_ctr2_user = 0;
    qd_satctr_user = 0;
    qd_ssatctr_user = 0;
    qd_avedwell_user = 0;
    u_avepos_user = 0;
    qu_avepos_user = 0;
    d_avepos_user = 0;
    qd_avepos_user = 0;
    u_lastinq_clicks_user = 0;
    u_lastinq_ctr_user = 0;
    u_lastinsession_clicks_user = 0;
    u_lastinsession_ctr_user = 0;
    qu_lastinq_clicks_user = 0;
    qu_lastinq_ctr_user = 0;
    qu_lastinsession_clicks_user = 0;
    qu_lastinsession_ctr_user = 0;
    d_lastinq_clicks_user = 0;
    d_lastinq_ctr_user = 0;
    d_lastinsession_clicks_user = 0;
    d_lastinsession_ctr_user = 0;
    qd_lastinq_clicks_user = 0;
    qd_lastinq_ctr_user = 0;
    qd_lastinsession_clicks_user = 0;
    qd_lastinsession_ctr_user = 0;

    max_u_prevq_ctr_user = 0;
    max_u_prevu_ctr_user = 0;
    max_d_prevq_ctr_user = 0;
    max_d_prevd_ctr_user = 0;

    query_asked = 0;
    url_shown = 0;
    url_clicked = 0;
    cur_total_dwell = 0;
    time_passed = 0;
    cur_queries = 0;
    cur_clicks = 0;

    cur_pos = 0;

    q_terms = 0;
    td_shows = 0;
    td_clicks = 0;
    td_ctr = 0;
    td_ctr2 = 0;
    td_avedwell = 0;
    tu_shows = 0;
    tu_clicks = 0;
    tu_ctr = 0;
    tu_ctr2 = 0;
    tu_avedwell = 0;

    tu_shows_user = 0;
    tu_clicks_user = 0;
    tu_ctr_user = 0;
    tu_ctr2_user = 0;
    tu_avedwell_user = 0;
    td_shows_user = 0;
    td_clicks_user = 0;
    td_ctr_user = 0;
    td_ctr2_user = 0;
    td_avedwell_user = 0;

    qclicks1 = 0;
    qclicks2 = 0;
    qclicks3 = 0;
    qclicks4 = 0;
    qclicks5 = 0;
    qclicks6 = 0;
    qclicks7 = 0;
    qclicks8 = 0;
    qclicks9 = 0;
    qclicks10 = 0;
    qentropy = 0;
    uclicks1 = 0;
    uclicks2 = 0;
    uclicks3 = 0;
    uclicks4 = 0;
    uclicks5 = 0;
    uclicks6 = 0;
    uclicks7 = 0;
    uclicks8 = 0;
    uclicks9 = 0;
    uclicks10 = 0;
    uentropy = 0;
}

void FeatureGenerator::writeFeatureHeader(std::ostream& out) {
    out << "target,sessionid,url_id," << Features::getHeader() << "\n";
}

void FeatureGenerator::writeFeatures(std::ostream& out, Session& session, StatsCollector& stats) {

    std::unordered_set<int> seen_queries;
    std::unordered_set<int> seen_terms;
    std::unordered_set<int> seen_urls;
    std::unordered_set<int> seen_domains;
    std::unordered_set<int> clicked_urls;
    std::unordered_set<int> clicked_domains;

    int test_query_index = -1;
    int clicks = 0;
    int dwell = 0;
    for (int i = 0; i < session.queries.size(); ++i) {
        Query& q = session.queries[i];
        if (q.is_test) {
            test_query_index = i;
            break;
        }
        for (int j = 0; j < q.docs.size(); ++j) {
            seen_urls.insert(q.docs[j].first);
            seen_domains.insert(q.docs[j].second);
        }
        for (int j = 0; j < q.clicks.size(); ++j) {
            clicked_urls.insert(q.clicks[j].url_id);
            clicked_domains.insert(q.docs[q.urlid2pos[q.clicks[j].url_id]].second);
            ++clicks;
            dwell += q.clicks[j].dwell;
        }

        for (int j = 0; j < q.terms.size(); ++j) {
            seen_terms.insert(q.terms[j]);
        }

        seen_queries.insert(q.id);
    }

    if (test_query_index == -1) {
        std::cerr << "NO TEST QUERY IN SESSION " << session.id << std::endl;
        return;
    }
    const Query& test_query = session.queries[test_query_index];

    for (int i = 0; i < test_query.docs.size(); ++i) {
        int url_id = test_query.docs[i].first;
        int domain_id = test_query.docs[i].second;

        Features feats;

        if (stats.query_entropy.find(test_query.id) != stats.query_entropy.end()) {
            feats.qclicks1 = 1.0 * stats.query_entropy[test_query.id].posclicks[0] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks2 = 1.0 * stats.query_entropy[test_query.id].posclicks[1] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks3 = 1.0 * stats.query_entropy[test_query.id].posclicks[2] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks4 = 1.0 * stats.query_entropy[test_query.id].posclicks[3] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks5 = 1.0 * stats.query_entropy[test_query.id].posclicks[4] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks6 = 1.0 * stats.query_entropy[test_query.id].posclicks[5] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks7 = 1.0 * stats.query_entropy[test_query.id].posclicks[6] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks8 = 1.0 * stats.query_entropy[test_query.id].posclicks[7] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks9 = 1.0 * stats.query_entropy[test_query.id].posclicks[8] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qclicks10 = 1.0 * stats.query_entropy[test_query.id].posclicks[9] / (stats.query_entropy[test_query.id].clicks + 1);
            feats.qentropy = stats.query_entropy[test_query.id].getEntropy();
        }

        if (stats.user_entropy.find(session.user_id) != stats.user_entropy.end()) {
            feats.uclicks1 = 1.0 * stats.user_entropy[test_query.id].posclicks[0] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks2 = 1.0 * stats.user_entropy[test_query.id].posclicks[1] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks3 = 1.0 * stats.user_entropy[test_query.id].posclicks[2] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks4 = 1.0 * stats.user_entropy[test_query.id].posclicks[3] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks5 = 1.0 * stats.user_entropy[test_query.id].posclicks[4] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks6 = 1.0 * stats.user_entropy[test_query.id].posclicks[5] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks7 = 1.0 * stats.user_entropy[test_query.id].posclicks[6] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks8 = 1.0 * stats.user_entropy[test_query.id].posclicks[7] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks9 = 1.0 * stats.user_entropy[test_query.id].posclicks[8] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uclicks10 = 1.0 * stats.user_entropy[test_query.id].posclicks[9] / (stats.user_entropy[test_query.id].clicks + 1);
            feats.uentropy = stats.user_entropy[test_query.id].getEntropy();
        }

        if (stats.overall_stats.url_stats.find(url_id) != stats.overall_stats.url_stats.end()) {
            ClickStats& cur_stats = stats.overall_stats.url_stats[url_id].overall;

            // Just URL
            feats.u_shows = cur_stats.shows;
            feats.u_clicks = cur_stats.clicks;
            feats.u_ctr = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
            feats.u_ctr2 = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
            feats.u_satctr = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
            feats.u_ssatctr = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
            feats.u_avedwell = 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);
            feats.u_avepos = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
            feats.u_lastinq_clicks = cur_stats.last_in_query;
            feats.u_lastinq_ctr = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
            feats.u_lastinsession_clicks = cur_stats.last_in_session;
            feats.u_lastinsession_ctr = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);

            // QUERY - URL
            if (stats.overall_stats.url_stats[url_id].query_clicks.find(test_query.id) != stats.overall_stats.url_stats[url_id].query_clicks.end()) {
                cur_stats = stats.overall_stats.url_stats[url_id].query_clicks[test_query.id];

                feats.qu_shows = cur_stats.shows;
                feats.qu_clicks = cur_stats.clicks;
                feats.qu_ctr = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                feats.qu_ctr2 = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                feats.qu_satctr = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
                feats.qu_ssatctr = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
                feats.qu_avedwell = 1.0 * cur_stats.dwell / (cur_stats.clicks + 1);
                feats.qu_avepos = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
                
                feats.qu_lastinq_clicks = cur_stats.last_in_query;
                feats.qu_lastinq_ctr = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
                feats.qu_lastinsession_clicks = cur_stats.last_in_session;
                feats.qu_lastinsession_ctr = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);
            }

            // URL for terms
            feats.q_terms = test_query.terms.size();
            feats.tu_shows = 0;
            feats.tu_clicks = 0;
            feats.tu_ctr = 0;
            feats.tu_ctr2 = 0;
            feats.tu_avedwell = 0;
            for (int k = 0; k < test_query.terms.size(); ++k) {
                if (stats.overall_stats.url_stats[url_id].term_clicks.find(test_query.terms[k]) != stats.overall_stats.url_stats[url_id].term_clicks.end()) {
                    cur_stats = stats.overall_stats.url_stats[url_id].term_clicks[test_query.terms[k]];

                    feats.tu_shows += cur_stats.shows;
                    feats.tu_clicks += cur_stats.clicks;
                    feats.tu_ctr += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                    feats.tu_ctr2 += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                    feats.tu_avedwell += 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);
                }
            }
            feats.tu_shows /= test_query.terms.size();
            feats.tu_clicks /= test_query.terms.size();
            feats.tu_ctr /= test_query.terms.size();
            feats.tu_ctr2 /= test_query.terms.size();
            feats.tu_avedwell /= test_query.terms.size();

            feats.prevtu_ctr = 0;
            // Previous terms
            for (auto it = seen_terms.begin(); it != seen_terms.end(); ++it) {
                if (stats.overall_stats.url_stats[url_id].term_clicks.find(*it) != stats.overall_stats.url_stats[url_id].term_clicks.end()) {
                    feats.prevtu_ctr = std::max(feats.prevtu_ctr, 1.0 * (stats.overall_stats.url_stats[url_id].term_clicks[*it].clicks) / (stats.overall_stats.url_stats[url_id].term_clicks[*it].shows + 100));
                }
            }

            // URL for previous urls and queries
            feats.max_u_prevq_ctr = 0;
            feats.max_u_prevu_ctr = 0;
            for (auto it = seen_queries.begin(); it != seen_queries.end(); ++it) {
                if (stats.overall_stats.url_stats[url_id].prev_query_clicks.find(*it) != stats.overall_stats.url_stats[url_id].prev_query_clicks.end()) {
                    feats.max_u_prevq_ctr = std::max(feats.max_u_prevq_ctr, 1.0 * (stats.overall_stats.url_stats[url_id].prev_query_clicks[*it].clicks) / (stats.overall_stats.url_stats[url_id].prev_query_clicks[*it].shows + 1));
                }
            }

            for (auto it = clicked_urls.begin(); it != clicked_urls.end(); ++it) {
                if (stats.overall_stats.url_stats[url_id].coclicks.find(*it) != stats.overall_stats.url_stats[url_id].coclicks.end()) {
                    feats.max_u_prevu_ctr = std::max(feats.max_u_prevu_ctr, 1.0 * (stats.overall_stats.url_stats[url_id].coclicks[*it].clicks) / (stats.overall_stats.url_stats[url_id].coclicks[*it].shows + 1));
                }
            }

        }

        // Domains
        if (stats.overall_stats.domain_stats.find(domain_id) != stats.overall_stats.domain_stats.end()) {
            ClickStats& cur_stats = stats.overall_stats.domain_stats[domain_id].overall;

            feats.d_shows = cur_stats.shows;
            feats.d_clicks = cur_stats.clicks;
            feats.d_ctr = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
            feats.d_ctr2 = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
            feats.d_satctr = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
            feats.d_ssatctr = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
            feats.d_avedwell = 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);
            feats.d_avepos = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
            feats.d_lastinq_clicks = cur_stats.last_in_query;
            feats.d_lastinq_ctr = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
            feats.d_lastinsession_clicks = cur_stats.last_in_session;
            feats.d_lastinsession_ctr = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);

            // Query Domains
            if (stats.overall_stats.domain_stats[domain_id].query_clicks.find(test_query.id) != stats.overall_stats.domain_stats[domain_id].query_clicks.end()) {
                cur_stats = stats.overall_stats.domain_stats[domain_id].query_clicks[test_query.id];
                feats.qd_shows = cur_stats.shows;
                feats.qd_clicks = cur_stats.clicks;
                feats.qd_ctr = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                feats.qd_ctr2 = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                feats.qd_satctr = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
                feats.qd_ssatctr = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
                feats.qd_avedwell = 1.0 * cur_stats.dwell / (cur_stats.clicks + 1);
                feats.qd_avepos = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
                
                feats.qd_lastinq_clicks = cur_stats.last_in_query;
                feats.qd_lastinq_ctr = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
                feats.qd_lastinsession_clicks = cur_stats.last_in_session;
                feats.qd_lastinsession_ctr = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);
            }

            // Domains - terms
            feats.td_shows = 0;
            feats.td_clicks = 0;
            feats.td_ctr = 0;
            feats.td_ctr2 = 0;
            feats.td_avedwell = 0;
            for (int k = 0; k < test_query.terms.size(); ++k) {
                if (stats.overall_stats.domain_stats[domain_id].term_clicks.find(test_query.terms[k]) != stats.overall_stats.domain_stats[domain_id].term_clicks.end()) {
                    cur_stats = stats.overall_stats.domain_stats[domain_id].term_clicks[test_query.terms[k]];

                    feats.td_shows += cur_stats.shows;
                    feats.td_clicks += cur_stats.clicks;
                    feats.td_ctr += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                    feats.td_ctr2 += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                    feats.td_avedwell += 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);
                }
            }
            feats.td_shows /= test_query.terms.size();
            feats.td_clicks /= test_query.terms.size();
            feats.td_ctr /= test_query.terms.size();
            feats.td_ctr2 /= test_query.terms.size();
            feats.td_avedwell /= test_query.terms.size();


            feats.prevtd_ctr = 0;
            // Previous terms
            for (auto it = seen_terms.begin(); it != seen_terms.end(); ++it) {
                if (stats.overall_stats.domain_stats[domain_id].term_clicks.find(*it) != stats.overall_stats.domain_stats[domain_id].term_clicks.end()) {
                    feats.prevtd_ctr = std::max(feats.prevtd_ctr, 1.0 * (stats.overall_stats.domain_stats[domain_id].term_clicks[*it].clicks) / (stats.overall_stats.domain_stats[domain_id].term_clicks[*it].shows + 100));
                }
            }

            // Domain for previous urls
            feats.max_d_prevq_ctr = 0;
            feats.max_d_prevd_ctr = 0;
            for (auto it = seen_queries.begin(); it != seen_queries.end(); ++it) {
                if (stats.overall_stats.domain_stats[domain_id].prev_query_clicks.find(*it) != stats.overall_stats.domain_stats[domain_id].prev_query_clicks.end()) {
                    feats.max_d_prevq_ctr = std::max(feats.max_d_prevq_ctr, 1.0 * (stats.overall_stats.domain_stats[domain_id].prev_query_clicks[*it].clicks) / (stats.overall_stats.domain_stats[domain_id].prev_query_clicks[*it].shows + 1));
                }
            }

            for (auto it = clicked_domains.begin(); it != clicked_domains.end(); ++it) {
                if (stats.overall_stats.domain_stats[domain_id].coclicks.find(*it) != stats.overall_stats.domain_stats[domain_id].coclicks.end()) {
                    feats.max_d_prevd_ctr = std::max(feats.max_d_prevd_ctr, 1.0 * (stats.overall_stats.domain_stats[domain_id].coclicks[*it].clicks) / (stats.overall_stats.domain_stats[domain_id].coclicks[*it].shows + 1));
                }
            }
        }

        if ((feats.have_user_history = stats.user_stats.find(session.user_id) != stats.user_stats.end())) {
            if (stats.user_stats[session.user_id].url_stats.find(url_id) != stats.user_stats[session.user_id].url_stats.end()) {
                ClickStats& cur_stats = stats.user_stats[session.user_id].url_stats[url_id].overall;

                feats.u_shows_user = cur_stats.shows;
                feats.u_clicks_user = cur_stats.clicks;
                feats.u_ctr_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                feats.u_ctr2_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                feats.u_satctr_user = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
                feats.u_ssatctr_user = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
                feats.u_avedwell_user = 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);
                feats.u_avepos_user = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
                feats.u_lastinq_clicks_user = cur_stats.last_in_query;
                feats.u_lastinq_ctr_user = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
                feats.u_lastinsession_clicks_user = cur_stats.last_in_session;
                feats.u_lastinsession_ctr_user = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);

                if (stats.user_stats[session.user_id].url_stats[url_id].query_clicks.find(test_query.id) != stats.user_stats[session.user_id].url_stats[url_id].query_clicks.end()) {
                    cur_stats = stats.user_stats[session.user_id].url_stats[url_id].query_clicks[test_query.id];

                    feats.qu_shows_user = cur_stats.shows;
                    feats.qu_clicks_user = cur_stats.clicks;
                    feats.qu_ctr_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                    feats.qu_ctr2_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                    feats.qu_satctr_user = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
                    feats.qu_ssatctr_user = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
                    feats.qu_avedwell_user = 1.0 * cur_stats.dwell / (cur_stats.clicks + 1);
                    feats.qu_avepos_user = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
                    
                    feats.qu_lastinq_clicks_user = cur_stats.last_in_query;
                    feats.qu_lastinq_ctr_user = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
                    feats.qu_lastinsession_clicks_user = cur_stats.last_in_session;
                    feats.qu_lastinsession_ctr_user = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);
                }


                feats.tu_shows_user = 0;
                feats.tu_clicks_user = 0;
                feats.tu_ctr_user = 0;
                feats.tu_ctr2_user = 0;
                feats.tu_avedwell_user = 0;
                for (int k = 0; k < test_query.terms.size(); ++k) {
                    if (stats.user_stats[session.user_id].url_stats[url_id].term_clicks.find(test_query.terms[k]) != stats.user_stats[session.user_id].url_stats[url_id].term_clicks.end()) {
                        cur_stats = stats.user_stats[session.user_id].url_stats[url_id].term_clicks[test_query.terms[k]];

                        feats.tu_shows_user += cur_stats.shows;
                        feats.tu_clicks_user += cur_stats.clicks;
                        feats.tu_ctr_user += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                        feats.tu_ctr2_user += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                        feats.tu_avedwell_user += 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);;
                    }
                }
                feats.tu_shows_user /= test_query.terms.size();
                feats.tu_clicks_user /= test_query.terms.size();
                feats.tu_ctr_user /= test_query.terms.size();
                feats.tu_ctr2_user /= test_query.terms.size();
                feats.tu_avedwell_user /= test_query.terms.size();

                feats.prevtu_ctr_user = 0;
                // Previous terms
                for (auto it = seen_terms.begin(); it != seen_terms.end(); ++it) {
                    if (stats.user_stats[session.user_id].url_stats[url_id].term_clicks.find(*it) != stats.user_stats[session.user_id].url_stats[url_id].term_clicks.end()) {
                        feats.prevtu_ctr_user = std::max(feats.prevtu_ctr_user, 1.0 * (stats.user_stats[session.user_id].url_stats[url_id].term_clicks[*it].clicks) / (stats.user_stats[session.user_id].url_stats[url_id].term_clicks[*it].shows + 100));
                    }
                }

                feats.max_u_prevq_ctr_user = 0;
                feats.max_u_prevu_ctr_user = 0;
                for (auto it = seen_queries.begin(); it != seen_queries.end(); ++it) {
                    if (stats.user_stats[session.user_id].url_stats[url_id].prev_query_clicks.find(*it) != stats.user_stats[session.user_id].url_stats[url_id].prev_query_clicks.end()) {
                        feats.max_u_prevq_ctr_user = std::max(feats.max_u_prevq_ctr_user, 1.0 * (stats.user_stats[session.user_id].url_stats[url_id].prev_query_clicks[*it].clicks) / (stats.user_stats[session.user_id].url_stats[url_id].prev_query_clicks[*it].shows + 1));
                    }
                }

                for (auto it = clicked_urls.begin(); it != clicked_urls.end(); ++it) {
                    if (stats.user_stats[session.user_id].url_stats[url_id].coclicks.find(*it) != stats.user_stats[session.user_id].url_stats[url_id].coclicks.end()) {
                        feats.max_u_prevu_ctr_user = std::max(feats.max_u_prevu_ctr_user, 1.0 * (stats.user_stats[session.user_id].url_stats[url_id].coclicks[*it].clicks) / (stats.user_stats[session.user_id].url_stats[url_id].coclicks[*it].shows + 1));
                    }
                }

            }

            // Domains
            if (stats.user_stats[session.user_id].domain_stats.find(domain_id) != stats.user_stats[session.user_id].domain_stats.end()) {
                ClickStats cur_stats = stats.user_stats[session.user_id].domain_stats[domain_id].overall;

                feats.d_shows_user = cur_stats.shows;
                feats.d_clicks_user = cur_stats.clicks;
                feats.d_ctr_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                feats.d_ctr2_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                feats.d_satctr_user = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
                feats.d_ssatctr_user = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
                feats.d_avedwell_user = 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);
                feats.d_avepos_user = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
                feats.d_lastinq_clicks_user = cur_stats.last_in_query;
                feats.d_lastinq_ctr_user = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
                feats.d_lastinsession_clicks_user = cur_stats.last_in_session;
                feats.d_lastinsession_ctr_user = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);

                if (stats.user_stats[session.user_id].domain_stats[domain_id].query_clicks.find(test_query.id) != stats.user_stats[session.user_id].domain_stats[domain_id].query_clicks.end()) {
                    cur_stats = stats.user_stats[session.user_id].domain_stats[domain_id].query_clicks[test_query.id];

                    feats.qd_shows_user = cur_stats.shows;
                    feats.qd_clicks_user = cur_stats.clicks;
                    feats.qd_ctr_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                    feats.qd_ctr2_user = 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                    feats.qd_satctr_user = 1.0 * (cur_stats.sat_clicks + 1) / (cur_stats.shows + 100);
                    feats.qd_ssatctr_user = 1.0 * (cur_stats.ssat_clicks + 1) / (cur_stats.shows + 100);
                    feats.qd_avedwell_user = 1.0 * cur_stats.dwell / (cur_stats.clicks + 1);
                    feats.qd_avepos_user = 1.0 * cur_stats.pos / (cur_stats.shows + 1);
                    
                    feats.qd_lastinq_clicks_user = cur_stats.last_in_query;
                    feats.qd_lastinq_ctr_user = 1.0 * cur_stats.last_in_query / (cur_stats.clicks + 1);
                    feats.qd_lastinsession_clicks_user = cur_stats.last_in_session;
                    feats.qd_lastinsession_ctr_user = 1.0 * cur_stats.last_in_session / (cur_stats.clicks + 1);
                }

                feats.td_shows_user = 0;
                feats.td_clicks_user = 0;
                feats.td_ctr_user = 0;
                feats.td_ctr2_user = 0;
                feats.td_avedwell_user = 0;
                for (int k = 0; k < test_query.terms.size(); ++k) {
                    if (stats.user_stats[session.user_id].domain_stats[domain_id].term_clicks.find(test_query.terms[k]) != stats.user_stats[session.user_id].domain_stats[domain_id].term_clicks.end()) {
                        cur_stats = stats.user_stats[session.user_id].domain_stats[domain_id].term_clicks[test_query.terms[k]];

                        feats.td_shows_user += cur_stats.shows;
                        feats.td_clicks_user += cur_stats.clicks;
                        feats.td_ctr_user += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 100);
                        feats.td_ctr2_user += 1.0 * (cur_stats.clicks + 1) / (cur_stats.shows + 10);
                        feats.td_avedwell_user += 1.0 * (cur_stats.dwell) / (cur_stats.clicks + 1);;
                    }
                }
                feats.td_shows_user /= test_query.terms.size();
                feats.td_clicks_user /= test_query.terms.size();
                feats.td_ctr_user /= test_query.terms.size();
                feats.td_ctr2_user /= test_query.terms.size();
                feats.td_avedwell_user /= test_query.terms.size();

                feats.prevtd_ctr_user = 0;
                // Previous terms
                for (auto it = seen_terms.begin(); it != seen_terms.end(); ++it) {
                    if (stats.user_stats[session.user_id].domain_stats[domain_id].term_clicks.find(*it) != stats.user_stats[session.user_id].domain_stats[domain_id].term_clicks.end()) {
                        feats.prevtd_ctr_user = std::max(feats.prevtd_ctr_user, 1.0 * (stats.user_stats[session.user_id].domain_stats[domain_id].term_clicks[*it].clicks) / (stats.user_stats[session.user_id].domain_stats[domain_id].term_clicks[*it].shows + 100));
                    }
                }

                feats.max_d_prevq_ctr_user = 0;
                feats.max_d_prevd_ctr_user = 0;
                for (auto it = seen_queries.begin(); it != seen_queries.end(); ++it) {
                    if (stats.user_stats[session.user_id].domain_stats[domain_id].prev_query_clicks.find(*it) != stats.user_stats[session.user_id].domain_stats[domain_id].prev_query_clicks.end()) {
                        feats.max_d_prevq_ctr_user = std::max(feats.max_d_prevq_ctr_user, 1.0 * (stats.user_stats[session.user_id].domain_stats[domain_id].prev_query_clicks[*it].clicks) / (stats.user_stats[session.user_id].domain_stats[domain_id].prev_query_clicks[*it].shows + 1));
                    }
                }

                for (auto it = clicked_domains.begin(); it != clicked_domains.end(); ++it) {
                    if (stats.user_stats[session.user_id].domain_stats[domain_id].coclicks.find(*it) != stats.user_stats[session.user_id].domain_stats[domain_id].coclicks.end()) {
                        feats.max_d_prevd_ctr_user = std::max(feats.max_d_prevd_ctr_user, 1.0 * (stats.user_stats[session.user_id].domain_stats[domain_id].coclicks[*it].clicks) / (stats.user_stats[session.user_id].domain_stats[domain_id].coclicks[*it].shows + 1));
                    }
                }
            }
        }

        feats.query_asked = seen_queries.find(test_query.id) != seen_queries.end();
        feats.url_shown = seen_urls.find(url_id) != seen_urls.end();
        feats.url_clicked = clicked_urls.find(url_id) != clicked_urls.end();
        feats.cur_total_dwell = dwell;
        feats.time_passed = test_query.time_passed;
        feats.cur_queries = test_query_index;
        feats.cur_clicks = clicks;
        feats.cur_pos = i;

        int target = 0;
        for (int j = 0; j < test_query.clicks.size(); ++j) {
            if (test_query.clicks[j].url_id == url_id) {
                if (test_query.clicks[j].dwell >= 50)
                    target = 1;
                if (test_query.clicks[j].dwell >= 400)
                    target = 2;
                if (j == test_query.clicks.size() - 1 && test_query_index == session.queries.size() - 1)
                    target = 2;
            }
        }

        out << target << "," << session.id << "," << url_id << "," << feats.toString() << "\n";
    }
}
