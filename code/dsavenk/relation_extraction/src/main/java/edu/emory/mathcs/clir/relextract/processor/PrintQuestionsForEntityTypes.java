package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.DocumentUtils;

import java.io.*;
import java.util.*;

/**
 * Created by dsavenk on 2/16/15.
 */
public class PrintQuestionsForEntityTypes extends Processor {

    public static final String ENTITY_TYPES_PARAMETER = "entity_types_file";

    private Map<String, List<String>> entityTypes_ = new HashMap<>();

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public PrintQuestionsForEntityTypes(Properties properties) throws IOException {
        super(properties);
        String entityTypes = properties.getProperty(ENTITY_TYPES_PARAMETER);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(entityTypes)));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            if (fields.length > 1) {
                List<String> types = new ArrayList<>();
                for (int i = 1; i < fields.length; ++i) {
                    types.add(fields[i]);
                }
                entityTypes_.put(fields[0], types);
            }
        }
        reader.close();
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        int questionSentences = DocumentUtils.getQuestionSentenceCount(document);
        for (Document.Span span : document.getSpanList()) {
            if (span.hasNerType() && span.hasEntityId()) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < questionSentences) {
                        int count = 0;
                        for (String entityId : span.getCandidateEntityIdList()) {
                            if (count++ >= 5) break;
                            String text = DocumentUtils.getSentenceTextWithEntityBoundary(document, mention, entityId);
                            if (entityTypes_.containsKey(entityId)) {
                                for (String type : entityTypes_.get(entityId)) {
                                    System.out.println(type + "\t" + text);
                                }
                            }
                        }
                    }
                }
            }
        }
        return document;
    }
}
