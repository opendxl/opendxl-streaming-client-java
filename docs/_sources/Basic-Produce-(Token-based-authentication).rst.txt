Basic Produce token-based authentication
----------------------------------------

This sample demonstrates how to produce records using a channel
connection to the DXL streaming service. The sample repeatedly produces
records and it logs a message for each successfully produced one. The
channel connects to DXL streaming service using an identity token
authentication mechanism. A producer record consists of four attributes:
topic (mandatory), payload (mandatory), shardingKey (optional) and
headers (optional).

Code highlights are shown below:

Sample Code
~~~~~~~~~~~

.. code:: java

    ...
    static Logger logger = Logger.getLogger(ProduceRecordsWithToken.class);
    String channelUrl = "http://127.0.0.1:50080";
    String token = "TOKEN";
    ...
    // Create a new Channel object to just produce records.
    // Since produce action requires less parameters than consume action,
    // many Channel parameters can be set to null.
    Channel channel = new ChannelBuilder(channelUrl, new ChannelAuthToken(token), null)
                    .withCertificateBundle(verifyCertificateBundle)
                    .withHttpProxy(new HttpProxySettings(PROXY_ENABLED,
                            PROXY_HOST,
                            PROXY_PORT,
                            PROXY_USR,
                            PROXY_PWD))
                    .build());

    // Set up a flag to stop producing records and to quit gracefully when
    // the produce sample app is requested to end
    final AtomicBoolean keepProducing = new AtomicBoolean(true);
    // Set up a record counter which is appended to all record payloads to easily
    // notice in the logs that different records are being produced
    int recordCounter = 1;

    // Produce records indefinitely
    while (keepProducing.get()) {

        // Create Produce record
        final ProducerRecords producerRecords = new ProducerRecords();
        producerRecords.add(
                new ProducerRecords.ProducerRecord
                        .Builder("topic1", "Hello from OpenDXL - " + recordCounter)
                        .withHeaders(new HashMap<String, String>() {{
                            put("sourceId", "D5452543-E2FB-4585-8BE5-A61C3636819C");
                        }})
                        .withShardingKey("119159619")
                        .build()
        );

        // produce the record
        channel.produce(producerRecords);
        logger.info("produced record - " + recordCounter);
        recordCounter++;
    }

The first step is to create a ``Channel`` instance, which establishes a
channel to the streaming service. The channel must include the host and
port to connect to the streaming service, ``channelUrl``, and TOKEN that
the client uses to authenticate itself to the service,
``new ChannelAuthToken(token)``. It may also specify a path to the
streaming service, ``producerPathPrefix``, an additional certificate,
``verifyCertificateBundle`` , and HTTP proxy settings
``new HttpProxySettings(...)``. If ``producerPathPrefix`` is not
specified, then its default value, ``"/databus/cloudproxy/v1"`` will be
used instead.

As records are produced by the sample, a log indicating the number of
records produced up to now should be displayed to the output window. The
output should appear similar to the following:

::

     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 1
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 2
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 3
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 4
     ...

Finally, when CTRL+C is pressed, then the example will end logging the
following line:

::

    INFO [Thread-0] (ProduceRecordsWithToken.java:119) - Shutdown app requested. Exiting

Run the sample
~~~~~~~~~~~~~~

Prerequisites
^^^^^^^^^^^^^

-  A DXL streaming service is available for the sample to connect to.
-  Credentials for a producer are available for use with the sample.

Setup
^^^^^

Modify the example to include the appropriate settings for the streaming
service channel:

.. code:: java

        private static final String CHANNEL_URL = "http://127.0.0.1:50080";
        private static final String TOKEN = "Your_Token";
        private static final String PRODUCER_TOPIC = "topic1";
        private static final String VERIFY_CERTIFICATE_BUNDLE = "-----BEGIN CERTIFICATE-----"
                + "Your Certificate if nedeed"
                + "-----END CERTIFICATE-----";

        private static final boolean PROXY_ENABLED = true;
        private static final String PROXY_HOST = "10.20.30.40";
        private static final int PROXY_PORT = 8080;
        private static final String PROXY_USR = "";
        private static final String PROXY_PWD = "";

Running
^^^^^^^

To run this sample execute the runsample script as follows:

::

    $ ./runsample sample.ProduceRecordsWithToken

As records are produced by the sample, a log line indicating the number
of record being produced should be displayed to the output window.

::

     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 1
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 2
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 3
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 4
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 5
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 6
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 7
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 8
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 9
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 10
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 11
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 12
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 13
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 14
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 15
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 16
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 17
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 18
     INFO [main] (ProduceRecordsWithToken.java:142) - produced record - 19
    ...
