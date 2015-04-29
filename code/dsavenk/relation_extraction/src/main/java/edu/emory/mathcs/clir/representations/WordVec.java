package edu.emory.mathcs.clir.representations;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dsavenk on 4/28/15.
 */
public class WordVec {

    private final Map<String, float[]> wordvec_ = new HashMap<>();
    private int dim_;

    private String readString(DataInputStream stream) throws IOException {
        StringBuilder res = new StringBuilder();
        char ch = '\0';
        do {
            if (ch != '\0' && !Character.isWhitespace(ch)) res.append(ch);
            ch = (char)stream.read();
        } while (!Character.isWhitespace(ch) || res.length() == 0);
        return res.toString();
    }

    private float readFloat(DataInputStream stream) throws IOException {
        byte[] w = new byte[Float.BYTES];
        stream.read(w);
        return ByteBuffer.wrap(w).order(ByteOrder.nativeOrder()).getFloat();
    }

    public WordVec(String wordvecFile) throws IOException {
        DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(wordvecFile)));
        long words = Long.parseLong(readString(input));
        dim_ = Integer.parseInt(readString(input));
        for (int i = 0; i < words; ++i) {
            String word = readString(input);
            float[] w = new float[dim_];
            float len = 0;
            for (int j = 0; j < dim_; ++j) {
                w[j] = readFloat(input);
                len += w[j] * w[j];
            }
            for (int j = 0; j < dim_; ++j) {
                w[j] /= Math.sqrt(len);
            }
            wordvec_.put(word, w);
        }
        input.close();
    }

    public float[] getWordVec(String word) {
        if (wordvec_.containsKey(word)) {
            return wordvec_.get(word);
        } else if (wordvec_.containsKey(word.toLowerCase())) {
            return wordvec_.get(word.toLowerCase());
        }
        return new float[dim_];
    }

    public float[] getPhraseVec(String[] words) {
        float[] res = new float[dim_];
        for (String word : words) {
            float[] wordVec = getWordVec(word);
            for (int i = 0; i < dim_; ++i) {
                res[i] += wordVec[i] / words.length;
            }
        }
        return res;
    }
}
