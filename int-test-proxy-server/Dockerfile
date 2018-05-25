FROM openjdk:8u171-jdk-stretch
ADD ./build/install/int-test-proxy-server /
ADD ./docker-app.conf /etc/int-test-proxy/proxy.conf
ENV JAVA_OPTS="-DDEBUG"
CMD ["int-test-proxy-server", "proxy", "/etc/int-test-proxy/proxy.conf", "xio.h2ReverseProxy"]
