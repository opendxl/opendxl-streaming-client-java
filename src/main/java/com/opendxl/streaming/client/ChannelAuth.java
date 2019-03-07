/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.google.gson.Gson;
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
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

/**
 * Authentication class for use with channel requests.
 */
public class ChannelAuth {

    private final String base;
    private final String username;
    private final String password;
    private final String pathFragment;
    private final String verifyCertBundle;
    private Optional<String> token;

    /**
     * Constructor parameters:
     *
     * @param base Base URL to forward authentication requests to.
     * @param username User name to supply for request authentication.
     * @param password Password to supply for request authentication.
     * @param pathFragment Path to append to the base URL for the request.
     * @param verifyCertBundle Path to a CA bundle file containing
     *    certificates of trusted CAs. The CA bundle is used to validate that
     *    the certificate of the authentication server being connected to was
     *    signed by a valid authority. If set to an empty string, the server
     *    certificate is not validated.
     */
    public ChannelAuth(final String base, final String username, final String password, Optional<String> pathFragment,
                       final String verifyCertBundle) {

        this.base = base;
        this.username = username;
        this.password = password;
        this.pathFragment = pathFragment.orElse("/identity/v1/login");
        this.verifyCertBundle = verifyCertBundle == null ? "" : verifyCertBundle;
        token = Optional.ofNullable(null);

    }

    /**
     * Purge any credentials cached from a previous authentication.
     */
    public void reset() {

        token = Optional.ofNullable(null);

    }


    /**
     * Adds an http header with the Authorization token to the given http request
     *
     * @param httpRequest request where to add the Authorization header
     * @throws IOException exception that might be thrown by CloseableHttpClient.execute()
     * @throws KeyStoreException exception that might be thrown by SSLContextBuilder.loadTrustMaterial()
     * @throws NoSuchAlgorithmException exception that might be thrown by SSLContextBuilder.loadTrustMaterial()
     * @throws KeyManagementException exception that might be thrown by SSLContextBuilder.loadTrustMaterial.build()
     */
    public void authenticate(final HttpRequest httpRequest)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

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
     * @throws IOException exception that might be thrown by CloseableHttpClient.execute()
     * @throws KeyStoreException exception that might be thrown by SSLContextBuilder.loadTrustMaterial()
     * @throws NoSuchAlgorithmException exception that might be thrown by SSLContextBuilder.loadTrustMaterial()
     * @throws KeyManagementException exception that might be thrown by SSLContextBuilder.loadTrustMaterial.build()
     */
    private Optional<String> login(String uri, String username, String password, String pathFragment,
                                   String verifyCertBundle)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        // Create an httpClient which accepts any Certificate
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (certificate, authType) -> true).build();

        CloseableHttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        HttpGet httpGet = new HttpGet(uri + pathFragment);
        httpGet.setHeader("Accept", "application/xml");
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

        HttpResponse response = client.execute(httpGet);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {

            String entityString = EntityUtils.toString(response.getEntity());
            AuthorizationToken authorizationToken = new Gson().fromJson(entityString, AuthorizationToken.class);

            return Optional.ofNullable(authorizationToken.getAuthorizationToken());

        } else {

            return Optional.empty();

        }

    }

}

