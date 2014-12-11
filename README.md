xio
===

High performance Multithreaded non-blocking Async I/O for Java 8

`Simplicity Leads to Purity - Jiro`

###Server
```java
Server server = new Server();
server.addKey("resourcse/my.key");
server.addCsr("resources/my.csr");
server.serve(443);
server.addRoute("/", rootService);
```

###Service
```java
Service rootService = new Service();
rootService.proto("http/2");
rootService.addFilter(new timeoutFilert)
            .andThen(new ratelimitFilter)
            .andThen(new oAuthService);
```

###Filter
```java
// RateLimitFilter is specified as # of Connections, to what, over what period
Filter ratelimitFilter = new RateLimitFilter(200, perHost, perSecond)
```

