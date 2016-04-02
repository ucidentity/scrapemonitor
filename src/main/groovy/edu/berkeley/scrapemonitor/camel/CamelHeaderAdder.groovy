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

package edu.berkeley.scrapemonitor.camel

import java.util.regex.Pattern

/**
 * Configuration for a "header adder".
 *
 * A header adder adds headers to a
 * message moving through the camel route based on the incoming message data.
 *
 * For example, you may want to detect if a log entry is severe or just a
 * warning and put that in the subject of an outgoing email.  To accomplish
 * this you create a header that can then be used in a subjectTemplate of an
 * email.
 *
 * @author Brian Koehmstedt
 */
class CamelHeaderAdder {
    static class Header {
        String headerName
        String headerValue
    }

    static class Regex {
        String regexName
        Pattern bodyRegex
        Map<String, Header> headers

        void setBodyRegex(String regex) {
            this.bodyRegex = Pattern.compile(regex)
        }

        void addHeader(String headerName, Header header) {
            if (!headers) headers = [:]
            headers.put(headerName, header)
        }

        void addHeader(String headerName, String headerValue) {
            if (!headers) headers = [:]
            headers.put(headerName, new Header(headerName: headerName, headerValue: headerValue))
        }
    }

    String headerAdderName
    // If any of the regexps match, then all of the headers are added.
    Map<String, Regex> regexps

    void addBodyRegex(String regexName, Regex rex) {
        if (!regexps) regexps = [:]
        regexps.put(regexName, rex)
    }
}
