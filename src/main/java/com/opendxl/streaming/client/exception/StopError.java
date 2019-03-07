/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

import org.apache.http.HttpRequest;

/**
 * Exception raised for an operation which is interrupted due to the channel being stopped.
 */
public class StopError extends Exception {

    private int statusCode;
    private Throwable cause;
    private String api;
    private HttpRequest httpRequest;

    public StopError(final String message) {
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
