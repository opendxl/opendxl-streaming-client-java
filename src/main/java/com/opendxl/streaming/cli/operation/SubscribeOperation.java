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
import java.util.List;
import java.util.Map;

/**
 * This class represents the "subscriptions" argument for a --operation option
 */
public class SubscribeOperation implements CommandLineOperation {

    /**
     * Command line parsed options
     */
    private final OptionSet options;

    /**
     * A list of mandatory options for this operation command line
     */
    private Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();

    /**
     * The operation name
     */
    private static final String OPERATION_NAME = OperationArguments.SUBSCRIBE.argumentName;


    /**
     * Constructor
     *
     * @param optionSpecMap Options and argument spec
     * @param options       parsed command line options and arguments
     */
    public SubscribeOperation(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                              final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.URL, optionSpecMap.get(Options.URL));
        mandatoryOptions.put(Options.TOKEN, optionSpecMap.get(Options.TOKEN));
        mandatoryOptions.put(Options.CONSUMER_ID, optionSpecMap.get(Options.CONSUMER_ID));
        mandatoryOptions.put(Options.CONSUMER_PATH_PREFIX, optionSpecMap.get(Options.CONSUMER_PATH_PREFIX));
        mandatoryOptions.put(Options.VERIFY_CERT_BUNDLE, optionSpecMap.get(Options.VERIFY_CERT_BUNDLE));
        mandatoryOptions.put(Options.TOPIC, optionSpecMap.get(Options.TOPIC));
        mandatoryOptions.put(Options.COOKIE, optionSpecMap.get(Options.COOKIE));
        mandatoryOptions.put(Options.DOMAIN, optionSpecMap.get(Options.DOMAIN));

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
     * @return The result of subscribing operation command line. Example in Json notation after serializing
     * <pre>
     * {
     *     "code": "204",
     *     "result": "",
     *     "options": {
     *         "cookie": ["jgpMAGq7QRR5U+hTnTFliPwDdijW6aIMvzVJBThxrKmNQmv88Otm/..."],
     *         "consumer-prefix": ["/databus/consumer-service/v1"],
     *         "domain": ["my-host.my-domain.net"],
     *         "verify-cert-bundle": ["1234"],
     *         "consumer-id": ["38e993a5-d4e5-4b7c-a403-419c2605fe0c9284b653-71b0-4a85-a9ce-78f3dd518da5"],
     *         "topic": ["topic3"],
     *         "url": ["https://my-host.my-domain.net/databus/consumer-service/v1"],
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
            Map<String, Object> optionalConsumerConfig = null;

            // Create a Channel
            URL url = null;
            try {
                url = new URL(options.valueOf(mandatoryOptions.get(Options.URL)));
            } catch (MalformedURLException e) {
                CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
            }

            Channel channel = new Channel(CliUtils.getBaseURL(url),
                    channelAuth,
                    "",
                    null,
                    options.valueOf(mandatoryOptions.get(Options.CONSUMER_PATH_PREFIX)),
                    false,
                    options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE)),
                    null,
                    null,
                    100);

            PA.setValue(channel, "consumerId", options.valueOf(mandatoryOptions.get(Options.CONSUMER_ID)));

            // Inject Stickiness cookie to channel
            CliUtils.setCookie(channel,
                     new StickinessCookie(options.valueOf(mandatoryOptions.get(Options.COOKIE)),
                             options.valueOf(mandatoryOptions.get(Options.DOMAIN))));

            final List<String> topics = CliUtils.topicsToList(options.valueOf(mandatoryOptions.get(Options.TOPIC)));
            channel.subscribe(topics);

            return new ExecutionResult("204", "",
                    CliUtils.getCommandLine(options, mandatoryOptions));


        } catch (Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }

        return null;

    }
}
