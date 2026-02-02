package org.example.config;

import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OpenSearchClientConfig extends AbstractOpenSearchConfiguration {

    @Value("${opensearch.uris}")
    private String uris;

    @Override
    @Bean
    public RestHighLevelClient opensearchClient() {

        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(uris)
                .build();

        return RestClients.create(clientConfiguration).rest();
    }
}
