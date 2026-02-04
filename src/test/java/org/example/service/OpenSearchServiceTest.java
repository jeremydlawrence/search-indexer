package org.example.service;

import org.example.model.IndexableDocument;
import org.example.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenSearchServiceTest {

    @Mock
    private OpenSearchClient mockClient;

    @InjectMocks
    private OpenSearchService openSearchService;

    @Captor
    private ArgumentCaptor<BulkRequest> bulkRequestCaptor;

    private TestDocument testDoc1;
    private TestDocument testDoc2;
    private List<TestDocument> testDocs;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testDoc1 = new TestDocument("DOC-001", "Test Document 1");
        testDoc2 = new TestDocument("DOC-002", "Test Document 2");
        testDocs = List.of(testDoc1, testDoc2);
        
        // Create a simple test product using reflection or directly use TestDocument
        testProduct = new Product() {
            @Override
            public String getId() {
                return "PROD-001";
            }
        };
    }

    @Test
    void clusterHealth_WhenSuccessful_ReturnsHealthStatus() throws IOException {
        // Arrange
        HealthResponse mockHealthResponse = mock(HealthResponse.class);
        when(mockHealthResponse.status()).thenReturn(HealthStatus.Green);
        
        OpenSearchClusterClient mockClusterClient =
            mock(OpenSearchClusterClient.class);
        when(mockClusterClient.health()).thenReturn(mockHealthResponse);
        when(mockClient.cluster()).thenReturn(mockClusterClient);

        // Act
        String result = openSearchService.clusterHealth();

        // Assert
        assertEquals("Green", result);
        verify(mockClusterClient).health();
        verify(mockClient).cluster();
    }

    @Test
    void clusterHealth_WhenIOExceptionOccurs_ReturnsNull() throws IOException {
        // Arrange
        OpenSearchClusterClient mockClusterClient =
            mock(OpenSearchClusterClient.class);
        when(mockClusterClient.health()).thenThrow(new IOException("Connection failed"));
        when(mockClient.cluster()).thenReturn(mockClusterClient);

        // Act
        String result = openSearchService.clusterHealth();

        // Assert
        assertNull(result);
        verify(mockClusterClient).health();
        verify(mockClient).cluster();
    }

    @Test
    void bulkIndex_WithValidDocuments_SuccessfullyIndexes() throws IOException {
        // Arrange
        BulkResponse mockBulkResponse = mock(BulkResponse.class);
        when(mockBulkResponse.errors()).thenReturn(false);
        when(mockBulkResponse.took()).thenReturn(100L);
        
        when(mockClient.bulk(any(BulkRequest.class))).thenReturn(mockBulkResponse);

        // Act
        assertDoesNotThrow(() -> openSearchService.bulkIndex(testDocs, "test-index"));

        // Assert
        verify(mockClient).bulk(bulkRequestCaptor.capture());
        BulkRequest capturedRequest = bulkRequestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(2, capturedRequest.operations().size());
    }

    @Test
    void bulkIndex_WithNullDocuments_LogsWarningAndReturns() throws IOException {
        // Act
        assertDoesNotThrow(() -> openSearchService.bulkIndex(null, "test-index"));

        // Assert
        verify(mockClient, never()).bulk(any(BulkRequest.class));
    }

    @Test
    void bulkIndex_WithEmptyDocuments_LogsWarningAndReturns() throws IOException {
        // Act
        assertDoesNotThrow(() -> openSearchService.bulkIndex(List.of(), "test-index"));

        // Assert
        verify(mockClient, never()).bulk(any(BulkRequest.class));
    }

    @Test
    void bulkIndex_WithGenericDocuments_WorksCorrectly() throws IOException {
        // Arrange
        TestDocument testDoc1 = new TestDocument("DOC-001", "Test Document 1");
        TestDocument testDoc2 = new TestDocument("DOC-002", "Test Document 2");
        List<TestDocument> testDocuments = List.of(testDoc1, testDoc2);

        BulkResponse mockBulkResponse = mock(BulkResponse.class);
        when(mockBulkResponse.errors()).thenReturn(false);
        when(mockBulkResponse.took()).thenReturn(50L);
        
        when(mockClient.bulk(any(BulkRequest.class))).thenReturn(mockBulkResponse);

        // Act
        assertDoesNotThrow(() -> openSearchService.bulkIndex(testDocuments, "generic-index"));

        // Assert
        verify(mockClient).bulk(bulkRequestCaptor.capture());
        BulkRequest capturedRequest = bulkRequestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(2, capturedRequest.operations().size());
    }

    @Test
    void bulkIndex_WhenBulkResponseHasErrors_LogsErrors() throws IOException {
        // Arrange
        BulkResponse mockBulkResponse = mock(BulkResponse.class);
        when(mockBulkResponse.errors()).thenReturn(true);
        
        when(mockClient.bulk(any(BulkRequest.class))).thenReturn(mockBulkResponse);

        // Act & Assert
        assertDoesNotThrow(() -> openSearchService.bulkIndex(testDocs, "test-index"));

        // Assert
        verify(mockClient).bulk(any(BulkRequest.class));
    }

    @Test
    void bulkIndex_WhenExceptionOccurs_ThrowsRuntimeException() throws IOException {
        // Arrange
        when(mockClient.bulk(any(BulkRequest.class)))
            .thenThrow(new IOException("Bulk operation failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> openSearchService.bulkIndex(testDocs, "test-index"));
        
        assertTrue(exception.getMessage().contains("Bulk indexing failed for 2 documents"));
        assertTrue(exception.getMessage().contains("Bulk operation failed"));
    }

    @Test
    void bulkIndex_VerifiesCorrectIndexNameAndDocumentIds() throws IOException {
        // Arrange
        BulkResponse mockBulkResponse = mock(BulkResponse.class);
        when(mockBulkResponse.errors()).thenReturn(false);
        when(mockBulkResponse.took()).thenReturn(75L);
        
        when(mockClient.bulk(any(BulkRequest.class))).thenReturn(mockBulkResponse);

        // Act
        openSearchService.bulkIndex(testDocs, "products-index");

        // Assert
        verify(mockClient).bulk(bulkRequestCaptor.capture());
        BulkRequest capturedRequest = bulkRequestCaptor.getValue();
        
        // Verify index name
        capturedRequest.operations().forEach(operation -> {
            assertEquals("products-index", operation.index().index());
        });
        
        // Verify document IDs
        assertEquals("DOC-001", capturedRequest.operations().get(0).index().id());
        assertEquals("DOC-002", capturedRequest.operations().get(1).index().id());
    }

    @Test
    void bulkIndex_SingleDocument_HandlesCorrectly() throws IOException {
        // Arrange
        BulkResponse mockBulkResponse = mock(BulkResponse.class);
        when(mockBulkResponse.errors()).thenReturn(false);
        when(mockBulkResponse.took()).thenReturn(25L);
        
        when(mockClient.bulk(any(BulkRequest.class))).thenReturn(mockBulkResponse);

        // Act
        assertDoesNotThrow(() -> openSearchService.bulkIndex(List.of(testDoc1), "single-index"));

        // Assert
        verify(mockClient).bulk(bulkRequestCaptor.capture());
        BulkRequest capturedRequest = bulkRequestCaptor.getValue();
        assertEquals(1, capturedRequest.operations().size());
        assertEquals("DOC-001", capturedRequest.operations().get(0).index().id());
        assertEquals("single-index", capturedRequest.operations().get(0).index().index());
    }

    // Test helper class that implements IndexableDocument for generic testing
    private static class TestDocument implements IndexableDocument {
        private final String id;
        private final String name;

        public TestDocument(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}