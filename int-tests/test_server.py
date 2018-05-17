import unittest
from unsafe_client import http_get
import os.path as path
from server_controller import Server, Initializer, module_dir
from unittest import TestCase, skip


class TestReverseProxyServer(TestCase):
  def setUp(self):
    self.front_end = None
    self.back_ends = []

  def tearDown(self):
    for each in [each for each in self.back_ends + [self.front_end] if each is not None]:
      each.kill()

  def setup_back(self, h2: bool):
    if h2:
      raise Exception('int-test-backend-server is not configurable for h2 yet')  # todo: (WK)

    back_init = Initializer('int-test-backend-server')
    back_ready_str = "starting to accept connections"
    host = "127.0.0.1"
    self.back_ends += [Server(back_init.init_script, back_ready_str, name="backend1",
                              host=host, port=8444, verbose=False).run()
                       ]

  def setup_front(self, h2: bool):
    front_init = Initializer('int-test-proxy-server')
    front_ready_str = "XioServerBootstrap - Building"
    conf = path.abspath(path.join(module_dir, "proxy.conf"))
    self.assertTrue(path.exists(conf))
    if h2:
      proxy_config = 'xio.h2ReverseProxy'
    else:
      proxy_config = 'xio.h1ReverseProxy'
    self.front_end = Server(front_init.init_script, front_ready_str, conf, proxy_config,
                            name="proxy", verbose=False).run()

  # region tests

  # @skip
  def test_backend_server_works(self):
    self.setup_back(h2=False)
    for each in self.back_ends:
      response = http_get(url='https://localhost:{}/'.format(each.port))
      self.assertEqual('backend1', response.headers['header-tag'])
      self.assertEqual('Release the Kraken', response.body)
      self.assertEqual(200, response.status)

  # @skip
  def test_proxy_get_h2_h1(self):
    self.setup_front(h2=True)
    self.setup_back(h2=False)
    responses = (http_get(url='https://localhost:8443/', h2=True),
                 # http_get(url='https://localhost:8443/', h2=True),  # todo: (WK) fails Http2To1ProxyRequestQueue
                 )
    for response in responses:
      self.assertEqual('backend1', response.headers['header-tag'])
      self.assertEqual('Release the Kraken', response.body)
      self.assertEqual(200, response.status)

  # endregion


if __name__ == '__main__':
  unittest.main()
