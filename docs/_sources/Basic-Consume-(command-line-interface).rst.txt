Consume CLI (Command Line Interface)
====================================

OpenDXL streaming client can be invoked from command line for testing or
checking purposes. Executing CLI like a standard Java library with no
arguments, will output the help:

::

    $ java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar

    ERROR: There are no options
    Option (* = required)                  Description                           
    ---------------------                  -----------                           
    --auth-url <String: auth-url>          The URL to authorization service.     
    --cg <String: cg>                      The consumer group name.              
    --config <String: config>              The consumer configuration.
    --consume-timeout <String: domain>     Consume Poll Timeout. Time that the channel waits for
                                           new records during a consume operation. Optional
                                           parameter. (default: 0)
    --consumer-id <String: consumer-id>    Consumer Id                           
    --consumer-prefix <String: consumer-   Consumer path prefix. (default:       
      prefix>                                /databus/consumer-service/v1)       
    --cookie <String: cookie>              Cookie value                          
    --domain <String: domain>              Cookie domain value                   
    --http-proxy <String: http-proxy>      Http Proxy settings in comma-separated format:
                                           enabled (true/false), host (URL format),
                                           port (integer), username (string),
                                           password (string). Enabled, host and port are
                                           mandatory. (default: no proxy). Example:
                                           true,<0.0.0.0>,<PORT>,<USERNAME>,<PASSWORD>
                                           where <0.0.0.0> must be replaced by the HttpProxy
                                           IP Address or hostname
    * --operation <String: operation>      Operations: login | create | subscribe
                                             | consume | commit | subscription   
    --password <String: password>          The password to send to authorization 
                                             service.                            
    --retry <String: retry>                Retry on fail. (default: true)        
    --token <String: token>                The authorized token.                 
    --topic <String: topic>                Comma-separated topic list to         
                                             subscribe to: topic1,topic2,...,    
                                             topicN.                             
    --url <String: url>                    The URL to hit consumer service.      
    --user <String: user>                  The user name to send to authorization
                                             service.                            
    --verify-cert-bundle <String: verify-  The ca certificate. (default: "", e.g.: empty string)
      cert-bundle>                

*Note:* ``<0.0.0.0>`` and ``<PORT>`` are throughout this page
placeholders which you should respectively replace with the correct IP
address or hostname and TCP port number of the service you are
attempting to invoke. Similarly, ``<TOKEN>`` is a placeholder for the
token value received after executing the ``login`` operation.

Supported Operations
--------------------

In order to get records from Streaming Service, the user has to invoke a
few CLI operations. Operations arguments are placed after
``--operation`` option. For instance:

::

    $ java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar --operation <OPERATION_ARGUMENT> ...

Operation Arguments
~~~~~~~~~~~~~~~~~~~

+-----------------------+-----------------------------------------+
| Operation Arguments   | Description                             |
+=======================+=========================================+
| login                 | get a identity token                    |
+-----------------------+-----------------------------------------+
| create                | create a consumer                       |
+-----------------------+-----------------------------------------+
| subscribe             | subscribe to topics                     |
+-----------------------+-----------------------------------------+
| consume               | poll records from Streaming Service     |
+-----------------------+-----------------------------------------+
| commit                | mark records as read for the consumer   |
+-----------------------+-----------------------------------------+
| delete                | close and erase a consumer              |
+-----------------------+-----------------------------------------+
| subscriptions         | get the list of topics subscribed to    |
+-----------------------+-----------------------------------------+

The sequence invocation of CLI operations to get records assuming a
token has already been received is: ``create`` -> ``subscribe`` ->
``consume`` . Because of the polling model, ``consume`` operation should
be called repeatedly to read the following records.

login
~~~~~

It is an operation argument to get an identity token. The token is a
prerequisite for the rest of the CLI operations.

+---------------------------------+-----------------------------------------------------+
| Mandatory Arguments for login   | Description                                         |
+=================================+=====================================================+
| ``--auth-url``                  | Identity REST service which should return a token   |
+---------------------------------+-----------------------------------------------------+
| ``--user``                      | Identity REST service user                          |
+---------------------------------+-----------------------------------------------------+
| ``--password``                  | Identity REST service password                      |
+---------------------------------+-----------------------------------------------------+

