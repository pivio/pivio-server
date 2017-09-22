package io.pivio.server.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.pivio.server.AbstractApiTestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URLEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class SearchApiTest extends AbstractApiTestCase {

    @Before
    public void setUpTestData() {
        RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder().build();

        postDocument(objectMapper.createObjectNode().put("id", "no1")
                .put("name", "User Service")
                .put("short_name", "USS")
                .put("type", "service")
                .put("description", randomStringGenerator.generate(20))
                .put("owner", "lambda")
                .put("newfield1", "test1"));

        postDocument(objectMapper.createObjectNode().put("id", "no2")
                .put("name", "Micro Service 2")
                .put("short_name", "MSS 2")
                .put("type", "service")
                .put("description", randomStringGenerator.generate(20))
                .put("owner", "lambda"));

        postDocument(objectMapper.createObjectNode().put("id", "no3")
                .put("name", "Micro Service 3")
                .put("short_name", "MSS")
                .put("type", "service")
                .put("description", randomStringGenerator.generate(20))
                .put("owner", "lambda")
                .put("newfield1", "test2")
                .put("newfield2", "test"));

        postDocument(objectMapper.createObjectNode().put("id", "no4")
                .put("name", "Service 4")
                .put("short_name", "MSS")
                .put("type", "service")
                .put("description", randomStringGenerator.generate(20))
                .put("owner", "other")
                .put("newfield1", "test3"));

        postDocument(objectMapper.createObjectNode().put("id", "nestedObject")
                .put("name", "Service 5")
                .put("short_name", "NOS")
                .put("type", "service")
                .put("description", randomStringGenerator.generate(20))
                .put("owner", "nestedTeam")
                .set("software_dependencies", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("name", "de.websitename:file.jar")
                                .set("licenses", objectMapper.createArrayNode()
                                        .add(objectMapper.createObjectNode().put("key", "prop").put("fullName", "Some Proprietary License"))))
                        .add(objectMapper.createObjectNode().set("licenses", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode().put("key", "apl").put("fullName", "Apache Public License"))
                                .add(objectMapper.createObjectNode().put("key", "gpl").put("fullName", "GNU Public License"))))));

        postDocument(objectMapper.createObjectNode().put("id", "array")
                .put("name", "Service Array")
                .put("short_name", "ARR")
                .put("type", "service")
                .put("description", randomStringGenerator.generate(20))
                .put("owner", "arrayTeam")
                .set("arrayfield", objectMapper.createArrayNode().add("a").add("c").add("bb").add("bd")));

        postDocument(objectMapper.createObjectNode().put("id", "array2")
                .put("name", "Service Array 2")
                .put("short_name", "ARR2")
                .put("type", "service")
                .put("description", randomStringGenerator.generate(20))
                .put("owner", "arrayTeam")
                .set("arrayfield", objectMapper.createArrayNode().add("d").add("b").add("e")));
    }

    @Test
    public void search_returns_documents_matching_search_criteria() throws Exception {
        // given
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode match = query.putObject("match");
        match.put("name", "Micro");

        // when
        ArrayNode searchResult = search(query);

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsOnly("no2", "no3");
    }

    @Test
    public void search_returns_documents_containing_only_requested_fields() throws Exception {
        // given
        ObjectNode ownerLambdaQuery = createOwnerLambdaQuery();

        // when
        ArrayNode searchResult = search(ownerLambdaQuery, "newfield1,newfield2,", "");

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsOnly("no1", "no2", "no3");
        assertThat(searchResult.get(2).fieldNames()).containsOnly("id", "newfield1", "newfield2");
    }

    @Test
    public void search_returns_documents_sorted_descending_by_short_name() throws Exception {
        // given
        ObjectNode ownerLambdaQuery = createOwnerLambdaQuery();

        // when
        ArrayNode searchResult = search(ownerLambdaQuery, "newfield1", "short_name:desc");

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("no1", "no2", "no3");
    }

    @Test
    public void search_returns_documents_sorted_ascending_by_short_name_even_when_a_comma_follows_in_sort_parameter() throws Exception {
        // given
        ObjectNode ownerLambdaQuery = createOwnerLambdaQuery();

        // when
        ArrayNode searchResult = search(ownerLambdaQuery, "newfield1", "short_name:asc,");

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("no2", "no3", "no1");
    }

    @Test
    public void search_returns_documents_sorted_ascending_by_short_name_and_descending_by_owner() throws Exception {
        // given
        ObjectNode query = objectMapper.createObjectNode();
        query.putObject("match_all");

        // when
        ArrayNode searchResult = search(query, "", "short_name:asc,owner:desc");

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("no2", "array", "array2", "no4", "no3", "nestedObject", "no1");
    }

    /**
     * <pre>
     * {
     *   "query": {
     *     "nested": {
     *       "path": "software_dependencies",
     *       "query": {
     *         "nested": {
     *           "path": "software_dependencies.licenses",
     *           "query": {
     *             "match": {
     *               "software_dependencies.licenses.key": "gpl"
     *             }
     *           }
     *         }
     *       }
     *     }
     *   }
     * }
     * </pre>
     */
    @Test
    public void search_returns_document_matching_nested_query() throws Exception {
        // given
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode outerNested = query.putObject("nested");
        outerNested.put("path", "software_dependencies");
        ObjectNode outerNestedQuery = outerNested.putObject("query");
        ObjectNode innerNested = outerNestedQuery.putObject("nested");
        innerNested.put("path", "software_dependencies.licenses");
        ObjectNode innerNestedQuery = innerNested.putObject("query");
        ObjectNode match = innerNestedQuery.putObject("match");
        match.put("software_dependencies.licenses.key", "gpl");

        // when
        ArrayNode searchResult = search(query);

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("nestedObject");
    }

    @Test
    public void search_returns_single_document_matching_data_in_array() throws Exception {
        // given
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode matchQuery = query.putObject("match");
        matchQuery.put("arrayfield", "bd");

        // when
        ArrayNode searchResult = search(query);

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("array");
    }

    @Test
    public void search_returns_all_documents_matching_data_in_array() throws Exception {
        // given
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode matchQuery = query.putObject("prefix");
        matchQuery.put("arrayfield", "b");

        // when
        ArrayNode searchResult = search(query);

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactlyInAnyOrder("array", "array2");
    }

    /**
     * <pre>
     * {
     *   "query": {
     *     "nested": {
     *       "path": "software_dependencies",
     *       "query": {
     *          "match": {
     *            "software_dependencies.name": "websitename"
     *          }
     *       }
     *     }
     *   }
     * }
     * </pre>
     */
    @Test
    public void search_returns_documents_matching_data_containing_colon() throws Exception {
        // given
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode nested = query.putObject("nested");
        nested.put("path", "software_dependencies");
        ObjectNode nestedQuery = nested.putObject("query");
        ObjectNode match = nestedQuery.putObject("match");
        match.put("software_dependencies.name", "websitename");

        // when
        ArrayNode searchResult = search(query);

        // then
        assertThat(searchResult.findValues("id")).extracting(JsonNode::textValue).containsExactly("nestedObject");
    }

    @Test
    public void search_cannot_be_executed_when_fields_parameter_is_empty() throws Exception {
        // given
        String query = URLEncoder.encode(createOwnerLambdaQuery().toString(), "UTF-8");
        String emptyFields = "";

        // when
        ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity("/document?query={query}&fields={fields}", JsonNode.class, query, emptyFields);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void search_cannot_be_executed_when_sort_parameter_is_empty() throws Exception {
        assertThatSearchRequestWithSortParameterResultsInBadRequestResponse("");
    }

    @Test
    public void search_cannot_be_executed_when_sort_parameter_has_no_colon() throws Exception {
        assertThatSearchRequestWithSortParameterResultsInBadRequestResponse("short_nameasc");
    }

    @Test
    public void search_cannot_be_executed_when_fields_for_sorting_does_not_exist() throws Exception {
        assertThatSearchRequestWithSortParameterResultsInBadRequestResponse("does_not_exist:asc");
    }

    @Test
    public void search_cannot_be_executed_when_sort_order_is_invalid() throws Exception {
        assertThatSearchRequestWithSortParameterResultsInBadRequestResponse("short_name:asce");
    }

    private ObjectNode createOwnerLambdaQuery() {
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode match = query.putObject("match");
        match.put("owner", "lambda");
        return query;
    }

    private void assertThatSearchRequestWithSortParameterResultsInBadRequestResponse(String sortParam) throws Exception {
        // given
        String query = createOwnerLambdaQuery().toString();

        // when
        ResponseEntity<JsonNode> responseEntity = search(query, sortParam);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ArrayNode search(JsonNode queryParam) throws Exception {
        return search(queryParam, "", "");
    }

    private ArrayNode search(JsonNode queryParam, String fieldsParam, String sortParam) throws Exception {
        String searchParameter = "?query=" + URLEncoder.encode(queryParam.toString(), "UTF-8");
        if (StringUtils.isNotBlank(fieldsParam)) {
            searchParameter += "&fields=" + fieldsParam;
        }
        if (StringUtils.isNotBlank(sortParam)) {
            searchParameter += "&sort=" + sortParam;
        }
        String searchUrl = PIVIO_SERVER_BASE_URL + "/document" + searchParameter;
        RequestEntity<Void> requestEntity = RequestEntity.get(URI.create(searchUrl)).header(CONTENT_TYPE, APPLICATION_JSON_VALUE).build();
        return restTemplate.exchange(requestEntity, ArrayNode.class).getBody();
    }

    private ResponseEntity<JsonNode> search(String queryParam, String sortParam) throws Exception {
        return restTemplate.getForEntity("/document?query={query}&sort={sort}", JsonNode.class, URLEncoder.encode(queryParam, "UTF-8"), sortParam);
    }
}
