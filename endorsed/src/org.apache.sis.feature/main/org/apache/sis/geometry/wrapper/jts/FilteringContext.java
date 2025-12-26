/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.geometry.wrapper.jts;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.DelayedRunnable;


/**
 * Helper objects needed during the execution of a filtering operation.
 * Those objects are assumed non-thread safe; different instances will be created for each thread.
 *
 * <p>Ideally this object should be created when a filtering operation on a collection of features
 * is about to start, and disposed after the filtering operation is completed. We do not yet have
 * a notification mechanism for those events, so current implementation use a {@link Queue}.
 * A future version may revisit this strategy and expand the use of "filtering context" to all
 * geometry implementations, not only JTS (but we may keep a specialized JTS subclass).</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FilteringContext {
    /**
     * Well-Known Binary (WKB) writers of geometry objects. This is currently the only kind
     * of objects that we recycle because it contains potentially large buffer arrays.
     * All other JTS readers and writers are cheap to construct, so caching them may be counter-productive.
     */
    private static final Queue<WKBWriter> WRITERS = new ConcurrentLinkedQueue<>();

    /**
     * A flag telling whether a cleaning task has been registered.
     */
    private static final AtomicBoolean CLEANER_REGISTERED = new AtomicBoolean();

    /**
     * Do not allow (in current version) instantiation of this class.
     */
    private FilteringContext() {
    }

    /**
     * Writes the given geometry in Well-Known Binary (WKB) format.
     */
    static byte[] writeWKB(final Geometry geometry) {
        WKBWriter writer = WRITERS.poll();
        if (writer == null) {
            writer = new WKBWriter();
        }
        final byte[] wkb = writer.write(geometry);
        /*
         * Unconditionally dispose all writers after 2 minutes, no matter if some threads
         * still need writers or not. The intent is to avoid retention of large buffers.
         * WKB writer are not so expensive to creates, so recreating them every 2 minutes
         * should not have a visible impact on performance.
         */
        if (WRITERS.add(writer) && CLEANER_REGISTERED.compareAndSet(false, true)) {
            DelayedExecutor.schedule(new DelayedRunnable(2, TimeUnit.MINUTES) {
                @Override public void run() {
                    CLEANER_REGISTERED.set(false);
                    WRITERS.clear();
                }
            });
        }
        return wkb;
    }
}
