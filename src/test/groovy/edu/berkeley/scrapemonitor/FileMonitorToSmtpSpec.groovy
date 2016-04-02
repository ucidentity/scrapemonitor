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

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import spock.lang.Specification

import javax.mail.internet.MimeMessage

/**
 * Test monitoring files for added lines and sending out alerts via SMTP.
 *
 * @author Brian Koehmstedt
 */
class FileMonitorToSmtpSpec extends Specification {
    String monitorName = "sampleFileToEmail"
    File sampleFile = new File("sampleFileForEmail.txt")
    ScrapeMonitorDaemon daemon
    PrintStream ps
    GreenMail greenMail

    def setup() {
        sampleFile.delete()

        // has to be opened before the daemon starts
        ps = new PrintStream(new FileOutputStream(sampleFile))

        // Start a test SMTP server on port 7525
        greenMail = new GreenMail(
                new ServerSetup(7525, "localhost", ServerSetup.PROTOCOL_SMTP)
        )
        greenMail.start()

        daemon = new ScrapeMonitorDaemon()
        daemon.initialize()
        assert daemon.getMonitorExecutor(monitorName)
        daemon.start()
    }

    def cleanup() {
        ps.close()
        greenMail.stop()
        daemon.stop()
        sampleFile.delete()
    }

    void "test that the file monitor picks up added lines and sends them via smtp"() {
        when:
            appendToFile(ps, "new line 1")
            appendToFile(ps, "new line 2")

            // allow time for the consumer to pick it up
            for (int i = 0; i < 10; i++) {
                if (greenMail.receivedMessages.length == 2)
                    break
                sleep(100)
            }
            MimeMessage[] messages = greenMail.receivedMessages


        then:
            messages.length == 2
            messages*.subject == [
                    "[ScrapeMon] $monitorName ALERT1".toString(),
                    "[ScrapeMon] $monitorName ALERT2".toString()
            ]
            messages*.content == ["new line 1\r\n", "new line 2\r\n"]
    }

    void appendToFile(PrintStream ps, String line) {
        ps.println(line)
    }
}
