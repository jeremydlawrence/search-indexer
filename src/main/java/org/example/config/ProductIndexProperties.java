package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "indexing.product")
public class ProductIndexProperties {
    private String alias;
    private String oldAlias;
    private int oldIndexKeepDays;
    private int batchSize;
    private String settings;
    private String mapping;
    private int replicas;
    private int refreshSeconds;
    private int statusWaitSeconds;
}