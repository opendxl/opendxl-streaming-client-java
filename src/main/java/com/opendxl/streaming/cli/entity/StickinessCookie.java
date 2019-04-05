/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.entity;

import com.opendxl.streaming.client.Channel;

/**
 * Represent the Stickiness cookie used by ALB to route and forward request to the right Consumer Service
 */
public class StickinessCookie {
    /**
     * Cookie value of the cookie returned in the HTTP Response to a
     * {@link com.opendxl.streaming.cli.operation.CreateOperation}
     */
    private final String value;
    /**
     * Domain value of the cookie returned in the HTTP Response to a
     * {@link com.opendxl.streaming.cli.operation.CreateOperation}
     */
    private final String domain;

    /**
     * Convenient object to store the two main attributes of a Cookie, its value and its domain. The Cookie is obtained
     * when {@link com.opendxl.streaming.cli.operation.CreateOperation} executes {@link Channel#create}. A cookie with
     * exactly these values obtained at consumer creation time must be included later in all subsequent operations
     * including consumer deletion.
     *
     * @param value the cookie value
     * @param domain the cookie domain attribute
     */
    public StickinessCookie(final String value, final String domain) {

        this.value = value;
        this.domain = domain;
    }

    /**
     * Get the cookie value attribute
     *
     * @return cookie value
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the cookie domain attribute
     *
     * @return cookie domain
     */
    public String getDomain() {
        return domain;
    }
}
