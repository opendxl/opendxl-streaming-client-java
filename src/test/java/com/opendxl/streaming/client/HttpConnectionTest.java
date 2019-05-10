/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import junit.extensions.PA;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertificateException;

import java.util.Set;

public class HttpConnectionTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public final void testHttpConnectionSuccessfullyAcceptsCertificateInStringFormat() throws Exception {

        // Test
        HttpConnection httpConnection = new HttpConnection(CERTIFICATE, true, null);

        // Evaluate
        CloseableHttpClient httpClient = (CloseableHttpClient) PA.getValue(httpConnection, "httpClient");
        Assert.assertTrue(httpClient != null);

    }

    @Test
    public final void testHttpConnectionSuccessfullyAcceptsCertificateFile() throws Exception {
        // Setup
        final String certificateFilename = "certificate.crt";
        final File certificateFile = folder.newFile(certificateFilename);
        Files.write(Paths.get(certificateFile.getAbsolutePath()), CERTIFICATE.getBytes(), StandardOpenOption.APPEND);

        // Test
        HttpConnection httpConnection = new HttpConnection(certificateFile.getAbsolutePath(), true, null);

        // Evaluate
        CloseableHttpClient httpClient = (CloseableHttpClient) PA.getValue(httpConnection, "httpClient");
        Assert.assertTrue(httpClient != null);
    }

    @Test
    public final void testHttpConnectionSuccessfullyAcceptsNoCertificateData() throws Exception {

        // Test
        HttpConnection httpConnection = new HttpConnection(null, true, null);

        // Evaluate
        CloseableHttpClient httpClient = (CloseableHttpClient) PA.getValue(httpConnection, "httpClient");
        Assert.assertTrue(httpClient != null);
    }

    @Test
    public final void testHttpConnectionFailsWhenBadCertificate() throws Exception {
        // Setup
        final String badCertificate = CERTIFICATE.substring("-----BEGIN CERTIFICATE-----\n".length());

        // Test
        CertificateException exception = null;
        try {
            new HttpConnection(badCertificate, true, null);
        } catch (final CertificateException e) {
            exception = e;
        }

        // Evaluate
        Assert.assertTrue(exception != null);
        Assert.assertTrue(exception instanceof CertificateException);
    }

    @Test
    public final void testHttpConnectionFailsWhenAnotherBadCertificate() throws Exception {
        // Setup
        final String badCertificate = "dummy";

        // Test
        CertificateException exception = null;
        try {
            new HttpConnection(badCertificate, true, null);
        } catch (final CertificateException e) {
            exception = e;
        }

        // Evaluate
        Assert.assertTrue(exception != null);
        Assert.assertTrue(exception instanceof CertificateException);
    }


    @Test
    public void testCreateSSLConnectionSocketFactorySuccessfullyInitializesToCustomValuesWhenCertificateIsValid()
            throws Exception {

        // Test
        HttpConnection httpConnection = new HttpConnection(CERTIFICATE, true, null);

        // Evaluate attributes of instantiated SSLConnectionSocketFactory
        SSLConnectionSocketFactory socketFactory = (SSLConnectionSocketFactory) PA.invokeMethod(httpConnection,
                "createSSLConnectionSocketFactory(java.lang.String)", CERTIFICATE);
        Assert.assertTrue(socketFactory != null);
        HostnameVerifier hostnameVerifier = (HostnameVerifier) PA.getValue(socketFactory, "hostnameVerifier");
        Assert.assertTrue(hostnameVerifier instanceof DefaultHostnameVerifier);
        String[] supportedProtocols = (String[]) PA.getValue(socketFactory, "supportedProtocols");
        Assert.assertEquals(SUPPORTED_PROTOCOLS.length, supportedProtocols.length);
        for (int i = 0; i < SUPPORTED_PROTOCOLS.length; ++i) {
            Assert.assertEquals(SUPPORTED_PROTOCOLS[i], supportedProtocols[i]);
        }
        String[] supportedCipherSuites = (String[]) PA.getValue(socketFactory, "supportedCipherSuites");
        Assert.assertTrue(supportedCipherSuites == null);
        Object innerSocketFactory = PA.getValue(socketFactory, "socketfactory");
        Object sslContext = PA.getValue(innerSocketFactory, "context");
        Object trustManager = PA.getValue(sslContext, "trustManager");
        Set trustedCerts = (Set) PA.getValue(trustManager, "trustedCerts");
        Assert.assertEquals(1, trustedCerts.size());
    }

    //-------------------------------------------------
    private static final String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDBzCCAe+gAwIBAgIJAMBaKSQntCBNMA0GCSqGSIb3DQEBBQUAMBoxGDAWBgNV\n"
            + "BAMMD3d3dy5leGFtcGxlLmNvbTAeFw0xOTA1MDkxNTA5MzdaFw0yOTA1MDYxNTA5\n"
            + "MzdaMBoxGDAWBgNVBAMMD3d3dy5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEB\n"
            + "BQADggEPADCCAQoCggEBALYeHH+UgWpMFUqbaClAV5V60aovYXkR9RL7rxEdVdmv\n"
            + "u2d2bugqN2SUXuZpngFRP/O6YSnC6i5CP1hNV2sCn3HVxE0+dpvDdDQS0jBnxK+n\n"
            + "4WuAYKjTWbj/ddMYdRcPqM/9kPCAdayte7B18twSuDhf4wXBAZ9D6hQ9Cbh/7Cou\n"
            + "8Xz8RGxiGt4Nf16IfKj0eyYobYMHelblJ/7WAsG42lKt66GyXqbUDkKdE2fgI7zF\n"
            + "ZEd43qKm+j6VTJY+AknDtQPdp/PT/MIHq2/rUuTKUCT0/tMX0eTRndQjWYbjiLk/\n"
            + "Q5ObbThCko1fYr9nq/qNJbE9KrTKVXLsFean+t0hGUECAwEAAaNQME4wHQYDVR0O\n"
            + "BBYEFLPOFBKrhOmP6cbwL6ygTohfmkq3MB8GA1UdIwQYMBaAFLPOFBKrhOmP6cbw\n"
            + "L6ygTohfmkq3MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBAKwGslyZ\n"
            + "jq7AaZ7MGBq2/HhOeRtFS2uxLXiG6RcVKtgKN8gxp7D+LhksObT76nALonlOS47G\n"
            + "bTVLpfCsFBru6Kupek4yq1tsqi6n+yRoZ95pIRBLH1OaZBT+hD07Hqtto6xuAN6a\n"
            + "iMo7xF4jp4tMalUe5WqNt6yRiRPf0Mgpn2KicCF923gNxk2+IiopyY7TFcLIw3If\n"
            + "CgooVH4TtwvpO7Z/nRXcEHEPv2ENrlsUiZmgu9lsUMGjHBDcpc2+XYUxpY55UsX7\n"
            + "YhVLMh5ozL6iCZPXR+n7kWXltodHRnPBzHa03KrMShUWuoYZWwYZLyRXTy3DzyQR\n"
            + "4KMP896aB46y5r4=\n"
            + "-----END CERTIFICATE-----\n";

    private static final String[] SUPPORTED_PROTOCOLS = new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};

}
