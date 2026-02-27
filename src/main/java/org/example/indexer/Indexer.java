package org.example.indexer;

import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nullable;
import java.util.List;

public interface Indexer {
    /**
     * Index documents from a local file
     *
     * @param fileName path to the documents file
     * @return the number of indexed documents
     */
    int indexFromFile(final String fileName);

    /**
     * Index documents from a local file with a limit
     *
     * @param fileName path to the documents file
     * @param limit maximum number of documents to index
     * @return the number of indexed documents
     */
    int indexFromFile(final String fileName, @Nullable final Integer limit);

    /**
     * Index a collection of generic document objects
     *
     * @param nodeList collection of document objects
     * @param indexName the name of the destination index
     * @return the number of documents indexed
     */
    int bulkIndexRecords(final List<JsonNode> nodeList, final String indexName);

    /**
     * Finalize the indexing operation and perform any validation checks
     *
     * @param newIndexName the name of the updated index
     */
    void finalizer(final String newIndexName);
}
