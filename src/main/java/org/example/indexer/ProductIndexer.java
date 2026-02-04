package org.example.indexer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Product;
import org.example.service.OpenSearchService;
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

@Component
public class ProductIndexer implements Indexer {
    private static final Logger logger = LoggerFactory.getLogger(ProductIndexer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonFactory jsonFactory = objectMapper.getFactory();

    private final Integer batchSize;
    private final String indexName;
    private final OpenSearchService openSearchService;

    @Autowired
    public ProductIndexer(
            OpenSearchService openSearchService,
            @Value("${indexing.product.batch-size}") Integer batchSize,
            @Value("${indexing.product.index-name}") String indexName) {
        this.openSearchService = openSearchService;
        this.batchSize = batchSize;
        this.indexName = indexName;
    }

    public int indexFromFile(final String filePath) {
        return indexFromFile(filePath, null);
    }

    public int indexFromFile(final String filePath, @Nullable final Integer limit) {
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
                if (nodeList.size() >= batchSize) {
                    indexed += bulkIndexRecords(nodeList);
                    nodeList.clear();
                }
            }

            // bulk index any remaining lines less than batch size
            if (!nodeList.isEmpty()) {
                indexed += bulkIndexRecords(nodeList);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return indexed;
        }

        return indexed;
    }

    public int bulkIndexRecords(final List<JsonNode> nodeList) {
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
