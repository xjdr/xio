#!/bin/bash

NAME=$1

# Create Launch Configuration
aws autoscaling create-launch-configuration --launch-configuration-name $NAME \
  --image-id ami-c00072b8 --key-name kratos --security-groups sg-adadbed3 --instance-type t2.micro \
  --region us-west-2 --profile nordstrom-federated

# Spin up the ASG
aws autoscaling create-auto-scaling-group --auto-scaling-group-name $NAME \
  --launch-configuration-name $NAME --min-size 0 --max-size 0 \
  --vpc-zone-identifier subnet-3bac8a60 --region us-west-2 --profile nordstrom-federated
