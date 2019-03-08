/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

import org.apache.http.HttpRequest;

/**
 * Exception raised for an operation which would not be expected to succeed even if the operation were retried.
 */
public class PermanentError extends Exception {

    private int statusCode;
    private Throwable cause;
    private String api;
    private HttpRequest httpRequest;

    /**
     * @param message error description
     */
    public PermanentError(final String message) {
        super(message);
    }

    /**
     * Get the HTTP Status Code that caused the PermanentError.
     *
     * @return HTTP Status Code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP Status Code which caused the PermanentError to be thrown.
     *
     * @param statusCode if PermanentError is caused by an HTTP Client or Server error, then it is set to the HTTP
     *                   status code. If PermanentError is not due to an HTTP error, then it is set to zero.
     */
    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the exception which caused the PermanentError.
     *
     * @return original exception
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Sets the exception which caused the PermanentError to be thrown.
     *
     * @param cause if PermanentError is thrown as consequence of another exception. If PermanentError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public void setCause(final Throwable cause) {
        this.cause = cause;
    }

    /**
     * Gets the {@link com.opendxl.streaming.client.Channel} method name in which the PermanentError occurred.
     *
     * @return Channel API name
     */
    public String getApi() {
        return api;
    }

    /**
     * Sets the {@link com.opendxl.streaming.client.Channel} method name in which the PermanentError occurred.
     *
     * @param api the Channel method name, e.g.: create, subscribe, consume, commit, run
     */
    public void setApi(final String api) {
        this.api = api;
    }

    /**
     * Gets the HttpRequest in which the PermanentError occurred.
     *
     * @return http request which was sent when PermanentError occurred.
     */
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Sets the HttpRequest in which the PermanentError occurred.
     *
     * @param httpRequest http request which was sent when PermanentError occurred.
     */
    public void setHttpRequest(final HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

}
