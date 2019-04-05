/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.client.Channel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opendxl.streaming.cli.entity.ExecutionResult;
import com.opendxl.streaming.cli.operation.CommandLineOperation;
import com.opendxl.streaming.cli.operation.OperationFactory;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class for the OpenDXL Streaming Java Client CLI.
 * This class enable OpenDXL streaming client to be used as a command line tool.
 * Just type java -jar JavaJarName.jar and follow instructions
 *
 */
public class CommandLineInterface {
    /**
     * Represents the set of options arguments in the command line
     */
    private OptionSet options;
    /**
     * Operation is a CommandLineOperation instance suitable to {@link Channel} API method that will be executed.
     * Each API method is associated to an specific Operation, e.g.:
     * {@link Channel#create()} is associated to {@link com.opendxl.streaming.cli.operation.CreateOperation},
     * {@link Channel#subscribe(List)} is associated to {@link com.opendxl.streaming.cli.operation.SubscribeOperation},
     * etc. Goal of each Operation class is to call its associated API method from the command line.
     */
    private CommandLineOperation operation;
    /**
     * Parses command line arguments
     */
    public static OptionParser parser = new OptionParser(false);

    /**
     * Constructor
     *
     * @param args options and arguments passing in command line
     */
    public CommandLineInterface(String[] args) {


        // operation option spec represented as --operation command line
        final ArgumentAcceptingOptionSpec<String> operationsOpt =
                parser.accepts("operation", "Operations: login | create | subscribe | consume | commit | subscription")
                        .withRequiredArg()
                        .describedAs("operation")
                        .ofType(String.class)
                        .required();

        // topic option spec represented as --topic command line
        final ArgumentAcceptingOptionSpec<String> topicIdOpt =
                parser.accepts("topic", "Comma-separated topic list to subscribe to: topic1,topic2,...,topicN.")
                        .withRequiredArg()
                        .describedAs("topic")
                        .ofType(String.class);


        // Authorization Service URL represented as --auth-url command line
        final ArgumentAcceptingOptionSpec<String> authURLOpt =
                parser.accepts("auth-url", "The URL to authorization service.")
                        .withRequiredArg()
                        .describedAs("auth-url")
                        .ofType(String.class);

        // User name option spec represented as --user command line
        final ArgumentAcceptingOptionSpec<String> userOpt =
                parser.accepts("user", "The user name to send to authorization service.")
                        .withRequiredArg()
                        .describedAs("user")
                        .ofType(String.class);

        // password option spec represented as --password command line
        final ArgumentAcceptingOptionSpec<String> passwordOpt =
                parser.accepts("password", "The password to send to authorization service.")
                        .withRequiredArg()
                        .describedAs("password")
                        .ofType(String.class);

        // Verify Cert Bundle option spec represented as --verify-cert-bundle command line
        final ArgumentAcceptingOptionSpec<String> verifyCertBundleOpt =
                parser.accepts("verify-cert-bundle", "The ca certificate.")
                        .withRequiredArg()
                        .describedAs("verify-cert-bundle")
                        .ofType(String.class)
                        .defaultsTo("");


        // Consumer Service URL option spec represented as --url command line
        final ArgumentAcceptingOptionSpec<String> uRLOpt =
                parser.accepts("url", "The URL to hit consumer service.")
                        .withRequiredArg()
                        .describedAs("url")
                        .ofType(String.class);

        // Token option spec represented as --token command line
        final ArgumentAcceptingOptionSpec<String> tokenOpt =
                parser.accepts("token", "The authorized token.")
                        .withRequiredArg()
                        .describedAs("token")
                        .ofType(String.class);

        // Consumer Group option spec represented as --cg command line
        final ArgumentAcceptingOptionSpec<String> consumerGroupOpt =
                parser.accepts("cg", "The consumer group name.")
                        .withRequiredArg()
                        .describedAs("cg")
                        .ofType(String.class);

        // Consumer config option spec represented as --config command line
        final ArgumentAcceptingOptionSpec<String> consumerConfigOpt =
                parser.accepts("config", "The consumer configuration.")
                        .withRequiredArg()
                        .describedAs("config")
                        .ofType(String.class);

        // Consumer config option spec represented as --retry command line
        final ArgumentAcceptingOptionSpec<String> retryOnFailOpt =
                parser.accepts("retry", "Retry on fail.")
                        .withRequiredArg()
                        .describedAs("retry")
                        .ofType(String.class)
                        .defaultsTo("true");


        // Consumer path prefix option spec represented as --consumer-prefix command line
        final ArgumentAcceptingOptionSpec<String> consumerPathPrefix =
                parser.accepts("consumer-prefix", "Consumer path prefix.")
                        .withRequiredArg()
                        .describedAs("consumer-prefix")
                        .ofType(String.class)
                        .defaultsTo("/databus/consumer-service/v1");

        // Consumer ID  option spec represented as --consumer-id command line
        final ArgumentAcceptingOptionSpec<String> consumerIdOpt =
                parser.accepts("consumer-id", "Consumer Id")
                        .withRequiredArg()
                        .describedAs("consumer-id")
                        .ofType(String.class);

        // Cookie value  option spec represented as --cookie command line
        final ArgumentAcceptingOptionSpec<String> cookieOpt =
                parser.accepts("cookie", "Cookie value")
                        .withRequiredArg()
                        .describedAs("cookie")
                        .ofType(String.class);

        // Cookie domain value option spec represented as --domain command line
        final ArgumentAcceptingOptionSpec<String> domainOpt =
                parser.accepts("domain", "Cookie domain value")
                        .withRequiredArg()
                        .describedAs("domain")
                        .ofType(String.class);

        if (args.length == 0) {
            CliUtils.printUsageAndFinish(parser, "There are not options");
        }

        parseOptions(args);
        String s = options.valueOf(verifyCertBundleOpt);

        final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap = new HashMap();
        optionSpecMap.put(Options.OPERATION, operationsOpt);
        optionSpecMap.put(Options.TOPIC, topicIdOpt);
        optionSpecMap.put(Options.AUTH_URL, authURLOpt);
        optionSpecMap.put(Options.USER, userOpt);
        optionSpecMap.put(Options.PASSWORD, passwordOpt);
        optionSpecMap.put(Options.VERIFY_CERT_BUNDLE, verifyCertBundleOpt);
        optionSpecMap.put(Options.URL, uRLOpt);
        optionSpecMap.put(Options.TOKEN, tokenOpt);
        optionSpecMap.put(Options.CG, consumerGroupOpt);
        optionSpecMap.put(Options.CONFIG, consumerConfigOpt);
        optionSpecMap.put(Options.RETRY, retryOnFailOpt);
        optionSpecMap.put(Options.CONSUMER_PATH_PREFIX, consumerPathPrefix);
        optionSpecMap.put(Options.CONSUMER_ID, consumerIdOpt);
        optionSpecMap.put(Options.COOKIE, cookieOpt);
        optionSpecMap.put(Options.DOMAIN, domainOpt);

        this.operation = buildOperation(optionSpecMap);
        CliUtils.validateMandatoryOperationArgs(operation, parser, options);
    }

