package io.pivio.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivio.server.changeset.Changeset;
import io.pivio.server.document.PivioDocument;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
// todo @ContextConfiguration(initializers = DockerEnvironmentInitializer.class)
public abstract class AbstractApiTestCase {

    protected static final String PIVIO_SERVER_BASE_URL = "http://localhost:9123";
    protected static final String SOME_ID = "someId";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractApiTestCase.class);

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    // we do not want to use a SpringBootTest.WebEnvironment, hence we have no access to a fault-tolerant TestRestTemplate
    protected RestTemplate restTemplate = new RestTemplateBuilder().rootUri(PIVIO_SERVER_BASE_URL).errorHandler(new NoOpResponseErrorHandler()).build();

    @Before
    public void waitUntilPivioServerIsUpAndCleanUpPersistentData() {
        waitUntilPivioServerIsUp();
        cleanUpPersistentData(elasticsearchTemplate);
    }

    private void waitUntilPivioServerIsUp() {
        await().atMost(180, SECONDS).until(() -> {
            String documentResponse = "";
            RestTemplate faultSensitiveRestTemplate = new RestTemplateBuilder().rootUri(PIVIO_SERVER_BASE_URL).build();
            try {
                documentResponse = faultSensitiveRestTemplate.getForObject("/document", String.class);
            }
            catch (Exception ignored) {
                LOG.debug("Pivio Server is not up yet. Exception message: {}", ignored.getMessage());
            }
            return !documentResponse.isEmpty();
        });
    }

    private void cleanUpPersistentData(ElasticsearchTemplate elasticsearchTemplate) {
        LOG.debug("Cleaning up persistent data from Elasticsearch: deleting indices, creating new ones, put mappings, refresh indices");

        elasticsearchTemplate.deleteIndex(PivioDocument.class);
        elasticsearchTemplate.deleteIndex(Changeset.class);

        elasticsearchTemplate.createIndex(PivioDocument.class);
        elasticsearchTemplate.putMapping(PivioDocument.class);

        elasticsearchTemplate.createIndex(Changeset.class);
        elasticsearchTemplate.putMapping(Changeset.class);

        refreshIndices();
    }

    private void refreshIndices() {
        elasticsearchTemplate.refresh(PivioDocument.class);
        elasticsearchTemplate.refresh(Changeset.class);
    }

    protected PivioDocument postDocumentWithSomeId() {
        return postDocumentWithId(SOME_ID);
    }

    protected PivioDocument postDocumentWithId(String id) {
        PivioDocument documentWithId = createDocumentWithId(id);
        postDocument(documentWithId);
        return documentWithId;
    }

    protected PivioDocument createDocumentWithId(String id) {
        return PivioDocument.builder()
                .id(id)
                .type("service")
                .name("MicroService")
                .serviceName("MS")
                .description("Super service...")
                .owner("Awesome Team")
                .build();
    }

    protected ResponseEntity<PivioDocument> postDocument(PivioDocument document) {
        return postDocument(document, PivioDocument.class);
    }

    protected ResponseEntity<JsonNode> postDocument(JsonNode document) {
        return postDocument(document, JsonNode.class);
    }

    protected <T> ResponseEntity<T> postDocument(Object document, Class<T> responseType) {
        ResponseEntity<T> responseEntity = restTemplate.postForEntity("/document", document, responseType);
        refreshIndices();
        return responseEntity;
    }

    // When provoking responses indicating client or server side HTTP errors (400, 500) we do not want the test to fail.
    private static final class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
        }
    }
}
