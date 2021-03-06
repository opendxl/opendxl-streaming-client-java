Basic Consume token-based authentication
----------------------------------------

This sample demonstrates how to establish a channel connection to the
DXL streaming service by using an identity token authentication
mechanism. Once the connection is established, the sample repeatedly
consumes and displays available records for the consumer group.

Code highlights are shown below:

Sample Code
~~~~~~~~~~~

.. code:: java

    ...
    static Logger logger = Logger.getLogger(ConsumeRecordsWithToken.class);
    ...
    // Create a new Channel object
    Channel channel = new ChannelBuilder(channelUrl, new ChannelAuthToken(token), channelConsumerGroup)
                    .withExtraConfigs(extraConfigs)
                    .withRetryOnFail(true)
                    .withCertificateBundle(verifyCertificateBundle)
                    .withHttpProxy(new HttpProxySettings(PROXY_ENABLED,
                            PROXY_HOST,
                            PROXY_PORT,
                            PROXY_USR,
                            PROXY_PWD))
                    .build();

    // Create object which processCallback() method will be called back upon by the run method (see below)
    // when records are received from the channel
    ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {

        @Override
        public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) {
            // Print the received payloads. 'payloads' is a list of
            // dictionary objects extracted from the records received
            // from the channel.
            logger.info(new StringBuilder("Received ")
                    .append(consumerRecords.getRecords().size())
                    .append(" records")
                    .toString());

            for (ConsumerRecords.ConsumerRecord record : consumerRecords.getRecords()) {

                logger.info("topic = " + record.getTopic());
                logger.info("partition = " + record.getPartition());
                logger.info("offset = " + record.getOffset());
                logger.info("sharding key = " + record.getShardingKey());
                logger.info("headers = " + record.getHeaders());
                logger.info("payload = " + record.getPayload());
                logger.info("decoded payload = " + new String(record.getDecodedPayload()));
                logger.info("");

            }

            // Return 'True' in order for the 'run' call to continue attempting to consume records.
            logger.info("let commit records");
            return true;
        }
    };

    // Consume records indefinitely
    final int consumePollTimeoutMs = 500;
    channel.run(consumerRecordCallback, channelTopicSubscriptions, consumePollTimeoutMs);

The first step is to create a ``Channel`` instance, which establishes a
channel to the streaming service. The channel includes the URL to the
streaming service, ``channelUrl``, and TOKEN that the client uses to
authenticate itself to the service, ``new ChannelAuthToken(token)``. It
also includes a certificate, ``verifyCertificateBundle`` , and HTTP
proxy settings ``new HttpProxySettings(...)``

The example defines a ``consumerRecordCallback`` instance which is
invoked with the ``consumerRecords`` extracted from records consumed
from the channel. The ``consumerRecordCallback`` function outputs the
contents of the each record, its metadata and returns ``true`` to
indicate that the channel should continue consuming records. Note that
if the ``consumerRecordCallback`` function were to instead return
``false``, the ``run()`` method would stop polling the service for new
records and it would return.

The final step is to call the ``run()`` method. The ``run()`` method
establishes a consumer instance with the service, subscribes the
consumer instance for records delivered to the ``topics`` included in
the ``channelTopicSubscriptions`` variable, and continuously polls the
streaming service for available records. The records from any records
which are received from the streaming service are passed in a call to
the ``consumerRecordCallback`` instance. Note that if no records are
received from a poll attempt, an empty list of records is passed into
the ``consumerRecordCallback`` function.

As records are received by the sample, the contents of the messages
should be displayed to the output window. The output should appear
similar to the following:

