/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package sample;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ConsumerRecordProcessor;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.auth.ChannelAuthToken;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.entity.ProducerRecords;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This example uses the opendxl-streaming-java-client to produce records to and consume records from Databus topics.
 * It instantiates a ChannelAuthToken object, two Channel objects: one to produce records to topics and another to
 * consume records from topics and finally a ConsumerRecordProcessor object. Then, a thread is instantiated to
 * call the produce() method on one Channel object to produce records periodically. Then, the run() method is
 * invoked on the other Channel object so it starts to indefinitely consume records and to deliver them
 * to ConsumerRecordProcessor which finally prints them out. To quit this sample program, press CTRL+C or execute kill
 * to call Channel destroy method which will stop the both Channel instances and gracefully exit.
 */
public class ProduceAndConsumeRecordsWithToken {

    private static final String CHANNEL_URL = "http://127.0.0.1:50080";
    private static final String TOKEN = "TOKEN3";
    private static final String CONSUMER_GROUP = "sample_consumer_group";
    private static final List<String> TOPICS = Arrays.asList("case-mgmt-events",
            "my-topic",
            "topic-abc123",
            "topic1");

    private static final String VERIFY_CERTIFICATE_BUNDLE = "-----BEGIN CERTIFICATE-----"
            + "MIIDBzCCAe+gAwIBAgIJALteQYzVdTj3MA0GCSqGSIb3DQEBBQUAMBoxGDAWBgNV"
            + "BAMMD3d3dy5leGFtcGxlLmNvbTAeFw0xOTA0MjIxNTI2MjZaFw0yOTA0MTkxNTI2"
            + "MjZaMBoxGDAWBgNVBAMMD3d3dy5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEB"
            + "BQADggEPADCCAQoCggEBAMHv/jBHmUI6s2FhDdw4I7I9RTU3yvpkXM1e/5ISfBwe"
            + "18gkeml7q9t9eLpPc08W0akYn/SySeT0TEvw6w8mpCfEefe+RHg7f6taAzzMwtei"
            + "bt98VSdrckQh2DfL+Dp47BeP/XsHh80V4rschYbK/RCt6tMARcR5VRoC3VETKGqH"
            + "tGTgUjLrpsCqsPTQuSLaST8brLp0KBVS1T39ltB6UFLdmw3WxiuuHvy9Tk5KLuFv"
            + "SjfR6zPP/b9BsnYw35rceEB/+bh3KGCnTS6hO1Qbt3sAolOc6Y8VuDAQRZfsD7m5"
            + "8hrsvT/7VRBr0RoWUSYTZJRXrPUUmjP3CMJkfeXOauUCAwEAAaNQME4wHQYDVR0O"
            + "BBYEFN6UJ/tpppQTRvb5zo+6nnPGfJoXMB8GA1UdIwQYMBaAFN6UJ/tpppQTRvb5"
            + "zo+6nnPGfJoXMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBAB635aKg"
            + "mAifM5P/T9Xt8tFpIfVWGRc9dbdWsce1/IMoMUDwQuGpmvdKfY68FVN4niKC/HeB"
            + "J4OBflvM7by8KBC28N/g8It/rqOCU14JFyCcYQPpPj7uTFLwiGuraGnnCGX+GYW3"
            + "bGSCAVnJvH0gb0kWFTJwdK1dBUsMrRwHDhkrYLe8Z6NzT39VA3hI9cedbzIsfAJf"
            + "pRBkBaMQyB6u15NHnqKy57zpekuoChU8snWLu7G8E6coMW1AlMGuNiZZqX3XCvAd"
            + "8gc45ashE41QRpGz9fh3FfUJIq1BBoIjvJahzIPLVfvfDhTwpBHZ+PJkBcsUUgcf"
            + "lHTRe1CZks4JfS8="
            + "-----END CERTIFICATE-----";

    private static final boolean PROXY_ENABLED = true;
    private static final String PROXY_HOST = "10.20.30.40";
    private static final int PROXY_PORT = 8080;
    private static final String PROXY_USR = "";
    private static final String PROXY_PWD = "";


