#!/bin/sh

./gradlew -Dedu.berkeley.scrapemonitor.config_file=$1 $2 run
