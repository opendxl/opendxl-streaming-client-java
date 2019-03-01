/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.sample;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.ConsumerRecordProcessor;
import com.opendxl.streaming.client.Error;
import com.opendxl.streaming.client.entity.ConsumerRecords;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * This example uses the opendxl-streaming-java-client to consume records from a Databus topic. It instantiates
 * a ChannelAuth object, a Channel object and a ConsumerRecordProcessor object. Then, the Channel run method is invoked
 * and Channel starts to indefinitely consume records and to deliver them to ConsumerRecordProcessor which finally
 * prints them out. To quit this sample program, press CTRL+C or execute kill to call Channel destroy method which
 * will stop the Channel and gracefully exit.
 */
public class ConsumerRecordCallbackLocalKafka {

    private ConsumerRecordCallbackLocalKafka() { }

    public static void main(String[] args) {

        String channelUrl = "http://127.0.0.1:48080";
        String channelUsername = "me";
        String channelPassword = "secret";
        String channelConsumerGroup = "cg1";
        List<String> channelTopicSubscriptions = Arrays.asList("case-mgmt-events",
                "my-topic",
                "topic-abc123",
                "topic1");

        // Path to a CA bundle file containing certificates of trusted CAs. The CA
        // bundle is used to validate that the certificate of the server being connected
        // to was signed by a valid authority. If set to an empty string, the server
        // certificate is not validated.
        String verifyCertificateBundle = "";

        // This constant controls the frequency (in seconds) at which the channel 'run'
        // call below polls the streaming service for new records.
        int waitBetweenQueries = 20;

        Properties extraConfigs = new Properties();
        extraConfigs.put("enable.auto.commit", false);
        extraConfigs.put("auto.commit.interval.ms", 0);

        try (Channel channel = new Channel(channelUrl,
                new ChannelAuth(channelUrl,
                        channelUsername,
                        channelPassword,
                        Optional.empty(),
                        verifyCertificateBundle),
                channelConsumerGroup,
                Optional.empty(),
                Optional.of("/v1"),
                "earliest",
                16,
                15,
                true,
                verifyCertificateBundle,
                Optional.of(extraConfigs))) {

            // Setup shutdown hook to call stop when program is terminated
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        System.out.println("Shutdown app requested. Exiting");

                        try {
                            channel.destroy();
                        } catch (final Error e) {
                            System.out.println("Failed to shutdown app.");
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
                    // TODO: call a logger.info() method instead of System.out.println()
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
                    return true;
                }
            };

            // Consume records indefinitely
            channel.run(Optional.ofNullable(consumerRecordCallback), waitBetweenQueries,
                    Optional.ofNullable(channelTopicSubscriptions));

        } catch (final Error e) {

            System.out.println("Error occurred: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            System.out.println(e.getCause() != null
                    ? e.getClass().getCanonicalName() + ": " + e.getCause().getMessage()
                    : "no exception cause reported");

            e.printStackTrace();

        }

    }

}
