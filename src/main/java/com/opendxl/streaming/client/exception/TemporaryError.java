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

    /**
     * @param message error description
     */
    public TemporaryError(final String message) {
        super(message);
    }

    /**
     * Get the HTTP Status Code that caused the TemporaryError.
     *
     * @return HTTP Status Code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP Status Code which caused the TemporaryError to be thrown.
     *
     * @param statusCode if TemporaryError is caused by an HTTP Client or Server error, then it is set to the HTTP
     *                   status code. If TemporaryError is not due to an HTTP error, then it is set to zero.
     */
    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the exception which caused the TemporaryError.
     *
     * @return original exception
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Sets the exception which caused the TemporaryError to be thrown.
     *
     * @param cause if TemporaryError is thrown as consequence of another exception. If TemporaryError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public void setCause(final Throwable cause) {
        this.cause = cause;
    }

    /**
     * Gets the {@link com.opendxl.streaming.client.Channel} method name in which the TemporaryError occurred.
     *
     * @return Channel API name
     */
    public String getApi() {
        return api;
    }

    /**
     * Sets the {@link com.opendxl.streaming.client.Channel} method name in which the TemporaryError occurred.
     *
     * @param api the Channel method name, e.g.: create, subscribe, consume, commit, run
     */
    public void setApi(final String api) {
        this.api = api;
    }

    /**
     * Gets the HttpRequest in which the TemporaryError occurred.
     *
     * @return http request which was sent when TemporaryError occurred.
     */
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Sets the HttpRequest in which the TemporaryError occurred.
     *
     * @param httpRequest http request which was sent when TemporaryError occurred.
     */
    public void setHttpRequest(final HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

}