::

    topic = topic1
    partition = 0
    offset = 4
    sharding key = pool-1-thread-1-0-0
    headers = {sourceId=abc, scope=algo, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
    payload = SGVsbG8gV29ybGQgYXQ6MjAxOS0wNS0wN1QxNjowMjoxOC42NjcgRXh0cmE6IEI0ODlNOTNTSFVEME5VTVYzWlZKTU43NkJSNE5HUEE4NFIzSVI1R1NDME05WTFYT1FISjMyNzhMSzY2UFpYNTg4QU42WjEyMjlKRUE4Nlg2MDhLSUxDSDczSFRSSkQyUlNKTkQ=
    decoded payload = Hello World at:2019-05-07T16:02:18.667 Extra: B489M93SHUD0NUMV3ZVJMN76BR4NGPA84R3IR5GSC0M9Y1XOQHJ3278LK66PZX588AN6Z1229JEA86X608KILCH73HTRJD2RSJND

    topic = topic1
    partition = 0
    offset = 5
    sharding key = pool-1-thread-1-0-0
    headers = {sourceId=abc, scope=algo, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
    payload = SGVsbG8gV29ybGQgYXQ6MjAxOS0wNS0wN1QxNjowMjoyMi4wNzggRXh0cmE6IDk5MVYwN0FOOUdOOUROTjVYRUo2Q09NTzQwU1ZRVFJTRlZYUUZBWVE1WjRFV1paME5XVkVRNElaVk5aTzlORkxRMTlKVEw2Q1lGNVJWV0RJRUpPQkM3OTM5TzBTTkQ5OFpKTVg=
    decoded payload = Hello World at:2019-05-07T16:02:22.078 Extra: 991V07AN9GN9DNN5XEJ6COMO40SVQTRSFVXQFAYQ5Z4EWZZ0NWVEQ4IZVNZO9NFLQ19JTL6CYF5RVWDIEJOBC7939O0SND98ZJMX

Run the sample
~~~~~~~~~~~~~~

Prerequisites
^^^^^^^^^^^^^

-  A DXL streaming service is available for the sample to connect to.
-  Credentials for a consumer are available for use with the sample.

Setup
^^^^^

Modify the example to include the appropriate settings for the streaming
service channel:

.. code:: java

        private static final String CHANNEL_URL = "http://127.0.0.1:50080";
        private static final String TOKEN = "Your_Token";
        private static final List<String> TOPICS = Arrays.asList("topic1");
        private static final String CONSUMER_GROUP = "sample_consumer_group";
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

    $ ./runsample sample.ConsumeRecordsWithToken

The initial line in the output window should be similar to the
following:

::

    INFO [main] (Channel.java:691) - Channel is running

As records are received by the sample, the contents of the message
payloads should be displayed to the output window.

::

    Received 15 records
    topic = topic1
    partition = 5
    offset = 13
    sharding key = 123
    headers = {scope=soc.evt.vi, tenantId=DBB1FA1E-6A68-4837-982E-FB8D839FF4DA, zoneId=TMP.Identity.TRUCHATOR}
    payload = SGVsbG8sIFdvcmxkLg==
    decoded payload = Hello, World.

    topic = topic1
    partition = 5
    offset = 14
    sharding key = 123
    headers = {scope=soc.evt.vi, tenantId=DBB1FA1E-6A68-4837-982E-FB8D839FF4DA, zoneId=TMP.Identity.TRUCHATOR}
    payload = SGVsbG8sIFdvcmxkLg==
    decoded payload = Hello, World.

    topic = topic1
    partition = 5
    offset = 15
    sharding key = 123
    headers = {scope=soc.evt.vi, tenantId=DBB1FA1E-6A68-4837-982E-FB8D839FF4DA, zoneId=TMP.Identity.TRUCHATOR}
    payload = SGVsbG8sIFdvcmxkLg==
    decoded payload = Hello, World.

    topic = topics1
    partition = 5
    offset = 16
    sharding key = 123
    headers = {scope=soc.evt.vi, tenantId=DBB1FA1E-6A68-4837-982E-FB8D839FF4DA, zoneId=TMP.Identity.TRUCHATOR}
    payload = SGVsbG8sIFdvcmxkLg==
    decoded payload = Hello, World.