+--------------------------------+--------------------------------------------------------------------------------------------------+
| Optional Arguments for login   | Description                                                                                      |
+================================+==================================================================================================+
| ``--verify-cert-bundle``       | Certificate filename or certificate in String format to be able to hit Identity REST service     |
+--------------------------------+--------------------------------------------------------------------------------------------------+
| ``--http-proxy``               | Http Proxy data: whether enabled, FQDN or IP Address, TCP Port. Optional username and password   |
+--------------------------------+--------------------------------------------------------------------------------------------------+

example
^^^^^^^

::

    $ java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar \
    --operation login \
    --auth-url http://<0.0.0.0>:<PORT>/login \
    --user <USER_NAME> \
    --password <PASSWORD> \ 
    --verify-cert-bundle cerf-file-name.crt

.. code:: json

    {
        "code": "200",
        "result": <TOKEN>,
        "options": {
            "password": ["PASSWORD"],
            "auth-url": ["http://<0.0.0.0>:<PORT>/login"],
            "verify-cert-bundle": ["cerf-file-name.crt"],
            "user": ["USER_NAME"],
            "http-proxy": [""]
        }
    }

create
~~~~~~

It is an operation argument to create a consumer that will be part of a
specific Consumer Group and will have specific configuration.

+----------------------------------+--------------------------------------------+
| Mandatory Arguments for create   | Description                                |
+==================================+============================================+
| ``--url``                        | Streaming Service base URL                 |
+----------------------------------+--------------------------------------------+
| ``--token``                      | Identity token gotten by login operation   |
+----------------------------------+--------------------------------------------+
| ``--cg``                         | Consumer Group name                        |
+----------------------------------+--------------------------------------------+

+---------------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Optional Arguments for create   | Description                                                                                                                                                                                                                                    |
+=================================+================================================================================================================================================================================================================================================+
| ``--config``                    | Consumer configuration, a string of comma separated values Kafka Consumer properties, e.g.: max.message.size=1000,min.message.size=200,auto.offset.reset=latest,session.timeout.ms=300000,request.timeout.ms=310000,enable.auto.commit=false   |
+---------------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| ``--consumer-prefix``           | Consumer prefix URL path. If not present, then its default value will be used instead. Default value: /databus/consumer-service/v1                                                                                                             |
+---------------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| ``--verify-cert-bundle``        | Certificate file name or certificate in String format to be able to hit Streaming Service                                                                                                                                                      |
+---------------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| ``--http-proxy``                | Http Proxy data: whether enabled, FQDN or IP Address, TCP Port. Optional username and password                                                                                                                                                 |
+---------------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

example
^^^^^^^

::

    java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar \
    --operation create \
    --url http://<0.0.0.0>:<PORT>/streaming \
    --consumer-prefix /v1 \
    --token <TOKEN> \
    --cg cg1 \
    --verify-cert-bundle cert-file-name.crt

.. code:: json

    {
        "code": "200",
        "result": {
            "consumerId": "6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7",
            "cookie": {
                "value": "ee92c43b218c",
                "domain": "domain"
            }
        },
        "options": {
            "cg": ["cg1"],
            "consumer-prefix": ["/v1"],
            "verify-cert-bundle": ["cert-file-name.crt"],
            "http-proxy": [""],
            "retry": ["true"],
            "url": ["http://<0.0.0.0>:<PORT>/streaming"],
            "token": [<TOKEN>]
        }
    }

subscribe
~~~~~~~~~

It is an operation argument to set the topics from which to receive
records. If operation is successful, then its response is HTTP 204 No
Content without body.

+-------------------------------------+-----------------------------------------------------------+
| Mandatory Arguments for subscribe   | Description                                               |
+=====================================+===========================================================+
| ``--topic``                         | One or more comma-separated topic names to subscribe to   |
+-------------------------------------+-----------------------------------------------------------+
| ``--consumer-id``                   | Consumer Id previously obtained by create operation       |
+-------------------------------------+-----------------------------------------------------------+
| ``--cookie``                        | Cookie value obtained by create operation                 |
+-------------------------------------+-----------------------------------------------------------+
| ``--domain``                        | Cookie Domain value obtained by create operation          |
+-------------------------------------+-----------------------------------------------------------+
| ``--url``                           | Streaming Service base URL                                |
+-------------------------------------+-----------------------------------------------------------+
| ``--token``                         | Identity token gotten by login operation                  |
+-------------------------------------+-----------------------------------------------------------+

