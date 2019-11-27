/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.auth;

import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.HttpConnection;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.HttpStatusCodes;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.nio.charset.Charset;
import java.util.Base64;

/**
 * Authentication class where a token is retrieved from a base URL for the given user and password and such token is
 * later used in Authorization headers of channel requests.
 */
public class ChannelAuthUserPass implements ChannelAuth {

    /**
     * The logger
     */
    private Logger logger = LoggerFactory.getLogger(ChannelAuthUserPass.class);

    private final String base;
    private final String username;
    private final String password;
    private final String pathFragment;
    private final String verifyCertBundle;
    private final HttpProxySettings httpProxySettings;
    private final boolean isHttps;
    private String token;

    /**
     * @param base Base URL to forward authentication requests to. Its value will be prepended to pathFragment.
     * @param username User name to supply for request authentication.
     * @param password Password to supply for request authentication.
     * @param pathFragment Path to append to the base URL for the request.
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @param httpProxySettings http proxy url, port, username and password
     * @throws PermanentError if base is null or empty or if username is null or empty or if password is null
     */
    public ChannelAuthUserPass(final String base, final String username, final String password, String pathFragment,
                               final String verifyCertBundle, final HttpProxySettings httpProxySettings)
            throws PermanentError {

        if (base == null || base.isEmpty()) {
            throw new PermanentError("Base URL may not be null or empty");
        }

        if (username == null || username.isEmpty()) {
            throw new PermanentError("username may not be null or empty");
        }

        if (password == null) {
            throw new PermanentError("password may not be null");
        }

        this.base = base;
        this.username = username;
        this.password = password;
        this.pathFragment = pathFragment != null ? pathFragment : "/identity/v1/login";
        this.verifyCertBundle = verifyCertBundle == null ? "" : verifyCertBundle;
        this.isHttps = base.toLowerCase().startsWith("https");
        this.httpProxySettings = httpProxySettings;
        token = null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {

        token = null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticate(final HttpRequest httpRequest)
            throws PermanentError, TemporaryError {

        if (token == null) {
            // Ask for the token
            token = login(base, username, password, pathFragment, verifyCertBundle);
            if (logger.isDebugEnabled()) {
                logger.debug("Got token " + token);
            }
        }

        if (token != null) {
            // Add authorization header with token to request
            httpRequest.addHeader("Authorization", "Bearer " + token);
            if (logger.isDebugEnabled()) {
                logger.debug("Added Authorization header: Bearer " + token);
            }
        }

    }

    /**
     * Make a login request to the supplied login url.
     *
     * @param uri Base URL at which to make the request.
     * @param username User name to supply for request authentication.
     * @param password Password to supply for request authentication.
     * @param pathFragment Path to append to the base URL for the request.
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @return a String containing the Authorization token if login succeeded
     *
     * @throws TemporaryError if an unexpected (but possibly recoverable) authentication error occurs for the request.
     * @throws PermanentError if the request fails due to the user not being authenticated successfully or if the user
     * is unauthorized to make the request or if a non-recoverable error occurs for the request.
     */
    private String login(String uri, String username, String password, String pathFragment,
                                   String verifyCertBundle)
            throws PermanentError, TemporaryError {

        HttpGet httpGet = new HttpGet(uri + pathFragment);
        httpGet.setHeader("Accept", "application/xml");
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

        HttpConnection httpConnection;
        try {
            // Create an http connection which client will use the given Certificate Bundle data
            httpConnection = new HttpConnection(verifyCertBundle, isHttps, httpProxySettings);
        } catch (final Throwable e) {
            throw new PermanentError("Unexpected error: " + e.getMessage(), e);
        }


        HttpResponse response;
        String entityString;
        try {
            response = httpConnection.execute(httpGet);
            entityString = EntityUtils.toString(response.getEntity());
        } catch (final Throwable e) {
            throw new TemporaryError("Unexpected error: " + e.getMessage(), e);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (HttpStatusCodes.isSuccess(statusCode)) {

            AuthorizationToken authorizationToken = new Gson().fromJson(entityString, AuthorizationToken.class);
            return authorizationToken.getAuthorizationToken();

        } else if (statusCode == HttpStatusCodes.UNAUTHORIZED.getCode()
                || statusCode == HttpStatusCodes.FORBIDDEN.getCode()) {

            String error = "Unauthorized " + statusCode + ": " + entityString;
            logger.error(error);
            throw new PermanentError(error);

        } else {

            String error = "Unexpected status code " + statusCode + ": " + entityString;
            logger.error(error);
            throw new TemporaryError(error);

        }

    }

}

// Helper class used to deserialize JSON objects
class AuthorizationToken {

    @SerializedName(value = "authorizationToken", alternate = {"AuthorizationToken"})
    private String authorizationToken;

    AuthorizationToken(final String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(final String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

}
