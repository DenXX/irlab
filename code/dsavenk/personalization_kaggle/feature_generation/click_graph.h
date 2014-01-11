#pragma once

#include <vector>
#include <unordered_map>
#include <unordered_map>
#include <sstream>
#include <ostream>
#include <iostream>

#include "session.h"

struct ClickGraphNode {
    int id;
    char type;
    std::unordered_map<int, int> query_edges;
    std::unordered_map<int, int> url_edges;
    int weight_sum;

    ClickGraphNode() :
        id(0),
        type(0),
        weight_sum(0) {}
};

class ClickGraph {
    std::vector<ClickGraphNode> nodes;

    std::unordered_map<int, int> tid2pos;
    std::unordered_map<int, int> qid2pos;
    std::unordered_map<int, int> uid2pos;

private:
    void _addNode(std::unordered_map<int, int>& nodeid2pos, int node_id, char type) {
        if (nodeid2pos.find(node_id) == nodeid2pos.end()) {
            nodes.push_back(ClickGraphNode());
            nodeid2pos[node_id] = nodes.size() - 1;
            nodes.back().id = node_id;
            nodes.back().type = type;
        }
    }

public:
    void addTQEdge(int term_id, int qid) {
        return;
        _addNode(tid2pos, term_id, 0);
        _addNode(qid2pos, qid, 1);
        ++nodes[tid2pos[term_id]].query_edges[qid];
        ++nodes[tid2pos[term_id]].weight_sum;
    }

    void addQQEdge(int qid1, int qid2) {
        return;
        _addNode(qid2pos, qid1, 1);
        _addNode(qid2pos, qid2, 1);
        ++nodes[qid2pos[qid1]].query_edges[qid2];
        ++nodes[qid2pos[qid1]].weight_sum;
        ++nodes[qid2pos[qid2]].query_edges[qid1];
        ++nodes[qid2pos[qid2]].weight_sum;
    }

    void addQUEdge(int qid, int uid) {
        _addNode(qid2pos, qid, 1);
        _addNode(uid2pos, uid, 2);
        ++nodes[qid2pos[qid]].url_edges[uid];
        ++nodes[qid2pos[qid]].weight_sum;
        ++nodes[uid2pos[uid]].query_edges[qid];
        ++nodes[uid2pos[uid]].weight_sum;
    }

    void processSession(const Session& session);

    void writeFeaturesHeader(std::ostream& out) {
        out << "target,sessionid,urlid,rwback,rwforward,rwtermsforw,rwsession" << std::endl;
    }

    void writeFeatures(std::ostream& out, const Session& session) {
        std::vector<int> qids;
        std::vector<std::vector<int> > tids;
        std::vector<std::vector<int> > uids;

        std::vector<std::pair<int, int> > test_urls;

        for (int i = 0; i < session.queries.size(); ++i) {
            const Query& q = session.queries[i];
            qids.push_back(q.id);
            tids.push_back(std::vector<int>());
            uids.push_back(std::vector<int>());
            for (int j = 0; j < q.terms.size(); ++j)
                tids.back().push_back(q.terms[j]);
            for (int j = 0; j < q.clicks.size(); ++j)
                uids.back().push_back(q.clicks[j].url_id);

            if (q.is_test) {
                std::unordered_map<int, int> targets;
                for (int j = 0; j < q.clicks.size(); ++j) {
                    int target = 0;
                    if (q.clicks[j].dwell >= 50)
                        target = 1;
                    if (q.clicks[j].dwell >= 400)
                        target = 2;
                    if (targets.find(q.clicks[j].url_id) != targets.end())
                        targets[q.clicks[j].url_id] = std::max(target, targets[q.clicks[j].url_id]);
                    else
                        targets[q.clicks[j].url_id] = target;
                }
                    
                for (int j = 0; j < q.docs.size(); ++j) {
                    int url_id = q.docs[j].first;
                    if (targets.find(url_id) != targets.end())
                        test_urls.push_back(std::make_pair(url_id, targets[url_id]));
                    else
                        test_urls.push_back(std::make_pair(url_id, 0));
                }
                break;
            }
        }
        std::unordered_map<int, double> scores1;
        getRandomWalkScore1(qids, tids, uids, test_urls, 1, scores1);
        std::unordered_map<int, double> scores2;
        getRandomWalkScore2(qids, tids, uids, test_urls, 1, scores2);
        std::unordered_map<int, double> scores5;
        //getRandomWalkScore5(qids, tids, uids, test_urls, 1, scores5);
        std::unordered_map<int, double> scores4;
        getRandomWalkScore4(qids, tids, uids, test_urls, 1, scores4);
 
        for (int i = 0; i < test_urls.size(); ++i) {
            out << test_urls[i].second << "," << session.id << "," << test_urls[i].first << "," 
            << scores1[test_urls[i].first] << ","
            << scores2[test_urls[i].first] << ","
            << scores5[test_urls[i].first] << ","
            << scores4[test_urls[i].first]
            << std::endl;
        }
    }

