FROM manimaul/xio:running-jdk8
ADD ./build/install/int-test-proxy-server /
ADD ./docker-app.conf /etc/int-test-proxy/proxy.conf
ENV JAVA_OPTS="-DDEBUG"
RUN apt-get install -y curl
CMD ["int-test-proxy-server", "proxy", "/etc/int-test-proxy/proxy.conf", "xio.h2ReverseProxy"]
