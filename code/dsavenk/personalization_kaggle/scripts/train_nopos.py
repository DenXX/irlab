
from sklearn.ensemble import GradientBoostingClassifier, GradientBoostingRegressor
from sys import argv
import csv
import cPickle as pickle
from operator import itemgetter

def read_data(train_file):
    feats = []
    targets = []
    with open(train_file, 'r') as input:
        reader = csv.reader(input)
        header = []
        header = reader.next()
        pos_index = -1
        for i in xrange(len(header)):
            if header[i] == "cur_pos":
                pos_index = i
        for row in reader:
            target = 0
            if int(row[0]) == 1:
                target = 0.8
            elif int(row[0]) == 2:
                target = 1
            targets.append(target)
            feat = map(float, row[3:])
            feat[pos_index - 3] = 0
            feats.append(feat)
    return targets, feats

def train(targets, features, model_file, params):
    model = GradientBoostingRegressor(**params)
    print "Training hard..."
    model.fit(features, targets)
    print "Saving model..."
    pickle.dump(model, open(model_file, 'wb'))
    return model

def predict(model, test_file, test_pred, write_score = False):
    print "Predicting test..."
    with open(test_pred, 'w') as out:
        print >> out, "SessionID,URLID" + (",Score" if write_score else "")
        targets = []
        last_session_id = -1
        with open(test_file, 'r') as input:
            reader = csv.reader(input)
            next(reader, None)
            for row in reader:
                session_id = int(row[1])
                if session_id != last_session_id and len(targets) > 0:
                    targets.sort(key=itemgetter(0), reverse=True)
                    for target in targets:
                        print >> out, str(last_session_id) + "," + str(target[1]) + (("," + str(target[0])) if write_score else "")
                    targets = []
                last_session_id = session_id
                feat = map(float, row[3:])
                target = model.predict(feat)
                targets.append((target[0], int(row[2])))
            if len(targets) > 0:
                targets.sort(key=itemgetter(0), reverse=True)
                for target in targets:
                    print >> out, str(last_session_id) + "," + str(target[1]) + (("," + str(target[0])) if write_score else "")

def balance_dataset(targets, features):
    zeros_targets = []
    zeros_features = []
    nonzero_targets = []
    nonzero_features = []
    for i in xrange(len(targets)):
        if targets[i] == 0:
            zeros_targets.append(0)
            zeros_features.append(features[i])
        else:
            nonzero_targets.append(targets[i])
            nonzero_features.append(features[i])
    min_size = min([len(zeros_targets), len(nonzero_targets)])
    return zeros_targets[:min_size] + nonzero_targets[:min_size], zeros_features[:min_size] + nonzero_features[:min_size]

def main(train_file, model_file, test_file, test_pred, params, balance, write_scores):
    train_targets, train_features = read_data(train_file)
    if balance == "1":
        train_targets, train_features = balance_dataset(train_targets, train_features)
    model = train(train_targets, train_features, model_file, params)
    predict(model, test_file, test_pred, write_scores)
 
if __name__ == "__main__":
    params = {'n_estimators': int(argv[5]), 'subsample' : float(argv[6]),
              'max_depth': int(argv[7]), 'learning_rate': float(argv[8]), 'loss': argv[9], 'verbose': 1}
    main(argv[1], argv[2], argv[3], argv[4], params, argv[10] if len(argv) > 10 else "0", True if len(argv) > 11 else False)
