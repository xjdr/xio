import unittest
import requests
from server_controller import Server, Initializer


class TestReverseProxyServer(unittest.TestCase):
  def setUp(self):
    back_init = Initializer('int-test-backend-server')
    terminal_condition = "starting to accept connections"
    host = "127.0.0.1"
    self.servers = [Server(back_init.init_script, terminal_condition, "backend1", host=host, port=8443),
                    Server(back_init.init_script, terminal_condition, "backend2", host=host, port=8444),
                    ]
    for each in self.servers:
      each.run()

  def tearDown(self):
    for each in self.servers:
      each.kill()

  def test_backend_server(self):
    for each in self.servers:
      r = requests.get("https://localhost:{}".format(each.port), verify=False)
      self.assertEqual(200, r.status_code)


if __name__ == '__main__':
  unittest.main()
