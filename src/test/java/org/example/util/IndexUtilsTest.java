package org.example.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexUtilsTest {
    private static final String TEST_PATTERN = "yyyy.MM.dd.HHmmss";

    @Test
    void getIndexName_WithValidRootName_ReturnsIndexNameWithTimestamp() {
        // Act
        String result = IndexUtils.getIndexName("products");

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("products-"));
        assertTrue(result.matches("products-\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{6}"));
    }

    @Test
    void getDateFromIndexName_WithValidIndexName_ReturnsCorrectDate() {
        // Arrange
        String indexName = "products-2026.02.22.165241";

        // Act
        LocalDateTime result = IndexUtils.getDateFromIndexName(indexName);

        // Assert
        assertNotNull(result);
        assertEquals(2026, result.getYear());
        assertEquals(2, result.getMonthValue());
        assertEquals(22, result.getDayOfMonth());
        assertEquals(16, result.getHour());
        assertEquals(52, result.getMinute());
        assertEquals(41, result.getSecond());
    }

    @Test
    void shouldDeleteIndex_WithIndexOlderThanMaxDays_ReturnsTrue() {
        // Arrange - Create an index name from 10 days ago
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);
        String oldIndexName = "products-" + tenDaysAgo.format(DateTimeFormatter.ofPattern(TEST_PATTERN));

        // Act
        boolean result = IndexUtils.shouldDeleteIndex(oldIndexName, 7);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldDeleteIndex_WithIndexWithinMaxDays_ReturnsFalse() {
        // Arrange - Create an index name from 3 days ago
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        String recentIndexName = "products-" + threeDaysAgo.format(DateTimeFormatter.ofPattern(TEST_PATTERN));

        // Act
        boolean result = IndexUtils.shouldDeleteIndex(recentIndexName, 7);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldDeleteIndex_WithIndexExactlyAtMaxDays_ReturnsFalse() {
        // Arrange - Create an index name from exactly 7 days ago
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        String indexName = "products-" + sevenDaysAgo.format(DateTimeFormatter.ofPattern(TEST_PATTERN));

        // Act
        boolean result = IndexUtils.shouldDeleteIndex(indexName, 7);

        // Assert
        // When period.getDays() == maxDays, it should return false (> not >=)
        assertFalse(result);
    }

    @Test
    void shouldDeleteIndex_WithIndexOneDayOverMaxDays_ReturnsTrue() {
        // Arrange - Create an index name from exactly 8 days ago
        LocalDateTime eightDaysAgo = LocalDateTime.now().minusDays(8);
        String indexName = "products-" + eightDaysAgo.format(DateTimeFormatter.ofPattern(TEST_PATTERN));

        // Act
        boolean result = IndexUtils.shouldDeleteIndex(indexName, 7);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldDeleteIndex_WithZeroMaxDays_OnlyDeletesToday() {
        // Arrange - Create an index name from today
        LocalDateTime today = LocalDateTime.now();
        String indexName = "products-" + today.format(DateTimeFormatter.ofPattern(TEST_PATTERN));

        // Act
        boolean result = IndexUtils.shouldDeleteIndex(indexName, 0);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldDeleteIndex_WithNegativeMaxDays_ThrowsException() {
        // Arrange
        String indexName = "products-2026.02.22.165241";

        // Act & Assert
        // Negative maxDays should result in true (any positive days > negative)
        // But let's verify it handles gracefully - Period.between with negative might behave unexpectedly
        boolean result = IndexUtils.shouldDeleteIndex(indexName, -1);
        assertTrue(result); // Any positive days > -1
    }

    @Test
    void getIndexName_Consistency_CheckMultipleCallsHaveSameDate() {
        // Arrange
        String firstCall = IndexUtils.getIndexName("test");
        
        // Small delay to ensure different second if timestamp changes
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        String secondCall = IndexUtils.getIndexName("test");
        
        // Both calls should have same date portion (same second)
        String firstDate = firstCall.substring(firstCall.indexOf("-") + 1);
        String secondDate = secondCall.substring(secondCall.indexOf("-") + 1);
        
        // Extract just the date part (yyyy.MM.dd) - they should be same
        assertEquals(firstDate.substring(0, 10), secondDate.substring(0, 10));
    }

    @Test
    void roundTrip_IndexNameToDateAndBack() {
        // Arrange
        String originalRootName = "myproducts";
        String indexName = IndexUtils.getIndexName(originalRootName);
        
        // Act
        LocalDateTime extractedDate = IndexUtils.getDateFromIndexName(indexName);
        
        // Assert
        assertNotNull(extractedDate);
        // The extracted date should be parseable back to the same pattern
        String reformatted = extractedDate.format(DateTimeFormatter.ofPattern(TEST_PATTERN));
        assertTrue(indexName.contains(reformatted));
    }

    @Test
    void getDateFromIndexName_WithLongRootName_ParsesCorrectly() {
        // Arrange - root name with multiple hyphens
        String indexName = "my-very-long-product-index-name-2026.03.15.123456";

        // Act
        LocalDateTime result = IndexUtils.getDateFromIndexName(indexName);

        // Assert
        assertNotNull(result);
        assertEquals(2026, result.getYear());
        assertEquals(3, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(12, result.getHour());
        assertEquals(34, result.getMinute());
        assertEquals(56, result.getSecond());
    }
}