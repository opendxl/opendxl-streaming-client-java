/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.entity.ConsumerServiceError;
import com.opendxl.streaming.client.exception.ConsumerError;
import com.opendxl.streaming.client.exception.ErrorType;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

/**
 * <p>Request class is used to set up and send HTTP Post, Get and Delete requests to a given URI.</p>
 * <p>It also manages received Cookies and it includes them in sent requests. It uses
 * {@link org.apache.http.client.HttpClient} and {@link org.apache.http.client.protocol.HttpClientContext} classes.</p>
 */
public class Request implements AutoCloseable {

    private final String base;
    private final ChannelAuth auth;

    /**
     * Helper object which provides SSL connections and Http Proxy support for sending HTTP requests. SSL connection
     * uses the certificates from the Certificate Bundle data passed to
     * {@link Request#Request(String, ChannelAuth, String, boolean, HttpProxySettings)} constructor.
     */
    private final HttpConnection httpConnection;

    /**
     * The logger
     */
    private static Logger logger = LoggerFactory.getLogger(Request.class);

    /**
     * @param base scheme (http or https) and host parts of target URLs. It will be prepended to uri parameter of
     *             {@link Request#post(String, byte[], Map)}, {@link Request#get(String, Map)} and
     *             {@link Request#delete(String, Map)} methods.
     * @param auth provider of the Authorization token header to be included in the HttpRequest
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @param isHttps set to true if the connection requires SSL, false otherwise
     * @param httpProxySettings contains http proxy url, port, username and password
     * @throws TemporaryError if attempt to create and configure the HttpClient instance fails
     */
    public Request(final String base, final ChannelAuth auth, final String verifyCertBundle, boolean isHttps,
                   final HttpProxySettings httpProxySettings) throws TemporaryError {

        this.base = base;
        this.auth = auth;

        try {

            this.httpConnection = new HttpConnection(verifyCertBundle, isHttps, httpProxySettings);

        } catch (final Throwable e) {
            String error = "Unexpected temporary error when instantiating Request" + e.getClass() + ": "
                    + e.getMessage();
            logger.error(error);
            throw new TemporaryError(error, e);
        }

    }

    /**
     * Send a POST request
     *
     * @param uri path plus query string components of the destination URL
     * @param body to include in the request. If body is {@code null}, then no entity body is added to the request.
     * @param httpStatusMapping map HTTP Status Code to {@link ErrorType}
     * @return the entity string of the HttpResponse object
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful and httpStatusMapping maps the response http status code to
     * a TemporaryError
     * @throws PermanentError if request was not successful and httpStatusMapping maps the response http status code to
     * a PermanentError
     */
    public String post(final String uri, final byte[] body,
                                 final Map<Integer, ErrorType> httpStatusMapping)
            throws ConsumerError, TemporaryError, PermanentError {

        HttpPost httpRequest = new HttpPost(base + uri);
        if (body != null) {
            httpRequest.setEntity(new ByteArrayEntity(body));
        }

        return request(httpRequest, httpStatusMapping);

    }

    /**
     * Send a GET request
     *
     * @param uri path plus query string components of the destination URL
     * @param httpStatusMapping map HTTP Status Code to {@link ErrorType}
     * @return the entity string of the HttpResponse object
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful and httpStatusMapping maps the response http status code to
     * a TemporaryError
     * @throws PermanentError if request was not successful and httpStatusMapping maps the response http status code to
     * a PermanentError
     */
    public String get(final String uri, final Map<Integer, ErrorType> httpStatusMapping) throws ConsumerError,
            TemporaryError, PermanentError {

        HttpGet httpRequest = new HttpGet(base + uri);

        return request(httpRequest, httpStatusMapping);

    }

    /**
     * Send a DELETE request
     *
     * @param uri path plus query string components of the destination URL
     * @param httpStatusMapping map HTTP Status Code to {@link ErrorType}
     * @return the entity string of the HttpResponse object
     * @throws ConsumerError if consumer was not found
     * @throws TemporaryError if request was not successful and httpStatusMapping maps the response http status code to
     * a TemporaryError
     * @throws PermanentError if request was not successful and httpStatusMapping maps the response http status code to
     * a PermanentError
     */
    public String delete(final String uri, final Map<Integer, ErrorType> httpStatusMapping)
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
    private String request(final HttpRequestBase httpRequest, final Map<Integer, ErrorType> httpStatusMapping)
            throws ConsumerError, TemporaryError, PermanentError {


        HttpResponse httpResponse;
        int statusCode = 0;
        String returnValue = null;

        auth.authenticate(httpRequest);
        try {

            httpResponse = httpConnection.execute(httpRequest);
            statusCode = httpResponse.getStatusLine().getStatusCode();

            if (httpResponse.getEntity() != null) {
                returnValue = EntityUtils.toString(httpResponse.getEntity());
            }

        } catch (final Throwable e) {
            throw new TemporaryError("Unexpected temporary error "
                    + e.getClass().getCanonicalName() + ": " + e.getMessage(), e, 0, httpRequest, null);
        }

        // Evaluate Response HTTP Status Code
        if (HttpStatusCodes.isSuccess(statusCode)) {

            return returnValue;

        } else if (httpStatusMapping.containsKey(HttpStatusCodes.getHttpStatus(statusCode))) {

            // http status code maps to an error. Then an exception must be thrown.
            ErrorType errorType = httpStatusMapping.get(HttpStatusCodes.getHttpStatus(statusCode));
            // Create the error message
            String message = "";
            if (returnValue != null) {
                message = getConsumerServiceErrorMessage(returnValue) + ": " + httpResponse.getStatusLine();
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
            throw new TemporaryError("Unexpected temporary error" + ": " + httpResponse.getStatusLine(),
                    statusCode, httpRequest);
        }

    }

    /**
     * Clean up session data which are sent in all requests, e.g.: delete cookies from HttpClientContext CookieStore
     */
    public void resetCookies() {

        httpConnection.resetCookies();

    }

    /**
     * Clean up authorization data which is sent in all requests, e.g.: delete authorization token
     */
    public void resetAuthorization() {

        auth.reset();

    }


    /**
     * Closes the request object and its supporting HttpConnection.
     *
     * This method is added to allow Request to be used in conjunction with Java try-with-resources statement.
     *
     * @throws TemporaryError if an error occurs when closing the request object.
     */
    @Override
    public void close() throws TemporaryError {

        try {

            httpConnection.close();
            resetCookies();
            resetAuthorization();

        } catch (final IOException e) {
            String error = "Unexpected temporary error " + e.getClass().getCanonicalName() + ": " + e.getMessage();
            logger.error(error);
            throw new TemporaryError(error, e);
        }

    }

    /**
     * Get the error message of a ConsumerServiceError object in JSON format.
     *
     * @param responseEntityString string representing a ConsumerServiceError in JSON format
     * @return String with error message
     */
    private static String getConsumerServiceErrorMessage(final String responseEntityString) {

        Gson gson = new Gson();
        ConsumerServiceError apiGatewayError;
        try {
            apiGatewayError = gson.fromJson(responseEntityString,
                    ConsumerServiceError.class);
        } catch (final Exception e) {
            apiGatewayError = null;
        }

        return apiGatewayError != null
                ? apiGatewayError.getMessage()
                : responseEntityString;

    }

}
