package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.DocumentUtils;
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

    private static final int MAX_TOKEN_GAP = 100;

    /**
     * Creates an instance of the SentenceBasedRelationExtractorTrainerProcessor
     * class.
     *
     * @param properties Need to have {@link RelationExtractorTrainEvalProcessor.PREDICATES_LIST_PARAMETER}
     *                   and {@link RelationExtractorTrainEvalProcessor.DATASET_OUTFILE_PARAMETER}
     *                   parameters set.
     */
    public QuestionAnswerBasedRelationExtractorTrainEvalProcessor(Properties properties)
            throws Exception {
        super(properties);
    }

    private String getSentenceTextForMention(Document.NlpDocument document,
                                             Document.Span span,
                                             Integer mention,
                                             boolean isSubj) {
        StringBuilder str = new StringBuilder();
        int sentenceIndex = span.getMention(mention).getSentenceIndex();
        int tokenIndex = document.getSentence(sentenceIndex).getFirstToken();
        while (tokenIndex < document.getSentence(sentenceIndex).getLastToken()) {
            if (span.getMention(mention).getTokenEndOffset() == tokenIndex) {
                str.append(">");
            }

            str.append(" ");
            if (span.getMention(mention).getTokenBeginOffset() == tokenIndex) {
                str.append("<");
                if (isSubj) str.append("s:");
                else str.append("o:");
            }
            str.append(document.getToken(tokenIndex).getText().replace("\n", " "));
            ++tokenIndex;
        }
        if (span.getMention(mention).getTokenEndOffset() == tokenIndex) {
            str.append(">");
        }
        return str.toString();
    }

    @Override
    protected String getMentionText(Document.NlpDocument document,
                                    Document.Span subjSpan,
                                    Integer subjMention, Document.Span objSpan,
                                    Integer objMention) {
        int subjSent = subjSpan.getMention(subjMention).getSentenceIndex();
        int objSent = objSpan.getMention(objMention).getSentenceIndex();
        String subjMentionText = getSentenceTextForMention(document, subjSpan, subjMention, true);
        String objMentionText = getSentenceTextForMention(document, objSpan, objMention, false);
        if (subjSent < objSent) {
            return subjMentionText + "\n" + objMentionText;
        } else {
            return objMentionText + "\n" + subjMentionText;
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
        //features.add("SUBJ_OBJ:" + subjSpan.getNerType() + " " + objSpan.getNerType());
        int subjMentionHeadToken = DependencyTreeUtils.getMentionHeadToken(document,
                subjSpan.getMention(subjMention));
        int objMentionHeadToken = DependencyTreeUtils.getMentionHeadToken(document,
                objSpan.getMention(objMention));

        int questionSentenceIndex = 0;
        int questionEntityToken = 0;
        int answerSentenceIndex = 0;
        int answerEntityToken = 0;
        Document.Span questionSpan;
        Document.Span answerSpan;
        int questionMention = 0;
        int answerMention = 0;
        boolean reversed = subjMentionHeadToken > objMentionHeadToken;
        if (subjMentionHeadToken < objMentionHeadToken) {
            questionSentenceIndex =
                    document.getToken(subjMentionHeadToken).getSentenceIndex();
            questionEntityToken = subjMentionHeadToken;
            answerSentenceIndex = document.getToken(objMentionHeadToken).getSentenceIndex();
            answerEntityToken = objMentionHeadToken;
            questionSpan = subjSpan;
            answerSpan = objSpan;
            questionMention = subjMention;
            answerMention = objMention;
        } else {
            questionSentenceIndex =
                    document.getToken(objMentionHeadToken).getSentenceIndex();
            questionEntityToken = objMentionHeadToken;
            answerSentenceIndex = document.getToken(subjMentionHeadToken).getSentenceIndex();
            answerEntityToken = subjMentionHeadToken;
            questionSpan = objSpan;
            answerSpan = subjSpan;
            questionMention = objMention;
            answerMention = subjMention;
        }

        addQuestionFeatures(document, questionSentenceIndex, answerSentenceIndex, questionSpan, answerSpan, answerMention, questionEntityToken, features, reversed);
        addAnswerFeatures(document, answerSentenceIndex, answerSpan, questionSpan, answerEntityToken, features, reversed);
        addQuestionTemplateFeatures(document, questionSentenceIndex, questionSpan,  questionMention, answerSentenceIndex, answerSpan, answerMention, features, reversed);
        addQuestionAnswerPathFeatures(document, questionSentenceIndex, answerSentenceIndex, questionSpan, answerSpan, questionEntityToken, answerEntityToken, features, reversed);

        for (Document.Attribute attr : document.getAttributeList()) {
            if (attr.getKey().equals("cat")) {
                features.add("QUESTION_CATEGORY:\t" + attr.getValue());
            } else if (attr.getKey().equals("subcat")) {
                features.add("QUESTION_SUBCATEGORY:\t" + attr.getValue());
            }
        }

        return features;
    }

    private void addAnswerFeatures(Document.NlpDocument document, int answerSentenceIndex, Document.Span answerSpan, Document.Span questionSpan, int answerEntityToken, List<String> features, boolean reversed) {
        int root = document.getSentence(answerSentenceIndex).getDependencyRootToken() +
                document.getSentence(answerSentenceIndex).getFirstToken() - 1;
        String path = DependencyTreeUtils.getDependencyPath(document, root, answerEntityToken, true, true, false);
        if (path != null) {
            path = "[" + questionSpan.getNerType() + "] => " + path.trim() + " [" + answerSpan.getNerType() + "]";
            features.add("ANSWER_PATH:\t" + (reversed ? "<> " : "") + path);
        }

        String answerPivot = NlpUtils.getSentencePivot(document, answerSentenceIndex, answerEntityToken);
        if (answerPivot != null) {
            features.add("ANSWER_PIVOT:\t" + answerPivot);
        }
    }

    private void addQuestionFeatures(Document.NlpDocument document, int questionSentenceIndex, int answerSentenceIndex, Document.Span questionSpan, Document.Span answerSpan, int answerMention, int questionEntityToken, List<String> features, boolean reversed) {
        List<Integer> questionWords = new ArrayList<>();
        for (int index = document.getSentence(questionSentenceIndex).getFirstToken();
             index < document.getSentence(questionSentenceIndex).getLastToken();
                ++index) {
            if (document.getToken(index).getPos().startsWith("W")||
                    document.getToken(index).getPos().startsWith("MD")) {
                questionWords.add(index);
            }
        }

        List<String> window = new ArrayList<>();
        window.add("");
        int mentionFirstToken = answerSpan.getMention(answerMention).getTokenBeginOffset();
        for (int token = mentionFirstToken - 1;
             token >= Math.max(document.getSentence(answerSentenceIndex).getFirstToken(), mentionFirstToken - 3); --token) {
            window.add(document.getToken(token).getLemma().toLowerCase());
        }

        if (questionWords.size() > 0) {
            for (int questionWord : questionWords) {
                String path = DependencyTreeUtils.getDependencyPath(document, questionWord, questionEntityToken, true, true, false);
                if (path != null) {
                    StringBuilder windowStrAcc = new StringBuilder();
                    for (String windowStr : window) {
                        if (windowStrAcc.length() != 0) windowStrAcc.insert(0, " ");
                        windowStrAcc.insert(0, windowStr);
                        features.add("QUESTION_PATH:\t"+ (reversed ? "<> " : "") + path.trim() + " [" + questionSpan.getNerType() + "]" + " => |" + windowStrAcc + "| [" +answerSpan.getNerType() + "]");
                    }
                }
            }
        } else {
            int root = document.getSentence(questionSentenceIndex).getDependencyRootToken() +
                    document.getSentence(questionSentenceIndex).getFirstToken() - 1;
            if (root >= 0) {
                String path = DependencyTreeUtils.getDependencyPath(document, root, questionEntityToken, true, true, false);
                if (path != null) {
                    StringBuilder windowStrAcc = new StringBuilder();
                    for (String windowStr : window) {
                        if (windowStrAcc.length() != 0) windowStrAcc.insert(0, " ");
                        windowStrAcc.insert(0, windowStr);
                        features.add("QUESTION_PATH:\t"+ (reversed ? "<> " : "") + path.trim() + " [" + questionSpan.getNerType() + "]" + " => |" + windowStrAcc + "| [" +answerSpan.getNerType() + "]");
                    }
                }
            }
        }
        String questionPivot = NlpUtils.getSentencePivot(document, questionSentenceIndex, questionEntityToken);
        if (questionPivot != null) {
            features.add("QUESTION_PIVOT:\t" + questionPivot);
        }
        for (String qword : NlpUtils.getQuestionWords(document, questionSentenceIndex)) {
//            features.add("QUESTION_WORD:\t" + qword);
            features.add("QUESTION_WORD_PIVOT:\t" + qword + "_" + questionPivot);
        }

//        for (String questionLemma : DocumentUtils.getQuestionLemmas(document)) {
//            features.add("QUESTION_LEMMA:\t" + questionLemma);
//        }
    }

    private void addQuestionAnswerPathFeatures(Document.NlpDocument document, int questionSentenceIndex,
                                               int answerSentenceIndex,
                                               Document.Span questionSpan,
                                               Document.Span answerSpan,
                                               int questionEntityToken,
                                               int answerEntityToken,
                                               List<String> features, boolean reversed) {
        int ansRoot = document.getSentence(answerSentenceIndex).getDependencyRootToken() +
                document.getSentence(answerSentenceIndex).getFirstToken() - 1;
        String ansPath = DependencyTreeUtils.getDependencyPath(document, ansRoot, answerEntityToken, true, true, false);
        if (ansPath == null) ansPath = "";

        ansPath = (ansPath.trim() + " [" + answerSpan.getNerType() + "]").trim();
        List<Integer> questionWords = new ArrayList<>();
        for (int index = document.getSentence(questionSentenceIndex).getFirstToken();
             index < document.getSentence(questionSentenceIndex).getLastToken();
             ++index) {
            if (document.getToken(index).getPos().startsWith("W")||
                    document.getToken(index).getPos().startsWith("MD")) {
                questionWords.add(index);
            }
        }

        if (questionWords.size() > 0) {
            for (int questionWord : questionWords) {
                String questionPath = DependencyTreeUtils.getDependencyPath(document, questionWord, questionEntityToken, true, true, false);
                if (questionPath != null) {
                    questionPath = questionPath.trim() + " [" + questionSpan.getNerType() + "]";
                    features.add("QUESTION_ANSWER_PATH:\t"+ (reversed ? "<> " : "") + questionPath + " => " + ansPath);
                }
            }
        } else {
            int questionRoot = document.getSentence(questionSentenceIndex).getDependencyRootToken() +
                    document.getSentence(questionSentenceIndex).getFirstToken() - 1;
            if (questionRoot >= 0) {
                String questionPath = DependencyTreeUtils.getDependencyPath(document, questionRoot, questionEntityToken, true, true, false);
                if (questionPath != null) {
                    questionPath = questionPath.trim() + " [" + questionSpan.getNerType() + "]";
                    features.add("QUESTION_ANSWER_PATH:\t"+ (reversed ? "<> " : "") + questionPath + " => " + ansPath);
                }
            }
        }
    }

    private void addQuestionTemplateFeatures(Document.NlpDocument document, int questionSentenceIndex, Document.Span questionSpan, int questionMention, int answerSentenceIndex,
                                             Document.Span answerSpan, int answerMention, List<String> features, boolean reversed) {
        StringBuilder template = NlpUtils.getQuestionTemplate(document, questionSentenceIndex, questionSpan, questionMention);

        List<String> window = new ArrayList<>();
        window.add("");
        int mentionFirstToken = answerSpan.getMention(answerMention).getTokenBeginOffset();
        for (int token = mentionFirstToken - 1;
             token >= Math.max(document.getSentence(answerSentenceIndex).getFirstToken(), mentionFirstToken - 3); --token) {
            window.add(document.getToken(token).getLemma().toLowerCase());
        }

        StringBuilder windowStrAcc = new StringBuilder();
        for (String windowStr : window) {
            if (windowStrAcc.length() != 0) windowStrAcc.insert(0, " ");
            windowStrAcc.insert(0, windowStr);
            features.add("QUESTION_TEMPLATE:\t"+ (reversed ? "<> " : "") + template.toString().trim() + " => |" + windowStrAcc + "| [" + answerSpan.getNerType() + "]");
        }
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
            private int questionSentencesCount_ = 0;

            public QuestionAnswerRelationMentionIterator() {
                questionSentencesCount_ = 0;
                while (questionSentencesCount_ < document_.getSentenceCount() &&
                        document_.getToken(
                                document_.getSentence(questionSentencesCount_)
                                        .getFirstToken()).getBeginCharOffset()
                                < document_.getQuestionLength()) {
                    ++questionSentencesCount_;
                }
                questionTokens_ = document_.getSentence(questionSentencesCount_ - 1).getLastToken();
                findNextPair();
            }

            private boolean findNextPair() {
                // Skip all question, where answer contains more than one sentence.
                // if (document_.getSentenceCount() - questionSentencesCount_ > 1) {
                //    currentSubjectMention_ = subjectSpan_.getMentionCount();
                //    currentObjectMention_ = objectSpan_.getMentionCount();
                //    return false;
                //}

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
                int minEnd = Math.min(subjectSpan_.getMention(currentSubjectMention_).getTokenEndOffset(), objectSpan_.getMention(currentObjectMention_).getTokenEndOffset());
                int maxBegin = Math.max(subjectSpan_.getMention(currentSubjectMention_).getTokenBeginOffset(), objectSpan_.getMention(currentObjectMention_).getTokenBeginOffset());
                return (subjectSpan_.getMention(currentSubjectMention_).getTokenBeginOffset() < questionTokens_)
                        != (objectSpan_.getMention(currentObjectMention_).getTokenBeginOffset() < questionTokens_)
                        && maxBegin - minEnd < QuestionAnswerBasedRelationExtractorTrainEvalProcessor.MAX_TOKEN_GAP;
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
