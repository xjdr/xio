FROM manimaul/xio:running-jdk8
ADD ./build/install/int-test-backend-server /
ENV JAVA_OPTS="-DDEBUG"
CMD ["int-test-backend-server", "443", "backend", "true"]
