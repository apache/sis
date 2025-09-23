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
package org.apache.sis.referencing.factory;

import org.opengis.referencing.IdentifiedObject;
import org.apache.sis.system.Configuration;
import org.apache.sis.system.DelayedExecutor;
import org.apache.sis.system.DelayedRunnable;
import org.apache.sis.util.internal.shared.Constants;


/**
 * Most recently used objects stored or accessed in {@link ConcurrentAuthorityFactory#findPool},
 * retained by strong references for preventing too early garbage collection.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>Elements in this collection are generally not in {@link ConcurrentAuthorityFactory#cache}
 *       because they are "foreigner" objects, possibly created by different authorities.
 *       We have to maintain them in a separated collection.</li>
 *   <li>We have to be careful about the references kept in this object. The purpose is to prevent garbage collection,
 *       so {@link Object#equals(Object)} is not the appropriate contract for deciding which elements to put.
 *       For example, a call to {@code Map.put(key, value)} may update the value without replacing the key if an
 *       entry already exists in the map, in which case the instance that is protected against garbage collection
 *       is not the intended one.</li>
 *   <li>We tried to use {@link java.util.LinkedHashMap} as a LRU map in a previous version.
 *       It was not really simpler because of above-cited issue with object identities.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ReferenceKeeper {
    /**
     * Number of references to keep. Should be relatively small because this class implementation
     * is not designed for large collections. Note that the number of unique entries may be lower
     * because this class does not try to avoid duplicated references.
     */
    @Configuration
    private static final int CAPACITY = 40;

    /**
     * Time to wait before to remove entries from this map. Current value is 5 minutes.
     */
    @Configuration
    private static final long EXPIRATION_TIME = 5L * 60 * Constants.NANOS_PER_SECOND;

    /**
     * The objects to retain by strong reference. May contains duplicated values and {@code null} anywhere.
     * This is used as a cyclic queue. We use an array instead of {@link java.util.LinkedHashMap} for more
     * control on which instance is retained (objet identity matter, not just object equality).
     */
    private IdentifiedObject[] cache;

    /**
     * Time where object references were stored in this object.
     * Used for finding which references expired.
     */
    private long[] timestamps;

    /**
     * Index of the last element stored in the {@link #cache} array.
     */
    private int indexOfLast;

    /**
     * Whether a cleaner task has already been registered for removing oldest entries.
     */
    private boolean hasCleanerTask;

    /**
     * Constructs an initially empty instance.
     */
    ReferenceKeeper() {
    }

    /**
     * Retains the given object by strong reference for a limited amount of time.
     *
     * @param  object  the object to temporarily retain by strong reference.
     */
    final synchronized void markAsUsed(final IdentifiedObject object) {
        if (cache == null) {
            cache = new IdentifiedObject[CAPACITY];
            timestamps = new long[CAPACITY];
        }
        final Long now = System.nanoTime();
        if (cache[indexOfLast] != object) {
            if (++indexOfLast >= CAPACITY) {
                indexOfLast = 0;
            }
        }
        cache[indexOfLast] = object;
        timestamps[indexOfLast] = now;
        if (!hasCleanerTask) {
            scheduleCleanerTask(now);
        }
    }

    /**
     * Registers a task to be executed later for removing expired entries.
     * It is caller responsibility to verify that this method should be invoked.
     *
     * @param  now  value of {@link System#nanoTime()}.
     */
    private void scheduleCleanerTask(final long now) {
        DelayedExecutor.schedule(new DelayedRunnable(now + EXPIRATION_TIME) {
            @Override public void run() {
                clearExpiredEntries();
            }
        });
        hasCleanerTask = true;
    }

    /**
     * Invoked in a background thread for clearing expired entries.
     * This will allow the garbage collector to remove the entries from
     * {@link ConcurrentAuthorityFactory#findPool} if not used elsewhere.
     */
    private synchronized void clearExpiredEntries() {
        hasCleanerTask = false;
        boolean empty = true;
        final long now = System.nanoTime();
        for (int i=0; i<CAPACITY; i++) {
            if (now - timestamps[i] >= EXPIRATION_TIME) {
                cache[i] = null;
            } else {
                empty &= (cache[i] == null);
            }
        }
        if (empty) {
            cache = null;
            timestamps = null;
        } else {
            scheduleCleanerTask(now);
        }
    }
}
