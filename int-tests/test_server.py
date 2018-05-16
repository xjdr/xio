import unittest
import requests
from server_controller import Server, Initializer


class TestReverseProxyServer(unittest.TestCase):
  def setUp(self):
    back_init = Initializer('int-test-backend-server')
    front_init = Initializer('int-test-proxy-server')
    back_ready_str = "starting to accept connections"
    front_ready_str = "XioServerBootstrap - Building"
    host = "127.0.0.1"
    self.servers = [Server(back_init.init_script, back_ready_str, name="backend1", host=host, port=8443),
                    Server(back_init.init_script, back_ready_str, name="backend2", host=host, port=8444),
                    Server(front_init.init_script, front_ready_str, "/todo/path/to/conf", port=8445)
                    ]
    for each in self.servers:
      each.run()

  def tearDown(self):
    for each in self.servers:
      each.kill()

  def test_backend_server(self):
    for each in [s for s in self.servers if 'backend' in s.name]:
      r = requests.get("https://localhost:{}".format(each.port), verify=False)
      self.assertEqual(200, r.status_code)


if __name__ == '__main__':
  unittest.main()
