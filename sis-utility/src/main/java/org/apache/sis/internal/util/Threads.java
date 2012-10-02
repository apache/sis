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
package org.apache.sis.internal.util;

import org.apache.sis.util.logging.Logging;


/**
 * Utilities methods for threads. This class declares in a single place every {@link ThreadGroup}
 * used in SIS. Their intend is to bring some order in debugger informations, by grouping the
 * threads created by SIS together under the same parent tree node.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
public final class Threads {
    /**
     * The parent of every threads declared in this class. This parent will be declared as close
     * as possible to the root of all thread groups (i.e. not as an application thread subgroup).
     * The intend is to separate the library thread groups from the user application thread groups.
     */
    private static final ThreadGroup SIS;
    static {
        ThreadGroup parent = Thread.currentThread().getThreadGroup();
        try {
            ThreadGroup candidate;
            while ((candidate = parent.getParent()) != null) {
                parent = candidate;
            }
        } catch (SecurityException e) {
            // If we are not allowed to get the parent, stop there.
            // We went up in the tree as much as we were allowed to.
        }
        SIS = new ThreadGroup(parent, "Apache SIS");
    }

    /**
     * The group of threads for resources disposal. We give them a priority slightly higher
     * than the normal one since this group shall contain only tasks to be completed very
     * quickly, and the benefit of executing those tasks soon is more resources made available.
     */
    static final ThreadGroup RESOURCE_DISPOSERS = new ThreadGroup(SIS, "ResourceDisposers") {
        @Override public void uncaughtException(final Thread thread, final Throwable exception) {
            Logging.severeException(Logging.getLogger("org.apache.sis"), thread.getClass(), "run", exception);
        }
    };
    static {
        RESOURCE_DISPOSERS.setMaxPriority(Thread.NORM_PRIORITY + 2);
    }

    /**
     * Do not allows instantiation of this class.
     */
    private Threads() {
    }
}
