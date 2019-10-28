Basic Produce and Consume using token-based authentication
==========================================================

This sample demonstrates how to produce records to and to consume them
from the DXL streaming service using ``Producer`` and ``Consumer``
objects respectively. Producer objects implement the ``Producer``
interface which exposes just the producer methods (e.g.: produce) of
Channel class. Consumer objects implement the ``Consumer`` interface
which exposes the Channel class consumer methods (e.g.: create,
subscribe, subscriptions, consume, commit, delete). Two builders,
``ProducerBuilder`` and ``ConsumerBuilder``, are available to
respectively instantiate ``Producer`` and ``Consumer`` objects which
internally are Channel objects initialized to either producer or
consume.

Rationale to use ``Producer`` and ``Consumer`` objects instead of
``Channel`` is to avoid trying to consume using a ``Channel`` object
which was not properly set up. To discuss this in more detail, let's
assume a ``Channel`` object is instantiated to just produce, so
``Channel`` constructor just sets produce parameters, like
``producerPathPrefix``, and no consumer ones. However, such ``Channel``
object also exposes its consume methods although it was not properly
initialized to call them. If its ``create()`` method was called, then an
error would be thrown because no consumer group had been set. If a
``Producer`` object is used instead of a ``Channel`` object, then such
error will never take place because ``Producer`` does not expose
``create()`` nor other consume related API.

Code highlights are shown below:

Sample Code
~~~~~~~~~~~

.. code:: java

    ...
    // topics to consume from
    static final List<String> CONSUMER_TOPICS = Arrays.asList("case-mgmt-events",
                "my-topic",
                "topic-abc123",
                "topic1");
    // topics to produce to
    static final String PRODUCER_TOPIC_1 = "my-topic";
    static final String PRODUCER_TOPIC_2 = "topic1";
    // logger instance
    static Logger logger = Logger.getLogger(ProduceAndConsumeRecordsUsingInterfacesWithToken.class);
    ...
    // 1st step
    // Create new Consumer object
    Consumer consumer = new ConsumerBuilder(channelUrl, new ChannelAuthToken(token), channelConsumerGroup)
                    .withRetryOnFail(true)
                    .withCertificateBundle(verifyCertificateBundle)
                    .withExtraConfigs(extraConfigs)
                    .withHttpProxy(new HttpProxySettings(PROXY_ENABLED,
                            PROXY_HOST,
                            PROXY_PORT,
                            PROXY_USR,
                            PROXY_PWD))
                    .build();

    // 2nd step
    // Create new Producer object
    Producer producer = new ProducerBuilder(channelUrl, new ChannelAuthToken(token))
                    .withCertificateBundle(verifyCertificateBundle)
                    .withHttpProxy(new HttpProxySettings(PROXY_ENABLED,
                            PROXY_HOST,
                            PROXY_PORT,
                            PROXY_USR,
                            PROXY_PWD))
                    .build();

    // 3rd step
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

    // 4th step
    // Create thread to produce records to selected topics. These records will be consumed and printed out
    // by the consumerRecordCallback once consumer.run() is called.
    final Thread produceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // counter to append to produce record payloads so each payload will be unique
                int recordCounter = 1;
                while (!Thread.interrupted()) {
                    // Set up a ProducerRecords object containing two producer records.
                    // produce API can send many records, even to different topics, in a single call.
                    // Showing only record mandatory parameters, topic and payload.
                    final ProducerRecords producerRecords = new ProducerRecords();
                    producerRecords.add(
                            new ProducerRecords.ProducerRecord
                                    .Builder(PRODUCER_TOPIC_1,
                                    "Hello from OpenDXL - " + recordCounter)
                                    .build()
                    );
                    producerRecords.add(
                            new ProducerRecords.ProducerRecord
                                    .Builder(PRODUCER_TOPIC_2,
                                    "Hello from OpenDXL - " + (recordCounter + 1))
                                    .build()
                    );
                    try {
                        logger.info("produce records " + recordCounter + " and " + (recordCounter + 1));

                        // produce records to DXL streaming service
                        producer.produce(producerRecords);

                        recordCounter += 2;
                    } catch (final PermanentError | TemporaryError e) {
                        printError(e);
                    }
            }
        });

    // Finally let Producer and Consumer run
    // Produce records indefinitely
    produceThread.start();
    // Consume records indefinitely
    final int consumePollTimeoutMs = 500;
    consumer.run(consumerRecordCallback, channelTopicSubscriptions, consumePollTimeoutMs);

