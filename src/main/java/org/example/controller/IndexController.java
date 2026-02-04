package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.indexer.ProductIndexer;
import org.example.service.OpenSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Index Controller", description = "API endpoints for product indexing operations")
public class IndexController {
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    private final OpenSearchService openSearchService;
    private final ProductIndexer productIndexer;

    @Autowired
    public IndexController(OpenSearchService openSearchService, ProductIndexer productIndexer) {
        this.openSearchService = openSearchService;
        this.productIndexer = productIndexer;
    }

    @GetMapping("/index-products")
    @Operation(summary = "Index products", description = "Triggers indexing process for products")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product indexing completed successfully"),
        @ApiResponse(responseCode = "500", description = "Error occurred during indexing")
    })
    public String indexProducts() {
        logger.info("Starting product indexing process");
        
        try {
            final int indexed = productIndexer.indexFromFile("src/main/resources/products-men-min.json");
            final String message = String.format("Successfully indexed %d products", indexed);
            logger.info(message);
            return message;
        } catch (Exception e) {
            logger.error("Failed to index products: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to index products: " + e.getMessage(), e);
        }
    }

    @GetMapping("/index-health")
    @Operation(summary = "Check OpenSearch cluster health", description = "Verifies cluster staus of OpenSearch")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OpenSearch is healthy"),
        @ApiResponse(responseCode = "503", description = "Cannot connect to OpenSearch")
    })
    public String checkOpenSearchHealth() {
        final String status = openSearchService.clusterHealth();
        if (status != null) {
            return String.format("OpenSearch status: %s", status);
        } else {
            throw new RuntimeException("Cannot connect to OpenSearch");
        }
    }
}
