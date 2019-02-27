/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.entity.ConsumerRecords;

/**
 * ConsumerRecordProcessor is an interface that must be implemented by the process which receives the consumed records
 */
public interface ConsumerRecordProcessor {

    boolean processCallback(final ConsumerRecords consumerRecords, final String consumerId);

}
