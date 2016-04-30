package io.pivio.server.changeset;

import com.fasterxml.jackson.databind.JsonNode;
import io.pivio.server.ElasticsearchQueryHelper;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@CrossOrigin
@RestController
public class ChangesetController {

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetController.class);

    private final Client client;
    private final ElasticsearchQueryHelper queryHelper;

    @Autowired
    public ChangesetController(Client client, ElasticsearchQueryHelper queryHelper) {
        this.client = client;
        this.queryHelper = queryHelper;
    }

    @RequestMapping(value = "/changeset", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode listAll(@RequestParam(required = false) String since, HttpServletResponse response) throws IOException {
        if (!isSinceParameterValid(since)) {
            LOG.info("Received changeset request with invalid since parameter in {} for all documents", since);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        LOG.debug("Retrieving changesets for all documents", since);
        return queryHelper.retrieveAllDocuments(client.prepareSearch("changeset")
                .setTypes("changeset")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addSort("timestamp", SortOrder.DESC)
                .setScroll(new TimeValue(60000))
                .setQuery(createQuery(since))
                .setSize(100));
    }

    @RequestMapping(value = "/document/{id}/changeset", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode get(@PathVariable String id, @RequestParam(required = false) String since, HttpServletResponse response) throws IOException {
        if (!queryHelper.isDocumentPresent("steckbrief", "steckbrief", id)) {
            LOG.info("Client wants to retrieve changesets for missing document with id {}", id);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        if (!isSinceParameterValid(since)) {
            LOG.info("Received changeset request with invalid since parameter in {} for document {}", since, id);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        LOG.debug("Retrieving changesets for document {} with since parameter {}", id, since);
        return queryHelper.retrieveAllDocuments(client.prepareSearch("changeset")
                .setTypes("changeset")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addSort("timestamp", SortOrder.DESC)
                .setScroll(new TimeValue(60000))
                .setQuery(createQuery(id, since))
                .setSize(100));
    }

    private boolean isSinceParameterValid(String since) {
        if (since == null) {
            return true;
        }
        if (since.length() < 2) {
            return false;
        }
        if (!(since.charAt(since.length() - 1) == 'd' || since.charAt(since.length() - 1) == 'w')) {
            return false;
        }

        try {
            int sinceValue = Integer.parseInt(since.substring(0, since.length() - 1));
            return sinceValue > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private QueryBuilder createQuery(String since) {
        if (since == null) {
            return QueryBuilders.matchAllQuery();
        } else {
            String sinceDate = calculateSinceDate(since);
            return QueryBuilders.rangeQuery("timestamp").gte(sinceDate).lte("now");
        }
    }

    private QueryBuilder createQuery(String id, String since) {
        if (since == null) {
            return QueryBuilders.matchQuery("document", id);
        } else {
            String sinceDate = calculateSinceDate(since);
            return QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("document", id))
                    .must(QueryBuilders.rangeQuery("timestamp").gte(sinceDate).lte("now"));
        }
    }

    private String calculateSinceDate(String since) {
        final DateTime sinceDate;
        if (since.charAt(since.length() - 1) == 'd') {
            sinceDate = DateTime.now().minusDays(Integer.parseInt(since.substring(0, since.length() - 1)));
        } else {
            sinceDate = DateTime.now().minusWeeks(Integer.parseInt(since.substring(0, since.length() - 1)));
        }
        return sinceDate.getYear() + "-" + sinceDate.getMonthOfYear() + "-" + sinceDate.getDayOfMonth();
    }
}
