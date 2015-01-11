#!/bin/bash


tmux -S /tmp/xio new-session -s xio -d 'vim'
tmux rename-window 'vim'
tmux neww
tmux rename-window 'server'
chmod 700 /tmp/xio
tmux -S /tmp/xio attach -t xio



# tmux new-session -d -s xio 'exec pfoo'
# tmux send-keys 'bundle exec thin start' 'C-m'
# tmux rename-window 'Foo'
# tmux select-window -t xio:0
# tmux split-window -h 'exec pfoo'
# tmux send-keys 'bundle exec compass watch' 'C-m'
# tmux split-window -v -t 0 'exec pfoo'
# tmux send-keys 'rake ts:start' 'C-m'
# tmux split-window -v -t 1 'exec pfoo'
# tmux -2 attach-session -t xio
#
#
