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
public class QnATrainEvalProcessor
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
    public QnATrainEvalProcessor(Properties properties)
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
        return null;
    }


    @Override
    protected Iterable<Pair<Integer, Integer>> getRelationMentionsIterator(
            Document.NlpDocument document, Document.Span subjSpan, Document.Span objSpan) {
        return new QnARelationMentionIterable(document, subjSpan, objSpan);
    }


    private static class QnARelationMentionIterable implements Iterable<Pair<Integer, Integer>> {

        private final Document.Span subjectSpan_;
        private final Document.Span objectSpan_;
        private final Document.NlpDocument document_;

        public QnARelationMentionIterable(Document.NlpDocument document,
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
                return true;
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
