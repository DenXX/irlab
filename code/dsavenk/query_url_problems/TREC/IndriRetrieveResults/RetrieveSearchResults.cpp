#include <ctype.h>
#include <string>
#include <indri/QueryEnvironment.hpp>
#include <lemur/Exception.hpp>

using namespace indri::api;
using namespace lemur::api;

string preprocessQuery(string& query) {
    string res = "";
    for (int i = 0; i < query.length(); ++i)
        if (query[i] == ' ' || isalnum(query[i]))
        res += query[i];
    return res;
}

int main( int argc, char** argv ) {
    QueryEnvironment env;

    indri::api::Parameters& param = indri::api::Parameters::instance();

    string paramxml = "";
    getline(cin, paramxml);
    param.load(paramxml);

    // open an Indri repository
    env.addIndex((string)param["index"]);

    vector<string> stopwords;
    for (int i = 0; i < param["stopwords"].size(); ++i) {
        stopwords.push_back((string)param["stopwords"][i]);
    }
    env.setStopwords(stopwords);

    std::vector<std::string> names;

    int count;
    std::string myQuery;
    bool retrievePassages = false;

    cin >> count >> retrievePassages;
    getline(cin, myQuery);
    //myQuery = preprocessQuery(myQuery);
    
    if (retrievePassages) {
        QueryRequest qr;
        qr.query = myQuery;
        qr.resultsRequested = count;
        qr.options = QueryRequest::HTMLSnippet;
        qr.metadata.push_back("docno");

        QueryResults results = env.runQuery(qr);

        // print the results, including document score,
        // first and last word position, and document name
        for( int i=0; i<results.results.size(); i++ ) {
            std::cout << results.results[i].docid << "\t"
                    << results.results[i].metadata[0].value << "\t"
                    << results.results[i].snippet
                    << "\n\n";
        }
    }
    else {
        std::vector<ScoredExtentResult> results = env.runQuery(myQuery, count);
        std::vector<string> docids = env.documentMetadata(results, "docno");
        for( int i=0; i < docids.size(); i++ ) {
            std::cout << docids[i]
                    << "\n\n";
        }        
    }

    env.close();
    return 0;
}
