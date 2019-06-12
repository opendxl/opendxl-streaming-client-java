/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.entity.ConsumerRecords;

import com.opendxl.streaming.client.entity.Topics;
import com.opendxl.streaming.client.exception.ClientError;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.ErrorType;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;

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
 * // Setup consumer properties if non-default configuration values are necessary, e.g.:
 * Properties extraConfigs = new Properties();
 * extraConfigs.put("enable.auto.commit", false);
 * extraConfigs.put("auto.commit.interval.ms", 0);
 * extraConfigs.put("auto.offset.reset", "earliest");
 * extraConfigs.put("request.timeout.ms", 16000);
 * extraConfigs.put("session.timeout.ms", 15000);
 *
 * // Setup Http Proxy if required, e.g.:
 * HttpProxySettings httpProxySettings = new HttpProxySettings(true, "my-proxy.my-domain.net", 8080, "", "");
 *
 * // Create the channel
 * Channel channel = new Channel("http://channel-server",       // channelUrl
 *                               new ChannelAuthUserPass(
 *                                               "http://channel-server",   // channelUrlLogin
 *                                               "user",                    // channelUsername
 *                                               "password",                // channelPassword
 *                                               null,                      // pathPrefix
 *                                               "",                        // verifyCertificateBundle
 *                                               httpProxySettings),        // http proxy settings
 *                               "thegroup",                    // channelConsumerGroup
 *                               null,                          // pathPrefix
 *                               "/databus/consumer-service/v1",  // consumerPathPrefix
 *                               false,                         // retryOnFail
 *                               "",                            // verifyCertificateBundle
 *                               extraConfigs,                  // consumer additional properties
 *                               httpProxySettings);            // http proxy settings
 *
 *
 * // Create a new consumer on the consumer group
 * channel.create()
 * </pre>
 */
public class Channel implements AutoCloseable {

    // Constants for consumer config settings
    private static final String ENABLE_AUTO_COMMIT_CONFIG_SETTING = "enable.auto.commit";
    private static final String MULTI_TENANT_CONFIG_SETTING = "enable.multitenant";

    /**
     * Default URL path for Consumer Service
     */
    private static final String DEFAULT_CONSUMER_PATH_PREFIX = "/databus/consumer-service/v1";

    /**
     * Time to wait between two consecutive checks while awaiting a running Channel to be stopped.
     */
    private static final int STOP_CHANNEL_WAIT_PERIOD_MS = 1000;

    /**
     * Base URL at which the streaming service resides.
     */
    private final String base;

    /**
     * Path to append to base for consumer-related requests made to the streaming service.
     */
    private final String consumerPathPrefix;

    /**
     * Consumer group to subscribe the channel consumer to.
     */
    private final String consumerGroup;

    /**
     * Properties object which contains all consumer configuration properties. Its values are set in
     * {@link Channel#Channel(String, ChannelAuth, String, String, String, boolean, String, Properties,
     * HttpProxySettings)}
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
     * Http headers
     */
    private final Map<String, String> httpHeaders;

    /**
     * String that define the value of multi_tenant query string for subscription API
     */
    private String multiTenantQueryString;

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
    private final AtomicBoolean running;

    /**
     * Flag used to indicate that {@link Channel#stop()} has been called to stop a running Channel.
     * It is set to true when entering {@link Channel#stop()}.
     * It is set to false on exit of {@link Channel#stop()} after the channel has been stopped and thus no thread is
     * executing {@link Channel#run(ConsumerRecordProcessor,List)}
     */
    private final AtomicBoolean stopRequested;

    /**
     * Flag used to ensure {@link Channel#destroy()} is run by one thread at a time.
     * It is set to true when entering {@link Channel#destroy()} and it is set back to false before leaving the method.
     */
    private final AtomicBoolean destroying;

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
     * Flag that is set to true if the connection requires SSL, false otherwise.
     * It is set to true if URL to connect to begins with "https"
     */
    private final boolean isHttps;
    /**
     * POJO containing http proxy hostname, port, username and password
     */
    private final HttpProxySettings httpProxySettings;

    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(Channel.class);

