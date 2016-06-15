.PHONY: check-syntax

PROJECT_ROOT=../../../../../../../../
TARGETDIR=$(PROJECT_ROOT)target/

include $(PROJECT_ROOT)*.mk

all:
	infer -o /tmp/infer -- javac $(JAVA_ARGS) -cp $(MAVEN_CLASSPATH):$(TARGETDIR) -d $(TARGETDIR) *.java

$(PROJECT_ROOT)/target:
	mkdir -p $(PROJECT_ROOT)/target

check-syntax:
	javac $(JAVA_ARGS) -cp $(MAVEN_CLASSPATH):$(TARGETDIR) -Xlint -d $(TARGETDIR) *.java

test:
	java -cp $(PROJECT_ROOT)lib/*.jar: org.junit.runner.JUnitCore *.java
