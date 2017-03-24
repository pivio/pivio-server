package io.pivio.server.changeset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import io.pivio.server.AppLauncher;
import io.pivio.server.TestHelper;
import io.pivio.server.document.PivioDocument;
import net.minidev.json.JSONArray;
import org.elasticsearch.client.Client;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(AppLauncher.class)
@WebIntegrationTest(randomPort = true)
public class ChangesetApiTest {

    private static final String ADD_OPERATION = "add";
    private static final String REPLACE_OPERATION = "replace";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private Client client;

    @Autowired
    private ObjectMapper mapper;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private ObjectNode document;

    @Before
    public void setup() {
        TestHelper.cleanElasticsearch(elasticsearchTemplate);

        document = mapper.createObjectNode()
                .put("id", "randomId")
                .put("type", "service")
                .put("name", "MicroService")
                .put("serviceName", "MS")
                .put("description", "Super service...")
                .put("owner", "Awesome Team");
    }

    @Test
    public void firstInsertOfDocumentShouldResultInFirstChangeset() {
        addDocument(document);
        List<JsonNode> changesets = retrieveChangesets("randomId");
        assertThat(changesets).hasSize(1);
    }

    @Test
    public void firstChangesetShouldContainAllFields() {
        addDocument(document);

        List<JsonNode> changesets = retrieveChangesets("randomId");
        JsonNode changedFields = changesets.get(0).get("fields");

        assertThat(changedFields.findValues("path").stream().map(JsonNode::textValue))
                .containsOnly("/id", "/type", "/serviceName", "/name", "/description", "/owner");
    }

    @Test
    public void changesetShouldConsistOfDiffForAllChangedFields() {
        addDocument(document);

        List<JsonNode> changesets = retrieveChangesets("randomId");
        JsonNode firstChangeset = changesets.get(0);

        assertThatFieldHasCorrectDiff(firstChangeset, "/id", "randomId", ADD_OPERATION);
        assertThatFieldHasCorrectDiff(firstChangeset, "/type", "service", ADD_OPERATION);
        assertThatFieldHasCorrectDiff(firstChangeset, "/name", "MicroService", ADD_OPERATION);
        assertThatFieldHasCorrectDiff(firstChangeset, "/serviceName", "MS", ADD_OPERATION);
        assertThatFieldHasCorrectDiff(firstChangeset, "/description", "Super service...", ADD_OPERATION);
        assertThatFieldHasCorrectDiff(firstChangeset, "/owner", "Awesome Team", ADD_OPERATION);
    }

    @Test
    public void changesetForChangedValuesInArray() {
        addDocument(document.set("provides", mapper.createArrayNode().add("aaa").add("bbb").add("ccc")));
        addDocument(document.set("provides", mapper.createArrayNode().add("aaa").add("d")));

        List<JsonNode> changesets = retrieveChangesets("randomId");
        JsonNode firstChangeset = changesets.get(0);

        assertThatFieldHasCorrectDiff(firstChangeset, "/provides/1", "d", REPLACE_OPERATION);
        List<String> jsonOperationValue = JsonPath.read(firstChangeset.toString(), "$.fields[?(@.path == '/provides/2')].op");
        assertThat(jsonOperationValue.get(0)).isEqualTo("remove");
    }

    @Test
    public void changesetForNestedObject() {
        document = (ObjectNode) document.set("dependencies", mapper.createObjectNode().put("name", "de.websitename:file.jar"));
        addDocument(document);
        document = (ObjectNode) document.set("dependencies", mapper.createObjectNode().put("name", "file.jar"));
        addDocument(document);

        List<JsonNode> changesets = retrieveChangesets("randomId");
        JsonNode firstChangeset = changesets.get(0);

        assertThatFieldHasCorrectDiff(firstChangeset, "/dependencies/name", "file.jar", REPLACE_OPERATION);
    }

    @Test
    public void changesetShouldReferenceChangedDocument() {
        addDocument(document);

        List<JsonNode> changesets = retrieveChangesets("randomId");
        JsonNode firstChangeset = changesets.get(0);

        assertThat(firstChangeset.get("document").textValue()).isEqualTo("randomId");
    }

    @Test
    public void notFailOnCorruptStorageWhenDocumentAlreadyExistsButHasNoFirstChangeset() throws JsonProcessingException {
        persistDocument(document);
        addDocument(document.put("name", "Other"));
        List<JsonNode> changesets = retrieveChangesets("randomId");
        assertThat(changesets).hasSize(1);
    }

