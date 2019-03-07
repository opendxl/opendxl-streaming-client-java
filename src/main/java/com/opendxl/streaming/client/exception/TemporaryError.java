/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

import org.apache.http.HttpRequest;

/**
 * Exception raised when an unexpected/unknown (but possibly recoverable) error occurs.
 */
public class TemporaryError extends Exception {

    private int statusCode;
    private Throwable cause;
    private String api;
    private HttpRequest httpRequest;

    public TemporaryError(final String message) {
        super(message);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(final Throwable cause) {
        this.cause = cause;
    }

    public String getApi() {
        return api;
    }

    public void setApi(final String api) {
        this.api = api;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(final HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

}
