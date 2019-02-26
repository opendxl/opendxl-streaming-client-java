/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.Request;
import com.opendxl.streaming.client.entity.ConsumerRecords;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;
import junit.extensions.PA;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConsumeOperationArgument implements CommandLineOperationArgument {


    /**
     * The operation name
     */
    public static final String OPERATION_NAME = OperationArguments.CONSUME.argumentName;


    Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();

    private final OptionSet options;

    public ConsumeOperationArgument(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                                    final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.URL, optionSpecMap.get(Options.URL));
        mandatoryOptions.put(Options.TOKEN, optionSpecMap.get(Options.TOKEN));
        mandatoryOptions.put(Options.CONSUMER_ID, optionSpecMap.get(Options.CONSUMER_ID));
        mandatoryOptions.put(Options.CONSUMER_PATH_PREFIX, optionSpecMap.get(Options.CONSUMER_PATH_PREFIX));
        mandatoryOptions.put(Options.VERIFY_CERT_BUNDLE, optionSpecMap.get(Options.VERIFY_CERT_BUNDLE));
        mandatoryOptions.put(Options.COOKIE, optionSpecMap.get(Options.COOKIE));
        mandatoryOptions.put(Options.DOMAIN, optionSpecMap.get(Options.DOMAIN));
    }

    @Override
    public Map<Options, ArgumentAcceptingOptionSpec<String>> getMandatoryOptions() {
        return mandatoryOptions;
    }

    @Override
    public String getOperationName() {
        return OPERATION_NAME;
    }

    @Override
    public ExecutionResult execute() {

        try {
            // create a channel auth just to inject a token and be used by Channel
            final ChannelAuth channelAuth =
                    Helper.channelAuthFactory(options.valueOf(mandatoryOptions.get(Options.URL)),
                            "", "", "");

            PA.setValue(channelAuth, "token",
                    Optional.of(options.valueOf(mandatoryOptions.get(Options.TOKEN))));

            // parse config
            Optional<Map<String, Object>> optionalConsumerConfig = Optional.empty();

            // Create a Channel
            URL url = null;
            try {
                url = new URL(options.valueOf(mandatoryOptions.get(Options.URL)));
            } catch (MalformedURLException e) {
                CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
            }

            Channel channel = new Channel(Helper.getBaseURL(url),
                    channelAuth,
                    "", // options.valueOf(mandatoryOptions.get(Options.CG)),
                    Optional.empty(),
                    Optional.of(options.valueOf(mandatoryOptions.get(Options.CONSUMER_PATH_PREFIX))),
                    "latest", // options.valueOf(mandatoryOptions.get(Options.CONSUMER_OFFSET_RESET)),
                    null, //Integer.valueOf(options.valueOf(mandatoryOptions.get(Options.CONSUMER_REQUEST_TIMEOUT))),
                    null, //Integer.valueOf(options.valueOf(mandatoryOptions.get(Options.CONSUMER_SESSION_TIMEOUT))),
                    false,  //Boolean.valueOf(options.valueOf(mandatoryOptions.get(Options.RETRY))),
                    options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE)),
                    Optional.empty());

            PA.setValue(channel, "consumerId", options.valueOf(mandatoryOptions.get(Options.CONSUMER_ID)));


            final Request request = (Request) PA.getValue(channel, "request");
            final HttpClientContext clientContext =
                    (HttpClientContext) PA.getValue(request, "httpClientContext");
            BasicClientCookie cookie = new BasicClientCookie("AWSALB",
                    options.valueOf(mandatoryOptions.get(Options.COOKIE)));
            clientContext.getCookieStore().addCookie(cookie);
            cookie.setDomain(options.valueOf(mandatoryOptions.get(Options.DOMAIN)));

            ConsumerRecords records = channel.consume();


            return new ExecutionResult("200", records.getRecords(),
                    CommandLineUtils.getCommandLine(options, mandatoryOptions));


        } catch (Exception e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }

        return null;

    }
}
