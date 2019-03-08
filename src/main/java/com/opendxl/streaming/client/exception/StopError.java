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

    /**
     * @param message error description
     */
    public StopError(final String message) {
        super(message);
    }

    /**
     * Gets the exception which caused the StopError.
     *
     * @return original exception
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Sets the exception which caused the StopError to be thrown.
     *
     * @param cause if StopError is thrown as consequence of another exception. If StopError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public void setCause(final Throwable cause) {
        this.cause = cause;
    }

    /**
     * Gets the {@link com.opendxl.streaming.client.Channel} method name in which the StopError occurred.
     *
     * @return Channel API name
     */
    public String getApi() {
        return api;
    }

    /**
     * Sets the {@link com.opendxl.streaming.client.Channel} method name in which the StopError occurred.
     *
     * @param api the Channel method name, e.g.: create, subscribe, consume, commit, run
     */
    public void setApi(final String api) {
        this.api = api;
    }

}
