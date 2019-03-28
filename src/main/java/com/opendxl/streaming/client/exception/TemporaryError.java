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
    private String api;
    private HttpRequest httpRequest;

    /**
     * @param message error description
     * @param cause if TemporaryError is thrown as consequence of another exception. If TemporaryError is not caused by
     *              another exception, then it set to {@code null}.
     * @param statusCode HTTP Status Code that caused the TemporaryError. If TemporaryError is caused by an HTTP Client
     *                   or HTTP Server error, then it is set to the HTTP status code. If TemporaryError is not due to
     *                   an HTTP error, then it is set to zero.
     * @param httpRequest HttpRequest in which the TemporaryError occurred
     * @param api the {@link com.opendxl.streaming.client.Channel} method name in which the TemporaryError occurred.
     */
    public TemporaryError(final String message, final Throwable cause, final int statusCode,
                          final HttpRequest httpRequest, final String api) {

        super(message, cause);
        this.statusCode = statusCode;
        this.httpRequest = httpRequest;
        this.api = api;

    }

    /**
     * @param message error description
     * @param statusCode HTTP Status Code that caused the TemporaryError. If TemporaryError is caused by an HTTP Client
     *                   or HTTP Server error, then it is set to the HTTP status code. If TemporaryError is not due to
     *                   an HTTP error, then it is set to zero.
     * @param httpRequest HttpRequest in which the TemporaryError occurred
     */
    public TemporaryError(final String message, final int statusCode, final HttpRequest httpRequest) {

        this(message, null, statusCode, httpRequest, null);

    }

    /**
     * @param message error description
     * @param cause if TemporaryError is thrown as consequence of another exception. If TemporaryError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public TemporaryError(final String message, final Throwable cause) {

        this(message, cause, 0, null, null);

    }

    /**
     * @param message error description
     * @param cause if TemporaryError is thrown as consequence of another exception. If TemporaryError is not caused by
     *              another exception, then it set to {@code null}.
     * @param api the {@link com.opendxl.streaming.client.Channel} method name in which the TemporaryError occurred.
     */
    public TemporaryError(final String message, final Throwable cause, final String api) {

        this(message, cause, 0, null, api);

    }

    /**
     * @param message error description
     */
    public TemporaryError(final String message) {

        this(message, null, 0, null, null);

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
     * Gets the {@link com.opendxl.streaming.client.Channel} method name in which the TemporaryError occurred.
     *
     * @return Channel API name, e.g.: create, subscribe, consume, commit, run
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

}
