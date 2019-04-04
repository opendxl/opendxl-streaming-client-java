/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.PermanentError;

import java.util.List;

/**
 * ConsumerRecordProcessor is an interface that must be implemented by the process which receives the consumed records.
 * The {@link Channel#run(ConsumerRecordProcessor, List)} )} method invokes the
 * {@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} method to deliver the consumed records to
 * the user for further processing.
 */
public interface ConsumerRecordProcessor {

    /**
     * Method to be implemented by the receiver of the consumed records.
     *
     * @param consumerRecords instance of {@link ConsumerRecords}. It contains the consumed records returned by
     * {@link Channel#consume()} method.
     * @param consumerId consumer identifier
     * @return {@code true} if receiver wants to continue consuming records; {@code false} otherwise
     * @throws ConsumerError if receiver wants to retry consuming records since last committed offset. Receiver should
     *                       raise this exception upon finding errors in consumer records that might be overcome by
     *                       consuming such records again.
     * @throws PermanentError if receiver wants to stop consuming records without even committing the last consumed
     *                        ones. Receiver should raise this exception upon finding unrecoverable errors in consumer
     *                        records.
     */
    boolean processCallback(final ConsumerRecords consumerRecords, final String consumerId)
            throws ConsumerError, PermanentError;

}
