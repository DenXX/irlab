
#include <map>
#include <set>
#include <string>
#include <fstream>
#include <iostream>
#include <algorithm>
#include <indri/CompressedCollection.hpp>
#include <indri/Repository.hpp>

using namespace std;

vector<string> getRelevantDocs(char* qrelsFilePath, int topic) {
	vector<string> relevant;

	ifstream inp;
	inp.open(qrelsFilePath, ifstream::in);
	int curTopic;
	int isRel;
	string docid;
	int tmp;
	while (inp.good()) {
		inp >> curTopic >> tmp >> docid >> isRel;
		if (curTopic == topic && isRel > 0)
			relevant.push_back(docid);
	}
	inp.close();

	return relevant;
}

map<int, pair<string, int> > readQrels(char* qrelsFilePath) {
	
}

void usage() {
	cout << "Usage: ./IntersectDocuments <index_path> <qrel_file> <topic_num>" << endl;
}

int main(int argc, char* argv[]) {
	if (argc != 4) {
		usage();
		return 0;
	}

	char* indexPath = argv[1];
	char* qrelFilePath = argv[2];
	int topic = atoi(argv[3]);

	vector<string> relevantDocuments = getRelevantDocs(qrelFilePath, topic);

	set<lemur::api::TERMID_T> termsSet;

	// Open repository
	indri::collection::Repository rep;
	rep.openRead(indexPath);

	indri::index::Index* index = (*rep.indexes())[0];
	indri::collection::CompressedCollection* collection = rep.collection();

	for (int i = 0; i < relevantDocuments.size(); ++i) {
		lemur::api::DOCID_T docid = collection->retrieveIDByMetadatum("docno", relevantDocuments[i])[0];
		cout << "doc" << docid << endl;
		indri::utility::greedy_vector<lemur::api::TERMID_T> terms = index->termList(docid)->terms();
		set<int> curDoc(terms.begin(), terms.end());
		if (i == 0) {
			for (set<int>::iterator it = curDoc.begin(); it != curDoc.end(); ++it) {
				termsSet.insert(*it);
			}
		}
		else {
			for (set<int>::iterator it = termsSet.begin(); it != termsSet.end();) {
				if (curDoc.find(*it) == curDoc.end()) termsSet.erase(it++);
				else ++it;
			}
		}
		cout << "! " << termsSet.size() << endl;
		if (i == 1) break;
	}

	cout << "Total:" << termsSet.size() << endl;
	for (set<int>::iterator it = termsSet.begin(); it != termsSet.end(); ++it) {
		cout << index->term(*it) << endl;
	}

	rep.close();

	return 0;
}
