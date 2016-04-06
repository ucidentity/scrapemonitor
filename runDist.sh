#!/bin/sh

#
# Copyright (c) 2016, Regents of the University of California and
# contributors.
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
# 1. Redistributions of source code must retain the above copyright notice,
#    this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
# IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
# THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
# PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
# PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

#
# To build ScrapeMonitor and create or update the build/install directory,
# run this:
#
# ./gradlew installDist
#
# This has to be done after updates are pulled from Github.
#

SCRIPT_DIR=`dirname "$0"`
APP_NAME="scrapemonitor"

if [ ! -e "${SCRIPT_DIR}/build/install/${APP_NAME}/bin/${APP_NAME}" ];
then
  echo "${SCRIPT_DIR}/build/install/${APP_NAME}/bin/${APP_NAME} does not exist"
  echo "Run this first before running this script again:"
  echo "(cd $SCRIPT_DIR; ./gradlew installDist)"
  exit 1
fi

# helps if you need to run killall
SCRAPEMONITOR_OPTS="-Dis.scrapemonitor=1 $SCRAPEMONITOR_OPTS"

# If SCRAPEMON_ENV_FILE is not set, then try ./local.env
if [ -n "$SCRAPEMON_ENV_FILE" ];
then
  echo "Environment file: $SCRAPEMON_ENV_FILE"
  if [ -r "$SCRAPEMON_ENV_FILE" ];
  then
    . "$SCRAPEMON_ENV_FILE"
  else
    echo "$SCRAPEMON_ENV_FILE does not exist or is not readable"
    exit 1
  fi
else
  if [ -e "${SCRIPT_DIR}/local.env" ];
  then
    echo "Environment file: ${SCRIPT_DIR}/local.env"
    . ${SCRIPT_DIR}/local.env
   else
     echo "Environment file: none"
  fi
fi

if [ -n "$SCRAPEMON_CONFIG_FILE" ];
then
  echo "Config.groovy file: $SCRAPEMON_CONFIG_FILE"
  if [ -r "$SCRAPEMON_CONFIG_FILE" ];
  then
    SCRAPEMONITOR_OPTS="$SCRAPEMONITOR_OPTS -Dedu.berkeley.${APP_NAME}.config_file=$SCRAPEMON_CONFIG_FILE"
  else
    echo "$SCRAPEMON_CONFIG_FILE does not exist or is not readable"
    exit 1
  fi
else
  echo "Config.groovy file: None set.  Config.groovy will be searched in classpath."
fi

if [ -n "$SCRAPEMON_LOG4J_FILE" ];
then
  echo "log4j2.properties file: $SCRAPEMON_LOG4J_FILE"
  if [ -r "$SCRAPEMON_LOG4J_FILE" ];
  then
    SCRAPEMONITOR_OPTS="$SCRAPEMONITOR_OPTS -Dlog4j.configurationFile=$SCRAPEMON_LOG4J_FILE"
  else
    echo "$SCRAPEMON_LOG4J_FILE does not exist or is not readable"
    exit 1
  fi
else
  echo "log4j2.properties file: None set.  Default log4j2.properties in classpath will be used."
fi

echo "SCRAPEMONITOR_OPTS: $SCRAPEMONITOR_OPTS"

echo "Starting..."
(export SCRAPEMONITOR_OPTS; \
  ${SCRIPT_DIR}/build/install/${APP_NAME}/bin/${APP_NAME}) &
