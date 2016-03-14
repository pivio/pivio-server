package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.pivio.server.AppLauncher;
import io.pivio.server.TestHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URLEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(AppLauncher.class)
@WebIntegrationTest(randomPort = true)
public class SearchApiTest {

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private ElasticsearchTemplate elasticsearchTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final TestRestTemplate restTemplate = new TestRestTemplate();

  private ObjectNode teamLambdaQuery;

  @Before
  public void initializeTestData() {
    TestHelper.cleanElasticsearch(elasticsearchTemplate);

    addDocument(objectMapper.createObjectNode().put("id", "no1")
        .put("name", "User Service")
        .put("short_name", "USS")
        .put("type", "service")
        .put("description", RandomStringUtils.random(20))
        .put("team", "lambda")
        .put("newfield1", "test1"));
    addDocument(objectMapper.createObjectNode().put("id", "no2")
        .put("name", "Micro Service 2")
        .put("short_name", "MSS 2")
        .put("type", "service")
        .put("description", RandomStringUtils.random(20))
        .put("team", "lambda"));
    addDocument(objectMapper.createObjectNode().put("id", "no3")
        .put("name", "Micro Service 3")
        .put("short_name", "MSS")
        .put("type", "service")
        .put("description", RandomStringUtils.random(20))
        .put("team", "lambda")
        .put("newfield1", "test2")
        .put("newfield2", "test"));
    addDocument(objectMapper.createObjectNode().put("id", "no4")
        .put("name", "Service 4")
        .put("short_name", "MSS")
        .put("type", "service")
        .put("description", RandomStringUtils.random(20))
        .put("team", "other")
        .put("newfield1", "test3"));

    addDocument(objectMapper.createObjectNode().put("id", "nestedObject")
        .put("name", "Service 5")
        .put("short_name", "NOS")
        .put("type", "service")
        .put("description", RandomStringUtils.random(20))
        .put("team", "nestedTeam")
        .set("software_dependencies", objectMapper.createArrayNode()
            .add(objectMapper.createObjectNode()
                .put("name", "de.websitename:file.jar")
                .set("licences", objectMapper.createArrayNode()
                    .add(objectMapper.createObjectNode().put("key", "prop").put("fullName", "Some Proprietary License"))))
            .add(objectMapper.createObjectNode().set("licences", objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode().put("key", "apl").put("fullName", "Apache Public License"))
                .add(objectMapper.createObjectNode().put("key", "gpl").put("fullName", "GNU Public License"))))));

    addDocument(objectMapper.createObjectNode().put("id", "array")
        .put("name", "Service Array")
        .put("short_name", "ARR")
        .put("type", "service")
        .put("description", RandomStringUtils.random(20))
        .put("team", "arrayTeam")
        .set("arrayfield", objectMapper.createArrayNode().add("a").add("c").add("bb").add("bd")));

    addDocument(objectMapper.createObjectNode().put("id", "array2")
        .put("name", "Service Array 2")
        .put("short_name", "ARR2")
        .put("type", "service")
        .put("description", RandomStringUtils.random(20))
        .put("team", "arrayTeam")
        .set("arrayfield", objectMapper.createArrayNode().add("d").add("b").add("e")));

    elasticsearchTemplate.refresh(PivioDocument.class, true);

