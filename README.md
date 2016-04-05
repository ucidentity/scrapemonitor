# ScrapeMonitor

An application for monitoring system resources such as log files and sending
out alerts.

## License

[See the ScrapeMonitor BSD two-clause license in the LICENSE.txt file](LICENSE.txt).

## Building

`gradle installDist`

## Creating a distribution archive

`gradle distZip` or `gradle distTar`

## Running

If you did `gradle installDist`:
```build/install/scrapemonitor/bin/scrapemonitor -Dedu.berkeley.scrapemonitor.config_file=/path/Config.groovy```

Replace `/path/Config.groovy` with the path to your `Config.groovy` file
that configures ScrapeMonitor.

You may also use the `run.sh` script that does the same thing using the
`gradlew` wrapper instead.  The first argument to the script is the path to
your config file.
```./run.sh /path/Config.groovy```

Optionally, if you want to override the default `log4j2.properties` file:
```./run.sh /path/Config.groovy -Dlog4j.configurationFile=/path/log4j2.properties```

Replace `/path/log4j2.properties` with the path to your `log4j2.properties`
file.  See 
[src/main/resources/log4j2.properties](src/main/resources/log4j2.properties)
for the default file andyou can also use this as a template for your own
[Log4j2 configuration](https://logging.apache.org/log4j/2.x/manual/configuration.html).

### Configuration

See [src/test/resources/Config.groovy](src/test/resources/Config.groovy) for
a sample configuration file.  
