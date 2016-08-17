export PROJECT_ROOT=$(shell pwd)
export TARGET_DIR := $(PROJECT_ROOT)/target

# thrift

THRIFT_DIR = src/main/java/com/xjeffrose/xio/marshall/thrift
THRIFT_SRC = $(shell cd src/main/thrift; ls *.thrift)
THRIFT_OUT = $(THRIFT_SRC:%.thrift=$(THRIFT_DIR)/%.java)

$(THRIFT_DIR)/%.java: src/main/thrift/%.thrift
	thrift --gen java -out src/main/java $<

JAVAC = javac
JFLAGS = -g

# The directory root where the Java source files are found
JAVA_SRC_DIR = src

JAVA_SRC_ROOTS = example main test

# The directory root where generated .d files go
DEP_DIR = depend

EXAMPLE_SRC = $(shell cd src/example/java; find com -name '*.java')
EXAMPLE_OBJ = $(EXAMPLE_SRC:%.java=$(TARGET_DIR)/%.class)

MAIN_SRC = $(shell cd src/main/java; find com -name '*.java')
MAIN_OBJ = $(MAIN_SRC:%.java=$(TARGET_DIR)/%.class)

TEST_SRC = $(shell cd src/test/java; find com -name '*.java')
TEST_OBJ = $(TEST_SRC:%.java=$(TARGET_DIR)/%.class)

PROJECT_JAR = xio.jar
PROJECT_DEP = $(EXAMPLE_SRC:%.java=$(DEP_DIR)/%.d) $(MAIN_SRC:%.java=$(DEP_DIR)/%.d) $(TEST_SRC:%.java=$(DEP_DIR)/%.d)

DIRS = $(DEP_DIR) $(TARGET_DIR)

# declare phonies and display a useful help message
.PHONY: all checkstyle check-syntax clean compile fetch jar repl run thrift test
all:
	@echo
	@echo "  checkstyle - run checkstyle over the entire project"
	@echo "  check-syntax - run ECJ over the entire project"
	@echo "  clean - delete build artifacts and generated files"
	@echo "  compile - build examples, main, and test"
	@echo "  fetch - download all dependencies"
	@echo "  jar - build $(PROJECT_JAR)"
	@echo "  repl - run the java repl"
	@echo "  run - compile and run $(MAIN_CLASS)"
	@echo "  thrift - generate thrift sources"
	@echo "  test - compile and test"
	@echo

# disable implicit rules
.SUFFIXES:
%:: %,v
%:: RCS/%,v
%:: RCS/%
%:: s.%
%:: SCCS/s.%

include Classpath.mk
include Dependencies.mk

Generated.mk: Makefile Dependencies.mk lib/*.jar
	echo > Generated.mk
	echo "export JAR_ECJ := $$(coursier fetch -p $(DEP_ECJ))" >> Generated.mk
	echo "export LOMBOK_JAR := $$(coursier fetch -p $(DEP_LOMBOK))" >> Generated.mk
	echo "export CLASSPATH_VENDOR := $(shell ls lib/*.jar | sed -e s%^%${PWD}/% | tr '\n' ':' | sed -e 's/jar:$$/jar/')" >> Generated.mk
	echo "export CLASSPATH_COURSIER := $$(coursier fetch -p $(DEPS_COMPILE))" >> Generated.mk
	echo 'export CLASSPATH_COMPILE := $$(CLASSPATH_VENDOR):$$(CLASSPATH_COURSIER)' >> Generated.mk

-include Generated.mk

$(DIRS):
	mkdir -p $@

$(TARGET_DIR)/%.class: src/example/java/%.java
	touchp $@

$(TARGET_DIR)/%.class: src/main/java/%.java
	touchp $@

$(TARGET_DIR)/%.class: src/test/java/%.java
	touchp $@

target/.example_compiled: $(EXAMPLE_OBJ)
	scripts/run-compile $(?:$(TARGET_DIR)/%.class=src/example/java/%.java)
	jdep -c $(TARGET_DIR) -j src/example/java -d $(DEP_DIR) -i com.xjeffrose.xio $?
	touch $@

target/.main_compiled: $(MAIN_OBJ)
	scripts/run-compile $(?:$(TARGET_DIR)/%.class=src/main/java/%.java)
	jdep -c $(TARGET_DIR) -j src/main/java -d $(DEP_DIR) -i com.xjeffrose.xio $?
	touch $@

target/.test_compiled: $(TEST_OBJ)
	scripts/run-compile $(?:$(TARGET_DIR)/%.class=src/test/java/%.java)
	jdep -c $(TARGET_DIR) -j src/test/java -d $(DEP_DIR) -i com.xjeffrose.xio $?
	touch $@

$(PROJECT_JAR):
	cd $(TARGET_DIR); jar cf ../$@ `find com -name '*.class'`

$(TARGET_DIR)/%.class: $(JAVA_SRC_DIR)/%.java
	touchp $@

-include $(PROJECT_DEP)

# copy main resources into the target dir

MAIN_RESOURCES = $(shell cd src/main; find resources -type f -print | sed -e 's+resources+$(TARGET_DIR)+')

$(MAIN_RESOURCES): $(TARGET_DIR)/% : src/main/resources/%
	cp $< $@

# copy test resources into the target dir

TEST_RESOURCES = $(shell cd src/test; find resources -type f -print | sed -e 's+resources+$(TARGET_DIR)+')

$(TEST_RESOURCES): $(TARGET_DIR)/% : src/test/resources/%
	cp $< $@

RESOURCES := $(MAIN_RESOURCES) $(TEST_RESOURCES)

# phonies

checkstyle:
	drip -Dcheckstyle.cache.file=checkstyle.cache -cp `coursier fetch -p $(DEPS_ALL)` com.puppycrawl.tools.checkstyle.Main -c checkstyle.xml src/main

check-syntax:
	@for dir in $$(find src -type d); do \
		[[ -e $$dir/Makefile ]] && (echo $$dir; make -C $$dir check-syntax); \
	done

clean:
	rm Generated.mk
	rm -fr $(DIRS)

compile: $(DIRS) target/.main_compiled target/.test_compiled target/.example_compiled  $(RESOURCES)

fetch:
	@coursier fetch --verbose $(DEPS_ALL)

jar: compile $(PROJECT_JAR)

repl:
	@echo ${CLASSPATH_COMPILE}:${TARGET_DIR} | sed -e 's/^/:/' | sed -e 's/:/|:cp /g' | tr '|' '\n'
	@echo
	@javarepl

run: compile
	java -cp $(CLASSPATH_COMPILE):$(TARGET_DIR) $(MAIN_CLASS)

test: compile
	scripts/run-test $$(find src/test -name "*Test.java")

thrift: $(THRIFT_OUT)
