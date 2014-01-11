
#include <fstream>
#include <unordered_set>
#include <vector>
#include <iostream>

#include "session.h"

int main(int argc, char* argv[]) {
    std::unordered_set<long long> train_users;

    int count = 0;
    // Open train file to get the list of users
    std::ifstream train_file(argv[1]);
    while (true) {
        Session session;
        if (count % 1000 == 0) std::cout << "Reading train: " << count << std::endl;
        if (!Session::readSession(train_file, session, true)) break;
        train_users.insert(session.user_id);
        ++count;
    }

    std::unordered_set<long long> test_users;
    count = 0;
    // Open train file to get the list of users
    std::ifstream test_file(argv[2]);
    std::ofstream output_file(argv[3]);

    bool skip = false;
    while (true) {
        skip = false;
        Session session;
        if (count % 1000 == 0) std::cout << "Reading test: " << count << std::endl;
        if (!Session::readSession(test_file, session, false)) break;
        if (test_users.find(session.user_id) == test_users.end()) {
            for (int i = session.queries.size() - 1; i >= 0 && !skip; --i) {
                for (int j = session.queries[i].clicks.size() - 1; j >= 0 && !skip; --j) {
                    if (session.queries[i].clicks[j].dwell == -1 
                        || session.queries[i].clicks[j].dwell > 50) {
                        session.queries[i].is_test = true;
                        session.writeTest(output_file);
                        skip = true;
                    }
                }
                // session.queries.erase(session.queries.begin() + i);
            }
        }
        test_users.insert(session.user_id);
        ++count;
    }
}
