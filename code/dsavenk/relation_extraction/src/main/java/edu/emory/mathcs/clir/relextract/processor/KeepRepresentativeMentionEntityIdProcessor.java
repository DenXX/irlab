package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.stanford.nlp.util.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 2/27/15.
 */
public class KeepRepresentativeMentionEntityIdProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public KeepRepresentativeMentionEntityIdProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        Set<String> representativeIds = new HashSet<>();
        int[][] newIndexes = new int[document.getSpanCount()][];

        Document.NlpDocument.Builder docBuilder = document.toBuilder();

        int spanIndex = 0;
        for (Document.Span span : document.getSpanList()) {
            if (!"MEASURE".equals(span.getType())) {
                representativeIds.clear();
                if (span.hasRepresentativeMention()) {
                    docBuilder.getSpanBuilder(spanIndex).clearEntityId();
                    for (int i = 0; i < span.getMention(span.getRepresentativeMention()).getCandidateEntityIdCount(); ++i) {
                        Document.Mention mention = span.getMention(span.getRepresentativeMention());
                        if (mention.getCandidateEntityScore(i) < Parameters.MIN_ENTITYID_SCORE) break;
                        if (i == 0) {
                            docBuilder.getSpanBuilder(spanIndex).setText(mention.getText());
                            docBuilder.getSpanBuilder(spanIndex).setValue(mention.getValue());
                            docBuilder.getSpanBuilder(spanIndex).setEntityId(mention.getCandidateEntityId(i));
                        }
                        representativeIds.add(mention.getCandidateEntityId(i));
                    }
                }

                newIndexes[spanIndex] = new int[span.getCandidateEntityIdCount()];
                int curIndex = 1;
                Document.Span.Builder spanBuilder = docBuilder.getSpanBuilder(spanIndex);
                spanBuilder.clearCandidateEntityId();
                spanBuilder.clearCandidateEntityScore();
                for (int i = 0; i < span.getCandidateEntityIdCount(); ++i) {
                    if (!representativeIds.contains(span.getCandidateEntityId(i))) {
                        newIndexes[spanIndex][i] = -1;
                    } else {
                        newIndexes[spanIndex][i] = curIndex++;
                        spanBuilder.addCandidateEntityId(span.getCandidateEntityId(i));
                        spanBuilder.addCandidateEntityScore(span.getCandidateEntityScore(i));
                    }
                }
            }
            ++spanIndex;
        }

        docBuilder.clearRelation();
        for (int relationIndex = 0; relationIndex < document.getRelationCount(); ++relationIndex) {
            Document.Relation.Builder rel = document.getRelation(relationIndex).toBuilder();

            int subjectIndex = rel.getSubjectSpan();
            int subjIdIndex = rel.getSubjectSpanCandidateEntityIdIndex();
            int objectIndex = rel.getObjectSpan();
            int objIdIndex = rel.getObjectSpanCandidateEntityIdIndex();

            // subject
            if (newIndexes[subjectIndex][subjIdIndex - 1] != -1) {
                rel.setSubjectSpanCandidateEntityIdIndex(newIndexes[subjectIndex][subjIdIndex - 1]);
            } else {
                continue;
            }

            // object
            if (!"MEASURE".equals(document.getSpan(objectIndex).getType())) {
                if (newIndexes[objectIndex][objIdIndex - 1] != -1) {
                    rel.setObjectSpanCandidateEntityIdIndex(newIndexes[objectIndex][objIdIndex - 1]);
                } else {
                    continue;
                }
            }

            docBuilder.addRelation(rel);
        }

        return docBuilder.build();
    }
}
