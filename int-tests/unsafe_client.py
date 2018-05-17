from io import BytesIO
import pycurl


class Response(object):
  def __init__(self):
    self.headers = {}
    self.body = ''
    self.status = 0

_CHAR_ENCODING = 'iso-8859-1'

def _prep_request(url: str, h2: bool):
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
  curl.setopt(curl.URL, url)
  curl.setopt(curl.WRITEDATA, buffer)
  if (h2):
    curl.setopt(pycurl.HTTP_VERSION, pycurl.CURL_HTTP_VERSION_2_0)
  else:
    curl.setopt(pycurl.HTTP_VERSION, pycurl.CURL_HTTP_VERSION_1_1)
  curl.setopt(pycurl.SSL_VERIFYPEER, 0)
  curl.setopt(pycurl.SSL_VERIFYHOST, 0)
  curl.setopt(curl.HEADERFUNCTION, _header_function)
  curl.setopt(pycurl.TIMEOUT, 1)
  return curl, response, buffer



def http_get(url: str, h2: bool = True):
  curl, response, buffer = _prep_request(url, h2)
  curl.perform()
  response.status = curl.getinfo(curl.RESPONSE_CODE)
  response.body = buffer.getvalue().decode(_CHAR_ENCODING)
  curl.close()
  return response


def http_post(url: str, h2: bool = True):
  raise Exception('so now you want to post - sorry cannot do that yet')  # todo: (WK)
