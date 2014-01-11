
import cPickle
from sys import argv
import csv

if __name__ == "__main__":
    header = []
    with open(argv[1], 'r') as input:
        reader = csv.reader(input)
        header = reader.next()

    model = cPickle.load(open(argv[2], 'r'))
    feats = []
    for index, v in enumerate(model.feature_importances_):
        feats.append((v, header[3 + index]))
    feats = sorted(feats, reverse = True)
    print "\n".join(map(str, feats))
