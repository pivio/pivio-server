package io.pivio.server.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivio.server.changeset.ChangesetService;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DocumentControllerTest {

    private Client client = null;
    DocumentController documentController;
    ObjectMapper objectMapper;
    CounterService counterService;

    @Before
    public void setUp() {
        client = mock(Client.class);
        counterService = mock(CounterService.class);
        objectMapper = new ObjectMapper();
        documentController = new DocumentController(client, new ChangesetService(client, objectMapper), objectMapper, counterService);
    }

    @Test
    public void testDeleteNotFound() throws Exception {
        String id = "1";
        DeleteRequestBuilder mockDeleteRequestBuilder = mock(DeleteRequestBuilder.class);
        ListenableActionFuture mockListenableActionFuture = mock(ListenableActionFuture.class);
        DeleteResponse mockDeleteResponse = mock(DeleteResponse.class);

        when(client.prepareDelete("steckbrief", "steckbrief", id)).thenReturn(mockDeleteRequestBuilder);
        when(mockDeleteRequestBuilder.execute()).thenReturn(mockListenableActionFuture);
        when(mockListenableActionFuture.actionGet()).thenReturn(mockDeleteResponse);
        when(mockDeleteResponse.isFound()).thenReturn(false);

        ResponseEntity response = documentController.delete(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}