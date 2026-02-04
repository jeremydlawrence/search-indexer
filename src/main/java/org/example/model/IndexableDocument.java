package org.example.model;

/**
 * Interface for all indexable documents in OpenSearch.
 * Any document that needs to be indexed must implement this interface
 * to provide the required fields for OpenSearch operations.
 */
public interface IndexableDocument {
    /**
     * Returns the unique identifier for this document.
     * This ID will be used as the document ID in OpenSearch.
     * 
     * @return the unique document identifier
     */
    String getId();
}