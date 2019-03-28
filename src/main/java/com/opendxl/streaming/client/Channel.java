/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.google.gson.JsonSyntaxException;
import com.opendxl.streaming.client.entity.ConsumerRecords;

import com.google.gson.Gson;

import com.opendxl.streaming.client.entity.Topics;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.ErrorType;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>The {@link Channel} class is responsible for all communication with the streaming service.
 * </p>
 * <p>The following example demonstrates the creation of a {@link Channel} instance and creating a consumer for the
 * consumer group:</p>
 *
 * <pre>
 * // Create the channel
 * Channel channel = new Channel("http://channel-server",       // channelUrl
 *                               new ChannelAuth("http://channel-server",   // channelUrlLogin
 *                                               "user",                    // channelUsername
 *                                               "password",                // channelPassword
 *                                               ""),                       // verifyCertificateBundle
 *                               "thegroup",                    // channelConsumerGroup
 *                               null,                          // pathPrefix
 *                               "/databus/consumer-service/v1",  // consumerPathPrefix
 *                               "earliest",                    // offset
 *                               301,                           // requestTimeout
 *                               300,                           // sessionTimeout
 *                               false,                         // retryOnFail
 *                               "",                            // verifyCertificateBundle
 *                               extraConfigs);
 *
 *
 * // Create a new consumer on the consumer group
 * channel.create()
 * </pre>
 */
public class Channel implements AutoCloseable {

    // Constants for consumer config settings
    private static final String _AUTO_OFFSET_RESET_CONFIG_SETTING = "auto.offset.reset";
    private static final String _ENABLE_AUTO_COMMIT_CONFIG_SETTING = "enable.auto.commit";
    private static final String _REQUEST_TIMEOUT_CONFIG_SETTING = "request.timeout.ms";
    private static final String _SESSION_TIMEOUT_CONFIG_SETTING = "session.timeout.ms";

    private static final String _DEFAULT_CONSUMER_PATH_PREFIX = "/databus/consumer-service/v1";

    private final String base;
    private final String consumerPathPrefix;
    private final String consumerGroup;
    /**
     * Allowed values for auto.offset.reset consumer configuration property.
     */
    private final List<String> offsetValues = Arrays.asList("latest", "earliest", "none");
    /**
     * Properties object which contains all consumer configuration properties. Its values are set in
     * {@link Channel#Channel(String, ChannelAuth, String, String, String, String, Integer, Integer, boolean, String,
     * Properties)}
     * constructor and it is later used in {@link Channel#create()} when consumer is created.
     */
    private final Properties configs = new Properties();
    /**
     * Filename of a Certificate Bundle file. Certificates are loaded and managed by {@link HttpConnection} class.
     * Certificates are evaluated when creating SSL connections. If it is set to an empty string, then no certificate
     * nor hostname validation is performed when setting up an SSL Connection.
     */
    private final String verifyCertBundle;

    /**
     * String identifying the consumer instance which is obtained by {@link Channel#create()} API.
     * It is set to a string value in {@link Channel#create()} API.
     * It is reset to {@code null} in {@link Channel#reset()} method which is called when deleting a consumer using
     * {@link Channel#delete()} API
     */
    private String consumerId;
    /**
     * List of subscribed topics. Topic names are set by {@link Channel#subscribe(List)} upon a successful subscribe
     * request. It is emptied by {@link Channel#reset} when deleting a consumer using {@link Channel#delete()} API.
     */
    private List<String> subscriptions;

    /**
     * An implementation of Channel Authorization interface. It adds the http header with the Authorization token to
     * http requests. Two implementations are available: {@link com.opendxl.streaming.client.auth.ChannelAuthToken} and
     * {@link com.opendxl.streaming.client.auth.ChannelAuthUserPass}.
     */
    private final ChannelAuth auth;
    /**
     * Helper class to send HTTP requests. If the received HTTP Response Status code is not successful, then an error
     * with a description message will be thrown.
     */
    private Request request;

