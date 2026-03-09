package org.example.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.example.deserializer.PriceDeserializer;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements IndexableDocument {
    @JsonProperty("id")
    @JsonAlias("asin")
    private String id;

    private String title;
    private String description;
    private String brand;
    private List<String> category;

    @JsonProperty("fts_embedding")
    private List<Float> ftsEmbedding;
    
    @JsonDeserialize(using = PriceDeserializer.class)
    private BigDecimal price;

    @JsonProperty("image")
    @JsonAlias("imageURLHighRes")
    private List<String> image;

    @JsonProperty("description")
    public void setDescription(List<String> descriptions) {
        if (descriptions != null && !descriptions.isEmpty()) {
            this.description = descriptions.getFirst();
        }
    }

    @JsonProperty("category")
    public void setCategory(List<String> category) {
        if (category == null || category.isEmpty()) {
            this.category = null;
        } else {
            final int catSize = Math.min(5, category.size());
            this.category = category.subList(1, catSize);
        }
    }

    public String generateFts() {
        final StringBuilder builder = new StringBuilder();

        if (getTitle() != null) {
            builder.append(getTitle());
            builder.append(" ");
        }

        if (getDescription() != null) {
            builder.append(getDescription());
            builder.append(" ");
        }

        if (getBrand() != null) {
            builder.append(getBrand());
            builder.append(" ");
        }

        if (getCategory() != null && !getCategory().isEmpty()) {
            builder.append(getCategory().getLast());
        }
        return builder.toString();
    }
}
