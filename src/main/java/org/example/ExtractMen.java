package org.example;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

public class ExtractMen {

    public static void main(String[] args) {
        String inputFile = "src/main/resources/products-full.json";
        String outputFile = "products-men.json";
        
        try {
            extractMenProducts(inputFile, outputFile);
            System.out.println("Successfully extracted men's products to " + outputFile);
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void extractMenProducts(String inputFile, String outputFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = objectMapper.getFactory();
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile));
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            
            String line;
            boolean firstProduct = true;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try (JsonParser parser = jsonFactory.createParser(line)) {
                    JsonNode jsonNode = objectMapper.readTree(parser);
                    
                    if (hasCategoryMen(jsonNode)) {
                        if (!firstProduct) {
                            writer.newLine();
                        }
                        writer.write(line);
                        firstProduct = false;
                    }
                }
            }
        }
    }
    
    private static boolean hasCategoryMen(JsonNode product) {
        JsonNode categoryNode = product.get("category");
        
        if (categoryNode == null || !categoryNode.isArray()) {
            return false;
        }
        
        for (JsonNode category : categoryNode) {
            if (category.isTextual() && "Men".equals(category.asText())) {
                return true;
            }
        }
        
        return false;
    }
}
