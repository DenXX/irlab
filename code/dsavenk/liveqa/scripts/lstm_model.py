from __future__ import absolute_import
from __future__ import print_function

import hashlib
import socket
import sys

import numpy as np

from keras.layers.core import Dense, Dropout, Activation
from keras.layers.embeddings import Embedding
from keras.layers.recurrent import LSTM
from keras.models import Sequential
from keras.preprocessing.sequence import pad_sequences

'''
    Train a LSTM on the Yahoo! Answers reranking task,

    GPU command:
        THEANO_FLAGS=mode=FAST_RUN,device=gpu,floatX=float32,nvcc.fastmath=True python lstm_model.py ~/ir/data/liveqa/FullOct2007_lstm_train_smalltrain.txt ~/ir/data/liveqa/FullOct2007_lstm_train_smalltest.txt model_10K_64_1Mwords test
'''

MAX_TOKEN_LENGTH = 100
EMBEDDING_DIMENSION = 128
HIDDEN_DIMENSION = 128
BATCH_SIZE = 200
EPOCHS = 100
VOCABULARY_SIZE = 1000000


def read_weights(model_file, embedding_layer, lstm_layer, dense_layer):
    data = np.load(model_file + ".npy")
    embedding_layer.set_weights(data[0])
    lstm_layer.set_weights(data[1])
    dense_layer.set_weights(data[2])


def store_weights(embedding_layer, lstm_layer, dense_layer, model_file):
    weigths = (embedding_layer.get_weights(), lstm_layer.get_weights(),
               dense_layer.get_weights())
    np.save(model_file, weigths)


def parse_stories(lines):
    data = []
    for line in lines:
        line = line.decode('utf-8')
        label, title, body, answer = line.split('\t')
        q = title.strip().split(" ") + body.strip().split(" ")
        a = answer.strip().split(" ")
        q = q[:MAX_TOKEN_LENGTH]
        a = a[:MAX_TOKEN_LENGTH]
        data.append((int(label), q + ["\t"] + a))
    return data


def get_stories(f):
    return parse_stories(f.readlines())


def word_index(w):
    return int(hashlib.sha1(w.encode('utf-8')).hexdigest(),
               16) % VOCABULARY_SIZE


def vectorize_stories(data):
    X = []
    Y = []
    for label, qa in data:
        x = [word_index(w) for w in qa]
        y = label
        X.append(x)
        Y.append(y)
    return pad_sequences(X, maxlen=MAX_TOKEN_LENGTH * 2 + 1), np.array(Y)


def score_qa_pairs_from_socket(model):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('0.0.0.0', 8080)
    print('starting up on %s port %s' % server_address, file=sys.stderr)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(server_address)
    # Listen for incoming connections
    sock.listen(1)
    while True:
        # Wait for a connection
        print('waiting for a connection', file=sys.stderr)
        connection, client_address = sock.accept()
        try:
            print('connection from', client_address, file=sys.stderr)

            # Receive the data in small chunks and retransmit it
            qa = ""
            while True:
                data = connection.recv(1024 * 1024)
                if data:
                    qa += data
                    if qa.count('\t') > 0:
                        break
                else:
                    print('no more data from', client_address, file=sys.stderr)
                    break
            print('received: ', qa, file=sys.stderr)
            question, answer = qa.decode("utf-8").strip().split("\t")
            question = question.strip().split()[:MAX_TOKEN_LENGTH]
            answer = answer.strip().split()[:MAX_TOKEN_LENGTH]
            x_predict, y_predict = vectorize_stories(
                [(0, question + ["\t"] + answer), ])
            print('sending the result to the client', file=sys.stderr)
            score = model.predict(x_predict)
            connection.sendall(str(score[0][0]) + "\n")

        finally:
            # Clean up the connection
            print('closing connection', file=sys.stderr)
            connection.close()


def main(train_dataset, test_dataset, model_file, do_train):
    np.random.seed(1337)  # for reproducibility

    if do_train:
        print("Reading training file...", file=sys.stderr)
        with open(sys.argv[1]) as train_input:
            train = get_stories(train_input)

    print("Reading test file...", file=sys.stderr)
    with open(sys.argv[2]) as test_input:
        test = get_stories(test_input)

    # word_idx = dict((c, i + 1) for i, c in enumerate(vocab))
    print("Vectorizing data...", file=sys.stderr)
    if do_train:
        X_train, Y_train = vectorize_stories(train)
    X_test, Y_test = vectorize_stories(test)

    print('Defining layers...', file=sys.stderr)
    embedding_layer = Embedding(VOCABULARY_SIZE, EMBEDDING_DIMENSION)
    lstm_layer = LSTM(EMBEDDING_DIMENSION,
                      HIDDEN_DIMENSION)  # , return_sequences=True))
    dense_layer = Dense(HIDDEN_DIMENSION, 1)

    if not do_train:
        print('Reading weights...', file=sys.stderr)
        read_weights(model_file, embedding_layer, lstm_layer, dense_layer)

    print('Defining network...', file=sys.stderr)
    model = Sequential()
    model.add(embedding_layer)
    model.add(lstm_layer)
    model.add(Dropout(0.5))
    model.add(dense_layer)
    model.add(Activation('sigmoid'))

    model.compile(loss='binary_crossentropy', optimizer='adam',
                  class_mode="binary")

    if do_train:
        print("Training...", file=sys.stderr)
        model.fit(X_train, Y_train, batch_size=BATCH_SIZE, nb_epoch=EPOCHS,
                  validation_split=0.05, show_accuracy=True,
                  class_weight={1: 10, 0: 1})
        print("Saving model..")
        store_weights(embedding_layer, lstm_layer, dense_layer, model_file)

    print("Testing...", file=sys.stderr)
    score, acc = model.evaluate(X_test, Y_test, batch_size=BATCH_SIZE,
                                show_accuracy=True)
    print('Test score:', score, file=sys.stderr)
    print('Test accuracy:', acc, file=sys.stderr)

    score_qa_pairs_from_socket(model)


if __name__ == "__main__":
    EMBEDDING_DIMENSION = int(sys.argv[4])
    HIDDEN_DIMENSION = int(sys.argv[5])
    BATCH_SIZE = int(sys.argv[6])
    main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[7] == "train")
