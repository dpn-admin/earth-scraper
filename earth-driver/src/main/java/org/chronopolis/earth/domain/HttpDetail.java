package org.chronopolis.earth.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * Information regarding http calls
 *
 * Created by shake on 8/12/16.
 */
@SuppressWarnings("WeakerAccess")
public class HttpDetail {
    private final Logger log = LoggerFactory.getLogger(HttpDetail.class);
    static final String INSERT = "INSERT INTO http_detail(url, request_method, request_body, response_code, response_body, %s) " +
            "VALUES(:url, :requestMethod, :requestBody, :responseCode, :responseBody, :fk)";

    Long httpId;
    String url;
    String requestBody;
    String requestMethod;

    int responseCode = -1;
    String responseBody;

    public Long getHttpId() {
        return httpId;
    }

    public HttpDetail setHttpId(Long httpId) {
        this.httpId = httpId;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public HttpDetail setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public HttpDetail setRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public HttpDetail setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
        return this;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public HttpDetail setResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public HttpDetail setResponseBody(String responseBody) {
        this.responseBody = responseBody;
        return this;
    }

    public void insert(String type, Long fk, Connection conn) {
        String insertDetail = String.format(HttpDetail.INSERT, type);
        // log.info("{}", insertDetail);
        log.debug("Creating http detail for {} - {}", type, fk);
        conn.createQuery(insertDetail)
                .addParameter("url", url)
                .addParameter("requestBody", requestBody)
                .addParameter("requestMethod", requestMethod)
                .addParameter("responseCode", responseCode)
                .addParameter("responseBody", responseBody)
                .addParameter("fk", fk)
                .executeUpdate();
    }
}
