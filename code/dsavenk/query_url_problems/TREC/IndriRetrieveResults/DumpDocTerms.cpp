
#include <map>
#include <set>
#include <string>
#include <fstream>
#include <iostream>
#include <algorithm>
#include <indri/CompressedCollection.hpp>
#include <indri/Repository.hpp>

using namespace std;

map<int, pair<string, int> > readQrels(char* qrelsFilePath) {
	map<int, pair<string, int> > res;
	ifstream inp(qrelsFilePath);
	int topic;
	int tmp;
	string docid;
	int isRel;
	while(inp.good()) {
		inp >> topic >> tmp >> docid >> isRel;
		res[topic] = make_pair(docid, isRel);
	}
	inp.close();
	return res;
}

void usage() {
	cout << "Usage: ./DumpDocTerms <index_path>" << endl;
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
	indri::collection::CompressedCollection* collection = rep.collection();

	for (lemur::api::DOCID_T docid = 1; docid < index->documentMaximum(); ++docid) {
		indri::utility::greedy_vector<lemur::api::TERMID_T> terms = index->termList(docid)->terms();
		map<lemur::api::TERMID_T, int> termCount;
		for (int i = 0; i < terms.size(); ++i) 
			++termCount[terms[i]];
		cout << docid;
		for (map<lemur::api::TERMID_T, int>::const_iterator it = termCount.begin(); it != termCount.end(); ++it)
			cout << " " << it->first << ":" << it->second;
		cout << endl;
	}

	rep.close();

	return 0;
}
