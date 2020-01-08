/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.builder.ConsumerBuilder;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import java.util.List;

/**
 * <p>The {@link Consumer} interface only exposes the {@link Channel} methods to consume records. It does not expose
 * methods to produce records. Objects complying with {@link Consumer} interface are instantiated using
 * {@link ConsumerBuilder} which implements a build pattern for {@link Consumer}.</p>
 *
 * <p>{@link Consumer} is easier to use than {@link Channel} when you only need to consume records and not to produce
 * them. Benefits are:</p>
 * - a simplified instance creation because it is not necessary to provide parameters required by {@link Channel}
 *   produce operations (e.g.: producerPathPrefix)
 * - a slim set of operations to consume only. Produce operations are not exposed, then it is not possible to call them
 *   by accident.
 */
public interface Consumer extends AutoCloseable {

    /**
     * <p>Creates a new consumer on the consumer group specified by
     * {@link ConsumerBuilder#ConsumerBuilder(String, ChannelAuth, String)}} at construction time.</p>
     *
     * @throws PermanentError if no consumer group was specified.
     * @throws TemporaryError if the creation attempt fails.
     */
    void create() throws PermanentError, TemporaryError;

    /**
     * <p>Subscribes the consumer to a list of topics</p>
     *
     * @param topics Topic list.
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws PermanentError if no topics were specified.
     * @throws TemporaryError if the subscription attempt fails.
     */
    void subscribe(List<String> topics) throws ConsumerError, PermanentError, TemporaryError;

    /**
     * <p>List the topic names to which the consumer is subscribed</p>
     *
     * @return List of topic names
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws TemporaryError if the retrieval of subscriptions fails.
     * @throws PermanentError if request was malformed.
     */
    List<String> subscriptions() throws ConsumerError, PermanentError, TemporaryError;

    /**
     * <p>Deletes the consumer from the consumer group</p>
     *
     * @throws TemporaryError if the delete attempt fails.
     * @throws PermanentError if request was malformed.
     */
    void delete() throws TemporaryError, PermanentError;

    /**
     * <p>Consumes records from all the subscribed topics</p>
     *
     * @return {@link ConsumerRecords} a list of the consumer record objects from the records returned by the server.
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws PermanentError if the channel has not been subscribed to any topics.
     * @throws TemporaryError if the consume attempt fails.
     */
    ConsumerRecords consume() throws ConsumerError, PermanentError, TemporaryError;

    /**
     * <p>Consumes records from all the subscribed topics</p>
     *
     * @param timeout Timeout in milliseconds to wait for records before returning
     * @return {@link ConsumerRecords} a list of the consumer record objects from the records returned by the server.
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws PermanentError if the channel has not been subscribed to any topics.
     * @throws TemporaryError if the consume attempt fails.
     */
    ConsumerRecords consume(int timeout) throws ConsumerError, PermanentError, TemporaryError;

    /**
     * <p>Commits the record offsets to the channel.</p>
     *
     * <p>Committed offsets are the latest consumed ones on all consumed topics and partitions.</p>
     *
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws TemporaryError if the commit attempt fails.
     * @throws PermanentError if request was malformed.
     */
    void commit() throws ConsumerError, TemporaryError, PermanentError;

    /**
     * <p>Repeatedly consume records from the subscribed topics.</p>
     *
     * <p>The supplied
     * {@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} method is invoked with a list containing
     * each consumer record.</p>
     *
     * <p>{@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} return value is <b>currently
     * ignored</b>. It is <b>reserved for future use</b>.</p>
     *
     * <p>The {@link Channel#stop()} method can also be called to halt an execution of this method.</p>
     *
     * @param processCallback Callable which is invoked with a list of records which have been consumed.
     * @param topics If set to a non-empty value, the channel will be subscribed to the specified topics.
     *              If set to an empty value, the channel will use topics previously subscribed via a call to the
     *              subscribe method.
     * @throws PermanentError if a prior run is already in progress or no consumer group value was specified or
     *                         callback to deliver records was not specified
     * @throws TemporaryError consume or commit attempts failed with errors other than ConsumerError.
     */
    void run(ConsumerRecordProcessor processCallback, List<String> topics)
            throws PermanentError, TemporaryError;

