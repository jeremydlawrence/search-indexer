package org.example.service;

import jakarta.json.stream.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.example.model.IndexableDocument;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteAliasRequest;
import org.opensearch.client.opensearch.indices.DeleteAliasResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.FlushRequest;
import org.opensearch.client.opensearch.indices.FlushResponse;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetAliasResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.PutAliasRequest;
import org.opensearch.client.opensearch.indices.PutAliasResponse;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class OpenSearchService implements SearchService {
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchService.class);
    private final OpenSearchClient client;

    @Autowired
    public OpenSearchService(final OpenSearchClient client) {
        this.client = client;
    }

    protected JsonpMapper getJsonpMapper() {
        return client._transport().jsonpMapper();
    }

    protected IndexSettings deserializeSettings(Reader reader, JsonpMapper mapper) {
        JsonParser parser = mapper.jsonProvider().createParser(reader);
        return IndexSettings._DESERIALIZER.deserialize(parser, mapper);
    }

    protected TypeMapping deserializeMapping(Reader reader, JsonpMapper mapper) {
        JsonParser parser = mapper.jsonProvider().createParser(reader);
        return TypeMapping._DESERIALIZER.deserialize(parser, mapper);
    }

    protected Time getTimeInSeconds(final int seconds) {
        return new Time.Builder()
                .time(String.format("%ss", seconds))
                .build();
    }

    @Override
    public String clusterHealth() {
        try {
            final HealthStatus health = client.cluster().health().status();
            return health.toString();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
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
                logger.info("Bulk indexing completed in {}ms", result.took());
            }
        } catch (Exception e) {
            final String message = String.format("Bulk indexing failed for %s documents: %s",
                    documents.size(),
                    e.getMessage());
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public void createIndex(final String indexName, final String settingsPath, final String mappingPath) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        final JsonpMapper mapper = getJsonpMapper();

        try (Reader readerSettings = new InputStreamReader(
                Objects.requireNonNull(this.getClass().getResourceAsStream(settingsPath)));
             Reader readerMapping = new InputStreamReader(
                     Objects.requireNonNull(this.getClass().getResourceAsStream(mappingPath)))
        ) {
            final IndexSettings settings = deserializeSettings(readerSettings, mapper);
            final TypeMapping mapping = deserializeMapping(readerMapping, mapper);

            final CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .settings(settings)
                    .mappings(mapping)
                    .build();
            CreateIndexResponse response = client.indices().create(createIndexRequest);
            logger.info("Created index: {}", response.index());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteIndex(final String indexName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        try {
            final DeleteIndexRequest request = new DeleteIndexRequest.Builder()
                    .index(indexName)
                    .build();
            final DeleteIndexResponse response = client.indices().delete(request);
            logger.info("Deleted index: {}", indexName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flushIndex(final String indexName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        final FlushRequest flushRequest = FlushRequest.builder()
                .index(indexName)
                .build();
        try {
            final FlushResponse response = client.indices().flush(flushRequest);
            logger.info("Flushed index: {}", indexName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateSettings(
            final String indexName,
            final int numReplicas,
            final int refreshSeconds) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        try {
            final IndexSettings settings = new IndexSettings.Builder()
                    .numberOfReplicas(numReplicas)
                    .refreshInterval(getTimeInSeconds(refreshSeconds))
                    .build();
            PutIndicesSettingsRequest request = new PutIndicesSettingsRequest.Builder()
                    .index(indexName)
                    .settings(settings)
                    .build();
            final PutIndicesSettingsResponse response = client.indices().putSettings(request);
            logger.info("Updated settings for index {}", indexName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean waitForGreenStatus(final String indexName, final int waitSeconds) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        try {
            final HealthRequest healthRequest = new HealthRequest.Builder()
                    .index(indexName)
                    .waitForStatus(HealthStatus.Green)
                    .timeout(getTimeInSeconds(waitSeconds))
                    .build();
            final HealthResponse response = client.cluster().health(healthRequest);

            if (response.status() == HealthStatus.Green) {
                logger.info("Green status for index {}", indexName);
                return true;
            } else {
                logger.info("Index status for {} is {} after waiting for {} seconds",
                        indexName, response.status(), waitSeconds);
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getIndexesByAlias(final String aliasName) {
        if (StringUtils.isBlank(aliasName)) {
            throw new IllegalArgumentException("aliasName cannot be blank");
        }
        try {
            final GetAliasRequest request = new GetAliasRequest.Builder().name(aliasName).build();
            final GetAliasResponse response = client.indices().getAlias(request);
            if (!response.result().isEmpty()) {
                return response.result().keySet();
            } else {
                logger.info("No index with alias {} found", aliasName);
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getIndexByAlias(final String aliasName) {
        if (StringUtils.isBlank(aliasName)) {
            throw new IllegalArgumentException("aliasName cannot be blank");
        }
        final Set<String> indexes = getIndexesByAlias(aliasName);
        return !indexes.isEmpty()
                ? indexes.iterator().next()
                : null;
    }

    @Override
    public void addAlias(final String indexName, final String aliasName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        if (StringUtils.isBlank(aliasName)) {
            throw new IllegalArgumentException("aliasName cannot be blank");
        }
        try {
            final PutAliasRequest aliasRequest = new PutAliasRequest.Builder()
                    .index(indexName)
                    .alias(aliasName)
                    .build();
            final PutAliasResponse response = client.indices().putAlias(aliasRequest);
            logger.info("Added alias {} to index {}", aliasName, indexName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAlias(final String indexName, final String aliasName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        if (StringUtils.isBlank(aliasName)) {
            throw new IllegalArgumentException("aliasName cannot be blank");
        }
        try {
            DeleteAliasRequest request = DeleteAliasRequest.of(d -> d
                    .index(indexName)
                    .name(aliasName)
            );
            DeleteAliasResponse response = client.indices().deleteAlias(request);
            logger.info("Deleted alias {} from index {}", aliasName, indexName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String moveAlias(final String indexName, final String aliasName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("indexName cannot be blank");
        }
        if (StringUtils.isBlank(aliasName)) {
            throw new IllegalArgumentException("aliasName cannot be blank");
        }
        final String oldIndexName = getIndexByAlias(aliasName);
        addAlias(indexName, aliasName);
        if (oldIndexName != null) {
            removeAlias(oldIndexName, aliasName);
            return oldIndexName;
        }
        return null;
    }
}