    /**
     * Boolean value set by Channel constructor. It indicates whether records consumption must be continued in
     * {@link Channel#run(ConsumerRecordProcessor, List)} when a ConsumerError occurs. If it is set to {@code true},
     * then a new consumer will be created and records consumption will resume. If it set to {@code false}, then
     * execution will exit {@link Channel#run(ConsumerRecordProcessor, List)} method.
     */
    private final boolean retryOnFail;
    /**
     * Boolean value which indicates the Channel instance has been fully constructed. It is set to true by Channel
     * constructor. It is set to false when {@link Channel#destroy()} is called.
     */
    private boolean active;
    /**
     * Flag that is set when Channel object is executing the {@link Channel#run(ConsumerRecordProcessor,List)}.
     * It is set to true when entering {@link Channel#run(ConsumerRecordProcessor, List)}
     * It is set to false on exit of {@link Channel#run(ConsumerRecordProcessor, List)}
     */
    private AtomicBoolean running;
    /**
     * Flag used to indicate that {@link Channel#stop()} has been called to stop a running Channel.
     * It is set to true when entering {@link Channel#stop()}.
     * It is set to false on exit of {@link Channel#stop()} after the channel has been stopped and thus no thread is
     * executing {@link Channel#run(ConsumerRecordProcessor,List)}
     */
    private AtomicBoolean stopRequested;
    /**
     * Flag used to indicate that a stop has been completed.
     * It is set to true when entering {@link Channel#stop()}.
     * It is set to false on exit of {@link Channel#stop()} after the channel has been stopped and thus no thread is
     * executing {@link Channel#run(ConsumerRecordProcessor,List)}
     */
    private AtomicBoolean stopDone;
    /**
     * Flag used to ensure {@link Channel#destroy()} is run by one thread at a time.
     * It is set to true when entering {@link Channel#destroy()} and it is set back to false before leaving the method.
     */
    private AtomicBoolean destroying;
    /**
     * Determines whether an explicit commit request will take place when {@link Channel#commit()} is invoked.
     * If it is set to true, then no commit request will be sent because Kafka will automatically commit records after
     * they have been consumed. If it is set to false, then commit requests will be sent.
     * Its value is set at Channel construction time:
     * it is set to true when extraConfigs parameter of Channel constructor contains the "enable.auto.commit" property
     * and it is set to true.
     * it is set to false when extraConfigs parameter of Channel constructor contains the "enable.auto.commit" property
     * and it is set to false or when extraConfigs does not contain "enable.auto.commit" property.
     */
    private final boolean isAutoCommitEnabled;

    // value used to indicate that no thread is currently accessing Channel methods
    private static final long NO_CURRENT_THREAD = -1L;
    // currentThread holds the threadId of the current thread accessing Channel
    // and is used to prevent multi-threaded access
    private final AtomicLong currentThread = new AtomicLong(NO_CURRENT_THREAD);
    // refcount is used to allow reentrant access by the thread who has acquired currentThread
    private final AtomicInteger refcount = new AtomicInteger(0);

