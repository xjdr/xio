from io import BytesIO
import pycurl
from urllib.parse import urlencode
import json
import requests
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class Response(object):
  def __init__(self):
    self.headers = {}
    self.body = ''
    self.status = 0

  @property
  def json_body(self):
    return json.loads(self.body)


_CHAR_ENCODING = 'iso-8859-1'
_TIMEOUT_SECS = 5

def _prep_request(url: str, headers: dict, verbose: bool):
  response = Response()

  def _header_function(header_line):

    header_line = header_line.decode(_CHAR_ENCODING)
    if ':' not in header_line:
      return
    name, value = header_line.split(':', 1)
    name = name.strip()
    value = value.strip()
    name = name.lower()
    response.headers[name] = value

  buffer = BytesIO()
  curl = pycurl.Curl()
  curl.reset()
  curl.setopt(pycurl.URL, url)
  if headers != None:
    headers = ["{}: {}".format(str(each[0]), str(each[1])) for each in headers.items()]
    curl.setopt(pycurl.HTTPHEADER, headers)
    pass
  curl.setopt(pycurl.WRITEDATA, buffer)
  curl.setopt(pycurl.HTTP_VERSION, pycurl.CURL_HTTP_VERSION_2_0)
  if verbose:
    curl.setopt(curl.VERBOSE, True)
  curl.setopt(pycurl.SSL_VERIFYPEER, 0)
  curl.setopt(pycurl.SSL_VERIFYHOST, 0)
  curl.setopt(pycurl.HEADERFUNCTION, _header_function)
  curl.setopt(pycurl.TIMEOUT, _TIMEOUT_SECS)
  return curl, response, buffer


def _complete_request(curl: pycurl.Curl, buffer: BytesIO, response: Response):
  curl.perform()
  response.status = curl.getinfo(curl.RESPONSE_CODE)
  response.body = buffer.getvalue().decode(_CHAR_ENCODING)
  curl.close()


def http_get(url: str, headers: dict = None, h2: bool = False, verbose: bool = False):
  if h2:
    curl, response, buffer = _prep_request(url, headers, verbose)
    _complete_request(curl, buffer, response)
    return response
  else:
    r = requests.get(url, headers=headers, verify=False, timeout=_TIMEOUT_SECS)
    response = Response()
    response.headers = r.headers
    response.body = r.text
    response.status = r.status_code
    return response



def http_post(url: str, data: dict, headers: dict= None, h2: bool = False, verbose: bool = False):
  if h2:
    curl, response, buffer = _prep_request(url, headers, verbose)
    postfields = urlencode(data)
    curl.setopt(pycurl.POSTFIELDS, postfields)
    _complete_request(curl, buffer, response)
    return response
  else:
    r = requests.post(url, headers=headers, data=data, verify=False, timeout=_TIMEOUT_SECS)
    response = Response()
    response.headers = r.headers
    response.body = r.text
    response.status = r.status_code
    return response
