package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.pivio.server.changeset.Changeset;
import io.pivio.server.changeset.ChangesetService;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.elasticsearch.rest.RestStatus.NOT_FOUND;

@CrossOrigin
@RestController
@RequestMapping(value = "/document")
public class DocumentController {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentController.class);

    private final Client client;
    private final ChangesetService changesetService;
    private final ObjectMapper mapper;
    private final List<String> mandatoryFields;
    private final Counter documentPostCallsCounter;
    private final Counter documentIdGetCallsCounter;
    private final Counter documentIdDeleteCallsCounter;

    @Autowired
    public ObjectMapper objectMapper;

    public DocumentController(Client client, ChangesetService changesetService, ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.client = client;
        this.changesetService = changesetService;
        this.mapper = mapper;
        mandatoryFields = Arrays.asList("id", "type", "name", "owner", "description");
        documentPostCallsCounter = meterRegistry.counter("counter.calls.document.post");
        documentIdGetCallsCounter = meterRegistry.counter("counter.calls.document.id.get");
        documentIdDeleteCallsCounter = meterRegistry.counter("counter.calls.document.id.delete");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity create(@RequestBody ObjectNode document, UriComponentsBuilder uriBuilder) throws IOException {
        documentPostCallsCounter.increment();
        if (isIdMissingOrEmpty(document)) {
            return ResponseEntity.badRequest().body(missingIdError(document));
        }

        if (isMandatoryFieldMissingOrEmpty(document)) {
            return ResponseEntity.badRequest().body(missingMandatoryField(document));
        }

        removeNullNodes(document);


        final Changeset changeset = changesetService.computeNext(document);
        final String documentId = document.get("id").asText();
        final GetResponse persistedPivioDocument = client.prepareGet("steckbrief", "steckbrief", documentId).get();

        final String formattedChangeTime = ISODateTimeFormat.dateTime().print(changeset.getTimestamp());
        if (persistedPivioDocument.isExists()) {
            JsonNode persistentPivioDocumentJson = mapper.readTree(persistedPivioDocument.getSourceAsString());
            document.put("created", getFieldOrElse(persistentPivioDocumentJson, "created", formattedChangeTime));
            document.put("lastUpload", ISODateTimeFormat.dateTime().print(changeset.getTimestamp()));
            if (changeset.isEmpty()) {
                document.put("lastUpdate", getFieldOrElse(persistentPivioDocumentJson, "lastUpdate", formattedChangeTime));
            }
            else {
                document.put("lastUpdate", formattedChangeTime);
            }
        }
        else {
            document.put("created", formattedChangeTime);
            document.put("lastUpdate", formattedChangeTime);
            document.put("lastUpload", formattedChangeTime);
        }

        client.prepareIndex("steckbrief", "steckbrief", documentId)
                .setSource(document.toString(), XContentType.JSON)
                .get();

        if (changeset.isNotEmpty()) {
            client.prepareIndex("changeset", "changeset")
                    .setSource(mapper.writeValueAsString(changeset), XContentType.JSON)
                    .setCreate(true)
                    .get();
        }

        LOG.info("Indexed document {} for {}", documentId, document.get("name").asText());
        return ResponseEntity.created(uriBuilder.path("/document/{documentId}").buildAndExpand(documentId).toUri()).build();
    }

    private JsonNode removeNullNodes(JsonNode node) {
        Iterator<JsonNode> iterator = node.iterator();
        while (iterator.hasNext()) {
            JsonNode next = iterator.next();
            if (next.getNodeType().equals(JsonNodeType.NULL)) {
                iterator.remove();
            }
            if (next.getNodeType().equals(JsonNodeType.ARRAY) || next.getNodeType().equals(JsonNodeType.OBJECT)) {
                JsonNode jsonNode = removeNullNodes(next);
                if (!jsonNode.iterator().hasNext()) {
                    iterator.remove();
                }
            }
        }
        return node;
    }

    private JsonNode missingMandatoryField(JsonNode document) {
        ObjectNode error = mapper.createObjectNode();
        String missingMandatoryField = getMissingMandatoryField(document);
        if (missingMandatoryField != null) {
            LOG.info("Received document with missing mandatory field in {}", document.toString());
            error.put("error", "mandatory field '" + missingMandatoryField + "' is missing");
        }
        else {
            LOG.info("Received document with empty mandatory field in {}", document.toString());
            error.put("error", "mandatory field '" + getEmptyMandatoryField(document) + "' is empty");
        }
        return error;
    }

    private JsonNode missingIdError(JsonNode document) {
        LOG.info("Received document without or with empty id field in {}", document.toString());
        ObjectNode newId = mapper.createObjectNode();
        newId.put("id", UUID.randomUUID().toString());
        return newId;
    }

    private boolean isIdMissingOrEmpty(JsonNode document) {
        return document.get("id") == null || StringUtils.isEmpty(document.get("id").asText(""));
    }

    private String getFieldOrElse(JsonNode json, String fieldName, String defaultValue) {
        return json.has(fieldName) ? json.get(fieldName).textValue() : defaultValue;
    }

    private boolean isMandatoryFieldMissingOrEmpty(JsonNode document) {
        return getMissingMandatoryField(document) != null || getEmptyMandatoryField(document) != null;
    }

    private String getMissingMandatoryField(JsonNode document) {
        for (String field : mandatoryFields) {
            if (!document.has(field)) {
                return field;
            }
        }
        return null;
    }

    private String getEmptyMandatoryField(JsonNode document) {
        for (String field : mandatoryFields) {
            if (document.has(field) && StringUtils.isEmpty(document.get(field).asText(""))) {
                return field;
            }
        }
        return null;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity get(@PathVariable String id) throws IOException {
        GetResponse getResponse = client.prepareGet("steckbrief", "steckbrief", id).get();
        if (!getResponse.isExists()) {
            return ResponseEntity.notFound().build();
        }
        documentIdGetCallsCounter.increment();
        return ResponseEntity.ok(mapper.readTree(getResponse.getSourceAsString()));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity delete(@PathVariable String id) throws IOException {
        LOG.info("Try to delete document {}", id);
        documentIdDeleteCallsCounter.increment();
        DeleteResponse deleteResponse = client.prepareDelete("steckbrief", "steckbrief", id).get();
        if (deleteResponse.status() == NOT_FOUND) {
            LOG.warn("Could not delete document {}", id);
            return ResponseEntity.notFound().build();
        }
        DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .filter(QueryBuilders.matchQuery("document", id))
                .source("changeset")
                .get();
        LOG.info("Deleted document {} successfully", id);
        return ResponseEntity.noContent().build();
    }
}