    /**
     * @param base Base URL at which the streaming service resides.
     * @param auth Authentication object to use for channel requests.
     * @param consumerGroup Consumer group to subscribe the channel consumer to.
     * @param pathPrefix Path to append to streaming service requests.
     * @param consumerPathPrefix Path to append to consumer-related requests made to the streaming service. Note that
     *                          if the pathPrefix parameter is set to a non-empty value, the pathPrefix value will be
     *                          appended to consumer-related requests instead of the consumerPathPrefix value.
     * @param offset Offset for the next record to retrieve from the streaming service for the new
     *               {@link Channel#consume()} call. Must be one of 'latest', 'earliest', or 'none'.
     * @param requestTimeout The configuration controls the maximum amount of time the client (consumer) will wait for
     *                      the broker response of a request. If the response is not received before the request timeout
     *                      elapses the client may resend the request or fail the request if retries are exhausted. If
     *                      set to {@code null}, the request timeout is determined automatically by the
     *                      streaming service. Note that if a value is set for the request timeout, the value should
     *                      exceed the sessionTimeout. Otherwise, the streaming service may fail to create new
     *                      consumers properly. To ensure that the request timeout is greater than the sessionTimeout,
     *                      values for either both (or neither) of the requestTimeout and sessionTimeout parameters
     *                      should be specified.
     * @param sessionTimeout The timeout (in seconds) used to detect channel consumer failures. The consumer sends
     *                      periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by
     *                      the broker before the expiration of this session timeout, then the broker may remove this
     *                      consumer from the group. If set to {@code null}, the session timeout is determined
     *                      automatically by the streaming service. Note that if a value is set for the session timeout,
     *                      the value should be less than the requestTimeout. Otherwise, the streaming service may
     *                      fail to create new consumers properly. To ensure that the session timeout is less than the
     *                      requestTimeout, values for either both (or neither) of the requesTimeout and
     *                      sessionTimeout parameters should be specified.
     * @param retryOnFail Whether or not the channel will automatically retry a call which failed due to a temporary
     *                   error.
     * @param verifyCertBundle Path to a CA bundle file containing certificates of trusted CAs. The CA bundle is used
     *                        to validate that the certificate of the authentication server being connected to was
     *                        signed by a valid authority. If set to an empty string, the server certificate is not
     *                        validated.
     * @param extraConfigs Dictionary of key/value pairs containing any custom configuration settings which should be
     *                    sent to the streaming service when a consumer is created. Note that any values specified for
     *                    the offset, requestTimeout, and/or sessionTimeout parameters will override the
     *                    corresponding values, if specified, in the extraConfigs parameter.
     * @throws PermanentError if offset value is not one of 'latest', 'earliest', 'none'.
     * @throws TemporaryError if http client request object failed to be created.
     */
    public Channel(final String base, final ChannelAuth auth, final String consumerGroup,
            final String pathPrefix, final String consumerPathPrefix, final String offset,
            final Integer requestTimeout, final Integer sessionTimeout, final boolean retryOnFail,
            final String verifyCertBundle, final Properties extraConfigs) throws PermanentError,
            TemporaryError {

        this.base = base;
        this.auth = auth;

        if (pathPrefix != null) {
            this.consumerPathPrefix = pathPrefix;
        } else if (consumerPathPrefix != null) {
            this.consumerPathPrefix = consumerPathPrefix;
        } else {
            this.consumerPathPrefix = _DEFAULT_CONSUMER_PATH_PREFIX;
        }

        this.consumerGroup = consumerGroup;
        this.verifyCertBundle = verifyCertBundle;

        if (!this.offsetValues.contains(offset)) {

            throw new PermanentError("Value for 'offset' must be one of: " + offsetValues);
        }

        // Setup customer configs from supplied parameters
        if (extraConfigs != null) {
            this.configs.putAll(extraConfigs);
        }

        if (!this.configs.containsKey(_ENABLE_AUTO_COMMIT_CONFIG_SETTING)) {
            // this has to be false for now
            this.configs.put(_ENABLE_AUTO_COMMIT_CONFIG_SETTING, "false");
        }

        this.isAutoCommitEnabled = Boolean.valueOf(this.configs.get(_ENABLE_AUTO_COMMIT_CONFIG_SETTING).toString());

        this.configs.put(_AUTO_OFFSET_RESET_CONFIG_SETTING, offset);

        if (sessionTimeout != null) {
            // Convert from seconds to milliseconds
            this.configs.put(_SESSION_TIMEOUT_CONFIG_SETTING, sessionTimeout * 1000);
        }

        if (requestTimeout != null) {
            // Convert from seconds to milliseconds
            this.configs.put(_REQUEST_TIMEOUT_CONFIG_SETTING, requestTimeout * 1000);
        }

        // State variables
        this.consumerId = null;
        this.subscriptions = new ArrayList<>();

        // Create a custom Request object so that we can store cookies across requests
        this.request = new Request(base, auth, this.verifyCertBundle);

        this.retryOnFail = retryOnFail;

        this.destroying = new AtomicBoolean(false);
        this.active = true;

        this.running = new AtomicBoolean(false);
        this.stopRequested = new AtomicBoolean(false);
        this.stopDone = new AtomicBoolean(false);

    }

    /**
     * Resets local consumer data stored for the channel.
     */
    private void reset() {

        consumerId = null;
        subscriptions.clear();

        request.resetCookies();
        request.resetAuthorization();

    }

    /**
     * Creates a new consumer on the consumer group
     *
     * @throws PermanentError if no consumer group was specified.
     * @throws TemporaryError if the creation attempt fails.
     */
    public void create() throws PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            if (consumerGroup == null || consumerGroup.isEmpty()) {

                throw new PermanentError("No value specified for 'consumerGroup' during channel init");

            }

            reset();

            // Add consumerGroup value and config properties to request payload
            ConsumerConfig consumerConfig = new ConsumerConfig(consumerGroup, configs);
            Gson gson = new Gson();
            byte[] body = gson.toJson(consumerConfig).getBytes();

