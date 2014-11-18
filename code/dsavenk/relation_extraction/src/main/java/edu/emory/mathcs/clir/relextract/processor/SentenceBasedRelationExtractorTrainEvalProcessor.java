package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.stanford.nlp.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * Relation extractor that extracts relation mentions from spans that occur in
 * the same sentence. Based on Mintz et al 2009 work.
 */
public class SentenceBasedRelationExtractorTrainEvalProcessor
        extends RelationExtractorTrainEvalProcessor {

    /**
     * Creates an instance of the SentenceBasedRelationExtractorTrainerProcessor
     * class.
     *
     * @param properties Need to have {@link RelationExtractorTrainEvalProcessor.PREDICATES_LIST_PARAMETER}
     *                   and {@link RelationExtractorTrainEvalProcessor.DATASET_OUTFILE_PARAMETER}
     *                   parameters set.
     */
    public SentenceBasedRelationExtractorTrainEvalProcessor(Properties properties)
            throws IOException {
        super(properties);
    }

    @Override
    protected String getMentionText(Document.NlpDocument document,
                                    Document.Span subjSpan,
                                    Integer subjMention, Document.Span objSpan,
                                    Integer objMention) {
        return document.getSentence(
                subjSpan.getMention(subjMention).getSentenceIndex()).getText();
    }

    @Override
    protected List<String> generateFeatures(Document.NlpDocument document,
                                            Document.Span subjSpan,
                                            Integer subjMention,
                                            Document.Span objSpan,
                                            Integer objMention) {
        List<String> features = new ArrayList<>();
        //features.add("SUBJ_NER:" + subjSpan.getNerType());
        //features.add("OBJ_NER:" + objSpan.getNerType());
        int subjMentionHeadToken = getHeadToken(document,
                subjSpan.getMention(subjMention));
        int objMentionHeadToken = getHeadToken(document,
                objSpan.getMention(objMention));

        // Now we need to find a path between this nodes...
        addDirectedPath(document, subjSpan, objSpan, subjMentionHeadToken,
                objMentionHeadToken, true, features);
        addSurfaceFeatures(document, subjSpan, subjMention, objSpan, objMention, features);
        addQuestionFeatures(document, true, features);
        return features;
    }

    @Override
    protected Iterable<Pair<Integer, Integer>> getRelationMentionsIterator(
            Document.Span subjSpan, Document.Span objSpan) {
        return new SentenceRelationMentionIterable(subjSpan, objSpan);
    }

    protected void addSurfaceFeatures(
            Document.NlpDocument document, Document.Span subjSpan,
            int subjSpanMentionIndex, Document.Span objSpan,
            int objSpanMentionIndex, List<String> features) {
        String subjNerType = subjSpan.getNerType();
        String objNerType = objSpan.getNerType();
        Document.Mention subjMention =
                subjSpan.getMention(subjSpanMentionIndex);
        Document.Mention objMention =
                objSpan.getMention(objSpanMentionIndex);
        boolean reversed = subjMention.getTokenBeginOffset() >
                objMention.getTokenBeginOffset();

        // Surface pattern features.
        StringBuilder between = new StringBuilder();

        int firstSentenceToken = document.getSentence(
                subjMention.getSentenceIndex()).getFirstToken();
        int lastSentenceToken = document.getSentence(
                subjMention.getSentenceIndex()).getLastToken();

        int minFirstToken = Math.min(subjMention.getTokenBeginOffset(),
                objMention.getTokenBeginOffset());
        int minLastToken = Math.min(subjMention.getTokenEndOffset(),
                objMention.getTokenEndOffset());
        int maxFirstToken = Math.max(subjMention.getTokenBeginOffset(),
                objMention.getTokenBeginOffset());
        int maxLastToken = Math.max(subjMention.getTokenEndOffset(),
                objMention.getTokenEndOffset());
        for (int tokenIndex = minLastToken; tokenIndex < maxFirstToken; ++tokenIndex) {
            if (tokenIndex != minLastToken) {
                between.append(" ");
            }
            between.append(document.getToken(tokenIndex).getLemma().toLowerCase());
            between.append("/");
            between.append(document.getToken(tokenIndex).getPos());
        }

        List<String> leftWindows = new ArrayList<>();
        List<String> rightWindows = new ArrayList<>();
        // Start with no context on left or right.
        leftWindows.add("");
        rightWindows.add("");
        // Add some tokens from the left of the leftmost span.
        for (int leftWindowTokenIndex = minFirstToken - 1;
             leftWindowTokenIndex >= Math.max(firstSentenceToken, minFirstToken - 3);
             --leftWindowTokenIndex) {
            String tmp = document.getToken(leftWindowTokenIndex).getLemma() + "/" +
                    document.getToken(leftWindowTokenIndex).getPos() + " " + leftWindows.get(leftWindows.size() - 1);
            leftWindows.add(tmp.trim().replaceAll("\\s+", " "));
        }
        for (int rightWindowTokenIndex = maxLastToken; rightWindowTokenIndex < Math.min(lastSentenceToken, maxLastToken + 3); ++rightWindowTokenIndex) {
            String tmp = rightWindows.get(rightWindows.size() - 1) + " " +
                    document.getToken(rightWindowTokenIndex).getLemma() + "/" +
                    document.getToken(rightWindowTokenIndex).getPos();
            rightWindows.add(tmp.trim().replaceAll("\\s+", " "));
        }

        for (String left : leftWindows) {
            for (String right : rightWindows) {
                String feature = (reversed ? "<-- <" : "--> <") + left +
                        "> [" + (!reversed ? subjNerType : objNerType) + "] -"
                        + between + "- [" + (reversed ? subjNerType : objNerType) + "] <" +
                        right + ">";
                features.add("SURFACE_PATH:\t" + feature.trim().replaceAll("\\s+", " "));
            }
        }
    }

    private void addDirectedPath(Document.NlpDocument document,
                                 Document.Span subjSpan,
                                 Document.Span objSpan,
                                 int leftTokenIndex,
                                 int rightTokenIndex, boolean lexicalized,
                                 List<String> features) {
        String path = DependencyTreeUtils.getDependencyPath(document,
                leftTokenIndex, rightTokenIndex, lexicalized, false);
        if (path != null) {
            features.add("DEP_PATH:\t[" + subjSpan.getNerType() + "]:" + path +
                    ":[" + objSpan.getNerType() + "]");
        }
    }

    private void addQuestionFeatures(Document.NlpDocument document,
                                     boolean lexicalized, List<String> features) {
        if (document.hasQuestionLength()) {
            int sentences = 0;
            while (sentences < document.getSentenceCount() &&
                    document.getToken(document.getSentence(
                            sentences).getFirstToken()).getBeginCharOffset() <
                            document.getQuestionLength()) {
                ++sentences;
            }
            for (int index = 0; index < sentences; ++index) {
                int rootToken = document.getSentence(index).getFirstToken() +
                        document.getSentence(index).getDependencyRootToken() - 1;
                List<Integer> questionWords = new ArrayList<>();
                List<Integer> subjectWords = new ArrayList<>();
                for (int token = document.getSentence(index).getFirstToken();
                        token < document.getSentence(index).getLastToken();
                        ++token) {
                    if (document.getToken(token).getPos().startsWith("W")) {
                        questionWords.add(token);
                    }
                    // If this is a node under the root
                    if (document.getToken(token).getDependencyGovernor() ==
                            rootToken - document.getSentence(index).getFirstToken() + 1
                                && document.getToken(token).getDependencyType().contains("subj")) {
                        subjectWords.add(token);
                    }
                }
                for (int questionWord : questionWords) {
                    if (questionWord != rootToken) {
                        String qToRoot = DependencyTreeUtils.getDependencyPath(document, questionWord, rootToken, lexicalized, lexicalized);
                        features.add("QUESTION_PATH: " + qToRoot);
                    }
                    for (int subj : subjectWords) {
                        if (subj != questionWord && subj != rootToken) {
                            String qToSubj = DependencyTreeUtils.getDependencyPath(document, questionWord, subj, lexicalized, lexicalized);
                            features.add("QUESTION_PATH: " + qToSubj);
                        }
                    }
                }
            }
        }
    }

    private int getHeadToken(Document.NlpDocument document, Document.Mention mention) {
        int firstSentenceToken = document.getSentence(mention.getSentenceIndex()).getFirstToken();

        for (int tokenIndex = mention.getTokenBeginOffset();
             tokenIndex < mention.getTokenEndOffset(); ++tokenIndex) {
            // Skip nodes without parent (ROOT or dependency was collapsed).
            if (document.getToken(tokenIndex).hasDependencyGovernor()) {
                int governonTokenIndex = tokenIndex;
//                int iterations = 0;
                do {
                    // TODO(denxx): Why do we need this? Loops?
//                    if (++iterations > mention.getTokenEndOffset() - mention.getTokenBeginOffset()) {
//                        tokenIndex = mention.getTokenEndOffset() - 1;
//                        break;
//                    }
                    tokenIndex = governonTokenIndex;
                    governonTokenIndex = document.getToken(tokenIndex).getDependencyGovernor() + firstSentenceToken - 1;
                } while (governonTokenIndex >= mention.getTokenBeginOffset() &&
                        governonTokenIndex < mention.getTokenEndOffset() &&
                        document.getToken(tokenIndex).getDependencyGovernor() != 0);

                return tokenIndex;
            }
        }
        // If we wasn't able to find a head in a normal way, last token is usually head.
        return mention.getTokenEndOffset() - 1;
    }

    private static class SentenceRelationMentionIterable implements Iterable<Pair<Integer, Integer>> {

        private final Document.Span subjectSpan_;
        private final Document.Span objectSpan_;

        public SentenceRelationMentionIterable(Document.Span subjSpan,
                                               Document.Span objSpan) {
            subjectSpan_ = subjSpan;
            objectSpan_ = objSpan;
        }

        @Override
        public Iterator<Pair<Integer, Integer>> iterator() {
            return new SentenceRelationMentionIterator();
        }

        private class SentenceRelationMentionIterator implements Iterator<Pair<Integer, Integer>> {

            private int currentSubjectMention = 0;
            private int currentObjectMention = -1;

            public SentenceRelationMentionIterator() {
                findNextPair();
            }

            private boolean findNextPair() {
                while (currentSubjectMention < subjectSpan_.getMentionCount()) {
                    while (++currentObjectMention < objectSpan_.getMentionCount()) {
                        if (isMentionOk())
                            return true;
                    }
                    currentObjectMention = -1;
                    ++currentSubjectMention;
                }
                return false;
            }

            private boolean isMentionOk() {
                int minEnd = Math.min(subjectSpan_.getMention(currentSubjectMention).getTokenEndOffset(), objectSpan_.getMention(currentObjectMention).getTokenEndOffset());
                int maxBegin = Math.max(subjectSpan_.getMention(currentSubjectMention).getTokenBeginOffset(), objectSpan_.getMention(currentObjectMention).getTokenBeginOffset());
                return subjectSpan_.getMention(currentSubjectMention).getSentenceIndex() ==
                        objectSpan_.getMention(currentObjectMention).getSentenceIndex() &&
                        maxBegin - minEnd <= 20;
            }

            @Override
            public boolean hasNext() {
                return currentSubjectMention < subjectSpan_.getMentionCount() &&
                        currentObjectMention < objectSpan_.getMentionCount();
            }

            @Override
            public Pair<Integer, Integer> next() {
                Pair<Integer, Integer> pair =
                        new Pair<>(currentSubjectMention, currentObjectMention);
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
