package io.pivio.server.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchQueryHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryHelper.class);

    private final Client client;
    private final ObjectMapper mapper;

    public ElasticsearchQueryHelper(Client client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public boolean isDocumentPresent(String index, String type, String id) {
        return client.prepareGet(index, type, id).get().isExists();
    }

    public ArrayNode retrieveAllDocuments(SearchRequestBuilder searchRequest) {
        try {
            SearchResponse searchResponse = searchRequest.get();
            ArrayNode allDocuments = mapper.createArrayNode();
            while (true) {
                for (SearchHit searchHit : searchResponse.getHits().getHits()) {
                    allDocuments.add(mapper.readTree(searchHit.getSourceAsString()));
                }
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(60000)).get();
                if (searchResponse.getHits().getHits().length == 0) {
                    break;
                }
            }
            return allDocuments;
        }
        catch (Exception e) {
            LOG.warn("Could not retrieve all documents for " + searchRequest.toString(), e);
            return mapper.createArrayNode();
        }
    }
}
