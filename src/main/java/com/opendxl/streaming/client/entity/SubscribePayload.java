/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.entity;

import java.util.List;
import java.util.Map;

public class SubscribePayload {
    private List<String> topics;

    private Map<String, Object> filter;

    /**
     * @param topics list of topic names
     */
    public SubscribePayload(final List<String> topics, final Map<String, Object> filter) {
        this.topics = topics;
        this.filter = filter;
    }
}
