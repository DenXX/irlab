package edu.emory.mathcs.ir.qa;

import edu.emory.mathcs.ir.utils.NlpUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a text with NLP annotations, i.e. sentence splits, tokens, POS, etc.
 */
public class Text {
    /**
     * Represents a natural language sentence.
     */
    public static class Sentence {
        public final String text;
        public final Token[] tokens;
        public final int charBeginOffset;
        public final int charEndOffset;

        /**
         * Constructs a sentence with the given number of tokens.
         * @param tokensCount The number of tokens in the sentence.
         */
        public Sentence(String text, int tokensCount, int charBeginOffset,
                        int charEndOffset) {
            this.text = text;
            tokens = new Token[tokensCount];
            this.charBeginOffset = charBeginOffset;
            this.charEndOffset = charEndOffset;
        }
    }

    /**
     * Represents natural language token (word, number etc).
     */
    public static class Token {
        public final String text;
        public final String lemma;
        public final String pos;
        public final int charBeginOffset;
        public final int charEndOffset;

        /**
         * Creates a token with the given parameters.
         * @param text Token text.
         * @param lemma Token lemma.
         * @param pos Token part of speech.
         */
        public Token(String text, String lemma, String pos, int charBeginOffset,
                     int charEndOffset) {
            this.text = text;
            this.lemma = lemma;
            this.pos = pos;
            this.charBeginOffset = charBeginOffset;
            this.charEndOffset = charEndOffset;
        }
    }

    /**
     * Represents an entity.
     */
    public static class Entity {
        /**
         * Represents a mention of the entity.
         */
        public class Mention {
            public final String text;
            public final int sentenceIndex;
            public final int beginToken;
            public final int endToken;

            /**
             * Constructs a mention with the given text in the given sentence
             * and tokens span.
             * @param text The text of the mention.
             * @param sentenceIndex The index of the sentence of the mention.
             * @param beginToken The first token of the mention.
             * @param endToken The token after the last token of the mention.
             */
            public Mention(String text, int sentenceIndex, int beginToken,
                           int endToken) {
                this.text = text;
                this.sentenceIndex = sentenceIndex;
                this.beginToken = beginToken;
                this.endToken = endToken;
            }
        }

        public final String name;
        public final List<Mention> mentions = new ArrayList<>();

        /**
         * Constructs the entity with the given name.
         * @param name The name of the entity.
         */
        public Entity(String name) {
            this.name = name;
        }
    }

    /**
     * Stores the string representation of the text.
     */
    public final String text;

    /**
     * Array of sentences.
     */
    private Sentence[] sentences_;

    /**
     * Array of entities mentioned in text.
     */
    private Entity[] entities_;

    /**
     * Creates an annotated text from its string representation.
     * @param text
     */
    public Text(String text) {
        this.text = text;
        Annotate();
    }

    /**
     * Annotates the text with an additional information.
     */
    private void Annotate() {
        Annotation annotation = NlpUtils.getAnnotations(this.text);

        // Get sentences.
        List<CoreMap> sentences = annotation.get(
                CoreAnnotations.SentencesAnnotation.class);
        sentences_ = new Sentence[sentences.size()];
        int[] cumulativeTokenCount = new int[sentences.size()];
        int sentenceIndex = 0;
        for (CoreMap sentence : sentences) {

            // Get beginning and end offset.
            int sentenceBeginOffset = sentence.get(
                    CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            int sentenceEndOffset = sentence.get(
                    CoreAnnotations.CharacterOffsetEndAnnotation.class);
            List<CoreLabel> tokens =
                    sentence.get(CoreAnnotations.TokensAnnotation.class);

            // Create current sentence.
            sentences_[sentenceIndex] = new Sentence(
                    sentence.get(CoreAnnotations.TextAnnotation.class),
                    tokens.size(), sentenceBeginOffset, sentenceEndOffset);

            // The number of tokens before the current sentence.
            final int tokensBefore = sentenceIndex == 0 ?
                    0 :
                    cumulativeTokenCount[sentenceIndex - 1];
            cumulativeTokenCount[sentenceIndex] = tokensBefore + tokens.size();

            int tokenIndex = 0;
            for (CoreMap token : tokens) {
                final String tokenText =
                        token.get(CoreAnnotations.TextAnnotation.class);
                final String posTag =
                        token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                final String lemma =
                        token.get(CoreAnnotations.LemmaAnnotation.class);
                final int tokenBeginOffset = token.get(
                        CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                final int tokenEndOffset = token.get(
                        CoreAnnotations.CharacterOffsetEndAnnotation.class);
                sentences_[sentenceIndex].tokens[tokenIndex] =
                        new Token(tokenText, lemma, posTag, tokenBeginOffset,
                                tokenEndOffset);
                ++tokenIndex;
            }

            // Increment sentence index.
            ++sentenceIndex;
        }

        List<CoreMap> entities =
                annotation.get(CoreAnnotations.MentionsAnnotation.class);
        entities_ = new Entity[entities.size()];
        int entityIndex = 0;
        for (CoreMap entity : entities) {
            final String entityName = entity.get(
                    CoreAnnotations.TextAnnotation.class);
            final int mentionSentence = entity.get(
                    CoreAnnotations.SentenceIndexAnnotation.class);
            final int firstSentenceTokenIndex = mentionSentence == 0 ?
                    0 :
                    cumulativeTokenCount[mentionSentence - 1];
            final int mentionBeginToken = entity.get(
                    CoreAnnotations.TokenBeginAnnotation.class) -
                    firstSentenceTokenIndex;
            final int mentionEndToken = entity.get(
                    CoreAnnotations.TokenEndAnnotation.class) -
                    firstSentenceTokenIndex;

            Entity currentEntity = new Entity(entityName);
            entities_[entityIndex] = currentEntity;
            currentEntity.mentions.add(currentEntity.new Mention(
                    entityName, mentionSentence, mentionBeginToken,
                    mentionEndToken));

            ++entityIndex;
        }
    }

    /**
     * @return Returns an array of sentences.
     */
    public Sentence[] getSentences() {
        return sentences_;
    }

    /**
     * @return Returns an array of entities mentioned in the text.
     */
    public Entity[] getEntities() {
        return entities_;
    }

    @Override
    public String toString() {
        return text;
    }
}
