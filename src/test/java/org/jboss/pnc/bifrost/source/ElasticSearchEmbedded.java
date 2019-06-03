package org.jboss.pnc.bifrost.source;

import org.jboss.pnc.bifrost.test.ElasticsearchEmbeddedTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ElasticsearchEmbeddedTest
public class ElasticSearchEmbedded {

    private static Logger logger = LoggerFactory.getLogger(ElasticSearchEmbedded.class);

    private static String[] indexes = new String[] { "test" };

    private String defaultLogger = "org.jboss.pnc._userlog_";

    private static ElasticSearchConfig elasticSearchConfig = ElasticSearchConfig.newBuilder()
        .hosts("http://localhost:9200")
        .indexes(Arrays.asList(indexes).toString())
        .build();

    private static ClientFactory clientFactory = new ClientFactory(elasticSearchConfig);
    //TODO end-to-end test
    /*
    @BeforeAll
    public static void beforeTest() throws Exception {
        RestClient lowLevelRestClient = clientFactory.getConnectedClient();

        Map<String, String> typeKeyword = Collections.singletonMap("type", "keyword");
        Map<String, String> typeDate = Collections.singletonMap("type", "date");

        Map<String, Object> fileds = new HashMap<>();
        fileds.put("logger", typeKeyword);
        fileds.put("ctx", typeKeyword);
        fileds.put("id", typeKeyword);
        fileds.put("timestamp", typeDate);

        Map<String, Object> properties = Collections.singletonMap("properties", fileds);
        Map<String, Object> doc = Collections.singletonMap("doc", properties);
        Map<String, Object> mappings = Collections.singletonMap("mappings", doc);

        String jsonString = ObjectMapperProvider.get().writeValueAsString(mappings);

        logger.info("Json create index: {}", jsonString);

        HttpEntity entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON);
        Request request = new Request("PUT", "/test", Collections.emptyMap(), entity);
        lowLevelRestClient.performRequest(request);
        lowLevelRestClient.close();
    }

    @Test
    public void shouldInsertAndReadData() throws Exception {

        Line insertLine = LineProducer.getLine(1, true, "fddgfh");

        byte[] jsonLine = ObjectMapperProvider.get().writeValueAsBytes(insertLine);
        logger.info("json line: {}", new String(jsonLine));

        RestClient restClient = clientFactory.getConnectedClient();
        RestHighLevelClient client = new RestHighLevelClient(restClient);
        IndexRequest indexRequest = new IndexRequest("test", "doc", "1");
        indexRequest.source(jsonLine, XContentType.JSON);
        IndexResponse response = client.index(indexRequest);

        logger.info("Response status: {}", response.status());
        Assertions.assertEquals(RestStatus.CREATED, response.status());

        //wait to stabilize
        Thread.sleep(1000);

        ElasticSearch elasticSearchSource = new ElasticSearch(elasticSearchConfig);

        Map<String, String> noFilters = Collections.emptyMap();

        AtomicReference<Line> receivedLine = new AtomicReference<>();
        Consumer<Line> onLine = line -> {
            logger.info("Found line: {}", line);
            receivedLine.set(line);
        };
        elasticSearchSource.get(noFilters, noFilters, Optional.empty(), Direction.ASC, 10, onLine);

        Wait.forCondition(() -> receivedLine.get() != null, 10L, ChronoUnit.SECONDS);
        restClient.close();
    }

    @Test
    public void shouldGetLinesMatchingCtx()
            throws Exception {

        insertLine(2, "def");
        insertLine(10, "abc");
        Thread.sleep(1000);

        ElasticSearch elasticSearch = new ElasticSearch(elasticSearchConfig);

        List<Line> anyLines = new ArrayList<>();
        Consumer<Line> anyLine = (line -> {
            logger.info("Found line: {}", line);
            anyLines.add(line);
        });

        Map<String, String> defaultLogMatcher = Collections.singletonMap("logger", defaultLogger);
        elasticSearch.get(Collections.emptyMap(), defaultLogMatcher, Optional.empty(), Direction.ASC, 100, anyLine);
        Wait.forCondition(() -> anyLines.size() == 12, 5L, ChronoUnit.SECONDS, () -> "Found " + anyLines.size() + " lines while expecting 12.");

        Map<String, String> matchFilters = new HashMap<>();
        matchFilters.put("ctx", "abc");
        List<Line> matchingLines = new ArrayList<>();
        Consumer<Line> onLine = (line -> {
            logger.info("Found line: {}", line);
            matchingLines.add(line);
        });
        elasticSearch.get(matchFilters, defaultLogMatcher, Optional.empty(), Direction.ASC, 100, onLine);
        Wait.forCondition(() -> matchingLines.size() == 10, 5L, ChronoUnit.SECONDS, () -> "Found " + matchingLines.size() + " matching lines while expecting 10.");
        elasticSearch.close();
    }

    @Test
    public void shouldGetLinesAfter() throws Exception {
        insertLine(5, "should");
        Thread.sleep(100);
        insertLine(5, "should");
        Thread.sleep(1000);

        ElasticSearch elasticSearch = new ElasticSearch(elasticSearchConfig);
        ResultProcessor source = new ResultProcessor(elasticSearch);

        List<Line> lines = source.get("ctx:should", "logger:" + defaultLogger, Optional.empty(), Direction.ASC, 5);

        Assertions.assertEquals(5, lines.size());
        lines.forEach(System.out::println);


        Line afterLine = lines.get(4);
        List<Line> newLines = source.get("ctx:should", "logger:" + defaultLogger, Optional.of(afterLine), Direction.ASC, 5);

        Assertions.assertEquals(5, newLines.size());
        Assertions.assertTrue(
                Long.parseLong(lines.get(4).getTimestamp()) < Long.parseLong(newLines.get(0).getTimestamp())
                );
        newLines.forEach(System.out::println);
        elasticSearch.close();
    }

    @Test
    public void shouldGetAllLines() throws Exception {
        insertLine(20, "all");
        Thread.sleep(1000);

        ElasticSearch elasticSearch = new ElasticSearch(elasticSearchConfig);
        ResultProcessor source = new ResultProcessor(elasticSearch);

//        List<Line> lines = new ArrayList<>();
//        Consumer<Line> onLine = line -> {
//            lines.add(line);
//        };
        List<Line> lines = source.get(
                "ctx=all",
                "logger=" + defaultLogger,
                Optional.empty(),
                Direction.ASC,
                4);

//        Wait.forCondition(()->lines.size() == 20, 3, ChronoUnit.SECONDS);

        Assertions.assertEquals(20, lines.size());
        lines.forEach(System.out::println);
        elasticSearch.close();
    }

//    @Test
    public void shouldSubscribeToNewLines() throws JsonProcessingException, InterruptedException, TimeoutException {
        //TODO test shouldSubscribeToNewLines
    }

    private void insertLine(Integer numberOfLines, String ctx) throws Exception {
        List<Line> lines = LineProducer.getLines(numberOfLines, ctx);
        for (Line line : lines) {
            byte[] jsonLine = ObjectMapperProvider.get().writeValueAsBytes(line);
            RestClient restClient = clientFactory.getConnectedClient();
            RestHighLevelClient client = new RestHighLevelClient(restClient);
            IndexRequest indexRequest = new IndexRequest("test", "doc");
            indexRequest.source(jsonLine, XContentType.JSON);
            IndexResponse response = client.index(indexRequest);

            Assertions.assertEquals(RestStatus.CREATED, response.status());
            restClient.close();
        }
    }
*/
}