all: xio/core xio/server xio/client/lb xio/client/retry xio/client xio/ssl xio/log xio/mux xio/proxy

PROJECT_ROOT=$(shell pwd)

include Classpath.mk

repl:
	@echo $(MAVEN_CLASSPATH) | sed -e 's/^/:/' | sed -e 's/:/|:cp /g' | tr '|' '\n'
	@echo
	@javarepl

target:
	mkdir -p target

xio/core: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/core

xio/client: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/client

xio/client/lb: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/client/loadbalancer

xio/client/retry: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/client/retry

xio/server: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/server

xio/ssl: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/SSL

xio/log: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/log

xio/mux: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/mux

xio/proxy: target
	$(MAKE) -C src/main/java/com/xjeffrose/xio/proxy

test:
	java -cp lib/*.jar org.junit.runner.JUnitCore src/test/com/xjeffrose/xio/*.java

jar:
	jar cvf test.jar target/test/mod1/*.class target/test/mod2/*.class target/chica/*.class
	jar cfe test.jar test.Main.main target/test/Main.class
