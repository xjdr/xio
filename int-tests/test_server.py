import os.path as path
import unittest
from unittest import TestCase

from server_controller import Server, Initializer, module_dir
from unsafe_client import http_get, http_post, Response

back_init = Initializer('int-test-backend-server')
front_init = Initializer('int-test-proxy-server')
back_end = None
front_end = None


class TestReverseProxyServer(TestCase):

  @classmethod
  def tearDownClass(cls):
    for each in [each for each in [back_end, front_end] if each is not None]:
      each.kill()

  @classmethod
  def setup_back(cls, h2: bool, h1_tls: bool = True, verbose=False):
    back_ready_str = "Active Connections"
    global back_end
    back_end = Server(back_init.init_script, back_ready_str, h2, h1_tls, name="backend1", port=8444, verbose=verbose).run()

  @classmethod
  def setup_front(cls, h2: bool, back_tsl: bool = True, verbose=False):
    front_ready_str = "proxy accepting connections"
    conf = path.abspath(path.join(module_dir, "proxy.conf"))
    if h2:
      proxy_config = 'xio.h2ReverseProxy'
    else:
      proxy_config = 'xio.h1ReverseProxy'
    if back_tsl:
      client_config = 'xio.testProxyRoute'
    else:
      client_config = 'xio.testProxyRoutePlainText'
    global front_end
    front_end = Server(front_init.init_script, front_ready_str, conf, proxy_config, client_config,
                       name="proxy", verbose=verbose).run()

  def check_response(self, response: Response, method: str):
    self.assertEqual('backend1', response.headers['x-tag'])
    self.assertEqual(method, response.headers['x-method'])
    self.assertEqual('echo', response.headers['x-echo'])
    keys = { k.lower(): k for k in response.headers.keys() }
    self.assertFalse('transfer-encoding' in keys and 'content-length' in keys)
    self.assertEqual({'title': 'Release', 'description': 'the Kraken'}, response.json_body)
    self.assertEqual(200, response.status)


class TestReverseProxyServerH1H1PlainText(TestReverseProxyServer):

  @classmethod
  def setUpClass(cls):
    print("setup h1:h1")
    cls.setup_front(h2=False, back_tsl=False)
    cls.setup_back(h2=False, h1_tls=False)

  # @skip
  def test_backend_server_get_h1_works(self):
    response = http_get(url='http://localhost:{}/'.format(back_end.port), headers={"x-echo": "echo"}, h2=False)
    self.check_response(response, 'GET')

  # @skip
  def test_backend_server_post_h1_works(self):
    response = http_post(url='http://localhost:{}/'.format(back_end.port), data={"key": "value"},
                         headers={"x-echo": "echo"}, h2=False)
    self.check_response(response, 'POST')

  # @skip
  def test_proxy_get_h1_h1(self):
    responses = [
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=False),
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=False),
    ]
    for response in responses:
      self.check_response(response, 'GET')

  # @skip
  def test_proxy_post_h1_h1(self):
    responses = [
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=False),
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=False),
    ]
    for response in responses:
      self.check_response(response, 'POST')


class TestReverseProxyServerH1H1(TestReverseProxyServer):

  @classmethod
  def setUpClass(cls):
    print("setup h1:h1")
    cls.setup_front(h2=False)
    cls.setup_back(h2=False)

  # @skip
  def test_backend_server_get_h1_works(self):
    response = http_get(url='https://localhost:{}/'.format(back_end.port), headers={"x-echo": "echo"}, h2=False)
    self.check_response(response, 'GET')

  # @skip
  def test_backend_server_post_h1_works(self):
    response = http_post(url='https://localhost:{}/'.format(back_end.port), data={"key": "value"},
                         headers={"x-echo": "echo"}, h2=False)
    self.check_response(response, 'POST')

  # @skip
  def test_proxy_get_h1_h1(self):
    responses = [
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=False),
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=False),
    ]
    for response in responses:
      self.check_response(response, 'GET')

  # @skip
  def test_proxy_post_h1_h1(self):
    responses = [
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=False),
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=False),
    ]
    for response in responses:
      self.check_response(response, 'POST')


class TestReverseProxyServerH2H1(TestReverseProxyServer):

  @classmethod
  def setUpClass(cls):
    print("setup h2:h1")
    cls.setup_front(h2=True)
    cls.setup_back(h2=False)

  # @skip
  def test_proxy_get_h2_h1(self):
    responses = [
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=True),
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=True),
    ]
    for response in responses:
      self.check_response(response, 'GET')

  # @skip
  def test_proxy_post_h2_h1(self):
    responses = [
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=True),
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=True),
    ]
    for response in responses:
      self.check_response(response, 'POST')


class TestReverseProxyServerH2H1PlainText(TestReverseProxyServer):

  @classmethod
  def setUpClass(cls):
    print("setup h2:h1")
    cls.setup_front(h2=True, back_tsl=False)
    cls.setup_back(h2=False, h1_tls=False)

  # @skip
  def test_proxy_get_h2_h1(self):
    responses = [
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=True),
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=True),
    ]
    for response in responses:
      self.check_response(response, 'GET')

  # @skip
  def test_proxy_post_h2_h1(self):
    responses = [
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=True),
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=True),
    ]
    for response in responses:
      self.check_response(response, 'POST')


class TestReverseProxyServerH2H2(TestReverseProxyServer):

  @classmethod
  def setUpClass(cls):
    print("setup h2:h2")
    cls.setup_front(h2=True)
    cls.setup_back(h2=True)

  # @skip
  def test_backend_server_get_h2_works(self):
    responses = [
      http_get(url='https://localhost:{}/'.format(back_end.port), headers={"x-echo": "echo"}, h2=True),
      http_get(url='https://localhost:{}/'.format(back_end.port), headers={"x-echo": "echo"}, h2=True),
    ]
    for response in responses:
      self.check_response(response, 'GET')

  # @skip
  def test_backend_server_post_h2_works(self):
    responses = [
      http_post(url='https://localhost:{}/'.format(back_end.port), data={"key": "value"}, headers={"x-echo": "echo"},
                h2=True),
      http_post(url='https://localhost:{}/'.format(back_end.port), data={"key": "value"}, headers={"x-echo": "echo"},
                h2=True),
    ]

    for response in responses:
      self.check_response(response, 'POST')

  # @skip
  def test_proxy_get_h2_h2(self):
    responses = [
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=True),
      http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}, h2=True),
    ]
    for response in responses:
      self.check_response(response, 'GET')

  # @skip
  def test_proxy_post_h2_h2(self):
    responses = [
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=True),
      http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}, h2=True),
    ]
    for response in responses:
      self.check_response(response, 'POST')


if __name__ == '__main__':
  unittest.main()
