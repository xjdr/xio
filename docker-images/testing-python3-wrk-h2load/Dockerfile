FROM manimaul/xio:base-stretch as builder

# nghttp2 sources
RUN apt-get install -y wget bzip2 checkinstall
WORKDIR /build
RUN wget https://github.com/nghttp2/nghttp2/releases/download/v1.32.0/nghttp2-1.32.0.tar.bz2
RUN tar -xf nghttp2-1.32.0.tar.bz2
WORKDIR /build/nghttp2-1.32.0

# nghttp2 build dependencies
RUN apt-get -y install build-essential libjemalloc-dev libev-dev libevent-dev libjansson-dev libspdylay-dev libssl-dev libxml2-dev zlib1g-dev libc-ares-dev libcunit1-dev libicu-dev libcunit1 libevent-core-2.0-5 libevent-extra-2.0-5 libevent-openssl-2.0-5 libevent-pthreads-2.0-5 icu-devtools libjs-sphinxdoc libjs-underscore docutils-common  pkg-config python-alabaster python-babel python-babel-localedata python-docutils python-imagesize python-jinja2 python-markupsafe python-pygments python-roman python-sphinx python-tz sphinx-common

# nghttp2 build
RUN ./configure --enable-app && make && make install
RUN apt-get -y install openssl libev4 zlib1g libc-ares2
RUN checkinstall -y make install

# --- stage 2 ---

FROM manimaul/xio:base-stretch
MAINTAINER Will Kamp <manimaul@gmail.com>
COPY --from=builder /build/nghttp2-1.32.0/nghttp2_1.32.0-1_amd64.deb .
RUN apt-get -y install openssl libev4 zlib1g libc-ares2 libjemalloc1 libjansson4
RUN apt install /nghttp2_1.32.0-1_amd64.deb
RUN ldconfig
RUN apt-get -y install wrk python3 python3-pip




