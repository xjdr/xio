import unittest
import requests
import os.path as path
from server_controller import Server, Initializer, module_dir


class TestReverseProxyServer(unittest.TestCase):
  def setUp(self):
    self.front_end = None
    self.back_ends = []

  def tearDown(self):
    for each in [each for each in self.back_ends + [self.front_end] if each is not None]:
      each.kill()

  def setup_back(self):
    back_init = Initializer('int-test-backend-server')
    back_ready_str = "starting to accept connections"
    host = "127.0.0.1"
    self.back_ends += [Server(back_init.init_script, back_ready_str, name="backend1", host=host, port=8444).run()]

  def setup_front(self, h2: bool):
    front_init = Initializer('int-test-proxy-server')
    front_ready_str = "XioServerBootstrap - Building"
    conf = path.abspath(path.join(module_dir, "proxy.conf"))
    self.assertTrue(path.exists(conf))
    if h2:
      proxy_config = 'xio.h2ReverseProxy'
    else:
      proxy_config = 'xio.h1ReverseProxy'
    self.front_end = Server(front_init.init_script, front_ready_str, conf, proxy_config, name="proxy", verbose=False).run()

  # region tests

  def test_backend_server(self):
    self.setup_back()
    for each in self.back_ends:
      r = requests.get("https://localhost:{}".format(each.port), verify=False)
      self.assertEqual(200, r.status_code)

  def test_proxy_server(self):
    self.setup_front(True)
    self.setup_back()
    r = requests.get("https://localhost:{}/".format(8443), verify=False)
    self.assertEqual(200, r.status_code)

  # endregion


if __name__ == '__main__':
  unittest.main()
