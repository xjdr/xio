#!/usr/bin/env python

import argparse
import ipaddress
import sys

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from thriftgen.ConfigurationService import ConfigurationService
from thriftgen.ConfigurationService.ConfigurationService import RuleType, IpRule

def create_ip_rule(args, ip_address=None, rule_type=None):
  if ip_address is None:
    ip_address = args.ip_address
  if rule_type is None:
    rule_type = RuleType._NAMES_TO_VALUES[args.rule_type]
  return IpRule(ip_address.packed, rule_type)

def add_ip(client, args):
  if args.dry_run:
    print 'dry_run: add_ip', args.ip_address, args.rule_type
    return True

  ip_rule = create_ip_rule(args)
  result = client.addIpRule(ip_rule)
  if not result.success:
    print >> sys.stderr, result.errorReason
    return False
  return True

def remove_ip(client, args):
  if args.dry_run:
    print 'dry_run: remove_ip', args.ip_address
    return True

  ip_rule = create_ip_rule(args, rule_type=RuleType.blacklist)
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
  for host in hosts:
    hosts_added += 1
    if args.verbose:
      print 'add_network', args.network_address, 'host', host
    ip_rule = create_ip_rule(args, ip_address=host)
    result = client.addIpRule(ip_rule)
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
    ip_rule = create_ip_rule(args, ip_address=host, rule_type=RuleType.blacklist)
    result = client.removeIpRule(ip_rule)
    if not result.success:
      print >> sys.stderr, result.errorReason
      success = False
  if args.verbose or hosts_removed == 0:
    print "%d hosts removed" % hosts_removed
  return hosts_removed != 0

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

def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument('-H', '--config-host', dest='config_host', required=True)
  parser.add_argument('-P', '--config-port', dest='config_port', default=9999)
  parser.add_argument('-n', '--dry-run', action='store_const', const=True, default=False)
  parser.add_argument('-v', '--verbose', action='store_const', const=True, default=False)
  subparsers = parser.add_subparsers()
  ip_subcommand(subparsers)
  network_subcommand(subparsers)

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
  result = args.command(client, args)
  if not result:
    sys.exit(1)

if __name__ == '__main__':
  main()
