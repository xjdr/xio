DEP_CONFIG=com.typesafe:config:1.3.0
DEP_FINDBUGS=com.google.code.findbugs:jsr305:3.0.1
DEP_GUAVA=com.google.guava:guava:19.0
DEP_LOMBOK=org.projectlombok:lombok:1.16.8
DEP_NETTY=io.netty:netty-all:4.1.0.Final
DEP_NETTY_TCNATIVE=io.netty:netty-tcnative:1.1.33.Fork17
DEP_NETTY_SSL=io.netty:netty-tcnative-boringssl-static:1.1.33.Fork17
DEP_AIRLIFT=io.airlift:units:0.128
DEP_CURATOR=org.apache.curator:curator-framework:3.1.0
DEP_CURATOR_RECIPES=org.apache.curator:curator-recipes:3.1.0
DEP_JUNIT=junit:junit:4.12
DEP_MOCKITO=org.mockito:mockito-all:1.10.19
DEP_JETTY=org.eclipse.jetty:jetty-server:9.3.1.v20150714
DEP_OKHTTP=com.squareup.okhttp:okhttp:2.4.0
DEP_LOG4J=log4j:log4j:1.2.17

DEP_CHECKSTYLE=com.puppycrawl.tools:checkstyle:6.19

DEP_ECJ=org.eclipse.jdt.core.compiler:ecj:4.5.1

DEPS_COMPILE=$(DEP_CONFIG) \
             $(DEP_FINDBUGS) \
             $(DEP_GUAVA) \
             $(DEP_LOMBOK) \
             $(DEP_NETTY) \
             $(DEP_NETTY_TCNATIVE) \
             $(DEP_NETTY_SSL) \
             $(DEP_AIRLIFT) \
             $(DEP_CURATOR) \
             $(DEP_CURATOR_RECIPES) \
             $(DEP_JUNIT) \
             $(DEP_MOCKITO) \
             $(DEP_JETTY) \
             $(DEP_OKHTTP) \
             $(DEP_LOG4J)

DEPS_ALL=$(DEPS_COMPILE) $(DEP_CHECKSTYLE) $(DEP_ECJ)
