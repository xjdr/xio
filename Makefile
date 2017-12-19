# thrift

THRIFT_MARSHALL_SRC = $(shell cd src/main/thrift/marshall; ls *.thrift)
THRIFT_MARSHALL_DIR_JAVA = src/main/java/com/xjeffrose/xio/marshall/thrift
THRIFT_CONFIGURATOR_SRC = $(shell cd src/main/thrift/configurator; ls *.thrift)
THRIFT_CONFIGURATOR_DIR_JAVA = src/main/java/com/xjeffrose/xio/config/thrift
THRIFT_OUT_JAVA = $(THRIFT_MARSHALL_SRC:%.thrift=$(THRIFT_MARSHALL_DIR_JAVA)/%.java) $(THRIFT_CONFIGURATOR_SRC:%.thrift=$(THRIFT_CONFIGURATOR_DIR_JAVA)/%.java)

$(THRIFT_MARSHALL_DIR_JAVA)/%.java: src/main/thrift/marshall/%.thrift
	thrift --gen java -out src/main/java $<

$(THRIFT_CONFIGURATOR_DIR_JAVA)/%.java: src/main/thrift/configurator/%.thrift
	thrift --gen java -out src/main/java $<

THRIFT_CONFIGURATOR_DIR_PY = configuration-client/configurator/thriftgen
THRIFT_OUT_PY = $(THRIFT_MARSHALL_SRC:%.thrift=$(THRIFT_CONFIGURATOR_DIR_PY)/%/ttypes.py) $(THRIFT_CONFIGURATOR_SRC:%.thrift=$(THRIFT_CONFIGURATOR_DIR_PY)/%/ttypes.py)
$(THRIFT_CONFIGURATOR_DIR_PY)/%/ttypes.py: src/main/thrift/configurator/%.thrift
	thrift --gen py -out configuration-client $<

$(THRIFT_CONFIGURATOR_DIR_PY)/%/ttypes.py: src/main/thrift/marshall/%.thrift
	thrift --gen py -out configuration-client $<

# declare phonies and display a useful help message
.PHONY: all checkstyle check-syntax clean compile fetch jar repl run thrift test
all:
	@echo
	@echo "  checkstyle - run checkstyle over the entire project"
	@echo "  thrift - generate thrift sources"
	@echo

# disable implicit rules
.SUFFIXES:
%:: %,v
%:: RCS/%,v
%:: RCS/%
%:: s.%
%:: SCCS/s.%

# phonies

checkstyle:
	mvn checkstyle:check

thrift: $(THRIFT_OUT_JAVA) $(THRIFT_OUT_PY)
