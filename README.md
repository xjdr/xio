xio
===

High performance Multithreaded non-blocking Async I/O for Java 8

`Simplicity Leads to Purity - Jiro`


## Server Quickstart


####Server
```java
Server server = new Server();
server.addKey("resourcse/my.key");
server.addCsr("resources/my.csr");
server.announce("/my/server/v1/, ("zk://localhost:2181", "zk://localhost:2182", "zk://localhost:2183"));
server.serve(443);
server.addRoute("/", new RootService());
server.addRoute("/health", new HeathService());
server.addRoute("/admin", new AdminService);

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
xioClient.response // will return an int of the returncode i.e 200
xioClient.headers // will dump the headers as a HashMap
xioClient.text // will dump the body of the HTTP response as a string
xioClient.test.toJson // will parse the body of the response as json

Client xioRestClient = new XioClient();
xioRestClient.method(post);
xioRestClient.url("https://github.com/users");
xioRestClient.addHeader("X-Auth: My Voice is My Password");
Future<Response> response = xioRestClient.send;

Client xioFancyClient = new xioClient();
xioFancyClient.proto(HTTP/2);
xioFancyClient.auth("resources/myAuth.key");
xioFancyClient.secret("mySecret");
xioFancyClient.hosts("/my/server/v1/, ("zk://localhost:2181", "zk://localhost:2182", "zk://localhost:2183"));mySecret");
xioFancyClient.method(get);
Future<Response> response = xioFancyClient.send;
```
