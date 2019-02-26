/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;
import junit.extensions.PA;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents the "create" argument for a --operation option
 */
public class CreateOperationArgument implements CommandLineOperationArgument {

    /**
     * The operation name
     */
    public static final String OPERATION_NAME = OperationArguments.CREATE.argumentName;


    private final OptionSet options;

    Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();


    /**
     * Constructor
     *
     * @param optionSpecMap Options and argument spec
     * @param options       parsed command line options and arguments
     */
    public CreateOperationArgument(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                                   final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.URL, optionSpecMap.get(Options.URL));
        mandatoryOptions.put(Options.TOKEN, optionSpecMap.get(Options.TOKEN));
        mandatoryOptions.put(Options.CG, optionSpecMap.get(Options.CG));
        mandatoryOptions.put(Options.CONFIG, optionSpecMap.get(Options.CONFIG));
        mandatoryOptions.put(Options.RETRY, optionSpecMap.get(Options.RETRY));
        mandatoryOptions.put(Options.CONSUMER_PATH_PREFIX, optionSpecMap.get(Options.CONSUMER_PATH_PREFIX));
        mandatoryOptions.put(Options.CONSUMER_OFFSET_RESET, optionSpecMap.get(Options.CONSUMER_OFFSET_RESET));
        mandatoryOptions.put(Options.CONSUMER_SESSION_TIMEOUT, optionSpecMap.get(Options.CONSUMER_SESSION_TIMEOUT));
        mandatoryOptions.put(Options.CONSUMER_REQUEST_TIMEOUT, optionSpecMap.get(Options.CONSUMER_REQUEST_TIMEOUT));
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
     */
    @Override
    public ExecutionResult execute() {
        // create a channel auth just to inject a token and be used by Channel
        final ChannelAuth channelAuth =
                Helper.channelAuthFactory(options.valueOf(mandatoryOptions.get(Options.URL)), "", "", "");

        PA.setValue(channelAuth, "token", Optional.of(options.valueOf(mandatoryOptions.get(Options.TOKEN))));

        // parse config
        Optional<Map<String, Object>> optionalConsumerConfig =
                Helper.configToMap(options.valueOf(mandatoryOptions.get(Options.CONFIG)));

        // Create a Channel
        URL url = null;
        try {
            url = new URL(options.valueOf(mandatoryOptions.get(Options.URL)));
        } catch (MalformedURLException e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }

        Channel channel = new Channel(Helper.getBaseURL(url),
                channelAuth,
                options.valueOf(mandatoryOptions.get(Options.CG)),
                Optional.empty(),
                Optional.of(options.valueOf(mandatoryOptions.get(Options.CONSUMER_PATH_PREFIX))),
                options.valueOf(mandatoryOptions.get(Options.CONSUMER_OFFSET_RESET)),
                Integer.valueOf(options.valueOf(mandatoryOptions.get(Options.CONSUMER_REQUEST_TIMEOUT))),
                Integer.valueOf(options.valueOf(mandatoryOptions.get(Options.CONSUMER_SESSION_TIMEOUT))),
                Boolean.valueOf(options.valueOf(mandatoryOptions.get(Options.RETRY))),
                options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE)),
                optionalConsumerConfig);

        channel.create();

        final String consumerId = (String) PA.getValue(channel, "consumerId");

        return new ExecutionResult("200", new CreateOperationResult(consumerId, Helper.getCookie(channel)),
                CommandLineUtils.getCommandLine(options, mandatoryOptions));
    }


    private class CreateOperationResult {
        private String consumerId;
        private String cookie;

        CreateOperationResult(final String consumerId, final String cookie) {
            this.consumerId = consumerId;
            this.cookie = cookie;
        }
    }
}