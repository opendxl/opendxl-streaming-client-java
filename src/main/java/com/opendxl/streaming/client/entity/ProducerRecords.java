/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.entity;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>{@link ProducerRecords} is the parameter type used in
 * {@link com.opendxl.streaming.client.Channel#produce(ProducerRecords)}. They consists of a {@link List} of
 * {@link ProducerRecord} objects each one mainly containing a data pair: the payload data and the target topic. It also
 * contains other optional attributes. In order to easily create instances of {@link ProducerRecord}, it implements the
 * builder pattern (see {@link ProducerRecord.Builder}).</p>
 * <p>The sole goal of {@link ProducerRecord} is to easily build the equivalent JSON objects required by the RESTful
 * produce interface.</p>
 */
public class ProducerRecords {

    /**
     * List of ProducerRecord
     */
    private final List<ProducerRecord> records = new ArrayList<>();

    /**
     * Get the {@link List} of {@link ProducerRecord}
     *
     * @return {@link List} of {@link ProducerRecord}
     */
    public List<ProducerRecord> getRecords() {
        return records;
    }

    /**
     * Add a {@link ProducerRecord} object to the {@link ProducerRecords} internal list
     *
     * @param producerRecord the {@link ProducerRecord} to add
     */
    public void add(final ProducerRecord producerRecord) {
        records.add(producerRecord);
    }

    /**
     * <p>Helper class containing all attributes and inner classes of a produce record.</p>
     */
    public static class ProducerRecord {

        /**
         * A ProducerRecord consists of a RoutingData object and Message object. They are both mandatory attributes.
         */
        private final RoutingData routingData;
        private final Message message;

        private ProducerRecord(final RoutingData routingData, final Message message) {
            this.routingData = routingData;
            this.message = message;
        }

        /**
         * RoutingData is a class that contains the topic to produce to (mandatory value) and the shardingKey (optional
         * attribute)
         */
        public static class RoutingData {

            /**
             * topic to produce to. It is a mandatory attribute
             */
            private String topic;
            /**
             * shardingKey to use to produced. It is an optional attribute
             */
            private String shardingKey;

            public RoutingData(final String topic, final String shardingKey) {
                this.topic = topic;
                this.shardingKey = shardingKey;
            }

        }

        /**
         * Message is a class that contains the payload to produce (mandatory attribute) and a map of headers (optional
         * attribute)
         */
        public static class Message {

            /**
             * headers is an optional attribute.
             */
            private final Map<String, String> headers;
            /**
             * payload is user data to be sent encoded in Base64. Please note that the builder pattern, e.g.:
             * {@link ProducerRecord.Builder} constructor performs this encoding: it receives the user payload and
             * it encodes it to Base64.
             */
            private String payload;

            public Message(final Map<String, String> headers, final String base64EncodedPayload) {
                this.headers = headers == null ? null : (Map) headers.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, (e) -> {
                            return (String) Optional.ofNullable(e.getValue()).orElse("");
                        }));
                this.payload = base64EncodedPayload;
            }

        }

        /**
         * {@link Builder} is a helper class implementing the builder pattern to easily create {@link ProducerRecord}
         * objects.
         */
        public static class Builder {
            /**
             * topic is used to set the {@link ProducerRecord#routingData#topic} attribute. It is mandatory.
             */
            private final String topic;
            /**
             * shardingKey is used to set the {@link ProducerRecord#routingData#shardingKey} attribute. It is optional.
             */
            private String shardingKey;
            /**
             * headers is used to set the {@link ProducerRecord#message#headers} attribute. It is optional.
             */
            private Map<String, String> headers;
            /**
             * base64EncodedPayload is used to set the {@link ProducerRecord#message#payload} attribute. It is
             * mandatory. The {@link ProducerRecord#message#payload} attribute value MUST be Base64 encoded,
             * production will fail otherwise. Thus, the {@link Builder} constructor encodes the received payload
             * parameter in Base64.
             */
            private final String base64EncodedPayload;

            /**
             * {@link Builder} constructor asks for the {@link ProducerRecord} mandatory attributes which are topic and
             * payload.
             *
             * @param topic topic value to be set in ProducerRecord.routingData.topic attribute
             * @param payload payload value to be set in ProducerRecord.message.payload attribute
             */
            public Builder(final String topic, final String payload) {
                this.topic = topic;
                this.base64EncodedPayload = new String(Base64.getEncoder()
                        .encode(Optional.ofNullable(payload).orElse("").getBytes()));
            }

            /**
             * Set the shardingKey attribute value to be set in the upcoming {@link ProducerRecord} object.
             *
             * @param shardingKey value to set in the {@link RoutingData#shardingKey} attribute of the
             *                    {@link ProducerRecord} to be built.
             * @return the {@link Builder} instance
             */
            public Builder withShardingKey(final String shardingKey) {
                this.shardingKey = shardingKey;

                return this;
            }

            /**
             * Set the headers value to be set in the upcoming {@link ProducerRecord} object.
             *
             * @param headers value to be set in the {@link Message#headers} attribute of the {@link ProducerRecord}
             *                to be built
             * @return the {@link Builder} instance
             */
            public Builder withHeaders(final Map<String, String> headers) {
                this.headers = headers;

                return this;
            }

            /**
             * Create a brand new {@link ProducerRecord} object which attributes are set to the values given for
             * {@link Builder#topic}, {@link Builder#base64EncodedPayload}, {@link Builder#shardingKey} and
             * {@link Builder#headers}
             *
             * @return the new {@link ProducerRecord} object
             */
            public ProducerRecord build() {
                RoutingData routingData = new RoutingData(topic, shardingKey);
                Message message = new Message(headers, base64EncodedPayload);

                ProducerRecord producerRecord = new ProducerRecord(routingData, message);

                return producerRecord;

            }

        }

    }

}
