/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.entity;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.entity.ProducerRecords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link SimplifiedProducerRecords} is collection of {@link SimplifiedProducerRecord} object. It is a helper class
 * to create an equivalent {@link ProducerRecords} object as required by {@link Channel#produce(ProducerRecords)}
 */
public final class SimplifiedProducerRecords {
    /**
     * List of {@SimplifiedProducerRecord} objects
     */
    private List<SimplifiedProducerRecord> records = new ArrayList<>();

    /**
     * {@link SimplifiedProducerRecords} is a public constructor which takes a single String argument as required by
     * {@link joptsimple} requirement. {@link joptsimple} internally calls this constructor to convert a
     * {@link joptsimple.OptionSpec} into a {@link SimplifiedProducerRecords} object.
     *
     * @param jsonSimplifiedProducerRecords JSON string representation of the {@link SimplifiedProducerRecords} object
     *                                      as typed in by the user
     */
    public SimplifiedProducerRecords(final String jsonSimplifiedProducerRecords) {
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        SimplifiedProducerRecord[] parsedInputData = gson.fromJson(jsonSimplifiedProducerRecords,
                SimplifiedProducerRecord[].class);

        records.addAll(Arrays.asList(Optional.ofNullable(parsedInputData)
                .orElseThrow(() -> new IllegalArgumentException("records parameter may not be empty"))));
    }

    /**
     * Create a {@link ProducerRecords} object from the {@link SimplifiedProducerRecords} JSON objects specified by user
     * in the --records CLI parameter. Each element of {@link ProducerRecords} is equivalent (e.g.: same topic value,
     * same payload, same headers, etc.) to its corresponding element of {@link SimplifiedProducerRecords} and they are
     * presented in the same order.
     *
     * @return an equivalent {@link ProducerRecords} object
     */
    public ProducerRecords getProducerRecords() {

        final ProducerRecords producerRecords = new ProducerRecords();
        for (SimplifiedProducerRecord record : records) {
            producerRecords.add(
                    new ProducerRecords.ProducerRecord
                            .Builder(record.getTopic(), record.getPayload())
                            .withShardingKey(record.getShardingKey())
                            .withHeaders(record.getHeaders())
                            .build());
        }

        return producerRecords;

    }
}

/**
 * Helper class intended to reduce the typing required to enter {@link ProducerRecords} value from the
 * CLI. The {@link SimplifiedProducerRecord} consists of exactly the same basic attributes of a
 * {@link ProducerRecords.ProducerRecord} but they are arranged in a plain structure, e.g.:
 * while a {@link ProducerRecords.ProducerRecord} structure is:
 *
 * ProducerRecord
 *      RoutingData
 *          topic
 *          shardingKey
 *      Message
 *          headers
 *          payload
 *
 * the {@link SimplifiedProducerRecord} structure is:
 *
 * SimplifiedProducerRecord
 *      topic
 *      payload
 *      shardingKey
 *      headers
 *
 * CLI "--records" parameter asks for a {@link SimplifiedProducerRecord} JSON array and then the CLI internally
 * builds the equivalent {@link ProducerRecords} object (check {@link SimplifiedProducerRecords#getProducerRecords()}
 * method).
 */
final class SimplifiedProducerRecord {
    /**
     * topic where to produce the payload and headers
     */
    private final String topic;
    /**
     * message payload to produce
     * Later, when {@link ProducerRecords.ProducerRecord.Builder} builds the
     * equivalent {@link ProducerRecords.ProducerRecord} object, it will be Base64 encoded as required by
     * the produce RESTful service.
     */
    private final String payload;
    /**
     * shardingKey value can be used to indicate a preferred partition where to produce the record.
     * If shardingkey is present, then the partition will be chosen using a hash of the shardingKey. All records
     * having the same shardingKey value, thus yielding the same hash value, will be produced on the same partition.
     * If shardingKey is not present, then the partition will be assigned in a round-robin fashion.
     */
    private final String shardingKey;
    /**
     * The headers that will be included in the record
     */
    private final Map<String, String> headers;

    SimplifiedProducerRecord(final String topic, final String payload, final String shardingKey,
                             final Map<String, String> headers) {

        this.topic = topic;
        this.payload = payload;
        this.shardingKey = shardingKey;
        this.headers = headers;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public String getShardingKey() {
        return shardingKey;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
