package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.pivio.server.TestHelper;
import io.pivio.server.changeset.Changeset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DocumentApiTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void cleanData() {
        TestHelper.cleanElasticsearch(elasticsearchTemplate);
    }

    @Test
    public void insertNewDocument() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        ResponseEntity<PivioDocument> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseEntity.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:" + port + "/document/randomId"));
    }

    @Test
    public void insertDocumentWithBigNumberAsIDShouldNotFail() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder()
                .id(Integer.MAX_VALUE + "" + Integer.MAX_VALUE + "" + Integer.MAX_VALUE + "" + Integer.MAX_VALUE)
                .type("service")
                .name("MicroService")
                .serviceName("MS")
                .description("Super service...")
                .owner("Awesome Team")
                .build();
        ResponseEntity<PivioDocument> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    public void shouldRejectNewDocumentWithEmptyIdFieldInRequest() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        ResponseEntity<PivioDocument> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getId()).isNotEmpty();
    }

    @Test
    public void shouldRejectNewDocumentWithoutIdFieldInRequest() throws Exception {
        final ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "service");
        document.put("name", "MicroService");
        document.put("serviceName", "MS");
        document.put("description", "Super service...");
        document.put("owner", "Awesome Team");

        ResponseEntity<PivioDocument> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", document, PivioDocument.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getId()).isNotEmpty();
    }

    @Test
    public void queryInsertedDocument() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().get("id").asText()).isEqualTo(newPivioDocument.getId());
    }

    @Test
    public void shouldAddCreatedFieldForNewDocument() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getBody().has("created")).isTrue();
    }

    @Test
    public void shouldAddLastUpdateFieldForNewDocument() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getBody().has("lastUpdate")).isTrue();
    }

    @Test
    public void shouldAddLastUploadedFieldForNewDocument() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getBody().has("lastUpload")).isTrue();
    }

    @Test
    public void createdLastUploadAndLastUpdateShouldBeSameForNewInsertedDocument() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getBody().get("lastUpload").textValue()).isEqualTo(responseEntity.getBody().get("created").textValue());
        assertThat(responseEntity.getBody().get("lastUpload").textValue()).isEqualTo(responseEntity.getBody().get("lastUpdate").textValue());
        assertThat(responseEntity.getBody().get("created").textValue()).isEqualTo(responseEntity.getBody().get("lastUpdate").textValue());
    }

    @Test
    public void shouldUpdateLastUploadedFieldAlsoWhenNothingChanges() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);
        String lastUpload = responseEntity.getBody().get("lastUpload").textValue();
        String lastUpdate = responseEntity.getBody().get("lastUpdate").textValue();

        elasticsearchTemplate.refresh(Changeset.class);
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);
        responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getBody().get("lastUpload").textValue()).isNotEqualTo(lastUpload);
        assertThat(responseEntity.getBody().get("lastUpdate").textValue()).isEqualTo(lastUpdate);
    }

    @Test
    public void shouldUpdateLastUpdatedFieldOnlyWhenSomethingChanges() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);
        String lastUpload = responseEntity.getBody().get("lastUpload").textValue();
        String lastUpdate = responseEntity.getBody().get("lastUpdate").textValue();

        newPivioDocument.setOwner("User Team");
        elasticsearchTemplate.refresh(Changeset.class);
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);
        responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getBody().get("lastUpload").textValue()).isNotEqualTo(lastUpload);
        assertThat(responseEntity.getBody().get("lastUpdate").textValue()).isNotEqualTo(lastUpdate);
    }

    @Test
    public void retrieveAllDocuments() throws Exception {
        PivioDocument firstDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        PivioDocument secondDocument = PivioDocument.builder().id("randomId2").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();

        restTemplate.postForEntity("http://localhost:" + port + "/document", firstDocument, PivioDocument.class);
        restTemplate.postForEntity("http://localhost:" + port + "/document", secondDocument, PivioDocument.class);
        elasticsearchTemplate.refresh(PivioDocument.class);

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document", JsonNode.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(toList(responseEntity.getBody())).hasSize(2);
    }

    @Test
    public void notFoundForNotExistingDocuments() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        ResponseEntity<JsonNode> responseEntity = restTemplate.exchange("http://localhost:" + port + "/document/notExisting", HttpMethod.GET, new HttpEntity<>(null, headers), JsonNode.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void deleteInsertedDocument() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").name("MicroService").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);

        restTemplate.delete("http://localhost:" + port + "/document/randomId");

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId", JsonNode.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldRejectDocumentWithEmptyMandatoryField() throws Exception {
        PivioDocument newPivioDocument = PivioDocument.builder().id("randomId").type("service").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        ResponseEntity<PivioDocument> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", newPivioDocument, PivioDocument.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void shouldRejectDocumentWithMissingMandatoryField() throws Exception {
        JsonNode pivioDocumentJson = objectMapper.createObjectNode().put("id", "randomId").put("name", "Micro Service");
        ResponseEntity<PivioDocument> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", pivioDocumentJson, PivioDocument.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void shouldRespondWithMeaningfulErrorMessageOnEmptyMandatoryField() throws Exception {
        PivioDocument newDocument = PivioDocument.builder().id("randomId").type("service").name("").serviceName("MS").description("Super service...").owner("Awesome Team").build();
        ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", newDocument, JsonNode.class);
        assertThat(responseEntity.getBody().get("error").asText()).isEqualToIgnoringCase("mandatory field 'name' is empty");
    }

    @Test
    public void shouldRespondWithMeaningfulErrorMessageOnMissingMandatoryField() throws Exception {
        JsonNode newDocument = objectMapper.createObjectNode().put("id", "randomId").put("name", "Micro Service");
        ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", newDocument, JsonNode.class);
        assertThat(responseEntity.getBody().get("error").asText()).isEqualToIgnoringCase("mandatory field 'type' is missing");
    }

    private List<JsonNode> toList(JsonNode json) {
        List<JsonNode> documents = new LinkedList<>();
        for (Iterator<JsonNode> iterator = json.elements(); iterator.hasNext(); ) {
            documents.add(iterator.next());
        }
        return documents;
    }
}
