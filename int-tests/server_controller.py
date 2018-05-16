import subprocess
import os.path
import sys
import functools

_module_dir = os.path.abspath(os.path.join(os.path.dirname(__file__)))
_root_dir = os.path.abspath(os.path.join(_module_dir, '..'))



class Initializer:
  def __init__(self, project):
    cmd = self._cmd_for_task(project, 'assembleDist', 'installDist')
    print("cmd: {}".format(cmd))
    project_module_dir = os.path.abspath(os.path.join(_root_dir, project))
    self._init_script = os.path.join(project_module_dir,
                            'build/install/{}/bin/{}'.format(project, project))
    if subprocess.call(cmd, shell=True) == 0 and os.path.exists(self._init_script):
      print('assembleDist installDist success')
    else:
      print('assembleDist installDist failed')
      sys.exit(1)

  def _cmd_for_task(self, project, *tasks):
    template = ':{}:{}'
    t = functools.reduce(lambda a, b: ("", template.format(project, a) + " " + template.format(project, b)), tasks)[1]
    return "{}/gradlew -p {} {}".format(_root_dir, _root_dir, t)

  @property
  def init_script(self):
    return self._init_script


class Server:
  def __init__(self, script, terminal_condition, *args, **kwargs):
    self._port = kwargs.get('port', '8443')
    host = kwargs.get('host', '')
    argv = args
    if len(args) > 1:
      argv = functools.reduce(lambda a, b: ("", str(a).strip() + " " + str(b).strip()), args)[1]
    else:
      argv = str(argv[0]).strip()
    self.cmd = "{} {} {} {}".format(script, host, self._port, argv)
    self.process = None
    self.terminal_condition = terminal_condition

  @property
  def port(self):
    return str(self._port)

  def run(self):
    if self.process is None:
      self.process = subprocess.Popen("exec " + self.cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
      while True:
          line = str(self.process.stderr.readline())
          self.process.stderr.flush()
          print(line)
          if self.terminal_condition in line:
            break
    return self

  def kill(self):
    if self.process is not None:
      self.process.kill()

if __name__ == '__main__':
  initializer = Initializer('int-test-backend-server')
  servers = [Server(initializer.init_script, "starting to accept connections", "localhost", 8443, "backend1").run()]
  for each in servers:
    each.kill()
    sys.exit(0)
  else:
    print('WOOPS - something is awry')
    sys.exit(1)
