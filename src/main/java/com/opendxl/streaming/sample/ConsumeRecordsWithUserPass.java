/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.sample;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.auth.ChannelAuthUserPass;
import com.opendxl.streaming.client.ConsumerRecordProcessor;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * This example uses the opendxl-streaming-java-client to consume records from a Databus topic. It instantiates
 * a ChannelAuthUserPass object, a Channel object and a ConsumerRecordProcessor object. Then, the Channel run method is
 * invoked and Channel starts to indefinitely consume records and to deliver them to ConsumerRecordProcessor which
 * finally prints them out. To quit this sample program, press CTRL+C or execute kill to call Channel destroy method
 * which will stop the Channel and gracefully exit.
 */
public class ConsumeRecordsWithUserPass {

    private ConsumeRecordsWithUserPass() { }

    public static void main(String[] args) {

        String channelUrl = "http://127.0.0.1:50080";
        String channelUsername = "me";
        String channelPassword = "secret";
        String channelConsumerGroup = "sample_consumer_group";
        List<String> channelTopicSubscriptions = Arrays.asList("case-mgmt-events",
                "my-topic",
                "topic-abc123",
                "topic1");

        // Path to a CA bundle file containing certificates of trusted CAs. The CA
        // bundle is used to validate that the certificate of the server being connected
        // to was signed by a valid authority. If set to an empty string, the server
        // certificate is not validated.
        String verifyCertificateBundle = "/tmp/certificates.crt";

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

        try (Channel channel = new Channel(channelUrl,
                new ChannelAuthUserPass(channelUrl,
                        channelUsername,
                        channelPassword,
                        null,
                        verifyCertificateBundle),
                channelConsumerGroup,
                null,
                null,
                true,
                verifyCertificateBundle,
                extraConfigs)) {

            // Setup shutdown hook to call stop when program is terminated
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        System.out.println("Shutdown app requested. Exiting");

                        try {
                            channel.stop();
                        } catch (final StopError e) {
                            System.out.println("Failed to shutdown app." + e.getMessage());
                        }
                    }));

            // Create object which processCallback() method will be called back upon by the run method (see below)
            // when records are received from the channel
            ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {

                @Override
                public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) {
                    // Print the payloads which were received. 'payloads' is a list of
                    // dictionary objects extracted from the records received from the
                    // channel.
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

            // Consume records indefinitely
            channel.run(consumerRecordCallback, channelTopicSubscriptions);

        } catch (final PermanentError | StopError | TemporaryError e) {

            System.out.println("Error occurred: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            System.out.println(e.getCause() != null
                    ? e.getClass().getCanonicalName() + ": " + e.getCause().getMessage()
                    : "no exception cause reported");

            e.printStackTrace();

        }

    }

}
