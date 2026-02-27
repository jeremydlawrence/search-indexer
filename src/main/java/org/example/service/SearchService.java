package org.example.service;

import org.example.model.IndexableDocument;

import java.util.List;
import java.util.Set;

public interface SearchService {
    /**
     * Returns basic information about the health of the cluster.
     *
     * @return health status
     */
    String clusterHealth();

    /**
     * Indexes a list of documents in bulk to a specified index
     *
     * @param documents list of documents to index in bulk
     * @param indexName name of the destination index for documents
     * @param <T> document model
     */
    <T extends IndexableDocument> void bulkIndex(final List<T> documents, final String indexName);

    /**
     * Create a new index
     *
     * @param indexName name of the index to create
     * @param settingsPath path to the index settings file
     * @param mappingPath path to the index mapping file
     */
    void createIndex(final String indexName, final String settingsPath, final String mappingPath);

    /**
     * Delete an index by name
     *
     * @param indexName name of the index to delete
     */
    void deleteIndex(final String indexName);

    /**
     * Flush/commit all modification to an index
     *
     * @param indexName name of the index to flush
     */
    void flushIndex(final String indexName);

    /**
     * Update index settings for a specified index. The common use case is to
     * modify index settings before or after periods of heavy indexing activity.
     *
     * @param indexName the name of the index to modify setting
     * @param numReplicas number of shard replicas
     * @param refreshSeconds index refresh period in seconds
     */
    void updateSettings(final String indexName, final int numReplicas, final int refreshSeconds);

    /**
     * Wait for the status of an index to reach GREEN status. The common use case is to wait for
     * document flushing and replica balancing across nodes to finish before use the index for reads.
     *
     * @param indexName the name of the index to check
     * @param waitSeconds max wait time for GREEN status
     * @return boolean TRUE for successful GREEN status check
     */
    boolean waitForGreenStatus(final String indexName, final int waitSeconds);

    /**
     * Return a set of index names for a given index alias name
     *
     * @param aliasName the name of the alias
     * @return set of index names
     */
    Set<String> getIndexesByAlias(final String aliasName);

    /**
     * Return the first index that matches a given alias name
     *
     * @param aliasName the name of the alias
     * @return name of the index or null
     */
    String getIndexByAlias(final String aliasName);

    /**
     * Add an alias to a specified index
     *
     * @param indexName the name of the index
     * @param aliasName the name of the alias
     */
    void addAlias(final String indexName, final String aliasName);

    /**
     * Remove an alias from a specified index
     *
     * @param indexName the name of the index
     * @param aliasName the name of the alias
     */
    void removeAlias(final String indexName, final String aliasName);

    /**
     * Remove an alias from one index and add to another
     *
     * @param indexName the name of the index add the alias to
     * @param aliasName the name of the alias
     * @return String the name of the index the alias was removed from or null
     */
    String moveAlias(final String indexName, final String aliasName);
}
