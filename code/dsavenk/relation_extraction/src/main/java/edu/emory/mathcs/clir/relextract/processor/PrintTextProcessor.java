package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.Properties;

/**
 * Created by dsavenk on 9/26/14.
 */
public class PrintTextProcessor extends Processor {
    private int count = 0;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public PrintTextProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(
            Document.NlpDocument document) throws Exception {
        if (document.hasQuestionLength()) {
            for (int i = 0; i < document.getSentenceCount(); ++i) {
                int firstToken = document.getSentence(i).getFirstToken();
                int lastToken = document.getSentence(i).getLastToken();
                if (document.getToken(firstToken).getBeginCharOffset() >= document.getQuestionLength()) {
                    break;
                }
                System.out.println(document.getSentence(i).getText().replace("\n", " "));
                for (int j = firstToken; j < lastToken; ++j) {
                    if (document.getToken(j).hasDependencyGovernor() &&
                            document.getToken(j).getDependencyGovernor() == 0) {
                        System.out.println("---> " + document.getToken(j).getText());
                    }
                    if (document.getToken(j).getPos().startsWith("VB")) {
                        System.out.println("VB - " + document.getToken(j).getText() + "[" + document.getToken(j).getPos() + "]");
                    } else if (document.getToken(j).getPos().startsWith("W")) {
                        System.out.println("W - " + document.getToken(j).getText() + "[" + document.getToken(j).getPos() + "]");
                    }
                }
            }
        }
        System.out.println();
        return null;
    }

    @Override
    public void finishProcessing() {
        System.out.println(count);
    }
}
