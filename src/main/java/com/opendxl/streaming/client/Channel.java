/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.entity.ConsumerRecords;

import com.google.gson.Gson;

import com.google.gson.annotations.SerializedName;
import com.opendxl.streaming.client.entity.ConsumerServiceError;
import com.opendxl.streaming.client.entity.Topics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The `Channel` class is responsible for all communication with the streaming service.
 * The following example demonstrates the creation of a :class:`Channel` instance and creating a consumer for the
 * consumer group:
 *
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
 *
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
    private List<CommitLog> recordsCommitLog;

    private final Request request;

    private final boolean retryOnFail;
    private boolean active;
    private boolean running;
    private boolean continueRunning;
    private boolean stopRequested;

    private ReentrantLock runLock;
    private ReentrantLock destroyLock;
    private Condition stopRequestedCondition;
    private Condition stoppedCondition;

    /**
     * Constructor parameters:
     *
     * @param base: Base URL at which the streaming service resides.
     * @param auth: Authentication object to use for channel requests.
     * @param consumerGroup: Consumer group to subscribe the channel consumer to.
     * @param pathPrefix: Path to append to streaming service requests.
     * @param consumerPathPrefix: Path to append to consumer-related requests made to the streaming service. Note that
     *                          if the path_prefix parameter is set to a non-empty value, the pathPrefix value will be
     *                          appended to consumer-related requests instead of the consumerPathPrefix value.
     * @param offset: Offset for the next record to retrieve from the streaming service for the new consume() call.
     *              Must be one of 'latest', 'earliest', or 'none'.
     * @param requestTimeout: The configuration controls the maximum amount of time the client (consumer) will wait for
     *                      the broker response of a request. If the response is not received before the request timeout
     *                      elapses the client may resend the request or fail the request if retries are exhausted. If
     *                      set to `None` (the default), the request timeout is determined automatically by the
     *                      streaming service. Note that if a value is set for the request timeout, the value should
     *                      exceed the `sessionTimeout`. Otherwise, the streaming service may fail to create new
     *                      consumers properly. To ensure that the request timeout is greater than the `sessionTimeout`,
     *                      values for either both (or neither) of the `requestTimeout` and `sessionTimeout` parameters
     *                      should be specified.
     * @param sessionTimeout: The timeout (in seconds) used to detect channel consumer failures. The consumer sends
     *                      periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by
     *                      the broker before the expiration of this session timeout, then the broker may remove this
     *                      consumer from the group. If set to `None` (the default), the session timeout is determined
     *                      automatically by the streaming service. Note that if a value is set for the session timeout,
     *                      the value should be less than the `requestTimeout`. Otherwise, the streaming service may
     *                      fail to create new consumers properly. To ensure that the session timeout is less than the
     *                      `requestTimeout`, values for either both (or neither) of the `requesTimeout` and
     *                      `sessionTimeout` parameters should be specified.
     * @param retryOnFail: Whether or not the channel will automatically retry a call which failed due to a temporary
     *                   error.
     * @param verifyCertBundle: Path to a CA bundle file containing certificates of trusted CAs. The CA bundle is used
     *                        to validate that the certificate of the authentication server being connected to was
     *                        signed by a valid authority. If set to an empty string, the server certificate is not
     *                        validated.
     * @param extraConfigs: Dictionary of key/value pairs containing any custom configuration settings which should be
     *                    sent to the streaming service when a consumer is created. Note that any values specified for
     *                    the `offset`, `requestTimeout`, and/or `sessionTimeout` parameters will override the
     *                    corresponding values, if specified, in the `extraConfigs` parameter.
     */
    public Channel(final String base, final ChannelAuth auth, final String consumerGroup,
            final Optional<String> pathPrefix, final Optional<String> consumerPathPrefix, final String offset,
            final Integer requestTimeout, final Integer sessionTimeout, final boolean retryOnFail,
            final String verifyCertBundle, final Optional<Properties> extraConfigs) {

        this.base = base;
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
        this.recordsCommitLog = new ArrayList<>();

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
        recordsCommitLog.clear();

        request.resetCookies();

    }

    /**
     * Creates a new consumer on the consumer group
     */
    public void create() {

        if (!consumerGroup.isPresent()) {

            throw new PermanentError("No value specified for 'consumerGroup' during channel init");

        }

        reset();

        // Add consumerGroup value and config properties to request payload
        ConsumerConfig consumerConfig = new ConsumerConfig(consumerGroup.get(), configs);
        Gson gson = new Gson();
        byte[] body = gson.toJson(consumerConfig).getBytes();

        HttpResponse response = request.post(consumerPathPrefix + "/consumers", Optional.of(body));
        int statusCode = response.getStatusLine().getStatusCode();
        String responseEntityString = getString(response.getEntity(), statusCode);

        if (isSuccess(statusCode)) {
            ConsumerId consumer = (ConsumerId) gson.fromJson(responseEntityString, ConsumerId.class);
            consumerId = consumer.getConsumerInstanceId();

        } else {
            throw new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + getConsumerServiceErrorMessage(responseEntityString));
        }

    }

    /**
     * Subscribes the consumer to a list of topics
     *
     * @param topics: Topic list.
     */
    public void subscribe(final List<String> topics) {

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

        HttpResponse response = request.post(api, Optional.of(body));
        int statusCode = response.getStatusLine().getStatusCode();

        if (isSuccess(statusCode)) {

            subscriptions.clear();
            subscriptions.addAll(topics);

        } else if (statusCode == 404) {

            throw new ConsumerError("Consumer " + consumerId + " does not exist");

        } else {

            throw new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + getConsumerServiceErrorMessage(getString(response.getEntity(), statusCode)));

        }

    }

    /**
     * List the topic names to which the consumer is subscribed.
     *
     * @return List of topic names
     */
    public List<String> subscriptions() {

        final Gson gson = new Gson();
        final String api =  new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/subscription").toString();

        final List<String> list = new ArrayList<>();

        final HttpResponse response = request.get(api);
        final int statusCode = response.getStatusLine().getStatusCode();
        String responseEntityString = getString(response.getEntity(), statusCode);

        if (isSuccess(statusCode)) {

            list.addAll(gson.fromJson(responseEntityString, List.class));

        } else if (statusCode == 404) {

            throw new ConsumerError("Consumer " + consumerId + " does not exist");

        } else {

            throw new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + getConsumerServiceErrorMessage(responseEntityString));

        }

        return list;

    }

    /**
     * Unsubscribe the consumer from all topics
     *
     */
    public void unsubscribe() {

        final String api =  new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/subscription").toString();

        HttpResponse response = request.delete(api);
        final int statusCode = response.getStatusLine().getStatusCode();

        if (isSuccess(statusCode)) {

            subscriptions.clear();
            return;

        } else if (statusCode == 404) {
            throw new ConsumerError("Consumer " + consumerId + " does not exist");
        } else {
            throw new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + response.getStatusLine().getReasonPhrase());
        }

    }

    /**
     * Deletes the consumer from the consumer group
     */
    public void delete() {

        if (consumerId == null) {
            return;
        }

        String api = new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId).toString();

        HttpResponse response = request.delete(api);

        // Delete session attribute values, cookies and authorization token
        reset();
        request.resetAuthorization();

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 404) {

            System.out.println("Consumer with ID " + consumerId + " not found. Resetting consumer anyways.");

        } else if (!isSuccess(statusCode)) {

            throw new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + getConsumerServiceErrorMessage(getString(response.getEntity(), statusCode)));

        }

    }

    /**
     * Consumes records from all the subscribed topics
     *
     * @return ConsumerRecords: a list of the consumer record objects from the records returned from the server.
     */
    public ConsumerRecords consume() {

        if (subscriptions.isEmpty()) {
            throw new PermanentError("Channel is not subscribed to any topic");
        }

        String api = new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/records").toString();

        HttpResponse response = request.get(api);

        int statusCode = response.getStatusLine().getStatusCode();
        if (isSuccess(statusCode)) {

            try {

                List<CommitLog> commitLogs = new ArrayList<>();

                final Gson gson = new Gson();
                final String responseEntity = EntityUtils.toString(response.getEntity());
                final ConsumerRecords consumerRecords = gson.fromJson(responseEntity, ConsumerRecords.class);

                return consumerRecords;

            } catch (final Exception e) {
                throw new TemporaryError("Error while parsing response: " + e.getClass().getCanonicalName() + " "
                        + e.getMessage());
            }
        } else if (statusCode == 404) {
            //  TODO: ADD MORE INFORMATION, LIKE ERROR CODE, TO CONSUMER ERROR
            throw new ConsumerError("Consumer " + consumerId + " does not exist");
        } else {
            throw new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + response.getStatusLine().getReasonPhrase() + " details: "
                    + getConsumerServiceErrorMessage(getString(response.getEntity(), statusCode)));
        }

    }

    /**
     * Commits the record offsets to the channel. Committed offsets are the latest consumed ones on all consumed topics
     * and partitions.
     */
    public void commit() {

        String api = new StringBuilder(consumerPathPrefix)
                .append("/consumers/")
                .append(consumerId)
                .append("/offsets").toString();

        HttpResponse response = request.post(api, Optional.empty());

        int statusCode = response.getStatusLine().getStatusCode();

        if (isSuccess(statusCode)) {

            recordsCommitLog.clear();

        } else if (statusCode == 404) {
            throw new ConsumerError("Consumer '" + consumerId + "' does not exist");
        } else {
            throw new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + response.getStatusLine().getReasonPhrase());
        }

    }

    /**
     * Repeatedly consume records from the subscribed topics. The supplied consumerRecordProcessor.callback() method
     * is invoked with a list containing each consumer record.
     *
     * The consumerRecordProcessor.callback() should return a value of True in order for this function to continue
     * consuming additional records. For a return value of False or no return value, no additional records will be
     * consumed and this function will return.
     *
     * The stop method can also be called to halt an execution of this method.
     *
     * @param processCallback: Callable which is invoked with a list of payloads from records which have been consumed.
     * @param waitBetweenQueries: Number of seconds to wait between calls to consume records.
     * @param topics: If set to a non-empty value, the channel will be subscribed to the specified topics.
     *              If set to an empty value, the channel will use topics previously subscribed via a call to the
     *              subscribe method.
     */
    public void run(final Optional<ConsumerRecordProcessor> processCallback, final int waitBetweenQueries,
                    final Optional<List<String>> topics) {

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
                subscribe(topicsOfInterest);
                continueRunning = consumeLoop(processCallback.get(), waitPeriod, topicsOfInterest);
            }

        } catch (final StopError e) {
            // Ignore it
        } finally {
            runLock.lock();
            running = false;
            stopRequested = false;
            stoppedCondition.signalAll();
            runLock.unlock();
        }

    }

    public void run(final Optional<ConsumerRecordProcessor> processCallback, final int waitBetweenQueries,
                    final String topic) {

        run(processCallback, waitBetweenQueries,
                topic != null && !topic.isEmpty() ? Optional.of(Arrays.asList(topic)) : Optional.empty());

    }

    /**
     * Stop an active execution of the :meth:`run` call. If no :meth:`run` call is active, this function returns
     * immediately. If a :meth:`run` call is active, this function blocks until the run has been completed.
     */
    public void stop() {

        runLock.lock();

        if (running) {

            stopRequested = true;
            stopRequestedCondition.signalAll();

            while (running) {
                try {

                    stoppedCondition.await();

                } catch (final Exception e) {
                    throw new StopError("Channel was stopped");
                }
            }
        }

        runLock.unlock();

    }

    /**
     * Destroys the channel (releases all associated resources).
     *
     * **NOTE:** Once the method has been invoked, no other calls should be made to the channel.
     *
     * Also note that this method should rarely be called directly. Instead, the preferred usage of the channel is via
     * a Java "try-with-resources" statement as shown below:
     *
     *      // Create the channel
     *      try (Channel channel = new Channel(("http://channel-server",
     *              auth=ChannelAuth("http://channel-server,
     *              "user", "password"),
     *              consumer_group="thegroup")) {
     *
     *                  channel.create();
     *      }
     *
     * The "try-with-resources" statement ensures that resources associated with the channel are properly cleaned up
     * when the block is exited (the :func:`destroy` method is invoked).
     */
    public void destroy() {

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
     * Closes the channel. It calls destroy() to stop the channel and to release its resources.
     *
     * This method is added to allow Channel to be used in conjunction with Java try-with-resources statement.
     */
    @Override
    public void close() {

        destroy();

    }

    /**
     * Calls consume to get records, delivers them to processCallback and commit the consumed records.
     *
     * @param processCallback
     * @param waitBetweenQueries
     * @param topics
     * @return true if callback has successfully processed records and stop has not been requested
     *         false otherwise
     */
    private boolean consumeLoop(final ConsumerRecordProcessor processCallback, final long waitBetweenQueries,
                                List<String> topics) {

        boolean continueRunning = true;

        while (continueRunning) {

            try {

                ConsumerRecords records = consume();
                continueRunning = processCallback.processCallback(records, consumerId);
                // Commit the offsets for the records which were just consumed.
                commit();

                runLock.lock();
                if (stopRequested) {
                    continueRunning = false;
                } else if (continueRunning) {
                    stopRequestedCondition.await(waitBetweenQueries, TimeUnit.SECONDS);
                    continueRunning = !stopRequested;
                }
                runLock.unlock();

            } catch (final ConsumerError e) {
                // This exception could be raised if the consumer has been removed
                System.out.println("Resetting consumer loop: " + e);
                topics = subscriptions;
                reset();
                // TODO: implement retryOnFail
//              if (!retryOnFail) {
//                  continueRunning = false;
//              }

                // TODO: catch of ConsumerProcessorIrrecoverableException and ConsumerProcessorRecoverableException
//            } catch(final ConsumerProcessorIrrecoverableException | ConsumerProcessorRecoverableException e) {
            } catch (final Exception e) {
                throw new TemporaryError("Unexpected temporary error " + e.getClass().getCanonicalName() + ": "
                        + e.getClass().getCanonicalName() + " " + e.getMessage());
            }
        }

        return continueRunning;

    }

    /**
     * Get a string from an HttpEntity.
     *
     * @param httpEntity entity to get a string from
     * @param httpStatusCode status code
     * @return String with HttpEntity contents
     * @throws TemporaryError if creating a string from the httpEntity fails. TemporaryError also contains the
     *         status code.
     */
    private static String getString(final HttpEntity httpEntity, final int httpStatusCode) {

        try {
            return EntityUtils.toString(httpEntity);
        } catch (final Exception e) {
            throw new TemporaryError("Unexpected temporary error " + httpStatusCode + ": "
                    + e.getClass().getCanonicalName() + " " + e.getMessage());
        }

    }

    /**
     * Get the error message of a ConsumerServiceError object in JSON format.
     *
     * @param responseEntityString string representing a ConsumerServiceError in JSON format
     * @return String with error message
     */
    private static String getConsumerServiceErrorMessage(final String responseEntityString) {

        Gson gson = new Gson();
        ConsumerServiceError apiGatewayError = (ConsumerServiceError) gson.fromJson(responseEntityString,
                ConsumerServiceError.class);

        return apiGatewayError != null
                ? apiGatewayError.getMessage()
                : responseEntityString;

    }

    /**
     * Checks whether a status code is successful one
     *
     * @param statusCode an HTTP Response Status Code
     * @return true if status code belongs to 2xx Success range
     *         false otherwise
     */
    private boolean isSuccess(final int statusCode) {

        return statusCode >= 200 && statusCode < 300;

    }

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
