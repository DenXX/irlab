
#include "click_graph.h"

void ClickGraph::processSession(const Session& session) {
    int last_q_id = -1;
    for (int i = 0; i < session.queries.size(); ++i) {
        const Query& q = session.queries[i];
        // Terms
        for (int j = 0; j < q.terms.size(); ++j) {
            addTQEdge(q.terms[j], q.id);
        }

        // Clicks
        for (int j = 0; j < q.clicks.size(); ++j) {
            addQUEdge(q.id, q.clicks[j].url_id);
        }

        if (last_q_id != -1)
            addQQEdge(last_q_id, q.id);
        last_q_id = q.id;
    }
}

void ClickGraph::getRandomWalkScore1(
    const std::vector<int>& qids,
    const std::vector<std::vector<int> >& tids,
    const std::vector<std::vector<int> >& uids,
    const std::vector<std::pair<int, int> >& test_urls,
    int type,
    std::unordered_map<int, double>& scores) {

    

    for (int i = 0; i < test_urls.size(); ++i) {
        if (uid2pos.find(test_urls[i].first) == uid2pos.end() || 
            qid2pos.find(qids.back()) == qid2pos.end()) {
            scores[test_urls[i].first] = 0;
            continue;
        }

        std::unordered_map<int, double> state;
        state[uid2pos[test_urls[i].first]] = 1.0;

        runRandomWalk(state, 0.9, 10);
        scores[test_urls[i].first] = state[qid2pos[qids.back()]];
    }
}
