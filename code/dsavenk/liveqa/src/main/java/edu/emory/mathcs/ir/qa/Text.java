package edu.emory.mathcs.ir.qa;

import edu.emory.mathcs.ir.utils.NlpUtils;
import edu.emory.mathcs.ir.utils.StringUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores a text with NLP annotations, i.e. sentence splits, tokens, POS, etc.
 */
public class Text {
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

    // TODO(denxx): This should have connection to tokens and sentences, similar
    // to entities.
    /**
     * List of VP and NP chunks in the text.
     */
    private String[] chunks_;

    /**
     * Creates an annotated text from its string representation.
     * @param text The content of the text.
     */
    public Text(String text) {
        this(text, true);
    }

    /**
     * Creates an annotated text from its string representation.
     * @param text The content of the text.
     * @param annotate Whether to annotate text with sentences, tokens, etc.
     */
    private Text(String text, boolean annotate) {
        this.text = text;
        if (annotate) {
            Annotate();
        }
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
                final String posTag = getPosTag(token);
                final String lemma = getLemma(token);
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
        entities_ = new Entity[entities != null ? entities.size() : 0];
        if (entities != null) {
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

                Entity currentEntity = new Entity(entityName,
                        entity.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                entities_[entityIndex] = currentEntity;
                currentEntity.mentions.add(currentEntity.new Mention(
                        entityName, mentionSentence, mentionBeginToken,
                        mentionEndToken));

                ++entityIndex;
            }
        }

        // Get chunks
        List<String> chunks = NlpUtils.getChunks(annotation);
        chunks_ = chunks.toArray(new String[chunks.size()]);
    }

    private String getLemma(CoreMap token) {
        if (token.containsKey(CoreAnnotations.LemmaAnnotation.class)) {
            return token.get(CoreAnnotations.LemmaAnnotation.class);
        } else {
            return token.get(CoreAnnotations.TextAnnotation.class);
        }
    }

