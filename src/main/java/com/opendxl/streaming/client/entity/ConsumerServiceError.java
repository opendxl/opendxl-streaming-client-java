/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.entity;

/**
 * ConsumerServiceError is a helper class used to parse the JSON object returned by Consumer Service when an error
 * has occurred.
 */
public class ConsumerServiceError {
    private String message;

    public ConsumerServiceError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
