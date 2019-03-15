/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.auth;

import com.opendxl.streaming.client.ChannelAuth;

import org.apache.http.HttpRequest;

import java.util.Optional;

/**
 * Authentication class where a given fixed token is used in Authorization headers of channel requests.
 */
public class ChannelAuthToken implements ChannelAuth {

    private Optional<String> token;

    /**
     * @param token token value to use in the Authorization HTTP Header. The token is added to HTTP requests when
     *              the {@link ChannelAuthToken#authenticate(HttpRequest)} method is invoked.
     */
    public ChannelAuthToken(final String token) {

        this.token = Optional.ofNullable(token);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticate(HttpRequest httpRequest) {

        token.ifPresent(value -> httpRequest.addHeader("Authorization", "Bearer " + value));

    }

    /**
     * This method does no operation in {@link ChannelAuthToken} class. If a new token has to be provided, then
     * it is necessary to create a new {@link ChannelAuthToken} instance and provide the new token to the
     * {@link ChannelAuthToken#ChannelAuthToken(String)} constructor.
     */
    @Override
    public void reset() {

    }

}
