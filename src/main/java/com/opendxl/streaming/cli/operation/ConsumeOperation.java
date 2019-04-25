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
 * It represents a --consume operation command line
 */
public class ConsumeOperation implements CommandLineOperation {

    /**
     * The operation name
     */
    private  static final String OPERATION_NAME = OperationArguments.CONSUME.argumentName;

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
     * @param options       parsed options
     */
    public ConsumeOperation(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
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
     * @return The result of consuming operation command line. Example in Json notation after serializing
     * <pre>
     * {
     *     "code": "200",
     *     "result": [{
     *         "routingData": {
     *             "topic": "topic3",
     *             "shardingKey": "pool-1-thread-1-30-0"
     *         },
     *         "message": {
     *             "headers": {
     *                 "sourceId": "abc",
     *                 "tenantId": "D27BF561-D6CF-443B-A134-995459EA8921",
     *                 "zoneId": "TMP.Identity.MULTIMOCK"
     *             },
     *             "payload": "SGVsbG8gV29ybGQgYXQ6MjAxOS0wMi0yN1QxNToyMDozMC4wMTYgRXh..."
     *         },
     *         "partition": 3,
     *         "offset": 764
     *     }],
     *     "options": {
     *         "cookie": ["ixB1cHbCVprX8qTIuoo7n8WtTPT8uNnmQ2bcZCDO+NqiRNBms49QAE96b3Q+D7..."],
     *         "consumer-prefix": ["/databus/consumer-service/v1"],
     *         "domain": ["my-host.my-domain.net"],
     *         "verify-cert-bundle": ["1234"],
     *         "consumer-id": ["227e7fcf-84ad-48c4-8e1a-6c3614ac9782c3363971-3f45-417f-a827-80f9079d002d"],
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
                CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
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

            // Set Subscription
            PA.setValue(channel, "subscriptions", channel.subscriptions());

            // Consume records and return execution result
            return new ExecutionResult("200", channel.consume().getRecords(),
                    CliUtils.getCommandLine(options, mandatoryOptions));


        } catch (Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }

        return null;
    }

}
