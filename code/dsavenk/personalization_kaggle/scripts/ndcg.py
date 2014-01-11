
from sys import argv
from csv import reader
from math import log

def calc_ndcg(feats_file, predictions):
    rel = {}
    with open(feats_file, 'r') as input:
        csv = reader(input)
        next(csv, None)
        for line in csv:
            target = int(line[0])
            session_id = int(line[1])
            url_id = int(line[2])
            if session_id not in rel:
                rel[session_id] = {}
            rel[session_id][url_id] = target

    res = 0
    count = 0
    with open(predictions, 'r') as input:
        csv = reader(input)
        next(csv, None)
        last_sid = -1
        urls = []
        for line in csv:
            cur_sid = int(line[0])
            if cur_sid != last_sid and len(urls) > 0:
                rels = sorted(rel[last_sid].values(), reverse = True)
                idcg = 0.0
                dcg = 0.0
                for i in xrange(len(urls)):
                    dcg += 1.0 * ((1 << rel[last_sid][urls[i]]) - 1) / log(i + 2, 2)
                    idcg += 1.0 * ((1 << rels[i]) - 1) / log(i + 2, 2)
                res += (dcg / idcg) if idcg > 0 else 1
                count += 1
                urls = []
            last_sid = cur_sid
            urls.append(int(line[1]))
        if len(urls) > 0:
            rels = sorted(rel[last_sid].values(), reverse = True)
            idcg = 0
            dcg = 0
            for i in xrange(len(urls)):
                dcg += ((1 << rel[last_sid][urls[i]]) - 1) / log(i + 2, 2)
                idcg += ((1 << rels[i]) - 1) / log(i + 2, 2)
            res += (dcg / idcg) if idcg > 0 else 1
            count += 1

    return res / count
            


if __name__ == "__main__":
    feats_file = argv[1]
    predictions = argv[2]
    print calc_ndcg(feats_file, predictions)