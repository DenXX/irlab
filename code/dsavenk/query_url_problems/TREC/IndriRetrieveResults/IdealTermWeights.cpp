
#include <math.h>
#include <map>
#include <set>
#include <string>
#include <fstream>
#include <iostream>
#include <algorithm>
#include <indri/CompressedCollection.hpp>
#include <indri/Repository.hpp>

using namespace std;

map<string, int> readQrels(char* qrelsFilePath, int topic) {
	map<string, int> res;
	ifstream inp(qrelsFilePath);
	int curtopic;
	int tmp;
	string docid;
	int isRel;
	while(inp.good()) {
		inp >> curtopic >> tmp >> docid >> isRel;
		if (curtopic == topic)
			res[docid] = isRel;
	}
	inp.close();
	return res;
}

void usage() {
	cout << "Usage: ./IdealTermWeights <index_path> <qrel_file> <topic> <allIrrel>" << endl;
}

int main(int argc, char* argv[]) {
	if (argc != 5) {
		usage();
		return 0;
	}

	char* indexPath = argv[1];
	char* qrelsFilePath = argv[2];
	int topic = atoi(argv[3]);
	bool allIrrel = strcmp(argv[4], "1") == 0;

	map<string, int> qrels = readQrels(qrelsFilePath, topic);

	// Open repository
	indri::collection::Repository rep;
	rep.openRead(indexPath);
	indri::index::Index* index = (*rep.indexes())[0];
	indri::collection::CompressedCollection* collection = rep.collection();

	map<lemur::api::TERMID_T, long long> relTerms;
	long long relCount = 0;
	map<lemur::api::TERMID_T, long long> irrelTerms;
	long long irrelCount = 0;

	for (lemur::api::DOCID_T docid = 1; docid < index->documentMaximum(); ++docid) {
		string docno = collection->retrieveMetadatum(docid, "docno");
		bool rel;
		bool count = false;
		if (qrels.find(docno) != qrels.end()) {
			rel = (qrels[docno] > 0);
			count = true;
		}
		else if (allIrrel)
		{ 
			count = true;
			rel = false;
		}

		if (count) {
			indri::utility::greedy_vector<lemur::api::TERMID_T> terms = index->termList(docid)->terms();

			for (int i = 0; i < terms.size(); ++i) 
				if (rel) {
					++relTerms[terms[i]];
					++relCount;
				}
				else {
					++irrelTerms[terms[i]];
					++irrelCount;
				}
		}
	}

	vector< pair<double, lemur::api::TERMID_T> > termWeights;
	for (map<lemur::api::TERMID_T, long long>::const_iterator it = relTerms.begin(); it != relTerms.end(); ++it) {
		long long irr = (irrelTerms.find(it->first) != irrelTerms.end()) ? irrelTerms[it->first] : 0;
		double weight = log((it->second + 1.0) / (relCount + 10.0)) - log((irr + 1.0) / (irrelCount + 100.0));
		termWeights.push_back(make_pair(weight, it->first));
	}
	sort(termWeights.rbegin(), termWeights.rend());

	for (int i = 0; i < termWeights.size(); ++i)
		cout << index->term(termWeights[i].second) << " " << termWeights[i].first << endl;

	rep.close();

	return 0;
}
