/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.google.gson.JsonSyntaxException;
import com.opendxl.streaming.client.entity.ConsumerRecords;

import com.google.gson.Gson;

import com.google.gson.annotations.SerializedName;
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
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
 *                               Optional.empty(),              // pathPrefix
 *                               Optional.of("/databus/consumer-service/v1"),  // consumerPathPrefix
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

    // Default number of seconds to wait between consume queries made to the streaming service
    private static final int _DEFAULT_WAIT_BETWEEN_QUERIES = 30;

    private final String base;
    private final String consumerPathPrefix;
    private final Optional<String> consumerGroup;
    private final List<String> offsetValues = Arrays.asList("latest", "earliest", "none");
    private final Properties configs = new Properties();

    private String consumerId;
    private List<String> subscriptions;

    private final ChannelAuth auth;
    private Request request;

    private final boolean retryOnFail;
    private boolean active;
    private boolean running;
    private boolean continueRunning;
    private boolean stopRequested;
    private final boolean isAutoCommitEnabled;

    private ReentrantLock runLock;
    private ReentrantLock destroyLock;
    private Condition stopRequestedCondition;
    private Condition stoppedCondition;

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
            final Optional<String> pathPrefix, final Optional<String> consumerPathPrefix, final String offset,
            final Integer requestTimeout, final Integer sessionTimeout, final boolean retryOnFail,
            final String verifyCertBundle, final Optional<Properties> extraConfigs) throws PermanentError,
            TemporaryError {

        this.base = base;
        this.auth = auth;
        this.consumerPathPrefix = pathPrefix.isPresent() ? pathPrefix.get()
                : consumerPathPrefix.orElse(_DEFAULT_CONSUMER_PATH_PREFIX);

        this.consumerGroup = Optional.of(consumerGroup);

        if (!this.offsetValues.contains(offset)) {

            throw new PermanentError("Value for 'offset' must be one of: " + offsetValues);
        }

        // Setup customer configs from supplied parameters
        extraConfigs.ifPresent(values -> this.configs.putAll(values));

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
        this.request = new Request(base, auth);

        this.retryOnFail = retryOnFail;

        this.destroyLock = new ReentrantLock();
        this.active = true;

        this.runLock = new ReentrantLock();
        this.running = false;
        this.stopRequested = false;
        this.stopRequestedCondition = this.runLock.newCondition();
        this.stoppedCondition = this.runLock.newCondition();

    }

    /**
     * Resets local consumer data stored for the channel.
     */
    void reset() {

        consumerId = null;
        subscriptions.clear();

        request.resetCookies();

    }

    /**
     * Creates a new consumer on the consumer group
     *
     * @throws PermanentError if no consumer group was specified.
     * @throws TemporaryError if the creation attempt fails.
     */
    public void create() throws PermanentError, TemporaryError {

        if (!consumerGroup.isPresent()) {

            throw new PermanentError("No value specified for 'consumerGroup' during channel init");

        }

        reset();

        // Add consumerGroup value and config properties to request payload
        ConsumerConfig consumerConfig = new ConsumerConfig(consumerGroup.get(), configs);
        Gson gson = new Gson();
        byte[] body = gson.toJson(consumerConfig).getBytes();

        try {

            Optional<String> responseEntityString = request.post(consumerPathPrefix + "/consumers", Optional.of(body),
                    CREATE_ERROR_MAP);

            responseEntityString.ifPresent(entity -> {
                ConsumerId consumer = (ConsumerId) gson.fromJson(entity, ConsumerId.class);
                consumerId = consumer.getConsumerInstanceId();
            });

        } catch (final TemporaryError error) {
            error.setApi("create");
            throw error;
        } catch (final JsonSyntaxException | ConsumerError e) {
            TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                    + e.getClass().getCanonicalName() + " " + e.getMessage(), e, 0, null);
            temporaryError.setApi("create");
            throw temporaryError;
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

            request.post(api, Optional.of(body), SUBSCRIBE_ERROR_MAP);

            subscriptions.clear();
            subscriptions.addAll(topics);

        } catch (final TemporaryError error) {
            error.setApi("subscribe");
            throw error;
        } catch (final ConsumerError error) {
            error.setApi("subscribe");
            throw error;
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

        final Gson gson = new Gson();
        final String api =  new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/subscription").toString();

        final List<String> list = new ArrayList<>();

        try {

            Optional<String> responseEntity = request.get(api, GET_SUBSCRIPTIONS_ERROR_MAP);

            responseEntity.ifPresent(value -> list.addAll(gson.fromJson(value, List.class)));
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
                    + e.getClass().getCanonicalName() + " " + e.getMessage(), e, 0, null);
            temporaryError.setApi("subscriptions");
            throw temporaryError;
        }

    }

    /**
     * Unsubscribe the consumer from all topics
     *
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws TemporaryError if the unsubscribe attempt fails.
     * @throws PermanentError if request was malformed.
     */
    public void unsubscribe() throws ConsumerError, PermanentError, TemporaryError {

        final String api =  new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/subscription").toString();

        try {

            request.delete(api, UNSUBSCRIBE_ERROR_MAP);
            subscriptions.clear();
            return;

        }  catch (final TemporaryError error) {
            error.setApi("unsubscribe");
            throw error;
        } catch (final ConsumerError error) {
            error.setApi("unsubscribe");
            throw error;
        } catch (final PermanentError error) {
            error.setApi("unsubscribe");
            throw error;
        }

    }

    /**
     * Deletes the consumer from the consumer group
     *
     * @throws TemporaryError if the delete attempt fails.
     * @throws PermanentError if request was malformed.
     */
    public void delete() throws TemporaryError, PermanentError {

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
            request.resetAuthorization();
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

        if (subscriptions.isEmpty()) {
            throw new PermanentError("Channel is not subscribed to any topic");
        }

        String api = new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/records").toString();

        try {

            Optional<String> responseEntity = request.get(api, CONSUME_RECORDS_ERROR_MAP);

            final Gson gson = new Gson();
            final ConsumerRecords consumerRecords = gson.fromJson(responseEntity.get(), ConsumerRecords.class);

            return consumerRecords;

        } catch (final TemporaryError error) {
            error.setApi("consume");
            throw error;
        } catch (final ConsumerError error) {
            error.setApi("consume");
            throw error;
        } catch (final JsonSyntaxException e) {
            TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                    + e.getClass().getCanonicalName() + " " + e.getMessage(), e, 0, null);
            temporaryError.setApi("consume");
            throw temporaryError;
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

        if (isAutoCommitEnabled) {
            return;
        }

        String api = new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/offsets").toString();

        try {

            request.post(api, Optional.empty(), COMMIT_ALL_RECORDS_ERROR_MAP);

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
     * @param waitBetweenQueries Number of seconds to wait between calls to consume records.
     * @param topics If set to a non-empty value, the channel will be subscribed to the specified topics.
     *              If set to an empty value, the channel will use topics previously subscribed via a call to the
     *              subscribe method.
     * @throws PermanentError if a prior run is already in progress or no consumer group value was specified or
     *                         callback to deliver records was not specified
     * @throws TemporaryError consume or commit attempts failed with errors other than ConsumerError.
     */
    public void run(final Optional<ConsumerRecordProcessor> processCallback, final int waitBetweenQueries,
                    final Optional<List<String>> topics) throws PermanentError, TemporaryError {

        if (!consumerGroup.isPresent()) {
            throw new PermanentError("No value specified for 'consumerGroup' during channel init");
        }

        if (!processCallback.isPresent()) {
            throw new PermanentError("processCallback not provided");
        }

        int waitPeriod = waitBetweenQueries > 0 ? waitBetweenQueries : _DEFAULT_WAIT_BETWEEN_QUERIES;

        List<String> topicsOfInterest = topics.isPresent() && !topics.get().isEmpty() ? topics.get() : subscriptions;

        continueRunning = true;
        runLock.lock();
        if (running) {
            runLock.unlock();
            throw new PermanentError("Previous run already in progress");
        }

        running = true;
        continueRunning = !stopRequested;
        runLock.unlock();

        try {

            while (continueRunning) {
                continueRunning = consumeLoop(processCallback.get(), waitPeriod, topicsOfInterest);
            }

        } finally {
            runLock.lock();
            running = false;
            stopRequested = false;
            stoppedCondition.signalAll();
            runLock.unlock();
        }

    }

    public void run(final Optional<ConsumerRecordProcessor> processCallback, final int waitBetweenQueries,
                    final String topic) throws PermanentError, TemporaryError {

        run(processCallback, waitBetweenQueries,
                topic != null && !topic.isEmpty() ? Optional.of(Arrays.asList(topic)) : Optional.empty());

    }

    /**
     * <p>Stop an active execution of the {@link Channel#run(Optional, int, Optional)} call.</p>
     *
     * <p>If no {@link Channel#run(Optional, int, Optional)} call is active, this function returns
     * immediately. If a {@link Channel#run(Optional, int, Optional)} call is active, this function blocks until
     * the run has been completed.</p>
     *
     * @throws StopError an error occurred while waiting for channel to be stopped
     */
    public void stop() throws StopError {

        runLock.lock();

        if (running) {

            stopRequested = true;
            stopRequestedCondition.signalAll();

            while (running) {
                try {

                    stoppedCondition.await();

                } catch (final Exception e) {
                    throw new StopError("Failed to stop channel");
                }
            }
        }

        runLock.unlock();

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
    public void destroy() throws TemporaryError, StopError, PermanentError {

        destroyLock.lock();

        if (active) {

            stop();
            delete();
            request.close();

            active = false;
        }

        destroyLock.unlock();

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
     * @param waitBetweenQueries
     * @param topics
     * @return <code>true</code> if callback has successfully processed records and stop has not been requested
     *         <code>false</code> otherwise.
     * @throws TemporaryError the consume or commit attempt failed with an error other than ConsumerError.
     * @throws PermanentError the callback asks to stop consuming records.
     */
    private boolean consumeLoop(final ConsumerRecordProcessor processCallback, final long waitBetweenQueries,
                                List<String> topics) throws PermanentError, TemporaryError {

        boolean continueRunning = true;
        boolean subscribed = false;

        while (continueRunning) {

            // if consumer is not subscribed yet, then subscribe it
            try {
                if (!subscribed) {
                    subscribe(topics);
                    subscribed = true;
                }
            } catch (final ConsumerError error) {
                // ConsumerError exception can be raised if the consumer has been removed.
                // Then, current consumer is deleted and a brand new one is created to resume consuming from
                // last commit.
                subscribed = false;
                recreateConsumer(topics, error);
                continueRunning = !retryOnFail ? false : continueRunning;
                continue;
            } catch (final PermanentError | TemporaryError error) {
                // Delete consumer instance.
                delete();
                throw error;
            } finally {
                runLock.lock();
                continueRunning = stopRequested ? false : continueRunning;
                runLock.unlock();
            }

            // consume records
            ConsumerRecords records = null;
            try {
                records = consume();
            } catch (final ConsumerError error) {
                subscribed = false;
                recreateConsumer(topics, error);
                continueRunning = !retryOnFail ? false : continueRunning;
                continue;
            } catch (final PermanentError | TemporaryError error) {
                // Delete consumer instance.
                delete();
                throw error;
            } finally {
                runLock.lock();
                continueRunning = stopRequested ? false : continueRunning;
                runLock.unlock();
            }

            // invoke callback
            try {
                continueRunning = processCallback.processCallback(records, consumerId);
            } catch (final TemporaryError error) {
                subscribed = false;
                recreateConsumer(topics, error);
                continueRunning = !retryOnFail ? false : continueRunning;
                continue;
            } catch (final PermanentError error) {
                // Callback found errors in records and it does not want to retry consuming them.
                // Delete consumer instance.
                delete();
                error.setApi("run");
                throw error;
            } finally {
                runLock.lock();
                continueRunning = stopRequested ? false : continueRunning;
                runLock.unlock();
            }

            // Commit offsets for just consumed records
            try {
                commit();
            } catch (final ConsumerError error) {
                subscribed = false;
                recreateConsumer(topics, error);
                continueRunning = !retryOnFail ? false : continueRunning;
                continue;
            } catch (final TemporaryError error) {
                // Delete consumer instance.
                delete();
                throw error;
            } finally {
                runLock.lock();
                continueRunning = stopRequested ? false : continueRunning;
                runLock.unlock();
            }

            // Wait for a while
            try {
                runLock.lock();
                if (continueRunning) {
                    stopRequestedCondition.await(waitBetweenQueries, TimeUnit.SECONDS);
                    continueRunning = !stopRequested;
                }
            } catch (final InterruptedException e) {
                // Ignore it
            } finally {
                runLock.unlock();
            }
        }

        return continueRunning;

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
        request = new Request(base, auth);
        create();

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
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#unsubscribe()} API
     */
    private static final Map<Integer, ErrorType> UNSUBSCRIBE_ERROR_MAP = new HashMap() {{
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
class ConsumerId {

    private String consumerInstanceId;

    ConsumerId(String consumerInstanceId) {
        this.consumerInstanceId = consumerInstanceId;
    }

    public String getConsumerInstanceId() {
        return this.consumerInstanceId;
    }

}

class ConsumerConfig {

    private String consumerGroup;
    private Properties configs;

    ConsumerConfig(final String consumerGroup, final Properties configs) {
        this.consumerGroup = consumerGroup;
        this.configs = configs;
    }
}

class CommitLog {

    private String topic;
    private int partition;
    private long offset;

    CommitLog(final String topic, final int partition, final long offset) {

        this.topic = topic;
        this.partition = partition;
        this.offset = offset;

    }

}

class AuthorizationToken {

    @SerializedName(value = "authorizationToken", alternate = {"AuthorizationToken"})
    private String authorizationToken;

    AuthorizationToken(final String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(final String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

}
