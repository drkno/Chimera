#!/bin/sh

nohup sudo node chimera.js 2>&1 > /dev/null & disown
exit 0