    /**
     * @param base Base URL at which the streaming service resides.
     * @param auth Authentication object to use for channel requests.
     * @param consumerGroup Consumer group to subscribe the channel consumer to.
     * @param pathPrefix Path to append to streaming service requests.
     * @param consumerPathPrefix Path to append to consumer-related requests made to the streaming service. Note that
     *                          if the pathPrefix parameter is set to a non-empty value, the pathPrefix value will be
     *                          appended to consumer-related requests instead of the consumerPathPrefix value.
     * @param retryOnFail Whether or not the channel will automatically retry a call which failed due to a temporary
     *                   error.
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @param extraConfigs Dictionary of key/value pairs containing any custom configuration settings which should be
     *                     sent to the streaming service when a consumer is created. Examples of key/value pairs are:
     *                     ("auto.offset.reset", "latest"); ("request.timeout.ms", 30000) and
     *                     ("session.timeout.ms", 10000).
     * @param httpProxySettings contains http proxy hostname, port, username and password.
     * @throws TemporaryError if http client request object failed to be created.
     */
    public Channel(final String base, final ChannelAuth auth, final String consumerGroup, final String pathPrefix,
                   final String consumerPathPrefix, final boolean retryOnFail, final String verifyCertBundle,
                   final Properties extraConfigs, final HttpProxySettings httpProxySettings)
            throws TemporaryError {

        this.base = base;
        this.auth = auth;

        if (pathPrefix != null) {
            this.consumerPathPrefix = pathPrefix;
        } else if (consumerPathPrefix != null) {
            this.consumerPathPrefix = consumerPathPrefix;
        } else {
            this.consumerPathPrefix = DEFAULT_CONSUMER_PATH_PREFIX;
        }

        this.consumerGroup = consumerGroup;
        this.verifyCertBundle = verifyCertBundle;

        // Setup customer configs from supplied parameters
        if (extraConfigs != null) {
            this.configs.putAll(extraConfigs);
        }

        if (!this.configs.containsKey(ENABLE_AUTO_COMMIT_CONFIG_SETTING)) {
            // this has to be false for now
            this.configs.put(ENABLE_AUTO_COMMIT_CONFIG_SETTING, "false");
        }

        this.isAutoCommitEnabled = Boolean.valueOf(this.configs.get(ENABLE_AUTO_COMMIT_CONFIG_SETTING).toString());

        // State variables
        this.consumerId = null;
        this.subscriptions = new ArrayList<>();

        // Get http headers
        this.httpHeaders = getHttpHeadersFromConfig(configs);

        // Create a custom Request object so that we can store cookies across requests
        this.isHttps = base.toLowerCase().startsWith("https");
        this.httpProxySettings = httpProxySettings;
        this.request = new Request(base, auth,
                this.verifyCertBundle, this.isHttps, this.httpProxySettings, httpHeaders);

        this.retryOnFail = retryOnFail;

        this.destroying = new AtomicBoolean(false);
        this.active = true;

        this.running = new AtomicBoolean(false);
        this.stopRequested = new AtomicBoolean(false);

        this.multiTenantQueryString = "";
        if (this.configs.containsKey(MULTI_TENANT_CONFIG_SETTING)) {
            final Boolean isMultiTenant = Boolean.valueOf(this.configs.get(MULTI_TENANT_CONFIG_SETTING).toString());
            if (isMultiTenant) {
                this.multiTenantQueryString = "?multi_tenant=true";
            } else {
                this.multiTenantQueryString = "?multi_tenant=false";
            }
        }
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

                String error = "No value specified for 'consumerGroup' during channel init";
                logger.error(error);
                throw new PermanentError(error);

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
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created " + logConsumerId());
                    }
                }

            } catch (final JsonSyntaxException | ConsumerError e) {
                TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                        + e.getClass().getCanonicalName() + " " + e.getMessage(), e, "create");
                logger.error("Failed to create consumer.", temporaryError);
                throw temporaryError;
            }  catch (final ClientError error) {
                error.setApi("create");
                logger.error("Failed to create consumer.", error);
                throw error;
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

                String error = "Non-empty value must be specified for topics.";
                logger.error(error);
                throw new PermanentError(error);

            }

            // Remove any null or empty topic from list
            if (topics.isEmpty()) {

                String error = "Non-empty value must be specified for topics";
                logger.error(error);
                throw new PermanentError(error);

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
                    .append("/subscription")
                    .append(multiTenantQueryString)
                    .toString();

            final String logMessage = new StringBuilder(logConsumerId())
                    .append(" to ").append(topics).append(" topics.").toString();
            try {

                request.post(api, body, SUBSCRIBE_ERROR_MAP);
                if (logger.isDebugEnabled()) {
                    logger.debug("Subscribed " + logMessage);
                }

                subscriptions.clear();
                subscriptions.addAll(topics);

            } catch (final ClientError error) {
                error.setApi("subscribe");
                logger.error("Failed to subscribe " + logMessage, error);
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
                if (logger.isDebugEnabled()) {
                    logger.debug(logConsumerId() + " is subscribed to " + list);
                }
                return list;

            } catch (final ClientError error) {
                error.setApi("subscriptions");
                logger.error("Failed to get subscriptions of " + logConsumerId(), error);
                throw error;
            } catch (final JsonSyntaxException e) {
                TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                        + e.getClass().getCanonicalName() + " " + e.getMessage(), e, "subscriptions");
                logger.error("Failed to get subscriptions of " + logConsumerId(), e);
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
                logger.warn("Ignoring call to delete because consumerId is empty.");
                return;
            }

            String api = new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId).toString();

            try {

                request.delete(api, DELETE_ERROR_MAP);
                if (logger.isDebugEnabled()) {
                    logger.debug(logConsumerId() + " was deleted.");
                }

            } catch (final ConsumerError consumerError) {

                logger.error(logConsumerId() + " not found. Deleting consumer anyways.",
                        consumerError);

            } catch (final ClientError error) {
                error.setApi("delete");
                logger.error("Failed to delete " + logConsumerId(), error);
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
        return consume(0);
    }

    /**
     * Consumes records from all the subscribed topics
     *
     * @param timeout Timeout in milliseconds to wait for records before returning
     * @return {@link ConsumerRecords} a list of the consumer record objects from the records returned by the server.
     * @throws ConsumerError if the consumer associated with the channel does not exist on the server.
     * @throws PermanentError if the channel has not been subscribed to any topics.
     * @throws TemporaryError if the consume attempt fails.
     */
    public ConsumerRecords consume(final int timeout) throws ConsumerError, PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            if (subscriptions.isEmpty()) {
                String error = "Channel is not subscribed to any topic";
                logger.error(error);
                throw new PermanentError(error);
            }

            final StringBuilder builder = new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId)
                    .append("/records");
            if (timeout > 0) {
                builder.append("?timeout=");
                builder.append(timeout);
            }

            final String api = builder.toString();

            try {

                String responseEntity = request.get(api, CONSUME_RECORDS_ERROR_MAP);

                final Gson gson = new Gson();
                final ConsumerRecords consumerRecords = gson.fromJson(responseEntity, ConsumerRecords.class);
                if (logger.isDebugEnabled()) {
                    int numberOfRecords = consumerRecords != null && consumerRecords.getRecords() != null
                            ? consumerRecords.getRecords().size() : 0;
                    logger.debug(logConsumerId() + " consumed " + numberOfRecords + " records.");
                }
                return consumerRecords;

            } catch (final ClientError error) {
                error.setApi("consume");
                logger.error("Failed to consume with " + logConsumerId(), error);
                throw error;
            } catch (final JsonSyntaxException e) {
                TemporaryError temporaryError = new TemporaryError("Error while parsing response: "
                        + e.getClass().getCanonicalName() + " " + e.getMessage(), e, "consume");
                logger.error("Failed to consume with " + logConsumerId(), e);
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
                if (logger.isDebugEnabled()) {
                    logger.debug("Not sending commit request because enable.auto.commit is set to true.");
                }
                return;
            }

            String api = new StringBuilder(consumerPathPrefix)
                    .append("/consumers/")
                    .append(consumerId)
                    .append("/offsets").toString();

            try {

                request.post(api, null, COMMIT_ALL_RECORDS_ERROR_MAP);
                if (logger.isDebugEnabled()) {
                    logger.debug(logConsumerId() + " committed records.");
                }

            } catch (final ClientError error) {
                error.setApi("commit");
                logger.error("Failed to commit with " + logConsumerId(), error);
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
    public void run(final ConsumerRecordProcessor processCallback, final List<String> topics)
            throws PermanentError, TemporaryError {
        run(processCallback, topics, 0);
    }

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
    public void run(final ConsumerRecordProcessor processCallback, final List<String> topics, final int timeout)
            throws PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            if (consumerGroup == null || consumerGroup.isEmpty()) {
                String error = "No value specified for 'consumerGroup' during channel init";
                logger.error(error);
                throw new PermanentError(error);
            }

            if (processCallback == null) {
                String error = "processCallback not provided";
                logger.error(error);
                throw new PermanentError(error);
            }

            List<String> topicsOfInterest = topics != null && !topics.isEmpty() ? topics : subscriptions;

            if (running.compareAndSet(false, true)) {

                logger.info("Channel is running");

                while (!stopRequested.get()) {
                    consumeLoop(processCallback, topicsOfInterest, timeout);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Exiting run method.");
                }
            }

        } finally {
            running.set(false);
            release();
        }

    }

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
    public void run(final ConsumerRecordProcessor processCallback, final String topic) throws PermanentError,
            TemporaryError {
        run(processCallback, topic, 0);
    }

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
    public void run(final ConsumerRecordProcessor processCallback, final String topic, final int timeout)
            throws PermanentError, TemporaryError {

        acquireAndEnsureChannelIsActive();
        try {
            run(processCallback, topic != null && !topic.isEmpty() ? Arrays.asList(topic) : null, timeout);
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

            try {
                // wait until Channel is no longer running
                while (running.get()) {
                    Thread.sleep(STOP_CHANNEL_WAIT_PERIOD_MS);
                }
                logger.info("Channel was stopped.");

            } catch (final Exception e) {
                String error = "Failed to stop channel.";
                logger.error(error);
                throw new StopError(error, e);
            } finally {
                stopRequested.set(false);
            }
        } else {
            logger.warn("Ignoring call to stop because Channel is already being stopped.");
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

        if (destroying.compareAndSet(false, true)) {
            try {
                if (active) {

                    stop();
                    delete();
                    request.close();

                    active = false;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Channel was destroyed.");
                    }
                } else {
                    logger.warn("Ignoring call to destroy because Channel is not active.");
                }
            } finally {
                destroying.set(false);
            }
        } else {
            String error = "Channel is not safe for multi-threaded access.";
            logger.error(error);
            throw new TemporaryError(error);
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
     * @param processCallback Callable which is invoked with a list of records which have been consumed.
     * @param topics the channel will be subscribed to the specified topics.
     * @param timeout Timeout in milliseconds to wait for records before returning
     * @throws TemporaryError the consume or commit attempt failed with an error other than ConsumerError.
     * @throws PermanentError the callback asks to stop consuming records.
     */
    private void consumeLoop(final ConsumerRecordProcessor processCallback, List<String> topics, final int timeout)
            throws PermanentError, TemporaryError {

        boolean continueRunning = true;
        boolean subscribed = false;

        while (continueRunning) {

            try {
                // if consumer is not subscribed yet, then subscribe it
                if (!subscribed) {
                    subscribe(topics);
                    subscribed = true;
                    if (logger.isDebugEnabled()) {
                        // show topics consumer is subscribed to
                        subscriptions();
                    }
                }

                // consume records
                ConsumerRecords records = consume(timeout);

                // invoke callback
                continueRunning = processCallback.processCallback(records, consumerId);
                if (!continueRunning) {
                    logger.info("Callback requested to stop consuming records.");
                }

                // Commit offsets for just consumed records
                commit();

            } catch (final ConsumerError error) {
                // ConsumerError exception can be raised if the consumer has been removed or if callback found errors
                // in records and it wants them to be consumed again.
                // Then, current consumer is deleted and a brand new one is created to resume consuming from
                // last commit.
                logger.info("Creating a new consumer to resume consuming.");
                logger.error("Consumer error was: ", error);
                subscribed = false;
                recreateConsumer(topics, error);

                if (!retryOnFail) {
                    continueRunning = false;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Exiting run method because retryOnFail is " + retryOnFail);
                    }
                }

            } catch (final PermanentError | TemporaryError error) {
                // Delete consumer instance.
                delete();
                error.setApi("run");
                logger.error("Exiting run method due to error", error);
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

        if (logger.isDebugEnabled()) {
            logger.debug("Deleting current consumer and creating a brand new one.");
        }
        delete();
        request.close();
        request = new Request(base, auth, verifyCertBundle, isHttps, httpProxySettings, httpHeaders);
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


    private Map<String, String> getHttpHeadersFromConfig(final Properties configs) {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<Object, Object> config : configs.entrySet()) {
            final String key =  (String) config.getKey();
            if (key.toLowerCase().startsWith("header_"))  {
                final String headerKey = key.substring(key.indexOf("_") + 1);
                if (!headerKey.isEmpty()) {
                    final String value = (String) config.getValue();
                    headers.put(headerKey, value);
                }
            }

        }
        return headers;
    }

    /**
     * Release the light lock protecting the consumer from multi-threaded access.
     */
    private void release() {
        if (refcount.decrementAndGet() == 0)
            currentThread.set(NO_CURRENT_THREAD);
    }

    /**
     * Prepend "consumer " string to consumerId. It is used in logged messages.
     */
    private String logConsumerId() {
        return "consumer " + consumerId;
    }


    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#create()} API
     */
    private static final Map<Integer, ErrorType> CREATE_ERROR_MAP = new HashMap() {{
        put(HttpStatusCodes.BAD_REQUEST, ErrorType.PERMANENT_ERROR);
        put(HttpStatusCodes.UNAUTHORIZED, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.FORBIDDEN, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.NOT_FOUND, ErrorType.PERMANENT_ERROR);
        put(HttpStatusCodes.INTERNAL_SERVER_ERROR, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#delete()} API
     */
    private static final Map<Integer, ErrorType> DELETE_ERROR_MAP = new HashMap() {{
        put(HttpStatusCodes.BAD_REQUEST, ErrorType.PERMANENT_ERROR);
        put(HttpStatusCodes.UNAUTHORIZED, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.FORBIDDEN, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.NOT_FOUND, ErrorType.CONSUMER_ERROR);
        put(HttpStatusCodes.CONFLICT, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.INTERNAL_SERVER_ERROR, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#subscribe(List)} API
     */
    private static final Map<Integer, ErrorType> SUBSCRIBE_ERROR_MAP = new HashMap() {{
        put(HttpStatusCodes.BAD_REQUEST, ErrorType.PERMANENT_ERROR);
        put(HttpStatusCodes.UNAUTHORIZED, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.FORBIDDEN, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.NOT_FOUND, ErrorType.CONSUMER_ERROR);
        put(HttpStatusCodes.CONFLICT, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.INTERNAL_SERVER_ERROR, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#subscriptions()} API
     */
    private static final Map<Integer, ErrorType> GET_SUBSCRIPTIONS_ERROR_MAP = new  HashMap() {{
        put(HttpStatusCodes.BAD_REQUEST, ErrorType.PERMANENT_ERROR);
        put(HttpStatusCodes.UNAUTHORIZED, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.FORBIDDEN, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.NOT_FOUND, ErrorType.CONSUMER_ERROR);
        put(HttpStatusCodes.CONFLICT, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.INTERNAL_SERVER_ERROR, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#consume()} API
     */
    private static final Map<Integer, ErrorType> CONSUME_RECORDS_ERROR_MAP = new HashMap() {{
        put(HttpStatusCodes.BAD_REQUEST, ErrorType.PERMANENT_ERROR);
        put(HttpStatusCodes.UNAUTHORIZED, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.FORBIDDEN, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.NOT_FOUND, ErrorType.CONSUMER_ERROR);
        put(HttpStatusCodes.CONFLICT, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.INTERNAL_SERVER_ERROR, ErrorType.TEMPORARY_ERROR);
    }};

    /**
     * Mapping of HTTP Status Code errors to {@link ErrorType} for {@link Channel#commit()} API
     */
    private static final Map<Integer, ErrorType> COMMIT_ALL_RECORDS_ERROR_MAP = new HashMap() {{
        put(HttpStatusCodes.BAD_REQUEST, ErrorType.PERMANENT_ERROR);
        put(HttpStatusCodes.UNAUTHORIZED, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.FORBIDDEN, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.NOT_FOUND, ErrorType.CONSUMER_ERROR);
        put(HttpStatusCodes.CONFLICT, ErrorType.TEMPORARY_ERROR);
        put(HttpStatusCodes.INTERNAL_SERVER_ERROR, ErrorType.TEMPORARY_ERROR);
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
