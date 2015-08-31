package edu.emory.mathcs.ir.search;

import edu.emory.mathcs.ir.qa.AppConfig;
import org.carrot2.core.*;
import org.carrot2.core.attribute.CommonAttributesDescriptor;
import org.carrot2.source.microsoft.Bing3WebDocumentSource;
import org.carrot2.source.microsoft.Bing3WebDocumentSourceDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web search interface implementation, that uses Bing Search API.
 */
public class BingWebSearch implements WebSearch {
    private static final Controller controller_ =
            ControllerFactory.createSimple();
    private static final String[] apiKeys;
    private static int currentApiKeyIndex = 0;

    static {
        apiKeys = AppConfig.PROPERTIES.getProperty(
                AppConfig.BING_API_KEY_PARAMETER).split("\t");
        currentApiKeyIndex = 0;
    }

    @Override
    public SearchResult[] search(String query, int top) {
        if (query.trim().length() > 0) {
            final Map<String, Object> attributes = new HashMap<>();
            Bing3WebDocumentSourceDescriptor.attributeBuilder(attributes)
                    .appid(apiKeys[currentApiKeyIndex]);
        /* Query and the required number of results */
            attributes.put(CommonAttributesDescriptor.Keys.QUERY, query);
            attributes.put(CommonAttributesDescriptor.Keys.RESULTS, top);
            ProcessingResult result = null;
            try {
                result = controller_.process(attributes,
                        Bing3WebDocumentSource.class);
            } catch (ProcessingException ex) {
                // This exception is probably caused by the fact, API rate limit
                // was exceeded. Use the new key and continue.
                currentApiKeyIndex = (currentApiKeyIndex + 1) % apiKeys.length;
                Bing3WebDocumentSourceDescriptor.attributeBuilder(attributes)
                        .appid(apiKeys[currentApiKeyIndex + 1]);
                result = controller_.process(attributes,
                        Bing3WebDocumentSource.class);
            }
            if (result != null) {
                final List<Document> documents = result.getDocuments();
                final SearchResult[] res = new SearchResult[documents.size()];
                for (int rank = 0; rank < documents.size(); ++rank) {
                    res[rank] = SearchResult.create(rank, documents.get(rank));
                }
                return res;
            }
        }
        return new SearchResult[0];
    }
}
