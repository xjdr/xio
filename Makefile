.PHONY: check-syntax

all: xio/core xio/client xio/server xio/ssl xio/log xio/mux xio/proxy

xio/core:
	$(MAKE) -C src/main/java/com/xjeffrose/xio/core

xio/client:
	$(MAKE) -C src/main/java/com/xjeffrose/xio/client

xio/server:
	$(MAKE) -C src/main/java/com/xjeffrose/xio/server

xio/ssl:
	$(MAKE) -C src/main/java/com/xjeffrose/xio/SSL

xio/log:
	$(MAKE) -C src/main/java/com/xjeffrose/xio/log

xio/mux:
	$(MAKE) -C src/main/java/com/xjeffrose/xio/mux

xio/proxy:
	$(MAKE) -C src/main/java/com/xjeffrose/xio/proxy

test:
	java -cp lib/*.jar org.junit.runner.JUnitCore src/test/com/xjeffrose/xio/*.java

jar:
	jar cvf test.jar target/test/mod1/*.class target/test/mod2/*.class target/chica/*.class
	jar cfe test.jar test.Main.main target/test/Main.class