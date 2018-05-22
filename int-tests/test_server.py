import unittest
from unsafe_client import http_get, http_post
import os.path as path
from server_controller import Server, Initializer, module_dir
from unittest import TestCase, skip
import json


class TestReverseProxyServer(TestCase):

  # region setup

  @classmethod
  def setUpClass(cls):
    global back_init
    back_init = Initializer('int-test-backend-server')
    global front_init
    front_init = Initializer('int-test-proxy-server')

  def setUp(self):
    self.front_end = None
    self.back_ends = []

  def tearDown(self):
    for each in [each for each in self.back_ends + [self.front_end] if each is not None]:
      each.kill()

  def setup_back(self, h2: bool):
    back_ready_str = "starting to accept connections"
    self.back_ends += [Server(back_init.init_script, back_ready_str, h2,
                              name="backend1", port=8444, verbose=False).run()
                       ]

  def setup_front(self, h2: bool):
    front_ready_str = "proxy accepting connections"
    conf = path.abspath(path.join(module_dir, "proxy.conf"))
    self.assertTrue(path.exists(conf))
    if h2:
      proxy_config = 'xio.h2ReverseProxy'
    else:
      proxy_config = 'xio.h1ReverseProxy'
    self.front_end = Server(front_init.init_script, front_ready_str, conf, proxy_config,
                            name="proxy", verbose=False).run()

  # endregion

  # region tests

  # @skip
  def test_backend_server_get_works(self):
    self.setup_back(h2=False)
    for each in self.back_ends:
      response = http_get(url='https://localhost:{}/'.format(each.port), headers={"x-echo": "echo"})
      self.assertEqual('backend1', response.headers['x-tag'])
      self.assertEqual('GET', response.headers['x-method'])
      self.assertEqual('echo', response.headers['x-echo'])
      self.assertEqual({'title': 'Release', 'description': 'the Kraken'}, response.json_body)
      self.assertEqual(200, response.status)

  # @skip
  def test_backend_server_post_works(self):
    self.setup_back(h2=False)
    for each in self.back_ends:
      response = http_post(url='https://localhost:{}/'.format(each.port), data={"key": "value"}, headers={"x-echo": "echo"})
      self.assertEqual('backend1', response.headers['x-tag'])
      self.assertEqual('POST', response.headers['x-method'])
      self.assertEqual('echo', response.headers['x-echo'])
      self.assertEqual({'title': 'Release', 'description': 'the Kraken'}, response.json_body)
      self.assertEqual(200, response.status)

  # @skip
  def test_proxy_get_h2_h1(self):
    self.setup_front(h2=True)
    self.setup_back(h2=False)
    responses = (http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}),
                 http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}),
                 )
    for response in responses:
      self.assertEqual('backend1', response.headers['x-tag'])
      self.assertEqual('GET', response.headers['x-method'])
      self.assertEqual('echo', response.headers['x-echo'])
      self.assertEqual({'title': 'Release', 'description': 'the Kraken'}, response.json_body)
      self.assertEqual(200, response.status)

  # @skip
  def test_proxy_post_h2_h1(self):
    self.setup_front(h2=True)
    self.setup_back(h2=False)
    responses = (http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}),
                 http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}),
                 )
    for response in responses:
      self.assertEqual('backend1', response.headers['x-tag'])
      self.assertEqual('POST', response.headers['x-method'])
      self.assertEqual('echo', response.headers['x-echo'])
      self.assertEqual({'title': 'Release', 'description': 'the Kraken'}, response.json_body)
      self.assertEqual(200, response.status)

  # @skip
  def test_proxy_get_h1_h1(self):
    self.setup_front(h2=False)
    self.setup_back(h2=False)
    responses = (http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}),
                 http_get(url='https://localhost:8443/', headers={"x-echo": "echo"}),
                 )
    for response in responses:
      self.assertEqual('backend1', response.headers['x-tag'])
      self.assertEqual('GET', response.headers['x-method'])
      self.assertEqual('echo', response.headers['x-echo'])
      self.assertEqual({'title': 'Release', 'description': 'the Kraken'}, response.json_body)
      self.assertEqual(200, response.status)

  # @skip
  def test_proxy_post_h1_h1(self):
    self.setup_front(h2=False)
    self.setup_back(h2=False)
    responses = (http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}),
                 http_post(url='https://localhost:8443/', data={"key": "value"}, headers={"x-echo": "echo"}),
                 )
    for response in responses:
      self.assertEqual('backend1', response.headers['x-tag'])
      self.assertEqual('POST', response.headers['x-method'])
      self.assertEqual('echo', response.headers['x-echo'])
      self.assertEqual({'title': 'Release', 'description': 'the Kraken'}, response.json_body)
      self.assertEqual(200, response.status)

  # todo: (WK) :
  # test large POST
  # test h2_h2

  # endregion


if __name__ == '__main__':
  unittest.main()
