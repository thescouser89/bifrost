package org.jboss.pnc.bifrost.source;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ElasticSearch {

    private final Logger logger = LoggerFactory.getLogger(ElasticSearch.class);

    private RestClient lowLevelRestClient;
    private RestHighLevelClient client;

    private String[] indexes;

    public ElasticSearch(ElasticSearchConfig elasticSearchConfig) throws Exception {
        lowLevelRestClient = new ClientFactory(elasticSearchConfig).getConnectedClient();
        this.indexes = elasticSearchConfig.getIndexes().split(",");
        client = new RestHighLevelClient(lowLevelRestClient);
    }

    public void close() {
        try {
            lowLevelRestClient.close();
        } catch (IOException e) {
            e.printStackTrace(); //TODO
        }
    }

    /**
     * Queries the source and call onLine in the same thread when a new line is received.
     * Method returns when all the lines are fetched.
     */
    public void get(
            Map<String, String> matchFilters,
            Map<String, String> prefixFilters,
            Optional<Line> searchAfter,
            Direction direction,
            int maxLines,
            Consumer<Line> onLine) throws IOException {
        logger.info("Searching ...");
        BoolQueryBuilder queryBuilder = getQueryBuilder(matchFilters, prefixFilters);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder
                .query(queryBuilder)
                .size(maxLines + 1)
                .from(0)
                //TODO need _id as doc type https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-search-after.html
            //    .sort(new FieldSortBuilder("timestamp").order(direction.getSortOrder()))
            //    .sort(new FieldSortBuilder("id").order(direction.getSortOrder()))
        ;
        if (searchAfter.isPresent()) {
            Object[] searchAfterTimeStampId = new Object[]{searchAfter.get().getTimestamp(), searchAfter.get().getId()};
            sourceBuilder.searchAfter(searchAfterTimeStampId);
        } else {
            //TODO tailFromNow vs tailFromBeginning
            RangeQueryBuilder timestampRange = QueryBuilders.rangeQuery("timestamp");
            //timestampRange.from(System.currentTimeMillis() - 5000); //TODO parametrize how long back
            queryBuilder.must(timestampRange);
        }

        SearchRequest searchRequest = new SearchRequest(indexes);
        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest);

        SearchHits responseHits = response.getHits();
        logger.info("Total hits: {}, limited to {}.", responseHits.getTotalHits(), maxLines);
        int hitNum = 0;

        //loop until maxLines or all the elements are read
        //note that (maxLines + 1) is used as a limit in the query to check if there are more results
        Iterator<SearchHit> responseHitIterator = responseHits.iterator();
        while (responseHitIterator.hasNext() && hitNum < maxLines) {
            hitNum++;
            SearchHit hit = responseHitIterator.next();
            boolean last = !responseHitIterator.hasNext();
            Line line = getLine(hit, last);
            onLine.accept(line);
        }
    }

    private Line getLine(SearchHit hit, boolean last) {
        Map<String, Object> source = hit.getSourceAsMap();
        logger.info("Received line {}", source); //TODO debug

        String id = source.get("id").toString();
        String timestamp = source.get("timestamp").toString();
        String logger = source.get("logger").toString();
        String message = source.get("message").toString();
        String ctx = source.get("ctx").toString();
        boolean tmp = Boolean.parseBoolean(source.get("tmp").toString());
        String exp = "exp";
        String expire = getString(source, exp);

        this.logger.info("Constructing line ...");
//        return new Line(id, timestamp, logger, message, last, ctx, tmp, expire);
        return Line.newBuilder()
                .id(id)
                .timestamp(timestamp)
                .logger(logger)
                .message(message)
                .last(last)
                .ctx(ctx)
                .tmp(tmp)
                .exp(expire)
                .build();
    }

    private String getString(Map<String, Object> source, String fieldName) {
        Object obj = source.get(fieldName);
        if (obj == null) {
            return null;
        } else {
            return obj.toString();
        }
    }

    private BoolQueryBuilder getQueryBuilder(Map<String, String> matchFilters, Map<String, String> prefixFilters) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        matchFilters.forEach((name, text) -> queryBuilder.must(QueryBuilders.matchQuery(name, text)));
        prefixFilters.forEach((name, text) -> queryBuilder.must(QueryBuilders.prefixQuery(name, text)));
        return queryBuilder;
    }
}
