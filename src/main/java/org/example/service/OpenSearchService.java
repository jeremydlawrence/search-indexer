package org.example.service;

import org.example.model.IndexableDocument;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class OpenSearchService {
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchService.class);

    private final OpenSearchClient client;

    @Autowired
    public OpenSearchService(final OpenSearchClient client) {
        this.client = client;
    }

    public String clusterHealth() {
        try {
            final HealthStatus health = client.cluster().health().status();
            return health.toString();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public <T extends IndexableDocument> void bulkIndex(final List<T> documents, final String indexName) {
        if (documents == null || documents.isEmpty()) {
            logger.warn("Attempted to bulk index null or empty list");
            return;
        }

        logger.debug("Starting bulk index of {} documents", documents.size());

        try {
            final BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (T document : documents) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(document.getId())
                                .document(document)
                        )
                );
            }
            BulkResponse result = client.bulk(bulkBuilder.build());

            if (result.errors()) {
                logger.error("Bulk had errors");
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        logger.error(item.error().reason());
                    }
                }
            } else {
                logger.debug("Bulk indexing completed in {}ms", result.took());
            }
        } catch (Exception e) {
            final String message = String.format("Bulk indexing failed for %s documents: %s",
                    documents.size(),
                    e.getMessage());
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }
}