+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| Optional Arguments for subscribe   | Description                                                                                                                          |
+====================================+======================================================================================================================================+
| ``--consumer-prefix``              | Consumer prefix URL path. If not present, then its default value will be used instead. Default value: /databus/consumer-service/v1   |
+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--verify-cert-bundle``           | Certificate file name or certificate in String format to be able to hit Streaming Service                                            |
+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--http-proxy``                   | Http Proxy data: whether enabled, FQDN or IP Address, TCP Port. Optional username and password                                       |
+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+

example
^^^^^^^

::

    java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar \
    --operation subscribe \
    --topic topic-1,topic-2,topic-3 \
    --consumer-id 6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7 \
    --cookie ee92c43b218c \
    --domain domain \
    --url http://<0.0.0.0>:<PORT>/consumer-service/v1 \
    --token <TOKEN> \
    --consumer-prefix /databus/consumer-service/v1

.. code:: json

    {
        "code":"204",
        "result":"",
        "options":{
            "cookie":["ee92c43b218c"],
            "consumer-prefix":["/databus/consumer-service/v1"],
            "domain":["domain"],
            "consumer-id":["6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7"],
            "verify-cert-bundle":[""],
            "topic":["topic-1,topic-2,topic-3"],
            "http-proxy":[""],
            "url":["http://<0.0.0.0>:<PORT>/databus/consumer-service/v1"],
            "token":[<TOKEN>]
        }
    }

subscriptions
~~~~~~~~~~~~~

It is an operation argument to get the topics that the channel is
already subscribed to. If operation is successful, then its response is
HTTP 200 with a body showing the subscribed topics.

+-----------------------------------------+-------------------------------------------------------+
| Mandatory Arguments for subscriptions   | Description                                           |
+=========================================+=======================================================+
| ``--consumer-id``                       | Consumer Id previously obtained by create operation   |
+-----------------------------------------+-------------------------------------------------------+
| ``--cookie``                            | Cookie value obtained by create operation             |
+-----------------------------------------+-------------------------------------------------------+
| ``--domain``                            | Cookie Domain value obtained by create operation      |
+-----------------------------------------+-------------------------------------------------------+
| ``--url``                               | Streaming Service base URL                            |
+-----------------------------------------+-------------------------------------------------------+
| ``--token``                             | Identity token gotten by login operation              |
+-----------------------------------------+-------------------------------------------------------+

+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| Optional Arguments for subscribe   | Description                                                                                                                          |
+====================================+======================================================================================================================================+
| ``--consumer-prefix``              | Consumer prefix URL path. If not present, then its default value will be used instead. Default value: /databus/consumer-service/v1   |
+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--verify-cert-bundle``           | Certificate file name or certificate in String format to be able to hit Streaming Service                                            |
+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--http-proxy``                   | Http Proxy data: whether enabled, FQDN or IP Address, TCP Port. Optional username and password                                       |
+------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+

example
^^^^^^^

::

    java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar \
    --operation subscriptions \
    --url http://<0.0.0.0>:<PORT>/ \
    --token <TOKEN> \
    --consumer-id 6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7 \
    --cookie ee92c43b218c \
    --domain domain

.. code:: json

    {
        "code":"200",
        "result":[
            "topic-2-5ca969eb-2757-46ed-bc3f-f9266ccccea7-group0",
            "topic-3-5ca969eb-2757-46ed-bc3f-f9266ccccea7-group0",
            "topic-1-5ca969eb-2757-46ed-bc3f-f9266ccccea7-group0"
        ],
        "options":{
            "cookie":["ee92c43b218c"],
            "consumer-prefix":["/databus/consumer-service/v1"],
            "verify-cert-bundle":[""],
            "consumer-id":["6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7"],
            "domain":["domain"],
            "http-proxy":[""],
            "url":["http://<0.0.0.0>:<PORT>/"],
            "token":[<TOKEN>]
        }
    }

