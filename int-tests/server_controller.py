import subprocess
import sys
import functools
import os
import os.path as path
from threading import Thread
from queue import Queue, Empty

module_dir = path.abspath(path.join(path.dirname(__file__)))
_root_dir = path.abspath(path.join(module_dir, '..'))


class StdOutReader:
  def __init__(self, stream, verbose=False):
    self._stream = stream
    self._queue = Queue()

    def _reader(s, queue, verbose):
      while True:
        line = s.readline()
        s.flush()
        if line:
          if verbose:
            print(line)
          queue.put(line)

    self._thread = Thread(target=_reader, args=(self._stream, self._queue, verbose))
    self._thread.daemon = True
    self._thread.start()

  def readline(self):
    try:
      return str(self._queue.get(block=False, timeout=0.1))
    except Empty:
      return ''


class Initializer:
  def __init__(self, project):
    cmd = self._cmd_for_task(project, 'assembleDist', 'installDist')
    print("gradle cmd: {}".format(cmd))
    project_module_dir = path.abspath(path.join(_root_dir, project))
    self._init_script = path.join(project_module_dir,
                                  'build/install/{}/bin/{}'.format(project, project))
    if subprocess.call(cmd, shell=True) == 0 and path.exists(self._init_script):
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
  def __init__(self, script, ready_str, *args, **kwargs):
    self._port = kwargs.get('port', '')
    self._name = kwargs.get('name', 'unnamed')
    self._verbose = kwargs.get('verbose', False)
    if len(args) > 1:
      argv = functools.reduce(lambda a, b: ("", str(a).strip() + " " + str(b).strip()), args)[1]
    elif len(args) is 1:
      argv = str(args[0]).strip()
    else:
      argv = ''
    self.cmd = ' '.join("{} {} {} {}".format(script, self.port, self.name, argv).split())
    self.process = None
    self.ready_str = ready_str

  @property
  def port(self):
    return str(self._port)

  @property
  def name(self):
    return str(self._name)

  def run(self):
    if self.process is None:
      print("server {} run cmd: {}".format(self.name, self.cmd))
      env = {**os.environ, 'JAVA_OPTS': '-DDEBUG=1'}
      self.process = subprocess.Popen("exec " + self.cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, env=env)
      nb_err = StdOutReader(self.process.stderr, verbose=self._verbose)
      nb_out = StdOutReader(self.process.stdout, verbose=self._verbose)
      while True:
        if self.ready_str in nb_err.readline() or self.ready_str in nb_out.readline():
          break
    return self

  def kill(self):
    print("server {} killed".format(self.name))
    if self.process is not None:
      self.process.kill()
