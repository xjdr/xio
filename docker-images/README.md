# Base Docker Images

https://hub.docker.com/r/manimaul/xio/

## base-stretch

The root base Docker image. 

Tag:
`manimaul/xio:base-stretch`

Build and push:
```bash
docker build -t manimaul/xio:base-stretch ./base-stretch
docker push manimaul/xio:base-stretch
```

## building-jdk8

The base Docker image for building xio with openjdk8.

Tag:
`manimaul/xio:building-jdk8`


## running-jre8

The base Docker image for running xio with openjdk8. 

Tag:
`manimaul/xio:running-jdk8`



## testing-python3-wrk-h2load

The base Docker image for running xio integration and load tests with pyunit, wrk and h2load. 

Tag:
`manimaul/xio:testing-python3-wrk-h2load`
