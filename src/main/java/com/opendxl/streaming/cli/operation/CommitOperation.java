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

/**
 * It represents the "commit" argument for a --operation option
 */
public class CommitOperation implements CommandLineOperation {

    /**
     * The operation name
     */
    private static final String OPERATION_NAME = OperationArguments.COMMIT.argumentName;

    /**
     * A list of mandatory options for this operation command line
     */
    private Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();

    /**
     * Command line parsed options
     */
    private final OptionSet options;

    /**
     * Constructor
     *
     * @param optionSpecMap Map of options spec
     * @param options Command line parsed options
     */
    public CommitOperation(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                           final OptionSet options) {

        this.options = options;
        mandatoryOptions.put(Options.URL, optionSpecMap.get(Options.URL));
        mandatoryOptions.put(Options.TOKEN, optionSpecMap.get(Options.TOKEN));
        mandatoryOptions.put(Options.CONSUMER_ID, optionSpecMap.get(Options.CONSUMER_ID));
        mandatoryOptions.put(Options.CONSUMER_PATH_PREFIX, optionSpecMap.get(Options.CONSUMER_PATH_PREFIX));
        mandatoryOptions.put(Options.VERIFY_CERT_BUNDLE, optionSpecMap.get(Options.VERIFY_CERT_BUNDLE));
        mandatoryOptions.put(Options.COOKIE, optionSpecMap.get(Options.COOKIE));
        mandatoryOptions.put(Options.DOMAIN, optionSpecMap.get(Options.DOMAIN));
        mandatoryOptions.put(Options.HTTP_PROXY, optionSpecMap.get(Options.HTTP_PROXY));
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public Map<Options, ArgumentAcceptingOptionSpec<String>> getMandatoryOptions() {
        return mandatoryOptions;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public String getOperationName() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return The result of committing operation command line. Example in Json notation after serializing
     * <pre>
     * {
     *     "code": "204",
     *     "result": "",
     *     "options": {
     *         "cookie": ["+w1mctRvvxFpi31nLDdHYxi7UaA/QzyMTTNhphyNRXXklE7Q0BL86fB23Tho01a2wnb..."],
     *         "consumer-prefix": ["/databus/consumer-service/v1"],
     *         "domain": ["my-host.my-domain.net"],
     *         "verify-cert-bundle": ["1234"],
     *         "consumer-id": ["50cc3c66-8f18-4339-ba67-211ab41a311fd4c8302f-8cdd-415c-b553-4286f497edca"],
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


            // Create a Channel
            URL url = null;
            try {
                url = new URL(options.valueOf(mandatoryOptions.get(Options.URL)));
            } catch (MalformedURLException e) {
                CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage(), e);
            }
            Channel channel = new Channel(CliUtils.getBaseURL(url),
                    channelAuth,
                    "",
                    null,
                    options.valueOf(mandatoryOptions.get(Options.CONSUMER_PATH_PREFIX)),
                    false,
                    CliUtils.getCertificate(options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE))),
                    null,
                    CliUtils.getHttpProxySettings(options.valueOf(mandatoryOptions.get(Options.HTTP_PROXY))));

            // Inject consumerId to channel
            PA.setValue(channel, "consumerId", options.valueOf(mandatoryOptions.get(Options.CONSUMER_ID)));

            // Inject Stickiness Cookie to channel
            CliUtils.setCookie(channel,
                    new StickinessCookie(options.valueOf(mandatoryOptions.get(Options.COOKIE)),
                            options.valueOf(mandatoryOptions.get(Options.DOMAIN))));

            channel.commit();

            // Consume records and return execution result
            return new ExecutionResult("204", "",
                    CliUtils.getCommandLine(options, mandatoryOptions));


        } catch (Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage(), e);
        }

        return null;

    }
}
