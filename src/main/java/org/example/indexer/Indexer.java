package org.example.indexer;

import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nullable;
import java.util.List;

public interface Indexer {
    int indexFromFile(String fileName);
    int indexFromFile(String fileName, @Nullable final Integer limit);
    int bulkIndexRecords(final List<JsonNode> nodeList);
}