    teamLambdaQuery = objectMapper.createObjectNode();
    ObjectNode match = teamLambdaQuery.putObject("match");
    match.put("team", "lambda");
  }

  @Test
  public void returnsAllDocumentsMatchingSearchCriteria() throws IOException {
    ObjectNode searchQuery = objectMapper.createObjectNode();
    ObjectNode match = searchQuery.putObject("match");
    match.put("name", "Micro");

    ArrayNode searchResult = executeSearch(searchQuery, "", "");

    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsOnly("no2", "no3");
  }

  @Test
  public void onlyReturnsRequestedFieldsOfDocuments() throws IOException {
    ArrayNode searchResult = executeSearch(teamLambdaQuery, "newfield1,newfield2,", "");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsOnly("no1", "no2", "no3");
    assertThat(searchResult.get(0).fieldNames()).containsOnly("id", "newfield1", "newfield2");
  }

  @Test
  public void sortResultsDescendingByshort_nameField() throws IOException {
    ArrayNode searchResult = executeSearch(teamLambdaQuery, "newfield1", "short_name:desc");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("no1", "no3", "no2");
  }

  @Test
  public void sortResultsAscendingByshort_nameFieldWithCommaAfterFieldInParameter() throws IOException {
    ArrayNode searchResult = executeSearch(teamLambdaQuery, "newfield1", "short_name:asc,");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("no2", "no3", "no1");
  }

  @Test
  public void sortResultsAscendingByshort_nameAndDescendingByTeamField() throws IOException {
    ObjectNode searchQuery = objectMapper.createObjectNode();
    searchQuery.putObject("match_all");

    ArrayNode searchResult = executeSearch(searchQuery, "", "short_name:asc,team:desc");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("no2", "array", "array2", "no4", "no3", "nestedObject", "no1");
  }

  @Test
  public void sortWithNotExistingFieldShouldGiveEmptyResultSet() throws IOException {
    ObjectNode searchQuery = objectMapper.createObjectNode();
    searchQuery.putObject("match_all");

    ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document?query="
        + URLEncoder.encode(searchQuery.toString(), "UTF-8") + "&sort=notexisting:asc", JsonNode.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void queryForNestedObject() throws IOException {
    ObjectNode searchQuery = objectMapper.createObjectNode();
    ObjectNode nested = searchQuery.putObject("nested");
    nested.put("path", "software_dependencies.licences");
    ObjectNode nestedQuery = nested.putObject("query");
    ObjectNode matchQuery = nestedQuery.putObject("match");
    matchQuery.put("key", "gpl");

    ArrayNode searchResult = executeSearch(searchQuery, "", "");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("nestedObject");
  }

  @Test
  public void queryForValuesInArray() throws IOException {
    ObjectNode searchQuery = objectMapper.createObjectNode();
    ObjectNode matchQuery = searchQuery.putObject("match");
    matchQuery.put("arrayfield", "bd");

    ArrayNode searchResult = executeSearch(searchQuery, "", "");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("array");
  }

  @Test
  public void queryForValuesInManyArrays() throws IOException {
    ObjectNode searchQuery = objectMapper.createObjectNode();
    ObjectNode matchQuery = searchQuery.putObject("prefix");
    matchQuery.put("arrayfield", "b");

    ArrayNode searchResult = executeSearch(searchQuery, "", "");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("array", "array2");
  }

  @Test
  public void queryForValuesWithColon() throws IOException {
    ObjectNode searchQuery = objectMapper.createObjectNode();
    ObjectNode matchQuery = searchQuery.putObject("match");
    matchQuery.put("software_dependencies.name", "websitename");

    ArrayNode searchResult = executeSearch(searchQuery, "", "");
    assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("nestedObject");
  }

  @Test
  public void badRequestOnEmptyFieldsParameter() throws IOException {
    ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document?query="
        + URLEncoder.encode(teamLambdaQuery.toString(), "UTF-8") + "&fields=", JsonNode.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void badRequestOnEmptySortParameter() throws IOException {
    ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document?query="
        + URLEncoder.encode(teamLambdaQuery.toString(), "UTF-8") + "&sort=", JsonNode.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void badRequestOnSortParameterWithMissingColon() throws IOException {
    ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document?query="
        + URLEncoder.encode(teamLambdaQuery.toString(), "UTF-8") + "&sort=short_nameasc", JsonNode.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void badRequestOnSortParameterWithWrongSortOrder() throws IOException {
    ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("http://localhost:" + port + "/document?query="
        + URLEncoder.encode(teamLambdaQuery.toString(), "UTF-8") + "&sort=short_name:asce", JsonNode.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private void addDocument(JsonNode document) {
    ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity("http://localhost:" + port + "/document", document, JsonNode.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  private ArrayNode executeSearch(JsonNode searchQuery, String fieldParameter, String sortParameter) throws IOException {
    String searchParameter = URLEncoder.encode(searchQuery.toString(), "UTF-8");
    if (StringUtils.isNotEmpty(fieldParameter)) {
      searchParameter += "&fields=" + fieldParameter;
    }
    if (StringUtils.isNotEmpty(sortParameter)) {
      searchParameter += "&sort=" + sortParameter;
    }

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      HttpGet request = new HttpGet("http://localhost:" + port + "/document?query=" + searchParameter);
      request.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
      HttpResponse response = httpClient.execute(request);
      return (ArrayNode) objectMapper.readTree(EntityUtils.toString(response.getEntity(), "UTF-8"));
    }
  }
}
