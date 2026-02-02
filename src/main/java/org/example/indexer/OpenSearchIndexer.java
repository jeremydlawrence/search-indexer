package org.example.indexer;

import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenSearchIndexer<T> implements Indexer<T> {

    @Autowired
    RestHighLevelClient highLevelClient;

    void createIndex(final String indexName) {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request

        highLevelClient.indices().create(request);
    }
}
