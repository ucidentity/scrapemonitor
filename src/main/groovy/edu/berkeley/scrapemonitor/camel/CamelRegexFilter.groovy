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
 * A CamelRegexFilter either accepts or rejects incoming messages from a
 * monitor executor based on regular expressions.
 *
 * rejectRegexps take precedence over acceptRegexps.  If no acceptRegexps set,
 * then all lines that are not rejected are accepted.
 *
 * @author Brian Koehmstedt
 */
class CamelRegexFilter extends AbstractFilterOnBodyBean<String> {
    List<Pattern> rejectRegexps
    List<Pattern> acceptRegexps

    boolean acceptBody(String body) {
        for (Pattern regex in rejectRegexps) {
            if (regex.matcher(body).matches())
                return false
        }

        if (acceptRegexps) {
            for (Pattern regex in acceptRegexps) {
                if (regex.matcher(body).matches())
                    return true
            }
            // acceptRegexps are set and none of them match this, so reject it on the basis of no matching accepts
            return false
        } else {
            // if no acceptRegexps are set, then the body will be accepted since it wasn't rejected in any of the rejectRegexps
        }

        return true
    }

    void setRejectRegexps(List<String> rejectRegexps) {
        this.rejectRegexps = rejectRegexps.collect {
            Pattern.compile(it)
        }
    }

    void setAcceptRegexps(List<String> acceptRegexps) {
        this.acceptRegexps = acceptRegexps.collect {
            Pattern.compile(it)
        }

    }
}
