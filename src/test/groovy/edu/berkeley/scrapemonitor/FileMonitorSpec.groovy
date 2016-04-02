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

import edu.berkeley.scrapemonitor.camel.CamelMonitorExecutor
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import spock.lang.Specification


/**
 * Test monitoring files for added lines.
 *
 * @author Brian Koehmstedt
 */
class FileMonitorSpec extends Specification {
    String monitorName = "sampleFileToBuffer"
    File sampleFile = new File("sampleFile.txt")
    ScrapeMonitorDaemon daemon
    LinkedList<String> received = Collections.synchronizedList([])
    PrintStream ps

    def setup() {
        sampleFile.delete()

        // has to be opened before the daemon starts
        ps = new PrintStream(new FileOutputStream(sampleFile))

        daemon = new ScrapeMonitorDaemon()
        daemon.initialize()
        // the sampleFile monitor should be configured in the test Config.groovy file
        assert daemon.getMonitorExecutor(monitorName)
        daemon.camelContext.addRoutes(createRouteBuilder())
        daemon.start()
        this.received = []
    }

    def cleanup() {
        ps.close()
        daemon.stop()
        sampleFile.delete()
    }

    RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            void configure() throws Exception {
                String destination = ((CamelMonitorExecutor) daemon.getMonitorExecutor(monitorName)).camelUri
                from(destination).process(new Processor() {
                    @Override
                    void process(Exchange exchange) throws Exception {
                        received.add(exchange.getIn().getBody(String))
                    }
                })
            }
        }
    }

    void "test that the file monitor picks up added lines"() {
        when:
            appendToFile(ps, "new line 1")
            // the following line should be filtered out (due to the filter regexps in the configuration) and not sent to
            // the destination and therefore not added to the 'received'
            // buffer
            appendToFile(ps, "filter out")
            appendToFile(ps, "new line 2")

            // allow time for the consumer to pick it up
            for (int i = 0; i < 10; i++) {
                if (received.size() == 2)
                    break
                sleep(100)
            }

        then:
            received == ["new line 1", "new line 2"] as LinkedList<String>
    }

    void appendToFile(PrintStream ps, String line) {
        ps.println(line)
    }
}
