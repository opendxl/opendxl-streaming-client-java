/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.cli.entity.StickinessCookie;
import com.opendxl.streaming.cli.operation.CommandLineOperation;
import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.HttpConnection;
import com.opendxl.streaming.client.HttpProxySettings;
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
import java.nio.file.Files;
import java.nio.file.Paths;
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

    /**
     * Stikiness cookie name
     */
    private static final String STIKINESS_COOKIE_NAME = "AWSALB";

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
            if (STIKINESS_COOKIE_NAME.equals(cookie.getName())) {
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
        BasicClientCookie cookie = new BasicClientCookie(STIKINESS_COOKIE_NAME,
                stickinessCookie.getValue());
        clientContext.getCookieStore().addCookie(cookie);
        cookie.setDomain(stickinessCookie.getDomain());
    }

    private static final int HTTP_PROXY_ENABLED_INDEX = 0;
    private static final int HTTP_PROXY_URL_INDEX = 1;
    private static final int HTTP_PROXY_PORT_INDEX = 2;
    private static final int HTTP_PROXY_USERNAME_INDEX = 3;
    private static final int HTTP_PROXY_PASSWORD_INDEX = 4;
    private static final int HTTP_PROXY_MANDATORY_PARAMETERS = 3;
    /**
     * Build an HttpProxySettings object from CLI arguments
     *
     * @param httpProxyArgument string with comma separated values for the Http Proxy attributes being:
     *                          enabled (true/false), url, port, username, password. Enabled, url and port attributes
     *                          are mandatory while username and password are optional.
     * @return an HttpProxySettings object built from httpProxyArgument attributes.
     */
    public static HttpProxySettings getHttpProxySettings(final String httpProxyArgument) {
        // Http Proxy is not a mandatory Channel constructor parameter.
        HttpProxySettings httpProxySettings = null;

        try {

            if (httpProxyArgument != null && !httpProxyArgument.trim().isEmpty()) {
                // if Http Proxy is specified, then enabled, url and port are mandatory parameters while username and
                // password are not.
                final List<String> httpProxyArguments = Arrays.asList(httpProxyArgument.split(","));
                // Set optional parameter values.
                if (httpProxyArguments.size() >= HTTP_PROXY_MANDATORY_PARAMETERS) {
                    final String username = httpProxyArguments.size() > HTTP_PROXY_USERNAME_INDEX
                            ? httpProxyArguments.get(HTTP_PROXY_USERNAME_INDEX)
                            : null;
                    final String password = httpProxyArguments.size() > HTTP_PROXY_PASSWORD_INDEX
                            ? httpProxyArguments.get(HTTP_PROXY_PASSWORD_INDEX)
                            : null;
                    // username and optional are optional parameters. They can be null or empty
                    httpProxySettings = new HttpProxySettings(
                            Boolean.parseBoolean(httpProxyArguments.get(HTTP_PROXY_ENABLED_INDEX)),
                            httpProxyArguments.get(HTTP_PROXY_URL_INDEX),
                            Integer.parseInt(httpProxyArguments.get(HTTP_PROXY_PORT_INDEX)),
                            username,
                            password);
                } else {
                    CliUtils.printUsageAndFinish(CommandLineInterface.parser, "Failed to set Http Proxy: "
                            + "insufficient number of parameters (" + httpProxyArguments.size() + "). "
                            + "Enabled, http proxy url and http proxy port are mandatory parameters.");
                }
            }
        } catch (final Exception e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, "Failed to set Http Proxy: "
                    + e.getMessage());
        }

        return httpProxySettings;
    }

    /**
     * Checks whether the certificate parameter value is an existing filename. If it is, then the certificate parameter
     * value is returned without any change. If it is not, then it is assumed the certificate parameter value is a
     * certificate itself and the "-----BEGIN CERTIFICATE-----" and "-----END CERTIFICATE-----" strings will be
     * prepended and appended respectively.
     *
     * @param certificateParameterValue the "--verify-cert-bundle" CLI input parameter value
     * @return the received string value if such string is an existing filename.
     *         the received string with prepended "BEGIN CERTIFICATE" and appended "END CERTIFICATE" strings otherwise.
     */
    public static String getCertificate(final String certificateParameterValue) {
        final String returnValue;

        if (Files.isRegularFile(Paths.get(certificateParameterValue))) {
            // certificate is provided in a file. Keep it as it is.
            returnValue = certificateParameterValue;
        } else {
            // certificate is provided in a string. Prepend "BEGIN CERTIFICATE" and append "END CERTIFICATE" strings.
            returnValue = new StringBuilder("-----BEGIN CERTIFICATE-----\n")
                    .append(certificateParameterValue)
                    .append("-----END CERTIFICATE-----\n").toString();
        }

        return returnValue;

    }

}