    @Test
    public void notPersistChangesetIfNothingChanged() throws JsonProcessingException {
        addDocument(document);
        addDocument(document);
        List<JsonNode> changesets = retrieveChangesets("randomId");
        assertThat(changesets).hasSize(1);
    }

    @Test
    public void changesetShouldOnlyHaveDiffsOfChangeFields() {
        addDocument(document);
        addDocument(document.put("name", "NewService").put("owner", "User Team"));

        String changesets = retrieveChangesetsAsString("randomId");
        JSONArray changeset = JsonPath.read(changesets, "$.[?(@.order == 2)]");
        assertThat(changeset).hasSize(1);

        JSONArray changedFields = JsonPath.read(changeset.get(0), "$.fields");
        assertThat(changedFields).hasSize(2);

        assertThatFieldHasCorrectDiff(changeset.get(0), "/name", "NewService", REPLACE_OPERATION);
        assertThatFieldHasCorrectDiff(changeset.get(0), "/owner", "User Team", REPLACE_OPERATION);
    }

    @Test
    public void orderNumberShouldBeIncrementedPerChangeset() {
        addDocument(document);
        document = document.put("name", "NewService");
        addDocument(document);
        document = document.put("owner", "User Team");
        addDocument(document);

        String changesets = retrieveChangesetsAsString("randomId");
        assertThat(((JSONArray) JsonPath.read(changesets, "$.[?(@.order == 1)]"))).hasSize(1);
        assertThat(((JSONArray) JsonPath.read(changesets, "$.[?(@.order == 2)]"))).hasSize(1);
        assertThat(((JSONArray) JsonPath.read(changesets, "$.[?(@.order == 3)]"))).hasSize(1);
        assertThat(((JSONArray) JsonPath.read(changesets, "$.[?(@.order == 4)]"))).hasSize(0);
    }

    @Test
    public void listChangesetsOfAllDocuments() {
        addDocument(document);
        addDocument(document.put("name", "NewService"));

        addDocument(mapper.createObjectNode()
                .put("id", "otherId")
                .put("type", "service")
                .put("name", "MicroService")
                .put("serviceName", "MS")
                .put("description", "Super service...")
                .put("owner", "Awesome Team"));

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/changeset", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().isArray()).isTrue();
        assertThat(toList(responseEntity.getBody())).hasSize(3);
    }

    @Test
    public void deleteOfDocumentShouldDeleteAllCorrespondingChangesets() {
        addDocument(document);
        addDocument(document.put("name", "NewService"));

        restTemplate.delete("http://localhost:" + port + "/document/randomId");

        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId/changeset", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void notFoundForRetrievingChangesetsOfNotExistingDocument() {
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/notExisting/changeset", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void returnsChangesetsForLast7Days() throws JsonProcessingException {
        Changeset oneDayAgo = createChangesetDaysAgo(3L, 1);
        Changeset twoDaysAgo = createChangesetDaysAgo(2L, 2);
        persistDocument(document);
        persistChangesets(oneDayAgo, twoDaysAgo, createChangesetDaysAgo(1L, 8));
        expectChangesetsSince("7d", oneDayAgo, twoDaysAgo);
    }

    @Test
    public void returnsChangesetsForLast7DaysOfAllDocuments() throws JsonProcessingException {
        Changeset oneDayAgo = createChangesetDaysAgo(3L, 1);
        Changeset twoDaysAgo = createChangesetDaysAgo("otherId", 2L, 2);
        persistDocument(document);
        persistDocument(mapper.createObjectNode()
                .put("id", "otherId")
                .put("type", "service")
                .put("name", "MicroService2")
                .put("serviceName", "MS2")
                .put("description", "Super service...")
                .put("owner", "Awesome Team"));
        persistChangesets(oneDayAgo, twoDaysAgo, createChangesetDaysAgo(1L, 8));
        expectChangesets("http://localhost:" + port + "/changeset?since=7d", oneDayAgo, twoDaysAgo);
    }

    @Test
    public void returnsChangesetsForLast3Weeks() throws JsonProcessingException {
        Changeset oneWeekAgo = createChangesetDaysAgo(3L, 7);
        Changeset twoWeeksAgo = createChangesetDaysAgo(2L, 14);
        persistDocument(document);
        persistChangesets(oneWeekAgo, twoWeeksAgo, createChangesetDaysAgo(1L, 28));
        expectChangesetsSince("3w", oneWeekAgo, twoWeeksAgo);
    }

