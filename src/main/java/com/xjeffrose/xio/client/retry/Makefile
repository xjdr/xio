.PHONY: check-syntax

export PROJECT_ROOT=$(shell git rev-parse --show-toplevel)
export TARGET_DIR := $(PROJECT_ROOT)/target/

include $(PROJECT_ROOT)/*.mk

all:
	$(PROJECT_ROOT)/scripts/run-compile *.java

$(PROJECT_ROOT)/target:
	mkdir -p $(PROJECT_ROOT)/target

check-syntax:
	$(PROJECT_ROOT)/scripts/run-check-syntax

test: compile-all
	$(PROJECT_ROOT)/scripts/run-test

compile-all:
	make -C $(PROJECT_ROOT) compile

test-all:
	make -C $(PROJECT_ROOT) test

watch-test:
	make -C $(PROJECT_ROOT) watch-test
