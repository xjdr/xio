#!/bin/bash

tmux -S /tmp/xio new-session -s xio -d 'vim'
tmux rename-window 'vim'
tmux neww
tmux rename-window 'server'
chmod 700 /tmp/xio
tmux -S /tmp/xio attach -t xio

