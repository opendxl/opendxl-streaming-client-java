/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.entity;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * <p>Helper class use to deserialize the ConsumerService responses of consume() requests.</p>
 *
 * <p>It consists of a list of {@link ConsumerRecord} objects</p>
 */
public class ConsumerRecords {

    private final List<ConsumerRecord> records = new ArrayList<>();

    public List<ConsumerRecord> getRecords() {
        return records;
    }

    /**
     * <p>Helper class containing all attributes of a consumed record.</p>
     */
    public static class ConsumerRecord {

        private ConsumerRecordRoutingData routingData;
        private ConsumerRecordMessage message;
        private int partition;
        private long offset;

        /**
         * Gets the record routing data
         *
         * @return a {@link ConsumerRecordRoutingData} object containing the record routing data
         */
        public ConsumerRecordRoutingData getRoutingData() {
            return routingData;
        }

        /**
         * Gets the record message
         *
         * @return a {@link ConsumerRecordMessage} object containing the record message
         */
        public ConsumerRecordMessage getMessage() {
            return message;
        }

        /**
         * Gets the partition from which the record is consumed.
         *
         * @return number corresponding to the partition from which the record is consumed
         */
        public int getPartition() {
            return partition;
        }

        /**
         * Gets the record offset.
         *
         * @return number corresponding to the record offset
         */
        public long getOffset() {
            return offset;
        }

        /**
         * Gets the topic name from which the record is consumed.
         *
         * @return record topic name
         */
        public String getTopic() {
            return routingData.getTopic();
        }

        /**
         * Gets the sharding key of the consumed record.
         *
         * @return sharding key value
         */
        public String getShardingKey() {
            return routingData.getShardingKey();
        }

        /**
         * Gets the record headers.
         *
         * @return the record headers
         */
        public Map<String, String> getHeaders() {
            return message.getHeaders();
        }

        /**
         * Gets the record payload.
         *
         * @return the record payload
         */
        public String getPayload() {
            return message.getPayload();
        }

        /**
         * Decodes a Base64 encoded record payload.
         *
         * @return a Base64 decoded payload
         */
        public byte[] getDecodedPayload() {
            return Base64.getDecoder().decode(message.getPayload());
        }

    }

    /**
     * Topic name and sharding key of the consumed record.
     */
    public static class ConsumerRecordRoutingData {

        private String topic;
        private String shardingKey;

        /**
         * Gets the record topic.
         *
         * @return the record topic name
         */
        public String getTopic() {
            return topic;
        }

        /**
         * Gets the record sharding key.
         *
         * @return the record sharding key
         */
        public String getShardingKey() {
            return shardingKey;
        }

    }

    /**
     * Headers and payload of the consumed record.
     */
    public static class ConsumerRecordMessage {
        private Map<String, String> headers;
        private String payload;

        /**
         * Gets the record payload.
         *
         * @return the record payload
         */
        public String getPayload() {
            return payload;
        }

        /**
         * Gets the record headers.
         *
         * @return the record headers
         */
        public Map<String, String> getHeaders() {
            return headers;
        }

    }

}
