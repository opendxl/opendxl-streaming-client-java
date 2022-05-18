package com.opendxl.streaming.client.auth;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.HttpConnection;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.HttpStatusCodes;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChannelAuthClientCredentialSecret implements ChannelAuth {

    /**
     * The logger
     */
    private Logger logger = LogManager.getLogger(ChannelAuthUserPass.class);

    private final String base;
    private final String clientId;
    private final String clientSecret;
    private final String pathFragment;
    private final String verifyCertBundle;
    private final HttpProxySettings httpProxySettings;
    private final boolean isHttps;
    private String token;
    private final String audience;
    private final String grantType;
    private final String scope;

    /**
     * @param base              Base URL to forward authentication requests to. Its
     *                          value will be prepended to pathFragment.
     * @param clientId          Client Id to supply for request authentication.
     * @param clientSecret      Client Secret to supply for request authentication.
     * @param audience          audience to supply for request authentication, eg. "IAM+AuthZ", "mcafee"
     * @param grantType         grant type to supply for request authentication, eg. "client_credentials"
     * @param scope             scope to supply for request authentication, Its a space separated list of scopes.
     * @param pathFragment      Path to append to the base URL for the request.
     * @param verifyCertBundle  CA Bundle chain certificates. This string shall be
     *                          either the certificates themselves or
     *                          a path to a CA bundle file containing those
     *                          certificates. The CA bundle is used to
     *                          validate that the certificate of the authentication
     *                          server being connected to was signed
     *                          by a valid authority. If set to an empty string, the
     *                          server certificate will not be
     *                          validated.
     * @param httpProxySettings http proxy url, port, username and password
     * @throws PermanentError if base is null or empty or if username is null or
     *                        empty or if password is null
     */
    public ChannelAuthClientCredentialSecret(final String base, final String clientId, final String clientSecret,
            final String audience, final String grantType, final String scope,
            final String pathFragment, final String verifyCertBundle, final HttpProxySettings httpProxySettings)
            throws PermanentError {

        if (base == null || base.isEmpty()) {
            throw new PermanentError("Base URL may not be null or empty");
        }

        if (clientId == null || clientId.isEmpty()) {
            throw new PermanentError("client id may not be null or empty");
        }

        if (clientSecret == null) {
            throw new PermanentError("client secret may not be null");
        }

        if (audience == null) {
            throw new PermanentError("audience may not be null");
        }

        if (grantType == null) {
            throw new PermanentError("grant_type may not be null");

        }
        if (scope == null) {
            throw new PermanentError("scope may not be null");
        }

        this.base = base;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.pathFragment = pathFragment != null ? pathFragment : "/iam/v1.4/token";
        this.verifyCertBundle = verifyCertBundle == null ? "" : verifyCertBundle;
        this.isHttps = base.toLowerCase().startsWith("https");
        this.httpProxySettings = httpProxySettings;
        this.audience = audience;
        this.grantType = grantType;
        this.scope = scope;
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
            token = getToken(base, clientId, clientSecret, audience, grantType, scope, pathFragment, verifyCertBundle);
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
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        token = null;
    }

    /**
     * Make a login request to the supplied login url.
     *
     * @param uri              Base URL at which to make the request.
     * @param clientId         Client Id to supply for request authentication.
     * @param clientSecret     Client Secret to supply for request authentication.
     * @param audience         audience to supply for request authentication, eg. "IAM+AuthZ", "mcafee"
     * @param grantType        grant type to supply for request authentication, eg. "client_credentials"
     * @param scope            scope to supply for request authentication, Its a space separated list of scopes.
     * @param pathFragment     Path to append to the base URL for the request.
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be
     *                         either the certificates themselves or
     *                         a path to a CA bundle file containing those
     *                         certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication
     *                         server being connected to was signed
     *                         by a valid authority. If set to an empty string, the
     *                         server certificate will not be
     *                         validated.
     * @return a String containing the Authorization token if login succeeded
     *
     * @throws TemporaryError if an unexpected (but possibly recoverable)
     *                        authentication error occurs for the request.
     * @throws PermanentError if the request fails due to the user not being
     *                        authenticated successfully or if the user
     *                        is unauthorized to make the request or if a
     *                        non-recoverable error occurs for the request.
     */
    private String getToken(String uri, String clientId, String clientSecret, final String audience,
        final String grantType, final String scope, final String pathFragment, final String verifyCertBundle)
        throws PermanentError, TemporaryError {
        HttpPost httpPost = new HttpPost(uri + pathFragment);
        httpPost.setHeader("Accept", "application/xml");
        String auth = clientId + ":" + clientSecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
        String authHeader = "Basic " + new String(encodedAuth);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

        NameValuePair audienceNVP = new BasicNameValuePair("audience", audience);
        NameValuePair grantTypeNVP = new BasicNameValuePair("grant_type", grantType);
        NameValuePair scopeNVP = new BasicNameValuePair("scope", scope);
        HttpEntity entity = EntityBuilder.create().setParameters(Arrays.asList(audienceNVP, grantTypeNVP, scopeNVP))
        .build();
        httpPost.setEntity(entity);

        HttpConnection httpConnection;
        try {
            // Create an http connection which client will use the given Certificate Bundle
            // data
            httpConnection = new HttpConnection(verifyCertBundle, isHttps, httpProxySettings);
        } catch (final Throwable e) {
            throw new PermanentError("Unexpected error: " + e.getMessage(), e);
        }

        HttpResponse response;
        String entityString;
        try {
            response = httpConnection.execute(httpPost);
            entityString = EntityUtils.toString(response.getEntity());
        } catch (final Throwable e) {
            throw new TemporaryError("Unexpected error: " + e.getMessage(), e);
        } finally {
            if (httpConnection != null) {
                try {
                    httpConnection.close();
                } catch (Exception e) {
                    //Eat it
                }
            }
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (HttpStatusCodes.isSuccess(statusCode)) {

            AccessToken authorizationToken = new Gson().fromJson(entityString, AccessToken.class);
            return authorizationToken.getAccessToken();
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
class AccessToken {

    @SerializedName(value = "access_token", alternate = {"Access_Token"})
    private String accessToken;

    AccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }
}