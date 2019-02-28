/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.util.Optional;

/**
 * Request class is used to set up and send HTTP Post, Get and Delete requests to a given URI. It also keeps received
 * Cookies in the CookieStore of its HttpClientContext attribute.
 */
public class Request implements AutoCloseable {

    private final String base;
    private final ChannelAuth auth;

    private final CloseableHttpClient httpClient;
    private final HttpClientContext httpClientContext;

    /**
     * Request Constructor
     *
     * @param base scheme (http or https) and host parts of target URLs
     * @param auth provider of the Authorization token header to be included in the HttpRequest
     */
    Request(final String base, final ChannelAuth auth) {

        this.base = base;
        this.auth = auth;

        try {

            RequestConfig globalConfig = RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD_STRICT)
                    .build();

            this.httpClientContext = HttpClientContext.create();
            this.httpClientContext.setRequestConfig(globalConfig);
            this.httpClientContext.setCookieStore(new BasicCookieStore());

            // Create a session object so that we can store cookies across requests
            this.httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(globalConfig)
                    // Accept any certificate
                    .setSSLContext(new SSLContextBuilder()
                            .loadTrustMaterial(null, (certificate, authType) -> true).build())
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

        } catch (final Throwable e) {
            throw new TemporaryError("Unexpected temporary error " + e.getClass() + ": " + e.getMessage());
        }

    }

    /**
     * Send a POST request
     *
     * @param uri path plus query string components of the destination URL
     * @param body to include in the request
     * @return an HttpResponse
     * @throws TemporaryError if request was not successful
     */
    public HttpResponse post(final String uri, final Optional<byte[]> body) {

        HttpPost httpRequest = new HttpPost(base + uri);
        body.ifPresent(value -> httpRequest.setEntity(new ByteArrayEntity(value)));

        HttpResponse response = request(httpRequest);

        return response;

    }

    /**
     * Send a GET request
     *
     * @param uri path plus query string components of the destination URL
     * @return an HttpResponse
     * @throws TemporaryError if request was not successful
     */
    public HttpResponse get(final String uri) {

        HttpGet httpRequest = new HttpGet(base + uri);

        HttpResponse response = request(httpRequest);

        return response;

    }

    /**
     * Send a DELETE request
     *
     * @param uri path plus query string components of the destination URL
     * @return an HttpResponse
     * @throws TemporaryError if request was not successful
     */
    public HttpResponse delete(final String uri) {

        HttpDelete httpRequest = new HttpDelete(base + uri);

        HttpResponse response = request(httpRequest);

        return response;

    }

    /**
     * Send the HttpRequest (either POST or GET or DELETE)
     * It uses the internal HttpClient which stored the Cookie got when Consumer was created
     *
     * @param httpRequest
     * @return an HttpResponse object
     * @throws TemporaryError if the request throws exception or an HTTP Unauthorized (401) or Forbidden (403) response
     *         is received
     */
    private HttpResponse request(final HttpRequestBase httpRequest) {

        HttpResponse httpResponse;
        try {

            auth.authenticate(httpRequest);
            httpResponse = httpClient.execute(httpRequest, httpClientContext);

        } catch (final Throwable e) {
            throw new TemporaryError("Unexpected temporary error " + e.getClass() + ": " + e.getMessage());
        }

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode == 401 || statusCode == 403) {
            // Reset authorization attribute
            resetAuthorization();
            throw new TemporaryError("Token potentially expired (" + statusCode + "): "
                    + httpResponse.getStatusLine().getReasonPhrase() + " entity " + getString(httpResponse.getEntity(),
                    statusCode));

        }

        return httpResponse;

    }

    /**
     * Clean up session data which are sent in all requests, e.g.: delete cookies from HttpClientContext CookieStore
     */
    public void resetCookies() {

        httpClientContext.getCookieStore().clear();

    }

    /**
     * Clean up authorization data which is sent in all requests, e.g.: delete authorization token
     */
    public void resetAuthorization() {

        auth.reset();

    }


    /**
     * Closes the request object and its supporting HttpClient.
     *
     * This method is added to allow Request to be used in conjunction with Java try-with-resources statement.
     */
    @Override
    public void close() {

        // TODO: verify that Request object is not sending a request or awaiting for a response when closing it
        try {

            httpClient.close();
            resetCookies();
            resetAuthorization();

        } catch (final Exception e) {
            throw new TemporaryError("Unexpected temporary error " + e.getClass().getCanonicalName() + ": "
                    + e.getClass().getCanonicalName() + " " + e.getMessage());
        }

    }

    /**
     * Get a string from an HttpEntity.
     *
     * @param httpEntity entity to get a string from
     * @param httpStatusCode status code
     * @return String with HttpEntity contents
     * @throws TemporaryError if creating a string from the httpEntity fails. TemporaryError also contains the
     *         status code.
     */
    private static String getString(final HttpEntity httpEntity, final int httpStatusCode) {

        try {
            return EntityUtils.toString(httpEntity);
        } catch (final Exception e) {
            throw new TemporaryError("Unexpected temporary error " + httpStatusCode + ": "
                    + e.getClass().getCanonicalName() + " " + e.getMessage());
        }

    }

}
