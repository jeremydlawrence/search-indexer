package org.example.indexer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.ProductIndexProperties;
import org.example.model.Product;
import org.example.service.OpenSearchService;
import org.example.util.IndexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class FullProductIndexer implements Indexer {
    private static final Logger logger = LoggerFactory.getLogger(FullProductIndexer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonFactory jsonFactory = objectMapper.getFactory();

    private final OpenSearchService openSearchService;
    private final ProductIndexProperties indexProperties;

    @Autowired
    public FullProductIndexer(
            OpenSearchService openSearchService,
            ProductIndexProperties indexProperties,
            @Value("${indexing.product.batch-size}") Integer batchSize,
            @Value("${indexing.product.index-name}") String indexName) {
        this.openSearchService = openSearchService;
        this.indexProperties = indexProperties;
    }

    protected String init() {
        final String newIndexName = IndexUtils.getIndexName(indexProperties.getAlias());
        openSearchService.createIndex(
                newIndexName,
                indexProperties.getSettings(),
                indexProperties.getMapping());

        return newIndexName;
    }

    @Override
    public void finalizer(final String newIndexName) {
        // commit docs to index
        openSearchService.flushIndex(newIndexName);

        // update to post-index settings
        openSearchService.updateSettings(
                newIndexName,
                indexProperties.getReplicas(),
                indexProperties.getRefreshSeconds());

        // wait for the cluster/index to rebalance
        final boolean isGreen = openSearchService.waitForGreenStatus(newIndexName, indexProperties.getStatusWaitSeconds());

        // TODO: Implement any other desired checks on the new index before moving the alias

        if (isGreen) {
            // move active alias to the new index and mark the previous as old
            final String oldIndexName = openSearchService.moveAlias(newIndexName, indexProperties.getAlias());
            if (oldIndexName != null) {
                openSearchService.addAlias(oldIndexName, indexProperties.getOldAlias());
            }
        } else {
            logger.error("Index {} has been created but is not in a good state", newIndexName);
        }

        // clean up old indexes
        final Set<String> oldIndexNames = openSearchService.getIndexesByAlias(indexProperties.getOldAlias());
        oldIndexNames.forEach(indexName -> {
            if (IndexUtils.shouldDeleteIndex(indexName, indexProperties.getOldIndexKeepDays())) {
                 openSearchService.deleteIndex(indexName);
            }
        });
    }

    @Override
    public int indexFromFile(final String filePath) {
        return indexFromFile(filePath, null);
    }

    @Override
    public int indexFromFile(final String filePath, @Nullable final Integer limit) {
        final String newIndexName = init();

        final List<JsonNode> nodeList = new ArrayList<>();
        final int maxRecords = limit == null ? Integer.MAX_VALUE : limit;
        int lineCount = 0;
        int indexed = 0;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null && lineCount < maxRecords) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                lineCount++;

                // bulk index with batch size
                nodeList.add(getJsonNodeFromLine(line));
                if (nodeList.size() >= indexProperties.getBatchSize()) {
                    indexed += bulkIndexRecords(nodeList, newIndexName);
                    nodeList.clear();
                }
            }

            // bulk index any remaining lines less than batch size
            if (!nodeList.isEmpty()) {
                indexed += bulkIndexRecords(nodeList, newIndexName);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return indexed;
        }

        // Finalize indexing operation
        finalizer(newIndexName);

        return indexed;
    }

    @Override
    public int bulkIndexRecords(final List<JsonNode> nodeList, final String indexName) {
        final List<Product> products = nodeList.stream()
                .map(node -> objectMapper.convertValue(node, Product.class))
                .toList();
        openSearchService.bulkIndex(products, indexName);
        return nodeList.size();
    }

    private JsonNode getJsonNodeFromLine(String line) {
        try (JsonParser parser = jsonFactory.createParser(line)) {
            return objectMapper.readTree(parser);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
}
