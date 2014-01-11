
from sklearn.ensemble import GradientBoostingClassifier, GradientBoostingRegressor
from sys import argv
import csv
import cPickle as pickle
from operator import itemgetter

from train import predict

def main(model_file, test_file, test_pred, write_score):
    print "Loading model..."
    model = pickle.load(open(model_file, 'r'))
    print "Predicting test..."
    predict(model, test_file, test_pred, write_score)


if __name__ == "__main__":
    main(argv[1], argv[2], argv[3], True)
