#!/bin/sh

#
# To build ScrapeMonitor and create or update the build/install directory,
# run this:
#
# ./gradlew installDist
#
# This has to be done after updates are pulled from Github.
#

# If SCRAPEMON_ENV_FILE is not set, then try ./local.env
if [ -n "$SCRAPEMON_ENV_FILE" ];
then
  echo "Environment file: $SCRAPEMON_ENV_FILE"
  . "$SCRAPEMON_ENV_FILE"
else
  if [ -e "./local.env" ];
  then
    echo "Environment file: ./local.env"
    . ./local.env
   else
     echo "Environment file: none"
  fi
fi

if [ -n "$SCRAPEMON_CONFIG_FILE" ];
then
  echo "Config.groovy file: $SCRAPEMON_CONFIG_FILE"
  SCRAPEMONITOR_OPTS+=" -Dedu.berkeley.scrapemonitor.config_file=$SCRAPEMON_CONFIG_FILE"
else
  echo "Config.groovy file: None set.  Config.groovy will be searched in classpath."
fi

if [ -n "$SCRAPEMON_LOG4J_FILE" ];
then
  echo "log4j2.properties file: $SCRAPEMON_LOG4J_FILE"
  SCRAPEMONITOR_OPTS+=" -Dlog4j.configurationFile=$SCRAPEMON_LOG4J_FILE"
else
  echo "log4j2.properties file: None set.  Default log4j2.properties in classpath will be used."
fi

echo "SCRAPEMONITOR_OPTS: $SCRAPEMONITOR_OPTS"

echo "Starting..."
(export SCRAPEMONITOR_OPTS; \
  /apps/scrapemonitor/build/install/scrapemonitor/bin/scrapemonitor)
