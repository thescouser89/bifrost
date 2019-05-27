package org.jboss.pnc.bifrost.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.pnc.bifrost.common.ObjectMapperProvider;
import org.jboss.pnc.bifrost.common.Strings;
import org.jboss.pnc.bifrost.common.scheduler.BackOffRunnableConfig;
import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.common.scheduler.Subscriptions;
import org.jboss.pnc.bifrost.source.ElasticSearch;
import org.jboss.pnc.bifrost.source.ElasticSearchConfig;
import org.jboss.pnc.bifrost.source.ResultProcessor;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ServerEndpoint("/socket")
public class Socket {

    //TODO configurable
    BackOffRunnableConfig backOffRunnableConfig = new BackOffRunnableConfig(1000L, 10, 5 * 60000, 1000);

    ElasticSearch elasticSearch;

    Subscriptions subscriptions;

    private final ObjectMapper mapper = ObjectMapperProvider.get();

    @Inject
    public Socket(ElasticSearchConfig elasticSearchConfig) throws Exception {
        elasticSearch = new ElasticSearch(elasticSearchConfig);
        subscriptions = new Subscriptions();
    }

    @OnOpen
    public void open(Session session) {
    }

    @OnClose
    public void close(Session session) {
        unsubscribeSession(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        //TODO log error
        unsubscribeSession(session.getId());
    }

    private void unsubscribeSession(String sessionId) {
        subscriptions.getAll().stream()
        .filter(s -> s.getClientId().equals(sessionId))
        .forEach(s -> subscriptions.unsubscribe(s));
    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        try {
            RemoteEndpoint.Basic remote = session.getBasicRemote();
            JsonNode node = mapper.readTree(message);
            Action action = Action.valueOf(node.get("action").asText());
            if (action.equals(Action.GET)) {
                ActionGet actionGet = mapper.treeToValue(node, ActionGet.class);
                Consumer<Line> onLine = (line) -> {
                    send(remote, line);
                };

                //run async to allow request completion
                subscriptions.submit(() ->
                        {
                            try {
                                elasticSearch.get(
                                        Strings.toMap(actionGet.getMatchFilters()),
                                        Strings.toMap(actionGet.getPrefixFilters()),
                                        Optional.ofNullable(actionGet.getAfterLine()),
                                        actionGet.getDirection(),
                                        actionGet.getMaxLines(),
                                        onLine);
                            } catch (IOException e) {
                                e.printStackTrace(); //TODO
                            }
                        }
                );

            } else if (action.equals(Action.SUBSCRIBE)) {
                ActionSubscribe actionSubscribe = mapper.treeToValue(node, ActionSubscribe.class);
                Consumer<Line> onLine = line -> {
                    send(remote, line);
                };

                String matchFilters = actionSubscribe.getMatchFilters();
                String prefixFilters = actionSubscribe.getPrefixFilters();

                Subscription subscription = new Subscription(session.getId(), actionSubscribe.getMatchFilters() + actionSubscribe.getPrefixFilters());

                ResultProcessor resultProcessor = new ResultProcessor(elasticSearch);
                Consumer<Subscriptions.TaskParameters<Line>> searchTask = (parameters) -> {
                    Optional<Line> lastResult = Optional.ofNullable(parameters.getLastResult());
                    try {
                        resultProcessor.get(
                                matchFilters,
                                prefixFilters,
                                lastResult,
                                Direction.ASC,
                                1000 //TODO configurable
                                );
                    } catch (IOException e) {
                        e.printStackTrace(); //TODO
                    }
                };

                subscriptions.subscribe(
                        subscription,
                        searchTask,
                        Optional.empty(), //TODO
                        onLine,
                        backOffRunnableConfig
                );

            } else if (action.equals(Action.UNSUBSCRIBE)) {
                ActionUnSubscribe actionUnSubscribe = mapper.treeToValue(node, ActionUnSubscribe.class);
                Subscription subscription = new Subscription(session.getId(), actionUnSubscribe.getMatchFilters() + actionUnSubscribe.getPrefixFilters());
                subscriptions.unsubscribe(subscription);
            } else {
                //TODO invalid
            }
        } catch (IOException e) {
            //TODO
            e.printStackTrace();
        }
    }

    private void send(RemoteEndpoint.Basic remote, Line line) {
        try {
            remote.sendText(mapper.writeValueAsString(line));
        } catch (JsonProcessingException e) {
            //TODO
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();//TODO
        }
    }
}
