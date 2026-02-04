package org.example.config;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Bean
    public OpenSearchClient openSearchClient(OpenSearchProperties properties) {
        final HttpHost host = new HttpHost(properties.getProtocol(), properties.getHost(), properties.getPort());
        final OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(host)
                .build();
        return new OpenSearchClient(transport);
    }
}