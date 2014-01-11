#include <iostream>
#include <fstream>
#include <iostream>
#include <sstream>
#include <limits>
#include <set>
#include <assert.h>
#include <vector>
#include <algorithm>
#include <time.h>

#include "stats_collector.h"
#include "session.h"
#include "feature_generator.h"

int main(int argc, char* argv[]) {
    if (argc < 2) {
        std::cout << "Too few parameters" << std::endl;
    }

    StatsCollector stats;

    std::ifstream test_file(argv[2]);
    int count = 0;
    while (true) {
        Session session;
        if (count % 10000 == 0) std::cout << "Filter: " << count << std::endl;
        if (!Session::readSession(test_file, session, false)) break;
        stats.buildFilter(session);
        ++count;
    }

    std::ifstream train_file(argv[1]);
    count = 0;
    while (true) {
        if (count % 10000 == 0) std::cout << "Stats: " << count << std::endl;
        Session session;
        if (!Session::readSession(train_file, session, false)) break;
        stats.processSession(session);
        ++count;
    }
    std::cout << "Finished stats: " << count << std::endl;

    test_file.clear();
    test_file.seekg(0, test_file.beg);
    std::ofstream feats_file(argv[3]);
    FeatureGenerator::writeFeatureHeader(feats_file);
    count = 0;
    while (true) {
        if (count % 10000 == 0) std::cout << "Features: " << count << std::endl;
        Session session;
        if (!Session::readSession(test_file, session, false)) break;
        FeatureGenerator::writeFeatures(feats_file, session, stats);
        ++count;
    }
    std::cout << "Finished feats: " << count << std::endl;    
}