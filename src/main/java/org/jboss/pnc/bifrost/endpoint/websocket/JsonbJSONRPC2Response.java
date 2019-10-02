package org.jboss.pnc.bifrost.endpoint.websocket;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.HashMap;
import java.util.Map;

public class JsonbJSONRPC2Response extends JSONRPC2Response {

    private Jsonb jsonb = JsonbBuilder.create();

    public JsonbJSONRPC2Response(Object result, Object id) {
        super(result, id);
    }

    public JsonbJSONRPC2Response(Object id) {
        super(id);
    }

    public JsonbJSONRPC2Response(JSONRPC2Error error, Object id) {
        super(error, id);
    }

    protected Jsonb getJsonb() {
        return jsonb;
    }

    @Override
    public String toJSONString() {
        Map<String, Object> out = new HashMap<>();
        if (getError() != null) {
            out.put("error", getError());
        } else {
            out.put("result", getResult());
        }
        out.put("id", getID());
        out.put("jsonrpc", "2.0");
        return getJsonb().toJson(out);
    }
}
