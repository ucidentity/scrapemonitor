/*
 * Copyright (c) 2016, Regents of the University of California and
 * contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.scrapemonitor

import edu.berkeley.scrapemonitor.camel.CamelHeaderAdder
import edu.berkeley.scrapemonitor.camel.CamelMonitorExecutor
import edu.berkeley.scrapemonitor.camel.CamelRegexFilter
import edu.berkeley.scrapemonitor.camel.destination.smtp.CamelSmtpDestination
import edu.berkeley.scrapemonitor.camel.destination.smtp.CamelSmtpDestinationConfiguration
import edu.berkeley.scrapemonitor.file.FileMonitorConfiguration
import edu.berkeley.scrapemonitor.file.FileMonitorExecutor
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import org.apache.camel.impl.DefaultCamelContext
import org.slf4j.LoggerFactory

/**
 * This is the main application daemon that runs all the configured monitors.
 *
 * Configuration is done in Config.groovy in the class path.  That is a ConfigSluper-style configuration file.
 *
 * @author Brian Koehmstedt
 */
@Slf4j
class ScrapeMonitorDaemon {
    static String CONFIG_FILE_PROPERTY_NAME = "edu.berkeley.scrapemonitor.config_file"

    ConfigObject config
    DefaultCamelContext camelContext
    Map<String, MonitorExecutor> monitorExecutors
    Map<String, MonitorExecutor> destinationExecutors

    static void main(String[] args) {
        ScrapeMonitorDaemon daemon = new ScrapeMonitorDaemon()
        daemon.initialize()
        daemon.start()
        // run for ever
        while (true) {
            Thread.sleep(5000)
        }
    }

    /**
     * initialize before starting
     */
    void initialize() {
        log.debug("Initializing")
        ConfigObject configObject = readConfiguration()
        if (configObject != null) {
            config = configObject

            createMonitors()

            createCamelContext()
        } else {
            throw new RuntimeException("No monitors can be started without configuration.")
        }
    }

    /**
     * Start the monitors.  initialize() must be run first.
     */
    void start() {
        if (config == null)
            throw new RuntimeException("No configuration.  initialize() must be called before start()")

        log.debug("Starting")

        startCamelContext()
        addCamelContextToCamelExecutors()

        startMonitors()

        log.info("Started")
    }

    /**
     * Stop all the monitors.
     */
    void stop() {
        stopMonitors()
        log.info("Stopped")
    }

    /**
     * Get a MonitorExecutor by its configuration name (typically configured in Config.groovy).
     *
     * @param name The configuration name for the MonitorExecutor.
     * @return The found MonitorExecutor with the given name.  Null if not found by given name.
     */
    MonitorExecutor getMonitorExecutor(String name) {
        return monitorExecutors[name]
    }

    MonitorExecutor getDestinationExecutor(String name) {
        return destinationExecutors[name]
    }

    /**
     * Read the system configuration from Config.groovy.
     *
     * @return The ConfigObject
     */
    protected static ConfigObject readConfiguration() {
        File configFile
        if (!System.getProperty(CONFIG_FILE_PROPERTY_NAME)) {
            URL configFileURL = ScrapeMonitorDaemon.class.classLoader.getResource("Config.groovy")
            if (!configFileURL)
                throw new RuntimeException("Config.groovy not found in classpath.  You can also set the $CONFIG_FILE_PROPERTY_NAME system property to point to a config file.")
            configFile = new File(configFileURL.toURI())
        } else {
            configFile = new File(System.getProperty(CONFIG_FILE_PROPERTY_NAME))
            if (!configFile.exists())
                throw new RuntimeException("System property $CONFIG_FILE_PROPERTY_NAME is set to ${configFile.absolutePath} but that file does not eixst")
        }
        if (configFile.exists()) {
            log.info("Using config file ${configFile.absolutePath}")
            ConfigSlurper configSlurper = new ConfigSlurper()
            def binding = [
                    log: LoggerFactory.getLogger("Config.groovy")
            ]
            configSlurper.setBinding(binding)
            ConfigObject configObject = configSlurper.parse(configFile.toURI().toURL())
            log.trace("Configuration: ${new JsonBuilder(configObject).toPrettyString()}")
            return configObject
        } else {
            throw new RuntimeException("${configFile.absolutePath} does not exist")
        }
    }

