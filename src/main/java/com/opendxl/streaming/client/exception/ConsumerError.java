/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

import org.apache.http.HttpRequest;

/**
 * Error raised when a channel operation fails due to the associated consumer
 * not being recognized by the streaming service.
 */
public class ConsumerError extends ClientError {

    private int statusCode;
    private HttpRequest httpRequest;

    /**
     * @param message error description
     * @param cause if ConsumerError is thrown as consequence of another exception. If ConsumerError is not caused by
     *              another exception, then it set to {@code null}.
     * @param statusCode HTTP Status Code that caused the ConsumerError. If ConsumerError is caused by an HTTP Client
     *                   or HTTP Server error, then it is set to the HTTP status code. If ConsumerError is not due to
     *                   an HTTP error, then it is set to zero.
     * @param httpRequest HttpRequest in which the ConsumerError occurred
     */
    public ConsumerError(final String message, final Throwable cause, final int statusCode,
                         final HttpRequest httpRequest) {

        super(message, cause);
        this.statusCode = statusCode;
        this.httpRequest = httpRequest;

    }

    /**
     * @param message error description
     * @param statusCode HTTP Status Code that caused the ConsumerError. If ConsumerError is caused by an HTTP Client
     *                   or HTTP Server error, then it is set to the HTTP status code. If ConsumerError is not due to
     *                   an HTTP error, then it is set to zero.
     * @param httpRequest HttpRequest in which the ConsumerError occurred
     */
    public ConsumerError(final String message, final int statusCode, final HttpRequest httpRequest) {

        this(message, null, statusCode, httpRequest);

    }

    /**
     * @param message error description
     * @param cause if ConsumerError is thrown as consequence of another exception. If ConsumerError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public ConsumerError(final String message, final Throwable cause) {

        this(message, cause, 0, null);

    }

    /**
     * @param message error description
     */
    public ConsumerError(final String message) {

        this(message, null, 0, null);

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
     * Gets the HttpRequest in which the ConsumerError occurred.
     *
     * @return http request which was sent when ConsumerError occurred.
     */
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

}
