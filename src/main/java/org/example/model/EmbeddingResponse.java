package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EmbeddingResponse {
    @JsonProperty("vectors")
    private List<List<Float>> vectors;
}
