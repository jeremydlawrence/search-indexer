package org.example.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.model.Product;
import org.example.service.OpenSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductIndexerTest {

    @Mock
    private OpenSearchService mockOpenSearchService;

    @InjectMocks
    private ProductIndexer productIndexer;

    @Captor
    private ArgumentCaptor<List<Product>> productsCaptor;

    @TempDir
    Path tempDir;

    private static final String TEST_INDEX_NAME = "products";
    private static final int TEST_BATCH_SIZE = 2;

    @BeforeEach
    void setUp() {
        // Use reflection to set the private fields for testing
        try {
            var batchSizeField = ProductIndexer.class.getDeclaredField("batchSize");
            batchSizeField.setAccessible(true);
            batchSizeField.set(productIndexer, TEST_BATCH_SIZE);

            var indexNameField = ProductIndexer.class.getDeclaredField("indexName");
            indexNameField.setAccessible(true);
            indexNameField.set(productIndexer, TEST_INDEX_NAME);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test fields", e);
        }
    }

    @Test
    void indexFromFile_WithValidJsonFile_SuccessfullyIndexesAll() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(5);
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(5, indexed);
        verify(mockOpenSearchService, times(3)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    @Test
    void indexFromFile_WithLimitParameter_RespectsLimit() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(10);
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString(), 3);

        // Assert
        assertEquals(3, indexed);
        verify(mockOpenSearchService, times(2)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    @Test
    void indexFromFile_WithEmptyJsonFile_ReturnsZero() throws IOException {
        // Arrange
        Path testFile = createTempFile("");

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(0, indexed);
        verify(mockOpenSearchService, never()).bulkIndex(any(), anyString());
    }

    @Test
    void indexFromFile_WithEmptyLines_IgnoresEmptyLines() throws IOException {
        // Arrange
        String testJson = """
            {"id": "1", "title": "Product 1"}
            
            {"id": "2", "title": "Product 2"}
            
            {"id": "3", "title": "Product 3"}
            """;
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(3, indexed);
        verify(mockOpenSearchService, times(2)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    @Test
    void indexFromFile_WithBatchSize_CallsBulkIndexCorrectly() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(4); // 4 items, batch size 2 = 2 calls
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(4, indexed);
        verify(mockOpenSearchService, times(2)).bulkIndex(productsCaptor.capture(), eq(TEST_INDEX_NAME));

        List<List<Product>> capturedBatches = productsCaptor.getAllValues();
        assertEquals(2, capturedBatches.size());
        assertEquals(2, capturedBatches.get(0).size());
        assertEquals(2, capturedBatches.get(1).size());
    }

    @Test
    void indexFromFile_WithItemsNotDivisibleByBatchSize_HandlesRemaining() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(5); // 5 items, batch size 2 = 3 calls
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(5, indexed);
        verify(mockOpenSearchService, times(3)).bulkIndex(productsCaptor.capture(), eq(TEST_INDEX_NAME));

        List<List<Product>> capturedBatches = productsCaptor.getAllValues();
        assertEquals(3, capturedBatches.size());
        assertEquals(2, capturedBatches.get(0).size());
        assertEquals(2, capturedBatches.get(1).size());
        assertEquals(1, capturedBatches.get(2).size());
    }

    @Test
    void indexFromFile_WithSingleItem_HandlesCorrectly() throws IOException {
        // Arrange
        String testJson = """
            {"id": "single", "title": "Single Product"}
            """;
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(1, indexed);
        verify(mockOpenSearchService, times(1)).bulkIndex(productsCaptor.capture(), eq(TEST_INDEX_NAME));

        List<Product> capturedProducts = productsCaptor.getValue();
        assertEquals(1, capturedProducts.size());
        assertEquals("single", capturedProducts.get(0).getId());
    }

    @Test
    void indexFromFile_WithInvalidJsonLines_LogsErrorAndContinues() throws IOException {
        // Arrange
        String testJson = """
            {"id": "1", "title": "Product 1"}
            {"invalid": json line}
            {"id": "2", "title": "Product 2"}
            """;
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(3, indexed); // All lines processed, including null for invalid JSON
        // Since we have a batch size of 2, we should get 2 calls (2 items first, then 1 item)
        verify(mockOpenSearchService, times(2)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    @Test
    void indexFromFile_WithNonExistentFile_ReturnsZero() {
        // Act
        int indexed = productIndexer.indexFromFile("non-existent-file.json");

        // Assert
        assertEquals(0, indexed);
        verify(mockOpenSearchService, never()).bulkIndex(any(), anyString());
    }

    @Test
    void bulkIndexRecords_WithValidJsonNodes_CallsBulkIndex() throws IOException {
        // Arrange
        List<JsonNode> jsonNodes = createTestJsonNodes(3);

        // Act
        int result = productIndexer.bulkIndexRecords(jsonNodes);

        // Assert
        assertEquals(3, result);
        verify(mockOpenSearchService, times(1)).bulkIndex(productsCaptor.capture(), eq(TEST_INDEX_NAME));

        List<Product> capturedProducts = productsCaptor.getValue();
        assertEquals(3, capturedProducts.size());
    }

    @Test
    void bulkIndexRecords_WithEmptyList_CallsBulkIndex() {
        // Act
        int result = productIndexer.bulkIndexRecords(List.of());

        // Assert
        assertEquals(0, result);
        verify(mockOpenSearchService, times(1)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    @Test
    void bulkIndexRecords_WithNullList_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> productIndexer.bulkIndexRecords(null));
        verify(mockOpenSearchService, never()).bulkIndex(any(), anyString());
    }

    @Test
    void bulkIndexRecords_WithValidJsonNode_WorksCorrectly() throws IOException {
        // Arrange
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JsonNode testNode = mapper.readTree("{\"id\": \"test\", \"title\": \"Test\"}");
        List<JsonNode> jsonNodes = List.of(testNode);

        // Act
        int result = productIndexer.bulkIndexRecords(jsonNodes);

        // Assert
        assertEquals(1, result);
        verify(mockOpenSearchService, times(1)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    @Test
    void indexFromFile_VerifiesProductFieldMapping() throws IOException {
        // Arrange
        String testJson = """
            {"id": "test123", "title": "Test Product", "price": "29.99"}
            """;
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(1, indexed);
        verify(mockOpenSearchService, times(1)).bulkIndex(productsCaptor.capture(), eq(TEST_INDEX_NAME));

        List<Product> capturedProducts = productsCaptor.getValue();
        assertEquals(1, capturedProducts.size());
        Product product = capturedProducts.get(0);
        assertEquals("test123", product.getId());
    }

    @Test
    void indexFromFile_NoLimitParameter_IndexesAll() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(3);
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString(), null);

        // Assert
        assertEquals(3, indexed);
        verify(mockOpenSearchService, times(2)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    @Test
    void indexFromFile_WithZeroLimit_ReturnsZero() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(5);
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString(), 0);

        // Assert
        assertEquals(0, indexed);
        verify(mockOpenSearchService, never()).bulkIndex(any(), anyString());
    }

    @Test
    void indexFromFile_WithLargeLimit_RespectsLimit() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(10);
        Path testFile = createTempFile(testJson);

        // Act
        int indexed = productIndexer.indexFromFile(testFile.toString(), 7);

        // Assert
        assertEquals(7, indexed);
        verify(mockOpenSearchService, times(4)).bulkIndex(any(), eq(TEST_INDEX_NAME));
    }

    // Helper methods
    private String createTestJsonLines(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(String.format("{\"id\": \"prod-%d\", \"title\": \"Product %d\"}", i, i));
            if (i < count - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private List<JsonNode> createTestJsonNodes(int count) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<JsonNode> nodes = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                String json = String.format("{\"id\": \"prod-%d\", \"title\": \"Product %d\"}", i, i);
                nodes.add(mapper.readTree(json));
            }
            return nodes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test JSON nodes", e);
        }
    }

    private Path createTempFile(String content) throws IOException {
        Path testFile = tempDir.resolve("test-products.json");
        Files.writeString(testFile, content);
        return testFile;
    }
}