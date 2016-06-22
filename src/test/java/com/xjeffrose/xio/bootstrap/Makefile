.PHONY: check-syntax

export PROJECT_ROOT=$(shell git rev-parse --show-toplevel)
export TARGETDIR := $(PROJECT_ROOT)/target/

include $(PROJECT_ROOT)/Generated.mk

all:
	$(PROJECT_ROOT)/scripts/run-compile *.java

$(PROJECT_ROOT)/target:
	mkdir -p $(PROJECT_ROOT)/target

check-syntax:
	$(PROJECT_ROOT)/scripts/run-check-syntax

test:
	NOINFER=1 make -C $(PROJECT_ROOT) compile-src-and-test
	$(PROJECT_ROOT)/scripts/run-test

compile-all:
	make -C $(PROJECT_ROOT) compile

test-all:
	make -C $(PROJECT_ROOT) test

watch-test:
	make -C $(PROJECT_ROOT) watch-test
