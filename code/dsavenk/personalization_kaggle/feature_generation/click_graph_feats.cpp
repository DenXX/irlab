
#include <fstream>
#include <iostream>

#include "click_graph.h"
#include "session.h"

int main(int argc, char* argv[]) {
    ClickGraph graph;

    std::ifstream stats_file(argv[1]);
    int count = 0;
    while (true) {
        Session session;
        if (count % 10000 == 0) std::cout << "Building graph: " << count << std::endl;
        if (!Session::readSession(stats_file, session, false)) break;
        graph.processSession(session);
        ++count;
    }

    std::ifstream train_file(argv[2]);
    std::ofstream feats_file(argv[3]);
    graph.writeFeaturesHeader(feats_file);
    count = 0;
    while (true) {
        Session session;
        if (count % 10000 == 0) std::cout << "Generating feats: " << count << std::endl;
        if (!Session::readSession(train_file, session, false)) break;
        graph.writeFeatures(feats_file, session);
        ++count;
    }

}
