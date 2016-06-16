.PHONY: check-syntax

PROJECT_ROOT=$(shell git rev-parse --show-toplevel)
export TARGETDIR := $(PROJECT_ROOT)/target/

include $(PROJECT_ROOT)/*.mk

all:
	$(PROJECT_ROOT)/scripts/run-compile *.java

$(PROJECT_ROOT)/target:
	mkdir -p $(PROJECT_ROOT)/target

check-syntax:
	$(PROJECT_ROOT)/scripts/run-check-syntax

test:
	$(PROJECT_ROOT)/scripts/run-test
