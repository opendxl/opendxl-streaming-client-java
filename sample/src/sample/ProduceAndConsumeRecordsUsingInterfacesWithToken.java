/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package sample;

import com.opendxl.streaming.client.ConsumerRecordProcessor;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.auth.ChannelAuthToken;
import com.opendxl.streaming.client.Consumer;
import com.opendxl.streaming.client.builder.ConsumerBuilder;
import com.opendxl.streaming.client.Producer;
import com.opendxl.streaming.client.builder.ProducerBuilder;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.entity.ProducerRecords;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * This example uses the opendxl-streaming-java-client to produce records to and consume records from Databus topics.
 * It instantiates a ChannelAuthToken object, a {@link Producer} object to produce records to topics, a {@link Consumer}
 * object to consume records from topics and finally a ConsumerRecordProcessor object. Then, a thread is instantiated
 * to call {@link Producer#produce(ProducerRecords)} to produce records periodically. Then,
 * {@link Consumer#run(ConsumerRecordProcessor, List, int)} is invoked so {@link Consumer} object starts to indefinitely
 * consume records and to deliver them to ConsumerRecordProcessor which finally prints them out. To quit this sample
 * program, press CTRL+C or execute kill to stop both, Producer Thread and Consumer object, and to then gracefully exit.
 */
public class ProduceAndConsumeRecordsUsingInterfacesWithToken {

    private static final String CHANNEL_URL = "http://127.0.0.1:50080";
    private static final String TOKEN = "TOKEN3";
    private static final String CONSUMER_GROUP = "sample_consumer_group";
    private static final List<String> TOPICS = Arrays.asList("case-mgmt-events",
            "my-topic",
            "topic-abc123",
            "topic1");

    private static final String VERIFY_CERTIFICATE_BUNDLE = "-----BEGIN CERTIFICATE-----\n"
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

    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(Consumer.class);

    private ProduceAndConsumeRecordsUsingInterfacesWithToken() { }

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
         * Offset for the next record to retrieve from the streaming service for the new {@link Consumer#consume()} call
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
         * Two objects, a {@link Producer} and {@link Consumer} objects are instantiated, one to produce records and
         * the other to consume them. Instances are respectively created by {@link ProducerBuilder} and
         * {@link ConsumerBuilder} using the build pattern. A {@link Producer} object can produce records but it
         * cannot consume them. Conversely, a {@link Consumer} object can subscribe to topics, consume records and
         * commit them but it cannot produce them.
         */
        try (Consumer consumer = new ConsumerBuilder()
                .withBase(channelUrl)
                .withChannelAuth(new ChannelAuthToken(token))
                .withConsumerGroup(channelConsumerGroup)
                .withRetryOnFail(true)
                .withCertificateBundle(verifyCertificateBundle)
                .withExtraConfigs(extraConfigs)
                .withHttpProxy(new HttpProxySettings(PROXY_ENABLED,
                        PROXY_HOST,
                        PROXY_PORT,
                        PROXY_USR,
                        PROXY_PWD))
                .build();
             Producer producer = new ProducerBuilder()
                     .withBase(channelUrl)
                     .withChannelAuth(new ChannelAuthToken(token))
                     .withCertificateBundle(verifyCertificateBundle)
                     .withHttpProxy(new HttpProxySettings(PROXY_ENABLED,
                             PROXY_HOST,
                             PROXY_PORT,
                             PROXY_USR,
                             PROXY_PWD))
                     .build()) {

            /**
             * Create thread to produce records to selected topics. When
             * {@link Consumer#run(ConsumerRecordProcessor, List, int)} is called, these records will be consumed and
             * printed out by the consumerRecordCallback.
             */
            final Thread produceThread = createProduceThread(producer);

            // Setup shutdown hook to call stop when program is terminated
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        logger.info("Shutdown app requested. Exiting");

                        try {
                            produceThread.interrupt();
                            produceThread.join();
                            consumer.stop();
                        } catch (final StopError e) {
                            logger.error("Failed to shutdown app." + e.getMessage());
                        } catch (InterruptedException e) {
                            logger.error("Failed to stop producing while shutting down app." + e.getMessage());
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
                    logger.info(new StringBuilder("Received ")
                            .append(consumerRecords.getRecords().size())
                            .append(" records")
                            .toString());

                    for (ConsumerRecords.ConsumerRecord record : consumerRecords.getRecords()) {

                        logger.info("topic = " + record.getTopic());
                        logger.info("partition = " + record.getPartition());
                        logger.info("offset = " + record.getOffset());
                        logger.info("sharding key = " + record.getShardingKey());
                        logger.info("headers = " + record.getHeaders());
                        logger.info("payload = " + record.getPayload());
                        logger.info("decoded payload = " + new String(record.getDecodedPayload()));
                        logger.info("");

                    }

                    // Return 'True' in order for the 'run' call to continue attempting to consume records.
                    logger.info("let commit records");
                    return true;
                }
            };

            // Produce records indefinitely
            produceThread.start();
            // Consume records indefinitely
            final int consumePollTimeoutMs = 500;
            consumer.run(consumerRecordCallback, channelTopicSubscriptions, consumePollTimeoutMs);

        } catch (final PermanentError | StopError | TemporaryError e) {

            printError(e);

        }

    }

    /**
     * Helper method to set up the produce thread, e.g.: the thread that will periodically produce 2 ProduceRecords
     * until it is terminated
     *
     * @param producer the object to use to produce records
     * @return the thread object to produce records
     */
    private static Thread createProduceThread(final Producer producer) {

        Runnable produceChannelRunnable = new Runnable() {
            @Override
            public void run() {
                /**
                 * counter which value is appended to produce record payloads.
                 * Its goal is cosmetic: just to always produce records different payloads.
                 */
                int recordCounter = 1;
                while (!Thread.interrupted()) {
                    /**
                     * Produce records: two records are sent in a single call to
                     * {@link Producer#produce(ProducerRecords)}
                     * so many records can be produced in just a single call.
                     */
                    final ProducerRecords producerRecords = new ProducerRecords();
                    producerRecords.add(
                            new ProducerRecords.ProducerRecord
                                    .Builder("my-topic",
                                    "Hello from OpenDXL - " + recordCounter)
                                    .withHeaders(new HashMap<String, String>() {{
                                        put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                                    }})
                                    .withShardingKey("123")
                                    .build()
                    );
                    producerRecords.add(
                            new ProducerRecords.ProducerRecord
                                    .Builder("topic1",
                                    "Hello from OpenDXL - " + (recordCounter + 1))
                                    .withHeaders(new HashMap<String, String>() {{
                                        put("sourceId", "F567D6A2-500E-4D35-AE15-A707f165D4FA");
                                    }})
                                    .withShardingKey("123")
                                    .build()
                    );
                    try {
                        logger.info("produce records " + recordCounter + " and " + (recordCounter + 1));
                        producer.produce(producerRecords);
                        recordCounter += 2;
                    } catch (final PermanentError | TemporaryError e) {
                        printError(e);
                    }
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
        logger.error("Error occurred: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
        logger.error(e.getCause() != null
                ? e.getClass().getCanonicalName() + ": " + e.getCause().getMessage()
                : "no exception cause reported");

        e.printStackTrace();
    }

}
