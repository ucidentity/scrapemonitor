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

package edu.berkeley.scrapemonitor.camel.destination.smtp

import edu.berkeley.scrapemonitor.camel.CamelMonitorExecutor
import edu.berkeley.scrapemonitor.camel.CamelRouteUtil
import groovy.text.GStringTemplateEngine
import groovy.text.Template
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mail.MailEndpoint

/**
 * This sends out email via SMTP.  It's implemented as a monitor because it first consumes
 * messages to send from a Camel direct endpoint.
 *
 * @author Brian Koehmstedt
 */
class CamelSmtpDestination implements CamelMonitorExecutor<CamelSmtpDestinationConfiguration, CamelSmtpDestinationContext> {
    String name
    CamelSmtpDestinationConfiguration monitorConfiguration
    CamelSmtpDestinationContext monitorContext
    CamelContext camelContext
    String camelUri
    List<String> toEmailAddresses
    String subjectTemplate

    CamelSmtpDestination(String name, CamelSmtpDestinationConfiguration configuration, CamelContext camelContext, String camelConsumerUri) {
        this.name = name
        this.monitorConfiguration = configuration
        this.camelContext = camelContext
        this.camelUri = camelConsumerUri
    }

    void start() {
        if (monitorContext)
            throw new IllegalStateException("Already started")

        this.monitorContext = new CamelSmtpDestinationContext();
        monitorContext.routeId = camelContext.uuidGenerator.generateUuid()

        // this starts the consumer which monitors the file
        camelContext.addRoutes(createRouteBuilder())
        // wait up to 5s for it to start
        if (!CamelRouteUtil.waitForRouteToStart(camelContext, monitorContext.routeId, 5000))
            throw new RuntimeException("Route ${monitorContext.routeId} could not be started")
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
                def toEmailAddressesString = toEmailAddresses.join(",")

                def engine = new GStringTemplateEngine()
                Template template = engine.createTemplate(subjectTemplate)

                String endpointUri = "smtp://${monitorConfiguration.smtpHost}:${monitorConfiguration.smtpPort}?to=$toEmailAddressesString&from=${monitorConfiguration.fromEmailAddress}"
                MailEndpoint endpoint = (MailEndpoint) context.getEndpoint(endpointUri)

                // configure a consumer that routes from the direct endpoint to the smtp endpoint, which sends the email
                from(camelUri).routeId(monitorContext.routeId).process(new Processor() {
                    @Override
                    void process(Exchange exchange) throws Exception {
                        // convert the subjectTemplate to an actual string
                        StringWriter writer = new StringWriter(1024)
                        Writable writable = template.make([
                                destinationName: name,
                                headers        : exchange.in.headers
                        ])
                        writable.writeTo(writer)
                        String subject = writer.toString()
                        exchange.out = exchange.in

                        // the smtp component will look at this header for the subject
                        exchange.out.setHeader("subject", subject)

                        log.debug("Sending out email from monitor ${exchange.in.getHeader('FROM_MONITOR')} with subject: $subject")
                    }
                }).to(endpoint)
            }
        }
    }
}
