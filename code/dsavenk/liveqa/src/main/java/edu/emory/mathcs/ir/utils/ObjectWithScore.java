package edu.emory.mathcs.ir.utils;

/**
 * Represents an object with double valued score.
 */
public class ObjectWithScore<T> implements Comparable<ObjectWithScore<T>> {
    public T object;
    public double score;

    public ObjectWithScore(T object, double score) {
        this.object = object;
        this.score = score;
    }

    @Override
    public int compareTo(ObjectWithScore<T> o) {
        return Double.compare(score, o.score);
    }
}