    private String getPosTag(CoreMap token) {
        if (token.has(CoreAnnotations.PartOfSpeechAnnotation.class)) {
            return token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        } else {
            return "UNKNOWN";
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

    /**
     * @return Returns an array of VP and NP chunks in the text.
     */
    public String[] getChunks() {
        return chunks_;
    }

    /**
     * Concatenates the current text with the provided.
     * @param text The text to concatenate to the current one.
     * @return The new text with concatenation of the current and given texts.
     */
    public Text concat(Text text) {
        // TODO(denxx): Make concatenation similar to subtext.
        return new Text(String.join(" \n", this.text, text.text));
    }

    /**
     * Extracts subtext from the current text starting and ending on sentences
     * with the provided indexes.
     * @param startSentence The first sentence to extract.
     * @param endSentence The last sentence to extract
     * @return New text containing the extracted part of the original text.
     */
    public Text subtext(int startSentence, int endSentence) {
        if (endSentence < startSentence) {
            throw new IndexOutOfBoundsException(
                    "endSentence should greater or equal to the startSentence");
        }
        if (startSentence >= sentences_.length ||
                endSentence >= sentences_.length) {
            throw new IndexOutOfBoundsException(
                    String.format("Sentence index out of range. Specified " +
                                    "range (%d, %d), total number of sentences is %d",
                            startSentence, endSentence, sentences_.length));
        }

        Text res = new Text(
                text.substring(sentences_[startSentence].charBeginOffset,
                        sentences_[endSentence].charEndOffset), false);

        // Get the offset of the start sentence.
        final int firstSentenceCharOffset =
                sentences_[startSentence].charBeginOffset;
        res.sentences_ = new Sentence[endSentence - startSentence + 1];

        // Go over all sentences and copy them.
        for (int sentenceIndex = startSentence; sentenceIndex <= endSentence;
             ++sentenceIndex) {
            final int newSentenceIndex = sentenceIndex - startSentence;
            res.sentences_[newSentenceIndex] =
                    new Sentence(sentences_[sentenceIndex],
                            -firstSentenceCharOffset);
        }

        // Copy all entities that have mentions in the specified range of
        // sentences.
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : entities_) {
            Entity copy = null;
            for (Entity.Mention mention : entity.mentions) {
                if (mention.sentenceIndex >= startSentence &&
                        mention.sentenceIndex <= endSentence) {
                    if (copy == null) {
                        copy = new Entity(entity.name, entity.type);
                    }
                    copy.mentions.add(
                            copy.new Mention(mention, -startSentence));
                }
            }
            // Add entity if we found at least one mention int he given range of
            // sentences.
            if (copy != null) {
                entities.add(copy);
            }
        }
        res.entities_ = entities.toArray(new Entity[entities.size()]);

        // TODO(denxx): Chunks are not copied, but needs to.
        res.chunks_ = new String[0];

        return res;
    }

    /**
     * Returns a set of normalized lemmas of terms in the text.
     * @return A set of lemmas of terms in the text.
     */
    public Set<String> getLemmaSet() {
        return getLemmaSet(false);
    }

    /**
     * Returns a set of normalized lemmas of terms in the text.
     * @param removeStopwords Whether to remove stopwords.
     * @return A set of lemmas of terms in the text.
     */
    public Set<String> getLemmaSet(boolean removeStopwords) {
        return getLemmaStream(removeStopwords)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a list of normalized lemmas of terms in the text.
     * @param removeStopwords Whether to remove stopwords.
     * @return A list of lemmas of terms in the text.
     */
    public List<String> getLemmaList(boolean removeStopwords) {
        return getLemmaStream(removeStopwords)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return text;
    }

    private Stream<String> getLemmaStream(boolean removeStopwords) {
        return Arrays.stream(this.getSentences())
                .flatMap(sentence -> Arrays.stream(sentence.tokens))
                .filter(token -> Character.isAlphabetic(token.pos.charAt(0)))
                .filter(token -> !removeStopwords ||
                        !NlpUtils.getStopwords().contains(token.lemma) ||
                        token.pos.startsWith("W"))
                .map(token -> token.lemma);
    }

    /**
     * Returns array of all tokens in the text.
     *
     * @return Array of tokens.
     */
    public Token[] getTokens() {
        List<Token> tokens = Arrays.stream(getSentences())
                .flatMap(sent -> Arrays.stream(sent.tokens))
                .collect(Collectors.toList());
        return tokens.toArray(new Token[tokens.size()]);
    }

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
         *
         * @param tokensCount The number of tokens in the sentence.
         */
        public Sentence(String text, int tokensCount, int charBeginOffset,
                        int charEndOffset) {
            this.text = text;
            tokens = new Token[tokensCount];
            this.charBeginOffset = charBeginOffset;
            this.charEndOffset = charEndOffset;
        }

        /**
         * Copy constructor.
         *
         * @param copy An original sentence to copy.
         */
        public Sentence(Sentence copy) {
            this(copy, 0);
        }

        /**
         * Creates a copy of the sentences and makes the appropriate corrections
         * in character-based beginning and end offsets.
         *
         * @param copy                      A sentence to copy.
         * @param characterOffsetCorrection Value to add to all character-based
         *                                  offsets.
         */
        public Sentence(Sentence copy, int characterOffsetCorrection) {
            this(copy.text, copy.tokens.length,
                    copy.charBeginOffset + characterOffsetCorrection,
                    copy.charEndOffset + characterOffsetCorrection);
            int index = 0;
            for (Token token : copy.tokens) {
                this.tokens[index++] =
                        new Token(token, characterOffsetCorrection);
            }
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
         *
         * @param text  Token text.
         * @param lemma Token lemma.
         * @param pos   Token part of speech.
         */
        public Token(String text, String lemma, String pos, int charBeginOffset,
                     int charEndOffset) {
            this.text = text;
            this.lemma = StringUtils.normalizeString(lemma);
            this.pos = pos;
            this.charBeginOffset = charBeginOffset;
            this.charEndOffset = charEndOffset;
        }

        /**
         * Copy constructor.
         *
         * @param copy Creates a copy of a token.
         */
        public Token(Token copy) {
            this(copy, 0);
        }

        /**
         * Creates a copy of the token and makes the specified correction in
         * the character-based beginning and end offset fields.
         *
         * @param copy                      The token to copy.
         * @param characterOffsetCorrection The value to add to all
         *                                  character-based offsets.
         */
        public Token(Token copy, int characterOffsetCorrection) {
            this(copy.text, copy.lemma, copy.pos,
                    copy.charBeginOffset + characterOffsetCorrection,
                    copy.charEndOffset + characterOffsetCorrection);
        }
    }

    /**
     * Represents an entity.
     */
    public static class Entity {
        public final String name;
        public final String type;
        public final List<Mention> mentions = new ArrayList<>();

        /**
         * Constructs the entity with the given name.
         *
         * @param name The name of the entity.
         */
        public Entity(String name, String type) {
            this.name = name;
            this.type = type;
        }

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
             *
             * @param text          The text of the mention.
             * @param sentenceIndex The index of the sentence of the mention.
             * @param beginToken    The first token of the mention.
             * @param endToken      The token after the last token of the mention.
             */
            public Mention(String text, int sentenceIndex, int beginToken,
                           int endToken) {
                this.text = text;
                this.sentenceIndex = sentenceIndex;
                this.beginToken = beginToken;
                this.endToken = endToken;
            }

            /**
             * Copy constructor and adjusts sentence index.
             *
             * @param copy                    A mention to copy.
             * @param sentenceIndexCorrection A value to add to the mention
             *                                sentence index.
             */
            public Mention(Mention copy, int sentenceIndexCorrection) {
                this(copy.text, copy.sentenceIndex + sentenceIndexCorrection,
                        copy.beginToken, copy.endToken);
            }

            /**
             * Copy constructor.
             * @param copy A mention to copy.
             */
            public Mention(Mention copy) {
                this(copy, 0);
            }
        }
    }
}
