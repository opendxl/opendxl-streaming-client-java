/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.operation;

import com.opendxl.streaming.cli.CliUtils;
import com.opendxl.streaming.cli.CommandLineInterface;
import com.opendxl.streaming.cli.Options;
import com.opendxl.streaming.cli.entity.ExecutionResult;
import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.auth.ChannelAuthToken;
import com.opendxl.streaming.client.entity.ProducerRecords;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the "produce" argument for a --operation option
 */
public class ProduceOperation implements CommandLineOperation {

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
    private static final String OPERATION_NAME = OperationArguments.PRODUCE.argumentName;

    /**
     * Constructor
     *
     * @param optionSpecMap Options and argument spec
     * @param options       parsed command line options and arguments
     */
    public ProduceOperation(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                            final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.URL, optionSpecMap.get(Options.URL));
        mandatoryOptions.put(Options.TOKEN, optionSpecMap.get(Options.TOKEN));
        mandatoryOptions.put(Options.PRODUCER_PATH_PREFIX, optionSpecMap.get(Options.PRODUCER_PATH_PREFIX));
        mandatoryOptions.put(Options.VERIFY_CERT_BUNDLE, optionSpecMap.get(Options.VERIFY_CERT_BUNDLE));
        mandatoryOptions.put(Options.HTTP_PROXY, optionSpecMap.get(Options.HTTP_PROXY));
        mandatoryOptions.put(Options.PRODUCER_RECORDS, optionSpecMap.get(Options.PRODUCER_RECORDS));

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
     * @return The result of produce operation command line. Example in Json notation after serializing
     *
     * <pre>
     * {
     *   "code": "204",
     *   "result": "",
     *   "options": {
     *     "records": ["[{\"topic\":\"topic1\",\"payload\":\"Hello OpenDXL\",\"shardingKey\":\"123\",
     *                  \"headers\":{\"sourceId\":\"D5452543-E2FB-4585-8BE5-A61C3636819C\"}}]"],
     *     "verify-cert-bundle": ["1234"],
     *     "producer-prefix": ["/databus/cloudproxy/v1"],
     *     "http-proxy": ["true,10.20.30.40,8080"],
     *     "url": ["https://my-host.my-domain.net"],
     *     "token": ["myToken"]
     *   }
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
                    null,
                    options.valueOf(mandatoryOptions.get(Options.PRODUCER_PATH_PREFIX)),
                    false,
                    CliUtils.getCertificate(options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE))),
                    null,
                    CliUtils.getHttpProxySettings(options.valueOf(mandatoryOptions.get(Options.HTTP_PROXY))));

            /**
             * Create the {@link ProducerRecords} object from the
             * {@link com.opendxl.streaming.cli.CliUtils.SimplifiedProducerRecord} object got from the "--records"
             * parameter
             */
            final ProducerRecords producerRecords = CliUtils.getProducerRecords(
                    options.valueOf(mandatoryOptions.get(Options.PRODUCER_RECORDS)));

            /**
             * Produce the records
             */
            channel.produce(producerRecords);

            return new ExecutionResult("204", "",
                    CliUtils.getCommandLine(options, mandatoryOptions));


        } catch (Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage(), e);
        }

        return null;

    }
}
