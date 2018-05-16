#!/bin/bash

NAME=$1
MIN=$2
MAX=$3
PROFILE=$4

aws autoscaling update-auto-scaling-group --auto-scaling-group-name $NAME --min-size $MIN --max-size $MAX \
  --region us-west-2 --profile $PROFILE

