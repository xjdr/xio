.PHONY: check-syntax

PROJECT_ROOT=../../../../../../../
TARGETDIR=$(PROJECT_ROOT)target/

include $(PROJECT_ROOT)*.mk

all:
	javac $(JAVA_ARGS) -cp $(MAVEN_CLASSPATH):$(TARGETDIR) -d $(TARGETDIR) *.java

$(PROJECT_ROOT)/target:
	mkdir -p $(PROJECT_ROOT)/target

check-syntax:
	javac $(JAVA_ARGS) -cp $(MAVEN_CLASSPATH):$(TARGETDIR) -Xlint -d $(TARGETDIR) *.java

test:
	echo $(shell $(PROJECT_ROOT)scripts/file2class $(FILE_NAME))
	javac $(JAVA_ARGS) -cp $(MAVEN_CLASSPATH):$(TARGETDIR):$(PROJECT_ROOT)lib/junit-4.12.jar: -d $(TARGETDIR) $(FILE_NAME)
	java -cp $(MAVEN_CLASSPATH):$(TARGETDIR):$(PROJECT_ROOT)lib/junit-4.12.jar::$(PROJECT_ROOT)lib/hamcrest-core-1.3.jar: org.junit.runner.JUnitCore $(shell $(PROJECT_ROOT)scripts/file2class $(FILE_NAME))
