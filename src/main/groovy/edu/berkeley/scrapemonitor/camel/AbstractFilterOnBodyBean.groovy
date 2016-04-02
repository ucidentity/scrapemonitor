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

import org.apache.camel.Body
import org.apache.camel.Handler

/**
 * Implementations of this bean are suitable for passing to a Camel
 * filter().  This filters based on the incoming message body of type T.
 *
 * @param < T >  The incoming message body type.
 *
 * @author Brian Koehmstedt
 */
abstract class AbstractFilterOnBodyBean<T> {
    /**
     * Accept an incoming message body.  Implementers should override this
     * method.
     *
     * @param body The incoming message body.
     * @return true to accept the message, false to filter it out.
     */
    abstract boolean acceptBody(T body)

    // for the annotations: http://camel.apache.org/parameter-binding-annotations.html
    // and: http://camel.apache.org/annotation-based-expression-language.html
    // for bean binding: http://camel.apache.org/bean-binding.html

    /**
     * Filter an incoming message body.  Camel's filter() will call this
     * method first because it's marked with the @Handler annotation, which
     * in turn * will delegate to acceptBody().  Implementers should
     * override the acceptBody() method.
     *
     * @param body The incoming message body.
     * @return true to accept the message, false to filter it out.
     */
    @Handler
    boolean acceptBodyHandler(@Body T body) {
        return acceptBody(body)
    }
}
