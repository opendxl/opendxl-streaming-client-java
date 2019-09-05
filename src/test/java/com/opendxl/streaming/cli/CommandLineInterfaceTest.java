/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.cli.entity.ExecutionResult;
import com.opendxl.streaming.cli.entity.StickinessCookie;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import com.opendxl.streaming.client.entity.ProducerRecords;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import joptsimple.OptionSet;
import junit.extensions.PA;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.util.HashMap;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static junit.framework.TestCase.assertTrue;


public class CommandLineInterfaceTest {
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Test
    public void shouldLoginSuccessfully() throws Exception {
        // Setup
        // Setup a mock http response to login request
        stubFor(get(urlEqualTo("/identity/v1/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"authorizationToken\":\"MY_AUTHORIZATION_TOKEN\"}")));
        // Setup CLI parameters
        String args = "--operation login "
                + "--auth-url http://localhost:8080/identity/v1/login "
                + "--user myUsername "
                + "--password myPassword "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate
        Assert.assertEquals("200", executionResult.getCode());
        Assert.assertEquals("MY_AUTHORIZATION_TOKEN", executionResult.getResult());
    }

    @Test
    public void shouldLoginFail() throws Exception {
        // Setup
        // Setup expected value when CommandLineInterface calls System.exit()
        exit.expectSystemExitWithStatus(1);
        // Setup a mock http response to login request
        stubFor(get(urlEqualTo("/identity/v1/login"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Dummy 401 error message")));
        // Setup CLI parameters
        String args = "--operation login "
                + "--auth-url http://localhost:8080/identity/v1/login "
                + "--user myUsername "
                + "--password myPassword "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        cli.execute();
    }


    @Test
    public void shouldFailWhenThereisNoOptions() {
        exit.expectSystemExit();
        String[] args = new String[0];
        CommandLineInterface.main(args);
    }

    @Test
    public void shouldFailWhenOperationOptionIsMissing() {
        exit.expectSystemExit();
        String args = "--brokers localhost:9092";
        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenOperationIsUnknown() {
        exit.expectSystemExit();
        String args = "--operation unknown";
        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldFailWhenOperationArgumentIsMissing() {
        exit.expectSystemExit();
        String args = "--operation";
        CommandLineInterface.main(args.split(" "));
    }

    // --create Operation

    @Test
    public void shouldCreateAConsumer() throws Exception {
        // Setup
        // Setup a mock http response to create request
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Set-Cookie", "AWSALB=my-cookie-value; Path=/my-path; HttpOnly")
                        .withBody("{\"consumerInstanceId\":\"" + CONSUMER_ID + "\"}")));
        // Setup CLI parameters
        String args = "--operation create "
                + "--url http://localhost:8080/databus/consumer-service/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--cg cg1 "
                + "--config max.message.size=1000,min.message.size=200,auto.offset.reset=latest,"
                + "session.timeout.ms=60000,request.timeout.ms=61000 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate
        Assert.assertEquals("200", executionResult.getCode());
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        final String executionResultString = gson.toJson(executionResult);
        Assert.assertTrue(executionResultString.contains("\"consumerId\":\"" + CONSUMER_ID + "\""));
        Assert.assertTrue(executionResultString.contains("\"cookie\":{\"value\":\"my-cookie-value\","
                + "\"domain\":\"localhost\"}"));
    }

    @Test
    public void shouldFailCreateAConsumer() {
        // Setup
        // Setup EVALUATION OF OUTPUT VALUES when CommandLineInterface calls System.exit()
        exit.expectSystemExitWithStatus(1);
        // Setup a mock http response to create request
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Dummy 401 error message")));
        // Setup CLI parameters
        String args = "--operation create "
                + "--url http://localhost:8080/databus/consumer-service/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--cg cg1 "
                + "--config max.message.size=1000,min.message.size=200,auto.offset.reset=anything,"
                + "session.timeout.ms=60000,request.timeout.ms=61000 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCreateOperationOptionHasNotAdditionalOptions() {
        exit.expectSystemExit();
        String args = "--operation create";
        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCreateOperationHasNotURLOption() {
        exit.expectSystemExit();
        final String args = "--operation create "
                + "--token myToken "
                + "--cg cg8 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--config auto.offset.reset=earliest,session.timeout.ms=30000,request.timeout.ms=31000 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCreateOperationHasNotTokenOption() {
        exit.expectSystemExit();
        final String args = "--operation create "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--config auto.offset.reset=earliest,session.timeout.ms=30000,request.timeout.ms=31000 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCreateOperationHasConsumerGroupOption() {
        exit.expectSystemExit();
        final String args = "--operation create "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--cg cg8 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--config auto.offset.reset=earliest,session.timeout.ms=30000,request.timeout.ms=31000 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCreateOperationHasCertRetryOption() {
        exit.expectSystemExit();
        String args = "--operation create "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cg cg1 "
                + "--config max.message.size=1000,min.message.size=200 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--config auto.offset.reset=latest,session.timeout.ms=60000,request.timeout.ms=61000 ";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldSetDefaultOptionsValuesForCreateOperation() {
        String args = "--operation create "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cg cg1 "
                + "--verify-cert-bundle 1234";

        final CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        final OptionSet options = (OptionSet) PA.getValue(cli, "options");
        assertTrue(options.valueOf("retry").equals("true"));
        assertTrue(options.valueOf("consumer-prefix").equals("/databus/consumer-service/v1"));
        assertTrue(options.valueOf("http-proxy").equals(""));

    }

    @Test
    public void shouldFailWhenCreateOperationHasInsufficientHttpProxyParameters() {
        exit.expectSystemExit();
        final String args = "--operation create "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cg cg8 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--config auto.offset.reset=earliest,session.timeout.ms=30000,request.timeout.ms=31000 "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy true";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCreateOperationHasEmptyUrlInHttpProxyParameters() {
        exit.expectSystemExit();
        final String args = "--operation create "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cg cg8 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--config auto.offset.reset=earliest,session.timeout.ms=30000,request.timeout.ms=31000 "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy true,,8080";

        CommandLineInterface.main(args.split(" "));
    }

    // --subscribe Operation

    @Test
    public void shouldFailWhenSubscribeOperationHasNotURLOption() {
        exit.expectSystemExit();
        String args = "--operation subscribe "
                + "--token myToken "
                + "--consumer-id  1341234 "
                + "--cookie  12341234 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234 "
                + "--topic topic3";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenSubscribeOperationHasNotTokenOption() {
        exit.expectSystemExit();
        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--consumer-id 1234234 "
                + "--cookie 2345245 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234 "
                + "--topic topic3";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenSubscribeOperationHasNotConsumerIdOption() {
        exit.expectSystemExit();

        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cookie 2342314 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234 "
                + "--topic topic3";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenSubscribeOperationHasNotCookieOption() {
        exit.expectSystemExit();

        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 2134234 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234 "
                + "--topic topic3";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldFailWhenSubscribeOperationHasNotVerifyCertOption() {
        exit.expectSystemExit();

        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 12341234 "
                + "--cookie 213415 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--topic topic3";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldFailWhenSubscribeOperationHasNotTopicOption() {
        exit.expectSystemExit();

        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 1234134 "
                + "--cookie 1234123 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234 ";
        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldSetDefaultOptionsValuesForSubscribeOperation() {

        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 143134 "
                + "--cookie 341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--topic topic3";

        final CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        final OptionSet options = (OptionSet) PA.getValue(cli, "options");
        assertTrue(options.valueOf("consumer-prefix").equals("/databus/consumer-service/v1"));
        assertTrue(options.valueOf("http-proxy").equals(""));

    }

    @Test
    public void shouldFailWhenSubscribeOperationHasInsufficientHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 143134 "
                + "--cookie 341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--topic topic3 "
                + "--http-proxy true";
        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenSubscribeOperationHasInvalidPortInHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation subscribe "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 143134 "
                + "--cookie 341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--topic topic3 "
                + "--http-proxy true,localhost,-1";
        CommandLineInterface.main(args.split(" "));
    }

    // --login Operation

    @Test
    public void shouldFailWhenLoginOperationAndHasNotAuthURL() {
        exit.expectSystemExit();
        String args = "--operation login "
                + "--user me "
                + "--password secret "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenLoginOperationAndHasNotUser() {
        exit.expectSystemExit();
        String args = "--operation login --auth-url https://localhost:8080/v1/login "
                + "--password password1 --verify-cert-bundle 1234";
        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenLoginOperationAndHasNotPassword() {
        exit.expectSystemExit();
        String args = "--operation login --auth-url https://localhost:8080/v1/login "
                + "--user user1 --verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenLoginOperationAndHasNotVerifyCertBundle() {
        exit.expectSystemExit();
        String args = "--operation login --auth-url https://localhost:8080/v1/login "
                + "--user user1 --password password1 ";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenLoginOperationHasInsufficientHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation login "
                + "--auth-url https://localhost:8080/v1/login "
                + "--user me "
                + "--password secret "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy false";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenLoginOperationHasEmptyUrlInHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation login "
                + "--auth-url https://localhost:8080/v1/login "
                + "--user me "
                + "--password secret "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy false,,8080";

        CommandLineInterface.main(args.split(" "));
    }

    // --consume Operation

    @Test
    public void shouldFailWhenConsumeOperationHasNotURLOption() {
        exit.expectSystemExit();

        String args = "--operation consume "
                + "--token myToken "
                + "--consumer-id 2341234 "
                + "--cookie 1234214 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenConsumeOperationHasNotTokenOption() {
        exit.expectSystemExit();

        String args = "--operation consume "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--consumer-id 23421453 "
                + "--cookie 3245234 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenConsumeOperationHasNotConsumerIdOption() {
        exit.expectSystemExit();

        String args = "--operation consume "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cookie 2142145 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenConsumeOperationHasNotCookieOption() {
        exit.expectSystemExit();

        String args = "--operation consume "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 121251241 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldFailWhenConsumeOperationHasNotVerifyCertOption() {
        exit.expectSystemExit();

        String args = "--operation consume "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 12321451 "
                + "--cookie 1231234 "
                + "--consumer-prefix /databus/consumer-service/v1 ";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldSetDefaultOptionsValuesForConsumeOperation() {

        String args = "--operation consume "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234";

        final CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        final OptionSet options = (OptionSet) PA.getValue(cli, "options");
        assertTrue(options.valueOf("consumer-prefix").equals("/databus/consumer-service/v1"));
        assertTrue(options.valueOf("http-proxy").equals(""));
        assertTrue(options.valueOf("consume-timeout").equals(""));

    }

    @Test
    public void shouldFailWhenConsumeOperationHasInsufficientHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation consume "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy invalidLogicValue";
        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenConsumeOperationHasEmptyUrlInHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation consume "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy true,,8080";
        CommandLineInterface.main(args.split(" "));
    }

    // --commit Operation

    @Test
    public void shouldFailWhenCommitOperationHasNotURLOption() {
        exit.expectSystemExit();

        String args = "--operation commit "
                + "--token myToken "
                + "--consumer-id 2341234 "
                + "--cookie 1234214 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCommitOperationHasNotTokenOption() {
        exit.expectSystemExit();

        String args = "--operation commit "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--consumer-id 23421453 "
                + "--cookie 3245234 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCommitOperationHasNotConsumerIdOption() {
        exit.expectSystemExit();

        String args = "--operation commit "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cookie 2142145 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCommitOperationHasNotCookieOption() {
        exit.expectSystemExit();

        String args = "--operation commit "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 121251241 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldFailWhenCommitOperationHasNotVerifyCertOption() {
        exit.expectSystemExit();

        String args = "--operation commit "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 12321451 "
                + "--cookie 1231234 "
                + "--consumer-prefix /databus/consumer-service/v1 ";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldSetDefaultOptionsValuesForCommitOperation() {

        String args = "--operation commit "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234";

        final CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        final OptionSet options = (OptionSet) PA.getValue(cli, "options");
        assertTrue(options.valueOf("consumer-prefix").equals("/databus/consumer-service/v1"));
        assertTrue(options.valueOf("http-proxy").equals(""));

    }

    @Test
    public void shouldFailWhenCommitOperationHasInsufficientHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation commit "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy fAlSe";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenCommitOperationHasInvalidPortInHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation commit "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy FaLsE,localhost,-1";

        CommandLineInterface.main(args.split(" "));
    }

    // --delete Operation

    @Test
    public void shouldFailWhenDeleteOperationHasNotURLOption() {
        exit.expectSystemExit();

        String args = "--operation delete "
                + "--token myToken "
                + "--consumer-id 2341234 "
                + "--cookie 1234214 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenDeleteOperationHasNotTokenOption() {
        exit.expectSystemExit();

        String args = "--operation delete "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--consumer-id 23421453 "
                + "--cookie 3245234 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenDeleteOperationHasNotConsumerIdOption() {
        exit.expectSystemExit();

        String args = "--operation delete "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cookie 2142145 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenDeleteOperationHasNotCookieOption() {
        exit.expectSystemExit();

        String args = "--operation delete "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 121251241 "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldFailWhenDeleteOperationHasNotVerifyCertOption() {
        exit.expectSystemExit();

        String args = "--operation delete "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 12321451 "
                + "--cookie 1231234 "
                + "--consumer-prefix /databus/consumer-service/v1 ";

        CommandLineInterface.main(args.split(" "));
    }


    @Test
    public void shouldSetDefaultOptionsValuesForDeleteOperation() {

        String args = "--operation delete "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234";

        final CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        final OptionSet options = (OptionSet) PA.getValue(cli, "options");
        assertTrue(options.valueOf("consumer-prefix").equals("/databus/consumer-service/v1"));
        assertTrue(options.valueOf("http-proxy").equals(""));

    }

    @Test
    public void shouldFailWhenDeleteOperationHasInsufficientHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation delete "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy alfa,bravo";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenDeleteOperationHasInvalidPortInHttpProxyParameters() {
        exit.expectSystemExit();

        String args = "--operation delete "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--http-proxy alfa,bravo,charlie";
        CommandLineInterface.main(args.split(" "));
    }

    // combined test to successfully call create, subscribe, consume, commit and delete
    @Test
    public void shouldSuccessfullySubscribeConsumeCommitAndDelete() {
        // Setup
        // Setup http stubs
        final String myCookieValue = "my-cookie-value";
        final String topic = "topic3";
        // Setup gson parser to deserialize ExecutionResult objects
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        setUpWireMockStubs(CONSUMER_ID, myCookieValue, topic, JSON_CONSUMER_RECORD, 100);

        // Setup create
        final String argsForCreate = "--operation create "
                + "--url http://localhost:8080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--cg cg16 "
                + "--retry true "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--config auto.offset.reset=earliest,session.timeout.ms=30000,request.timeout.ms=31000 "
                + "--verify-cert-bundle 1234";

        // Test create
        CommandLineInterface cli = new CommandLineInterface(argsForCreate.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate create
        Object result = executionResult.getResult();
        final String consumerId = (String) PA.getValue(result, "consumerId");
        Assert.assertEquals(CONSUMER_ID, consumerId);
        final StickinessCookie stickinessCookie = (StickinessCookie) PA.getValue(result, "cookie");
        Assert.assertEquals(myCookieValue, stickinessCookie.getValue());
        Assert.assertEquals("localhost", stickinessCookie.getDomain());

        // Setup subscribe
        String argsForSubscribe = "--operation subscribe "
                + "--url http://localhost:8080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id " + CONSUMER_ID + " "
                + "--cookie " + stickinessCookie.getValue() + " "
                + "--domain " + stickinessCookie.getDomain() + " "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--topic " + topic + " "
                + "--verify-cert-bundle 1234";

        // Test subscribe
        cli = new CommandLineInterface(argsForSubscribe.split(" "));
        executionResult = cli.execute();

        // Evaluate subscribe
        Assert.assertEquals("204", executionResult.getCode());

        // Setup consume and commit
        String argsForConsume = "--operation consume "
                + "--url http://localhost:8080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id " + consumerId + " "
                + "--cookie " + stickinessCookie.getValue() + " "
                + "--domain " + stickinessCookie.getDomain() + " "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234 "
                + "--consume-timeout 100";
        CommandLineInterface cliForConsume = new CommandLineInterface(argsForConsume.split(" "));

        String argsForCommit = "--operation commit "
                + "--url http://localhost:8080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id " + consumerId + " "
                + "--cookie " + stickinessCookie.getValue() + " "
                + "--domain " + stickinessCookie.getDomain() + " "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";
        CommandLineInterface cliForCommit = new CommandLineInterface(argsForCommit.split(" "));

        // Test consume and commit three times
        for (int i = 0; i < 3; ++i) {
            executionResult = cliForConsume.execute();
            Assert.assertEquals("200", executionResult.getCode());
            List<ConsumerRecords.ConsumerRecord> consumerRecords =
                    (List<ConsumerRecords.ConsumerRecord>) executionResult.getResult();
            Assert.assertEquals(1, consumerRecords.size());
            Assert.assertEquals("UGF5bG9hZCAjMQ==", consumerRecords.get(0).getPayload());
            Assert.assertEquals("topic-0", consumerRecords.get(0).getTopic());
            Assert.assertEquals("shardingKey-0", consumerRecords.get(0).getShardingKey());
            Assert.assertEquals(4, consumerRecords.get(0).getHeaders().size());
            Assert.assertEquals("sourceId-0", consumerRecords.get(0).getHeaders().get("sourceId"));
            Assert.assertEquals("scope-0", consumerRecords.get(0).getHeaders().get("scope"));
            Assert.assertEquals("tenantId-0", consumerRecords.get(0).getHeaders().get("tenantId"));
            Assert.assertEquals("zoneId-0", consumerRecords.get(0).getHeaders().get("zoneId"));
            Assert.assertEquals(0, consumerRecords.get(0).getPartition());
            Assert.assertEquals(1, consumerRecords.get(0).getOffset());

            executionResult = cliForCommit.execute();
            Assert.assertEquals("204", executionResult.getCode());
        }

        // Setup delete
        String argsForDelete = "--operation delete "
                + "--url http://localhost:8080/databus/consumer-service/v1 "
                + "--token myToken "
                + "--consumer-id " + consumerId + " "
                + "--cookie " + stickinessCookie.getValue() + " "
                + "--domain " + stickinessCookie.getDomain() + " "
                + "--consumer-prefix /databus/consumer-service/v1 "
                + "--verify-cert-bundle 1234";
        CommandLineInterface cliForDelete = new CommandLineInterface(argsForDelete.split(" "));

        // Test delete
        executionResult = cliForDelete.execute();

        // Evaluate delete
        Assert.assertEquals("204", executionResult.getCode());

    }

    // --produce Operation
    @Test
    public void shouldProduceRecords() throws Exception {
        // Setup
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        // Setup a mock http response to produce request
        stubFor(post(urlEqualTo("/databus/cloudproxy/v1/produce"))
                .withRequestBody(equalToJson(gson.toJson(TWO_PRODUCER_RECORDS, ProducerRecords.class)))
                .willReturn(aResponse()
                        .withStatus(204)));
        // Setup CLI parameters
        String args = "--operation produce "
                + "--url http://localhost:8080/databus/cloudproxy/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--records " + TWO_SIMPLIFIED_PRODUCER_RECORDS + " "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate
        Assert.assertEquals("204", executionResult.getCode());
    }

    @Test
    public void shouldProduceRecordsWithJustTopicAndPayloadAttributes() throws Exception {
        // Setup
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        // Setup a mock http response to produce request
        stubFor(post(urlEqualTo("/databus/cloudproxy/v1/produce"))
                .withRequestBody(equalToJson("{\"records\":[{"
                        + "\"routingData\":{\"topic\":\"my-topic\"},"
                        + "\"message\":{\"payload\":\"SGVsbG8tT3BlbkRYTC0x\"}"
                        + "}]}"))
                .willReturn(aResponse()
                        .withStatus(204)));
        // Setup CLI parameters
        String args = "--operation produce "
                + "--url http://localhost:8080/databus/cloudproxy/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--records [{\"topic\":\"my-topic\",\"payload\":\"Hello-OpenDXL-1\"}] "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate
        Assert.assertEquals("204", executionResult.getCode());
    }

    @Test
    public void shouldProduceRecordsIgnoringExtraUnknownAttributes() throws Exception {
        // Setup
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        // Setup a mock http response to produce request
        stubFor(post(urlEqualTo("/databus/cloudproxy/v1/produce"))
                .withRequestBody(equalToJson("{\"records\":[{"
                        + "\"routingData\":{\"topic\":\"my-topic\"},"
                        + "\"message\":{\"payload\":\"SGVsbG8tT3BlbkRYTC0x\"}"
                        + "}]}"))
                .willReturn(aResponse()
                        .withStatus(204)));
        // Setup CLI parameters
        String args = "--operation produce "
                + "--url http://localhost:8080/databus/cloudproxy/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--records [{\"topic\":\"my-topic\",\"payload\":\"Hello-OpenDXL-1\""
                // extra unknown attributes
                + ",\"test1\":\"value1\",\"test2\":{\"test21\":\"value21\"}}] "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate
        Assert.assertEquals("204", executionResult.getCode());
    }

    @Test
    public void shouldProduceFailWhenRecordsTopElementIsNotAnArray() throws Exception {
        // This test case evaluates the case when "--record" parameter value is not a JSON array of
        // SimplifiedProducerRecords. Since {"records":[{"topic":"my-topic"}]} top element is not an array but an
        // object, CliUtils.getProducerRecords() throw a JSON exception while parsing it:
        // ERROR: java.lang.IllegalStateException: Expected BEGIN_ARRAY but was BEGIN_OBJECT at line 1 column 2 path
        // CLI catches this exception and it exits with error.
        exit.expectSystemExitWithStatus(1);

        // Setup
        // Setup CLI parameters
        String args = "--operation produce "
                + "--url http://localhost:8080/databus/cloudproxy/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--records {\"records\":[{\"topic\":\"my-topic\"}]} "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate
        Assert.assertEquals("204", executionResult.getCode());
    }

    @Test
    public void shouldProduceFailWhenRecordsParameterAreIncompleteRecords() throws Exception {
        // in this test case, record is sent to stub, stub checks received JSON is the expected one and stub answers
        // 400 error. Streaming client will throw a PermanentError exception upon reception of 400 error. CLI catches
        // PermanentError and exits with error.
        exit.expectSystemExitWithStatus(1);

        // Setup
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        // Setup a mock http response to produce request
        stubFor(post(urlEqualTo("/databus/cloudproxy/v1/produce"))
                .withRequestBody(equalToJson("{\"records\":["
                        + "{"
                        + "\"routingData\":{\"topic\":\"my-topic\"},"
                        + "\"message\":{\"payload\":\"\"}"
                        + "}]}"))
                .willReturn(aResponse()
                        .withStatus(400)));
        // Setup CLI parameters
        String args = "--operation produce "
                + "--url http://localhost:8080/databus/cloudproxy/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--records [{\"topic\":\"my-topic\"}] "
                + "--verify-cert-bundle 1234";

        // Test
        CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        ExecutionResult executionResult = cli.execute();

        // Evaluate
        // Nothing to evaluate: test is expected to cause Streaming Client to throw a PermanentError which is caught by
        // the CLI which in turns exits with error.
    }

    @Test
    public void shouldFailWhenProduceOperationHasNotRecordsOption() {
        exit.expectSystemExitWithStatus(1);

        String args = "--operation produce "
                + "--url http://localhost:8080/databus/cloudproxy/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenProduceOperationHasNotURLOption() {
        exit.expectSystemExitWithStatus(1);

        String args = "--operation produce "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--records " + TWO_SIMPLIFIED_PRODUCER_RECORDS + " "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenProduceOperationHasNotTokenOption() {
        exit.expectSystemExitWithStatus(1);

        String args = "--operation produce "
                + "--url http://localhost:8080/databus/cloudproxy/v1 "
                + "--producer-prefix /databus/cloudproxy/v1 "
                + "--records " + TWO_SIMPLIFIED_PRODUCER_RECORDS + " "
                + "--verify-cert-bundle 1234";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldSetDefaultOptionsValuesForProduceOperation() {

        String args = "--operation produce "
                + "--url http://127.0.0.1:50080/ "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--records " + TWO_SIMPLIFIED_PRODUCER_RECORDS;

        final CommandLineInterface cli = new CommandLineInterface(args.split(" "));
        final OptionSet options = (OptionSet) PA.getValue(cli, "options");
        assertTrue(options.valueOf("producer-prefix").equals("/databus/cloudproxy/v1"));
        assertTrue(options.valueOf("http-proxy").equals(""));
    }

    @Test
    public void shouldFailWhenProduceOperationHasInsufficientHttpProxyParameters() {
        exit.expectSystemExitWithStatus(1);

        String args = "--operation produce "
                + "--url http://127.0.0.1:50080/databus/cloudproxy/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--verify-cert-bundle 1234 "
                + "--records " + TWO_SIMPLIFIED_PRODUCER_RECORDS + " "
                + "--http-proxy alfa,bravo";

        CommandLineInterface.main(args.split(" "));
    }

    @Test
    public void shouldFailWhenProduceOperationHasInvalidPortInHttpProxyParameters() {
        exit.expectSystemExitWithStatus(1);

        String args = "--operation produce "
                + "--url http://127.0.0.1:50080/databus/consumer-service/v1 "
                + "--token MY_AUTHORIZATION_TOKEN "
                + "--consumer-id 132413 "
                + "--cookie 12341234 "
                + "--domain my-domain "
                + "--verify-cert-bundle 1234 "
                + "--records " + TWO_SIMPLIFIED_PRODUCER_RECORDS + " "
                + "--http-proxy alfa,bravo,charlie";
        CommandLineInterface.main(args.split(" "));
    }


    // ------------------------------------------------------------

    private static final String CONSUMER_ID =
            "c4b60c6e-931e-496c-97c6-86c2935a353196fa80a1-f911-47ee-9a35-fc40a8c5137e";

    private static final String JSON_CONSUMER_RECORD = "{"
            + "\"routingData\":{\"topic\":\"topic-0\",\"shardingKey\":\"shardingKey-0\"},"
            + "\"message\":"
            + "{"
            + "\"headers\":"
            + "{\"sourceId\":\"sourceId-0\",\"scope\":\"scope-0\",\"tenantId\":\"tenantId-0\",\"zoneId\":\"zoneId-0\"},"
            + "\"payload\":\"UGF5bG9hZCAjMQ==\""
            + "},"
            + "\"partition\":0,\"offset\":1"
            + "}";

    private static final ProducerRecords TWO_PRODUCER_RECORDS = new ProducerRecords();
    static {
        TWO_PRODUCER_RECORDS.add(
                new ProducerRecords.ProducerRecord
                        .Builder("my-topic",
                        "Hello-OpenDXL-1")
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                        }})
                        .withShardingKey("123")
                        .build()
        );
        TWO_PRODUCER_RECORDS.add(
                new ProducerRecords.ProducerRecord
                        .Builder("topic1",
                        "Hello-OpenDXL-2")
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "F567D6A2-500E-4D35-AE15-A707f165D4FA");
                            put("anotherHeader", "one-two-three-four");
                        }})
                        .withShardingKey("456")
                        .build()
        );
    }

    // The following records are intended to be used as "--records" parameter value
    // They are equivalent to TWO_PRODUCER_RECORDS, e.g.: when CLI reads the simplified records, it will
    // instantiate an equivalent ProducerRecord
    private static final String TWO_SIMPLIFIED_PRODUCER_RECORDS = "["
            + "{"
            + "\"topic\":\"my-topic\","
            + "\"payload\":\"Hello-OpenDXL-1\","
            + "\"shardingKey\":\"123\","
            + "\"headers\":"
            + "{"
            + "\"sourceId\":\"D5452543-E2FB-4585-8BE5-A61C3636819C\""
            + "}"
            + "},"
            + "{"
            + "\"topic\":\"topic1\","
            + "\"payload\":\"Hello-OpenDXL-2\","
            + "\"shardingKey\":\"456\","
            + "\"headers\":"
            + "{"
            + "\"sourceId\":\"F567D6A2-500E-4D35-AE15-A707f165D4FA\","
            + "\"anotherHeader\":\"one-two-three-four\""
            + "}"
            + "}"
            + "]";

    private void setUpWireMockStubs(final String consumerId, final String cookieValue, final String topic,
                            final String jsonConsumerRecord, final int timeoutMs) {
        // set up response to create
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Set-Cookie", "AWSALB=" + cookieValue + "; Path=/my-path; HttpOnly")
                        .withBody("{\"consumerInstanceId\":\"" + consumerId + "\"}")));

        // set up response to subscription
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/subscription"))
                .withCookie("AWSALB", equalTo(cookieValue))
                .withRequestBody(equalToJson("{\"topics\":[\"" + topic + "\"]}"))
                .willReturn(aResponse()
                        .withStatus(204)));

        // set up response to get subscriptions
        stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/subscription"))
                .withCookie("AWSALB", equalTo(cookieValue))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"" + topic + "\"]")));

        // set up response to consume records
        if (timeoutMs > 0) {
            stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/records?timeout="
                    + Integer.toString(timeoutMs)))
                    .withCookie("AWSALB", equalTo(cookieValue))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"records\":[" + jsonConsumerRecord + "]}")));
        } else {
            stubFor(get(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/records"))
                    .withCookie("AWSALB", equalTo(cookieValue))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"records\":[" + jsonConsumerRecord + "]}")));
        }

        // set up response to commit offsets
        stubFor(post(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId + "/offsets"))
                .withCookie("AWSALB", equalTo(cookieValue))
                .willReturn(aResponse()
                        .withStatus(204)));

        // set up response to delete channel
        stubFor(delete(urlEqualTo("/databus/consumer-service/v1/consumers/" + consumerId))
                .withCookie("AWSALB", equalTo(cookieValue))
                .willReturn(aResponse()
                        .withStatus(204)));
    }

}