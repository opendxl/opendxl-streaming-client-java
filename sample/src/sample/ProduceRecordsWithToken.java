/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package sample;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.HttpProxySettings;
import com.opendxl.streaming.client.auth.ChannelAuthToken;
import com.opendxl.streaming.client.entity.ProducerRecords;
import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.StopError;
import com.opendxl.streaming.client.exception.TemporaryError;

import java.util.HashMap;

/**
 * This example uses the opendxl-streaming-java-client to produce records to a Databus topic. It instantiates
 * a ChannelAuthToken object and a Channel object. Then, the Channel produce method is
 * invoked to set the given topic and payload into a record and send the record to Databus.
 */
public class ProduceRecordsWithToken {

    private static final String CHANNEL_URL = "http://127.0.0.1:50080";
    private static final String TOKEN = "TOKEN3";

    private static final String VERIFY_CERTIFICATE_BUNDLE = "-----BEGIN CERTIFICATE-----"
            + "MIIDBzCCAe+gAwIBAgIJALteQYzVdTj3MA0GCSqGSIb3DQEBBQUAMBoxGDAWBgNV"
            + "BAMMD3d3dy5leGFtcGxlLmNvbTAeFw0xOTA0MjIxNTI2MjZaFw0yOTA0MTkxNTI2"
            + "MjZaMBoxGDAWBgNVBAMMD3d3dy5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEB"
            + "BQADggEPADCCAQoCggEBAMHv/jBHmUI6s2FhDdw4I7I9RTU3yvpkXM1e/5ISfBwe"
            + "18gkeml7q9t9eLpPc08W0akYn/SySeT0TEvw6w8mpCfEefe+RHg7f6taAzzMwtei"
            + "bt98VSdrckQh2DfL+Dp47BeP/XsHh80V4rschYbK/RCt6tMARcR5VRoC3VETKGqH"
            + "tGTgUjLrpsCqsPTQuSLaST8brLp0KBVS1T39ltB6UFLdmw3WxiuuHvy9Tk5KLuFv"
            + "SjfR6zPP/b9BsnYw35rceEB/+bh3KGCnTS6hO1Qbt3sAolOc6Y8VuDAQRZfsD7m5"
            + "8hrsvT/7VRBr0RoWUSYTZJRXrPUUmjP3CMJkfeXOauUCAwEAAaNQME4wHQYDVR0O"
            + "BBYEFN6UJ/tpppQTRvb5zo+6nnPGfJoXMB8GA1UdIwQYMBaAFN6UJ/tpppQTRvb5"
            + "zo+6nnPGfJoXMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBAB635aKg"
            + "mAifM5P/T9Xt8tFpIfVWGRc9dbdWsce1/IMoMUDwQuGpmvdKfY68FVN4niKC/HeB"
            + "J4OBflvM7by8KBC28N/g8It/rqOCU14JFyCcYQPpPj7uTFLwiGuraGnnCGX+GYW3"
            + "bGSCAVnJvH0gb0kWFTJwdK1dBUsMrRwHDhkrYLe8Z6NzT39VA3hI9cedbzIsfAJf"
            + "pRBkBaMQyB6u15NHnqKy57zpekuoChU8snWLu7G8E6coMW1AlMGuNiZZqX3XCvAd"
            + "8gc45ashE41QRpGz9fh3FfUJIq1BBoIjvJahzIPLVfvfDhTwpBHZ+PJkBcsUUgcf"
            + "lHTRe1CZks4JfS8="
            + "-----END CERTIFICATE-----";

    private static final boolean PROXY_ENABLED = true;
    private static final String PROXY_HOST = "10.20.30.40";
    private static final int PROXY_PORT = 8080;
    private static final String PROXY_USR = "";
    private static final String PROXY_PWD = "";

    private ProduceRecordsWithToken() { }

    public static void main(String[] args) {

        String channelUrl = CHANNEL_URL;
        String token = TOKEN;

        // CA bundle certificate chain of trusted CAs provided as a string. The CA
        // bundle is used to validate that the certificate of the server being connected
        // to was signed by a valid authority. If set to an empty string, the server
        // certificate is not validated.
        String verifyCertificateBundle = VERIFY_CERTIFICATE_BUNDLE;

        /**
         * Note that Channel extraConfigs parameter is not required to produce records. The extraConfigs value applies
         * only to consume records. The producerPathPrefix parameter should be set to value suitable to your
         * environment. If it is set to null, then its default value {@link Channel#DEFAULT_PRODUCER_PATH_PREFIX} will
         * be used.
         */
        try (Channel channel = new Channel(channelUrl,
                new ChannelAuthToken(token),
                null,
                null,
                null,
                null,
                true,
                verifyCertificateBundle,
                null,
                // http proxy settings
                new HttpProxySettings(PROXY_ENABLED,
                        PROXY_HOST,
                        PROXY_PORT,
                        PROXY_USR,
                        PROXY_PWD))) {

            // Create Produce record
            final ProducerRecords producerRecords = new ProducerRecords();
            producerRecords.add(
                    new ProducerRecords.ProducerRecord
                            .Builder("topic1", "Hello from OpenDXL")
                            .withHeaders(new HashMap<String, String>() {{
                                put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                            }})
                            .withShardingKey("123")
                            .build()
            );

            // produce the record
            channel.produce(producerRecords);

        } catch (final PermanentError | TemporaryError | StopError e) {

            System.out.println("Error occurred: " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            System.out.println(e.getCause() != null
                    ? e.getClass().getCanonicalName() + ": " + e.getCause().getMessage()
                    : "no exception cause reported");

            e.printStackTrace();

        }

    }

}