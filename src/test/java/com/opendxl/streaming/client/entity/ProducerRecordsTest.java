/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.extensions.PA;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ProducerRecordsTest {

    final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Test
    public final void testBuilderOfProducerRecordSetsAllItsAttributesSuccessfully() throws Exception {
        // Test
        final ProducerRecords.ProducerRecord pr = new ProducerRecords.ProducerRecord
                        .Builder("my-topic",
                        "Hello OpenDXL")
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                            put("anotherHeader", "one-two-three-four");
                        }})
                        .withShardingKey("my-sharding-key-123")
                        .build();

        // Evaluate
        Assert.assertEquals("my-topic", PA.getValue(PA.getValue(pr, "routingData"), "topic"));
        Assert.assertEquals("my-sharding-key-123", PA.getValue(PA.getValue(pr, "routingData"), "shardingKey"));
        Assert.assertEquals("SGVsbG8gT3BlbkRYTA==", PA.getValue(PA.getValue(pr, "message"), "payload"));
        final Map<String, String> headers = (Map<String, String>) PA.getValue(PA.getValue(pr, "message"), "headers");
        Assert.assertEquals(2, headers.size());
        Assert.assertEquals("sourceId", headers.keySet().toArray(new String[0])[0]);
        Assert.assertEquals("D5452543-E2FB-4585-8BE5-A61C3636819C", headers.values().toArray(new String[0])[0]);
        Assert.assertEquals("anotherHeader", headers.keySet().toArray(new String[0])[1]);
        Assert.assertEquals("one-two-three-four", headers.values().toArray(new String[0])[1]);
    }

    @Test
    public final void testProducerRecordsHasExpectedJSONStructure() throws Exception {
        // Setup
        // Setup producer records
        final ProducerRecords producerRecords = new ProducerRecords();
        producerRecords.add(
                new ProducerRecords.ProducerRecord
                        .Builder("my-topic",
                        "Hello OpenDXL - 1")
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                        }})
                        .withShardingKey("123")
                        .build()
        );
        producerRecords.add(
                new ProducerRecords.ProducerRecord
                        .Builder("topic1",
                        "Hello OpenDXL - 2")
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "F567D6A2-500E-4D35-AE15-A707f165D4FA");
                            put("anotherHeader", "one-two-three-four");
                        }})
                        .withShardingKey("456")
                        .build()
        );
        // Setup expected JSON output
        final String expectedJsonOutput = "{"
                + "\"records\":["
                + "{"
                + "\"routingData\":{"
                + "\"topic\":\"my-topic\","
                + "\"shardingKey\":\"123\""
                + "},"
                + "\"message\":{"
                + "\"headers\":{"
                + "\"sourceId\":\"D5452543-E2FB-4585-8BE5-A61C3636819C\""
                + "},"
                + "\"payload\":\"SGVsbG8gT3BlbkRYTCAtIDE=\""
                + "}"
                + "},"
                + "{"
                + "\"routingData\":{"
                + "\"topic\":\"topic1\","
                + "\"shardingKey\":\"456\""
                + "},"
                + "\"message\":{"
                + "\"headers\":{"
                + "\"sourceId\":\"F567D6A2-500E-4D35-AE15-A707f165D4FA\","
                + "\"anotherHeader\":\"one-two-three-four\""
                + "},"
                + "\"payload\":\"SGVsbG8gT3BlbkRYTCAtIDI=\""
                + "}"
                + "}"
                + "]"
                + "}";

        // Test
        final String jsonProducerRecords = gson.toJson(producerRecords, ProducerRecords.class);

        // Evaluate
        Assert.assertEquals(2, producerRecords.getRecords().size());
        Assert.assertEquals(expectedJsonOutput, jsonProducerRecords);
    }
}
