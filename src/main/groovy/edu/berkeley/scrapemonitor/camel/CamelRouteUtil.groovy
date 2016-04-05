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

import org.apache.camel.CamelContext
import org.apache.camel.CamelException
import org.apache.camel.ServiceStatus

/**
 * Utility methods for Camel routes.
 */
class CamelRouteUtil {
    /**
     * Is a route started yet?
     *
     * @param context The CamelContext
     * @param routeId The id of the route
     * @return true if route has started, false if not.  true will be returned when a route is not startable.
     */
    static boolean isRouteStarted(CamelContext context, String routeId) {
        ServiceStatus status = context.getRouteStatus(routeId);
        if (status == null)
            return false
        else if (!status.startable)
            return true // assume started if it's not startable
        else
            return status.started
    }

    /**
     * Wait up to the end of a timeout period for a route to start or a thread interruption occurs.  If the route has not started
     * after the timeout period has elapsed, then a CamelException is thrown.  It is possible for false to be returned when thread interruption occurs.
     *
     * @param context The CamelContext
     * @param routeId The id of the route
     * @param timeoutMillis The maximum amount of time in milliseconds to wait for the route to start.
     * @return true if the route has started or false if it hasn't.  False is returned when thread interruption occurs and the route hasn't started yet.
     * @throws CamelException When a timeout occurs (but not when thread interruption occurs).
     */
    static boolean waitForRouteToStart(CamelContext context, String routeId, long timeoutMillis) throws CamelException {
        int totalMillis = 0
        for (totalMillis = 0; totalMillis < timeoutMillis; totalMillis += 100) {
            if (isRouteStarted(context, routeId)) {
                break
            } else {
                try {
                    Thread.sleep(100)
                }
                catch (InterruptedException e) {
                    return isRouteStarted(context, routeId)
                }
            }
        }
        boolean started = isRouteStarted(context, routeId)
        if (!started)
            throw new CamelException("Timed out waiting for route to start after $totalMillis milliseconds")
        else
            return true
    }
}
