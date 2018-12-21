package com.hibox.rtsplib.rtp;

import java.util.HashMap;
import java.util.Map;

public class RtpRequestModel {
    private String method;
    private String uri;
    private String version;
    private int state;
    private Map<String, String> headers = new HashMap();
    private byte[] body;

    public RtpRequestModel() {
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
