/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.auth;

import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class ChannelAuthUserPassTest {

    private ChannelAuthUserPass channelAuthUserPass;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public final void setUp() throws PermanentError, TemporaryError {
        this.channelAuthUserPass = new ChannelAuthUserPass("http://localhost:8080",
                "username",
                "password",
                null,
                null,
                null);
    }

    @After
    public final void tearDown() {
    }

    @Test
    public final void testAuthenticateSuccessful() throws PermanentError, TemporaryError {
        // Setup
        HttpPost request = new HttpPost();
        // set up authenticate response
        stubFor(get(urlEqualTo("/identity/v1/login"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic dXNlcm5hbWU6cGFzc3dvcmQ="))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"authorizationToken\":\"MY_AUTHORIZATION_TOKEN\"}")));

        // Test
        channelAuthUserPass.authenticate(request);

        // Evaluate
        Assert.assertEquals(1, request.getHeaders(HttpHeaders.AUTHORIZATION).length);
        Assert.assertEquals(HttpHeaders.AUTHORIZATION, request.getHeaders(HttpHeaders.AUTHORIZATION)[0].getName());
        Assert.assertEquals("Bearer MY_AUTHORIZATION_TOKEN",
                request.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue());
    }

    @Test
    public final void testAuthenticateFailsWith401() throws PermanentError, TemporaryError {
        // Setup
        HttpPost request = new HttpPost();
        // set up response to create
        stubFor(get(urlEqualTo("/identity/v1/login"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Dummy 401 error message")));

        // Test
        PermanentError error = null;
        try {
            channelAuthUserPass.authenticate(request);
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals("Unauthorized 401: Dummy 401 error message", error.getMessage());
    }

    @Test
    public final void testAuthenticateFailsWith403() throws PermanentError, TemporaryError {
        // Setup
        HttpPost request = new HttpPost();
        // set up response to create
        stubFor(get(urlEqualTo("/identity/v1/login"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("Dummy 403 error message")));

        // Test
        PermanentError error = null;
        try {
            channelAuthUserPass.authenticate(request);
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals("Unauthorized 403: Dummy 403 error message", error.getMessage());
    }

    @Test
    public final void testAuthenticateFailsWith404() throws PermanentError, TemporaryError {
        // Setup
        HttpPost request = new HttpPost();
        // set up response to create
        stubFor(get(urlEqualTo("/identity/v1/login"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Dummy 404 error message")));

        // Test
        TemporaryError error = null;
        try {
            channelAuthUserPass.authenticate(request);
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals("Unexpected status code 404: Dummy 404 error message", error.getMessage());
    }

    @Test
    public final void testAuthenticateDoesNotAddAuthorizationHeader() throws PermanentError, TemporaryError {
        // Setup
        HttpPost request = new HttpPost();
        // set up response to create
        stubFor(get(urlEqualTo("/identity/v1/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"something\":\"invalid\"}")));

        // Test
        channelAuthUserPass.authenticate(request);

        // Evaluate
        Assert.assertEquals(0, request.getHeaders(HttpHeaders.AUTHORIZATION).length);
    }

}
