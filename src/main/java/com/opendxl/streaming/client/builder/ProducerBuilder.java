/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.builder;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.HttpConnection;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.Producer;
import com.opendxl.streaming.client.entity.ProducerRecords;
import com.opendxl.streaming.client.exception.TemporaryError;

public class ProducerBuilder {

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
     * Path to append to base for producer-related requests made to the streaming service.
     */
    private String producerPathPrefix;

    /**
     * Filename of a Certificate Bundle file. Certificates are loaded and managed by {@link HttpConnection} class.
     * Certificates are evaluated when creating SSL connections. If it is set to an empty string, then no certificate
     * nor hostname validation is performed when setting up an SSL Connection.
     */
    private String verifyCertBundle;

    /**
     * POJO containing http proxy hostname, port, username and password
     */
    private HttpProxySettings httpProxySettings;

    /**
     * <p>Constructor for {@link ProducerBuilder}</p>
     *
     * @param base base URL at which the streaming service resides.
     * @param auth Authentication object to use for channel requests.
     */
    public ProducerBuilder(final String base, final ChannelAuth auth) {
        this.base = base;
        this.auth = auth;
    }

    /**
     * <p>Set the producerPathPrefix to be used at {@link ProducerBuilder#build()} when creating {@link Producer}
     * instance</p>
     *
     * @param producerPathPrefix Path to append to producer-related requests made to the streaming service. Note that
     *                          if the pathPrefix parameter is set to a non-empty value, the pathPrefix value will be
     *                          appended to producer-related requests instead of the producerPathPrefix value.
     * @return this {@link ProducerBuilder} instance
     */
    public ProducerBuilder withProducerPathPrefix(final String producerPathPrefix) {
        this.producerPathPrefix = producerPathPrefix;
        return this;
    }

    /**
     * <p>Set the certificate bundle chain to be used at {@link ProducerBuilder#build()} when creating {@link Producer}
     * instance</p>
     *
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @return this {@link ProducerBuilder} instance
     */
    public ProducerBuilder withCertificateBundle(final String verifyCertBundle) {
        this.verifyCertBundle = verifyCertBundle;
        return this;
    }

    /**
     * <p>Set the httpProxySettings to be used at {@link ProducerBuilder#build()} when creating {@link Producer}
     * instance</p>
     *
     * @param httpProxySettings contains http proxy hostname, port, username and password.
     * @return this {@link ProducerBuilder} instance
     */
    public ProducerBuilder withHttpProxy(final HttpProxySettings httpProxySettings) {
        this.httpProxySettings = httpProxySettings;
        return this;
    }

    /**
     * <p>Build a {@link Channel} object using the previously set parameters and expose it as a {@link Producer}
     * object.</p>
     *
     * @return a {@link Producer} object which only exposes the {@link Channel#produce(ProducerRecords)},
     *         {@link Channel#produce(String)} and {@link Channel#close()} methods
     * @throws TemporaryError if {@link Channel} http client request object failed to be created.
     */
    public Producer build() throws TemporaryError {
        return new Channel(base, auth, null, null, null, producerPathPrefix, false, verifyCertBundle,
                null, httpProxySettings);
    }
}
