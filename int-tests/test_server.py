import unittest
import requests
from server_controller import Server, assemble_dist_script

script = assemble_dist_script()


class TestReverseProxyServer(unittest.TestCase):
  def setUp(self):
    self.servers = [Server("backend", script, 8443),
                    Server("backend", script, 8444)]
    for each in self.servers:
      # todo: WBK - this needs to block until the server is up
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
