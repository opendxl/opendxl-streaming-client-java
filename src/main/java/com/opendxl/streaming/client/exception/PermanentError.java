/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

import org.apache.http.HttpRequest;

/**
 * Exception raised for an operation which would not be expected to succeed even if the operation were retried.
 */
public class PermanentError extends ClientError {

    private int statusCode;
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

    /**
     * @param message error description
     * @param statusCode HTTP Status Code that caused the PermanentError. If PermanentError is caused by an HTTP Client
     *                   or HTTP Server error, then it is set to the HTTP status code. If PermanentError is not due to
     *                   an HTTP error, then it is set to zero.
     * @param httpRequest HttpRequest in which the PermanentError occurred
     */
    public PermanentError(final String message, final int statusCode, final HttpRequest httpRequest) {

        this(message, null, statusCode, httpRequest);

    }

    /**
     * @param message error description
     * @param cause if PermanentError is thrown as consequence of another exception. If PermanentError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public PermanentError(final String message, final Throwable cause) {

        this(message, cause, 0, null);

    }

    /**
     * @param message error description
     */
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
     * Gets the HttpRequest in which the PermanentError occurred.
     *
     * @return http request which was sent when PermanentError occurred.
     */
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

}
