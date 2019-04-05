/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

public abstract class ClientError extends Exception {

    /**
     * Channel API method where the error has occurred.
     */
    private String api;

    /**
     * @param message error description
     * @param cause if ClientError is thrown as consequence of another exception. If ClientError is not caused by
     *              another exception, then it set to {@code null}.
     */
    public ClientError(final String message, final Throwable cause) {

        super(message, cause);

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

}