consume
~~~~~~~

It is an operation argument to get the new records received by the
subscribed topics since last call to commit. If operation is successful,
then its response is HTTP 200 with a body showing the consumed records.

+-----------------------------------+-------------------------------------------------------+
| Mandatory Arguments for consume   | Description                                           |
+===================================+=======================================================+
| ``--consumer-id``                 | Consumer Id previously obtained by create operation   |
+-----------------------------------+-------------------------------------------------------+
| ``--cookie``                      | Cookie value obtained by create operation             |
+-----------------------------------+-------------------------------------------------------+
| ``--domain``                      | Cookie Domain value obtained by create operation      |
+-----------------------------------+-------------------------------------------------------+
| ``--url``                         | Streaming Service base URL                            |
+-----------------------------------+-------------------------------------------------------+
| ``--token``                       | Identity token gotten by login operation              |
+-----------------------------------+-------------------------------------------------------+

+----------------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Optional Arguments for consume   | Description                                                                                                                                                       |
+==================================+===================================================================================================================================================================+
| ``--consumer-timeout``           | Time limit to wait for consumer records. If no records are available when the request begins, then request will wait up to this time for new records to arrive.   |
+----------------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| ``--verify-cert-bundle``         | Certificate file name or certificate in String format to be able to hit Streaming Service                                                                         |
+----------------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| ``--http-proxy``                 | Http Proxy data: whether enabled, FQDN or IP Address, TCP Port. Optional username and password                                                                    |
+----------------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------+

example
^^^^^^^

::

    java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar \
    --operation consume \
    --url http://<0.0.0.0>:<PORT>/ \
    --token <TOKEN> \
    --consumer-id 6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7 \
    --cookie ee92c43b218c \
    --domain domain

.. code:: json

    {
        "code":"200",
        "result":[
            {
                "routingData":{
                    "topic":"topic1-5ca969eb-2757-46ed-bc3f-f9266ccccea7",
                    "shardingKey":"pool-1-thread-1-0-1"},
                "message":{
                    "headers":{
                        "sourceId":"abc",
                        "scope":"algo",
                        "tenantId":"5ca969eb-2757-46ed-bc3f-f9266ccccea7",
                        "zoneId":"TMP.Identity.TRUCHATOR"
                    },
                    "payload":"SGVsbG8gV29ybGQgYXQ6MjAxOS0wNS0xNFQxNjoyNDoxMC4xNDAgRXh0cmE6IA=="
                },
                "partition":1,"offset":3
            },
            {
                "routingData":{
                        "topic":"topic1-5ca969eb-2757-46ed-bc3f-f9266ccccea7",
                        "shardingKey":"pool-1-thread-1-0-2"},
                "message":{
                    "headers":{
                        "sourceId":"abc",
                        "scope":"algo",
                        "tenantId":"5ca969eb-2757-46ed-bc3f-f9266ccccea7",
                        "zoneId":"TMP.Identity.TRUCHATOR"
                    },
                    "payload":"SGVsbG8gV29ybGQgYXQ6MjAxOS0wNS0xNFQxNjoyNDoxMC4xNDEgRXh0cmE6IA=="
                },
                "partition":3,"offset":0
            }
        ],
        "options":{
            "cookie":["ee92c43b218c"],
            "consumer-prefix":["/databus/consumer-service/v1"],
            "verify-cert-bundle":[""],
            "consumer-id":["6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7"],
            "domain":["domain"],
            "consume-timeout":[""],
            "http-proxy":[""],
            "url":["http://<0.0.0.0>:<PORT>/"],
            "token":[<TOKEN>]
        }
    }

commit
~~~~~~

It is an operation argument to set the last consumed records to
successfully processed by this consumer group. Consequently, these last
consumed records will not be present in the response to next consume
operation execution.

Note that consume and commit are operations done on a consumer group
basis. If a consumer of a given consumer group consumes records and
commits afterwards, then next consume operations from any consumer of
such consumer group will no longer contain those records. However, those
records are still available to consumers of other consumer groups as
long as they have not consumed them yet and their retention period has
not expired.

