package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.Pair;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

import java.io.IOException;
import java.util.*;

/**
 * Relation extractor that extracts relation mentions from spans that occur in
 * question and answer text.
 */
public class QuestionAnswerBasedRelationExtractorTrainEvalProcessor
        extends RelationExtractorTrainEvalProcessor {

    /**
     * Creates an instance of the SentenceBasedRelationExtractorTrainerProcessor
     * class.
     *
     * @param properties Need to have {@link RelationExtractorTrainEvalProcessor.PREDICATES_LIST_PARAMETER}
     *                   and {@link RelationExtractorTrainEvalProcessor.DATASET_OUTFILE_PARAMETER}
     *                   parameters set.
     */
    public QuestionAnswerBasedRelationExtractorTrainEvalProcessor(Properties properties)
            throws IOException {
        super(properties);
    }

    @Override
    protected String getMentionText(Document.NlpDocument document,
                                    Document.Span subjSpan,
                                    Integer subjMention, Document.Span objSpan,
                                    Integer objMention) {
        Document.Sentence subjSent = document.getSentence(subjSpan.getMention(subjMention).getSentenceIndex());
        Document.Sentence objSent = document.getSentence(objSpan.getMention(objMention).getSentenceIndex());
        if (subjSent.getFirstToken() < objSent.getFirstToken()) {
            return subjSent.getText() + "\n" + objSent.getText();
        } else {
            return objSent.getText() + "\n" + subjSent.getText();
        }
    }

    @Override
    protected List<String> generateFeatures(Document.NlpDocument document,
                                            Document.Span subjSpan,
                                            Integer subjMention,
                                            Document.Span objSpan,
                                            Integer objMention) {
        int questionSentences = 0;
        while (questionSentences < document.getSentenceCount() &&
                document.getToken(document.getSentence(questionSentences).getFirstToken()).getBeginCharOffset() < document.getQuestionLength()) {
            ++questionSentences;
        }
        int questionTokens = document.getSentence(questionSentences - 1).getLastToken();

        List<String> features = new ArrayList<>();
        //features.add("SUBJ_NER:" + subjSpan.getNerType());
        //features.add("OBJ_NER:" + objSpan.getNerType());
        int subjMentionHeadToken = DependencyTreeUtils.getMentionHeadToken(document,
                subjSpan.getMention(subjMention));
        int objMentionHeadToken = DependencyTreeUtils.getMentionHeadToken(document,
                objSpan.getMention(objMention));

        int questionSentenceIndex = 0;
        int questionEntityToken = 0;
        int answerSentenceIndex = 0;
        int answerEntityToken = 0;
        if (subjMentionHeadToken < objMentionHeadToken) {
            questionSentenceIndex =
                    document.getToken(subjMentionHeadToken).getSentenceIndex();
            questionEntityToken = subjMentionHeadToken;
            answerSentenceIndex = document.getToken(objMentionHeadToken).getSentenceIndex();
            answerEntityToken = objMentionHeadToken;
        } else {
            questionSentenceIndex =
                    document.getToken(objMentionHeadToken).getSentenceIndex();
            questionEntityToken = objMentionHeadToken;
            answerSentenceIndex = document.getToken(subjMentionHeadToken).getSentenceIndex();
            answerEntityToken = subjMentionHeadToken;
        }

        addQuestionFeatures(document, questionSentenceIndex, questionEntityToken, features);
        addAnswerFeatures(document, answerSentenceIndex, answerEntityToken, features);
        addQuestionTemplateFeatures(document, questionSentenceIndex, features);

        return features;
    }

    private void addAnswerFeatures(Document.NlpDocument document, int answerSentenceIndex, int answerEntityToken, List<String> features) {
        int root = document.getSentence(answerSentenceIndex).getDependencyRootToken() +
                document.getSentence(answerSentenceIndex).getFirstToken() - 1;
        String path = DependencyTreeUtils.getDependencyPath(document, root, answerEntityToken, true, true, false);
        if (path != null) {
            features.add("ANSWER_PATH: " + path);
        }
    }

    private void addQuestionFeatures(Document.NlpDocument document, int questionSentenceIndex, int questionEntityToken, List<String> features) {
        List<Integer> questionWords = new ArrayList<>();
        for (int index = document.getSentence(questionSentenceIndex).getFirstToken();
             index < document.getSentence(questionSentenceIndex).getLastToken();
                ++index) {
            if (document.getToken(index).getPos().startsWith("W")) {
                questionWords.add(index);
            }
        }

        if (questionWords.size() > 0) {
            for (int questionWord : questionWords) {
                String path = DependencyTreeUtils.getDependencyPath(document, questionWord, questionEntityToken, true, true, false);
                if (path != null) {
                    features.add("QUESTION_PATH: " + path);
                }
            }
        } else {
            int root = document.getSentence(questionSentenceIndex).getDependencyRootToken() +
                    document.getSentence(questionSentenceIndex).getFirstToken() - 1;
            if (root >= 0) {
                String path = DependencyTreeUtils.getDependencyPath(document, root, questionEntityToken, true, true, false);
                if (path != null) {
                    features.add("QUESTION_PATH: " + path);
                }
            }
        }
    }

    private void addQuestionTemplateFeatures(Document.NlpDocument document, int questionSentenceIndex,
                                             List<String> features) {
        StringBuilder template = new StringBuilder();
        String lastNer = "";
        for (int token = document.getSentence(questionSentenceIndex).getFirstToken();
                token < document.getSentence(questionSentenceIndex).getLastToken();
                ++token) {
            if (document.getToken(token).hasNer()) {
                if (!document.getToken(token).getNer().equals(lastNer)) {
                    lastNer = document.getToken(token).getNer();
                    template.append(" [" + lastNer.toUpperCase() + "]");
                }
            } else {
                lastNer = "";
                if (document.getToken(token).getPos().startsWith("W")
                        || (Character.isLetterOrDigit(document.getToken(token).getPos().charAt(0)) &&
                        !StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(document.getToken(token).getLemma()))) {
                    template.append(" " + NlpUtils.normalizeStringForMatch(PTBTokenizer.ptb2Text(document.getToken(token).getLemma())));
                }
            }

        }

        features.add("QUESTION_TEMPLATE: " + template.toString().trim());
    }

    @Override
    protected Iterable<Pair<Integer, Integer>> getRelationMentionsIterator(
            Document.NlpDocument document, Document.Span subjSpan, Document.Span objSpan) {
        return new QuestionAnswerRelationMentionIterable(document, subjSpan, objSpan);
    }


    private static class QuestionAnswerRelationMentionIterable implements Iterable<Pair<Integer, Integer>> {

        private final Document.Span subjectSpan_;
        private final Document.Span objectSpan_;
        private final Document.NlpDocument document_;

        public QuestionAnswerRelationMentionIterable(Document.NlpDocument document,
                Document.Span subjSpan, Document.Span objSpan) {
            subjectSpan_ = subjSpan;
            objectSpan_ = objSpan;
            document_ = document;
        }

        @Override
        public Iterator<Pair<Integer, Integer>> iterator() {
            return new QuestionAnswerRelationMentionIterator();
        }

        private class QuestionAnswerRelationMentionIterator implements Iterator<Pair<Integer, Integer>> {

            private int currentSubjectMention_ = 0;
            private int currentObjectMention_ = -1;
            private int questionTokens_ = 0;

            public QuestionAnswerRelationMentionIterator() {
                int questionSentencesCount = 0;
                while (questionSentencesCount < document_.getSentenceCount() &&
                        document_.getToken(
                                document_.getSentence(questionSentencesCount)
                                        .getFirstToken()).getBeginCharOffset()
                                < document_.getQuestionLength()) {
                    ++questionSentencesCount;
                }
                questionTokens_ = document_.getSentence(questionSentencesCount - 1).getLastToken();
                findNextPair();
            }

            private boolean findNextPair() {
                while (currentSubjectMention_ < subjectSpan_.getMentionCount()) {
                    while (++currentObjectMention_ < objectSpan_.getMentionCount()) {
                        if (isMentionOk())
                            return true;
                    }
                    currentObjectMention_ = -1;
                    ++currentSubjectMention_;
                }
                return false;
            }

            private boolean isMentionOk() {
                // One of the spans should be in the question and the other in
                // the answer.
                return (subjectSpan_.getMention(currentSubjectMention_).getTokenBeginOffset() < questionTokens_)
                        != (objectSpan_.getMention(currentObjectMention_).getTokenBeginOffset() < questionTokens_);
            }

            @Override
            public boolean hasNext() {
                return currentSubjectMention_ < subjectSpan_.getMentionCount() &&
                        currentObjectMention_ < objectSpan_.getMentionCount();
            }

            @Override
            public Pair<Integer, Integer> next() {
                Pair<Integer, Integer> pair =
                        new Pair<>(currentSubjectMention_, currentObjectMention_);
                findNextPair();
                return pair;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Iterator is read-only");
            }
        }
    }
}
