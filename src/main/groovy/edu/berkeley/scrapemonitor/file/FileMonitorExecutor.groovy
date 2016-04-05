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

package edu.berkeley.scrapemonitor.file

import edu.berkeley.scrapemonitor.camel.AbstractCamelMonitorExecutor
import edu.berkeley.scrapemonitor.camel.CamelHeaderAdder
import edu.berkeley.scrapemonitor.camel.CamelRouteUtil
import groovy.util.logging.Slf4j
import org.apache.camel.*
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultProducerTemplate
import org.apache.camel.model.ProcessorDefinition
import software.javatailer.TailerCallback
import software.javatailer.TailerThread

import java.nio.file.Path

/**
 * A monitor executor that monitors changes to a file.
 *
 * @author Brian Koehmstedt
 */
@Slf4j
class FileMonitorExecutor extends AbstractCamelMonitorExecutor<FileMonitorConfiguration, FileMonitorContext> {
    String name
    FileMonitorConfiguration monitorConfiguration
    File file
    CamelContext camelContext
    String camelUri
    FileMonitorContext monitorContext
    TailerThread tailerThread

    /**
     * Optional, to filter which incoming lines go to the destination. 
     * Recommended to use an instance of an implementation of
     * AbstractFilterOnBodyBean.
     */
    Object filterBean

    FileMonitorExecutor(String name, FileMonitorConfiguration configuration, File file, CamelContext camelContext, String camelDestination) {
        this.name = name
        this.monitorConfiguration = configuration
        this.file = file
        this.camelContext = camelContext
        this.camelUri = camelDestination
    }

    void start() {
        if (monitorContext)
            throw new IllegalStateException("Already started")

        this.monitorContext = new FileMonitorContext();
        monitorContext.routeId = camelContext.uuidGenerator.generateUuid()

        // this starts the seda consumer which monitors the queue
        camelContext.addRoutes(createRouteBuilder())
        // wait up to 5s for it to start
        if (!CamelRouteUtil.waitForRouteToStart(camelContext, monitorContext.routeId, 5000))
            throw new RuntimeException("Route ${monitorContext.routeId} could not be started")

        // this starts the file monitor thread which adds content to the
        // seda queue as file modifications occur
        launchTailerThread()
    }

    void stop() {
        if (!monitorContext)
            throw new IllegalStateException("Never started")
        camelContext.stopRoute(monitorContext.routeId)

        tailerThread.doStop()
        tailerThread.interrupt()
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            void configure() throws Exception {
                log.trace("Monitor $name is going to monitor file ${file.absolutePath}")
                Endpoint endpoint = context.getEndpoint("seda:${file.path}")

                // filter input if a filter bean is set
                ProcessorDefinition from = from(endpoint).routeId(monitorContext.routeId)
                if (filterBean) {
                    from = from.choice().when().method(filterBean)
                }

                // set exchange header values based on input and regular
                // expressions
                if (headerAdders) {
                    from = from.process(new Processor() {
                        @Override
                        void process(Exchange exchange) throws Exception {
                            exchange.out = exchange.in
                            headerAdders?.values()?.each { final CamelHeaderAdder headerAdder ->
                                // for each regular expression, see if there
                                // is an expression match and if there is,
                                // set all the headers configured for the
                                // regex
                                headerAdder.regexps?.values()?.each { CamelHeaderAdder.Regex rex ->
                                    if (rex.bodyRegex.matcher(exchange.in.body as String).matches()) {
                                        rex.headers.values().each { CamelHeaderAdder.Header header ->
                                            exchange.out.setHeader(header.headerName, header.headerValue)
                                        }
                                    }
                                }
                            }

                        }
                    })
                }

                if (filterBean) {
                    // This is a continuation of the choice() started above
                    // if filterBean was set.  We need to stop the route in
                    // otherwise() if the filterBean returned false.
                    from = from.otherwise().stop().end()
                }

                from.setHeader("FROM_MONITOR", constant(name)).to(camelUri)
            }
        }
    }

    /**
     * Launch the JavaTailer thread that monitors a file for added data and
     * sends that data in the form of a String to a seda queue.  Camel then
     * route from the seda queue to the configured destination.  one example
     * of a destination would be the SmtpDestination.
     */
    protected void launchTailerThread() {
        final DefaultProducerTemplate template = new DefaultProducerTemplate(camelContext)
        template.start();
        for (int i = 0; i < 10; i++) {
            if (template.isStarted())
                break;
            Thread.sleep(1000);
        }
        if (!template.isStarted())
            throw new RuntimeException("Couldn't start template");

        Path thepath = file.toPath().toAbsolutePath()
        this.tailerThread = new TailerThread(new TailerCallback() {
            @Override
            void createEvent(Path path) {

            }

            @Override
            void deleteEvent(Path path) {

            }

            @Override
            void truncateEvent(Path path) {

            }

            @Override
            void receiveEvent(Path path, byte[] bytes) {
                // send each line to the seda queue
                String[] lines = new String(bytes).split("\\n")
                lines.each { String line ->
                    if (line) {
                        template.sendBody("seda:${file.path}", ExchangePattern.InOnly, line)
                    }
                }
            }

            @Override
            void callbackRuntimeException(String s, RuntimeException e) {

            }
        }, thepath.getParent().toString(), thepath.getName(thepath.getNameCount() - 1).toString())
        tailerThread.start()
        if (!tailerThread.waitForStart(2000)) {
            if (tailerThread.exception)
                throw new RuntimeException("TailerThread could not start", tailerThread.exception)
            else
                new RuntimeException("TailerThread could not start due to time out")
        }
    }
}
