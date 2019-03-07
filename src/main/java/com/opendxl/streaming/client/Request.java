/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.google.gson.Gson;
import com.opendxl.streaming.client.entity.ConsumerServiceError;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.TemporaryError;
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

    private Optional<String> consumerId;

    /**
     * Request Constructor
     *
     * @param base scheme (http or https) and host parts of target URLs
     * @param auth provider of the Authorization token header to be included in the HttpRequest
     * @throws TemporaryError if attempt to create and configure the HttpClient instance fails
     */
    Request(final String base, final ChannelAuth auth) throws TemporaryError {

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
            TemporaryError temporaryError = new TemporaryError("Unexpected temporary error when instantiating Request"
                    + e.getClass() + ": " + e.getMessage());
            temporaryError.setCause(e);
            throw temporaryError;
        }

    }

    /**
     * Set the consumerId. It is only used in error messages.
     *
     * @param consumerId the consumer identifier given by ConsumerService
     */
    public void setConsumerId(final String consumerId) {

        this.consumerId = Optional.ofNullable(consumerId);

    }

    /**
     * Send a POST request
     *
     * @param uri path plus query string components of the destination URL
     * @param body to include in the request
     * @return an HttpResponse
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful
     */
    public Optional<String> post(final String uri, final Optional<byte[]> body) throws ConsumerError, TemporaryError {

        HttpPost httpRequest = new HttpPost(base + uri);
        body.ifPresent(value -> httpRequest.setEntity(new ByteArrayEntity(value)));

        return request(httpRequest);

    }

    /**
     * Send a GET request
     *
     * @param uri path plus query string components of the destination URL
     * @return an HttpResponse
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful
     */
    public Optional<String> get(final String uri) throws ConsumerError, TemporaryError {

        HttpGet httpRequest = new HttpGet(base + uri);

        return request(httpRequest);

    }

    /**
     * Send a DELETE request
     *
     * @param uri path plus query string components of the destination URL
     * @return an HttpResponse
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful
     */
    public Optional<String> delete(final String uri) throws ConsumerError, TemporaryError {

        HttpDelete httpRequest = new HttpDelete(base + uri);

        return request(httpRequest);

    }

    /**
     * Send the HttpRequest (either POST or GET or DELETE)
     * It uses the internal HttpClient which stored the Cookie got when Consumer was created
     *
     * @param httpRequest
     * @return an HttpResponse object
     * @throws ConsumerError if the request throws exception or an HTTP Not Found (404) or Conflict (409) response
     *         or Internal Server Error (500) is received
     * @throws TemporaryError if the request throws exception or an HTTP Unauthorized (401) or Forbidden (403) response
     *         is received
     */
    private Optional<String> request(final HttpRequestBase httpRequest) throws ConsumerError, TemporaryError {

        HttpResponse httpResponse;
        int statusCode = 0;
        Optional<String> returnValue = Optional.empty();

        try {

            auth.authenticate(httpRequest);
            httpResponse = httpClient.execute(httpRequest, httpClientContext);
            statusCode = httpResponse.getStatusLine().getStatusCode();

            if (httpResponse.getEntity() != null) {
                returnValue = Optional.ofNullable(EntityUtils.toString(httpResponse.getEntity()));
            }

        } catch (final Throwable e) {
            TemporaryError temporaryError = new TemporaryError("Unexpected temporary error "
                    + e.getClass().getCanonicalName() + ": " + e.getMessage());
            temporaryError.setCause(e);
            temporaryError.setHttpRequest(httpRequest);
            temporaryError.setStatusCode(statusCode);
            throw temporaryError;
        }

        // Evaluate Response HTTP Status Code
        if (isSuccess(statusCode)) {

            return returnValue;

        } else if (statusCode == 401 || statusCode == 403) {
            // Reset authorization attribute
            resetAuthorization();
            TemporaryError temporaryError = new TemporaryError("Token potentially expired (" + statusCode + "): "
                    + httpResponse.getStatusLine().getReasonPhrase() + " entity "
                    + getString(httpResponse.getEntity(), statusCode));
            temporaryError.setApi(httpRequest.getMethod() + " " + httpRequest.getURI());
            temporaryError.setStatusCode(statusCode);
            temporaryError.setCause(null);

            throw temporaryError;

        } else if (shouldRecreateConsumer(statusCode)) {

            ConsumerError consumerError = new ConsumerError("Consumer " + consumerId.orElse("unknown")
                    + " does not exist - Status code: " + statusCode);
            consumerError.setApi(httpRequest.getMethod() + " " + httpRequest.getURI());
            consumerError.setStatusCode(statusCode);
            consumerError.setCause(null);

            throw consumerError;

        } else {
            // any other Response HTTP Status Code
            TemporaryError temporaryError = new TemporaryError("Unexpected temporary error " + statusCode + ": "
                    + getConsumerServiceErrorMessage(returnValue.get()));
            temporaryError.setApi(httpRequest.getMethod() + " " + httpRequest.getURI());
            temporaryError.setStatusCode(statusCode);
            temporaryError.setCause(null);

            throw temporaryError;

        }

    }

    /**
     * Clean up session data which are sent in all requests, e.g.: delete cookies from HttpClientContext CookieStore
     */
    public void resetCookies() {

        httpClientContext.getCookieStore().clear();
        consumerId = Optional.empty();

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
     *
     * @throws TemporaryError if an error occurs when closing the request object.
     */
    @Override
    public void close() throws TemporaryError {

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
    private static String getString(final HttpEntity httpEntity, final int httpStatusCode) throws TemporaryError {

        try {
            return EntityUtils.toString(httpEntity);
        } catch (final Exception e) {
            throw new TemporaryError("Unexpected temporary error " + httpStatusCode + ": "
                    + e.getClass().getCanonicalName() + " " + e.getMessage());
        }

    }

    /**
     * Checks whether a status code is successful one
     *
     * @param statusCode an HTTP Response Status Code
     * @return true if status code belongs to 2xx Success range
     *         false otherwise
     */
    private static boolean isSuccess(final int statusCode) {

        return statusCode >= 200 && statusCode < 300;

    }


    /**
     * Checks whether a status code is a consumer error one.
     *
     * An error is considered a consumer one if such error can be overcome by using a brand new consumer instead of the
     * current one.
     *
     * @param statusCode an HTTP Response Status Code
     * @return true if status code is 404 Bad Request or 409 Conflict or 500 Internal Server Error
     *         or 503 Server Unavailable
     *         false otherwise
     */
    private boolean shouldRecreateConsumer(final int statusCode) {

        return statusCode == 404 || statusCode == 409 || statusCode == 500 || statusCode == 503;

    }

    /**
     * Get the error message of a ConsumerServiceError object in JSON format.
     *
     * @param responseEntityString string representing a ConsumerServiceError in JSON format
     * @return String with error message
     */
    private static String getConsumerServiceErrorMessage(final String responseEntityString) {

        Gson gson = new Gson();
        ConsumerServiceError apiGatewayError = (ConsumerServiceError) gson.fromJson(responseEntityString,
                ConsumerServiceError.class);

        return apiGatewayError != null
                ? apiGatewayError.getMessage()
                : responseEntityString;

    }

}