    private ProduceAndConsumeRecordsWithToken() { }

    public static void main(String[] args) {

        String channelUrl = CHANNEL_URL;
        String token = TOKEN;
        String channelConsumerGroup = CONSUMER_GROUP;
        List<String> channelTopicSubscriptions = TOPICS;

        // CA bundle certificate chain of trusted CAs provided as a string. The CA
        // bundle is used to validate that the certificate of the server being connected
        // to was signed by a valid authority. If set to an empty string, the server
        // certificate is not validated.
        String verifyCertificateBundle = VERIFY_CERTIFICATE_BUNDLE;

        Properties extraConfigs = new Properties();
        extraConfigs.put("enable.auto.commit", false);
        extraConfigs.put("auto.commit.interval.ms", 0);
        /**
         * Offset for the next record to retrieve from the streaming service for the new {@link Channel#consume()} call.
         * Must be one of 'latest', 'earliest', or 'none'.
         */
        extraConfigs.put("auto.offset.reset", "earliest");
        /**
         * "request.timeout.ms" controls the maximum amount of time the client (consumer) will wait for
         * the broker response of a request. If the response is not received before the request timeout
         * elapses the client may resend the request or fail the request if retries are exhausted. If
         * set to {@code null}, the request timeout is determined automatically by the
         * streaming service. Note that if a value is set for the request timeout, the value should
         * exceed the session timeout. Otherwise, the streaming service may fail to create new
         * consumers properly. To ensure that the request timeout is greater than the session timeout,
         * values for either both (or neither) of the request timeout and session timeout parameters
         * should be specified.
         */
        extraConfigs.put("request.timeout.ms", 16000);
        /**
         *  "session.timeout.ms" is used to detect channel consumer failures. The consumer sends
         *  periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by
         *  the broker before the expiration of this session timeout, then the broker may remove this
         *  consumer from the group. If set to {@code null}, the session timeout is determined
         *  automatically by the streaming service. Note that if a value is set for the session timeout,
         *  the value should be less than the request timeout. Otherwise, the streaming service may
         *  fail to create new consumers properly. To ensure that the session timeout is less than the
         *  request timeout, values for either both (or neither) of the request timeout and
         *  session timeout parameters should be specified.
         */
        extraConfigs.put("session.timeout.ms", 15000);

        /**
         * Two channel objects are instantiated, one to produce records and the other to consume them.
         * Rationale is that Channel API can be used by one thread at a time. If
         * {@link Channel#run(ConsumerRecordProcessor, List, int)} is called, it starts consuming records continuously
         * and it will not exit run() API, thus preventing other threads from calling other Channel API methods
         * in general, and {@link Channel#produce(String)} in particular.
         */
        try (Channel consumeChannel = new Channel(channelUrl,
                new ChannelAuthToken(token),
                channelConsumerGroup,
                null,
                null,
                true,
                verifyCertificateBundle,
                extraConfigs,
                new HttpProxySettings(PROXY_ENABLED,
                        PROXY_HOST,
                        PROXY_PORT,
                        PROXY_USR,
                        PROXY_PWD));
             Channel produceChannel = new Channel(channelUrl,
                     new ChannelAuthToken(token),
                     channelConsumerGroup,
                     null,
                     null,
                     null,
                     true,
                     verifyCertificateBundle,
                     extraConfigs,
                     new HttpProxySettings(PROXY_ENABLED,
                             PROXY_HOST,
                             PROXY_PORT,
                             PROXY_USR,
                             PROXY_PWD))) {

            /**
             * Create thread to produce records to selected topics. When
             * {@link com.opendxl.streaming.client.Channel#run()} is called, these records will be consumed and
             * printed out by the consumerRecordCallback.
             */
            final AtomicBoolean keepProducing = new AtomicBoolean(true);
            final Thread channelProduceThread = createProduceThread(produceChannel, keepProducing);

            // Setup shutdown hook to call stop when program is terminated
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        System.out.println("Shutdown app requested. Exiting");

                        try {
                            keepProducing.set(false);
                            consumeChannel.stop();
                        } catch (final StopError e) {
                            System.out.println("Failed to shutdown app." + e.getMessage());
                        }
                    }));

            // Create object which processCallback() method will be called back upon by the run method (see below)
            // when records are received from the channel
            ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {

                @Override
                public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) {
                    // Print the received payloads. 'payloads' is a list of
                    // dictionary objects extracted from the records received
                    // from the channel.
                    System.out.println(new StringBuilder("Received ")
                            .append(consumerRecords.getRecords().size())
                            .append(" records")
                            .toString());

                    for (ConsumerRecords.ConsumerRecord record : consumerRecords.getRecords()) {

                        System.out.println("topic = " + record.getTopic());
                        System.out.println("partition = " + record.getPartition());
                        System.out.println("offset = " + record.getOffset());
                        System.out.println("sharding key = " + record.getShardingKey());
                        System.out.println("headers = " + record.getHeaders());
                        System.out.println("payload = " + record.getPayload());
                        System.out.println("decoded payload = " + new String(record.getDecodedPayload()));
                        System.out.println("");

                    }

                    // Return 'True' in order for the 'run' call to continue attempting to consume records.
                    System.out.println("let commit records");
                    return true;
                }
            };

            // Produce records indefinitely
            channelProduceThread.start();
            // Consume records indefinitely
            final int consumePollTimeoutMs = 500;
            consumeChannel.run(consumerRecordCallback, channelTopicSubscriptions, consumePollTimeoutMs);

        } catch (final PermanentError | StopError | TemporaryError e) {

            printError(e);

        }

    }

    /**
     * Helper method to set up the produce thread, e.g.: the thread that will periodically produce 2 ProduceRequest
     * until keepProducing flag is set to false
     *
     * @param produceChannel the channel object to use to produce records
     * @param keepProducing control flag which tells the thread when to stop producing
     * @return the thread object to produce records
     */
    private static Thread createProduceThread(final Channel produceChannel, final AtomicBoolean keepProducing) {

        Runnable produceChannelRunnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 1; keepProducing.get(); ++i) {
                    /**
                     * Produce records: two records are sent in a single call to
                     * {@link com.opendxl.streaming.client.Channel#produce()}
                     */
                    final ProducerRecords producerRecords = new ProducerRecords();
                    producerRecords.add(
                            new ProducerRecords.ProducerRecord
                                    .Builder("my-topic",
                                    "Hello from OpenDXL - " + i)
                                    .withHeaders(new HashMap<String, String>() {{
                                        put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                                    }})
                                    .withShardingKey("123")
                                    .build()
                    );
                    producerRecords.add(
                            new ProducerRecords.ProducerRecord
                                    .Builder("topic1",
                                    "Hello from OpenDXL - " + ++i)
                                    .withHeaders(new HashMap<String, String>() {{
                                        put("sourceId", "F567D6A2-500E-4D35-AE15-A707f165D4FA");
                                    }})
                                    .withShardingKey("123")
                                    .build()
                    );
                    try {
                        System.out.println("\nproduce records " + (i - 1) + " and " + i + "\n");
                        produceChannel.produce(producerRecords);
                    } catch (final PermanentError | TemporaryError e) {
                        printError(e);
                    }

                    sleep(2000);
                }
            }
        };

        return new Thread(produceChannelRunnable);
    }

    /**
     * Helper method to print out error information. It can be called when producing and when consuming records.
     *
     * @param e Exception occurred when producing or consuming
     */
    private static void printError(final Exception e) {
        System.out.println("Error occurred: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
        System.out.println(e.getCause() != null
                ? e.getClass().getCanonicalName() + ": " + e.getCause().getMessage()
                : "no exception cause reported");

        e.printStackTrace();
    }

    /**
     * Make a pause for the given number of milliseconds
     *
     * @param timeMs number of milliseconds to sleep
     */
    private static void sleep(final long timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
