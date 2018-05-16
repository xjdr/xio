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
    self.back_ends += [Server(back_init.init_script, back_ready_str, name="backend1", host=host, port=8443).run(),
                       Server(back_init.init_script, back_ready_str, name="backend2", host=host, port=8444).run()]

  def setup_front(self):
    front_init = Initializer('int-test-proxy-server')
    front_ready_str = "XioServerBootstrap - Building"
    conf = path.abspath(path.join(module_dir, "proxy.conf"))
    self.front_end = Server(front_init.init_script, front_ready_str, conf, port=8445, verbose=True).run()

  # region tests

  @unittest.skip("temp")
  def test_backend_server(self):
    self.setup_back()
    for each in self.back_ends:
      r = requests.get("https://localhost:{}".format(each.port), verify=False)
      self.assertEqual(200, r.status_code)

  def test_proxy_server(self):
    self.setup_front()

  # endregion


if __name__ == '__main__':
  unittest.main()
