package io.pivio.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ElasticsearchQueryHelper {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchQueryHelper.class);

  private final Client client;
  private final ObjectMapper mapper;

  @Autowired
  public ElasticsearchQueryHelper(Client client, ObjectMapper mapper) {
    this.client = client;
    this.mapper = mapper;
  }

  public boolean isDocumentPresent(String index, String type, String id) {
    return client.prepareGet(index, type, id).execute().actionGet().isExists();
  }

  public ArrayNode retrieveAllDocuments(SearchRequestBuilder searchRequest) throws IOException {
    try {
      SearchResponse searchResponse = searchRequest.execute().actionGet();
      ArrayNode allDocuments = mapper.createArrayNode();
      while (true) {
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
          allDocuments.add(mapper.readTree(searchHit.getSourceAsString()));
        }
        searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
        if (searchResponse.getHits().getHits().length == 0) {
          break;
        }
      }
      return allDocuments;
    } catch (Exception e) {
      LOG.warn("Could not retrieve all documents for " + searchRequest.toString(), e);
      return mapper.createArrayNode();
    }
  }
}
