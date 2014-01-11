
#include <unordered_map>

#include "session.h"

class StatsCollector2 {
    std::unordered_map<int, int> query_clicks;
    std::unordered_map<int, std::unordered_map<int, int> > query_url_clicks;
    std::unordered_map<int, std::unordered_map<int, int> > query_pos_clicks;
    std::unordered_map<int, int> user_clicks;
    std::unordered_map<int, std::unordered_map<int, int> > user_url_clicks;
    std::unordered_map<int, std::unordered_map<int, int> > user_pos_clicks;

public:
    void processSession(const Session& session) {
        for (int i = 0; i < session.queries.size(); ++i) {
            const Query& q = session.queries[i];

            for (int j = 0; j < q.clicks.size(); ++j) {
                ++query_url_clicks[q.id][q.clicks[j].url_id];
                ++query_clicks[q.id];
                ++user_url_clicks[session.user_id][q.clicks[j].url_id];
                ++user_clicks[session.user_id];

                int pos = q.urlid2pos.at(q.clicks[j].url_id);
                ++query_pos_clicks[q.id][pos];
                ++user_pos_clicks[session.user_id][pos];
            }
        }
    }

    void writeFeaturesHeader(std::ostream& out) {
        out << "target,sessionid,urlid,qurl_perc,qpos_perc,uurl_perc,upos_perc" << std::endl;
    }

    void writeFeatures(std::ostream& out, const Session& session) {
        for (int i = session.queries.size() - 1; i >= 0; --i) {
            const Query& q = session.queries[i];

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
                    out << targets[q.docs[j].first] << "," << session.id << "," 
                        << q.docs[j].first << "," 
                        << 1.0 * query_url_clicks[q.id][q.docs[j].first] / (query_clicks[q.id] + 1)
                        << ","
                        << 1.0 * query_pos_clicks[q.id][j] / (query_clicks[q.id] + 1) << ","
                        << 1.0 * user_url_clicks[session.user_id][q.docs[j].first] / (user_clicks[session.user_id] + 1) << ","
                        << 1.0 * user_pos_clicks[session.user_id][j] / (user_clicks[session.user_id] + 1) << "\n";
                }
                break;
            }
        }
    }
};