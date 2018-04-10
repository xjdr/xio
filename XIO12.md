# What's new in xio 0.12

* /configuration-client
  * python client for manipulating runtime settings in xio applications
* /configuration-server
  * minimal implementation of an xio application for testing configuration client
* /example-trailhead
  * example implementation of a reverse proxy server
* /log4j-formatter
  * previously part of core xio, now lives in it's own sub-project
* /src/example/java/com/xjeffrose/xio/client/chicago
  * partially implemented chicago client example
* /src/main/java/com/xjeffrose/xio/application
  * classes for storing state and configuration across an entire application
* /src/main/java/com/xjeffrose/xio/bootstrap
  * classes for bootstrapping various xio objects (Application, Server, Client)
* /src/main/java/com/xjeffrose/xio/client
  * XXX: client has changed
* /src/main/java/com/xjeffrose/xio/config
  * classes for runtime settings
* /src/main/java/com/xjeffrose/xio/core
  * XXX: core has changed
* /src/main/java/com/xjeffrose/xio/filter
  * Filters used block/allow ip/http traffic
* /src/main/java/com/xjeffrose/xio/guice
  * Google Guice is no longer supported
* /src/main/java/com/xjeffrose/xio/marshall
  * classes for marshalling runtime settings to/from storage
* /src/main/java/com/xjeffrose/xio/mux
  * classes for muxing multiple connections over a single connection
* /src/main/java/com/xjeffrose/xio/pipeline
  * xio pipelines bundle complex server configurations into simple classes
* /src/main/java/com/xjeffrose/xio/processor
  * XioProcessor is no longer supported
* /src/main/java/com/xjeffrose/xio/server
  * XXX: server has changed
* /src/main/java/com/xjeffrose/xio/storage
  * storage backends, currently only ZooKeeper
* /src/main/resources/reference.conf and /src/main/resources/tls.conf
  * Most configuration now done through Typesafe Config
* /src/main/thrift
  * thrift files for runtime configuration and storage marshalling