    /**
     * Create the MonitorExecutors based on what is configured.
     */
    protected void createMonitors() {
        this.monitorExecutors = [:]
        this.destinationExecutors = [:]

        Map<String, CamelHeaderAdder> headerAdderMap = [:]
        config.headerAdders?.each { String headerAdderName, Map<String, Map> m ->
            CamelHeaderAdder camelHeaderAdder = new CamelHeaderAdder(headerAdderName: headerAdderName)
            m?.each { String regexName, Map regexMap ->
                CamelHeaderAdder.Regex rex = new CamelHeaderAdder.Regex(regexName: regexName)
                if (!regexMap.bodyRegex)
                    throw new RuntimeException("bodyRegex required for headerAdder $headerAdderName and regex name $regexName")
                rex.bodyRegex = regexMap.bodyRegex
                regexMap.headers?.each { String headerName, Map headerMap ->
                    rex.addHeader(headerName, headerMap.value as String)
                    log.trace("Configured header adder for headerAdder name $headerAdderName and regexp $regexName: bodyRegex=${rex.bodyRegex}, header name=$headerName, value=${headerMap.value}")
                }
                camelHeaderAdder.addBodyRegex(regexName, rex)
            }
            headerAdderMap.put(headerAdderName, camelHeaderAdder)
        }

        config.monitors?.each { String monitorName, Map m ->
            if (m.type == "file") {
                if (!m.path)
                    throw new RuntimeException("path is required for $monitorName")
                if (m.cfg?.delayMillis == null)
                    throw new RuntimeException("cfg.delayMillis is required for $monitorName")
                if (!m.destination)
                    throw new RuntimeException("cfg.destination is required for $monitorName")

                FileMonitorConfiguration cfg = new FileMonitorConfiguration()
                cfg.delayMillis = m.cfg.delayMillis

                FileMonitorExecutor executor = new FileMonitorExecutor(
                        monitorName,
                        cfg,
                        new File(m.path as String),
                        null, // CamelContext added in addCamelContextToCamelExecutors()
                        m.destination as String
                )

                if (m.filterType == "regex") {
                    if (!m.rejectRegex && !m.acceptRegex) {
                        throw new RuntimeException("If filterType is regex, then either rejectRegex or acceptRegex must be set (or both)")
                    }
                    CamelRegexFilter filter = new CamelRegexFilter()
                    if (m.rejectRegex)
                        filter.rejectRegexps = m.rejectRegex as List<String>
                    if (m.acceptRegex)
                        filter.acceptRegexps = m.acceptRegex as List<String>

                    executor.filterBean = filter
                }

                // header adders
                m.headerAdders?.each { String headerAdderName ->
                    CamelHeaderAdder headerAdder = headerAdderMap[headerAdderName]
                    if (!headerAdder)
                        throw new RuntimeException("headerAdder $headerAdderName is not configured in the headerAdders section")
                    executor.addHeaderAdder(headerAdderName, headerAdder)
                }

                monitorExecutors[monitorName] = executor

            }
        }

        // destinations are where incoming monitor messages get sent
        config.destinations?.each { String destinationName, Map m ->
            if (m.type == "smtp") {
                if (!m.consumerUri)
                    throw new RuntimeException("consumerUri is required for $destinationName")
                if (!m.toEmailAddresses)
                    throw new RuntimeException("toEmailAddresses is required for $destinationName")
                if (!m.subjectTemplate)
                    throw new RuntimeException("subjectTemplate is required for $destinationName")
                if (!m.cfg?.smtpHost)
                    throw new RuntimeException("cfg.smtpHost is required for $destinationName")
                if (!m.cfg?.smtpPort)
                    throw new RuntimeException("cfg.smtpPort is required for $destinationName")
                if (!m.cfg?.fromEmailAddress)
                    throw new RuntimeException("cfg.fromEmailAddress is required for $destinationName")

                CamelSmtpDestinationConfiguration cfg = new CamelSmtpDestinationConfiguration()
                cfg.smtpHost = m.cfg.smtpHost
                cfg.smtpPort = m.cfg.smtpPort
                cfg.fromEmailAddress = m.cfg.fromEmailAddress

                CamelSmtpDestination executor = new CamelSmtpDestination(destinationName, cfg, camelContext, m.consumerUri)
                executor.toEmailAddresses = m.toEmailAddresses
                executor.subjectTemplate = m.subjectTemplate

                destinationExecutors[destinationName] = executor
            }
        }
    }

    /**
     * Create a CamelContext for this daemon.  The context is not started until startCamelContext() is called.
     */
    protected void createCamelContext() {
        this.camelContext = new DefaultCamelContext()
    }

    /**
     * Start the CamelContext for this daemon.
     */
    protected void startCamelContext() {
        camelContext.start()
        for (int i = 0; i < 10; i++) {
            if (camelContext.isStarted())
                break;
            Thread.sleep(1000)
        }
        if (!camelContext.isStarted())
            throw new RuntimeException("Couldn't start context")
    }

    /**
     * For each configured monitor executor that implements the CamelMonitorExecutor interface, set the CamelContext for that executor.
     */
    protected void addCamelContextToCamelExecutors() {
        monitorExecutors.values().each {
            if (it instanceof CamelMonitorExecutor) {
                ((CamelMonitorExecutor) it).camelContext = camelContext
            }
        }
        destinationExecutors.values().each {
            if (it instanceof CamelMonitorExecutor) {
                ((CamelMonitorExecutor) it).camelContext = camelContext
            }
        }
    }

    /**
     * Start each of the individual monitors.
     */
    protected void startMonitors() {
        /**
         * The destination consumers should be started first so that
         * they are already running by the time the monitors start
         * sending data to their destinations.
         */
        destinationExecutors.values().each { MonitorExecutor it ->
            it.start()
            log.info("Destination consumer ${it.name} started")
        }
        monitorExecutors.values().each { MonitorExecutor it ->
            it.start()
            log.info("Monitor ${it.name} started")
        }
    }

    /**
     * Stop each of the individual monitors.
     */
    protected void stopMonitors() {
        /**
         * Monitor executors should be stopped before their destinations
         * are stopped.
         */
        monitorExecutors.values().each { MonitorExecutor it ->
            it.stop()
            log.info("Monitor ${it.name} stopped")
        }
        destinationExecutors.values().each { MonitorExecutor it ->
            it.stop()
            log.info("Destination consumer ${it.name} stopped")
        }
    }
}
