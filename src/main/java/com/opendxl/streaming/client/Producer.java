/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.builder.ProducerBuilder;
import com.opendxl.streaming.client.entity.ProducerRecords;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

/**
 * <p>The {@link Producer} interface only exposes the {@link Channel} methods to produce records. It does not expose
 * methods to consume records. Objects complying with {@link Producer} interface are instantiated using
 * {@link ProducerBuilder} which implements a build pattern for {@link Producer}.</p>
 *
 * <p>{@link Producer} is easier to use than {@link Channel} when you only need to produce records and not to consume
 * them. Benefits are:</p>
 * - a simplified instance creation because it is not necessary to provide parameters required by {@link Channel}
 *   consume operations (e.g.: consumerGroup, consumerPathPrefix, extraConfigs)
 * - a slim set of operations to produce only. Consume operations are not exposed, then it is not possible to call them
 *   by accident.
 */
public interface Producer extends AutoCloseable {

    /**
     * <p>Produce records to the channel.</p>
     *
     * @param producerRecords a {@link ProducerRecords} object containing the records to be posted to the channel.
     * @throws PermanentError if produce request was malformed or produce RESTful service was not found.
     * @throws TemporaryError if produce request was temporarily not authorized or there was an internal RESTful error
     *                        while serving the request.
     */
    void produce(final ProducerRecords producerRecords) throws PermanentError, TemporaryError;

    /**
     * <p>Produce records to the channel.</p>
     *
     * @param jsonProducerRecords a {@link String} object containing the records to be posted to the channel in
     *                            JSON string format.
     * @throws PermanentError if produce request was malformed or produce RESTful service was not found.
     * @throws TemporaryError if produce request was temporarily not authorized or there was an internal RESTful error
     *                        while serving the request.
     */
    void produce(final String jsonProducerRecords) throws PermanentError, TemporaryError;

    /**
     * <p>Closes the Producer channel.</p>
     *
     * <p>It calls {@link Channel#destroy()} to stop the channel and to release its resources.</p>
     *
     * <p>This method is added to allow Channel to be used in conjunction with Java try-with-resources statement.</p>
     *
     * @throws StopError if the attempt to stop the channel fails.
     */
    void close() throws TemporaryError, StopError, PermanentError;

}
