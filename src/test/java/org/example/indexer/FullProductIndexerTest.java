package org.example.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.config.ProductIndexProperties;
import org.example.model.Product;
import org.example.service.EmbeddingService;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FullProductIndexerTest {

    @Mock
    private OpenSearchService mockOpenSearchService;

    @Mock
    private EmbeddingService mockEmbeddingService;

    @Mock
    private ProductIndexProperties mockIndexProperties;

    @InjectMocks
    private FullProductIndexer fullProductIndexer;

    @Captor
    private ArgumentCaptor<List<Product>> productsCaptor;

    @TempDir
    Path tempDir;

    private static final String TEST_ALIAS = "products";
    private static final String TEST_OLD_ALIAS = "products-old";
    private static final int TEST_BATCH_SIZE = 2;
    private static final String TEST_SETTINGS = "settings.json";
    private static final String TEST_MAPPING = "mapping.json";
    private static final int TEST_REPLICAS = 1;
    private static final int TEST_REFRESH_SECONDS = 1;
    private static final int TEST_STATUS_WAIT_SECONDS = 30;
    private static final int TEST_OLD_INDEX_KEEP_DAYS = 7;

    @BeforeEach
    void setUp() {
        // Configure mock index properties
        when(mockIndexProperties.getAlias()).thenReturn(TEST_ALIAS);
        when(mockIndexProperties.getOldAlias()).thenReturn(TEST_OLD_ALIAS);
        when(mockIndexProperties.getBatchSize()).thenReturn(TEST_BATCH_SIZE);
        when(mockIndexProperties.getSettings()).thenReturn(TEST_SETTINGS);
        when(mockIndexProperties.getMapping()).thenReturn(TEST_MAPPING);
        when(mockIndexProperties.getReplicas()).thenReturn(TEST_REPLICAS);
        when(mockIndexProperties.getRefreshSeconds()).thenReturn(TEST_REFRESH_SECONDS);
        when(mockIndexProperties.getStatusWaitSeconds()).thenReturn(TEST_STATUS_WAIT_SECONDS);
        when(mockIndexProperties.getOldIndexKeepDays()).thenReturn(TEST_OLD_INDEX_KEEP_DAYS);
    }

    @Test
    void init_CreatesNewIndexWithTimestampedName() {
        // Arrange
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());

        // Act
        String result = fullProductIndexer.init();

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith(TEST_ALIAS + "-"));
        assertTrue(result.matches(TEST_ALIAS + "-\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{6}"));
        verify(mockOpenSearchService).createIndex(eq(result), eq(TEST_SETTINGS), eq(TEST_MAPPING));
    }

    @Test
    void finalizer_WithGreenStatus_MovesAliasAndCleansUp() {
        // Arrange
        String newIndexName = "products-2026.02.23.120000";
        String oldIndexName = "products-2026.02.20.120000";
        
        when(mockOpenSearchService.waitForGreenStatus(eq(newIndexName), eq(TEST_STATUS_WAIT_SECONDS)))
            .thenReturn(true);
        when(mockOpenSearchService.moveAlias(eq(newIndexName), eq(TEST_ALIAS)))
            .thenReturn(oldIndexName);
        when(mockOpenSearchService.getIndexesByAlias(eq(TEST_OLD_ALIAS)))
            .thenReturn(Set.of(oldIndexName));

        // Act
        fullProductIndexer.finalizer(newIndexName);

        // Assert
        verify(mockOpenSearchService).flushIndex(newIndexName);
        verify(mockOpenSearchService).updateSettings(eq(newIndexName), eq(TEST_REPLICAS), eq(TEST_REFRESH_SECONDS));
        verify(mockOpenSearchService).waitForGreenStatus(eq(newIndexName), eq(TEST_STATUS_WAIT_SECONDS));
        verify(mockOpenSearchService).moveAlias(eq(newIndexName), eq(TEST_ALIAS));
        verify(mockOpenSearchService).addAlias(eq(oldIndexName), eq(TEST_OLD_ALIAS));
    }

    @Test
    void finalizer_WithNotGreenStatus_DoesNotMoveAlias() {
        // Arrange
        String newIndexName = "products-2026.02.23.120000";
        
        when(mockOpenSearchService.waitForGreenStatus(eq(newIndexName), eq(TEST_STATUS_WAIT_SECONDS)))
            .thenReturn(false);

        // Act
        fullProductIndexer.finalizer(newIndexName);

        // Assert
        verify(mockOpenSearchService).flushIndex(newIndexName);
        verify(mockOpenSearchService).updateSettings(eq(newIndexName), eq(TEST_REPLICAS), eq(TEST_REFRESH_SECONDS));
        verify(mockOpenSearchService).waitForGreenStatus(eq(newIndexName), eq(TEST_STATUS_WAIT_SECONDS));
        verify(mockOpenSearchService, never()).moveAlias(anyString(), anyString());
        verify(mockOpenSearchService, never()).addAlias(anyString(), anyString());
    }

    @Test
    void finalizer_WithNoOldIndex_DoesNotCallAddAlias() {
        // Arrange
        String newIndexName = "products-2026.02.23.120000";
        
        when(mockOpenSearchService.waitForGreenStatus(eq(newIndexName), eq(TEST_STATUS_WAIT_SECONDS)))
            .thenReturn(true);
        when(mockOpenSearchService.moveAlias(eq(newIndexName), eq(TEST_ALIAS)))
            .thenReturn(null); // No old index

        // Act
        fullProductIndexer.finalizer(newIndexName);

        // Assert
        verify(mockOpenSearchService).moveAlias(eq(newIndexName), eq(TEST_ALIAS));
        verify(mockOpenSearchService, never()).addAlias(anyString(), eq(TEST_OLD_ALIAS));
    }

    @Test
    void indexFromFile_WithValidJsonFile_SuccessfullyIndexesAll() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(5);
        Path testFile = createTempFile(testJson);
        
        // Mock init to return a specific index name
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        when(mockOpenSearchService.waitForGreenStatus(anyString(), anyInt())).thenReturn(true);
        when(mockOpenSearchService.moveAlias(anyString(), anyString())).thenReturn(null);
        when(mockOpenSearchService.getIndexesByAlias(anyString())).thenReturn(Set.of());
        when(mockEmbeddingService.getEmbeddings(anyList()))
                .thenReturn(List.of(List.of(1f),List.of(2f),List.of(3f),List.of(4f),List.of(5f)));

        // Act
        int indexed = fullProductIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(5, indexed);
        verify(mockOpenSearchService, times(3)).bulkIndex(any(), anyString());
    }

    @Test
    void indexFromFile_WithLimitParameter_RespectsLimit() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(10);
        Path testFile = createTempFile(testJson);
        
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        when(mockOpenSearchService.waitForGreenStatus(anyString(), anyInt())).thenReturn(true);
        when(mockOpenSearchService.moveAlias(anyString(), anyString())).thenReturn(null);
        when(mockOpenSearchService.getIndexesByAlias(anyString())).thenReturn(Set.of());
        when(mockEmbeddingService.getEmbeddings(anyList()))
                .thenReturn(List.of(List.of(1f),List.of(2f),List.of(3f)));

        // Act
        int indexed = fullProductIndexer.indexFromFile(testFile.toString(), 3);

        // Assert
        assertEquals(3, indexed);
        verify(mockOpenSearchService, times(2)).bulkIndex(any(), anyString());
    }

    @Test
    void indexFromFile_WithEmptyJsonFile_ReturnsZero() throws IOException {
        // Arrange
        Path testFile = createTempFile("");
        
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        when(mockOpenSearchService.waitForGreenStatus(anyString(), anyInt())).thenReturn(true);
        when(mockOpenSearchService.moveAlias(anyString(), anyString())).thenReturn(null);
        when(mockOpenSearchService.getIndexesByAlias(anyString())).thenReturn(Set.of());

        // Act
        int indexed = fullProductIndexer.indexFromFile(testFile.toString());

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
        
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        when(mockOpenSearchService.waitForGreenStatus(anyString(), anyInt())).thenReturn(true);
        when(mockOpenSearchService.moveAlias(anyString(), anyString())).thenReturn(null);
        when(mockOpenSearchService.getIndexesByAlias(anyString())).thenReturn(Set.of());
        when(mockEmbeddingService.getEmbeddings(anyList()))
                .thenReturn(List.of(List.of(1f),List.of(2f),List.of(3f)));

        // Act
        int indexed = fullProductIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(3, indexed);
        verify(mockOpenSearchService, times(2)).bulkIndex(any(), anyString());
    }

    @Test
    void indexFromFile_WithBatchSize_CallsBulkIndexCorrectly() throws IOException {
        // Arrange
        String testJson = createTestJsonLines(4); // 4 items, batch size 2 = 2 calls
        Path testFile = createTempFile(testJson);
        
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        when(mockOpenSearchService.waitForGreenStatus(anyString(), anyInt())).thenReturn(true);
        when(mockOpenSearchService.moveAlias(anyString(), anyString())).thenReturn(null);
        when(mockOpenSearchService.getIndexesByAlias(anyString())).thenReturn(Set.of());
        when(mockEmbeddingService.getEmbeddings(anyList()))
                .thenReturn(List.of(List.of(1f),List.of(2f),List.of(3f),List.of(4f)));

        // Act
        int indexed = fullProductIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(4, indexed);
        verify(mockOpenSearchService, times(2)).bulkIndex(productsCaptor.capture(), anyString());

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
        
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        when(mockOpenSearchService.waitForGreenStatus(anyString(), anyInt())).thenReturn(true);
        when(mockOpenSearchService.moveAlias(anyString(), anyString())).thenReturn(null);
        when(mockOpenSearchService.getIndexesByAlias(anyString())).thenReturn(Set.of());
        when(mockEmbeddingService.getEmbeddings(anyList()))
                .thenReturn(List.of(List.of(1f),List.of(2f),List.of(3f),List.of(4f),List.of(5f)));

        // Act
        int indexed = fullProductIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(5, indexed);
        verify(mockOpenSearchService, times(3)).bulkIndex(productsCaptor.capture(), anyString());

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
        
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        when(mockOpenSearchService.waitForGreenStatus(anyString(), anyInt())).thenReturn(true);
        when(mockOpenSearchService.moveAlias(anyString(), anyString())).thenReturn(null);
        when(mockOpenSearchService.getIndexesByAlias(anyString())).thenReturn(Set.of());
        when(mockEmbeddingService.getEmbeddings(anyList())).thenReturn(List.of(List.of(1f)));

        // Act
        int indexed = fullProductIndexer.indexFromFile(testFile.toString());

        // Assert
        assertEquals(1, indexed);
        verify(mockOpenSearchService, times(1)).bulkIndex(productsCaptor.capture(), anyString());

        List<Product> capturedProducts = productsCaptor.getValue();
        assertEquals(1, capturedProducts.size());
        assertEquals("single", capturedProducts.get(0).getId());
    }

    @Test
    void bulkIndexRecords_WithValidJsonNodes_CallsBulkIndex() throws IOException {
        // Arrange
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JsonNode node1 = mapper.readTree("{\"id\": \"prod-0\", \"title\": \"Product 0\"}");
        JsonNode node2 = mapper.readTree("{\"id\": \"prod-1\", \"title\": \"Product 1\"}");
        List<JsonNode> jsonNodes = List.of(node1, node2);
        String indexName = "products-2026.02.23.120000";

        when(mockEmbeddingService.getEmbeddings(anyList())).thenReturn(List.of(List.of(1f),List.of(2f)));

        // Act
        int result = fullProductIndexer.bulkIndexRecords(jsonNodes, indexName);

        // Assert
        assertEquals(2, result);
        verify(mockOpenSearchService, times(1)).bulkIndex(productsCaptor.capture(), eq(indexName));

        List<Product> capturedProducts = productsCaptor.getValue();
        assertEquals(2, capturedProducts.size());
    }

    @Test
    void bulkIndexRecords_WithEmptyList_CallsBulkIndexWithEmptyList() {
        when(mockEmbeddingService.getEmbeddings(anyList())).thenReturn(new ArrayList<>());

        // Act
        int result = fullProductIndexer.bulkIndexRecords(List.of(), "test-index");

        // Assert
        assertEquals(0, result);
        verify(mockOpenSearchService, times(1)).bulkIndex(any(), eq("test-index"));
    }

    @Test
    void indexFromFile_WithIOException_ReturnsIndexedCount() throws IOException {
        // Arrange - non-existent file should trigger IOException in the reading
        doNothing().when(mockOpenSearchService).createIndex(anyString(), anyString(), anyString());
        
        // The method should handle IOException gracefully
        // Act
        int indexed = fullProductIndexer.indexFromFile("non-existent-file.json");
        
        // Assert
        assertEquals(0, indexed);
        verify(mockOpenSearchService, never()).bulkIndex(any(), anyString());
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

    private Path createTempFile(String content) throws IOException {
        Path testFile = tempDir.resolve("test-products.json");
        Files.writeString(testFile, content);
        return testFile;
    }
}