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
    private String api;
    private HttpRequest httpRequest;

    /**
     * @param message error description
     * @param cause if PermanentError is thrown as consequence of another exception. If PermanentError is not caused by
     *              another exception, then it set to {@code null}.
     * @param statusCode HTTP Status Code that caused the PermanentError. If PermanentError is caused by an HTTP Client
     *                   or HTTP Server error, then it is set to the HTTP status code. If PermanentError is not due to
     *                   an HTTP error, then it is set to zero.
     * @param httpRequest HttpRequest in which the PermanentError occurred
     */
    public PermanentError(final String message, final Throwable cause, final int statusCode,
                          final HttpRequest httpRequest) {

        super(message, cause);
        this.statusCode = statusCode;
        this.httpRequest = httpRequest;

    }

    public PermanentError(final String message, final int statusCode, final HttpRequest httpRequest) {

        this(message, null, statusCode, httpRequest);

    }

    public PermanentError(final String message) {

        this(message, null, 0, null);

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
     * Gets the {@link com.opendxl.streaming.client.Channel} method name in which the PermanentError occurred.
     *
     * @return Channel API name, e.g.: create, subscribe, consume, commit, run
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

}
