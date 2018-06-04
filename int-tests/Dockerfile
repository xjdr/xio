FROM manimaul/xio:testing-python3-wrk-h2load
ADD ./proxy_load_tests.py /tests/
ADD ./requirements.txt /tests/requirements.txt
RUN apt-get install -y curl python3-pycurl \
&& pip3 install -r /tests/requirements.txt
