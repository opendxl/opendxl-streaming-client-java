/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.operation;

import com.opendxl.streaming.cli.CliUtils;
import com.opendxl.streaming.cli.CommandLineInterface;
import com.opendxl.streaming.cli.Options;
import com.opendxl.streaming.cli.entity.ExecutionResult;
import com.opendxl.streaming.cli.entity.StickinessCookie;
import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.auth.ChannelAuthToken;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;
import junit.extensions.PA;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class represents the "create" argument for a --operation option
 */
public class CreateOperation implements CommandLineOperation {

    /**
     * The operation name
     */
    private static final String OPERATION_NAME = OperationArguments.CREATE.argumentName;

    /**
     * A list of mandatory options for this operation command line
     */
    private final OptionSet options;

    /**
     * Command line parsed options
     */
    private Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();


    /**
     * Constructor
     *
     * @param optionSpecMap Options and argument spec
     * @param options       parsed command line options and arguments
     */
    public CreateOperation(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                           final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.URL, optionSpecMap.get(Options.URL));
        mandatoryOptions.put(Options.TOKEN, optionSpecMap.get(Options.TOKEN));
        mandatoryOptions.put(Options.CG, optionSpecMap.get(Options.CG));
        mandatoryOptions.put(Options.RETRY, optionSpecMap.get(Options.RETRY));
        mandatoryOptions.put(Options.CONSUMER_PATH_PREFIX, optionSpecMap.get(Options.CONSUMER_PATH_PREFIX));
        mandatoryOptions.put(Options.VERIFY_CERT_BUNDLE, optionSpecMap.get(Options.VERIFY_CERT_BUNDLE));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Options, ArgumentAcceptingOptionSpec<String>> getMandatoryOptions() {
        return mandatoryOptions;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperationName() {
        return OPERATION_NAME;
    }


    /**
     * {@inheritDoc}
     *
     * @return The result of creating operation command line. Example in Json notation
     *
     * <pre>
     * {
     *     "code": "200",
     *     "result": {
     *         "consumerId": "c0c29763-eb5a-4edd-b3fa-83b9c6d527c9a931f3a5-1e5a-4b3d-b5ae-e45cfdf75853",
     *         "cookie": {
     *             "value": "qMbg3fL95SVj0Mc++/f07G7PBfAvPlfYlFbjuelDYDlSMU...",
     *             "domain": "my-host.my-domain.net"
     *         }
     *     },
     *     "options": {
     *         "session-timeout": ["60000"],
     *         "cg": ["cg1"],
     *         "offset-reset": ["latest"],
     *         "consumer-prefix": ["/databus/consumer-service/v1"],
     *         "verify-cert-bundle": ["1234"],
     *         "request-timeout": ["60001"],
     *         "config": ["max.message.size=1000,min.message.size=200"],
     *         "url": ["https://my-host.my-domain.net/databus/consumer-service/v1"],
     *         "retry": ["true"],
     *         "token": ["myToken"]
     *     }
     * }
     * </pre>
     */
    @Override
    public ExecutionResult execute() {

        try {
            // create a channel auth just to inject a token and be used by Channel
            final ChannelAuth channelAuth = new ChannelAuthToken(options.valueOf(mandatoryOptions.get(Options.TOKEN)));

            // parse config
            Properties optionalConsumerConfig = options.hasArgument(Options.CONFIG.name().toLowerCase())
                    ? CliUtils.configToMap(options.valueOf(Options.CONFIG.name().toLowerCase()).toString())
                    : null;

            // Create a Channel
            URL url = null;
            try {
                url = new URL(options.valueOf(mandatoryOptions.get(Options.URL)));
            } catch (MalformedURLException e) {
                CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
            }

            Channel channel = new Channel(CliUtils.getBaseURL(url),
                    channelAuth,
                    options.valueOf(mandatoryOptions.get(Options.CG)),
                    null,
                    options.valueOf(mandatoryOptions.get(Options.CONSUMER_PATH_PREFIX)),
                    Boolean.valueOf(options.valueOf(mandatoryOptions.get(Options.RETRY))),
                    options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE)),
                    optionalConsumerConfig,
                    null,
                    100);

            channel.create();

            final String consumerId = (String) PA.getValue(channel, "consumerId");

            return new ExecutionResult("200", new CreateOperationResult(consumerId,
                    CliUtils.getCookie(channel)),
                    CliUtils.getCommandLine(options, mandatoryOptions));

        } catch (Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }

        return null;
    }


    private class CreateOperationResult {
        private String consumerId;
        private StickinessCookie cookie;

        CreateOperationResult(final String consumerId, final StickinessCookie stickinessCookie) {
            this.consumerId = consumerId;
            this.cookie = stickinessCookie;
        }
    }
}