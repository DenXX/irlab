
#include <map>
#include <set>
#include <string>
#include <fstream>
#include <iostream>
#include <algorithm>
#include <indri/CompressedCollection.hpp>
#include <indri/Repository.hpp>

using namespace std;

void usage() {
	cout << "Usage: ./TermById <index_path>" << endl;
}

int main(int argc, char* argv[]) {
	if (argc != 2) {
		usage();
		return 0;
	}

	char* indexPath = argv[1];

	// Open repository
	indri::collection::Repository rep;
	rep.openRead(indexPath);
	indri::index::Index* index = (*rep.indexes())[0];

	lemur::api::TERMID_T termid;
	while (cin.good()) {
		cin >> termid;
		cout << index->term(termid) << endl;
	}

	rep.close();

	return 0;
}
