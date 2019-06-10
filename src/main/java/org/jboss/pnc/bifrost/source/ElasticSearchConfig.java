package org.jboss.pnc.bifrost.source;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ApplicationScoped
public class ElasticSearchConfig {

    @ConfigProperty(name = "elasticsearch.hosts")
    String hosts;

    @ConfigProperty(name = "elasticsearch.indexes")
    String indexes;

    @ConfigProperty(name = "elasticsearch.keyStorePath", defaultValue = "")
    String keyStorePath;

    @ConfigProperty(name = "elasticsearch.keyStorePass", defaultValue = "")
    String keyStorePass;

    @ConfigProperty(name = "elasticsearch.keyPass", defaultValue = "")
    String keyPass;

    public String getHosts() {
        return hosts;
    }

    public String getIndexes() {
        return indexes;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePass() {
        return keyStorePass;
    }

    public String getKeyPass() {
        return keyPass;
    }

    //    private ElasticSearchConfig(Builder builder) {
//        hosts = builder.hosts;
//        indexes = builder.indexes;
//        keyStorePath = builder.keyStorePath;
//        keyStorePass = builder.keyStorePass;
//        keyPass = builder.keyPass;
//    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ElasticSearchConfig copy) {
        Builder builder = new Builder();
        builder.hosts = copy.getHosts();
        builder.indexes = copy.getIndexes();
        builder.keyStorePath = copy.getKeyStorePath();
        builder.keyStorePass = copy.getKeyStorePass();
        builder.keyPass = copy.getKeyPass();
        return builder;
    }

    public static final class Builder {

        private String hosts;

        private String indexes;

        private String keyStorePath;

        private String keyStorePass;

        private String keyPass;

        private Builder() {
        }

        public Builder hosts(String hosts) {
            this.hosts = hosts;
            return this;
        }

        public Builder indexes(String indexes) {
            this.indexes = indexes;
            return this;
        }

        public Builder keyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
            return this;
        }

        public Builder keyStorePass(String keyStorePass) {
            this.keyStorePass = keyStorePass;
            return this;
        }

        public Builder keyPass(String keyPass) {
            this.keyPass = keyPass;
            return this;
        }

        //        public ElasticSearchConfig build() {
        //            return new ElasticSearchConfig(this);
        //        }
        public ElasticSearchConfig build() {
            ElasticSearchConfig elasticSearchConfig = new ElasticSearchConfig();
            elasticSearchConfig.hosts = hosts;
            elasticSearchConfig.indexes = indexes;
            elasticSearchConfig.keyStorePath = keyStorePath;
            elasticSearchConfig.keyStorePass = keyStorePass;
            elasticSearchConfig.keyPass = keyPass;
            return elasticSearchConfig;
        }
    }
}
