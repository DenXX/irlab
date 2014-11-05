package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.*;

/**
 * Created by dsavenk on 11/4/14.
 */
public class AddDependencyTreeDepthProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public AddDependencyTreeDepthProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        for (Document.Sentence sent : document.getSentenceList()) {
            Queue<Integer> q = new LinkedList<>();

            int rootNode = sent.getDependencyRootToken();
            boolean foundRoot = false;
            // TODO(denxx): Remove this when root is available.
            for (int i = sent.getFirstToken(); i < sent.getLastToken(); ++i) {
                if (document.getToken(i).hasDependencyGovernor()
                        && document.getToken(i).getDependencyGovernor() == 0) {
                    docBuilder.getTokenBuilder(i).setDependencyTreeNodeDepth(0);
                    rootNode = i;
                    foundRoot = true;
                }
            }
            if (!foundRoot) {
                for (int i = sent.getFirstToken(); i < sent.getLastToken(); ++i) {
                    docBuilder.getTokenBuilder(i).setDependencyTreeNodeDepth(-1);
                }
                continue;
            }

            q.add(rootNode - sent.getFirstToken());
            //docBuilder.getTokenBuilder(sent.getDependencyRootToken()).setDependencyTreeNodeDepth(0);

            int[] depth = new int[sent.getLastToken() - sent.getFirstToken()];

            // We need to find children of all nodes.
            List<List<Integer>> children = new ArrayList<>();
            for (int i = 0; i < sent.getLastToken() - sent.getFirstToken(); ++i) {
                children.add(new LinkedList<Integer>());
            }
            for (int i = sent.getFirstToken(); i < sent.getLastToken(); ++i) {
                int governor = document.getToken(i).getDependencyGovernor();
                if (governor != 0) {
                    children.get(governor - 1).add(i - sent.getFirstToken());
                }
            }

            while (!q.isEmpty()) {
                int node = q.poll();
                for (int child : children.get(node)) {
                    depth[child] = depth[node] + 1;
                    docBuilder.getTokenBuilder(sent.getFirstToken() + child).setDependencyTreeNodeDepth(depth[child]);
                    q.add(child);
                }
            }
        }
        return docBuilder.build();
    }
}