    @Test
    public void badRequestForEmptySinceParameter() throws JsonProcessingException {
        persistDocument(document);
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId/changeset?since=", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void badRequestForSinceParameterWithoutSuffix() throws JsonProcessingException {
        persistDocument(document);
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId/changeset?since=32", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void badRequestForSinceParameterWithoutProperSuffix() throws JsonProcessingException {
        persistDocument(document);
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId/changeset?since=32h", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void badRequestForSinceParameterWithNegativeNumber() throws JsonProcessingException {
        persistDocument(document);
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId/changeset?since=-3d", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void badRequestForSinceParameterWithZeroAsNumber() throws JsonProcessingException {
        persistDocument(document);
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/randomId/changeset?since=0w", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void expectChangesetsSince(String sinceParameter, Changeset... expectedChangesets) {
        expectChangesets("http://localhost:" + port + "/document/" + document.get("id").asText() + "/changeset?since=" + sinceParameter, expectedChangesets);
    }

    private void expectChangesets(String url, Changeset... expectedChangesets) {
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(url, JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().isArray()).isTrue();
        List<JsonNode> receivedChangesets = toList(responseEntity.getBody());
        assertThat(receivedChangesets).hasSize(expectedChangesets.length);
        assertThat(receivedChangesets.stream().map(c -> c.get("order").longValue()))
                .containsExactly(Arrays.asList(expectedChangesets).stream().map(Changeset::getOrder).collect(Collectors.toList()).toArray(new Long[0]));
    }

    private void persistDocument(JsonNode document) throws JsonProcessingException {
        client.prepareIndex("steckbrief", "steckbrief", document.get("id").asText())
                .setSource(document.toString())
                .execute()
                .actionGet();
        elasticsearchTemplate.refresh(PivioDocument.class);
    }

    private void persistChangesets(Changeset... changesets) throws JsonProcessingException {
        for (Changeset changeset : changesets) {
            client.prepareIndex("changeset", "changeset")
                    .setSource(mapper.writeValueAsString(changeset))
                    .setCreate(true)
                    .execute()
                    .actionGet();
        }
        elasticsearchTemplate.refresh(Changeset.class);
    }

    private Changeset createChangesetDaysAgo(long order, int daysAgo) {
        return createChangesetDaysAgo(document.get("id").asText(), order, daysAgo);
    }

    private Changeset createChangesetDaysAgo(String document, long order, int daysAgo) {
        Changeset changeset = new Changeset(document, order, mapper.createArrayNode());
        changeset.setTimestamp(DateTime.now().minusDays(daysAgo));
        return changeset;
    }

    private void assertThatFieldHasCorrectDiff(JsonNode changeset, String fieldKey, String value, String operation) {
        List<String> jsonPreviousValue = JsonPath.read(changeset.toString(), "$.fields[?(@.path == '" + fieldKey + "')].value");
        assertThat(jsonPreviousValue.get(0)).isEqualTo(value);
        List<String> jsonOperationValue = JsonPath.read(changeset.toString(), "$.fields[?(@.path == '" + fieldKey + "')].op");
        assertThat(jsonOperationValue.get(0)).isEqualTo(operation);
    }

    private void assertThatFieldHasCorrectDiff(Object changeset, String fieldKey, String value, String operation) {
        List<String> jsonPreviousValue = JsonPath.read(changeset, "$.fields[?(@.path == '" + fieldKey + "')].value");
        assertThat(jsonPreviousValue.get(0)).isEqualTo(value);
        List<String> jsonOperationValue = JsonPath.read(changeset, "$.fields[?(@.path == '" + fieldKey + "')].op");
        assertThat(jsonOperationValue.get(0)).isEqualTo(operation);
    }

    private void addDocument(JsonNode document) {
        restTemplate.postForEntity("http://localhost:" + port + "/document", document, JsonNode.class);
        elasticsearchTemplate.refresh(PivioDocument.class);
        elasticsearchTemplate.refresh(Changeset.class);
    }

    private List<JsonNode> retrieveChangesets(String id) {
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/" + id + "/changeset", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().isArray()).isTrue();
        return toList(responseEntity.getBody());
    }

    private String retrieveChangesetsAsString(String id) {
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document/" + id + "/changeset", JsonNode.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().isArray()).isTrue();
        return responseEntity.getBody().toString();
    }

    private List<JsonNode> toList(JsonNode changesetsJson) {
        List<JsonNode> changesets = new LinkedList<>();
        for (Iterator<JsonNode> iterator = changesetsJson.elements(); iterator.hasNext(); ) {
            changesets.add(iterator.next());
        }
        return changesets;
    }
}
