FROM manimaul/xio:testing-python3-wrk-h2load
ADD ./*.py /tests/
ADD ./requirements.txt /tests/requirements.txt
RUN apt-get install -y python3-pycurl
RUN pip3 install -r /tests/requirements.txt
