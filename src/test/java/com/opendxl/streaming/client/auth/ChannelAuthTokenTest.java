/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.auth;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Test;

public class ChannelAuthTokenTest {

    @Test
    public final void testAuthenticateAddsAuthorizationHeaderWhenTokenIsNotNull() {
        // Setup
        HttpPost request = new HttpPost();
        ChannelAuthToken channelAuthToken = new ChannelAuthToken("MY_AUTHORIZATION_TOKEN");

        // Test
        channelAuthToken.authenticate(request);

        // Evaluate
        Assert.assertEquals(1, request.getHeaders(HttpHeaders.AUTHORIZATION).length);
        Assert.assertEquals(HttpHeaders.AUTHORIZATION, request.getHeaders(HttpHeaders.AUTHORIZATION)[0].getName());
        Assert.assertEquals("Bearer MY_AUTHORIZATION_TOKEN",
                request.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue());
    }

    @Test
    public final void testAuthenticateDoesNotAddAuthorizationHeaderWhenTokenIsNull() {
        // Setup
        HttpPost request = new HttpPost();
        ChannelAuthToken channelAuthToken = new ChannelAuthToken(null);

        // Test
        channelAuthToken.authenticate(request);

        // Evaluate
        Assert.assertEquals(0, request.getHeaders(HttpHeaders.AUTHORIZATION).length);
    }

}
