package org.jboss.pnc.bifrost.endpoint.websocket;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.apache.commons.beanutils.BeanUtils;
import org.jboss.logging.Logger;
import org.jboss.pnc.bifrost.common.scheduler.Subscriptions;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ServerEndpoint("/socket")
public class Socket {

    private Logger logger = Logger.getLogger(Socket.class);

    @Inject
    Subscriptions subscriptions;

    @Inject
    MethodFactory methodFactory;

    private final Jsonb jsonb = JsonbBuilder.create();

    private SendHandler commandResponseHandler = result -> {
        if (!result.isOK()) {
            logger.error("Error sending command response.", result.getException());
        }
    };

    private SendHandler lineResponseHandler = result -> {
        if (!result.isOK()) {
            logger.error("Error sending log line.", result.getException());
        }
    };

    private String LOGLINE_NOTIFICATION = "LOG";

    @OnOpen
    public void open(Session session) {
    }

    @OnClose
    public void close(Session session) {
        unsubscribeSession(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("Socket communication error.", error);
        unsubscribeSession(session.getId());
    }

    private void unsubscribeSession(String sessionId) {
        subscriptions.getAll().stream()
        .filter(s -> s.getClientId().equals(sessionId))
        .forEach(s -> subscriptions.unsubscribe(s));
    }


    @OnMessage
    public void handleMessage(String message, Session session) {
        logger.debug("Received message: " + message);
        RemoteEndpoint.Async remote = session.getAsyncRemote();

        JSONRPC2Request request;
        try {
            request = JSONRPC2Request.parse(message);
        } catch (JSONRPC2ParseException e) {
            String err = "Cannot parse request.";
            logger.error(err, e);
            Result result = new Result(Result.Status.ERROR, err + e.getMessage());
            sendResult(remote, "undefined", result);
            return;
        }
        Object requestId = request.getID();

        Optional<Method<?>> maybeMethod = methodFactory.get(request.getMethod());
        if (!maybeMethod.isPresent()) {
            String err = "Unsupported method " + request.getMethod();
            logger.warn(err);
            Result result = new Result(Result.Status.ERROR, err);
            sendResult(remote, request, result);
            return;
        }
        Method method = maybeMethod.get();
        method.setSession(session);
        logger.info("Requested method: " + method.getName());

        Object methodParameter = null;
        try {
            methodParameter = method.getParameterType().getConstructor(new Class[] {}).newInstance(new Object[] {});
            BeanUtils.populate(methodParameter, request.getNamedParams());
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String err = "Cannot construct parameters for method: " + request.getMethod() + ".";
            logger.error(err, e);
            Result result = new Result(Result.Status.ERROR, err + e.getMessage());
            sendResult(remote, request, result);
            return;
        }

        Consumer<Line> lineConsumer = line -> sendLine(remote, line);

        Result result = method.apply(methodParameter, lineConsumer);
        logger.debug("Method invoked, result: " + result);
        sendResult(remote, requestId, result);
    }

    private void sendResult(RemoteEndpoint.Async remote, Object requestId, Result result) {
        JSONRPC2Response jsonrpc2Response = new JSONRPC2Response(result, requestId);
        String responseString = jsonrpc2Response.toJSONString();
        remote.sendText(responseString, commandResponseHandler);
        logger.debug("Text response sent: " + responseString );
    }

    private void sendLine(RemoteEndpoint.Async remote, Line line) {
        logger.trace("Sending line as binary message: " + line.asString());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        jsonb.toJson(line, byteArrayOutputStream); // JsonbConfig.ENCODING default is UTF-8
        remote.sendBinary(ByteBuffer.wrap(byteArrayOutputStream.toByteArray()), lineResponseHandler);
    }
}
