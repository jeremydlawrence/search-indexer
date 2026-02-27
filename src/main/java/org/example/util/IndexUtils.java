package org.example.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class IndexUtils {
    private static final String INDEX_SUFFIX_PATTERN = "yyyy.MM.dd.HHmmss";

    /**
     * Return an index name encoded with the current date and time like coupons-2026.02.22.165241
     *
     * @param rootName the root index name
     * @return the fully encoded index name
     */
    public static String getIndexName(final String rootName) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(INDEX_SUFFIX_PATTERN);
        final String timestamp = LocalDateTime.now().format(formatter);
        return rootName + "-" + timestamp;
    }

    /**
     * Extract the LocalDateTime from an index name like coupons-2026.02.22.165241
     *
     * @param indexName the index name to extract date details from
     * @return the LocalDateTime encoded in the index name
     */
    public static LocalDateTime getDateFromIndexName(final String indexName) {
        return LocalDateTime.parse(
                indexName.substring(indexName.lastIndexOf("-") + 1),
                DateTimeFormatter.ofPattern(INDEX_SUFFIX_PATTERN));
    }

    /**
     * Determine if an index is old enough to be deleted
     *
     * @param indexName the index name to check
     * @param maxDays the max days to check against the age of indexName
     * @return boolean
     */
    public static boolean shouldDeleteIndex(final String indexName, final int maxDays) {
        final LocalDateTime indexDate = getDateFromIndexName(indexName);
        final LocalDate today = LocalDate.now(ZoneId.systemDefault());
        final Period period = Period.between(LocalDate.from(indexDate), today);
        return period.getDays() > maxDays;
    }
}
