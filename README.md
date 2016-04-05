# ScrapeMonitor

An application for monitoring system resources such as log files and sending
out alerts.

## Building

`gradle installDist`

## Creating a distribution archive

`gradle distZip` or `gradle distTar`

## Running

If you did `gradle installDist`:
```build/install/scrapemonitor/bin/scrapemonitor -Dedu.berkeley.scrapemonitor.config_file=/path/Config.groovy```

Replace `/path/Config.groovy` with the path to your `Config.groovy` file
that configures ScrapeMonitor.

You may also use the `run.sh` script that does the same thing, with the
first argument to the script being the path to your config file.
```./run.sh /path/Config.groovy```

Optionally, if you want to override the default log4j2.properties file:
```./run.sh /path/Config.groovy -Dlog4j.configurationFile=/path/log4j2.properties```

Replace `/path/log4j2.properties` with the path to your Log4j2 properties
file.  See `src/main/resources/log4j2.properties` for the default file and
you can also use this as a template for your own log4j2 configuration.

### Configuration

See `src/test/resources/Config.groovy` for a sample configuration file.
 