package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.LinkedList;

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
            return path.toString();
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
}
