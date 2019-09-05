/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.auth.ChannelAuthToken;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.entity.ProducerRecords;
import com.opendxl.streaming.client.exception.ClientError;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import junit.extensions.PA;

import com.google.gson.Gson;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;


public class ChannelTest {

    private static final String CONSUMER_ID =
            "c4b60c6e-931e-496c-97c6-86c2935a353196fa80a1-f911-47ee-9a35-fc40a8c5137e";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public final void setUp() {
    }

    @After
    public final void tearDown() {
    }

    // Channel create tests
    @Test
    public final void testCreateSuccessful() throws Exception {
        // Setup
        Channel channel = setupChannelCreatesConsumerTests();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"consumerInstanceId\":\"" + CONSUMER_ID + "\"}")));

        // Test
        channel.create();

        // Evaluate
        String consumerId = (String) PA.getValue(channel, "consumerId");
        Assert.assertEquals("Created Consumer Id", CONSUMER_ID, consumerId);
    }

    @Test
    public final void testCreateFailsWithTemporaryErrorDueToWrongResponseFormat() throws Exception {
        // Setup
        Channel channel = setupChannelCreatesConsumerTests();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("Wrong Format Response")));
        TemporaryError error = null;

        // Test
        try {
            channel.create();
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(0, error.getStatusCode());
        Assert.assertTrue(error.getMessage().startsWith("Error while parsing response: "));
        Assert.assertEquals("create", error.getApi());
    }

    @Test
    public final void testCreateFailWithPermanentErrorDueToNotFound() throws Exception {
        // Setup
        Channel channel = setupChannelCreatesConsumerTests();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Dummy 404 error message")));
        PermanentError error = null;

        // Test
        try {
            channel.create();
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(404, error.getStatusCode());
        Assert.assertEquals("Dummy 404 error message: HTTP/1.1 404 Not Found", error.getMessage());
        Assert.assertEquals("create", error.getApi());
    }

    @Test
    public final void testCreateFailsWithTemporaryErrorDueToUnauthorized() throws Exception {
        // Setup
        Channel channel = setupChannelCreatesConsumerTests();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Dummy 401 error message")));
        TemporaryError error = null;

        // Test
        try {
            channel.create();
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(401, error.getStatusCode());
        Assert.assertEquals("Dummy 401 error message: HTTP/1.1 401 Unauthorized", error.getMessage());
        Assert.assertEquals("create", error.getApi());
    }

    @Test
    public final void testCreateFailsWithUnmappedHttpError() throws Exception {
        // Setup
        Channel channel = setupChannelCreatesConsumerTests();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(418)
                        .withBody("Dummy 418 error message")));
        TemporaryError error = null;

        // Test
        try {
            channel.create();
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(418, error.getStatusCode());
        Assert.assertEquals("Unexpected temporary error: HTTP/1.1 418 I'm a Teapot", error.getMessage());
        Assert.assertEquals("create", error.getApi());
    }


    // Channel subscribe tests
    @Test
    public final void testSubscribeSuccessful() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        List<String> topicsToSubscribe = Arrays.asList("topic1", " ", "t-o-p-i-c");
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .withRequestBody(equalToJson("{\"topics\":[\"topic1\", \" \", \"t-o-p-i-c\"]}"))
                .willReturn(aResponse()
                        .withStatus(204)));

        // Test
        channel.subscribe(topicsToSubscribe);

        // Evaluate
        List<String> subscriptions = (List<String>) PA.getValue(channel, "subscriptions");
        Assert.assertEquals(3, subscriptions.size());
        Assert.assertEquals(topicsToSubscribe.get(0), subscriptions.get(0));
        Assert.assertEquals(topicsToSubscribe.get(1), subscriptions.get(1));
        Assert.assertEquals(topicsToSubscribe.get(2), subscriptions.get(2));
    }

    @Test
    public final void testSubscribeFailWithConsumerErrorDueTo404() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        List<String> topicsToSubscribe = Arrays.asList("topic1", null, " ", "t-o-p-i-c");
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Dummy 404 error message")));
        ConsumerError error = null;

        // Test
        try {
            channel.subscribe(topicsToSubscribe);
        } catch (final ConsumerError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(404, error.getStatusCode());
        Assert.assertEquals("Dummy 404 error message: HTTP/1.1 404 Not Found", error.getMessage());
        Assert.assertEquals("subscribe", error.getApi());
    }

    @Test
    public final void testSubscribeFailWithPermanentErrorDueTo400() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        List<String> topicsToSubscribe = Arrays.asList("topic1", null, " ", "t-o-p-i-c");
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("Dummy 400 error message")));
        PermanentError error = null;

        // Test
        try {
            channel.subscribe(topicsToSubscribe);
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(400, error.getStatusCode());
        Assert.assertEquals("Dummy 400 error message: HTTP/1.1 400 Bad Request", error.getMessage());
        Assert.assertEquals("subscribe", error.getApi());
    }

    @Test
    public final void testSubscribeFailWithTemporaryErrorDueTo409() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        List<String> topicsToSubscribe = Arrays.asList("topic1", null, " ", "t-o-p-i-c");
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withBody("Dummy 409 error message")));
        TemporaryError error = null;

        // Test
        try {
            channel.subscribe(topicsToSubscribe);
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(409, error.getStatusCode());
        Assert.assertEquals("Dummy 409 error message: HTTP/1.1 409 Conflict", error.getMessage());
        Assert.assertEquals("subscribe", error.getApi());
    }

    // Channel get subscription tests
    @Test
    public final void testSubscriptionsSuccessful() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"topic-1\",\"topic-2\",\"topic-3\"]")));

        // Test
        List<String> subscriptions = channel.subscriptions();

        // Evaluate
        Assert.assertEquals(3, subscriptions.size());
        Assert.assertEquals("topic-1", subscriptions.get(0));
        Assert.assertEquals("topic-2", subscriptions.get(1));
        Assert.assertEquals("topic-3", subscriptions.get(2));
    }

    @Test
    public final void testSubscriptionsFailWithConsumerErrorDueTo404() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Dummy 404 error message")));
        ConsumerError error = null;

        // Test
        try {
            channel.subscriptions();
        } catch (final ConsumerError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(404, error.getStatusCode());
        Assert.assertEquals("Dummy 404 error message: HTTP/1.1 404 Not Found", error.getMessage());
        Assert.assertEquals("subscriptions", error.getApi());
    }

    @Test
    public final void testSubscriptionsFailWithPermanentErrorDueTo400() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("Dummy 400 error message")));
        PermanentError error = null;

        // Test
        try {
            channel.subscriptions();
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(400, error.getStatusCode());
        Assert.assertEquals("Dummy 400 error message: HTTP/1.1 400 Bad Request", error.getMessage());
        Assert.assertEquals("subscriptions", error.getApi());
    }

    @Test
    public final void testSubscriptionsFailWithTemporaryErrorDueTo500() throws ClientError {
        // Setup
        Channel channel = getCreatedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Dummy 500 error message")));
        TemporaryError error = null;

        // Test
        try {
            channel.subscriptions();
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(500, error.getStatusCode());
        Assert.assertEquals("Dummy 500 error message: HTTP/1.1 500 Server Error", error.getMessage());
        Assert.assertEquals("subscriptions", error.getApi());
    }

    // Channel consume tests
    @Test
    public final void testConsumeSuccessful() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/records"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"records\":["
                                        + JSON_CONSUMER_RECORD_0 + ","
                                        + JSON_CONSUMER_RECORD_1 + ","
                                        + JSON_CONSUMER_RECORD_2 + "]}")));
        // Test
        ConsumerRecords consumerRecords = channel.consume();

        // Evaluate
        Assert.assertEquals(3, consumerRecords.getRecords().size());
        Assert.assertEquals("UGF5bG9hZCAjMQ==", consumerRecords.getRecords().get(0).getPayload());
        Assert.assertEquals("UGF5bG9hZCAjMg==", consumerRecords.getRecords().get(1).getPayload());
        Assert.assertEquals("UGF5bG9hZCAjMw==", consumerRecords.getRecords().get(2).getPayload());

        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals("topic-" + i, consumerRecords.getRecords().get(i).getTopic());
            Assert.assertEquals("shardingKey-" + i, consumerRecords.getRecords().get(i).getShardingKey());
            Assert.assertEquals(4, consumerRecords.getRecords().get(i).getHeaders().size());
            Assert.assertEquals("sourceId-" + i, consumerRecords.getRecords().get(i).getHeaders().get("sourceId"));
            Assert.assertEquals("scope-" + i, consumerRecords.getRecords().get(i).getHeaders().get("scope"));
            Assert.assertEquals("tenantId-" + i, consumerRecords.getRecords().get(i).getHeaders().get("tenantId"));
            Assert.assertEquals("zoneId-" + i, consumerRecords.getRecords().get(i).getHeaders().get("zoneId"));
            Assert.assertEquals(2 * i, consumerRecords.getRecords().get(i).getPartition());
            Assert.assertEquals(2 * i + 1, consumerRecords.getRecords().get(i).getOffset());
        }
    }

    @Test
    public final void testConsumeWithTimeoutSuccessful() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        final int timeoutMs = 1234;
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID
                + "/records?timeout=" + timeoutMs))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"records\":["
                                + JSON_CONSUMER_RECORD_0 + ","
                                + JSON_CONSUMER_RECORD_1 + ","
                                + JSON_CONSUMER_RECORD_2 + "]}")));
        // Test
        ConsumerRecords consumerRecords = channel.consume(timeoutMs);

        // Evaluate
        Assert.assertEquals(3, consumerRecords.getRecords().size());
        Assert.assertEquals("UGF5bG9hZCAjMQ==", consumerRecords.getRecords().get(0).getPayload());
        Assert.assertEquals("UGF5bG9hZCAjMg==", consumerRecords.getRecords().get(1).getPayload());
        Assert.assertEquals("UGF5bG9hZCAjMw==", consumerRecords.getRecords().get(2).getPayload());

        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals("topic-" + i, consumerRecords.getRecords().get(i).getTopic());
            Assert.assertEquals("shardingKey-" + i, consumerRecords.getRecords().get(i).getShardingKey());
            Assert.assertEquals(4, consumerRecords.getRecords().get(i).getHeaders().size());
            Assert.assertEquals("sourceId-" + i, consumerRecords.getRecords().get(i).getHeaders().get("sourceId"));
            Assert.assertEquals("scope-" + i, consumerRecords.getRecords().get(i).getHeaders().get("scope"));
            Assert.assertEquals("tenantId-" + i, consumerRecords.getRecords().get(i).getHeaders().get("tenantId"));
            Assert.assertEquals("zoneId-" + i, consumerRecords.getRecords().get(i).getHeaders().get("zoneId"));
            Assert.assertEquals(2 * i, consumerRecords.getRecords().get(i).getPartition());
            Assert.assertEquals(2 * i + 1, consumerRecords.getRecords().get(i).getOffset());
        }
    }

    @Test
    public final void testConsumeFailWithConsumerErrorDueTo404() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/records"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Dummy 404 error message")));
        ConsumerError error = null;

        // Test
        try {
            channel.consume();
        } catch (final ConsumerError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(404, error.getStatusCode());
        Assert.assertEquals("Dummy 404 error message: HTTP/1.1 404 Not Found", error.getMessage());
        Assert.assertEquals("consume", error.getApi());
    }

    @Test
    public final void testConsumeFailWithPermanentErrorDueTo400() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/records"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("Dummy 400 error message")));
        PermanentError error = null;

        // Test
        try {
            channel.consume();
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(400, error.getStatusCode());
        Assert.assertEquals("Dummy 400 error message: HTTP/1.1 400 Bad Request", error.getMessage());
        Assert.assertEquals("consume", error.getApi());
    }

    @Test
    public final void testConsumeFailWithTemporaryErrorDueTo409() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/records"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withBody("Dummy 409 error message")));
        TemporaryError error = null;

        // Test
        try {
            channel.consume();
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(409, error.getStatusCode());
        Assert.assertEquals("Dummy 409 error message: HTTP/1.1 409 Conflict", error.getMessage());
        Assert.assertEquals("consume", error.getApi());
    }

    // Channel commit tests
    @Test
    public final void testCommitSuccessful() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/offsets"))
                .willReturn(aResponse()
                        .withStatus(204)));

        // Test
        channel.commit();

        // Evaluate
        List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching(
                "/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/offsets")));
        Assert.assertEquals(1, requests.size());
    }

    @Test
    public final void testCommitFailWithConsumerErrorDueTo404() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/offsets"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Dummy 404 error message")));
        ConsumerError error = null;

        // Test
        try {
            channel.commit();
        } catch (final ConsumerError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(404, error.getStatusCode());
        Assert.assertEquals("Dummy 404 error message: HTTP/1.1 404 Not Found", error.getMessage());
        Assert.assertEquals("commit", error.getApi());
    }

    @Test
    public final void testCommitFailWithPermanentErrorDueTo400() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/offsets"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("Dummy 400 error message")));
        PermanentError error = null;

        // Test
        try {
            channel.commit();
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(400, error.getStatusCode());
        Assert.assertEquals("Dummy 400 error message: HTTP/1.1 400 Bad Request", error.getMessage());
        Assert.assertEquals("commit", error.getApi());
    }

    @Test
    public final void testCommitFailWithTemporaryErrorDueTo500() throws ClientError {
        // Setup
        Channel channel = getSubscribedChannel();
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/offsets"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Dummy 500 error message")));
        TemporaryError error = null;

        // Test
        try {
            channel.commit();
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(500, error.getStatusCode());
        Assert.assertEquals("Dummy 500 error message: HTTP/1.1 500 Server Error", error.getMessage());
        Assert.assertEquals("commit", error.getApi());
    }


    // Channel delete tests
    @Test
    public final void testDeleteSuccessful() throws ClientError {
        // Setup
        // Setup a ChannelAuth mock to verify that its reset() method is called once when channel is deleted
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getSubscribedChannel(channelAuthMock);
        // Add a dummy Cookie to the CookieStore to test that Channel.delete() has emptied the CookieStore.
        Request request = (Request) PA.getValue(channel, "request");
        HttpConnection httpConnection = (HttpConnection) PA.getValue(request, "httpConnection");
        HttpClientContext httpClientContext = (HttpClientContext) PA.getValue(httpConnection, "httpClientContext");
        httpClientContext.getCookieStore().addCookie(new BasicClientCookie("UnitTest", "DeleteChannel"));
        // Check CookieStore is not empty
        Assert.assertTrue(!httpClientContext.getCookieStore().getCookies().isEmpty());

        stubFor(delete(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID))
                .willReturn(aResponse()
                        .withStatus(204)));

        // Test
        channel.delete();

        // Evaluate
        Assert.assertTrue(null == PA.getValue(channel, "consumerId"));
        Assert.assertTrue(((List) PA.getValue(channel, "subscriptions")).isEmpty());
        // Check cookies were deleted
        Assert.assertTrue(httpClientContext.getCookieStore().getCookies().isEmpty());
        // Check authorization token was deleted
        Mockito.verify(channelAuthMock, Mockito.times(1)).reset();
    }

    // Channel run tests
    @Test
    public final void testRunAndStopSuccessful() throws ClientError {
        // Setup
        // Setup mock URLs and ConsumerRecords to be returned
        List<String> jsonConsumerRecords = Arrays.asList(JSON_CONSUMER_RECORD_0,
                JSON_CONSUMER_RECORD_1,
                JSON_CONSUMER_RECORD_2);
        setUpWireMockStubs(CONSUMER_ID, "topic-1", jsonConsumerRecords.get(0));
        // Setup channel object under test
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getCreatedChannel(channelAuthMock);
        // Setup two empty lists to collect records received by the callback
        List<ConsumerRecords.ConsumerRecord> consumedRecords = new ArrayList<>();
        List<String> consumedConsumerId = new ArrayList<>();
        // Setup callback which receives consumed records
        ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {
            int jsonConsumerRecordIndex = 0;
            @Override
            public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) throws PermanentError {
                jsonConsumerRecordIndex++;
                // keep consumerRecords to compare them at test end
                if (!consumerRecords.getRecords().isEmpty()) {
                    consumedRecords.addAll(consumerRecords.getRecords());
                    consumedConsumerId.add(consumerId);
                }
                // set up next mocked record to be consumed
                if (jsonConsumerRecordIndex < jsonConsumerRecords.size()) {
                    stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/records"))
                            .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("{\"records\":["
                                            + jsonConsumerRecords.get(jsonConsumerRecordIndex) + "]}")));
                } else {
                    // set up next records are empty
                    stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/records"))
                            .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("{\"records\":[]}")));
                }
                return true;
            }
        };
        // Setup a thread which will call channel.run() in background. Test main thread will call channel.stop() later.
        Runnable channelRun = new Runnable() {
            @Override
            public void run() {
                try {
                    channel.run(consumerRecordCallback, "topic-1");
                } catch (final ClientError e) {
                    Assert.fail("Unexpected ClientError exception: " + e.getMessage());
                }
            }
        };
        Thread runThread = new Thread(channelRun);

        // Test
        runThread.start();
        try {
            // Brief wait to let run consume records
            sleepRunConsumeRecords();
            channel.stop();
        } catch (StopError stopError) {
            Assert.fail("Unexpected StopError: " + stopError.getMessage());
        }

        // Evaluate
        Assert.assertEquals(3, consumedConsumerId.size());
        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals(CONSUMER_ID, consumedConsumerId.get(i));
        }

        Assert.assertEquals(3, consumedRecords.size());
        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals("topic-" + i, consumedRecords.get(i).getTopic());
            Assert.assertEquals("shardingKey-" + i, consumedRecords.get(i).getShardingKey());
            Assert.assertEquals(4, consumedRecords.get(i).getHeaders().size());
            Assert.assertEquals("sourceId-" + i, consumedRecords.get(i).getHeaders().get("sourceId"));
            Assert.assertEquals("scope-" + i, consumedRecords.get(i).getHeaders().get("scope"));
            Assert.assertEquals("tenantId-" + i, consumedRecords.get(i).getHeaders().get("tenantId"));
            Assert.assertEquals("zoneId-" + i, consumedRecords.get(i).getHeaders().get("zoneId"));
            Assert.assertEquals(2 * i, consumedRecords.get(i).getPartition());
            Assert.assertEquals(2 * i + 1, consumedRecords.get(i).getOffset());
        }
    }

    @Test
    public final void testRunWithConsumeTimeout() throws ClientError {
        // Setup
        // Setup mock URLs and ConsumerRecords to be returned
        List<String> jsonConsumerRecords = Arrays.asList(JSON_CONSUMER_RECORD_0,
                JSON_CONSUMER_RECORD_1,
                JSON_CONSUMER_RECORD_2);
        // Setup timeout for consume mock URL
        final int timeoutMs = 5678;
        setUpWireMockStubs(CONSUMER_ID, "topic-1", jsonConsumerRecords.get(0), timeoutMs);
        // Setup channel object under test
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getCreatedChannel(channelAuthMock);
        // Setup two empty lists to collect records received by the callback
        List<ConsumerRecords.ConsumerRecord> consumedRecords = new ArrayList<>();
        List<String> consumedConsumerId = new ArrayList<>();
        // Setup callback which receives consumed records
        ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {
            int jsonConsumerRecordIndex = 0;
            @Override
            public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) throws PermanentError {
                jsonConsumerRecordIndex++;
                // keep consumerRecords to compare them at test end
                if (!consumerRecords.getRecords().isEmpty()) {
                    consumedRecords.addAll(consumerRecords.getRecords());
                    consumedConsumerId.add(consumerId);
                }
                // set up next mocked record to be consumed
                if (jsonConsumerRecordIndex < jsonConsumerRecords.size()) {
                    stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId
                            + "/records?timeout=" + Integer.toString(timeoutMs)))
                            .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("{\"records\":["
                                            + jsonConsumerRecords.get(jsonConsumerRecordIndex) + "]}")));
                } else {
                    // set up next records are empty
                    stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId
                            + "/records?timeout=" + Integer.toString(timeoutMs)))
                            .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("{\"records\":[]}")));
                }
                return true;
            }
        };
        // Setup a thread which will call channel.run() in background. Test main thread will call channel.stop() later.
        Runnable channelRun = new Runnable() {
            @Override
            public void run() {
                try {
                    channel.run(consumerRecordCallback, Arrays.asList("topic-1"), timeoutMs);
                } catch (final ClientError e) {
                    Assert.fail("Unexpected ClientError exception: " + e.getMessage());
                }
            }
        };
        Thread runThread = new Thread(channelRun);

        // Test
        runThread.start();
        try {
            // Brief wait to let run consume records
            sleepRunConsumeRecords();
            channel.stop();
        } catch (StopError stopError) {
            Assert.fail("Unexpected StopError: " + stopError.getMessage());
        }

        // Evaluate
        Assert.assertEquals(3, consumedConsumerId.size());
        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals(CONSUMER_ID, consumedConsumerId.get(i));
        }

        Assert.assertEquals(3, consumedRecords.size());
        for (int i = 0; i < 3; ++i) {
            Assert.assertEquals("topic-" + i, consumedRecords.get(i).getTopic());
            Assert.assertEquals("shardingKey-" + i, consumedRecords.get(i).getShardingKey());
            Assert.assertEquals(4, consumedRecords.get(i).getHeaders().size());
            Assert.assertEquals("sourceId-" + i, consumedRecords.get(i).getHeaders().get("sourceId"));
            Assert.assertEquals("scope-" + i, consumedRecords.get(i).getHeaders().get("scope"));
            Assert.assertEquals("tenantId-" + i, consumedRecords.get(i).getHeaders().get("tenantId"));
            Assert.assertEquals("zoneId-" + i, consumedRecords.get(i).getHeaders().get("zoneId"));
            Assert.assertEquals(2 * i, consumedRecords.get(i).getPartition());
            Assert.assertEquals(2 * i + 1, consumedRecords.get(i).getOffset());
        }
    }

    @Test
    public final void testRunAndStopAndAgainRunAndStopSuccessful() throws ClientError {
        // Setup
        // Setup mock URLs and ConsumerRecords to be returned
        setUpWireMockStubs(CONSUMER_ID, "topic-1", JSON_CONSUMER_RECORD_0);
        // Setup channel object under test
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getCreatedChannel(channelAuthMock);
        // Setup a counter to tell whether this is the 1st or 2nd time channel runs
        final int[] runStopCount = new int[1];
        // Setup counter arrays to how many times the callback was called in the 1st and 2nd run
        final int[] consumedRecordCount = new int[2];
        final int[] callbackCount = new int[2];
        List<String> consumedConsumerId = new ArrayList<>();
        // Setup callback which receives consumed records
        ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {
            @Override
            public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) throws PermanentError {
                int index = runStopCount[0];
                callbackCount[index]++;
                consumedConsumerId.add(consumerId);
                // keep consumerRecords to compare them at test end
                if (!consumerRecords.getRecords().isEmpty()) {
                    consumedRecordCount[index]++;
                }
                return true;
            }
        };
        // Setup a thread which will call channel.run() in background. Test main thread will call channel.stop() later.
        Runnable channelRun = new Runnable() {
            @Override
            public void run() {
                try {
                    channel.run(consumerRecordCallback, "topic-1");
                } catch (final ClientError e) {
                    Assert.fail("Unexpected ClientError exception: " + e.getMessage());
                }
            }
        };

        // Test
        // call run, then stop, then run again and finally stop
        for (int i = 0; i < 2; ++i) {
            runStopCount[0] = i;
            Thread runThread = new Thread(channelRun);
            runThread.start();
            try {
                // Brief wait to let run consume records
                sleepRunConsumeRecords();
                channel.stop();
            } catch (StopError stopError) {
                Assert.fail("Unexpected StopError: " + stopError.getMessage());
            }
        }

        // Evaluate
        // Verify callback was called in the 1st run
        Assert.assertTrue(callbackCount[0] > 0);
        Assert.assertTrue(consumedRecordCount[0] > 0);
        // Verify callback was called in the 2nd run
        Assert.assertTrue(callbackCount[1] > 0);
        Assert.assertTrue(consumedRecordCount[1] > 0);
        // Verify callback received always the same consumerId
        Assert.assertTrue(consumedConsumerId.size() == callbackCount[0] + callbackCount[1]);
        for (String consumedId : consumedConsumerId) {
            Assert.assertEquals(CONSUMER_ID, consumedId);
        }
    }

    @Test
    public final void testRunExitsUponClientError() throws ClientError {
        // Setup
        // Setup mock URLs and ConsumerRecords to be returned
        setUpWireMockStubs(CONSUMER_ID, "topic-1", JSON_CONSUMER_RECORD_0);
        // Setup channel object under test
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getCreatedChannel(channelAuthMock);
        // Setup callback which receives consumed records. It will be called only once.
        final int[] callbackCounter = new int[1];
        ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {
            @Override
            public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) throws PermanentError {
                callbackCounter[0]++;
                return true;
            }
        };
        // Setup a commit response to throw a TemporaryError
        // set up response to commit request
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + CONSUMER_ID + "/offsets"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withBody("Dummy 409 error message")));

        // Test
        TemporaryError error = null;
        try {
            channel.run(consumerRecordCallback, "topic-1");
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertEquals(1, callbackCounter[0]);
        Assert.assertTrue(error != null);
        Assert.assertEquals(409, error.getStatusCode());
        Assert.assertEquals("Dummy 409 error message: HTTP/1.1 409 Conflict", error.getMessage());
        Assert.assertEquals("run", error.getApi());
    }

    @Test
    public final void testRunRecreatesConsumerUponConsumerError() throws ClientError {
        // Setup
        // Setup mock URLs and ConsumerRecords to be returned
        final String topic = "topic-1";
        setUpWireMockStubs(CONSUMER_ID, topic, JSON_CONSUMER_RECORD_0);
        final String newConsumerId = "recreated-consumer-id";
        // Setup channel object under test
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getCreatedChannel(channelAuthMock);
        // Setup callback which receives consumed records.
        final int[] consumerIdCounter = new int[1];
        List<String> consumerIds = new ArrayList<>();
        List<String> subscriptions = new ArrayList<>();
        List<String> consumedConsumerId = new ArrayList<>();
        ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {
            @Override
            public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) throws ConsumerError {
                consumedConsumerId.add(consumerId);
                consumerIds.add((String) PA.getValue(channel, "consumerId"));
                subscriptions.addAll((List<String>) PA.getValue(channel, "subscriptions"));
                // set up mocked response for the next call to create consumer
                final String newConsumerIdWillBe = newConsumerId + ++consumerIdCounter[0];
                setUpWireMockStubs(newConsumerIdWillBe, topic, "");
                throw new ConsumerError("dummy error to force recreate consumer");
            }
        };
        // Setup a thread which will call channel.run() in background. Test main thread will call channel.stop() later.
        Runnable channelRun = new Runnable() {
            @Override
            public void run() {
                try {
                    channel.run(consumerRecordCallback, topic);
                } catch (final ClientError e) {
                    Assert.fail("Unexpected ClientError exception: " + e.getMessage());
                }
            }
        };
        Thread runThread = new Thread(channelRun);

        // Test
        runThread.start();
        try {
            // Brief wait to let run consume records
            sleepRunConsumeRecords();
            channel.stop();
        } catch (StopError stopError) {
            Assert.fail("Unexpected StopError: " + stopError.getMessage());
        }

        // Evaluate
        // Evaluate all consumerIds obtained
        Assert.assertEquals(CONSUMER_ID, consumedConsumerId.get(0));
        for (int i = 1; i < consumedConsumerId.size(); ++i) {
            Assert.assertEquals(newConsumerId + i, consumedConsumerId.get(i));
        }
        // Evaluate all topic channel was subscribed to
        for (int i = 0; i < subscriptions.size(); ++i) {
            Assert.assertEquals(topic, subscriptions.get(i));
        }
    }

    // Channel stop tests
    @Test
    public final void stopShouldSucceedWhenChannelIsNotExecutingRunMethod() throws ClientError {
        // Setup
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getSubscribedChannel(channelAuthMock);

        // Test
        try {
            channel.stop();
        } catch (final StopError e) {
            Assert.fail("Unexpected StopError: " + e.getMessage());
        }

        // Evaluate
        Assert.assertFalse(((AtomicBoolean) PA.getValue(channel, "running")).get());
        Assert.assertFalse(((AtomicBoolean) PA.getValue(channel, "stopRequested")).get());
    }

    @Test
    public final void stopShouldSucceedWhenChannelIsExecutingRunMethod() throws ClientError {
        // Setup
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getSubscribedChannel(channelAuthMock);
        // Setup simulation that channel is running
        ((AtomicBoolean) PA.getValue(channel, "running")).set(true);
        // Setup a dummy callback to call channel.run()
        ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {
            @Override
            public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) {
                return true;
            }
        };
        // Setup a thread which will call channel.run() in background. Test main thread will call channel.stop() later.
        Runnable channelStop = new Runnable() {
            @Override
            public void run() {
                try {
                    channel.stop();
                } catch (final StopError e) {
                    Assert.fail("Unexpected StopError exception: " + e.getMessage());
                }
            }
        };
        Thread channelStopThread = new Thread(channelStop);
        boolean stopRequestedInitialValue = false;
        boolean stopRequestedAfterCallingStop = false;
        boolean stopRequestedAfterChannelIsNotRunning = false;
        // Setup access to Channel.stopRequested attribute
        final AtomicBoolean channelStopRequested = ((AtomicBoolean) PA.getValue(channel, "stopRequested"));

        // Test
        stopRequestedInitialValue = channelStopRequested.get();
        channelStopThread.start();
        try {
            stopRequestedAfterCallingStop = channelStopRequested.get();
            channel.run(consumerRecordCallback, "topic-1");
            channelStopThread.join();
            stopRequestedAfterChannelIsNotRunning = channelStopRequested.get();
        } catch (final ClientError e) {
            Assert.fail("Unexpected ClientError: " + e.getMessage());
        } catch (final InterruptedException e) {
            Assert.fail("Unexpected InterruptedException: " + e.getMessage());
        }

        // Evaluate stopRequested was briefly true but now it is false
        Assert.assertFalse(stopRequestedInitialValue);
        Assert.assertFalse(stopRequestedAfterCallingStop);
        Assert.assertFalse(stopRequestedAfterChannelIsNotRunning);
    }

    @Test
    public final void testAPIMethodsCanBeExecutedByOneThreadAtTime() throws ClientError {
        // Setup
        final String notProtected = "API is not protected from concurrent access";
        final String topic = "topic-1";
        setUpWireMockStubs(CONSUMER_ID, topic, "");
        // Setup Channel instance to be tested
        ChannelAuth channelAuthMock = Mockito.mock(ChannelAuth.class);
        Mockito.doNothing().when(channelAuthMock).authenticate(Matchers.any());
        Channel channel = getCreatedChannel(channelAuthMock);
        // Setup callback and a thread to execute Channel.run() in background
        ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {
            @Override
            public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) throws ConsumerError {
                return true;
            }
        };
        Runnable channelRun = new Runnable() {
            @Override
            public void run() {
                try {
                    channel.run(consumerRecordCallback, "topic-1");
                } catch (final ClientError e) {
                    Assert.fail("Unexpected ClientError exception: " + e.getMessage());
                }
            }
        };
        Thread runThread = new Thread(channelRun);

        // Test: start thread to execute Channel.run() in background and verify Channel APIs throw error exception
        // when being called from the test main thread
        runThread.start();
        // Wait 50ms to let the runThread start up
        sleep(50);
        // Test: call APIs
        TemporaryError error = null;
        try {
            channel.create();
        } catch (final TemporaryError e) {
            error = e;
        } finally {
            if (error == null) {
                Assert.fail("create " + notProtected);
            }
        }
        error = null;
        try {
            channel.subscribe(Arrays.asList(topic));
        } catch (final TemporaryError e) {
            error = e;
        } finally {
            if (error == null) {
                Assert.fail("subscribe " + notProtected);
            }
        }
        error = null;
        try {
            channel.subscriptions();
        } catch (final TemporaryError e) {
            error = e;
        } finally {
            if (error == null) {
                Assert.fail("subscriptions " + notProtected);
            }
        }
        error = null;
        try {
            channel.consume();
        } catch (final TemporaryError e) {
            error = e;
        } finally {
            if (error == null) {
                Assert.fail("consume " + notProtected);
            }
        }
        error = null;
        try {
            channel.commit();
        } catch (final TemporaryError e) {
            error = e;
        } finally {
            if (error == null) {
                Assert.fail("commit " + notProtected);
            }
        }
        error = null;
        try {
            channel.delete();
        } catch (final TemporaryError e) {
            error = e;
        } finally {
            if (error == null) {
                Assert.fail("delete " + notProtected);
            }
        }
        error = null;
        try {
            channel.run(null, "topic-2", 0);
        } catch (final TemporaryError e) {
            error = e;
        } finally {
            if (error == null) {
                Assert.fail("run " + notProtected);
            }
        }

        // Stop channel before exit
        try {
            channel.stop();
        } catch (final StopError e) {
            Assert.fail("Unexpected StopError: " + e.getMessage());
        }
    }

    // Channel produce tests
    @Test
    public final void testProduceRecordsSuccessfulDueTo204Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/producerPathPrefix");
        // Setup records to produce
        final ProducerRecords producerRecords = TWO_PRODUCER_RECORDS;
        // Setup RESTful service mock
        final String jsonProducerRecords = new Gson().toJson(producerRecords, ProducerRecords.class);
        stubFor(post(urlEqualTo("/producerPathPrefix/produce"))
                .withRequestBody(equalToJson(jsonProducerRecords))
                .willReturn(aResponse()
                        .withStatus(204)));

        // Test
        channel.produce(producerRecords);

        // Evaluate: nothing to evaluate; when successful, produce does not return any data nor throw any exception
    }

    @Test
    public final void testProduceJsonProducerRecordsSuccessfulDueTo204Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/jsonProducerRecords");
        // Setup records to produce
        final String jsonProducerRecords = "{\"records\":["
                + "{\"routingData\":{"
                + "\"topic\":\"my-topic\","
                + "\"shardingKey\":\"123\""
                + "},"
                + "\"message\":{"
                + "\"headers\":{"
                + "\"sourceId\":\"D5452543-E2FB-4585-8BE5-A61C3636819C\""
                + "},"
                + "\"payload\":\"SGVsbG8gT3BlbkRYTCAtIDE=\""
                + "}"
                + "},"
                + "{\"routingData\":{"
                + "\"topic\":\"topic1\","
                + "\"shardingKey\":\"456\""
                + "},"
                + "\"message\":{"
                + "\"headers\":{"
                + "\"sourceId\":\"F567D6A2-500E-4D35-AE15-A707f165D4FA\","
                + "\"anotherHeader\":\"one-two-three-four\""
                + "},"
                + "\"payload\":\"SGVsbG8gT3BlbkRYTCAtIDI=\""
                + "}"
                + "}"
                + "]}";
        // Setup RESTful service mock
        stubFor(post(urlEqualTo("/jsonProducerRecords/produce"))
                .withRequestBody(equalToJson(jsonProducerRecords))
                .willReturn(aResponse()
                        .withStatus(204)));

        // Test
        channel.produce(jsonProducerRecords);

        // Evaluate: nothing to evaluate; when successful, produce does not return any data nor throw any exception
    }

    @Test
    public final void testProduceMalFormedJsonFailsWithPermanentErrorDueTo400Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/malformedJsonProducerRecords");
        // Setup records to produce
        final String malformedJsonProducerRecords = "{\"records\":"
                + "["
                + "{"
                + "\"routingData\":{"
                + "\"topic\":\"my-topic\","
                + "\"shardingKey\":\"123\""
                + "},";
        // Setup RESTful service mock - it answers 400 upon receiving a malformed JSON request
        stubFor(post(urlEqualTo("/malformedJsonProducerRecords/produce"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withStatusMessage("Malformed JSON received")
                        .withBody("Dummy 400 error message")));
        // Setup expected exception
        ClientError error = null;

        // Test
        try {
            channel.produce(malformedJsonProducerRecords);
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(400, error.getStatusCode());
        Assert.assertEquals("Dummy 400 error message: HTTP/1.1 400 Malformed JSON received",
                error.getMessage());
        Assert.assertEquals("produce", error.getApi());
    }

    @Test
    public final void testProduceFailsWithTemporaryErrorDueTo401Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/unauthorized");
        // Setup RESTful service mock - it answers 401 regardless of received data
        stubFor(post(urlEqualTo("/unauthorized/produce"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withStatusMessage("Unauthorized")
                        .withBody("Dummy 401 error message")));
        // Setup expected exception
        ClientError error = null;

        // Test
        try {
            channel.produce(TWO_PRODUCER_RECORDS);
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(401, error.getStatusCode());
        Assert.assertEquals("Dummy 401 error message: HTTP/1.1 401 Unauthorized", error.getMessage());
        Assert.assertEquals("produce", error.getApi());
    }

    @Test
    public final void testProduceFailsWithTemporaryErrorDueTo403Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/forbidden");
        // Setup RESTful service mock - it answers 403 regardless of received data
        stubFor(post(urlEqualTo("/forbidden/produce"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withStatusMessage("Forbidden")
                        .withBody("Dummy 403 error message")));
        // Setup expected exception
        ClientError error = null;

        // Test
        try {
            channel.produce(TWO_PRODUCER_RECORDS);
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(403, error.getStatusCode());
        Assert.assertEquals("Dummy 403 error message: HTTP/1.1 403 Forbidden", error.getMessage());
        Assert.assertEquals("produce", error.getApi());
    }

    @Test
    public final void testProduceFailsWithPermanentErrorDueTo404Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/not-found");
        // Setup RESTful service mock - it answers 404 regardless of received data
        stubFor(post(urlEqualTo("/not-found/produce"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withStatusMessage("Not Found")
                        .withBody("Dummy 404 error message")));
        // Setup expected exception
        ClientError error = null;

        // Test
        try {
            channel.produce(TWO_PRODUCER_RECORDS);
        } catch (final PermanentError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(404, error.getStatusCode());
        Assert.assertEquals("Dummy 404 error message: HTTP/1.1 404 Not Found", error.getMessage());
        Assert.assertEquals("produce", error.getApi());
    }

    @Test
    public final void testProduceFailsWithTemporaryErrorDueTo409Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/conflict");
        // Setup RESTful service mock - it answers 409 regardless of received data
        stubFor(post(urlEqualTo("/conflict/produce"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withStatusMessage("Conflict")
                        .withBody("Dummy 409 error message")));
        // Setup expected exception
        ClientError error = null;

        // Test
        try {
            channel.produce(TWO_PRODUCER_RECORDS);
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(409, error.getStatusCode());
        Assert.assertEquals("Dummy 409 error message: HTTP/1.1 409 Conflict", error.getMessage());
        Assert.assertEquals("produce", error.getApi());
    }

    @Test
    public final void testProduceFailsWithTemporaryErrorDueTo500Response() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/internal-server-error");
        // Setup RESTful service mock - it answers 500 regardless of received data
        stubFor(post(urlEqualTo("/internal-server-error/produce"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withStatusMessage("Internal Server Error")
                        .withBody("Dummy 500 error message")));
        // Setup expected exception
        ClientError error = null;

        // Test
        try {
            channel.produce(TWO_PRODUCER_RECORDS);
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(500, error.getStatusCode());
        Assert.assertEquals("Dummy 500 error message: HTTP/1.1 500 Internal Server Error", error.getMessage());
        Assert.assertEquals("produce", error.getApi());
    }

    @Test
    public final void testProduceFailsWithTemporaryErrorDueToUnmapped405ErrorResponse() throws ClientError {
        // Setup
        final Channel channel = getProducerChannel("/internal-server-error");
        // Setup RESTful service mock - it answers 405 regardless of received data
        stubFor(post(urlEqualTo("/internal-server-error/produce"))
                .willReturn(aResponse()
                        .withStatus(405)
                        .withStatusMessage("Method Not Allowed")
                        .withBody("Dummy 405 error message")));
        // Setup expected exception
        TemporaryError error = null;

        // Test
        try {
            channel.produce(TWO_PRODUCER_RECORDS);
        } catch (final TemporaryError e) {
            error = e;
        }

        // Evaluate
        Assert.assertTrue(error != null);
        Assert.assertEquals(405, error.getStatusCode());
        Assert.assertEquals("Unexpected temporary error: HTTP/1.1 405 Method Not Allowed", error.getMessage());
        Assert.assertEquals("produce", error.getApi());
        Assert.assertEquals("POST http://localhost:8080/internal-server-error/produce HTTP/1.1",
                error.getHttpRequest().toString());
    }

    //---------------------------------------------------------------
    // Helper Methods
    void setUpWireMockStubs(final String consumerId, final String topic, final String jsonConsumerRecord) {
        setUpWireMockStubs(consumerId, topic, jsonConsumerRecord, 0);
    }

    void setUpWireMockStubs(final String consumerId, final String topic, final String jsonConsumerRecord,
                            final int timeoutMs) {
        // set up response to create
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"consumerInstanceId\":\"" + consumerId + "\"}")));

        // set up response to subscription
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/subscription"))
                .withRequestBody(equalToJson("{\"topics\":[\"" + topic + "\"]}"))
                .willReturn(aResponse()
                        .withStatus(204)));

        // set up response to get subscriptions
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/subscription"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"" + topic + "\"]")));

        // set up response to consume records
        if (timeoutMs > 0) {
            stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/records?timeout="
                    + Integer.toString(timeoutMs)))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"records\":[" + jsonConsumerRecord + "]}")));
        } else {
            stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/records"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"records\":[" + jsonConsumerRecord + "]}")));
        }

        // set up response to commit offsets
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/offsets"))
                .willReturn(aResponse()
                        .withStatus(204)));

        // set up response to delete channel
        stubFor(delete(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId))
                .willReturn(aResponse()
                        .withStatus(204)));
    }

    private Channel setupChannelCreatesConsumerTests() throws PermanentError, TemporaryError {
        Properties extraConfigs = new Properties();
        extraConfigs.put("enable.auto.commit", false);
        extraConfigs.put("auto.commit.interval.ms", 0);
        extraConfigs.put("auto.offset.reset", "earliest");
        extraConfigs.put("request.timeout.ms", 16000);
        extraConfigs.put("session.timeout.ms", 15000);

        Channel channel = new Channel("http://localhost:8080",
                new ChannelAuthToken("myToken"),
                "cg1",
                null,
                null,
                true,
                null,
                extraConfigs,
                null);

        return channel;
    }

    private Channel getCreatedChannel() throws PermanentError, TemporaryError {
        return getCreatedChannel(new ChannelAuthToken("myToken"));
    }

    private Channel getCreatedChannel(final ChannelAuth channelAuth) throws PermanentError, TemporaryError {
        Channel channel = new Channel("http://localhost:8080",
                channelAuth,
                "cg1",
                null,
                null,
                true,
                null,
                null,
                null);

        PA.setValue(channel, "consumerId", CONSUMER_ID);

        return channel;
    }

    private Channel getSubscribedChannel() throws PermanentError, TemporaryError {
        return getSubscribedChannel(new ChannelAuthToken("myToken"));
    }

    private Channel getSubscribedChannel(final ChannelAuth channelAuth) throws PermanentError, TemporaryError {
        Channel channel = new Channel("http://localhost:8080",
                channelAuth,
                "cg1",
                null,
                null,
                true,
                null,
                null,
                null);

        PA.setValue(channel, "consumerId", CONSUMER_ID);
        PA.setValue(channel, "subscriptions", new ArrayList<>(Arrays.asList("topic1")));

        return channel;
    }

    private static final String JSON_CONSUMER_RECORD_0 = "{"
            + "\"routingData\":{\"topic\":\"topic-0\",\"shardingKey\":\"shardingKey-0\"},"
            + "\"message\":"
            + "{"
            + "\"headers\":"
            + "{\"sourceId\":\"sourceId-0\",\"scope\":\"scope-0\",\"tenantId\":\"tenantId-0\",\"zoneId\":\"zoneId-0\"},"
            + "\"payload\":\"UGF5bG9hZCAjMQ==\""
            + "},"
            + "\"partition\":0,\"offset\":1"
            + "}";
    private static final String JSON_CONSUMER_RECORD_1 = "{"
            + "\"routingData\":{\"topic\":\"topic-1\",\"shardingKey\":\"shardingKey-1\"},"
            + "\"message\":"
            + "{\"headers\":"
            + "{\"sourceId\":\"sourceId-1\",\"scope\":\"scope-1\",\"tenantId\":\"tenantId-1\",\"zoneId\":\"zoneId-1\"},"
            + "\"payload\":\"UGF5bG9hZCAjMg==\""
            + "},"
            + "\"partition\":2,\"offset\":3"
            + "}";
    private static final String JSON_CONSUMER_RECORD_2 = "{"
            + "\"routingData\":{\"topic\":\"topic-2\",\"shardingKey\":\"shardingKey-2\"},"
            + "\"message\":"
            + "{"
            + "\"headers\":"
            + "{\"sourceId\":\"sourceId-2\",\"scope\":\"scope-2\",\"tenantId\":\"tenantId-2\",\"zoneId\":\"zoneId-2\"},"
            + "\"payload\":\"UGF5bG9hZCAjMw==\""
            + "},"
            + "\"partition\":4,\"offset\":5"
            + "}";

    private void sleepRunConsumeRecords() {
        sleep(500);
    }

    private void sleep(final int timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (final Exception e) {
            // ignore it
        }
    }

    private static Channel getProducerChannel(final String producerPathPrefix) throws TemporaryError {
        final Channel channel = new Channel("http://localhost:8080",
                new ChannelAuthToken("myToken"),
                null,
                null,
                null,
                producerPathPrefix,
                true,
                null,
                null,
                null);

        return channel;
    }

    private static final ProducerRecords TWO_PRODUCER_RECORDS = new ProducerRecords();
    static {
        TWO_PRODUCER_RECORDS.add(
                new ProducerRecords.ProducerRecord
                        .Builder("my-topic",
                        "Hello OpenDXL - 1")
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                        }})
                        .withShardingKey("123")
                        .build()
        );
        TWO_PRODUCER_RECORDS.add(
                new ProducerRecords.ProducerRecord
                        .Builder("topic1",
                        "Hello OpenDXL - 2")
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "F567D6A2-500E-4D35-AE15-A707f165D4FA");
                            put("anotherHeader", "one-two-three-four");
                        }})
                        .withShardingKey("456")
                        .build()
        );
    }
}
