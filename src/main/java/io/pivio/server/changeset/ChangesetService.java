package io.pivio.server.changeset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Component
public class ChangesetService {

  private final Client client;
  private final ObjectMapper mapper;
  private final Set<String> excludedFields;

  @Autowired
  public ChangesetService(Client client, ObjectMapper mapper) {
    this.client = client;
    this.mapper = mapper;

    excludedFields = new HashSet<>();
    excludedFields.add("created");
    excludedFields.add("lastUpload");
    excludedFields.add("lastUpdate");
  }

  public Changeset computeNext(JsonNode pivioDocument) throws IOException {
    final String pivioDocumentId = pivioDocument.get("id").asText();
    GetResponse persistentPivioDocument = client.prepareGet("steckbrief", "steckbrief", pivioDocumentId)
        .execute()
        .actionGet();

    Map<String, Pair<String, String>> changed = new HashMap<>();
    if (!persistentPivioDocument.isExists()) {

      Iterator<Map.Entry<String, JsonNode>> fields = pivioDocument.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> changedField = fields.next();
        changed.put(changedField.getKey(), Pair.of("", changedField.getValue().textValue()));
      }
      return new Changeset(pivioDocumentId, 1L, changed);

    } else {
      JsonNode oldPivioDocument = mapper.readTree(persistentPivioDocument.getSourceAsString());
      Iterator<Map.Entry<String, JsonNode>> oldPivioDocumentFields = oldPivioDocument.fields();
      while (oldPivioDocumentFields.hasNext()) {
        Map.Entry<String, JsonNode> oldField = oldPivioDocumentFields.next();
        if (excludedFields.contains(oldField.getKey())) {
          continue;
        }
        if (!pivioDocument.has(oldField.getKey())) {
          changed.put(oldField.getKey(), Pair.of(oldField.getValue().textValue(), ""));
        }
      }

      Iterator<Map.Entry<String, JsonNode>> fields = pivioDocument.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (excludedFields.contains(field.getKey())) {
          continue;
        }

        if (!oldPivioDocument.has(field.getKey())) {
          changed.put(field.getKey(), Pair.of("", field.getValue().textValue()));
        } else if (!oldPivioDocument.get(field.getKey()).toString().equals(field.getValue().toString())) {
          if (field.getValue().isObject() || field.getValue().isArray()) {
            changed.put(field.getKey(), Pair.of(oldPivioDocument.get(field.getKey()).toString(), field.getValue().toString()));
          } else {
            changed.put(field.getKey(), Pair.of(oldPivioDocument.get(field.getKey()).textValue(), field.getValue().textValue()));
          }
        }
      }

      return new Changeset(pivioDocumentId, retrieveLastOrderNumber(pivioDocumentId) + 1L, changed);
    }
  }

  private long retrieveLastOrderNumber(String documentId) throws IOException {
    try {
      SearchResponse lastChangesetResponse = client.prepareSearch("changeset").setTypes("changeset")
          .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
          .setQuery(QueryBuilders.matchQuery("document", documentId))
          .addSort("order", SortOrder.DESC)
          .execute()
          .actionGet();

      if (lastChangesetResponse.getHits().getTotalHits() == 0) {
        return 0L;
      }

      JsonNode lastChangeset = mapper.readTree(lastChangesetResponse.getHits().getAt(0).getSourceAsString());
      return lastChangeset.get("order").longValue();
    } catch (ElasticsearchException e) {
      return 0L;
    }
  }
}