            try {

                String responseEntityString = request.post(consumerPathPrefix + "/consumers", body, CREATE_ERROR_MAP);

                if (responseEntityString != null) {
                    ConsumerId consumer = gson.fromJson(responseEntityString, ConsumerId.class);
                    consumerId = consumer.getConsumerInstanceId();
                }

            } catch (final TemporaryError error) {
                error.setApi("create");
                throw error;
            } catch (final JsonSyntaxException | ConsumerError e) {
                TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                        + e.getClass().getCanonicalName() + " " + e.getMessage(), e, "create");
                throw temporaryError;
            }
        } finally {
            release();
        }

    }

    /**
     * Subscribes the consumer to a list of topics
     *
     * @param topics Topic list.
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws PermanentError if no topics were specified.
     * @throws TemporaryError if the subscription attempt fails.
     */
    public void subscribe(final List<String> topics) throws ConsumerError, PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            if (topics == null) {

                throw new PermanentError("Non-empty value must be specified for topics");

            }

            // Remove any null or empty topic from list
            topics.removeAll(Arrays.asList("", null));
            if (topics.isEmpty()) {

                throw new PermanentError("Non-empty value must be specified for topics");

            }

            if (consumerId == null || consumerId.isEmpty()) {
                // Auto-create consumer group if none present
                create();
            }

            Topics topicsToBeSubscribed = new Topics(topics);
            Gson gson = new Gson();
            byte[] body = gson.toJson(topicsToBeSubscribed).getBytes();

            String api = new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId)
                    .append("/subscription").toString();

            try {

                request.post(api, body, SUBSCRIBE_ERROR_MAP);

                subscriptions.clear();
                subscriptions.addAll(topics);

            } catch (final TemporaryError error) {
                error.setApi("subscribe");
                throw error;
            } catch (final ConsumerError error) {
                error.setApi("subscribe");
                throw error;
            }
        } finally {
            release();
        }

    }

    /**
     * List the topic names to which the consumer is subscribed.
     *
     * @return List of topic names
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws TemporaryError if the retrieval of subscriptions fails.
     * @throws PermanentError if request was malformed.
     */
    public List<String> subscriptions() throws ConsumerError, PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            final Gson gson = new Gson();
            final String api =  new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId)
                    .append("/subscription").toString();

            final List<String> list = new ArrayList<>();

            try {

                String responseEntity = request.get(api, GET_SUBSCRIPTIONS_ERROR_MAP);

                if (responseEntity != null) {
                    list.addAll(gson.fromJson(responseEntity, List.class));
                }
                return list;

            } catch (final TemporaryError error) {
                error.setApi("subscriptions");
                throw error;
            } catch (final ConsumerError error) {
                error.setApi("subscriptions");
                throw error;
            } catch (final PermanentError error) {
                error.setApi("subscriptions");
                throw error;
            } catch (final JsonSyntaxException e) {
                TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                        + e.getClass().getCanonicalName() + " " + e.getMessage(), e, "subscriptions");
                throw temporaryError;
            }
        } finally {
            release();
        }

    }

    /**
     * Deletes the consumer from the consumer group
     *
     * @throws TemporaryError if the delete attempt fails.
     * @throws PermanentError if request was malformed.
     */
    public void delete() throws TemporaryError, PermanentError {

        acquireAndEnsureChannelIsActive();
        try {
            if (consumerId == null) {
                return;
            }

            String api = new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId).toString();

            try {

                request.delete(api, DELETE_ERROR_MAP);

            } catch (final ConsumerError consumerError) {

                System.out.println("Consumer with ID " + consumerId + " not found. Resetting consumer anyways.");

            } catch (final TemporaryError error) {
                error.setApi("delete");
                throw error;
            } catch (final PermanentError error) {
                error.setApi("delete");
                throw error;
            } finally {
                // Delete session attribute values, cookies and authorization token
                reset();
            }
        } finally {
            release();
        }

    }

    /**
     * Consumes records from all the subscribed topics
     *
     * @return {@link ConsumerRecords} a list of the consumer record objects from the records returned by the server.
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws PermanentError if the channel has not been subscribed to any topics.
     * @throws TemporaryError if the consume attempt fails.
     */
    public ConsumerRecords consume() throws ConsumerError, PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            if (subscriptions.isEmpty()) {
                throw new PermanentError("Channel is not subscribed to any topic");
            }

            String api = new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId)
                    .append("/records").toString();

            try {

                String responseEntity = request.get(api, CONSUME_RECORDS_ERROR_MAP);

                final Gson gson = new Gson();
                final ConsumerRecords consumerRecords = gson.fromJson(responseEntity, ConsumerRecords.class);

                return consumerRecords;

            } catch (final TemporaryError error) {
                error.setApi("consume");
                throw error;
            } catch (final ConsumerError error) {
                error.setApi("consume");
                throw error;
            } catch (final JsonSyntaxException e) {
                TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                        + e.getClass().getCanonicalName() + " " + e.getMessage(), e, "consume");
                throw temporaryError;
            }
        } finally {
            release();
        }

    }

    /**
     * <p>Commits the record offsets to the channel.</p>
     *
     * <p>Committed offsets are the latest consumed ones on all consumed topics and partitions.</p>
     *
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws TemporaryError if the commit attempt fails.
     * @throws PermanentError if request was malformed.
     */
    public void commit() throws ConsumerError, TemporaryError, PermanentError {

        acquireAndEnsureChannelIsActive();
        try {
            if (isAutoCommitEnabled) {
                return;
            }

            String api = new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId)
                    .append("/offsets").toString();

            try {

                request.post(api, null, COMMIT_ALL_RECORDS_ERROR_MAP);

            } catch (final TemporaryError error) {
                error.setApi("commit");
                throw error;
            } catch (final ConsumerError error) {
                error.setApi("commit");
                throw error;
            } catch (final PermanentError error) {
                error.setApi("commit");
                throw error;
            }
        } finally {
            release();
        }

    }

    /**
     * <p>Repeatedly consume records from the subscribed topics.</p>
     *
     * <p>The supplied
     * {@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} method is invoked with a list containing
     * each consumer record.</p>
     *
     * <p>{@link ConsumerRecordProcessor#processCallback(ConsumerRecords, String)} should return a value of {@code true}
     * in order for this function to continue consuming additional records. For a return value of {@code false}, no
     * additional records will be consumed and this function will return.</p>
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
    public void run(final ConsumerRecordProcessor processCallback, final List<String> topics)
            throws PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            if (consumerGroup == null || consumerGroup.isEmpty()) {
                throw new PermanentError("No value specified for 'consumerGroup' during channel init");
            }

            if (processCallback == null) {
                throw new PermanentError("processCallback not provided");
            }

            List<String> topicsOfInterest = topics != null && !topics.isEmpty() ? topics : subscriptions;

            if (running.compareAndSet(false, true)) {

                while (!stopRequested.get()) {
                    consumeLoop(processCallback, topicsOfInterest);
                }
            }

        } finally {
            running.set(false);
            release();
            // notify stop requester that channel has been stopped
            synchronized (stopDone) {
                stopDone.set(true);
                stopDone.notifyAll();
            }
        }

    }

    public void run(final ConsumerRecordProcessor processCallback, final String topic) throws PermanentError,
            TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            run(processCallback, topic != null && !topic.isEmpty() ? Arrays.asList(topic) : null);
        } finally {
            release();
        }

    }

    /**
     * <p>Stop an active execution of the {@link Channel#run(ConsumerRecordProcessor, List)} call.</p>
     *
     * <p>If no {@link Channel#run(ConsumerRecordProcessor, List)} call is active, this function returns
     * immediately. If a {@link Channel#run(ConsumerRecordProcessor, List)} call is active, this function blocks
     * until the run has been completed.</p>
     *
     * @throws StopError an error occurred while waiting for channel to be stopped
     */
    public void stop() throws StopError {

        if (!running.get()) {
            return;
        }

        if (stopRequested.compareAndSet(false, true)) {
            stopDone.set(false);

            try {
                synchronized (stopDone) {
                    while (running.get() || !stopDone.get()) {
                        stopDone.wait();
                        System.out.println("Channel was stopped");
                    }
                }
            } catch (final Exception e) {
                throw new StopError("Failed to stop channel");
            } finally {
                stopRequested.set(false);
                stopDone.set(false);
            }
        }

    }

    /**
     * <p>Destroys the channel (releases all associated resources).</p>
     *
     * <b>**NOTE:** Once the method has been invoked, no other calls should be made to the channel.</b>
     *
     * <p>Also note that this method should rarely be called directly. Instead, the preferred usage of the channel is
     * via a Java "try-with-resources" statement as shown below:</p>
     *
     * <pre>
     *      // Create the channel
     *      try (Channel channel = new Channel(("http://channel-server",
     *              auth=ChannelAuth("http://channel-server,
     *              "user", "password"),
     *              consumer_group="thegroup")) {
     *
     *                  channel.create();
     *      }
     * </pre>
     *
     * <p>The "try-with-resources" statement ensures that resources associated with the channel are properly cleaned up
     * when the block is exited (the {@link Channel#close()} method is invoked which calls {@link Channel#destroy()}
     * in turn).</p>
     *
     * @throws TemporaryError if a consumer has previously been created for the channel but an attempt to delete the
     *                         consumer from the channel fails.
     * @throws StopError if the attempt to stop the channel fails.
     * @throws PermanentError if delete request was malformed.
     */
    private void destroy() throws TemporaryError, StopError, PermanentError {

        try {
            if (destroying.compareAndSet(false, true)) {
                if (active) {

                    stop();
                    delete();
                    request.close();

                    active = false;
                }
            } else {
                throw new TemporaryError("Channel is not safe for multi-threaded access");
            }
        } finally {
            destroying.set(false);
        }

    }

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
    @Override
    public void close() throws TemporaryError, StopError, PermanentError {

        destroy();

    }

    /**
     * <p>Calls consume to get records, then delivers them to processCallback and finally commit the consumed records.
     * </p>
     *
     * @param processCallback
     * @param topics
     * @throws TemporaryError the consume or commit attempt failed with an error other than ConsumerError.
     * @throws PermanentError the callback asks to stop consuming records.
     */
    private void consumeLoop(final ConsumerRecordProcessor processCallback, List<String> topics)
            throws PermanentError, TemporaryError {

        boolean continueRunning = true;
        boolean subscribed = false;

        while (continueRunning) {

            try {
                // if consumer is not subscribed yet, then subscribe it
                if (!subscribed) {
                    subscribe(topics);
                    subscribed = true;
                }

                // consume records
                ConsumerRecords records = consume();

                // invoke callback
                try {
                    continueRunning = processCallback.processCallback(records, consumerId);
                } catch (final TemporaryError error) {
                    // Callback found errors in records and it wants to retry consuming them from the last commit.
                    // Recovery procedure is the same as when a ConsumerError occurs.
                    throw new ConsumerError("Consume records callback requested to read records again.", error);
                }

                // Commit offsets for just consumed records
                commit();

            } catch (final ConsumerError error) {
                // ConsumerError exception can be raised if the consumer has been removed or if callback found errors
                // in records and it wants them to be consumed again.
                // Then, current consumer is deleted and a brand new one is created to resume consuming from
                // last commit.
                subscribed = false;
                recreateConsumer(topics, error);

                if (!retryOnFail) {
                    continueRunning = false;
                }

            } catch (final PermanentError | TemporaryError error) {
                // Delete consumer instance.
                delete();
                throw error;
            } finally {
                // Check if there is a request to stop consuming records
                if (stopRequested.get()) {
                    // Exit consume loop immediately
                    continueRunning = false;
                }
            }
        }

    }

    /**
     * <p>Deletes the current consumer and then creates a brand new one.</p>
     *
     * <p>This method is used to easily get a new consumer to continue consuming records from the given topics.</p>
     *
     * @param topics topics to which the new consumer will subscribe.
     * @throws PermanentError if consumer group or topics to subscribe to are not available.
     * @throws TemporaryError if the attempt to create a new subscriber and subscribed it to the topics failed.
     */
    private void recreateConsumer(final List<String> topics, final Exception error) throws PermanentError,
            TemporaryError {

        System.out.println("Resetting consumer loop: " + error.getMessage());
        delete();
        request.close();
        request = new Request(base, auth, verifyCertBundle);
        create();

    }

    /**
     * Acquire the light lock and ensure that the Channel is active.
     * @throws IllegalStateException If the Channel is not active
     */
    private void acquireAndEnsureChannelIsActive() throws PermanentError, TemporaryError {
        acquire();
        if (!this.active) {
            release();
            throw new PermanentError("Channel has been destroyed.");
        }
    }

    /**
     * Acquire the light lock protecting this Channel from multi-threaded access. Instead of blocking
     * when the lock is not available, however, we just throw an exception (since multi-threaded usage is not
     * supported).
     * @throws TemporaryError if another thread already has the lock
     */
    private void acquire() throws TemporaryError {
        long threadId = Thread.currentThread().getId();
        if (threadId != currentThread.get() && !currentThread.compareAndSet(NO_CURRENT_THREAD, threadId))
            throw new TemporaryError("Channel is not safe for multi-threaded access");
        refcount.incrementAndGet();
    }

    /**
     * Release the light lock protecting the consumer from multi-threaded access.
     */
    private void release() {
        if (refcount.decrementAndGet() == 0)
            currentThread.set(NO_CURRENT_THREAD);
    }

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#create()} API
     */
    private static final Map<Integer, ErrorType> CREATE_ERROR_MAP = new HashMap() {{
        put(400, ErrorType.PERMANENT_ERROR);
        put(401, ErrorType.TEMPORARY_ERROR);
        put(403, ErrorType.TEMPORARY_ERROR);
        put(404, ErrorType.PERMANENT_ERROR);
        put(500, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#delete()} API
     */
    private static final Map<Integer, ErrorType> DELETE_ERROR_MAP = new HashMap() {{
        put(400, ErrorType.PERMANENT_ERROR);
        put(401, ErrorType.TEMPORARY_ERROR);
        put(403, ErrorType.TEMPORARY_ERROR);
        put(404, ErrorType.CONSUMER_ERROR);
        put(409, ErrorType.TEMPORARY_ERROR);
        put(500, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#subscribe(List)} API
     */
    private static final Map<Integer, ErrorType> SUBSCRIBE_ERROR_MAP = new HashMap() {{
        put(400, ErrorType.PERMANENT_ERROR);
        put(401, ErrorType.TEMPORARY_ERROR);
        put(403, ErrorType.TEMPORARY_ERROR);
        put(404, ErrorType.CONSUMER_ERROR);
        put(409, ErrorType.TEMPORARY_ERROR);
        put(500, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#subscriptions()} API
     */
    private static final Map<Integer, ErrorType> GET_SUBSCRIPTIONS_ERROR_MAP = new  HashMap() {{
        put(400, ErrorType.PERMANENT_ERROR);
        put(401, ErrorType.TEMPORARY_ERROR);
        put(403, ErrorType.TEMPORARY_ERROR);
        put(404, ErrorType.CONSUMER_ERROR);
        put(409, ErrorType.TEMPORARY_ERROR);
        put(500, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#consume()} API
     */
    private static final Map<Integer, ErrorType> CONSUME_RECORDS_ERROR_MAP = new HashMap() {{
        put(400, ErrorType.PERMANENT_ERROR);
        put(401, ErrorType.TEMPORARY_ERROR);
        put(403, ErrorType.TEMPORARY_ERROR);
        put(404, ErrorType.CONSUMER_ERROR);
        put(409, ErrorType.TEMPORARY_ERROR);
        put(500, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#commit()} API
     */
    private static final Map<Integer, ErrorType> COMMIT_ALL_RECORDS_ERROR_MAP = new HashMap() {{
        put(400, ErrorType.PERMANENT_ERROR);
        put(401, ErrorType.TEMPORARY_ERROR);
        put(403, ErrorType.TEMPORARY_ERROR);
        put(404, ErrorType.CONSUMER_ERROR);
        put(409, ErrorType.TEMPORARY_ERROR);
        put(500, ErrorType.TEMPORARY_ERROR);
    }};

}

// Helper classes to serialize / deserialize JSON objects
/**
 * ConsumerId objects are returned by Databus Consumer Service when its createNewConsumerInstance() method is called.
 * It contains the consumer identifier code in string format.
 */
class ConsumerId {

    private String consumerInstanceId;

    ConsumerId(String consumerInstanceId) {
        this.consumerInstanceId = consumerInstanceId;
    }

    public String getConsumerInstanceId() {
        return this.consumerInstanceId;
    }

}

/**
 * ConsumerConfig objects are sent to createNewConsumerInstance() of Databus Consumer Service when
 * {@link Channel#create()} is called. ConsumerConfig objects carry all the required parameters to create a new
 * consumer, e.g.: consumer group, session and request timeouts, whether auto commit is enabled, auto commit timeout.
 */
class ConsumerConfig {

    private String consumerGroup;
    private Properties configs;

    ConsumerConfig(final String consumerGroup, final Properties configs) {
        this.consumerGroup = consumerGroup;
        this.configs = configs;
    }
}
