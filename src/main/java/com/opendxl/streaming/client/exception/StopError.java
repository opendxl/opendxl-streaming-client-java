/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

/**
 * Exception raised for an operation which is interrupted due to the channel being stopped.
 */
public class StopError extends Exception {

    private String api;

    /**
     * @param message error description
     * @param cause if StopError is thrown as consequence of another exception. If StopError is not caused by another
     *              exception, then it is set to {@code null}.
     */
    public StopError(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message error description
     */
    public StopError(final String message) {
        this(message, null);
    }

    /**
     * Gets the {@link com.opendxl.streaming.client.Channel} method name in which the StopError occurred.
     *
     * @return Channel API name, e.g.: create, subscribe, consume, commit, run
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
