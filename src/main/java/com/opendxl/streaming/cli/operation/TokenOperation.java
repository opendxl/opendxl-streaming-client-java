package com.opendxl.streaming.cli.operation;

import com.opendxl.streaming.cli.CliUtils;
import com.opendxl.streaming.cli.CommandLineInterface;
import com.opendxl.streaming.cli.Options;
import com.opendxl.streaming.cli.entity.ExecutionResult;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.auth.ChannelAuthClientCredentialSecret;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TokenOperation implements CommandLineOperation {
    /**
     * private internal Constants
     */
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String BEARER_TOKEN_TYPE = "Bearer";

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
    private static final String OPERATION_NAME = OperationArguments.TOKEN.argumentName;

    /**
     * Constructor
     *
     * @param optionSpecMap Options and argument spec
     * @param options       parsed command line options and arguments
     */
    public TokenOperation(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                          final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.AUTH_URL, optionSpecMap.get(Options.AUTH_URL));
        mandatoryOptions.put(Options.CLIENT_ID, optionSpecMap.get(Options.CLIENT_ID));
        mandatoryOptions.put(Options.CLIENT_SECRET, optionSpecMap.get(Options.CLIENT_SECRET));
        mandatoryOptions.put(Options.AUDIENCE, optionSpecMap.get(Options.AUDIENCE));
        mandatoryOptions.put(Options.GRANT_TYPE, optionSpecMap.get(Options.GRANT_TYPE));
        mandatoryOptions.put(Options.SCOPE, optionSpecMap.get(Options.SCOPE));
        mandatoryOptions.put(Options.VERIFY_CERT_BUNDLE, optionSpecMap.get(Options.VERIFY_CERT_BUNDLE));
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
     *
     * @return The result of token operation command line. Example in Json
     *         notation after serializing
     *
     *         <pre>
     * {
     *     "code": "200",
     *     "result": "myToken",
     *     "options": {
     *         "password": ["secret"],
     *         "auth-url": ["https://my-host.my-domain.net/identity/v1/login"],
     *         "verify-cert-bundle": ["1234"],
     *         "user": ["me"]
     *     }
     * }
     *         </pre>
     */
    @Override
    public ExecutionResult execute() {
        String base = "";
        String path = "";

        try {
            URL url = new URL(options.valueOf(mandatoryOptions.get(Options.AUTH_URL)));
            base = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
            path = url.getPath();
        } catch (MalformedURLException e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage(), e);
        }

        HttpGet httpRequest = new HttpGet(base + path);
        try {
            final ChannelAuth channelAuth = new ChannelAuthClientCredentialSecret(
                    options.valueOf(mandatoryOptions.get(Options.AUTH_URL)),
                    options.valueOf(mandatoryOptions.get(Options.CLIENT_ID)),
                    options.valueOf(mandatoryOptions.get(Options.CLIENT_SECRET)),
                    options.valueOf(mandatoryOptions.get(Options.AUDIENCE)),
                    options.valueOf(mandatoryOptions.get(Options.GRANT_TYPE)),
                    options.valueOf(mandatoryOptions.get(Options.SCOPE)),
                    "",
                    CliUtils.getCertificate(options.valueOf(mandatoryOptions.get(Options.VERIFY_CERT_BUNDLE))),
                    CliUtils.getHttpProxySettings(options.valueOf(mandatoryOptions.get(Options.HTTP_PROXY))));
            channelAuth.authenticate(httpRequest);
            Header authorization = httpRequest.getFirstHeader(AUTHORIZATION_HEADER_KEY);
            String token = getAuthorizationTokenFromHttpHeader(authorization);
            return new ExecutionResult("200", token, CliUtils.getCommandLine(options, mandatoryOptions));
        } catch (PermanentError | TemporaryError e) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage(), e);
        }

        return new ExecutionResult("", "", new HashMap<>());
    }

    /**
     * It returns token gotten from http header.
     *
     * @param authorization Authorization http header
     * @return token
     */
    private String getAuthorizationTokenFromHttpHeader(Header authorization) {
        if (authorization == null || authorization.getValue().isEmpty()) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser,
                    "Authorization Service did not return a authorization token");
        }

        final String rawToken = authorization.getValue();

        if (!rawToken.toLowerCase().startsWith(BEARER_TOKEN_TYPE.toLowerCase())) {
            CliUtils.printUsageAndFinish(CommandLineInterface.parser,
                    "Bearer token type is missing.");
        }

        return rawToken.substring(BEARER_TOKEN_TYPE.length()).trim();
    }
}
