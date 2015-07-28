### Status
[![Build Status](https://travis-ci.org/xjdr/xio.png)](https://travis-ci.org/xjdr/xio)


CURRENTLY DEPRICATED IN FAVOR OF XIO2
=====================================

[xio2](https://github.com/xjdr/xio2)


xio
===

High performance Multithreaded non-blocking Async I/O for Java 8

`Simplicity Leads to Purity - Jiro`


### This is meant to be a stable of tools to allow you to write highly performant Client <-> Server | Server <-> Server code in Java 8.

## Problem Statment

In looking to write high performance code for Client <-> Server and Server <-> Server applications
it became apparent that I would need to learn many different tools and then write my own middleware
to make them play nicely together. Mastering many tool sets and then writing your own middleware is
not scalable, especially as the development cycles for these tools change at drastically different Speeds.
It seems to me that there wasn't a Simple, Powerful, Scalable and Fast tools that worked for client and server.

### Candidates evaluated
I've written these systems professionally and have used the following systems:

- Netty [Powerful, Scalable, Fast(ish)]
- Jetty [Simple(ish)]
- Jetty/Guice [Simple(ish), Powerful]
- Finagle [Powerful, Scalable]
- JBOSS/J2EE [None of the Above]

### Initial use cases:
- System Front end (What most of us use Nginx for these days)

- High speed Andriod Client and Server

- Server to Server API backend

- Simple Http server

## Server Quickstart

####Server
```java
Server server = new Server();

server.addRoute("/", new rootService);
server.addRoute("/health", new heathService);
server.addRoute("/admin", new adminService);

server.serve(8080);
```

####Service
```java
Service rootService = new Service();
rootService.andThen(new RatelimitFilter)
           .andThen(new OAuthService);
```

## Client Quickstart
```java
Client xioClient =  new XioClient("get", "https://github.com/users");
xioClient.response    // will return an int of the returncode i.e 200
xioClient.headers     // will dump the headers as a HashMap
xioClient.body        // will dump the body of the HTTP response as a string
xioClient.body.toJson // will parse the body of the response as json

Client xioRestClient = new XioClient();
xioRestClient.method(post);
xioRestClient.url("https://github.com/users");
xioRestClient.addHeader("X-Auth: My Voice is My Password");
Future<Http2Response> response = xioRestClient.send;
response.onFailure(e -> new XioClientException(e));
response.onSuccess(r -> System.out.println(r.body.toString));

Client xioFancyClient = new xioClient();
xioFancyClient.proto(HTTP/2);
xioFancyClient.auth("resources/myAuth.key");
xioFancyClient.secret("mySecret");
xioFancyClient.hosts("/my/server/v1/, ("zk://localhost:2181", "zk://localhost:2182", "zk://localhost:2183"));
xioFancyClient.lb(roundRobin, 3, false) // load-balancer type, number of retries before ejection, auto rejoing to cluster
xioFancyClient.method(get);
Future<Response> response = xioFancyClient.send;
response.onFailure(e -> new XioClientException(e));
response.onSuccess(r -> System.out.println(r.body.toString));
```
