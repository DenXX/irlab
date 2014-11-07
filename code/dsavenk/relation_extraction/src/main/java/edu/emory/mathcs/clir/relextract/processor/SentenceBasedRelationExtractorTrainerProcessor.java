package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * Relation extractor that extracts relation mentions from spans that occur in
 * the same sentence. Based on Mintz et al 2009 work.
 */
public class SentenceBasedRelationExtractorTrainerProcessor
        extends RelationExtractorTrainerProcessor {

    /**
     * Creates an instance of the SentenceBasedRelationExtractorTrainerProcessor
     * class.
     *
     * @param properties Need to have {@link edu.emory.mathcs.clir.relextract.processor.RelationExtractorTrainerProcessor.PREDICATES_LIST_PARAMETER}
     *                   and {@link edu.emory.mathcs.clir.relextract.processor.RelationExtractorTrainerProcessor.DATASET_OUTFILE_PARAMETER}
     *                   parameters set.
     */
    public SentenceBasedRelationExtractorTrainerProcessor(Properties properties)
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
        features.add("SUBJ_NER:" + subjSpan.getNerType());
        features.add("OBJ_NER:" + objSpan.getNerType());
        return features;
    }

    @Override
    protected Iterable<Pair<Integer, Integer>> getRelationMentionsIterator(
            Document.Span subjSpan, Document.Span objSpan) {
        return new SentenceRelationMentionIterable(subjSpan, objSpan);
    }

    protected Map<String, Double> generateFeatures(
            Document.NlpDocument document, int subjSpanIndex,
            int subjSpanMentionIndex, int objSpanIndex,
            int objSpanMentionIndex) {
        Map<String, Double> features = new HashMap<>();

        String subjNerType = document.getSpan(subjSpanIndex).getNerType();
        String objNerType = document.getSpan(objSpanIndex).getNerType();
        Document.Mention subjMention =
                document.getSpan(subjSpanIndex).getMention(subjSpanMentionIndex);
        Document.Mention objMention =
                document.getSpan(objSpanIndex).getMention(objSpanMentionIndex);
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
            between.append(document.getToken(tokenIndex).getLemma());
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
            leftWindows.add(document.getToken(leftWindowTokenIndex).getLemma() + "/" +
                    document.getToken(leftWindowTokenIndex).getPos() + " " + leftWindows.get(leftWindows.size() - 1));
        }
        for (int rightWindowTokenIndex = maxLastToken; rightWindowTokenIndex < Math.min(lastSentenceToken, maxLastToken + 3); ++rightWindowTokenIndex) {
            rightWindows.add(rightWindows.get(rightWindows.size() - 1) + " " +
                    document.getToken(rightWindowTokenIndex).getLemma() + "/" +
                    document.getToken(rightWindowTokenIndex).getPos());
        }

        System.out.println("-------------------------");
        System.out.println(document.getSentence(subjMention.getSentenceIndex()).getText());
        System.out.println("Subject: " + subjMention.getText());
        System.out.println("Object: " + objMention.getText());
        for (String left : leftWindows) {
            for (String right : rightWindows) {
                // TODO(denxx): Add to the list of features.
                System.out.println((reversed ? "<> " : "") + left + " : " + (!reversed ? subjNerType : objNerType) + " - " + between + " - " + (reversed ? subjNerType : objNerType) + " : " + right);
            }
        }

        // Dependency tree pattern
        int subjMentionHeadToken = getHeadToken(document, subjMention);
        int objMentionHeadToken = getHeadToken(document, objMention);

        // Now we need to find a path between this nodes...
        String path = getDirectedPath(document, subjMentionHeadToken, objMentionHeadToken, true);
        System.out.println(path);

        return features;
    }

    private String getDirectedPath(Document.NlpDocument document,
                                   int leftTokenIndex,
                                   int rightTokenIndex, boolean lexalized) {
        // If any of the nodes have no depth information, then we need to skip.
        if (!document.getToken(leftTokenIndex).hasDependencyTreeNodeDepth() ||
                !document.getToken(rightTokenIndex).hasDependencyTreeNodeDepth()) {
            return null;
        }

        int leftNodeDepth = document.getToken(leftTokenIndex).getDependencyTreeNodeDepth();
        int rightNodeDepth = document.getToken(rightTokenIndex).getDependencyTreeNodeDepth();

        int firstSentenceToken = document.getSentence(document.getToken(
                leftTokenIndex).getSentenceIndex()).getFirstToken();

        // If something was wrong with the parse tree, we better skip.
        if (leftNodeDepth == -1 || rightNodeDepth == -1) return null;

        // Left and right part of the path
        LinkedList<String> leftPart = new LinkedList<>();
        LinkedList<String> rightPart = new LinkedList<>();

        while (leftTokenIndex != rightTokenIndex) {
            int nextToken;
            int curToken;
            StringBuilder currentNode = new StringBuilder();

            if (leftNodeDepth > rightNodeDepth) {
                currentNode.append(" -> ");
                curToken = leftTokenIndex;
                nextToken = firstSentenceToken + document.getToken(leftTokenIndex).getDependencyGovernor() - 1;
                currentNode.append(document.getToken(curToken).getDependencyType());
            } else {
                curToken = rightTokenIndex;
                nextToken = firstSentenceToken + document.getToken(rightTokenIndex).getDependencyGovernor() - 1;
            }
            // Just in case, let's check that we are no getting in a loop or
            // something.
            assert nextToken != curToken;

            // We don't want a duplicate of the common ancestor, thus we check
            // that the next node is not an ancestor.
            if (lexalized && nextToken != leftTokenIndex && nextToken != rightTokenIndex) {
                currentNode.append("(");
                currentNode.append(document.getToken(nextToken).getLemma());
                currentNode.append(")");
            }

            if (leftNodeDepth > rightNodeDepth) {
                leftPart.add(currentNode.toString());
                leftTokenIndex = nextToken;
                // It should be just depth - 1, but let's assume it might be different.
                leftNodeDepth = document.getToken(leftTokenIndex).getDependencyTreeNodeDepth();
            } else {
                currentNode.append(document.getToken(curToken).getDependencyType());
                currentNode.append(" <- ");
                rightPart.addFirst(currentNode.toString());
                rightTokenIndex = nextToken;
                // It should be just depth - 1, but let's assume it might be different.
                rightNodeDepth = document.getToken(rightTokenIndex).getDependencyTreeNodeDepth();
            }
        }
        StringBuilder path = new StringBuilder();
        for (String left : leftPart) {
            path.append(left);
        }
        for (String right : rightPart) {
            path.append(right);
        }
        return path.toString();
    }

    private int getHeadToken(Document.NlpDocument document, Document.Mention mention) {
        int firstSentenceToken = document.getSentence(mention.getSentenceIndex()).getFirstToken();

        for (int tokenIndex = mention.getTokenBeginOffset();
             tokenIndex < mention.getTokenEndOffset(); ++tokenIndex) {
            // Skip nodes without parent (ROOT or dependency was collapsed).
            if (document.getToken(tokenIndex).hasDependencyGovernor()) {
                int governonTokenIndex = tokenIndex;
                do {
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
            private int currentObjectMention = 0;

            public SentenceRelationMentionIterator() {
                findNextPair();
            }

            private boolean findNextPair() {
                while (currentSubjectMention < subjectSpan_.getMentionCount()) {
                    while (++currentObjectMention < objectSpan_.getMentionCount()) {
                        if (subjectSpan_.getMention(currentSubjectMention).getSentenceIndex() ==
                                objectSpan_.getMention(currentObjectMention).getSentenceIndex())
                            return true;
                    }
                    currentObjectMention = 0;
                    ++currentSubjectMention;
                }
                return false;
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
