/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.builder;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.ConsumerRecordProcessor;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.HttpConnection;
import com.opendxl.streaming.client.exception.TemporaryError;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p>{@link ChannelBuilder} implements a build pattern for {@link Channel} to ease constructing instances of it.</p>
 */
public class ChannelBuilder {

    /**
     * Base URL at which the streaming service resides.
     */
    private final String base;

    /**
     * An implementation of Channel Authorization interface. It adds the http header with the Authorization token to
     * http requests. Two implementations are available: {@link com.opendxl.streaming.client.auth.ChannelAuthToken} and
     * {@link com.opendxl.streaming.client.auth.ChannelAuthUserPass}.
     */
    private final ChannelAuth auth;

    /**
     * Consumer group to subscribe the channel consumer to.
     */
    private final String consumerGroup;

    /**
     * Path to append to streaming ALL service requests, to consume so as to produce. If it is set, then its value will
     * be used for consume and produce requests while consumerPathPrefix and producerPathPrefix values will be ignored.
     */
    private String pathPrefix;

    /**
     * Path to append to base for consumer-related requests made to the streaming service.
     */
    private String consumerPathPrefix;

    /**
     * Path to append to base for producer-related requests made to the streaming service.
     */
    private String producerPathPrefix;

    /**
     * Boolean value set by Channel constructor. It indicates whether records consumption must be continued in
     * {@link Channel#run(ConsumerRecordProcessor, List)} when a ConsumerError occurs. If it is set to {@code true},
     * then a new consumer will be created and records consumption will resume. If it set to {@code false}, then
     * execution will exit {@link Channel#run(ConsumerRecordProcessor, List)} method.
     */
    private boolean retryOnFail;

    /**
     * Filename of a Certificate Bundle file. Certificates are loaded and managed by {@link HttpConnection} class.
     * Certificates are evaluated when creating SSL connections. If it is set to an empty string, then no certificate
     * nor hostname validation is performed when setting up an SSL Connection.
     */
    private String verifyCertBundle;

    /**
     * consumer additional properties
     */
    private Properties extraConfigs;

    /**
     * POJO containing http proxy hostname, port, username and password
     */
    private HttpProxySettings httpProxySettings;

    /**
     * Boolean indicating tenant specific channel or non tenant specific.
     */
    private boolean multiTenant;

    /**
     * Map of request headers.
     */
    private Map<String, String> requestHeaders;

    /**
     * <p>Constructor for {@link ChannelBuilder}</p>
     *
     * <p>Its parameters are mandatory parameters to create a {@link Channel} instance.
     *
     * @param base base URL at which the streaming service resides. This base URL applies to both, consume and produce
     *             requests.
     * @param auth Authentication object to use for channel requests.
     * @param consumerGroup Consumer group to subscribe the channel consumer to
     */
    public ChannelBuilder(final String base, final ChannelAuth auth, final String consumerGroup) {
        this.base = base;
        this.auth = auth;
        this.consumerGroup = consumerGroup;
        this.multiTenant = true;
        this.requestHeaders = Collections.emptyMap();
    }

    /**
     * <p>Set the path value to be used at {@link ChannelBuilder#build()} when creating {@link Channel} instance</p>
     *
     * @param pathPrefix Path to append to streaming service requests. If pathPrefix is set, then it will take
     *                   precedence over consumerPathPrefix and producerPathPrefix. This means that it is this
     *                   pathPrefix the one to be appended to consume and produce requests while consumerPathPrefix and
     *                   producerPathPrefix will be ignored.
     * @return this {@link ChannelBuilder} instance
     */
    public ChannelBuilder withPathPrefix(final String pathPrefix) {
        this.pathPrefix = pathPrefix;
        return this;
    }

