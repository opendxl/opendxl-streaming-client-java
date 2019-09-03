OpenDXL Streaming Java Client Library
=====================================

See the navigation bar on the right for an overview of the OpenDXL Streaming
Java Client Library and examples.

Overview
--------
The OpenDXL Streaming Java client library is used to consume records
from a `Data Exchange Layer <http://www.mcafee.com/us/solutions/data-exchange-layer.aspx>`_
(DXL) Streaming Service.

The DXL Streaming Service exposes a REST-based API that communicates
with a back-end streaming platform. The streaming service
performs authentication and authorization and exposes methods to retrieve records.

One concrete example of a DXL Streaming Service is the `McAfee MVISION EDR <https://www.mcafee.com/enterprise/en-us/products/investigator.html>`_ "Events feed".

Installation
------------
To start using the OpenDXL Streaming Java client library:

* Download the `Latest Release <https://github.com/opendxl/opendxl-streaming-client-java/releases>`_
* Extract the release .zip file
* View the ``README.html`` file located at the root of the extracted files.
    * The ``README`` links to the documentation which includes installation instructions and usage examples.
    * The Javadoc API documentation is also available on-line `here <https://opendxl.github.io/opendxl-streaming-client-java/docs/javadoc/index.html>`_.

Maven Repository
----------------
Visit the `OpenDXL Streaming Java Client Maven Repository <https://search.maven.org/artifact/com.opendxl/dxlstreaming>`_ for
access to all released versions including the appropriate dependency syntax for a large number of management
systems (Maven, Gradle, SBT, Ivy, Grape, etc.).

Maven:

    .. code-block:: xml

        <dependency>
          <groupId>com.opendxl.streaming</groupId>
          <artifactId>opendxlstreamingclient-java-sdk</artifactId>
          <version>0.2.0</version>
        </dependency>

Gradle:

    .. code-block:: groovy

        compile 'com.opendxl.streaming:opendxlstreamingclient-java-sdk:0.2.0'

API Documentation
-----------------
* `JavaDoc API Documentation <https://opendxl.github.io/opendxl-streaming-client-java/docs/javadoc/index.html>`_

Table of content
----------------

.. toctree::
	:maxdepth: 1

	Prerequisites.rst

.. toctree::
	:maxdepth: 1

	Basic-Consume-(Token-based-authentication).rst

.. toctree::
	:maxdepth: 1

	Basic-Consume-(user-and-pass-authentication).rst

.. toctree::
	:maxdepth: 1

	Basic-Consume-(command-line-interface).rst