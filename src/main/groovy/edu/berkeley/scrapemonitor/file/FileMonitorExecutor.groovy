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
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.stream.StreamEndpoint
import org.apache.camel.model.ProcessorDefinition

/**
 * A monitor executor that monitors changes to a file.
 *
 * @author Brian Koehmstedt
 */
class FileMonitorExecutor extends AbstractCamelMonitorExecutor<FileMonitorConfiguration, FileMonitorContext> {
    String name
    FileMonitorConfiguration monitorConfiguration
    File file
    CamelContext camelContext
    String camelUri
    FileMonitorContext monitorContext

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

        // this starts the consumer which monitors the file
        camelContext.addRoutes(createRouteBuilder())
    }

    void stop() {
        if (!monitorContext)
            throw new IllegalStateException("Never started")
        camelContext.stopRoute(monitorContext.routeId)
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            void configure() throws Exception {
                log.trace("Monitor $name is going to monitor file ${file.absolutePath}")
                // tail the file with "delayMillis" delay in between change
                // detections
                StreamEndpoint endpoint = (StreamEndpoint) context.getEndpoint("stream:file?fileName=${file.path}&scanStream=true&scanStreamDelay=${monitorConfiguration.delayMillis}")

                // filter input if a filter bean is set
                ProcessorDefinition from = from(endpoint).routeId(monitorContext.routeId)
                if (filterBean) {
                    //from = from.filter().method(filterBean)
                    from = from.choice().when().method(filterBean)
                }

                // set exchange header values based on input and regular expressions
                if (headerAdders) {
                    from = from.process(new Processor() {
                        @Override
                        void process(Exchange exchange) throws Exception {
                            exchange.out = exchange.in
                            headerAdders?.values()?.each { final CamelHeaderAdder headerAdder ->
                                // for each regular expression, see if there is an expression match
                                // and if there is, set all the headers configured for the regex
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
                    // This is a continuation of the choice() started above if filterBean was set.
                    // We need to stop the route in otherwise() if the filterBean returned false.
                    from = from.otherwise().stop().end()
                }

                from.setHeader("FROM_MONITOR", constant(name)).to(camelUri)
            }
        }
    }
}
