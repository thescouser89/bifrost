package org.jboss.pnc.bifrost.endpoint.provider;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
//@ApplicationScoped
public class Factory {

//    private final ElasticSearch elasticSearch;
//    private final ResultProcessor source;
//    private final Subscriptions subscriptions;
//
//
//    @Inject
//    public Factory(ElasticSearchConfig elasticSearchConfig) throws Exception {
//        elasticSearch = new ElasticSearch(elasticSearchConfig);
//        this.source = new ResultProcessor(elasticSearch);
//
//        subscriptions = new Subscriptions();
//    }
//
//    @PreDestroy
//    public void preDestroy() {
//        try {
//            close();
//        } catch (Exception e) {
//            e.printStackTrace(); //TODO
//        }
//    }
//
//    @Produces
//    public ElasticSearch getElasticSearch() {
//        return elasticSearch;
//    }
//
//    @Produces
//    public Subscriptions getSubscriptions() {
//        return subscriptions;
//    }
//
//    public void close() throws Exception {
//        subscriptions.unsubscribeAll();
//        elasticSearch.close();
//    }
}
