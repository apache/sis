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

import java.util.EventListener;
import org.apache.sis.util.Arrays;

import static java.util.Arrays.copyOf;


/**
 * Listeners for changes in the Apache SIS system. This listener is used only for rare events,
 * like OSGi module loaded or unloaded. We use this class instead of OSGi listeners in order
 * to keep the SIS library OSGi-independent.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class SystemListener implements EventListener {
    /**
     * The listeners, or {@code null} if none.
     */
    private static SystemListener[] listeners;

    /**
     * Adds the given listener to the list of listeners to notify when a change occurs.
     * This method doesn't check if the given listener is already present in the array,
     * unless assertions are enabled.
     *
     * @param listener The listener to add. Can not be {@code null}.
     */
    public static synchronized void add(final SystemListener listener) {
        assert (listener != null) && !Arrays.contains(listeners, listener);
        SystemListener[] list = listeners;
        if (list == null) {
            list = new SystemListener[1];
        } else {
            list = copyOf(list, list.length + 1);
        }
        list[list.length - 1] = listener;
        listeners = list;
    }

    /**
     * Removes all occurrences (not just the first one) of the given listener.
     * Only one occurrence should exist, but this method check all of them as
     * a paranoiac check.`
     *
     * @param listener The listener to remove.
     *
     * @todo Not yet used. Given that the intend of this method is to remove all listeners
     *       registered by a specific module, a possible approach would be to remove all
     *       listeners associated to that through some module identifier.
     */
    public static synchronized void remove(final SystemListener listener) {
        SystemListener[] list = listeners;
        if (list != null) {
            for (int i=list.length; --i>=0;) {
                if (list[i] == listener) {
                    list = Arrays.remove(list, i, 1);
                }
            }
            listeners = list;
        }
    }

    /**
     * For sub-classes constructors.
     */
    protected SystemListener() {
    }

    /**
     * Invoked when the classpath is likely to have changed.
     * Any classes using {@link java.util.ServiceLoader} are advised to clear their cache.
     */
    protected abstract void classpathChanged();

    /**
     * Notifies all registered listeners that the classpath may have changed.
     */
    public static void fireClasspathChanged() {
        final SystemListener[] list;
        synchronized (SystemListener.class) {
            list = listeners;
        }
        if (list != null) {
            for (int i=0; i<list.length; i++) {
                list[i].classpathChanged();
            }
        }
    }
}
