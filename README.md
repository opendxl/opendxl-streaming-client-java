# OpenDXL Streaming Java Client Library

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://github.com/opendxl/opendxl-streaming-client-java/workflows/build/badge.svg?branch=master)](https://github.com/opendxl/opendxl-streaming-client-java/actions)

## Overview

The OpenDXL Streaming Java client library is used to consume records
from as well as to produce records to a
[Data Exchange Layer](http://www.mcafee.com/us/solutions/data-exchange-layer.aspx)
(DXL) Streaming Service.

The DXL Streaming Service exposes a REST-based API that communicates with a
back-end streaming platform (Kafka, Kinesis, etc.). The streaming service
performs authentication and authorization and exposes methods to retrieve records and to send them.

One concrete example of a DXL Streaming Service is the
[McAfee MVISION EDR](https://www.mcafee.com/enterprise/en-us/products/investigator.html)
"Events feed".

## Documentation

See the
[Wiki](https://github.com/opendxl/opendxl-streaming-client-java/wiki)
for an overview of the OpenDXL Streaming Java client library and
examples.

See the
[OpenDXL Streaming Java Client Library Documentation](https://opendxl.github.io/opendxl-streaming-client-java/docs/index.html)
for API documentation and examples.

## Installation

To start using the OpenDXL Streaming Java Client Library:

* Download the [Latest Release](https://github.com/opendxl/opendxl-streaming-client-java/releases/latest)
* Extract the release .zip file
* View the `README.html` file located at the root of the extracted files.
  * The `README` links to the documentation which includes installation instructions, API details, and samples.
  * The SDK documentation is also available on-line [here](https://opendxl.github.io/opendxl-streaming-client-java/docs/javadoc/index.html).

## Maven Repository

Visit the [OpenDXL Streaming Java Client Maven Repository](https://search.maven.org/artifact/com.opendxl/dxlstreaming) for
access to all released versions including the appropriate dependency syntax for a large number of management 
systems (Maven, Gradle, SBT, Ivy, Grape, etc.).

Maven:

```xml
<dependency>
  <groupId>com.opendxl</groupId>
  <artifactId>dxlstreamingclient</artifactId>
  <version>0.1.2</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.opendxl:dxlstreamingclient:0.1.2'
```

## Bugs and Feedback

For bugs, questions and discussions please use the
[GitHub Issues](https://github.com/opendxl/opendxl-streaming-client-java/issues).

## LICENSE

Copyright 2018, McAfee LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

