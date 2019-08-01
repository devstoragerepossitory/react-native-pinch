package com.localz.pinch.models;

import org.json.JSONObject;

public class HttpRequest {
    public String endpoint;
    public String method;
    public JSONObject headers;
    public String body;
    public String[] sslPinningCerts=new String[]{};
    public String mutualAuthCert="";
    public String mutualAuthPassword="";
    public int timeout;

    private static final int DEFAULT_TIMEOUT = 0;

    public HttpRequest() {
        this.timeout = DEFAULT_TIMEOUT;
    }

    public HttpRequest(String endpoint) {
        this.endpoint = endpoint;
        this.timeout = DEFAULT_TIMEOUT;
    }

    public HttpRequest(String endpoint, String method, JSONObject headers, String body, String[] sslPinningCerts, int timeout) {
        this.endpoint = endpoint;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.sslPinningCerts = sslPinningCerts;
        this.timeout = timeout;
    }
}
