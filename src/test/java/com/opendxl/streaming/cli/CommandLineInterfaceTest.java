package com.opendxl.streaming.cli;

import joptsimple.OptionSet;
import junit.extensions.PA;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import static junit.framework.TestCase.assertTrue;


public class CommandLineInterfaceTest {
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

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

    ///////





}