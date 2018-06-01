import subprocess
import unittest
import os
import re
from unittest import TestCase

url = 'https://front'
cores = os.cpu_count()


def _bytes_2_lines(output: bytes):
  lines = []
  for each_line in output.split(b'\n'):
    line = each_line.decode('utf-8').strip()
    print(line)
    lines.append(line)
  return lines


def _extract_int(value: str):
  return int(re.sub('[^0-9]', '', value))


def _parse_h2_load_output(output: bytes):
  results = {
    'total': 0,
    'started': 0,
    'done': 0,
    'succeeded': 0,
    'failed': 0,
    'errored': 0,
    'timeout': 0,
  }
  for line in _bytes_2_lines(output):
    if line.startswith('requests:'):
      result_list = line.lstrip('requests:').strip().split(',')
      for each in result_list:
        print('---', each.strip(), '---')
        count, title = each.strip().split(' ')
        results[title] = _extract_int(count)
  return results


def _parse_wrk_output(output: bytes):
  results = {
    'requests per sec': 0,
    'requests': 0,
    'errors': {
      'connect': 0,
      'read': 0,
      'write': 0,
      'timeout': 0
    }
  }
  for line in _bytes_2_lines(output):
    if line.startswith('Requests/sec:'):
      results['requests per sec'] = line.split(':')[1].strip()
    elif line.startswith('Socket errors:'):
      for error in line.lstrip('Socket errors:').strip().split(','):
        for key in results['errors'].keys():
          if key in error:
            results['errors'][key] = _extract_int(error)

    elif 'requests in' in line:
      results['requests'] = line.split('requests in')[0].strip()

  return results


class H2LoadTest(TestCase):

  # @skip
  def test_h2load(self):
    count = 100000
    print('running h2load test request count: {}...'.format(count))
    msg = subprocess.check_output(['h2load', '-t{}'.format(cores), '-c400', '-n{}'.format(count), url])
    results = _parse_h2_load_output(msg)
    self.assertEqual(results['total'], count)
    self.assertEqual(results['succeeded'], count)


class H1LoadTest(TestCase):

  # @skip
  def test_h1load(self):
    print('running h1load test for 30 seconds')
    msg = subprocess.check_output(['wrk', '-t{}'.format(cores), '-c400', '-d30s', url])
    result = _parse_wrk_output(msg)
    errors = result['errors']
    self.assertEqual(errors['connect'], 0)
    self.assertEqual(errors['read'], 0)
    self.assertEqual(errors['write'], 0)
    self.assertEqual(errors['timeout'], 0)


if __name__ == '__main__':
  unittest.main()
