/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.Constants;
import com.opendxl.streaming.cli.entity.StickinessCookie;
import com.opendxl.streaming.cli.operation.CommandLineOperation;
import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.HttpConnection;
import com.opendxl.streaming.client.Request;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import junit.extensions.PA;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Facilities for command line options
 */
public class CliUtils {


    private CliUtils() {

    }

    /**
     * This method is invoked when the command line made up by options and argument
     * are ill formed or do not meet options spec. Then , it shows the usage and exit with a error
     *
     * @param parser  The utility capable to show the usage
     * @param message Message Error
     */
    public static void printUsageAndFinish(final OptionParser parser, final String message) {
        try {
            System.err.println("ERROR: " + message);
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().exit(1);
    }


    /**
     * This method validate the mandatory arguments for a specific operation
     *
     * @param operationArgument It represents a instance of --operation argument
     * @param parser            The utility capable to show the usage
     * @param options           Represents the set of options in the command line
     */
    public static void validateMandatoryOperationArgs(final CommandLineOperation operationArgument,
                                                      final OptionParser parser, final OptionSet options) {
        if (operationArgument == null) {
            CliUtils.printUsageAndFinish(parser, "[operation argument is unknown]");
        }

        operationArgument.getMandatoryOptions().forEach((option, mandatoryOption) -> {
            if (!options.has(mandatoryOption) && mandatoryOption.defaultValues().isEmpty()) {
                CliUtils.printUsageAndFinish(parser, mandatoryOption.toString() + " is missing for "
                        + operationArgument.getOperationName() + " operation");
            }
        });
    }


    /**
     * Prints usage and exits without system error
     *
     * @param executionResult It represents the result of a command line operation
     */
    public static void printUsageAndFinish(final String executionResult) {
        System.out.println(executionResult);
        Runtime.getRuntime().exit(0);
    }


    /**
     * It gets a list of options used for a specific operation command line
     *
     * @param options Options supported by cli
     * @param mandatoryOptions mandatory options for a specific operation
     * @return list of options for the command line
     */
    public static Map<String, List<?>> getCommandLine(final OptionSet options,
                                                      final Map<Options, ArgumentAcceptingOptionSpec<String>>
                                                              mandatoryOptions) {

        Map<OptionSpec<?>, List<?>> optionSpecListMap = options.asMap();

        Map<String, List<?>> result = new HashMap<>();

        for (Map.Entry<Options, ArgumentAcceptingOptionSpec<String>> pair : mandatoryOptions.entrySet()) {
            List<?> objects = optionSpecListMap.get(pair.getValue());
            if (objects != null) {
                final String option = pair.getValue().toString();
                result.put(option.substring(1, option.length() - 1), objects);
            }
        }
        return result;
    }


    /**
     * Get url based String based on URL instance
     *
     * @param url instance
     * @return url based String
     */
    public static String getBaseURL(final URL url) {
        return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
    }


    /**
     * Get properties from comma-separated config String
     *
     * @param commaSeparatedConfig comma-separated config String
     * @return Properties instance
     */
    public static Properties configToMap(final String commaSeparatedConfig) {
        final Properties map = new Properties();
        try {
            final String[] keyValuePairs = commaSeparatedConfig.split(",");

            for (String pair : keyValuePairs) {
                String[] entry = pair.split("=");
                map.put(entry[0].trim(), entry[1].trim());
            }
        } catch (Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }
        return map;
    }


    /**
     * Get a List of topics from a command-separated topic String
     *
     * @param commaSeparatedTopics a comma-separated topic String
     * @return a List of topics
     */
    public static List<String> topicsToList(final String commaSeparatedTopics) {
        final List<String> result = new ArrayList<>();
        try {
            return Arrays.asList(commaSeparatedTopics.split(","));
        } catch (Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }
        return result;
    }

    /**
     * Get stickiness cookie from channel
     *
     * @param channel Channel instance to get cookie
     * @return stickiness cookie
     */
    public static StickinessCookie getCookie(final Channel channel) {
        final Request request = (Request) PA.getValue(channel, "request");
        final HttpConnection httpConnection = (HttpConnection) PA.getValue(request, "httpConnection");
        final HttpClientContext clientContext = (HttpClientContext) PA.getValue(httpConnection, "httpClientContext");
        final List<Cookie> cookies = clientContext.getCookieStore().getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(Constants.STIKINESS_COOKIE_NAME)) {
                return new StickinessCookie(cookie.getValue(), cookie.getDomain());
            }
        }
        return new StickinessCookie("", "");
    }

    /**
     * Set stickiness cookie to channel
     *
     * @param channel Channel instance to set cookie
     * @param stickinessCookie cookie to be injected
     */
    public static  void setCookie(final Channel channel, final StickinessCookie stickinessCookie) {
        final Request request = (Request) PA.getValue(channel, "request");
        final HttpConnection httpConnection = (HttpConnection) PA.getValue(request, "httpConnection");
        final HttpClientContext clientContext =
                (HttpClientContext) PA.getValue(httpConnection, "httpClientContext");
        BasicClientCookie cookie = new BasicClientCookie(Constants.STIKINESS_COOKIE_NAME,
                stickinessCookie.getValue());
        clientContext.getCookieStore().addCookie(cookie);
        cookie.setDomain(stickinessCookie.getDomain());
    }

}
