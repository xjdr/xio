#!/bin/bash

if [[ ! -f /tmp/xio ]]; then
  tmux -S /tmp/xio new-session -s xio -d 'vim'
  tmux -S /tmp/xio rename-window 'vim'
  tmux -S /tmp/xio neww
  tmux -S /tmp/xio rename-window 'server'
  chmod 700 /tmp/xio
fi
tmux -S /tmp/xio attach -t xio
