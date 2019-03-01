/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.entity;

/**
 * Represent the Stickiness cookie used by ALB to route and forward request to the right Consumer Service
 */
public class StickinessCookie {
    private final String value;
    private final String domain;

    public StickinessCookie(final String value, final String domain) {

        this.value = value;
        this.domain = domain;
    }

    public String getValue() {
        return value;
    }

    public String getDomain() {
        return domain;
    }
}