The first step is to create a ``Consumer`` instance, which establishes a
channel to the streaming service. The ``Consumer`` must include the host
and port to connect to the streaming service, ``channelUrl``, the TOKEN
that the client uses to authenticate itself to the service,
``new ChannelAuthToken(token)``, and the consumer group,
``channelConsumerGroup``. It may also include a certificate,
``verifyCertificateBundle``, additional consumer configuration
parameters, ``extraConfigs``, HTTP proxy settings
``new HttpProxySettings(...)`` and a path to the streaming service
consumer API, ``consumerPathPrefix``. If ``consumerPathPrefix`` is not
specified, then its default value, ``"/databus/consumer-service/v1"``,
will be used instead.

The second step is to create a ``Producer`` instance, which establishes
another channel to the streaming service, this one to produce only. The
channel must include the host and port to connect to the streaming
service, ``channelUrl``, and TOKEN that the client uses to authenticate
itself to the service, ``new ChannelAuthToken(token)``. It may also
specify a path to the streaming service, ``producerPathPrefix``, a
certificate, ``verifyCertificateBundle``, and HTTP proxy settings
``new HttpProxySettings(...)``. If ``producerPathPrefix`` is not
specified, then its default value, ``"/databus/cloudproxy/v1"``, will be
used instead.

The third step is to define a ``consumerRecordCallback`` instance which
is invoked with the ``consumerRecords`` extracted from records obtained
by the ``Consumer``. The ``consumerRecordCallback`` function outputs the
contents of each record and its metadata and returns ``true`` to
indicate that the channel should continue consuming records. Note that
if the ``consumerRecordCallback`` function were to instead return
``false``, the ``run()`` method would stop polling the service for new
records and it would return.

The fourth step is to define a ``produceThread`` thread which will
continuously call ``Producer`` produce method to send records to the DXL
streaming service. A ``ProducerRecords`` object is created and it is
populated with two producer records, one with ``"my-topic"`` topic and
another ``"topic1"`` topic. Then this ``ProducerRecords`` object is
passed to ``Producer`` produce method, thus sending two records in a
single API call.

The final step is to start ``producerThread`` and ``consumer``. Once
``producerThread.start()`` is called, records are continuously produced.
Then, ``consumer.run()`` method establishes a consumer instance with the
DXL streaming service, subscribes the consumer instance for records
delivered to the ``topics`` included in the
``channelTopicSubscriptions`` variable, and continuously polls the
streaming service for available records. The records returned by the
poll to the streaming service are passed in a call to the
``consumerRecordCallback`` instance. Note that if no records are
received from a poll attempt, an empty list of records is passed into
the ``consumerRecordCallback`` function.

As records are produced and received by the sample, the contents of the
messages should be displayed to the output window. The output should
appear similar to the following:

