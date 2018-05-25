import subprocess
import unittest
from unittest import TestCase

url = 'https://front'


def _parse_h2_load_output(output: bytes):
  results = {
    'total': '0',
    'started': '0',
    'done': '0',
    'succeeded': '0',
    'failed': '0',
    'errored': '0',
    'timeout': '0',
  }
  for each_line in output.split(b'\n'):
    line = each_line.decode('utf-8')
    if line.startswith('requests:'):
      result_list = line.lstrip('requests:').strip().split(',')
      for each in result_list:
        print('---', each.strip(), '---')
        count, title = each.strip().split(' ')
        results[title] = count
  return results


class H2LoadTest(TestCase):

  # @skip
  def test_h2load(self):
    count = '100000'
    print('running h2load test request count: {}...'.format(count))
    msg = subprocess.check_output(['h2load', '-t12', '-c400', '-n{}'.format(count), url])
    results = _parse_h2_load_output(msg)
    self.assertTrue(results['total'], count)
    self.assertTrue(results['succeeded'], count)


if __name__ == '__main__':
  unittest.main()
