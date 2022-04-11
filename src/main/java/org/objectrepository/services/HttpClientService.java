package org.objectrepository.services;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Date;

public class HttpClientService {

    final private static int TIMEOUT = 5000;

    final private static Logger log = Logger.getLogger(HttpClientService.class);
    private final CloseableHttpClient httpclient;
    private final RequestConfig.Builder requestConfig;
    private String endpoint;
    private String token;
    private String hostname;
    private String pipeline;

    public HttpClientService() {
        this.httpclient = HttpClients.createDefault();
        this.requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(TIMEOUT);
        requestConfig.setConnectionRequestTimeout(TIMEOUT);
        requestConfig.setSocketTimeout(TIMEOUT);
    }

    void status(String identifier, String messageQueue, int status, String info, Date date) {

        log.debug("info=" + info);

        final HttpPost httpPost = new HttpPost(endpoint + "/queue/" + identifier);
        httpPost.setConfig(requestConfig.build());
        httpPost.setHeader("User-Agent", "PMQ Agent");
        httpPost.setHeader("Agent-Hostname", this.hostname);
        httpPost.setHeader("Agent-Pipeline", this.pipeline);
        httpPost.setHeader("Authorization", "Bearer " + this.token);
        httpPost.setHeader("Content-Type", "application/json");
        final String[] split = messageQueue.split(":", 3);
        final String queue = split[split.length-1];
        final String message = String.format("{\"queue\": \"%s\", \"status\": %s, \"info\": \"%s\", \"date\": \"%s\"}", queue, status, escapeToJson(info), date);
        final StringEntity body = new StringEntity(message, "UTF-8");
        httpPost.setEntity(body);

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpclient.execute(httpPost);
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (httpResponse != null) {
                try {
                    final StatusLine statusLine = httpResponse.getStatusLine();
                    log.info(statusLine.toString());
                    if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                        log.debug(EntityUtils.toString(httpResponse.getEntity(), "UTF-8"));
                    }
                } catch (IOException e) {
                    log.error(e);
                } finally {
                    try {
                        httpResponse.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }
    }

    private String escapeToJson(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        char c;
        int i;
        int len = text.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        for (i = 0; i < len; i += 1) {
            c = text.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                case '/':
                    sb.append('\\').append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }
}