::

     ...
     INFO [Thread-0] ... - produce records 65 and 66
     INFO [Thread-0] ... - produce records 67 and 68
     INFO [main] ... - Received 3 records
     INFO [main] ... - topic = my-topic
     INFO [main] ... - partition = 2
     INFO [main] ... - offset = 29
     INFO [main] ... - sharding key = 29598919
     INFO [main] ... - headers = {sourceId=D5452543-E2FB-4585-8BE5-A61C3636819C, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gNTk=
     INFO [main] ... - decoded payload = Hello from OpenDXL - 59
     INFO [main] ... - 
     INFO [main] ... - topic = topic1
     INFO [main] ... - partition = 0
     INFO [main] ... - offset = 28
     INFO [main] ... - sharding key = 176927523
     INFO [main] ... - headers = {sourceId=F567D6A2-500E-4D35-AE15-A707f165D4FA, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gNTg=
     INFO [main] ... - decoded payload = Hello from OpenDXL - 58
     INFO [main] ... - 
     INFO [main] ... - topic = my-topic
     INFO [main] ... - partition = 0
     INFO [main] ... - offset = 29
     INFO [main] ... - sharding key = 176927523
     INFO [main] ... - headers = {sourceId=F567D6A2-500E-4D35-AE15-A707f165D4FA, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gNjA=
     INFO [main] ... - decoded payload = Hello from OpenDXL - 60
     INFO [main] ... - 
     INFO [main] ... - let commit records
     INFO [Thread-0] ... - produce records 69 and 70
     INFO [Thread-0] ... - produce records 71 and 72
     INFO [main] ... - Received 5 records
     ...

Finally, when CTRL+C is pressed, then the example will end logging the
following lines:

::

     INFO [Thread-1] (ProduceAndConsumeRecordsUsingInterfacesWithToken.java:175) - Shutdown app requested. Exiting
     INFO [Thread-1] (Channel.java:844) - Channel was stopped.

Run the sample
~~~~~~~~~~~~~~

Prerequisites
^^^^^^^^^^^^^

-  A DXL streaming service is available for the sample to connect to.
-  Credentials for producer and consumer are available for use with the
   sample.

Setup
^^^^^

Modify the example to include the appropriate settings for the streaming
service channel:

.. code:: java

        private static final String CHANNEL_URL = "http://127.0.0.1:50080";
        private static final String TOKEN = "Your_Token";
        private static final List<String> CONSUMER_TOPICS = Arrays.asList("topic1", "my-topic");
        private static final String CONSUMER_GROUP = "sample_consumer_group";

        private static final String PRODUCER_TOPIC_1 = "my-topic";
        private static final String PRODUCER_TOPIC_2 = "topic1";

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

    $ ./runsample sample.ProduceAndConsumeRecordsUsingInterfacesWithToken

The initial line in the output window should be similar to the
following:

::

    INFO [main] (Channel.java:745) - Channel is running

As records are sent and received by the sample, the ``Producer`` and
``Consumer`` log lines and the contents of the message payloads should
be displayed to the output window.

::

     INFO [Thread-0] ... - produce records 1 and 2
     INFO [Thread-0] ... - produce records 3 and 4
     INFO [Thread-0] ... - produce records 5 and 6
     INFO [main] ... - Received 2 records
     INFO [main] ... - topic = my-topic
     INFO [main] ... - partition = 2
     INFO [main] ... - offset = 0
     INFO [main] ... - sharding key = 29598919
     INFO [main] ... - headers = {sourceId=D5452543-E2FB-4585-8BE5-A61C3636819C, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gMQ==
     INFO [main] ... - decoded payload = Hello from OpenDXL - 1
     INFO [main] ... - 
     INFO [main] ... - topic = topic1
     INFO [main] ... - partition = 0
     INFO [main] ... - offset = 0
     INFO [main] ... - sharding key = 176927523
     INFO [main] ... - headers = {sourceId=F567D6A2-500E-4D35-AE15-A707f165D4FA, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gMg==
     INFO [main] ... - decoded payload = Hello from OpenDXL - 2
     INFO [main] ... - 
     INFO [main] ... - let commit records
     INFO [Thread-0] ... - produce records 7 and 8
     INFO [Thread-0] ... - produce records 9 and 10
     INFO [main] ... - Received 1 records
     INFO [main] ... - topic = my-topic
     INFO [main] ... - partition = 2
     INFO [main] ... - offset = 1
     INFO [main] ... - sharding key = 29598919
     INFO [main] ... - headers = {sourceId=D5452543-E2FB-4585-8BE5-A61C3636819C, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gMw==
     INFO [main] ... - decoded payload = Hello from OpenDXL - 3
     INFO [main] ... - 
     INFO [main] ... - let commit records
     INFO [Thread-0] ... - produce records 11 and 12
     INFO [Thread-0] ... - produce records 13 and 14
     INFO [main] ... - Received 4 records
     INFO [main] ... - topic = topic1
     INFO [main] ... - partition = 2
     INFO [main] ... - offset = 2
     INFO [main] ... - sharding key = 29598919
     INFO [main] ... - headers = {sourceId=D5452543-E2FB-4585-8BE5-A61C3636819C, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gNQ==
     INFO [main] ... - decoded payload = Hello from OpenDXL - 5
     INFO [main] ... - 
     INFO [main] ... - topic = my-topic
     INFO [main] ... - partition = 2
     INFO [main] ... - offset = 3
     INFO [main] ... - sharding key = 29598919
     INFO [main] ... - headers = {sourceId=D5452543-E2FB-4585-8BE5-A61C3636819C, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gNw==
     INFO [main] ... - decoded payload = Hello from OpenDXL - 7
     INFO [main] ... - 
     INFO [main] ... - topic = topic1
     INFO [main] ... - partition = 0
     INFO [main] ... - offset = 1
     INFO [main] ... - sharding key = 176927523
     INFO [main] ... - headers = {sourceId=F567D6A2-500E-4D35-AE15-A707f165D4FA, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gNA==
     INFO [main] ... - decoded payload = Hello from OpenDXL - 4
     INFO [main] ... - 
     INFO [main] ... - topic = my-topic
     INFO [main] ... - partition = 0
     INFO [main] ... - offset = 2
     INFO [main] ... - sharding key = 176927523
     INFO [main] ... - headers = {sourceId=F567D6A2-500E-4D35-AE15-A707f165D4FA, scope=soc.etv.vi, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
     INFO [main] ... - payload = SGVsbG8gZnJvbSBPcGVuRFhMIC0gNg==
     INFO [main] ... - decoded payload = Hello from OpenDXL - 6
     INFO [main] ... - 
     INFO [main] ... - let commit records
     ...
