package edu.emory.mathcs.ir.search;

import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;
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

    @Override
    public SearchResult[] search(String query, int top) {
        final Map<String, Object> attributes = new HashMap<>();

        // TODO(denxx): Remove the actual API key.
        Bing3WebDocumentSourceDescriptor.attributeBuilder(attributes)
                .appid("qaiWmCHnf0KlqvRinysEmXYAmXrV51+K1M/YiDmPBa0");
        /* Query and the required number of results */
        attributes.put(CommonAttributesDescriptor.Keys.QUERY, query);
        attributes.put(CommonAttributesDescriptor.Keys.RESULTS, top);
        final ProcessingResult result = controller_.process(attributes,
                Bing3WebDocumentSource.class);
        final List<Document> documents = result.getDocuments();
        final SearchResult[] res = new SearchResult[documents.size()];
        for (int rank = 0; rank < documents.size(); ++rank) {
            res[rank] = SearchResult.create(rank, documents.get(rank));
        }
        return res;
    }
}