    /**
     * <p>Set the consumerPathPrefix value to be used at {@link ChannelBuilder#build()} when creating {@link Channel}
     * instance</p>
     *
     * @param consumerPathPrefix Path to append to consumer-related requests made to the streaming service. Note that
     *                          if the pathPrefix parameter is set to a non-empty value, the pathPrefix value will be
     *                          appended to consumer-related requests instead of the consumerPathPrefix value.
     * @return this {@link ChannelBuilder} instance
     */
    public ChannelBuilder withConsumerPathPrefix(final String consumerPathPrefix) {
        this.consumerPathPrefix = consumerPathPrefix;
        return this;
    }

    /**
     * <p>Set the producerPathPrefix value to be used at {@link ChannelBuilder#build()} when creating {@link Channel}
     * instance</p>
     *
     * @param producerPathPrefix Path to append to producer-related requests made to the streaming service. Note that
     *                          if the pathPrefix parameter is set to a non-empty value, the pathPrefix value will be
     *                          appended to producer-related requests instead of the producerPathPrefix value.
     * @return this {@link ChannelBuilder}
     */
    public ChannelBuilder withProducerPathPrefix(final String producerPathPrefix) {
        this.producerPathPrefix = producerPathPrefix;
        return this;
    }

    /**
     * <p>Set the retryOnFail value to be used at {@link ChannelBuilder#build()} when creating {@link Channel}
     * instance</p>
     *
     * @param retryOnFail Whether or not the channel will automatically retry a call which failed due to a temporary
     *                   error.
     * @return this {@link ChannelBuilder} instance
     */
    public ChannelBuilder withRetryOnFail(final boolean retryOnFail) {
        this.retryOnFail = retryOnFail;
        return this;
    }

    /**
     * <p>Set the certificate bundle chain to be used at {@link ChannelBuilder#build()} when creating {@link Channel}
     * instance.</p>
     *
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @return this {@link ChannelBuilder} instance
     */
    public ChannelBuilder withCertificateBundle(final String verifyCertBundle) {
        this.verifyCertBundle = verifyCertBundle;
        return this;
    }

    /**
     * <p>Set the extraConfigs value to be used at {@link ChannelBuilder#build()} when creating {@link Channel}
     * instance</p>
     *
     * @param extraConfigs Dictionary of key/value pairs containing any custom configuration settings which should be
     *                     sent to the streaming service when a consumer is created. Examples of key/value pairs are:
     *                     ("auto.offset.reset", "latest"); ("request.timeout.ms", 30000) and
     *                     ("session.timeout.ms", 10000).
     * @return this {@link ChannelBuilder} instance
     */
    public ChannelBuilder withExtraConfigs(final Properties extraConfigs) {
        this.extraConfigs = extraConfigs;
        return this;
    }

    /**
     * <p>Set the httpProxySettings to be used at {@link ChannelBuilder#build()} when creating {@link Channel}
     * instance</p>
     *
     * @param httpProxySettings contains http proxy hostname, port, username and password.
     * @return this {@link ChannelBuilder} instance
     */
    public ChannelBuilder withHttpProxy(final HttpProxySettings httpProxySettings) {
        this.httpProxySettings = httpProxySettings;
        return this;
    }

    public ChannelBuilder withMultiTenant(final boolean multiTenant) {
        this.multiTenant = multiTenant;
        return this;
    }

    public ChannelBuilder withRequestHeaders(final Map<String, String> headers) {
        this.requestHeaders = headers;
        return this;
    }

    /**
     * <p>Build a {@link Channel} object using the previously set parameters.</p>
     *
     * @return a {@link Channel} object which can consume records from subscribed topics and also produce records to
     *         topics specified in the {@link com.opendxl.streaming.client.entity.ProducerRecords}
     * @throws TemporaryError if {@link Channel} http client request object failed to be created.
     */
    public Channel build() throws TemporaryError {
        return new Channel(base, auth, consumerGroup, pathPrefix, consumerPathPrefix, producerPathPrefix, retryOnFail,
                verifyCertBundle, extraConfigs, httpProxySettings, multiTenant, requestHeaders);
    }

}
