/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

import org.apache.http.HttpRequest;

/**
 * Error raised when a channel operation fails due to the associated consumer
 * not being recognized by the streaming service.
 */
public class ConsumerError extends Exception {

    private int statusCode;
    private Throwable cause;
    private String api;
    private HttpRequest httpRequest;

    public ConsumerError(final String message) {
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