    void runRandomWalk(std::unordered_map<int, double>& state, double selfp, int steps) {
        for (int step = 0; step < steps; ++step) {
            std::unordered_map<int, double> newstate;
            // for all nodes
            double sum = 0;
            // std::cerr << state.size() << std::endl;
            for (auto it = state.begin(); it != state.end(); ) {
                if (it->second < 0.000001) { it = state.erase(it); continue;}
                ClickGraphNode& node = nodes[it->first];
                for (auto itt = node.query_edges.begin(); itt != node.query_edges.end(); ++itt) {
                    int pos = qid2pos[itt->first];
                    newstate[pos] += it->second * (1 - selfp) * itt->second / node.weight_sum;
                    sum += it->second * (1 - selfp) * itt->second / node.weight_sum;
                }
                for (auto itt = node.url_edges.begin(); itt != node.url_edges.end(); ++itt) {
                    int pos = uid2pos[itt->first];
                    newstate[pos] += it->second * (1 - selfp) * itt->second / node.weight_sum;
                    sum += it->second * (1 - selfp) * itt->second / node.weight_sum;
                }

                // Self transition
                newstate[it->first] += it->second * selfp;
                sum += it->second * selfp;
                ++it;
            }

            for (auto it = newstate.begin(); it != newstate.end(); ++it) {
                it->second /= sum;
            }
            // Normalize
            state = std::move(newstate);
        }
    }

    void getRandomWalkScore1(
        const std::vector<int>& qids,
        const std::vector<std::vector<int> >& tids,
        const std::vector<std::vector<int> >& uids,
        const std::vector<std::pair<int, int> >& test_urls,
        int type,
        std::unordered_map<int, double>& scores);


        void getRandomWalkScore2(
        const std::vector<int>& qids,
        const std::vector<std::vector<int> >& tids,
        const std::vector<std::vector<int> >& uids,
        const std::vector<std::pair<int, int> >& test_urls,
        int type,
        std::unordered_map<int, double>& scores) {

        std::unordered_map<int, double> state;
        if (qid2pos.find(qids.back()) == qid2pos.end())
            return;
        state[qid2pos[qids.back()]] = 1.0;
        runRandomWalk(state, 0.9, 5);

        for (int i = 0; i < test_urls.size(); ++i) {
            if (uid2pos.find(test_urls[i].first) == uid2pos.end()) {
                scores[test_urls[i].first] = 0;
                continue;
            }
            scores[test_urls[i].first] = state[test_urls[i].first];
        }
    }

    void getRandomWalkScore5(
        const std::vector<int>& qids,
        const std::vector<std::vector<int> >& tids,
        const std::vector<std::vector<int> >& uids,
        const std::vector<std::pair<int, int> >& test_urls,
        int type,
        std::unordered_map<int, double>& scores) {

        std::unordered_map<int, double> state;
        if (qid2pos.find(qids.back()) == qid2pos.end())
            return;
        bool found = false;
        for (int i = 0; i < tids.back().size(); ++i)
            if (tid2pos.find(tids.back()[i]) != tid2pos.end()) {
                state[tid2pos[tids.back()[i]]] = 1.0;
                found = true;
            }
        if (!found) return;
        runRandomWalk(state, 0.9, 5);

        for (int i = 0; i < test_urls.size(); ++i) {
            if (uid2pos.find(test_urls[i].first) == uid2pos.end()) {
                scores[test_urls[i].first] = 0;
                continue;
            }
            scores[test_urls[i].first] = state[test_urls[i].first];
        }
    }

     void getRandomWalkScore4(
        const std::vector<int>& qids,
        const std::vector<std::vector<int> >& tids,
        const std::vector<std::vector<int> >& uids,
        const std::vector<std::pair<int, int> >& test_urls,
        int type,
        std::unordered_map<int, double>& scores) {

        std::unordered_map<int, double> state;
        if (qid2pos.find(qids.back()) == qid2pos.end())
            return;
        bool found = false;
        double weight = 1.0;
        for (int j = tids.size() - 1; j >= 0; --j) {
            for (int i = 0; i < tids[j].size(); ++i) {
                if (tid2pos.find(tids[j][i]) != tid2pos.end()) {
                    state[tid2pos[tids[j][i]]] = weight;
                    found = true;
                }
            }
            weight *= 0.9;
        }
        weight = 1.0;
         for (int j = uids.size() - 1; j >= 0; --j) {
            for (int i = 0; i < uids[j].size(); ++i) {
                if (uid2pos.find(uids[j][i]) != uid2pos.end()) {
                    state[uid2pos[uids[j][i]]] = weight;
                    found = true;
                }
            }
            weight *= 0.9;
        }        
        weight = 1.0;
         for (int j = qids.size() - 1; j >= 0; --j) {
                if (qid2pos.find(qids[j]) != qid2pos.end()) {
                    state[qid2pos[qids[j]]] = weight;
                    found = true;
                }
            weight *= 0.9;
        }        


        if (!found) return;
        runRandomWalk(state, 0.9, 5);

        for (int i = 0; i < test_urls.size(); ++i) {
            if (uid2pos.find(test_urls[i].first) == uid2pos.end()) {
                scores[test_urls[i].first] = 0;
                continue;
            }
            scores[test_urls[i].first] = state[test_urls[i].first];
        }
    }

  

};