    /**
     * <p>Repeatedly consume records from the subscribed topics.</p>
     *
     * <p>The supplied
     * {@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} method is invoked with a list containing
     * each consumer record.</p>
     *
     * <p>{@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} return value is <b>currently
     * ignored</b>. It is <b>reserved for future use</b>.</p>
     *
     * <p>The {@link Channel#stop()} method can also be called to halt an execution of this method.</p>
     *
     * @param processCallback Callable which is invoked with a list of records which have been consumed.
     * @param topics If set to a non-empty value, the channel will be subscribed to the specified topics.
     *              If set to an empty value, the channel will use topics previously subscribed via a call to the
     *              subscribe method.
     * @param timeout Timeout in milliseconds to wait for records before returning
     * @throws PermanentError if a prior run is already in progress or no consumer group value was specified or
     *                         callback to deliver records was not specified
     * @throws TemporaryError consume or commit attempts failed with errors other than ConsumerError.
     */
    void run(ConsumerRecordProcessor processCallback, List<String> topics, int timeout)
            throws PermanentError, TemporaryError;

    /**
     * <p>Repeatedly consume records from the subscribed topic.</p>
     *
     * <p>The supplied
     * {@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} method is invoked with a list containing
     * each consumer record.</p>
     *
     * <p>{@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} return value is <b>currently
     * ignored</b>. It is <b>reserved for future use</b>.</p>
     *
     * <p>The {@link Channel#stop()} method can also be called to halt an execution of this method.</p>
     *
     * @param processCallback Callable which is invoked with a list of records which have been consumed.
     * @param topic If set to a non-empty value, the channel will be subscribed to the specified topic.
     *              If set to an empty value, the channel will use topics previously subscribed via a call to the
     *              subscribe method.
     * @throws PermanentError if a prior run is already in progress or no consumer group value was specified or
     *                         callback to deliver records was not specified
     * @throws TemporaryError consume or commit attempts failed with errors other than ConsumerError.
     */
    void run(ConsumerRecordProcessor processCallback, String topic)
            throws PermanentError, TemporaryError;

    /**
     * <p>Repeatedly consume records from the subscribed topic.</p>
     *
     * <p>The supplied
     * {@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} method is invoked with a list containing
     * each consumer record.</p>
     *
     * <p>{@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} return value is <b>currently
     * ignored</b>. It is <b>reserved for future use</b>.</p>
     *
     * <p>The {@link Channel#stop()} method can also be called to halt an execution of this method.</p>
     *
     * @param processCallback Callable which is invoked with a list of records which have been consumed.
     * @param topic If set to a non-empty value, the channel will be subscribed to the specified topic.
     *              If set to an empty value, the channel will use topics previously subscribed via a call to the
     *              subscribe method.
     * @param timeout Timeout in milliseconds to wait for records before returning
     * @throws PermanentError if a prior run is already in progress or no consumer group value was specified or
     *                         callback to deliver records was not specified
     * @throws TemporaryError consume or commit attempts failed with errors other than ConsumerError.
     */
    void run(ConsumerRecordProcessor processCallback, String topic, int timeout)
            throws PermanentError, TemporaryError;

    /**
     * <p>Stop an active execution of the {@link Channel#run(ConsumerRecordProcessor, List)} call.</p>
     *
     * <p>If no {@link Channel#run(ConsumerRecordProcessor, List)} call is active, this function returns
     * immediately. If a {@link Channel#run(ConsumerRecordProcessor, List)} call is active, this function blocks
     * until the run has been completed.</p>
     *
     * @throws StopError an error occurred while waiting for channel to be stopped
     */
    void stop() throws StopError;

    /**
     * <p>Closes the channel.</p>
     *
     * <p>It calls {@link Channel#destroy()} to stop the channel and to release its resources.</p>
     *
     * <p>This method is added to allow Channel to be used in conjunction with Java try-with-resources statement.</p>
     *
     * @throws TemporaryError if a consumer has previously been created for the channel but an attempt to delete the
     *                         consumer from the channel fails.
     * @throws StopError if the attempt to stop the channel fails.
     */
    void close() throws TemporaryError, StopError, PermanentError;

}
