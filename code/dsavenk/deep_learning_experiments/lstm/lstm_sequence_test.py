from __future__ import print_function

__author__ = 'dsavenk'

import sys

import numpy as np
import random

from keras.layers.core import Dense, Dropout, Activation
from keras.layers.embeddings import Embedding
from keras.layers.recurrent import LSTM
from keras.models import Sequential
from keras.preprocessing.sequence import pad_sequences

VOCABULARY_SIZE = 100
EMBEDDING_DIMENSION = 32
HIDDEN_DIMENSION = 32
TRAINING_SIZE = 100000
TEST_SIZE = 1000
BATCH_SIZE = 200
EPOCHS = 50


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


def generate_data(size):
    X = []
    y = []
    for i in range(size):
        label = random.random() > 0.5
        X.append(generate_sequence(label, 20))
        y.append(1 if label else -1)
    return pad_sequences(X, maxlen=21), np.array(y)


def main():
    X, y = generate_data(TRAINING_SIZE)
    X_test, y_test = generate_data(TEST_SIZE)

    print('Defining network...', file=sys.stderr)
    model = Sequential()
    model.add(Embedding(VOCABULARY_SIZE, EMBEDDING_DIMENSION))
    model.add(LSTM(EMBEDDING_DIMENSION, HIDDEN_DIMENSION))
    #model.add(Dropout(0.5))
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
    print("Testing performance = " + score + ", acc = " + acc)

if __name__ == "__main__":
    main()
