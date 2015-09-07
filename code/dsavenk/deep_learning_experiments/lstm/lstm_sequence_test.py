from __future__ import print_function

__author__ = 'dsavenk'

import sys

import numpy as np
import random

from keras.layers.core import Dense, Activation, Merge
from keras.layers.embeddings import Embedding
from keras.layers.recurrent import LSTM
from keras.models import Sequential
from keras.preprocessing.sequence import pad_sequences

VOCABULARY_SIZE = 100
EMBEDDING_DIMENSION = 64
HIDDEN_DIMENSION = 32
TRAINING_SIZE = 1000
TEST_SIZE = 100
BATCH_SIZE = 500
EPOCHS = 5000


def generate_sequence(label, length):
    seq = list(range(1, VOCABULARY_SIZE))
    random.shuffle(seq)
    seq = seq[:length + 1]
    seq[int(length / 2)] = "0"
    if label:
        repetition = random.choice(seq[:int(length / 2)])
        position = random.randint(int(length / 2) + 2, length)
        seq[position] = repetition
    return seq


def generate_sequence2(label, length):
    seq = list(range(1, VOCABULARY_SIZE))
    random.shuffle(seq)
    seq1 = seq[:int(length / 2)]
    seq2 = seq[int(length / 2): length]
    if label:
        repetition = random.choice(seq1)
        position = random.randint(0, len(seq2) - 1)
        seq2[position] = repetition
    return seq1, seq2


def generate_data(size):
    X = []
    y = []
    for i in range(size):
        label = random.random() > 0.5
        X.append(generate_sequence(label, 20))
        y.append(1 if label else -1)
    return pad_sequences(X, maxlen=21), np.array(y)


def generate_data2(size, length=20):
    X1 = []
    X2 = []
    y = []
    for i in range(size):
        label = random.random() > 0.5
        X = generate_sequence2(label, length)
        X1.append(X[0])
        X2.append(X[1])
        y.append(1 if label else -1)
    return pad_sequences(X1, maxlen=int(length/2)), pad_sequences(X2, maxlen=int(length/2)), np.array(y)


def main_singlemodel():
    X, y = generate_data(TRAINING_SIZE)
    X_test, y_test = generate_data(TEST_SIZE)

    print('Defining network...', file=sys.stderr)
    model = Sequential()
    model.add(Embedding(VOCABULARY_SIZE, EMBEDDING_DIMENSION))
    model.add(LSTM(EMBEDDING_DIMENSION, HIDDEN_DIMENSION))
    model.add(Dense(HIDDEN_DIMENSION, 1))
    model.add(Activation('sigmoid'))
    print('Compiling...', file=sys.stderr)
    model.compile(loss='binary_crossentropy', optimizer='adam', class_mode="binary")

    print('Training...', file=sys.stderr)
    model.fit(X, y, batch_size=BATCH_SIZE, nb_epoch=EPOCHS,
              validation_split=0.05, show_accuracy=True)

    print("Testing...", file=sys.stderr)
    score, acc = model.evaluate(X_test, y_test, batch_size=BATCH_SIZE,
                                show_accuracy=True)
    print("Testing performance = " + str(score) + ", acc = " + str(acc))


def main_separatemodels():
    X1, X2, y = generate_data2(TRAINING_SIZE)
    X1_test, X2_test, y_test = generate_data2(TEST_SIZE)

    print('Defining network...', file=sys.stderr)
    firstlstm = Sequential()
    firstlstm.add(Embedding(VOCABULARY_SIZE, EMBEDDING_DIMENSION))
    firstlstm.add(LSTM(EMBEDDING_DIMENSION, HIDDEN_DIMENSION, return_sequences=False))

    secondlstm = Sequential()
    secondlstm.add(Embedding(VOCABULARY_SIZE, EMBEDDING_DIMENSION))
    secondlstm.add(LSTM(EMBEDDING_DIMENSION, HIDDEN_DIMENSION, return_sequences=False))

    model = Sequential()
    model.add(Merge([firstlstm, secondlstm], mode='concat'))
    model.add(Dense(HIDDEN_DIMENSION + HIDDEN_DIMENSION, 1, activation='sigmoid'))
    print('Compiling...', file=sys.stderr)
    model.compile(loss='binary_crossentropy', optimizer='adam', class_mode="binary")

    print('Training...', file=sys.stderr)
    model.fit([X1, X2], y, batch_size=BATCH_SIZE, nb_epoch=EPOCHS,
              validation_split=0.05, show_accuracy=True)

    print("Testing...", file=sys.stderr)
    score, acc = model.evaluate([X1_test, X2_test], y_test, batch_size=BATCH_SIZE,
                                show_accuracy=True)
    print("Testing performance = " + str(score) + ", acc = " + str(acc))

if __name__ == "__main__":
    main_separatemodels()
