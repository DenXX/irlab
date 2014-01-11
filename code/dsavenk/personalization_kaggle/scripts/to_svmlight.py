
import csv
from sys import argv

if __name__ == "__main__":
    with open(argv[1], 'r') as input:
        csv = csv.reader(input)
        next(csv, None)
        for line in csv:
            features = map(str, line[3:])
            features = [str(index + 1) + ":" + features[index] for index in xrange(len(features))]
            print " ".join(map(str, [line[0], ] + features))
