package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.*;

/**
 * Created by dsavenk on 11/4/14.
 */
public class SentenceRelationExtractorTrainerProcessor extends Processor {

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public SentenceRelationExtractorTrainerProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {

        for (Document.Relation rel : document.getRelationList()) {
            if (rel.getRelation().equals("people.person.profession")) {
                int subjMentionIndex = 0;
                for (Document.Mention subjMention : document.getSpan(rel.getSubjectSpan()).getMentionList()) {
                    int objMentionIndex = 0;
                    for (Document.Mention objMention : document.getSpan(rel.getObjectSpan()).getMentionList()) {
                        if (subjMention.getSentenceIndex() == objMention.getSentenceIndex()) {
                            generateFeatures(document, rel.getSubjectSpan(), subjMentionIndex, rel.getObjectSpan(), objMentionIndex);
                        }
                        ++objMentionIndex;
                    }
                    ++subjMentionIndex;
                }
            }
        }
        return null;
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
}
