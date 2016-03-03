package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.pivio.server.changeset.Changeset;
import io.pivio.server.changeset.ChangesetService;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping(value = "/document")
public class DocumentController {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentController.class);

  private final Client client;
  private final ChangesetService changesetService;
  private final ObjectMapper mapper;
  private final List<String> mandatoryFields;

  @Autowired
  public DocumentController(Client client, ChangesetService changesetService, ObjectMapper mapper) {
    this.client = client;
    this.changesetService = changesetService;
    this.mapper = mapper;
    mandatoryFields = Arrays.asList("id", "type", "name", "team", "description");
  }

  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode create(@RequestBody ObjectNode document, HttpServletResponse response) throws IOException {
    if (isIdMissingOrEmpty(document)) {
      return missingIdError(document, response);
    }

    if (isMandatoryFieldMissingOrEmpty(document)) {
      return missingMandatoryField(document, response);
    }

    final Changeset changeset = changesetService.computeNext(document);
    final String documentId = document.get("id").asText();
    final GetResponse persistedPivioDocument = client.prepareGet("steckbrief", "steckbrief", documentId)
        .execute()
        .actionGet();

    final String formattedChangeTime = ISODateTimeFormat.dateTime().print(changeset.getTimestamp());
    if (persistedPivioDocument.isExists()) {
      JsonNode persistentPivioDocumentJson = mapper.readTree(persistedPivioDocument.getSourceAsString());
      document.put("created", getFieldOrElse(persistentPivioDocumentJson, "created", formattedChangeTime));
      document.put("lastUpload", ISODateTimeFormat.dateTime().print(changeset.getTimestamp()));
      if (changeset.isEmpty()) {
        document.put("lastUpdate", getFieldOrElse(persistentPivioDocumentJson, "lastUpdate", formattedChangeTime));
      } else {
        document.put("lastUpdate", formattedChangeTime);
      }
    } else {
      document.put("created", formattedChangeTime);
      document.put("lastUpdate", formattedChangeTime);
      document.put("lastUpload", formattedChangeTime);
    }

    client.prepareIndex("steckbrief", "steckbrief", documentId)
        .setSource(document.toString())
        .execute()
        .actionGet();

    if (changeset.isNotEmpty()) {
      client.prepareIndex("changeset", "changeset")
          .setSource(mapper.writeValueAsString(changeset))
          .setCreate(true)
          .execute()
          .actionGet();
    }

    LOG.info("Indexed document {} for {}", documentId, document.get("name").asText());
    response.setStatus(HttpServletResponse.SC_CREATED);
    return null;
  }

  private JsonNode missingMandatoryField(@RequestBody ObjectNode document, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    ObjectNode error = mapper.createObjectNode();
    String missingMandatoryField = getMissingMandatoryField(document);
    if (missingMandatoryField != null) {
      LOG.info("Received document with missing mandatory field in {}", document.toString());
      error.put("error", "mandatory field '" + missingMandatoryField + "' is missing");
    } else {
      LOG.info("Received document with empty mandatory field in {}", document.toString());
      error.put("error", "mandatory field '" + getEmptyMandatoryField(document) + "' is empty");
    }
    return error;
  }

  private JsonNode missingIdError(@RequestBody ObjectNode document, HttpServletResponse response) {
    LOG.info("Received document without or with empty id field in {}", document.toString());
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    ObjectNode newId = mapper.createObjectNode();
    newId.put("id", UUID.randomUUID().toString());
    return newId;
  }

  private boolean isIdMissingOrEmpty(@RequestBody ObjectNode document) {
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

  @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode get(@PathVariable String id, HttpServletResponse response) throws IOException {
    GetResponse getResponse = client.prepareGet("steckbrief", "steckbrief", id)
        .execute()
        .actionGet();

    if (!getResponse.isExists()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return null;
    }

    return mapper.readTree(getResponse.getSourceAsString());
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  public void delete(@PathVariable String id) throws IOException {
    LOG.info("Try to delete document {}", id);
    if (client.prepareDelete("steckbrief", "steckbrief", id).execute().actionGet().isFound()) {
      client.prepareDeleteByQuery("changeset").setTypes("changeset")
          .setQuery(QueryBuilders.matchQuery("document", id))
          .execute()
          .actionGet();
      LOG.info("Deleted document {} successfully", id);
    } else {
      LOG.warn("Could not delete document {}", id);
    }
  }
}