    /**
     * It parses command line options and their arguments values. If they do not meet spec requirements, it shows
     * the usage and exists with a error.
     *
     * @param args options and arguments values passed in command line
     */
    private void parseOptions(String[] args) {

        try {
            // parse and make sure options passed in command line meet option spec
            this.options = parser.parse(args);
        } catch (Exception e) {
            CliUtils.printUsageAndFinish(parser, e.getMessage());
        }

    }

    /**
     * This method checks that  --operation argument option contains a validated value and
     * all mandatory option being included in command line. Finally, it create a {@link CommandLineOperation}
     * used later to perform the specific one.
     *
     * @param optionSpecMap keeps a relationship between a {@link Options} and a Option Spec
     */
    private CommandLineOperation buildOperation(
            final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap) {

        if (!options.has(optionSpecMap.get(Options.OPERATION))) {
            CliUtils.printUsageAndFinish(parser, "--operation is missing");
        }

        OperationFactory factory = new OperationFactory(optionSpecMap, options);
        return factory.getOperation(optionSpecMap.get(Options.OPERATION));
    }

    public ExecutionResult execute() {
        return operation.execute();
    }


    /**
     * Entry point
     *
     * @param args Command line options and arguments
     */
    public static void main(String[] args)  {
        final CommandLineInterface cli = new CommandLineInterface(args);
        final ExecutionResult executionResult = cli.execute();
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        CliUtils.printUsageAndFinish(gson.toJson(executionResult));
    }

}
