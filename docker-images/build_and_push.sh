#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

array=( running-jdk8 running-jdk8 testing-python3-wrk-h2load )
for i in "${array[@]}"
do
  docker build -t manimaul/xio:$i ./$i
  docker push manimaul/xio:$i
done
