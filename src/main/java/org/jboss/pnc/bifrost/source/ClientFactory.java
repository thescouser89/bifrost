package org.jboss.pnc.bifrost.source;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ClientFactory {

    private static final String className = ClientFactory.class.getName();

    private final Logger logger = LoggerFactory.getLogger(ClientFactory.class);

    private ElasticSearchConfig config;

    @Inject
    MeterRegistry registry;

    private Counter errCounter;

    @PostConstruct
    void initMetrics() {
        errCounter = registry.counter(className + ".error.count");
    }

    public ClientFactory(ElasticSearchConfig config) {
        this.config = config;
    }

    @Timed
    public RestClient getConnectedClient() throws Exception {
        try {
            List<HttpHost> httpHosts = Arrays.stream(config.getHosts().split(","))
                    .map(h -> HttpHost.create(h))
                    .collect(Collectors.toList());

            HttpHost[] hosts = httpHosts.toArray(new HttpHost[httpHosts.size()]);

            RestClientBuilder builder = RestClient.builder(hosts);

            Optional<String> keyStorePath = config.getKeyStorePath();
            if (keyStorePath.isPresent()) {
                KeyStore truststore = KeyStore.getInstance("PKCS12"); // or jks in case jks file is used
                try (InputStream is = Files.newInputStream(Paths.get(keyStorePath.get()))) {
                    truststore.load(is, config.getKeyStorePass().get().toCharArray());
                }
                SSLContextBuilder sslBuilder = SSLContexts.custom()
                        .loadTrustMaterial(truststore, new TrustSelfSignedStrategy())
                        .loadKeyMaterial(truststore, config.getKeyPass().get().toCharArray());
                final SSLContext sslContext = sslBuilder.build();

                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setSSLContext(sslContext);
                    httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                    httpClientBuilder.disableAuthCaching();
                    return httpClientBuilder;
                });
            }

            RestClient lowLevelRestClient = builder.build();
            return lowLevelRestClient;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException
                | CertificateException e) {
            errCounter.increment();
            throw new ClientConnectionException("Cannot connect to remote Elasticsearch server.", e);
        }
    }
}
