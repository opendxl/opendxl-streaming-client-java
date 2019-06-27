# Basic Consume token based-authentication example
This sample demonstrates how to establish a channel connection to the DXL streaming service by using an identity token authentication mechanism. Once the connection is established, the sample repeatedly consumes and displays available records for the consumer group.

Code highlights are shown below:

_Note:_ ```<0.0.0.0>``` and ```<PORT>``` are placeholders which you should respectively replace with the Http Proxy IP address or hostname and TCP port number.

### Sample Code

```java
// Create a new Channel object
Channel channel = new Channel(channelUrl,
                new ChannelAuthToken(token),
                channelConsumerGroup,
                pathPrefix,
                consumerPathPrefix,
                true,
                verifyCertificateBundle,
                extraConfigs,
                new HttpProxySettings(true,
                        <0.0.0.0>,
                        <PORT>,
                        "me",
                        "secret"))

// Create object which processCallback() method will be called back upon by the run method (see below)
// when records are received from the channel
ConsumerRecordProcessor consumerRecordCallback = new ConsumerRecordProcessor() {

    @Override
    public boolean processCallback(ConsumerRecords consumerRecords, String consumerId) {
        // Print the received payloads. 'payloads' is a list of
        // dictionary objects extracted from the records received
        // from the channel.
        System.out.println(new StringBuilder("Received ")
                .append(consumerRecords.getRecords().size())
                .append(" records")
                .toString());

        for (ConsumerRecords.ConsumerRecord record : consumerRecords.getRecords()) {

            System.out.println("topic = " + record.getTopic());
            System.out.println("partition = " + record.getPartition());
            System.out.println("offset = " + record.getOffset());
            System.out.println("sharding key = " + record.getShardingKey());
            System.out.println("headers = " + record.getHeaders());
            System.out.println("payload = " + record.getPayload());
            System.out.println("decoded payload = " + new String(record.getDecodedPayload()));
            System.out.println("");

        }

        // Return 'True' in order for the 'run' call to continue attempting to consume records.
        System.out.println("let commit records");
        return true;
    }
};

// Consume records indefinitely
final int consumePollTimeoutMs = 500;
channel.run(consumerRecordCallback, channelTopicSubscriptions, consumePollTimeoutMs);

```

The first step is to create a `Channel` instance, which establishes a channel
to the streaming service. The channel includes the URL to the streaming
service, `channelUrl`, and TOKEN that the client uses to authenticate
itself to the service, `new ChannelAuthToken(token)`. It also includes a certificate, `verifyCertificateBundle` , and HTTP proxy settings `new HttpProxySettings(...)`

The example defines a `consumerRecordCallback` instance which is invoked with the
`consumerRecords` extracted from records consumed from the
channel. The `consumerRecordCallback` function outputs the contents of the
each record, its metadata  and returns `true` to indicate that the channel should
continue consuming records. Note that if the `consumerRecordCallback` function were
to instead return `false`, the `run()` method would stop polling the service
for new records and it would return.

The final step is to call the `run()` method. The `run()` method establishes a
consumer instance with the service, subscribes the consumer instance for records
delivered to the `topics` included in the `channelTopicSubscriptions`
variable, and continuously polls the streaming service for available records. The
records from any records which are received from the streaming service are
passed in a call to the `consumerRecordCallback` instance. Note that if no records
are received from a poll attempt, an empty list of records is passed into the
`consumerRecordCallback` function.

As records are received by the sample, the contents of the messages
should be displayed to the output window. The output should appear similar to
the following:

```
topic = topic1-5ca969eb-2757-46ed-bc3f-f9266ccccea7
partition = 0
offset = 4
sharding key = pool-1-thread-1-0-0
headers = {sourceId=abc, scope=algo, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
payload = SGVsbG8gV29ybGQgYXQ6MjAxOS0wNS0wN1QxNjowMjoxOC42NjcgRXh0cmE6IEI0ODlNOTNTSFVEME5VTVYzWlZKTU43NkJSNE5HUEE4NFIzSVI1R1NDME05WTFYT1FISjMyNzhMSzY2UFpYNTg4QU42WjEyMjlKRUE4Nlg2MDhLSUxDSDczSFRSSkQyUlNKTkQ=
decoded payload = Hello World at:2019-05-07T16:02:18.667 Extra: B489M93SHUD0NUMV3ZVJMN76BR4NGPA84R3IR5GSC0M9Y1XOQHJ3278LK66PZX588AN6Z1229JEA86X608KILCH73HTRJD2RSJND

topic = topic1-5ca969eb-2757-46ed-bc3f-f9266ccccea7
partition = 0
offset = 5
sharding key = pool-1-thread-1-0-0
headers = {sourceId=abc, scope=algo, tenantId=5ca969eb-2757-46ed-bc3f-f9266ccccea7, zoneId=TMP.Identity.TRUCHATOR}
payload = SGVsbG8gV29ybGQgYXQ6MjAxOS0wNS0wN1QxNjowMjoyMi4wNzggRXh0cmE6IDk5MVYwN0FOOUdOOUROTjVYRUo2Q09NTzQwU1ZRVFJTRlZYUUZBWVE1WjRFV1paME5XVkVRNElaVk5aTzlORkxRMTlKVEw2Q1lGNVJWV0RJRUpPQkM3OTM5TzBTTkQ5OFpKTVg=
decoded payload = Hello World at:2019-05-07T16:02:22.078 Extra: 991V07AN9GN9DNN5XEJ6COMO40SVQTRSFVXQFAYQ5Z4EWZZ0NWVEQ4IZVNZO9NFLQ19JTL6CYF5RVWDIEJOBC7939O0SND98ZJMX
```