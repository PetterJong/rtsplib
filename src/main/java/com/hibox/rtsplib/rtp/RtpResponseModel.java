package com.hibox.rtsplib.rtp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RtpResponseModel {
    private String stateLine;
    private String version;
    private int state;
    private int code;
    private Map<String, String> headers = new HashMap();
    private byte[] body;

    public RtpResponseModel() {
    }

    public String getStateLine() {
        return this.stateLine;
    }

    public void setStateLine(String stateLine) {
        this.stateLine = stateLine;
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

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.version).append(" ").append(this.code).append(" ").append(this.stateLine).append("\r\n");
        Iterator i$ = this.headers.entrySet().iterator();

        while(i$.hasNext()) {
            Map.Entry head = (Map.Entry)i$.next();
            sb.append((String)head.getKey()).append(": ").append((String)head.getValue()).append("\r\n");
        }

        sb.append("\r\n");
        return sb.toString();
    }
}