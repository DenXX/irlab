#include <fstream>
#include <iostream>

#include "session.h"
#include "stats_collector2.h"

int main(int argc, char* argv[]) {
    StatsCollector2 stats;

    std::ifstream stats_file(argv[1]);
    int count = 0;
    while (true) {
        Session session;
        if (count % 10000 == 0) std::cout << "Stats: " << count << std::endl;
        if (!Session::readSession(stats_file, session, false)) break;
        stats.processSession(session);
        ++count;
    }

    std::ifstream train_file(argv[2]);
    std::ofstream feats_file(argv[3]);
    stats.writeFeaturesHeader(feats_file);
    count = 0;
    while (true) {
        Session session;
        if (count % 10000 == 0) std::cout << "Generating feats: " << count << std::endl;
        if (!Session::readSession(train_file, session, false)) break;
        stats.writeFeatures(feats_file, session);
        ++count;
    }

}
