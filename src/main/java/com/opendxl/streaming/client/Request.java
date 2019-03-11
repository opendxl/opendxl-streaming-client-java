/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.entity.ConsumerServiceError;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.ErrorType;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
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

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Optional;
import java.util.Map;

/**
 * <p>Request class is used to set up and send HTTP Post, Get and Delete requests to a given URI.</p>
 * <p>It also manages received Cookies and it includes them in sent requests. It uses
 * {@link org.apache.http.client.HttpClient} and {@link org.apache.http.client.protocol.HttpClientContext} classes.</p>
 */
public class Request implements AutoCloseable {

    private final String base;
    private final ChannelAuth auth;

    private final CloseableHttpClient httpClient;
    private final HttpClientContext httpClientContext;

    /**
     * @param base scheme (http or https) and host parts of target URLs. It will be prepended to uri parameter of
     *             {@link Request#post(String, Optional, Map)}, {@link Request#get(String, Map)} and
     *             {@link Request#delete(String, Map)} methods.
     * @param auth provider of the Authorization token header to be included in the HttpRequest
     * @throws TemporaryError if attempt to create and configure the HttpClient instance fails
     */
    public Request(final String base, final ChannelAuth auth) throws TemporaryError {

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
            throw new TemporaryError("Unexpected temporary error when instantiating Request"
                    + e.getClass() + ": " + e.getMessage(), e);
        }

    }

    /**
     * Send a POST request
     *
     * @param uri path plus query string components of the destination URL
     * @param body to include in the request. If body is {@link Optional#empty()}, then no entity body is added to the
     *             request.
     * @param httpStatusMapping map HTTP Status Code to {@link ErrorType}
     * @return the HttpResponse
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful and httpStatusMapping maps the response http status code to
     * a TemporaryError
     * @throws PermanentError if request was not successful and httpStatusMapping maps the response http status code to
     * a PermanentError
     */
    public Optional<String> post(final String uri, final Optional<byte[]> body,
                                 final Map<Integer, ErrorType> httpStatusMapping)
            throws ConsumerError, TemporaryError, PermanentError {

        HttpPost httpRequest = new HttpPost(base + uri);
        body.ifPresent(value -> httpRequest.setEntity(new ByteArrayEntity(value)));

        return request(httpRequest, httpStatusMapping);

    }

    /**
     * Send a GET request
     *
     * @param uri path plus query string components of the destination URL
     * @param httpStatusMapping map HTTP Status Code to {@link ErrorType}
     * @return the HttpResponse
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful and httpStatusMapping maps the response http status code to
     * a TemporaryError
     * @throws PermanentError if request was not successful and httpStatusMapping maps the response http status code to
     * a PermanentError
     */
    public Optional<String> get(final String uri, final Map<Integer, ErrorType> httpStatusMapping) throws ConsumerError,
            TemporaryError, PermanentError {

        HttpGet httpRequest = new HttpGet(base + uri);

        return request(httpRequest, httpStatusMapping);

    }

    /**
     * Send a DELETE request
     *
     * @param uri path plus query string components of the destination URL
     * @param httpStatusMapping map HTTP Status Code to {@link ErrorType}
     * @return the HttpResponse
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful and httpStatusMapping maps the response http status code to
     * a TemporaryError
     * @throws PermanentError if request was not successful and httpStatusMapping maps the response http status code to
     * a PermanentError
     */
    public Optional<String> delete(final String uri, final Map<Integer, ErrorType> httpStatusMapping)
            throws ConsumerError, TemporaryError, PermanentError {

        HttpDelete httpRequest = new HttpDelete(base + uri);

        return request(httpRequest, httpStatusMapping);

    }

    /**
     * Send the HttpRequest (either POST or GET or DELETE)
     * It uses the internal HttpClient which stored the Cookie got when Consumer was created
     *
     * @param httpRequest
     * @param httpStatusMapping map HTTP Status Code to {@link ErrorType}
     * @return the entity string of the HttpResponse object
     * @throws ConsumerError if request was not successful and httpStatusMapping maps the response http status code to a
     * ConsumerError
     * @throws TemporaryError if request was not successful and httpStatusMapping maps the response http status code to
     * a TemporaryError
     * @throws PermanentError if request was not successful and httpStatusMapping maps the response http status code to
     * a PermanentError
     */
    private Optional<String> request(final HttpRequestBase httpRequest, final Map<Integer, ErrorType> httpStatusMapping)
            throws ConsumerError, TemporaryError, PermanentError {


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
                    + e.getClass().getCanonicalName() + ": " + e.getMessage(), e, 0, httpRequest);
            throw temporaryError;
        }

        // Evaluate Response HTTP Status Code
        if (isSuccess(statusCode)) {

            return returnValue;

        } else if (httpStatusMapping.containsKey(statusCode)) {

            // http status code maps to an error. Then an exception must be thrown.
            ErrorType errorType = httpStatusMapping.get(statusCode);
            // Create the error message
            Gson gson = new Gson();
            String message = "";
            if (returnValue.isPresent()) {
                message = getConsumerServiceErrorMessage(returnValue.get());
            }

            // throw suitable exception
            if (errorType == ErrorType.CONSUMER_ERROR) {
                throw new ConsumerError(message, statusCode, httpRequest);
            } else if (errorType == ErrorType.TEMPORARY_ERROR) {
                throw new TemporaryError(message, statusCode, httpRequest);
            } else {
                throw new PermanentError(message, statusCode, httpRequest);
            }

        } else {
            throw new TemporaryError("Unexpected temporary error", statusCode, httpRequest);
        }

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
     *
     * @throws TemporaryError if an error occurs when closing the request object.
     */
    @Override
    public void close() throws TemporaryError {

        try {

            httpClient.close();
            resetCookies();
            resetAuthorization();

        } catch (final IOException e) {
            throw new TemporaryError("Unexpected temporary error " + e.getClass().getCanonicalName() + ": "
                    + " " + e.getMessage(), e);
        }

    }

    /**
     * Get a string from an HttpEntity.
     *
     * @param httpEntity entity to get a string from
     * @return String with HttpEntity contents
     */
    private static String getString(final HttpEntity httpEntity) throws TemporaryError {

        try {
            return EntityUtils.toString(httpEntity);
        } catch (final IOException | ParseException e) {
            return "'failure when when parsing HttpEntity'";
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
