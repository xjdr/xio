#!/usr/bin/env python

from __future__ import absolute_import

import argparse
import ipaddress
import sys

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from configurator.thriftgen.ConfigurationService import ConfigurationService
from configurator.thriftgen.ConfigurationService.ConfigurationService import RuleType, IpRule
from configurator.thriftgen.Http1Ruleset.ttypes import Http1Method, Http1Version, Http1HeaderTuple, Http1Rule

def create_rule_type(args):
  return RuleType._NAMES_TO_VALUES[args.rule_type]

def create_ip_rule(args, ip_address=None):
  if ip_address is None:
    ip_address = args.ip_address
  return IpRule(ip_address.packed)

def create_http1_rule(args):
  def build_headers(header_args):
    if header_args is None:
      return None
    results = []
    for header in header_args:
      key, value = header.split(': ')
      results.append(Http1HeaderTuple(key, value))
    return results

  versions = {
    '1.0': Http1Version.HTTP_1_0,
    '1.1': Http1Version.HTTP_1_1,
  }
  method = Http1Method._NAMES_TO_VALUES.get(args.method)
  uri = args.path
  version = versions.get(args.version)
  headers = build_headers(args.headers)
  return Http1Rule(method, uri, version, headers)

def add_ip(client, args):
  if args.dry_run:
    print 'dry_run: add_ip', args.ip_address, args.rule_type
    return True

  ip_rule = create_ip_rule(args)
  result = client.addIpRule(ip_rule, create_rule_type(args))
  if not result.success:
    print >> sys.stderr, result.errorReason
    return False
  return True

def remove_ip(client, args):
  if args.dry_run:
    print 'dry_run: remove_ip', args.ip_address
    return True

  ip_rule = create_ip_rule(args)
  result = client.removeIpRule(ip_rule)
  if not result.success:
    print >> sys.stderr, result.errorReason
    return False
  return True

def add_network(client, args):
  hosts = args.network_address.hosts()
  if args.dry_run:
    hosts = list(hosts)
    print 'dry_run: add_network', args.network_address, '(%s..%s)' % (hosts[0], hosts[-1]), args.rule_type
    return True

  success = True
  hosts_added = 0
  rule_type = create_rule_type(args)
  for host in hosts:
    hosts_added += 1
    if args.verbose:
      print 'add_network', args.network_address, 'host', host
    ip_rule = create_ip_rule(args, ip_address=host)
    result = client.addIpRule(ip_rule, rule_type)
    if not result.success:
      print >> sys.stderr, result.errorReason
      success = False
  if args.verbose or hosts_added == 0:
    print "%d hosts added" % hosts_added
  return hosts_added != 0

  ip_rule = create_ip_rule(args)

def remove_network(client, args):
  hosts = args.network_address.hosts()
  if args.dry_run:
    hosts = list(hosts)
    print 'dry_run: remove_network', args.network_address, '(%s..%s)' % (hosts[0], hosts[-1])
    return True

  success = True
  hosts_removed = 0
  for host in hosts:
    hosts_removed += 1
    if args.verbose:
      print 'remove_network', args.network_address, 'host', host
    ip_rule = create_ip_rule(args, ip_address=host)
    result = client.removeIpRule(ip_rule)
    if not result.success:
      print >> sys.stderr, result.errorReason
      success = False
  if args.verbose or hosts_removed == 0:
    print "%d hosts removed" % hosts_removed
  return hosts_removed != 0

def describe_http1_args(args):
  headers = args.headers
  if headers is not None:
    headers = repr(headers)
  if headers is None:
    headers = 'Any'
  path = args.path
  if path is None:
    path = 'Any'
  method = args.method
  if method is None:
    method = 'Any'
  version = args.version
  if version is None:
    version = 'Any'
  return 'headers', headers, 'path', path, 'method', method, 'version', version

