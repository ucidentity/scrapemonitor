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

If you did `gradle installDist` or are using a binary distribtion:

Create a file that will hold environment variables for the start-up script. 
To make it easy, name it `local.env` and put it in the same directory as
[runDist.sh](runDist.sh), which is the root directory of the source
distribution.

`local.env`:

```
# Don't forget to run "gradle installDist" first

# Mandatory
SCRAPEMON_CONFIG_FILE=/path/Config.groovy
# Optional
SCRAPEMON_LOG4J_FILE=/path/log4j2.properties
```

Replace `/path/Config.groovy` with the path to your `Config.groovy` file
that configures ScrapeMonitor.

If you want to override the default
[src/main/resources/log4j2.properties](src/main/resources/log4j2.properties)
file, replace `/path/log4j2.properties` with the location of your own
`log4j2.properties` file.  Otherwise, to use the default
[src/main/resources/log4j2.properties](src/main/resources/log4j2.properties)
file provided in the distribution, don't add a `SCRAPEMON_LOG4J_FILE` line
to your `local.env` file.

[Log4j2 configuration information](https://logging.apache.org/log4j/2.x/manual/configuration.html)

You may also use the `run.sh` script that does the same thing using the
`gradlew` wrapper instead.  The first argument to the script is the path to
your config file.

```./run.sh /path/Config.groovy```

Optionally, if you want to override the default `log4j2.properties` file:
```./run.sh /path/Config.groovy -Dlog4j.configurationFile=/path/log4j2.properties```

### Configuration

See [src/test/resources/Config.groovy](src/test/resources/Config.groovy) for
a sample configuration file.  
