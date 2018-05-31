FROM debian:sid
ADD ./*.py /tests/
ADD ./requirements.txt /tests/requirements.txt
RUN apt-get update
RUN apt-get -y upgrade
RUN apt-get install -y python3.7 python3-pip python3-pycurl nghttp2-client wrk
RUN pip3 install -r /tests/requirements.txt
