package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.pivio.server.AbstractApiTestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.CREATED;

public class DocumentApiTest extends AbstractApiTestCase {

    @Ignore
    @Test
    public void document_can_be_created() throws Exception {
        // given
        PivioDocument document = createDocumentWithSomeId();

        // when
        ResponseEntity<PivioDocument> responseEntity = postDocument(document);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(CREATED);
        assertThat(responseEntity.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:9123/document/" + SOME_ID));
    }

    @Ignore
    @Test
    public void document_with_big_id_can_be_created() throws Exception {
        // given
        String bigDocumentId = Integer.MAX_VALUE + "" + Integer.MAX_VALUE + "" + Integer.MAX_VALUE + "" + Integer.MAX_VALUE;
        PivioDocument document = createDocumentWithId(bigDocumentId);

        // when
        ResponseEntity<PivioDocument> responseEntity = postDocument(document);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(CREATED);
    }

    @Ignore
    @Test
    public void document_with_empty_id_is_rejected() throws Exception {
        // given
        String emptyId = null;
        PivioDocument documentWithEmptyId = createDocumentWithId(emptyId);

        // when
        ResponseEntity<PivioDocument> responseEntity = postDocument(documentWithEmptyId);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getId()).isNotEmpty();
    }

    @Ignore
    @Test
    public void document_without_id_is_rejected() throws Exception {
        // given
        ObjectNode documentWithoutId = objectMapper.createObjectNode();
        documentWithoutId.put("type", "service");
        documentWithoutId.put("name", "MicroService");
        documentWithoutId.put("serviceName", "MS");
        documentWithoutId.put("description", "Super service...");
        documentWithoutId.put("owner", "Awesome Team");

        // when
        ResponseEntity<PivioDocument> responseEntity = postDocumentWithOtherResponseType(documentWithoutId);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getId()).isNotEmpty();
    }

    @Ignore
    @Test
    public void document_with_empty_mandatory_field_is_rejected() throws Exception {
        // given
        PivioDocument documentWithEmptyMandatoryField = createDocumentWithSomeId();
        documentWithEmptyMandatoryField.setName(null);

        // when
        ResponseEntity<PivioDocument> responseEntity = postDocument(documentWithEmptyMandatoryField);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Ignore
    @Test
    public void responds_with_meaningful_error_message_when_document_has_empty_mandatory_field() throws Exception {
        // given
        PivioDocument documentWithEmptyMandatoryField = createDocumentWithSomeId();
        documentWithEmptyMandatoryField.setName(null);

        // when
        ResponseEntity<JsonNode> responseEntity = postDocumentWithOtherResponseType(documentWithEmptyMandatoryField);

        // then
        assertThat(responseEntity.getBody().get("error").asText()).isEqualToIgnoringCase("mandatory field 'name' is empty");
    }

    @Ignore
    @Test
    public void document_with_missing_mandatory_field_is_rejected() throws Exception {
        // given
        JsonNode documentWithMissingMandatoryField = objectMapper.createObjectNode().put("id", "randomId").put("name", "Micro Service");

        // when
        ResponseEntity<PivioDocument> responseEntity = postDocumentWithOtherResponseType(documentWithMissingMandatoryField);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Ignore
    @Test
    public void responds_with_meaningful_error_message_when_document_has_missng_mandatory_field() throws Exception {
        // given
        JsonNode documentWithMissingMandatoryField = objectMapper.createObjectNode().put("id", "randomId").put("name", "Micro Service");

        // when
        ResponseEntity<JsonNode> responseEntity = postDocument(documentWithMissingMandatoryField);

        // then
        assertThat(responseEntity.getBody().get("error").asText()).isEqualToIgnoringCase("mandatory field 'type' is missing");
    }

    @Ignore
    @Test
    public void existent_document_can_be_requested() throws Exception {
        // given
        postDocumentWithSomeId();

        // when
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().get("id").asText()).isEqualTo(SOME_ID);
    }

    @Ignore
    @Test
    public void existent_documents_can_be_requested() throws Exception {
        // given
        postDocumentWithId("id1");
        postDocumentWithId("id2");

        // when
        ResponseEntity<JsonNode> responseEntity = getDocuments();

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(toList(responseEntity.getBody())).hasSize(2);
    }

    @Ignore
    @Test
    public void non_existent_document_cannot_be_requested() throws Exception {
        // when
        ResponseEntity<JsonNode> responseEntity = getDocumentWithId("nonExistentId");

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Ignore
    @Test
    public void created_field_is_added_to_document() throws Exception {
        // given
        postDocumentWithSomeId();

        // when
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();

        // then
        assertThat(responseEntity.getBody().has("created")).isTrue();
    }

    @Ignore
    @Test
    public void lastUpdate_field_is_added_to_document() throws Exception {
        // given
        postDocumentWithSomeId();

        // when
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();

        // then
        assertThat(responseEntity.getBody().has("lastUpdate")).isTrue();
    }

    @Ignore
    @Test
    public void lastUpload_field_is_added_to_document() throws Exception {
        // given
        postDocumentWithSomeId();

        // when
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();

        // then
        assertThat(responseEntity.getBody().has("lastUpload")).isTrue();
    }

    @Ignore
    @Test
    public void created_lastUpdate_and_lastUpload_fields_are_identical_for_a_new_document() throws Exception {
        // given
        postDocumentWithSomeId();

        // when
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();

        // then
        assertThat(responseEntity.getBody().get("lastUpdate").textValue()).isEqualTo(responseEntity.getBody().get("created").textValue());
        assertThat(responseEntity.getBody().get("lastUpload").textValue()).isEqualTo(responseEntity.getBody().get("lastUpdate").textValue());
    }

    @Ignore
    @Test
    public void lastUpdate_field_is_changed_when_document_has_changed() throws Exception {
        // given
        PivioDocument document = postDocumentWithSomeId();
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();
        String firstCreated = responseEntity.getBody().get("created").textValue();
        String firstLastUpdate = responseEntity.getBody().get("lastUpdate").textValue();
        String firstLastUpload = responseEntity.getBody().get("lastUpload").textValue();

        // when
        document.setOwner("User Team");
        postDocumentWithOtherResponseType(document);
        responseEntity = getDocumentWithSomeId();
        String secondCreated = responseEntity.getBody().get("created").textValue();
        String secondLastUpdate = responseEntity.getBody().get("lastUpdate").textValue();
        String secondLastUpload = responseEntity.getBody().get("lastUpload").textValue();

        // then
        assertThat(secondCreated).isEqualTo(firstCreated);
        assertThat(secondLastUpdate).isNotEqualTo(firstLastUpdate);
        assertThat(secondLastUpload).isNotEqualTo(firstLastUpload);
    }

    @Ignore
    @Test
    public void lastUpload_field_is_changed_when_the_same_document_is_resent() throws Exception {
        // given
        PivioDocument document = postDocumentWithSomeId();
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();
        String firstCreated = responseEntity.getBody().get("created").textValue();
        String firstLastUpdate = responseEntity.getBody().get("lastUpdate").textValue();
        String firstLastUpload = responseEntity.getBody().get("lastUpload").textValue();

        // when
        postDocumentWithOtherResponseType(document);
        responseEntity = getDocumentWithSomeId();
        String secondCreated = responseEntity.getBody().get("created").textValue();
        String secondLastUpdate = responseEntity.getBody().get("lastUpdate").textValue();
        String secondLastUpload = responseEntity.getBody().get("lastUpload").textValue();

        // then
        assertThat(secondCreated).isEqualTo(firstCreated);
        assertThat(secondLastUpdate).isEqualTo(firstLastUpdate);
        assertThat(secondLastUpload).isNotEqualTo(firstLastUpload);
    }

    @Ignore
    @Test
    public void document_can_be_deleted() throws Exception {
        // given
        postDocumentWithSomeId();

        // when
        deleteDocumentWithSomeId();

        // and
        ResponseEntity<JsonNode> responseEntity = getDocumentWithSomeId();

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private PivioDocument createDocumentWithSomeId() {
        return createDocumentWithId(SOME_ID);
    }

    private ResponseEntity<PivioDocument> postDocumentWithOtherResponseType(JsonNode document) {
        return postDocument(document, PivioDocument.class);
    }

    private ResponseEntity<JsonNode> postDocumentWithOtherResponseType(PivioDocument document) {
        return postDocument(document, JsonNode.class);
    }

    private ResponseEntity<JsonNode> getDocuments() {
        return restTemplate.getForEntity("/document", JsonNode.class);
    }

    private ResponseEntity<JsonNode> getDocumentWithSomeId() {
        return getDocumentWithId(SOME_ID);
    }

    private ResponseEntity<JsonNode> getDocumentWithId(String id) {
        return restTemplate.getForEntity("/document/{id}", JsonNode.class, id);
    }

    private void deleteDocumentWithSomeId() {
        restTemplate.delete("/document/someId");
    }

    private List<JsonNode> toList(JsonNode json) {
        List<JsonNode> documents = new LinkedList<>();
        for (Iterator<JsonNode> iterator = json.elements(); iterator.hasNext(); ) {
            documents.add(iterator.next());
        }
        return documents;
    }
}
