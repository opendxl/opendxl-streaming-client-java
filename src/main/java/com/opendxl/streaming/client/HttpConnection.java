/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * The {@link HttpConnection} class provides an SSL communication layer for {@link Channel} requests.
 * Allowed certificates, which are used for HTTPS certificate validation, are given in a file which name is set in the
 * constructor {@link HttpConnection#HttpConnection(String, boolean, HttpProxySettings)}.
 */
public class HttpConnection implements AutoCloseable {

    /**
     * HttpClient instance that sends HTTP requests. It is setup in
     * {@link HttpConnection#HttpConnection(String, boolean, HttpProxySettings)}
     * constructor. It is used to send HTTP requests in {@link HttpConnection#execute(HttpRequestBase)} method.
     */
    private final CloseableHttpClient httpClient;
    /**
     * HttpClientContext instance which stores the Cookies got from HTTP responses.
     */
    private final HttpClientContext httpClientContext;

    //
    // Connection management
    //
    /**
     * The default connection pool maximum total of connections
     */
    private static final int DEFAULT_CONN_POOL_MAX_TOTAL = 100;
    /**
     * The default connection pool maximum total of connections per route
     */
    private static final int DEFAULT_CONN_POOL_MAX_PER_ROUTE = 100;

    //
    // Timeouts
    //
    /**
     * The default initial connect timeout
     */
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    /**
     * The default connection request timeout
     */
    private static final int DEFAULT_CONNECT_REQUEST_TIMEOUT = 5000;
    /**
     * The default socket timeout
     */
    private static final int DEFAULT_SOCKET_TIMEOUT = 5000;

    //
    // TLS
    //
    /**
     * The supported TLS protocols to use
     */
    private static final String[] SUPPORTED_PROTOCOLS = new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};

    /**
     * @param verifyCertBundle CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                         a path to a CA bundle file containing those certificates. The CA bundle is used to
     *                         validate that the certificate of the authentication server being connected to was signed
     *                         by a valid authority. If set to an empty string, the server certificate will not be
     *                         validated.
     * @param isHttps set to true if the connection requires SSL, false otherwise
     * @param httpProxySettings contains http proxy url, port, username and password
     * @throws KeyManagementException   If initialization of SSL context fails
     *
     * @throws KeyStoreException        If there is an error creating a key store from the certificates in the
     *                                  trust store file
     * @throws CertificateException     If there is an error creating a certificate from the certificates in the
     *                                  trust store file
     * @throws NoSuchAlgorithmException If there is an error creating a certificate factory
     * @throws IOException              If there is an error creating a key store from the certificates in
     *                                  the trust store file
     */
    public HttpConnection(final String verifyCertBundle, final boolean isHttps,
                          final HttpProxySettings httpProxySettings)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException,
            IOException {

        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();

        this.httpClientContext = HttpClientContext.create();
        this.httpClientContext.setRequestConfig(globalConfig);
        this.httpClientContext.setCookieStore(new BasicCookieStore());

        // Create a session object so that we can store cookies across requests
        final SSLConnectionSocketFactory socketFactory;
        if (isHttps) {
            socketFactory = createSSLConnectionSocketFactory(verifyCertBundle);
        } else {
            socketFactory = null;
        }

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(createRequestConfig())
                .setConnectionManager(createConnectionManager(socketFactory))
                .setRoutePlanner(createRoutePlanner(httpProxySettings))
                .setDefaultCredentialsProvider(createCredentialsProvider(httpProxySettings))
                .build();

    }

    /**
     * Create a socket factory based on the Certificate Bundle data.
     *
     * @param trustStoreData CA Bundle chain certificates. This string shall be either the certificates themselves or
     *                       a path to a CA bundle file containing those certificates. The CA bundle is used to validate
     *                       that the certificate of the authentication server being connected to was signed by a
     *                       valid authority. If set to an empty string, the server certificate will not be validated.
     * @return
     * @throws KeyManagementException   If initialization of SSL context fails
     *
     * @throws KeyStoreException        If there is an error creating a key store from the certificates in the
     *                                  trust store file
     * @throws CertificateException     If there is an error creating a certificate from the certificates in the
     *                                  trust store file
     * @throws NoSuchAlgorithmException If there is an error creating a certificate factory
     * @throws IOException              If there is an error creating a key store from the certificates in
     *                                  the trust store file
     */
    private SSLConnectionSocketFactory createSSLConnectionSocketFactory(final String trustStoreData)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            KeyManagementException {

        TrustManager[] tm;

        // Create a trust manager with the certificate chain
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        // By default host name validation is false
        boolean hostNameValidation = false;

        if (trustStoreData != null && !trustStoreData.isEmpty()) {
            tmf.init(createKeystore(trustStoreData));
            tm = tmf.getTrustManagers();
            hostNameValidation = true;
        } else {
            tm = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(
                                final X509Certificate[] chain, final String authType) throws CertificateException {
                        }

                        public void checkServerTrusted(
                                final X509Certificate[] chain, final String authType) throws CertificateException {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };
        }

        // Trust certs signed by the provided CA chain
        final SSLContext sslcontext = SSLContexts.createDefault();
        sslcontext.init(null, tm, null);

        SSLConnectionSocketFactory socketFactory;
        if (hostNameValidation) {
            // Allow TLSv1 protocol only
            socketFactory =
                    new SSLConnectionSocketFactory(
                            sslcontext, SUPPORTED_PROTOCOLS, null, new DefaultHostnameVerifier());
        } else {
            // Disable hostname verification
            socketFactory =
                    new SSLConnectionSocketFactory(
                            sslcontext, SUPPORTED_PROTOCOLS, null,
                            (hostName, session) -> true
                    );
        }

        return socketFactory;

    }

    /**
     * Creates a KeyStore for use with the HttpClient
     *
     * @param caChainPems the pems to be added in a single string format. If caChainPems string is the path to an
     *                    existing file, then certificates contained in the file will be loaded into the keystore.
     *                    If caChainPems string is not a path to an existing file, then caChainPems is expected to be
     *                    the certificates themselves and they will be loaded into the keystore.
     * @return The KeyStore
     * @throws KeyStoreException        If there is an error creating a key store from the certificates in the
     *                                  trust store file
     * @throws CertificateException     If there is an error creating a certificate from the certificates in the
     *                                  trust store file
     * @throws NoSuchAlgorithmException If there is an error creating a certificate factory
     * @throws IOException              If there is an error creating a key store from the certificates in
     *                                  the trust store file
     */
    private KeyStore createKeystore(final String caChainPems) throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {
        final KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);

        final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

        // Check whether certificateParameterValue is an existing filename
        boolean isFile;
        try {
            isFile = (new File(caChainPems)).isFile();
        } catch (final Exception e) {
            // certificateParameterValue is not an existing filename
            isFile = false;
        }

        java.util.Collection<? extends Certificate> chain;
        if (isFile) {
            try (FileInputStream fis = new FileInputStream(caChainPems);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                chain = certFactory.generateCertificates(bis);

            }
        } else {

            chain = certFactory.generateCertificates(new ByteArrayInputStream(caChainPems.getBytes()));

        }

        int certNumber = 0;
        for (Certificate caCert : chain) {
            clientKeyStore.setCertificateEntry(Integer.toString(certNumber), caCert);
            certNumber++;
        }

        return clientKeyStore;
    }

    /**
     * Creates and returns a {@link RequestConfig}
     *
     * @return A {@link RequestConfig} based on the specified descriptor properties
     */
    private RequestConfig createRequestConfig() {
        final RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        builder.setConnectionRequestTimeout(DEFAULT_CONNECT_REQUEST_TIMEOUT);
        builder.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
        builder.setCookieSpec(CookieSpecs.STANDARD);

        return builder.build();
    }

    /**
     * Creates and returns a {@link HttpClientConnectionManager}
     *
     * @param socketFactory The socket factory for TLS. pass null for non-TLS
     * @return A {@link HttpClientConnectionManager} based on the specified descriptor properties
     */
    private HttpClientConnectionManager createConnectionManager(SSLConnectionSocketFactory socketFactory) {
        final PoolingHttpClientConnectionManager cm;
        if (socketFactory != null) {
            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("https", socketFactory)
                            .register("http", PlainConnectionSocketFactory.INSTANCE)
                            .build();
            cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } else {
            cm = new PoolingHttpClientConnectionManager();
        }
        cm.setMaxTotal(DEFAULT_CONN_POOL_MAX_TOTAL);
        cm.setDefaultMaxPerRoute(DEFAULT_CONN_POOL_MAX_PER_ROUTE);
        return cm;
    }

    /**
     * Creates and returns a {@link HttpRoutePlanner} based on the specified descriptor properties
     *
     * @param httpProxySettings the http proxy url, port, user and password
     * @return A {@link HttpRoutePlanner} based on the specified http proxy settings; can be null if no http proxy is
     *         specified
     */
    private HttpRoutePlanner createRoutePlanner(final HttpProxySettings httpProxySettings) {
        DefaultProxyRoutePlanner routePlanner = null;

        if (httpProxySettings != null && httpProxySettings.isEnabled()) {
            HttpHost proxy = new HttpHost(httpProxySettings.getUrl(), httpProxySettings.getPort());
            routePlanner = new DefaultProxyRoutePlanner(proxy);
        }

        return routePlanner;
    }

    /**
     * Creates and returns a {@link CredentialsProvider} based on the specified descriptor properties
     *
     * @param httpProxySettings the http proxy url, port, user and password
     * @return A {@link CredentialsProvider} based on the specified http proxy settings
     */
    private CredentialsProvider createCredentialsProvider(final HttpProxySettings httpProxySettings) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        if (httpProxySettings != null && httpProxySettings.isEnabled()) {
            // if an HTTP proxy username has been provided add credentials for the HTTP proxy
            String proxyUsername = httpProxySettings.getUsername();
            if (proxyUsername != null && !proxyUsername.isEmpty()) {
                credsProvider.setCredentials(
                        new AuthScope(httpProxySettings.getUrl(), httpProxySettings.getPort()),
                        new UsernamePasswordCredentials(proxyUsername, httpProxySettings.getPassword()));
            }
        }

        return credsProvider;
    }

    /**
     * Execute the HTTP Request
     *
     * @param httpRequestBase http request to be executed
     * @return the response to the http request
     * @throws IOException in case of a problem or the connection was aborted
     * @throws ClientProtocolException in case of an http protocol error
     */
    public CloseableHttpResponse execute(final HttpRequestBase httpRequestBase)
            throws IOException, ClientProtocolException {

        return httpClient.execute(httpRequestBase, httpClientContext);

    }

    /**
     * Clean up session data which are sent in all requests, e.g.: delete cookies from HttpClientContext CookieStore
     */
    public void resetCookies() {
        httpClientContext.getCookieStore().clear();
    }

    /**
     * Closes the request object and its supporting HttpClient.
     *
     * This method is added to allow Request to be used in conjunction with Java try-with-resources statement.
     */
    public void close() throws IOException {

        httpClient.close();

    }
}
