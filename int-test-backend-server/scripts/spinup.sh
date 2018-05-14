#!/bin/bash

NAME=$1
KEY=$2
AMI=$3
SG=$4
PROFILE=$5
SUBNET=$6

# Create Launch Configuration
aws autoscaling create-launch-configuration --launch-configuration-name $NAME \
  --image-id $AMI --key-name $KEY --security-groups $SG --instance-type t2.micro \
  --region us-west-2 --profile $PROFILE

# Spin up the ASG
aws autoscaling create-auto-scaling-group --auto-scaling-group-name $NAME \
  --launch-configuration-name $NAME --min-size 0 --max-size 0 \
  --vpc-zone-identifier $SUBNET --region us-west-2 --profile $PROFILE
