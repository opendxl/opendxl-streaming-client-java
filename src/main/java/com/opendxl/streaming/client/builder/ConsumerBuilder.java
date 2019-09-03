/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.builder;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.Consumer;
import com.opendxl.streaming.client.ConsumerRecordProcessor;
import com.opendxl.streaming.client.HttpConnection;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.exception.TemporaryError;

import java.util.List;
import java.util.Properties;

public class ConsumerBuilder {

    /**
     * Base URL at which the streaming service resides.
     */
    private String base;

    /**
     * An implementation of Channel Authorization interface. It adds the http header with the Authorization token to
     * http requests. Two implementations are available: {@link com.opendxl.streaming.client.auth.ChannelAuthToken} and
     * {@link com.opendxl.streaming.client.auth.ChannelAuthUserPass}.
     */
    private ChannelAuth auth;

    /**
     * Consumer group to subscribe the channel consumer to.
     */
    private String consumerGroup;

    /**
     * Path to append to base for consumer-related requests made to the streaming service.
     */
    private String consumerPathPrefix;

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
     * <p>Constructor for {@link ConsumerBuilder}</p>
     */
    public ConsumerBuilder() { }

    /**
     * <p>Set the base URL to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer} instance</p>
     *
     * @param base Base URL at which the streaming service resides.
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withBase(final String base) {
        this.base = base;
        return this;
    }

    /**
     * <p>Set the Authentication object to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer}
     * instance</p>
     *
     * @param auth Authentication object to use for channel requests.
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withChannelAuth(final ChannelAuth auth) {
        this.auth = auth;
        return this;
    }

    /**
     * <p>Set the consumer group value to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer}
     * instance</p>
     *
     * @param consumerGroup Consumer group to subscribe the channel consumer to
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withConsumerGroup(final String consumerGroup) {
        this.consumerGroup = consumerGroup;
        return this;
    }

    /**
     * <p>Set the consumerPathPrefix value to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer}
     * instance</p>
     *
     * @param consumerPathPrefix Path to append to consumer-related requests made to the streaming service. Note that
     *                          if the pathPrefix parameter is set to a non-empty value, the pathPrefix value will be
     *                          appended to consumer-related requests instead of the consumerPathPrefix value.
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withConsumerPathPrefix(final String consumerPathPrefix) {
        this.consumerPathPrefix = consumerPathPrefix;
        return this;
    }

    /**
     * <p>Set the retryOnFail value to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer}
     * instance</p>
     *
     * @param retryOnFail Whether or not the channel will automatically retry a call which failed due to a temporary
     *                   error.
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withRetryOnFail(final boolean retryOnFail) {
        this.retryOnFail = retryOnFail;
        return this;
    }

    /**
     * <p>Set the certificate bundle chain to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer}
     * instance.</p>
     *
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withCertificateBundle(final String verifyCertBundle) {
        this.verifyCertBundle = verifyCertBundle;
        return this;
    }

    /**
     * <p>Set the extraConfigs value to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer}
     * instance</p>
     *
     * @param extraConfigs Dictionary of key/value pairs containing any custom configuration settings which should be
     *                     sent to the streaming service when a consumer is created. Examples of key/value pairs are:
     *                     ("auto.offset.reset", "latest"); ("request.timeout.ms", 30000) and
     *                     ("session.timeout.ms", 10000).
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withExtraConfigs(final Properties extraConfigs) {
        this.extraConfigs = extraConfigs;
        return this;
    }

    /**
     * <p>Set the httpProxySettings to be used at {@link ConsumerBuilder#build()} when creating {@link Consumer}
     * instance</p>
     *
     * @param httpProxySettings contains http proxy hostname, port, username and password.
     * @return this {@link ConsumerBuilder} instance
     */
    public ConsumerBuilder withHttpProxy(final HttpProxySettings httpProxySettings) {
        this.httpProxySettings = httpProxySettings;
        return this;
    }

    /**
     * <p>Build a {@link Channel} object using the previously set parameters and expose it as a {@link Consumer}
     * object.</p>
     *
     * @return a {@link Consumer} object which only exposes the {@link Channel} consume related methods, e.g.:
     *         {@link Channel#create()}, {@link Channel#subscribe(List)}, {@link Channel#subscriptions()},
     *         {@link Channel#consume()}, {@link Channel#commit()},
     *         {@link Channel#run(ConsumerRecordProcessor, List, int)}, {@link Channel#delete()}, {@link Channel#stop()}
     *         and {@link Channel#close()}
     * @throws TemporaryError if {@link Channel} http client request object failed to be created.
     */
    public Consumer build() throws TemporaryError {
        return new Channel(base, auth, consumerGroup, null, consumerPathPrefix, retryOnFail, verifyCertBundle,
                extraConfigs, httpProxySettings);
    }

}
