package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dsavenk on 11/18/14.
 */
public class DependencyTreeUtils {

    /**
     * Returns dependency path between 2 entities in a sentence.
     * @param document A document which stores the sentences.
     * @param firstToken Source token of dependency path.
     * @param secondToken Target token of dependency path.
     * @param lexicalized Whether path should be lexicalized.
     * @return null if there were any problems or string representation of a
     * path.
     */
    public static String getDependencyPath(Document.NlpDocument document,
                                           int firstToken, int secondToken,
                                           boolean lexicalized,
                                           boolean includeFirstNode,
                                           boolean includeLastNode) {
        // If any of the nodes have no depth information, then we need to skip.
        if (!document.getToken(firstToken).hasDependencyTreeNodeDepth() ||
                !document.getToken(secondToken).hasDependencyTreeNodeDepth() ||
                document.getToken(firstToken).getSentenceIndex() !=
                        document.getToken(secondToken).getSentenceIndex()) {
            return null;
        }

        int leftNodeDepth = document.getToken(firstToken).getDependencyTreeNodeDepth();
        int rightNodeDepth = document.getToken(secondToken).getDependencyTreeNodeDepth();

        int firstSentenceToken = document.getSentence(document.getToken(
                firstToken).getSentenceIndex()).getFirstToken();

        // If something was wrong with the parse tree, we better skip.
        if (leftNodeDepth == -1 || rightNodeDepth == -1) return null;

        // Left and right part of the path
        LinkedList<String> leftPart = new LinkedList<>();
        LinkedList<String> rightPart = new LinkedList<>();
        if (includeFirstNode && firstToken != secondToken) {
            leftPart.add("(" + document.getToken(firstToken).getLemma().toLowerCase() + ")");
        }
        if (includeLastNode && firstToken != secondToken) {
            rightPart.add("("+document.getToken(secondToken).getLemma().toLowerCase()+")");
        }
        if (firstToken == secondToken && includeFirstNode && includeLastNode) {
            leftPart.add("(" + document.getToken(firstToken).getLemma().toLowerCase() + ")");
        }

        // Stanford tree might have multiple roots for some reason (even basic)
        while (firstToken != secondToken && (leftNodeDepth != 0 || rightNodeDepth != 0)) {
            int nextToken;
            int curToken;
            StringBuilder currentNode = new StringBuilder();

            if (leftNodeDepth > rightNodeDepth) {
                currentNode.append(" -> ");
                curToken = firstToken;
                nextToken = firstSentenceToken + document.getToken(firstToken).getDependencyGovernor() - 1;
                currentNode.append(document.getToken(curToken).getDependencyType());
                // TODO(denxx): Why do we need this anyway? It seems that even for
                // basic tree this happens.
                if (nextToken == curToken) nextToken = secondToken;
            } else {
                curToken = secondToken;
                nextToken = firstSentenceToken + document.getToken(secondToken).getDependencyGovernor() - 1;

                if (nextToken == curToken) nextToken = firstToken;
            }

            // We don't want a duplicate of the common ancestor, thus we check
            // that the next node is not an ancestor.
            if (lexicalized && nextToken != firstToken && nextToken != secondToken) {
                currentNode.append("(");
                currentNode.append(document.getToken(nextToken).getLemma().toLowerCase());
                currentNode.append(")");
            }

            if (leftNodeDepth > rightNodeDepth) {
                leftPart.add(currentNode.toString());
                firstToken = nextToken;
                // It should be just depth - 1, but let's assume it might be different.
                leftNodeDepth = document.getToken(firstToken).getDependencyTreeNodeDepth();
            } else {
                currentNode.append(document.getToken(curToken).getDependencyType());
                currentNode.append(" <- ");
                rightPart.addFirst(currentNode.toString());
                secondToken = nextToken;
                // It should be just depth - 1, but let's assume it might be different.
                rightNodeDepth = document.getToken(secondToken).getDependencyTreeNodeDepth();
            }
        }
        if (firstToken == secondToken) {
            StringBuilder path = new StringBuilder();
            for (String left : leftPart) {
                path.append(left);
            }
            for (String right : rightPart) {
                path.append(right);
            }
            String res = path.toString().trim();
            return res.length() > 0 ? res : null;
        }
        return null;
    }


    public static int getMentionHeadToken(Document.NlpDocument document, Document.Mention mention) {
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
                } while (governonTokenIndex != tokenIndex &&
                        governonTokenIndex >= mention.getTokenBeginOffset() &&
                        governonTokenIndex < mention.getTokenEndOffset() &&
                        document.getToken(tokenIndex).getDependencyGovernor() != 0);

                return tokenIndex;
            }
        }
        // If we wasn't able to find a head in a normal way, last token is usually head.
        return mention.getTokenEndOffset() - 1;
    }

    public static String getQuestionDependencyPath(Document.NlpDocument document, int questionSentenceIndex, int targetToken) {
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
                String path = DependencyTreeUtils.getDependencyPath(document, questionWord, targetToken, true, true, false);
                if (path != null) {
                    return path;
                }
            }
        } else {
            int root = document.getSentence(questionSentenceIndex).getDependencyRootToken() +
                    document.getSentence(questionSentenceIndex).getFirstToken() - 1;
            if (root >= 0) {
                String path = DependencyTreeUtils.getDependencyPath(document, root, targetToken, true, true, false);
                if (path != null) {
                    return path;
                }
            }
        }
        return null;
    }

    public static String getDependencyPathPivot(Document.NlpDocument document, int sentenceIndex, int sourceToken, int targetToken) {
        if (sourceToken == targetToken) return null;
        int s = sourceToken;
        int t = targetToken;
        while (s != t) {
            int sourceDepth = document.getToken(s).getDependencyTreeNodeDepth();
            int targetDepth = document.getToken(t).getDependencyTreeNodeDepth();

            // In some cases there are multiple roots and we can get to different :(
            assert (sourceDepth == 0 || targetDepth != 0);

            if (sourceDepth < targetDepth) {
                t = document.getToken(t).getDependencyGovernor() +
                        document.getSentence(sentenceIndex).getFirstToken() - 1;
            } else {
                s = document.getToken(s).getDependencyGovernor() +
                        document.getSentence(sentenceIndex).getFirstToken() - 1;
            }
        }
        if (s != sourceToken && s != targetToken) {
            return NlpUtils.normalizeStringForMatch(document.getToken(s).getLemma());
        }
        return null;
    }
}
