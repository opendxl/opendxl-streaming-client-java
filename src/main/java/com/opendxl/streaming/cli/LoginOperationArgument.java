/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.client.ChannelAuth;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents the "login" argument for a --operation option
 */
public class LoginOperationArgument implements CommandLineOperationArgument {

    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String BEARER_TOKEN_TYPE = "Bearer";
    private  Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap;
    private final OptionSet options;

    Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();
    public static final String OPERATION_NAME = OperationArguments.LOGIN.argumentName;


    /**
     *
     * @param optionSpecMap Options and argument spec
     * @param options parsed command line options and arguments
     */
    public LoginOperationArgument(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                                  final OptionSet options) {
        this.options = options;

        mandatoryOptions.put(Options.AUTH_URL, optionSpecMap.get(Options.AUTH_URL));
        mandatoryOptions.put(Options.USER, optionSpecMap.get(Options.USER));
        mandatoryOptions.put(Options.PASSWORD, optionSpecMap.get(Options.PASSWORD));
        mandatoryOptions.put(Options.VERIFY_CERT_BUNDLE, optionSpecMap.get(Options.VERIFY_CERT_BUNDLE));
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

        String base = "";
        String path = "";

        try {
            URL url = new URL(options.valueOf(mandatoryOptions.get(Options.AUTH_URL)));
            base = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
            path = url.getPath();
        } catch (MalformedURLException e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }

        ChannelAuth channelAuth = new ChannelAuth(base,
                options.valueOf(mandatoryOptions.get(Options.USER)),
                options.valueOf(mandatoryOptions.get(Options.PASSWORD)),
                Optional.of(path),
                options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE)));

        HttpGet httpRequest = new HttpGet(base + path);
        try {
            channelAuth.authenticate(httpRequest);
            Header authorization = httpRequest.getFirstHeader(AUTHORIZATION_HEADER_KEY);
            String token = getAuthorizationTokenFromHttpHeader(authorization);
            return new ExecutionResult("[CODE ] 200", "[TOKEN] " + token);

        } catch (IOException e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        } catch (KeyStoreException e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        } catch (KeyManagementException e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }
        return new ExecutionResult("", "");
    }

    /**
     * It returns token gotten from http header.
     *
     * @param authorization Authorization http header
     * @return token
     */
    private String getAuthorizationTokenFromHttpHeader(Header authorization) {
        if (authorization == null || authorization.getValue().isEmpty()) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser,
                    "Authorization Service did not return a authorization token");
        }

        final String rawToken = authorization.getValue();

        if (!rawToken.toLowerCase().startsWith(BEARER_TOKEN_TYPE.toLowerCase())) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser,
                    "Bearer token type is missing.");
        }

        return rawToken.substring(BEARER_TOKEN_TYPE.length()).trim();

    }
}