+----------------------------------+-------------------------------------------------------+
| Mandatory Arguments for commit   | Description                                           |
+==================================+=======================================================+
| ``--consumer-id``                | Consumer Id previously obtained by create operation   |
+----------------------------------+-------------------------------------------------------+
| ``--cookie``                     | Cookie value obtained by create operation             |
+----------------------------------+-------------------------------------------------------+
| ``--domain``                     | Cookie Domain value obtained by create operation      |
+----------------------------------+-------------------------------------------------------+
| ``--url``                        | Streaming Service base URL                            |
+----------------------------------+-------------------------------------------------------+
| ``--token``                      | Identity token gotten by login operation              |
+----------------------------------+-------------------------------------------------------+

+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| Optional Arguments for commit   | Description                                                                                                                          |
+=================================+======================================================================================================================================+
| ``--consumer-prefix``           | Consumer prefix URL path. If not present, then its default value will be used instead. Default value: /databus/consumer-service/v1   |
+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--verify-cert-bundle``        | Certificate file name or certificate in String format to be able to hit Streaming Service                                            |
+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--http-proxy``                | Http Proxy data: whether enabled, FQDN or IP Address, TCP Port. Optional username and password                                       |
+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+

example
^^^^^^^

::

    java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar \
    --operation commit \
    --url http://<0.0.0.0>:<PORT>/ \
    --token <TOKEN> \
    --consumer-id 6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7 \
    --cookie ee92c43b218c \
    --domain domain

.. code:: json

    {
        "code":"204",
        "result":"",
        "options":{
            "cookie":["ee92c43b218c"],
            "consumer-prefix":["/databus/consumer-service/v1"],
            "verify-cert-bundle":[""],
            "consumer-id":["6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7"],
            "domain":["domain"],
            "http-proxy":[""],
            "url":["http://<0.0.0.0>:<PORT>/"],
            "token":[<TOKEN>]
        }
    }

delete
~~~~~~

It is an operation argument to delete the consumer identified by its
consumer-id. Once deleted, the consumer will fail in all subsequent
operations, e.g.: such consumer-id can no longer subscribe, get
subscriptions, consume or commit.

+----------------------------------+-------------------------------------------------------+
| Mandatory Arguments for delete   | Description                                           |
+==================================+=======================================================+
| ``--consumer-id``                | Consumer Id previously obtained by create operation   |
+----------------------------------+-------------------------------------------------------+
| ``--cookie``                     | Cookie value obtained by create operation             |
+----------------------------------+-------------------------------------------------------+
| ``--domain``                     | Cookie Domain value obtained by create operation      |
+----------------------------------+-------------------------------------------------------+
| ``--url``                        | Streaming Service base URL                            |
+----------------------------------+-------------------------------------------------------+
| ``--token``                      | Identity token gotten by login operation              |
+----------------------------------+-------------------------------------------------------+

+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| Optional Arguments for delete   | Description                                                                                                                          |
+=================================+======================================================================================================================================+
| ``--consumer-prefix``           | Consumer prefix URL path. If not present, then its default value will be used instead. Default value: /databus/consumer-service/v1   |
+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--verify-cert-bundle``        | Certificate file name or certificate in String format to be able to hit Streaming Service                                            |
+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
| ``--http-proxy``                | Http Proxy data: whether enabled, FQDN or IP Address, TCP Port. Optional username and password                                       |
+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+

example
^^^^^^^

::

    java -jar opendxlstreamingclient-java-sdk-<VERSION>.jar \
    --operation delete \
    --url http://<0.0.0.0>:<PORT>/ \
    --token <TOKEN> \
    --consumer-id 6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7 \
    --cookie ee92c43b218c \
    --domain domain

.. code:: json

    {
        "code":"204",
        "result":"",
        "options":{
            "cookie":["ee92c43b218c"],
            "consumer-prefix":["/databus/consumer-service/v1"],
            "verify-cert-bundle":[""],
            "consumer-id":["6d1dfd66-61f2-4525-ae70-e00ca6a9ffd78e9c79e2-26e6-4cd7-8e2d-75ce2a868cd7"],
            "domain":["domain"],
            "http-proxy":[""],
            "url":["http://<0.0.0.0>:<PORT>/"],
            "token":[<TOKEN>]
        }
    }

