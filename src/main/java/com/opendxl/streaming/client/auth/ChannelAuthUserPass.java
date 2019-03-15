/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.auth;

import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Optional;

/**
 * Authentication class where a token is retrieved from a base URL for the given user and password and such token is
 * later used in Authorization headers of channel requests.
 */
public class ChannelAuthUserPass implements ChannelAuth {

    private final String base;
    private final String username;
    private final String password;
    private final String pathFragment;
    private final String verifyCertBundle;
    private Optional<String> token;

    /**
     * @param base Base URL to forward authentication requests to. Its value will be prepended to pathFragment.
     * @param username User name to supply for request authentication.
     * @param password Password to supply for request authentication.
     * @param pathFragment Path to append to the base URL for the request.
     * @param verifyCertBundle Path to a CA bundle file containing
     *    certificates of trusted CAs. The CA bundle is used to validate that
     *    the certificate of the authentication server being connected to was
     *    signed by a valid authority. If set to an empty string, the server
     *    certificate is not validated.
     */
    public ChannelAuthUserPass(final String base, final String username, final String password,
                               Optional<String> pathFragment, final String verifyCertBundle) {

        this.base = base;
        this.username = username;
        this.password = password;
        this.pathFragment = pathFragment.orElse("/identity/v1/login");
        this.verifyCertBundle = verifyCertBundle == null ? "" : verifyCertBundle;
        token = Optional.ofNullable(null);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {

        token = Optional.ofNullable(null);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticate(final HttpRequest httpRequest)
            throws PermanentError, TemporaryError {

        if (!token.isPresent()) {
            // Ask for the token
            token = login(base, username, password, pathFragment, verifyCertBundle);
        }

        token.ifPresent(value -> httpRequest.addHeader("Authorization", "Bearer " + value));

    }

    /**
     * Make a login request to the supplied login url.
     *
     * @param uri Base URL at which to make the request.
     * @param username User name to supply for request authentication.
     * @param password Password to supply for request authentication.
     * @param pathFragment Path to append to the base URL for the request.
     * @param verifyCertBundle Path to a CA bundle file containing certificates of trusted CAs. The CA bundle is used
     *                        to validate that the certificate of the authentication server being connected to was
     *                        signed by a valid authority. If set to an empty string, the server certificate is not
     *                        validated.
     * @return an Optional containing the Authorization token if login succeeded
     *         an empty Optional otherwise
     *
     * @throws TemporaryError if an unexpected (but possibly recoverable) authentication error occurs for the request.
     * @throws PermanentError if the request fails due to the user not being authenticated successfully or if the user
     * is unauthorized to make the request or if a non-recoverable error occurs for the request.
     */
    private Optional<String> login(String uri, String username, String password, String pathFragment,
                                   String verifyCertBundle)
            throws PermanentError, TemporaryError {

        HttpGet httpGet = new HttpGet(uri + pathFragment);
        httpGet.setHeader("Accept", "application/xml");
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

        CloseableHttpClient client;
        try {
            // Create an httpClient which accepts any Certificate
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();

            client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();
        } catch (final Throwable e) {
            throw new PermanentError("Unexpected error: " + e.getMessage(), e);
        }


        HttpResponse response;
        String entityString;
        try {
            response = client.execute(httpGet);
            entityString = EntityUtils.toString(response.getEntity());
        } catch (final Throwable e) {
            throw new TemporaryError("Unexpected error: " + e.getMessage(), e);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {

            AuthorizationToken authorizationToken = new Gson().fromJson(entityString, AuthorizationToken.class);
            return Optional.ofNullable(authorizationToken.getAuthorizationToken());

        } else if (statusCode == 401 || statusCode == 403) {

            throw new PermanentError("Unauthorized " + statusCode + ": " + response.getEntity().toString());

        } else {

            throw new TemporaryError("Unexpected status code " + statusCode + ": " + response.getEntity().toString());

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
