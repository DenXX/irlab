
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
	cout << "Usage: ./GetDocText <index_path> <doc_id>" << endl;
}

int main(int argc, char* argv[]) {
	if (argc != 3) {
		usage();
		return 0;
	}

	char* indexPath = argv[1];

	// Open repository
	indri::collection::Repository rep;
	rep.openRead(indexPath);
	indri::index::Index* index = (*rep.indexes())[0];
	indri::collection::CompressedCollection* collection = rep.collection();

	lemur::api::DOCID_T doc_id = atoi(argv[2]);
	cout << collection->retrieve(doc_id)->getContent();

	rep.close();

	return 0;
}
