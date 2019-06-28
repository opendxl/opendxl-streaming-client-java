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