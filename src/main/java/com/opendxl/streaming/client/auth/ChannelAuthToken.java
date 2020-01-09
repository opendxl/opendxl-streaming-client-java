/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.auth;

import com.opendxl.streaming.client.ChannelAuth;

import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Authentication class where a given fixed token is used in Authorization headers of channel requests.
 */
public class ChannelAuthToken implements ChannelAuth {

    /**
     * The logger
     */
    private Logger logger = LogManager.getLogger(ChannelAuthUserPass.class);

    private String token;

    /**
     * @param token token value to use in the Authorization HTTP Header. The token is added to HTTP requests when
     *              the {@link ChannelAuthToken#authenticate(HttpRequest)} method is invoked.
     */
    public ChannelAuthToken(final String token) {

        this.token = token;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticate(HttpRequest httpRequest) {

        if (token != null) {
            httpRequest.addHeader("Authorization", "Bearer" + token);
            httpRequest.addHeader("X-Tenant-Id", "5ca969eb-2757-46ed-bc3f-f9266ccccea7");
            httpRequest.addHeader("X-CloudLink-scope", "soc.evt.vi");
            if (logger.isDebugEnabled()) {
                logger.debug("Added Authorization header: Bearer " + token);
            }
        }

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
