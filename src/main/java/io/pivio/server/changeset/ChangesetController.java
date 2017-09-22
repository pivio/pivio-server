package io.pivio.server.changeset;

import io.pivio.server.elasticsearch.ElasticsearchQueryHelper;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
public class ChangesetController {

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetController.class);

    private final Client client;
    private final ElasticsearchQueryHelper queryHelper;
    private final CounterService counterService;

    public ChangesetController(Client client, ElasticsearchQueryHelper queryHelper, CounterService counterService) {
        this.client = client;
        this.queryHelper = queryHelper;
        this.counterService = counterService;
    }

    @GetMapping(value = "/changeset", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listAll(@RequestParam(required = false) String since) throws IOException {
        counterService.increment("counter.calls.changeset.get");
        if (!isSinceParameterValid(since)) {
            LOG.info("Received changeset request with invalid since parameter in {} for all documents", since);
            return ResponseEntity.badRequest().build();
        }

        LOG.debug("Retrieving changesets for all documents", since);
        return ResponseEntity.ok(queryHelper.retrieveAllDocuments(client.prepareSearch("changeset")
                .setTypes("changeset")
                .addSort("timestamp", SortOrder.DESC)
                .setScroll(new TimeValue(60000))
                .setQuery(createQuery(since))
                .setSize(100)));
    }

    @GetMapping(value = "/document/{id}/changeset", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity get(@PathVariable String id, @RequestParam(required = false) String since) throws IOException {
        counterService.increment("counter.calls.document.id.changeset.get");

        if (!queryHelper.isDocumentPresent("steckbrief", "steckbrief", id)) {
            LOG.info("Client wants to retrieve changesets for missing document with id {}", id);
            return ResponseEntity.notFound().build();
        }

        if (!isSinceParameterValid(since)) {
            LOG.info("Received changeset request with invalid since parameter in {} for document {}", since, id);
            return ResponseEntity.badRequest().build();
        }

        LOG.debug("Retrieving changesets for document {} with since parameter {}", id, since);
        return ResponseEntity.ok(queryHelper.retrieveAllDocuments(client.prepareSearch("changeset")
                .setTypes("changeset")
                .addSort("timestamp", SortOrder.DESC)
                .setScroll(new TimeValue(60000))
                .setQuery(createQuery(id, since))
                .setSize(100)));
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
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private QueryBuilder createQuery(String since) {
        if (since == null) {
            return QueryBuilders.matchAllQuery();
        }
        else {
            String sinceDate = calculateSinceDate(since);
            return QueryBuilders.rangeQuery("timestamp").gte(sinceDate).lte("now");
        }
    }

    private QueryBuilder createQuery(String id, String since) {
        if (since == null) {
            return QueryBuilders.matchQuery("document", id);
        }
        else {
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
        }
        else {
            sinceDate = DateTime.now().minusWeeks(Integer.parseInt(since.substring(0, since.length() - 1)));
        }
        return ISODateTimeFormat.date().print(sinceDate);
    }
}
