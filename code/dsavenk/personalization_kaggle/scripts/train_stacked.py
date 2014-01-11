
from sklearn.ensemble import GradientBoostingClassifier, GradientBoostingRegressor
from sys import argv
import csv
import cPickle as pickle
from operator import itemgetter

from train import *

def read_stack_data(train_file):
    feats = []
    targets = []
    with open(train_file, 'r') as input:
        reader = csv.reader(input)
        next(reader, None)

        last_session_id = -1
        cur_session_feats = []
        for row in reader:
            target = 0
            session_id = int(row[1])
            if session_id != last_session_id and len(cur_session_feats) > 0:
                cur_feats = []
                for i in xrange(len(cur_session_feats)):
                    cur_feats = []
                    cur_feats += cur_session_feats[i]
                    for j in xrange(len(cur_session_feats)):
                        if i == j: continue
                        cur_feats += cur_session_feats[j]
                    feats.append(cur_feats)
                cur_session_feats = []
            last_session_id = session_id

            if int(row[0]) == 1:
                target = 0.8
            elif int(row[0]) == 2:
                target = 1
            targets.append(target)
            feat = map(float, row[3:])
            cur_session_feats.append(feat)
    if len(cur_session_feats) > 0:
        cur_feats = []
        for i in xrange(len(cur_session_feats)):
            cur_feats = []
            cur_feats += cur_session_feats[i]
            for j in xrange(len(cur_session_feats)):
                if i == j: continue
                cur_feats += cur_session_feats[j]
            feats.append(cur_feats)
        cur_session_feats = []
 
    return targets, feats

def predict_stacked(model, test_file, test_pred):
    print "Predicting test..."
    with open(test_pred, 'w') as out:
        print >> out, "SessionID,URLID,Score"
        targets = []
        last_session_id = -1
        with open(test_file, 'r') as input:
            reader = csv.reader(input)
            next(reader, None)

            cur_session_feats = []
            urlids = []
            for row in reader:
                session_id = int(row[1])
                if session_id != last_session_id and len(cur_session_feats) > 0:
                    for i in xrange(len(cur_session_feats)):
                        cur_feats = []
                        cur_feats += cur_session_feats[i]
                        for j in xrange(len(cur_session_feats)):
                            if i == j: continue
                            cur_feats += cur_session_feats[j]
                        target = model.predict(cur_feats)
                        targets.append((target[0], urlids[i]))
                    targets.sort(key=itemgetter(0), reverse=True)
                    for target in targets:
                        print >> out, str(last_session_id) + "," + str(target[1]) + "," + str(target[0])
                    targets = []
                    cur_session_feats = []
                    urlids = []
                last_session_id = session_id
                feat = map(float, row[3:])
                urlids.append(int(row[2]))
                cur_session_feats.append(feat)
            if len(cur_session_feats) > 0:
                for i in xrange(len(cur_session_feats)):
                    cur_feats = []
                    cur_feats += cur_session_feats[i]
                    for j in xrange(len(cur_session_feats)):
                        if i == j: continue
                        cur_feats += cur_session_feats[j]
                    target = model.predict(cur_feats)
                    targets.append((target[0], urlids[i]))
                targets.sort(key=itemgetter(0), reverse=True)
                for target in targets:
                    print >> out, str(last_session_id) + "," + str(target[1]) + "," + str(target[0])
                targets = []
                cur_session_feats = []
 

def main(train_file, model_file, test_file, test_predict, params):
    targets, features = read_stack_data(train_file)
    model = train(targets, features, model_file, params)
    predict_stacked(model, test_file, test_predict)

if __name__ == "__main__":
    params = {'n_estimators': int(argv[5]), 'subsample' : float(argv[6]),
              'max_depth': int(argv[7]), 'learning_rate': float(argv[8]), 'loss': argv[9], 'verbose': 1}
 
    main(argv[1], argv[2], argv[3], argv[4], params)
