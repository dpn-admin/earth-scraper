package org.chronopolis.earth.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Information regarding http calls
 *
 * Created by shake on 8/12/16.
 */
@Entity(name = "HttpDetail")
@SuppressWarnings("WeakerAccess")
public class HttpDetail {

    @Id
    Long id;

    String url;
    String requestBody;
    String requestMethod;

    String responseBody;
    int responseCode = -1;

    public Long getId() {
        return id;
    }

    public HttpDetail setId(Long id) {
        this.id = id;
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

}
