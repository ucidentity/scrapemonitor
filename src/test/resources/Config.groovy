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

/**
 * Configuration for tests.
 *
 * This is a Groovy ConfigSlurper-style configuration file.
 *
 * @author Brian Koehmstedt
 */
Map stdFileConfiguration = [
        // check every 100ms for new data appended to a file
        delayMillis: 100
]

Map stdSmtpConfiguration = [
        smtpHost        : "localhost",
        // the smtp tests uses GreenMail listening on port 7525
        smtpPort        : 7525,
        fromEmailAddress: "scrapemonitor@localhost"
]

monitors {
    sampleFileToBuffer {
        type = "file"
        path = "sampleFile.txt"
        destination = "direct:receivedline"
        cfg = stdFileConfiguration

        filterType = "regex"
        // an array of regular expresisons where if there's a match on any
        // of the regular expressions, the file line will be ignored
        rejectRegex = [
                "filter out"
        ]

        headerAdders = ["testLine"]
    }

    sampleFileToEmail {
        type = "file"
        path = "sampleFileForEmail.txt"
        destination = "direct:emailOut"
        cfg = stdFileConfiguration
        headerAdders = ["testLine"]
    }
}

headerAdders {
    testLine {
        newLine1 {
            bodyRegex = "new line 1"
            headers {
                SMTP_SUBJECT {
                    value = "ALERT1"
                }
            }
        }
        newLine2 {
            bodyRegex = "new line 2"
            headers {
                SMTP_SUBJECT {
                    value = "ALERT2"
                }
            }
        }
    }
}

destinations {
    emailOut {
        type = "smtp"
        consumerUri = "direct:emailOut"
        toEmailAddresses = ["bkoehmstedt@localhost"]
        cfg = stdSmtpConfiguration
        subjectTemplate = '[ScrapeMon] ${headers.FROM_MONITOR} ${headers.SMTP_SUBJECT ?: "Alert"}'
    }
}
