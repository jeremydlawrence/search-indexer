package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {
    private String protocol;
    private String host;
    private int port;
    private String path;
    private int charLimit;
}
