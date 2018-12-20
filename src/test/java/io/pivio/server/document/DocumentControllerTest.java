package io.pivio.server.document;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.rest.RestStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentControllerTest {

    @Mock
    Client clientMock;

    @Mock
    MeterRegistry meterRegistryMock;

    DocumentController documentController;

    @Before
    public void setUp() {
        when(meterRegistryMock.counter(anyString())).thenReturn(mock(Counter.class));
        documentController = new DocumentController(clientMock, null, null, meterRegistryMock);
    }

    @Test
    public void testDeleteNotFound() throws Exception {
        // given
        String unknownDocumentId = "unknownDocumentId";

        // and
        DeleteResponse deleteResponseMock = mock(DeleteResponse.class);
        when(deleteResponseMock.status()).thenReturn(RestStatus.NOT_FOUND);
        DeleteRequestBuilder deleteRequestBuilderMock = mock(DeleteRequestBuilder.class);
        when(deleteRequestBuilderMock.get()).thenReturn(deleteResponseMock);
        when(clientMock.prepareDelete("steckbrief", "steckbrief", unknownDocumentId)).thenReturn(deleteRequestBuilderMock);


        // when
        ResponseEntity response = documentController.delete(unknownDocumentId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}