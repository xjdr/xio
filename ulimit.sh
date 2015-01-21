#!/bin/bash

sudo sh -c "ulimit -n 65535 && exec su $LOGNAME"

