/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.entity;

import java.util.List;

/**
 * Helper class used to serialize and deserialize a list of topic names
 */
public class Topics {
    private List<String> topics;

    /**
     * @param topics list of topic names
     */
    public Topics(List<String> topics) {
        this.topics = topics;
    }
}
