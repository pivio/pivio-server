package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@CrossOrigin
@RestController
public class SearchQueryController {

    private static final Logger LOG = LoggerFactory.getLogger(SearchQueryController.class);

    private final Client client;
    private final ObjectMapper mapper;
    private final FieldFilter fieldFilter;
    private final Counter counter = Metrics.counter("counter.calls.document.get");

    public SearchQueryController(Client client, ObjectMapper mapper, FieldFilter fieldFilter) {
        this.client = client;
        this.mapper = mapper;
        this.fieldFilter = fieldFilter;
    }

    @GetMapping(value = "/document", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode search(@RequestParam(required = false) String query,
                           @RequestParam(required = false) String fields,
                           @RequestParam(required = false) String sort,
                           HttpServletResponse response) throws IOException {

        counter.increment();
        if (!isRequestValid(fields, sort)) {
            LOG.info("Received search query with invalid parameters, fields: {}, sort: {}", fields, sort);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        final SearchRequestBuilder searchRequest = client.prepareSearch("steckbrief")
                .setTypes("steckbrief")
                .setScroll(new TimeValue(60000))
                .setSize(100);

        if (StringUtils.isNotBlank(query)) {
            QueryBuilder queryBuilder = QueryBuilders.simpleQueryStringQuery(query);
            searchRequest.setQuery(queryBuilder);
        }
        else {
            searchRequest.setQuery(QueryBuilders.matchAllQuery());
        }

        if (sort != null) {
            String[] sortPairs = sort.split(",");
            for (String sortPair : sortPairs) {
                String[] sortPairConfig = sortPair.split(":");
                searchRequest.addSort(sortPairConfig[0], "asc".equalsIgnoreCase(sortPairConfig[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }

        try {
            SearchResponse searchResponse = searchRequest.execute().actionGet();
            List<String> filterForFields = new LinkedList<>();
            if (fields != null && fields.split(",").length > 0) {
                filterForFields.addAll(Arrays.asList(fields.split(",")));
                filterForFields.add("id");
            }

            ArrayNode searchResult = mapper.createArrayNode();
            while (true) {
                for (SearchHit searchHit : searchResponse.getHits().getHits()) {
                    JsonNode document = mapper.readTree(searchHit.getSourceAsString());
                    if (filterForFields.isEmpty()) {
                        searchResult.add(document);
                    }
                    else {
                        searchResult.add(fieldFilter.filterFields(document, filterForFields));
                    }
                }
                searchResponse = client.prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
                if (searchResponse.getHits().getHits().length == 0) {
                    break;
                }
            }
            return searchResult;
        }
        catch (ElasticsearchException e) {
            LOG.error("Could not execute search successfully, search request for ES: " + searchRequest.toString(), e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }

    private boolean isRequestValid(String fields, String sort) {
        if (fields != null && fields.trim().isEmpty()) {
            return false;
        }
        if (sort == null) {
            return true;
        }
        if (sort.trim().isEmpty()) {
            return false;
        }
        for (String sortPair : sort.split(",")) {
            String[] sortPairConfig = sortPair.split(":");
            if (sortPairConfig.length != 2) {
                return false;
            }
            if (!"asc".equalsIgnoreCase(sortPairConfig[1]) && !"desc".equalsIgnoreCase(sortPairConfig[1])) {
                return false;
            }
        }
        return true;
    }
}
