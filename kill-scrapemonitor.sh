#!/bin/sh

# Meant to kill scrapemonitor started via runDist.sh.

pkill --signal HUP -f "java -Dis.scrapemonitor=1"
