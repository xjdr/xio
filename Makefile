export PROJECT_ROOT=$(shell pwd)
export TARGETDIR := $(PROJECT_ROOT)/target
export TARGET_DIR := $(PROJECT_ROOT)/target

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

.PHONY: all clean compile
# disable implicit rules
.SUFFIXES:
%:: %,v
%:: RCS/%,v
%:: RCS/%
%:: s.%
%:: SCCS/s.%
all:
	@echo "compile - build examples, main, and test"
	@echo "jar - build $(PROJECT_JAR)"

include Classpath.mk
include Dependencies.mk

Generated.mk: Dependencies.mk
	echo > Generated.mk
	echo "export JAR_ECJ := $$(coursier fetch -p $(DEP_ECJ))" >> Generated.mk
	echo "export CLASSPATH_COMPILE := $$(coursier fetch -p $(DEPS_COMPILE))" >> Generated.mk

-include Generated.mk

repl:
	@echo ${CLASSPATH_COMPILE}:${TARGET_DIR} | sed -e 's/^/:/' | sed -e 's/:/|:cp /g' | tr '|' '\n'
	@echo
	@javarepl

fetch:
	@coursier fetch --verbose $(DEPS_ALL)

checkstyle:
	drip -Dcheckstyle.cache.file=checkstyle.cache -cp `coursier fetch -p $(DEPS_ALL)` com.puppycrawl.tools.checkstyle.Main -c checkstyle.xml src/main

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

# copy test resources into the target dir

TEST_RESOURCES = $(shell cd src/test; find resources -type f -print | sed -e 's+resources+$(TARGET_DIR)+')

$(TEST_RESOURCES): $(TARGET_DIR)/% : src/test/resources/%
	cp $< $@

# phonies

clean:
	rm Generated.mk
	rm -fr $(DIRS)

compile: $(DIRS) target/.main_compiled target/.test_compiled target/.example_compiled  $(TEST_RESOURCES)

jar: compile $(PROJECT_JAR)

test: compile
	scripts/run-test $$(find src/test -name "*Test.java")
