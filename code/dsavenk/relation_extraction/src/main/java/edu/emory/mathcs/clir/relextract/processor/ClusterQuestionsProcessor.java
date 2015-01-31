package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.DocumentUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by dsavenk on 1/29/15.
 */
public class ClusterQuestionsProcessor extends Processor {

    private KnowledgeBase kb_;

    private String[] deps = {"xcomp", "amod", "dobj", "nobj", "avdcl"};
    private String[] whdeps = {"nsubj"};

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public ClusterQuestionsProcessor(Properties properties) {
        super(properties);
        //kb_ = KnowledgeBase.getInstance(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        int questionSentencesCount = DocumentUtils.getQuestionSentenceCount(document);

        int count = 0;
        for (Document.Span span : document.getSpanList()) {
            if (span.getType().equals("ENTITY")) {
                for (Document.Mention mention : span.getMentionList()) {
                    int mentionIndex = 0;
                    if (mention.getSentenceIndex() < questionSentencesCount) {
                        ++count;
                    }
                }
            }
        }
        if (count != 1) return null;

        for (Document.Span span : document.getSpanList()) {
            if (span.getType().equals("ENTITY")) {
                for (Document.Mention mention : span.getMentionList()) {
                    int mentionIndex = 0;
                    if (mention.getSentenceIndex() < questionSentencesCount) {
                        Document.Sentence questionSentence = document.getSentence(mention.getSentenceIndex());
                        int questionWordsCount = 0;
                        int questionWordIndex = -1;
                        for (int token = questionSentence.getFirstToken(); token < questionSentence.getLastToken(); ++token) {
                            if (document.getToken(token).getPos().startsWith("W")) {
                                ++questionWordsCount;
                                questionWordIndex = token;
                            }
                        }

                        if (questionWordsCount == 1) {
                            System.out.println(questionSentence.getText());
                            int rootTokenIndex = document.getSentence(mention.getSentenceIndex()).getDependencyRootToken() + document.getSentence(mention.getSentenceIndex()).getFirstToken() - 1;
                            for (int token = questionSentence.getFirstToken(); token < questionSentence.getLastToken(); ++token) {
                                int gov = document.getToken(token).getDependencyGovernor() - 1 + questionSentence.getFirstToken();
                                if (gov == questionWordIndex || gov == rootTokenIndex) {
                                    String dep = document.getToken(token).getDependencyType();
                                    String parent = gov == questionWordIndex ? document.getToken(questionWordIndex).getText().toLowerCase() : document.getToken(rootTokenIndex).getText().toLowerCase();
                                    System.out.println(parent + "\t" + dep + "\t" + document.getToken(token).getText().toLowerCase());
                                }
                            }
                            System.out.println("-----------------");
                        }



                        //System.out.println(span.getNerType() + "\t" + document.getSentence(mention.getSentenceIndex()).getText().replace("\t", " ").replace("\n", " ") + "\t" + NlpUtils.getQuestionTemplate(document, mention.getSentenceIndex(), span, mentionIndex) + "\t" + rootToken);
                        //for (String type : kb_.getEntityTypes(span.getEntityId())) {
                        //    synchronized (this) {
                        //        System.out.println(type + "\t" + document.getSentence(mention.getSentenceIndex()).getText().replace("\t", " ").replace("\n", " "));
                        //    }
                        //}
                        ++mentionIndex;
                    }
                }
            }
        }

        return document;
    }
}
