import subprocess
import os.path
import sys
import functools

_module_dir = os.path.abspath(os.path.join(os.path.dirname(__file__)))
_root_dir = os.path.abspath(os.path.join(_module_dir, '..'))
_int_test_proxy_server_module_dir = os.path.abspath(os.path.join(_root_dir, 'int-test-backend-server'))


def cmd_for_task(*tasks):
  template = ':int-test-backend-server:{}'
  t = functools.reduce(lambda a, b: ("", template.format(a) + " " + template.format(b)), tasks)[1]
  return "{}/gradlew {}".format(_root_dir, t)


def assemble_dist_script():
  cmd = cmd_for_task('assembleDist', 'installDist')
  dist_dir = os.path.join(_int_test_proxy_server_module_dir,
                          'build/install/int-test-backend-server/bin/int-test-backend-server')
  if subprocess.call(cmd, shell=True) == 0 and os.path.exists(dist_dir):
    print('assembleDist installDist success')
    return dist_dir
  else:
    print('assembleDist installDist failed')
    return None


class Server:
  def __init__(self, name, script, port):
    self.name = name
    self.cmd = "{} {} {}".format(script, name, port)
    self.process = None
    self._port = port

  @property
  def port(self):
    return str(self._port)

  def run(self):
    if self.process is None:
      print("running server {}".format(self.name))
      self.process = subprocess.Popen("exec " + self.cmd, stdout=subprocess.PIPE, shell=True)
    return self

  def kill(self):
    if self.process is not None:
      self.process.kill()

  def wait(self):
    status = None
    while self.process is not None and status is None:
      s = self.process.poll()
      if s is not None:
        print("server {} process completed with status {}".format(self.name, s))
        self.process = None


if __name__ == '__main__':
  script = assemble_dist_script()
  if script is not None:
    servers = [Server("s1", script, 8443).run(),
               Server("s2", script, 8444).run()]
    for each in servers:
      each.kill()
      each.wait()
    sys.exit(0)
  else:
    print('WOOPS - something is awry')
    sys.exit(1)