def add_http1(client, args):
  def prompt(default):
    result = raw_input("Are you sure you want to %s all http1 traffic? [%s]: " % (args.rule_type, default))
    if result == '':
      return default
    return result.lower()

  if args.dry_run:
    print ' '.join(list(('dry_run: add_http1', ) + describe_http1_args(args)))

  if (args.headers is None and args.path is None and args.method is None and args.version is None):
    response = prompt('n')
    while response not in 'yn':
      response = prompt('n')
    if response == 'n':
      return True

  if args.dry_run:
    return True

  http1_rule = create_http1_rule(args)
  result = client.addHttp1Rule(http1_rule, create_rule_type(args))
  if not result.success:
    print >> sys.stderr, result.errorReason
    return False
  return True

def remove_http1(client, args):
  if args.dry_run:
    print ' '.join(list(('dry_run: remove_http1', ) + describe_http1_args(args)))
    return True

  http1_rule = create_http1_rule(args)
  result = client.removeHttp1Rule(http1_rule)
  if not result.success:
    print >> sys.stderr, result.errorReason
    return False
  return True

def ip_subcommand(parent):
  ip_parser = parent.add_parser('ip')
  ip_commands = ip_parser.add_subparsers()
  add_command = ip_commands.add_parser('add')
  add_command.set_defaults(command=add_ip)
  add_command.add_argument('rule_type', choices=sorted(RuleType._NAMES_TO_VALUES.keys()))
  add_command.add_argument('ip_address', type=ipaddress.ip_address)

  remove_command = ip_commands.add_parser('remove')
  remove_command.set_defaults(command=remove_ip)
  remove_command.add_argument('ip_address', type=ipaddress.ip_address)

def network_subcommand(parent):
  def strict_network_address(address):
    return ipaddress.ip_network(address, True)
  network_parser = parent.add_parser('network')
  network_commands = network_parser.add_subparsers()
  add_command = network_commands.add_parser('add')
  add_command.set_defaults(command=add_network)
  add_command.add_argument('rule_type', choices=sorted(RuleType._NAMES_TO_VALUES.keys()))
  add_command.add_argument('network_address', type=strict_network_address)

  remove_command = network_commands.add_parser('remove')
  remove_command.set_defaults(command=remove_network)
  remove_command.add_argument('network_address', type=strict_network_address)

def http1_subcommand(parent):
  def common(parser):
    parser.add_argument('-M', '--method', choices=sorted(Http1Method._NAMES_TO_VALUES.keys()), default=None)
    parser.add_argument('-V', '--version', choices=sorted(['1.0', '1.1']), default=None)
    parser.add_argument('-P', '--path', type=str, default=None)
    parser.add_argument('-H', '--headers', nargs='*', type=str, default=None, metavar='"Header-Key: Header-Value"')

  http1_parser = parent.add_parser('http1')
  http1_commands = http1_parser.add_subparsers()
  add_command = http1_commands.add_parser('add')
  add_command.set_defaults(command=add_http1)
  add_command.add_argument('rule_type', choices=sorted(RuleType._NAMES_TO_VALUES.keys()))
  common(add_command)

  remove_command = http1_commands.add_parser('remove')
  remove_command.set_defaults(command=remove_http1)
  common(remove_command)

def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument('-H', '--config-host', dest='config_host', required=True)
  parser.add_argument('-P', '--config-port', dest='config_port', default=9999)
  parser.add_argument('-n', '--dry-run', action='store_const', const=True, default=False)
  parser.add_argument('-v', '--verbose', action='store_const', const=True, default=False)
  subparsers = parser.add_subparsers()
  ip_subcommand(subparsers)
  network_subcommand(subparsers)
  http1_subcommand(subparsers)

  return parser.parse_args()

def connect(args):
  try:
    tsocket = TSocket.TSocket(args.config_host, args.config_port)
    transport = TTransport.TBufferedTransport(tsocket)
    protocol = TBinaryProtocol.TBinaryProtocol(transport)
    client = ConfigurationService.Client(protocol)
    transport.open()
    return client

  except Thrift.TException, tx:
    print >> sys.stderr, '%s' % (tx.message)
    return None

def main():
  args = parse_args()
  client = connect(args)
  if client is None:
    return False
  result = args.command(client, args)
  return result

if __name__ == '__main__':
  result = main()
  if not result:
    sys.exit(1)
