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

    /**
     * @param message error description
     */
    public ConsumerError(final String message) {
        super(message);
    }

    /**
     * Get the HTTP Status Code that caused the ConsumerError.
     *
     * @return HTTP Status Code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP Status Code which caused the ConsumerError to be thrown.
     *
     * @param statusCode if ConsumerError is caused by an HTTP Client or Server error, then it is set to the HTTP
     *                   status code. If ConsumerError is not due to an HTTP error, then it is set to zero.
     */
    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the exception which caused the ConsumerError.
     *
     * @return original exception
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Sets the exception which caused the ConsumerError to be thrown.
     *
     * @param cause if ConsumerError is thrown as consequence of another exception. If ConsumerError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public void setCause(final Throwable cause) {
        this.cause = cause;
    }

    /**
     * Gets the {@link com.opendxl.streaming.client.Channel} method name in which the ConsumerError occurred.
     *
     * @return Channel API name, e.g.: create, subscribe, consume, commit, run
     */
    public String getApi() {
        return api;
    }

    /**
     * Sets the {@link com.opendxl.streaming.client.Channel} method name in which the ConsumerError occurred.
     *
     * @param api the Channel method name, e.g.: create, subscribe, consume, commit, run
     */
    public void setApi(final String api) {
        this.api = api;
    }

    /**
     * Gets the HttpRequest in which the ConsumerError occurred.
     *
     * @return http request which was sent when ConsumerError occurred.
     */
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Sets the HttpRequest in which the ConsumerError occurred.
     *
     * @param httpRequest http request which was sent when ConsumerError occurred.
     */
    public void setHttpRequest(final HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

}
