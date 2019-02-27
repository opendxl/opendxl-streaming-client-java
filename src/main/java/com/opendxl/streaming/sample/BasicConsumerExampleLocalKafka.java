/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.sample;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.entity.ConsumerRecords;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * This example uses the opendxl-streaming-java-client to consume records from a Databus topic. It gets an
 * Authentication token from the configured channelUrlLogin and then it calls Databus Consumer Service APIs to create
 * a consumer, to subscribe to a topic and to consume records from such topic.
 */
public class BasicConsumerExampleLocalKafka {

    private BasicConsumerExampleLocalKafka() { }

    public static void main(String[] args) {

        String channelUrl = "http://127.0.0.1:48080";
        String channelUsername = "me";
        String channelPassword = "secret";
        String channelConsumerGroup = "cg1";
        List<String> channelTopicSubscriptions = Arrays.asList("case-mgmt-events",
                "my-topic",
                "topic-abc123");

        // Path to a CA bundle file containing certificates of trusted CAs. The CA
        // bundle is used to validate that the certificate of the server being connected
        // to was signed by a valid authority. If set to an empty string, the server
        // certificate is not validated.
        String verifyCertificateBundle = "";

        // This constant controls the frequency (in seconds) at which the channel 'run'
        // call below polls the streaming service for new records.
        int waitBetweenQueries = 5;

        try {

            ChannelAuth channelAuth = new ChannelAuth(channelUrl,
                    channelUsername,
                    channelPassword,
                    Optional.empty(),
                    verifyCertificateBundle);

            // Custom configuration values just to test they are accurately set in Consumer instance of ConsumerService
            Properties extraConfigs = new Properties();
            extraConfigs.put("enable.auto.commit", false);
            extraConfigs.put("auto.commit.interval.ms", 0);

            Channel channel = new Channel(channelUrl,
                    channelAuth,
                    channelConsumerGroup,
                    Optional.empty(),
                    Optional.of("/v1"),
                    "earliest",
                    301,
                    300,
                    false,
                    verifyCertificateBundle,
                    Optional.of(extraConfigs));
            // get a consumer instance from Databus Consumer Service
            channel.create();

            // subscribe consumer to "topic1"
            channel.subscribe(Arrays.asList("topic1"));

            // though not necessary, verify consumer is subscribe to "topic1"
            System.out.println("Subscribed to: " + channel.subscriptions());

            // consume records from "topic1" for a while
            for (int i = 0; i < 10; ++i) {

                ConsumerRecords consumerRecords = channel.consume();

                System.out.println("consume read " + consumerRecords.getRecords().size() + " records");

                // print out record attributes
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

                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                }
            }

            // though not necessary, verify consumer is still subscribed to "topic1"
            System.out.println("Subscribed to: " + channel.subscriptions());

            // remove all subscriptions
            channel.unsubscribe();

            // verify subscriptions were removed
            System.out.println("Subscribed to: " + channel.subscriptions());

            // delete consumer instance from Databus Consumer Service
            channel.delete();

        } catch (final Error e) {

            System.out.println("Error occurred: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            System.out.println(e.getCause() != null
                    ? e.getClass().getCanonicalName() + ": " + e.getCause().getMessage()
                    : "no exception cause reported");

            e.printStackTrace();

        }

    }

}
