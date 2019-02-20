/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.entity;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Helper class use to deserialize the ConsumerService responses of consume() requests
 */
public class ConsumerRecords {

    private final List<ConsumerRecord> records = new ArrayList<>();

    public List<ConsumerRecord> getRecords() {
        return records;
    }

    public static class ConsumerRecord {

        private ConsumerRecordRoutingData routingData;
        private ConsumerRecordMessage message;
        private int partition;
        private long offset;

        public ConsumerRecordRoutingData getRoutingData() {
            return routingData;
        }

        public ConsumerRecordMessage getMessage() {
            return message;
        }

        public int getPartition() {
            return partition;
        }

        public long getOffset() {
            return offset;
        }

        public String getTopic() {
            return routingData.getTopic();
        }

        public String getShardingKey() {
            return routingData.getShardingKey();
        }

        public Map<String, String> getHeaders() {
            return message.getHeaders();
        }

        public String getPayload() {
            return message.getPayload();
        }

        public byte[] getDecodedPayload() {
            return Base64.getDecoder().decode(message.getPayload());
        }

    }

    public static class ConsumerRecordRoutingData {

        private String topic;
        private String shardingKey;

        public String getTopic() {
            return topic;
        }

        public String getShardingKey() {
            return shardingKey;
        }

    }

    public static class ConsumerRecordMessage {
        private Map<String, String> headers;
        private String payload;

        public String getPayload() {
            return payload;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

    }

}
