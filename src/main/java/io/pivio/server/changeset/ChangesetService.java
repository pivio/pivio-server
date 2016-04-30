package io.pivio.server.changeset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.JsonDiff;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
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
        excludedFields.add("/created");
        excludedFields.add("/lastUpload");
        excludedFields.add("/lastUpdate");
    }

    public Changeset computeNext(JsonNode document) throws IOException {
        final String documentId = document.get("id").asText();
        final Optional<JsonNode> persistentDocument = getDocument(documentId);
        final JsonNode patch = JsonDiff.asJson(persistentDocument.orElse(mapper.createObjectNode()), document);
        return new Changeset(documentId, retrieveLastOrderNumber(documentId) + 1L, filterExcludedFields(patch));
    }

    private ArrayNode filterExcludedFields(JsonNode json) {
        ArrayNode filteredJson = mapper.createArrayNode();
        Iterator<JsonNode> elements = json.elements();
        while (elements.hasNext()) {
            JsonNode current = elements.next();
            if (current.has("path") && !excludedFields.contains(current.get("path").textValue())) {
                filteredJson.add(current);
            }
        }
        return filteredJson;
    }

    private long retrieveLastOrderNumber(String documentId) throws IOException {
        Optional<JsonNode> lastChangeset = getLastChangeset(documentId);
        return lastChangeset.map(c -> c.get("order").longValue()).orElse(0L);
    }

    private Optional<JsonNode> getDocument(String id) throws IOException {
        GetResponse response = client.prepareGet("steckbrief", "steckbrief", id).execute().actionGet();
        if (response.isExists()) {
            return Optional.of(mapper.readTree(response.getSourceAsString()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<JsonNode> getLastChangeset(String documentId) throws IOException {
        SearchResponse searchResponse = client.prepareSearch("changeset").setTypes("changeset")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("document", documentId))
                .addSort("order", SortOrder.DESC)
                .execute()
                .actionGet();
        if (searchResponse.getHits().getTotalHits() > 0) {
            return Optional.of(mapper.readTree(searchResponse.getHits().getAt(0).getSourceAsString()));
        } else {
            return Optional.empty();
        }
    }
}
