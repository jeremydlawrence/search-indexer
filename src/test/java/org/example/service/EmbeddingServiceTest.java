package org.example.service;

import org.example.config.EmbeddingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.net.http.HttpRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingProperties mockConfigProperties;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        when(mockConfigProperties.getHost()).thenReturn("localhost");
        when(mockConfigProperties.getPort()).thenReturn(8000);
        when(mockConfigProperties.getPath()).thenReturn("embed");
        when(mockConfigProperties.getCharLimit()).thenReturn(2500);
        
        embeddingService = new EmbeddingService(mockConfigProperties);
    }

    // Tests for getEmbeddings
    @Test
    void getEmbeddings_WithEmptyList_ReturnsEmptyList() {
        List<List<Float>> result = embeddingService.getEmbeddings(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getEmbeddings_WithNullList_ReturnsEmptyList() {
        List<List<Float>> result = embeddingService.getEmbeddings(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void embeddingService_CanBeInstantiated() {
        assertNotNull(embeddingService);
    }

    // Tests for buildRequestBody (protected)
    @Test
    void buildRequestBody_WithSingleText_ReturnsCorrectJson() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("buildRequestBody", List.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, List.of("hello"));
        
        assertEquals("{\"texts\": [\"hello\"]}", result);
    }

    @Test
    void buildRequestBody_WithMultipleTexts_ReturnsCorrectJson() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("buildRequestBody", List.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, List.of("hello", "world"));
        
        assertEquals("{\"texts\": [\"hello\", \"world\"]}", result);
    }

    @Test
    void buildRequestBody_WithEmptyList_ReturnsEmptyTextsArray() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("buildRequestBody", List.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, List.of());
        
        assertEquals("{\"texts\": []}", result);
    }

    @Test
    void buildRequestBody_WithSanitizedText_ReturnsEscapedJson() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("buildRequestBody", List.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, List.of("hello\"world"));
        
        assertEquals("{\"texts\": [\"hello\\\"world\"]}", result);
    }

    // Tests for sanitizeText (protected)
    @Test
    void sanitizeText_WithNull_ReturnsEmptyString() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, (Object) null);
        
        assertEquals("", result);
    }

    @Test
    void sanitizeText_WithNormalText_ReturnsUnchanged() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "hello world");
        
        assertEquals("hello world", result);
    }

    @Test
    void sanitizeText_WithQuotes_EscapesQuotes() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "hello\"world");
        
        assertEquals("hello\\\"world", result);
    }

    @Test
    void sanitizeText_WithNewline_ReplacesWithSpace() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "hello\nworld");
        
        assertEquals("hello world", result);
    }

    @Test
    void sanitizeText_WithTab_ReplacesWithSpace() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "hello\tworld");
        
        assertEquals("hello world", result);
    }

    @Test
    void sanitizeText_WithCarriageReturn_ReplacesWithSpace() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "hello\rworld");
        
        assertEquals("hello world", result);
    }

    @Test
    void sanitizeText_WithMultipleSpaces_CollapsesToSingle() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "hello    world");
        
        assertEquals("hello world", result);
    }

    @Test
    void sanitizeText_WithTextExceedingCharLimit_Truncates() throws Exception {
        when(mockConfigProperties.getCharLimit()).thenReturn(10);
        
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "this is a very long text");
        
        assertEquals(10, result.length());
        assertEquals("this is a ", result);
    }

    @Test
    void sanitizeText_WithTextAtCharLimit_NoTruncation() throws Exception {
        when(mockConfigProperties.getCharLimit()).thenReturn(5);
        
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "hello");
        
        assertEquals("hello", result);
    }

    @Test
    void sanitizeText_WithZeroCharLimit_NoTruncation() throws Exception {
        when(mockConfigProperties.getCharLimit()).thenReturn(0);
        
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "this is a very long text");
        
        assertEquals("this is a very long text", result);
    }

    @Test
    void sanitizeText_WithNegativeCharLimit_NoTruncation() throws Exception {
        when(mockConfigProperties.getCharLimit()).thenReturn(-1);
        
        Method method = EmbeddingService.class.getDeclaredMethod("sanitizeText", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(embeddingService, "this is a very long text");
        
        assertEquals("this is a very long text", result);
    }

    // Tests for parseResponse (protected)
    @Test
    void parseResponse_WithValidJson_ReturnsVectors() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("parseResponse", String.class);
        method.setAccessible(true);
        
        String responseBody = "{\"vectors\": [[0.1, 0.2, 0.3], [0.4, 0.5, 0.6]]}";
        
        @SuppressWarnings("unchecked")
        List<List<Float>> result = (List<List<Float>>) method.invoke(embeddingService, responseBody);
        
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(0.1f, result.get(0).get(0), 0.001);
        assertEquals(0.2f, result.get(0).get(1), 0.001);
        assertEquals(0.3f, result.get(0).get(2), 0.001);
    }

    @Test
    void parseResponse_WithSingleVector_ReturnsCorrectly() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("parseResponse", String.class);
        method.setAccessible(true);
        
        String responseBody = "{\"vectors\": [[1.0, 2.0]]}";
        
        @SuppressWarnings("unchecked")
        List<List<Float>> result = (List<List<Float>>) method.invoke(embeddingService, responseBody);
        
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(1.0f, result.get(0).get(0), 0.001);
    }

    @Test
    void parseResponse_WithEmptyVectors_ReturnsEmptyList() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("parseResponse", String.class);
        method.setAccessible(true);
        
        String responseBody = "{\"vectors\": []}";
        
        @SuppressWarnings("unchecked")
        List<List<Float>> result = (List<List<Float>>) method.invoke(embeddingService, responseBody);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseResponse_WithInvalidJson_ThrowsException() throws Exception {
        Method method = EmbeddingService.class.getDeclaredMethod("parseResponse", String.class);
        method.setAccessible(true);
        
        String invalidJson = "not valid json";
        
        assertThrows(Exception.class, () -> method.invoke(embeddingService, invalidJson));
    }
}
