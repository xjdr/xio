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

- Simple Http/2 server

### I wanted to write a toolset that I could use to build large projects time and time again.
### Right now this framework only supports HTTP/2 but I am hoping to add redis and thrift (maybe avro) soon.
### Hopefully you will find it useful too.

## Server Quickstart


####Server
```java
Server server = new Server();
server.addKey("resourcse/my.key");
server.addCsr("resources/my.csr");
server.announce("/my/server/v1/, ("zk://localhost:2181", "zk://localhost:2182", "zk://localhost:2183"));
server.port(443);
server.tls(true);
server.addRoute("/", rootService);
server.addRoute("/health", heathService);
server.addRoute("/admin", adminService);
Await.ready(server.serve());
```

####Service
```java
Service rootService = new Service();
rootService.proto(HTTP/2);
rootService.addFilter(timeoutFilert)
            .andThen(ratelimitFilter)
            .andThen(oAuthService);
```

####Filter
```java
// RateLimitFilter is specified as # of Connections, to what, over what period
Function<Channel, Filter> ratelimitFilter = new RateLimitFilter(200, perHost, perSecond)
ratelimitFilter.hosts("/my/server/v1/, ("zk://localhost:2181", "zk://localhost:2182", "zk://localhost:2183"));
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
