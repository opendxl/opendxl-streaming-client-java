/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package sample;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.auth.ChannelAuthToken;
import com.opendxl.streaming.client.ConsumerRecordProcessor;
import com.opendxl.streaming.client.builder.ChannelBuilder;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * This example uses the opendxl-streaming-java-client to consume records from a Databus topic. It instantiates
 * a ChannelAuthToken object, a Channel object and a ConsumerRecordProcessor object. Then, the Channel run method is
 * invoked and Channel starts to indefinitely consume records and to deliver them to ConsumerRecordProcessor which
 * finally prints them out. To quit this sample program, press CTRL+C or execute kill to call Channel destroy method
 * which will stop the Channel and gracefully exit.
 */
public class ConsumeRecordsWithToken {

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
    private static Logger logger = Logger.getLogger(ConsumeRecordsWithToken.class);


    private ConsumeRecordsWithToken() { }

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

        try (Channel channel = new ChannelBuilder(channelUrl, new ChannelAuthToken(token), channelConsumerGroup)
                .withExtraConfigs(extraConfigs)
                .withRetryOnFail(true)
                .withCertificateBundle(verifyCertificateBundle)
                .withHttpProxy(new HttpProxySettings(PROXY_ENABLED,
                        PROXY_HOST,
                        PROXY_PORT,
                        PROXY_USR,
                        PROXY_PWD))
                .build()) {

            // Setup shutdown hook to call stop when program is terminated
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        logger.info("Shutdown app requested. Exiting");

                        try {
                            channel.stop();
                        } catch (final StopError e) {
                            logger.error("Failed to shutdown app." + e.getMessage());
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

            // Consume records indefinitely
            final int consumePollTimeoutMs = 500;
            channel.run(consumerRecordCallback, channelTopicSubscriptions, consumePollTimeoutMs);

        } catch (final PermanentError | StopError | TemporaryError e) {

            logger.error("Error occurred: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            logger.error(e.getCause() != null
                    ? e.getClass().getCanonicalName() + ": " + e.getCause().getMessage()
                    : "no exception cause reported");

            e.printStackTrace();

        }

    }

}
