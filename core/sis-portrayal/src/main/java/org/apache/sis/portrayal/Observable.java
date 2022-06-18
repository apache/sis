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
package org.apache.sis.portrayal;

import java.util.Map;
import java.util.HashMap;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Parent class of all objects for which it is possible to register listeners.
 * This base class does not need to be public; its methods will appear as if
 * they were defined directly in sub-classes.
 *
 * @todo Consider replacing the use of {@link PropertyChangeListener} by a new listener interface
 *       defined in this package. The reason is because {@link PropertyChangeListener} is defined
 *       in the {@code java.desktop} module, for which the future after 2026 is not clear at this
 *       state. It would also allow to add information not currently provided to current listener,
 *       like the index of the element modified in a list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
abstract class Observable {
    /**
     * The registered listeners for each property, created when first needed.
     *
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     */
    private Map<String,PropertyChangeListener[]> listeners;

    /**
     * Only used by classes in this package.
     */
    Observable() {
    }

    /**
     * Registers a listener for the property of the given name.
     * The listener will be notified every time that the property of the given name got a new value.
     * If the same listener is registered twice for the same property, then it will be notified twice
     * (this method does not perform duplication checks).
     *
     * @param  propertyName  name of the property to listen (should be one of the {@code *_PROPERTY} constants).
     * @param  listener      property listener to register.
     */
    public final void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        ArgumentChecks.ensureNonEmpty("propertyName", propertyName);
        ArgumentChecks.ensureNonNull("listener", listener);
        if (listeners == null) {
            listeners = new HashMap<>(4);       // Assume few properties will be listened.
        }
        listeners.compute(propertyName, (key, oldList) ->
                (oldList != null) ? ArraysExt.append(oldList, listener)
                                  : new PropertyChangeListener[] {listener});
    }

    /**
     * Unregisters a property listener. The given {@code propertyName} should be the name used during
     * {@linkplain #addPropertyChangeListener(String, PropertyChangeListener) listener registration}.
     * If the specified listener is not registered for the named property, then nothing happen.
     * If the listener has been registered twice, then only one registration is removed
     * (one registration will remain).
     *
     * @param  propertyName  name of the listened property.
     * @param  listener      property listener to unregister.
     */
    public final void removePropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        ArgumentChecks.ensureNonEmpty("propertyName", propertyName);
        ArgumentChecks.ensureNonNull("listener", listener);
        if (listeners != null) {
            listeners.computeIfPresent(propertyName, (key, oldList) -> {
                for (int i=oldList.length; --i >= 0;) {
                    if (oldList[i] == listener) {
                        if (oldList.length != 1) {
                            return ArraysExt.remove(oldList, i, 1);
                        } else {
                            return null;
                        }
                    }
                }
                return oldList;
            });
        }
    }

    /**
     * Returns {@code true} if the given property has at least one listener.
     *
     * @param  propertyName  name of the property to test.
     * @return {@code true} if the given property has at least one listener.
     *
     * @since 1.3
     */
    protected final boolean hasPropertyChangeListener(final String propertyName) {
        return (listeners != null) && listeners.containsKey(propertyName);
    }

    /**
     * Notifies all registered listeners that a property of the given name changed its value.
     * The {@linkplain PropertyChangeEvent#getSource() change event source} will be {@code this}.
     * It is caller responsibility to verify that the old and new values are different
     * (this method does not check for equality).
     *
     * @param  propertyName  name of the property that changed its value.
     * @param  oldValue      the old property value (may be {@code null}).
     * @param  newValue      the new property value (may be {@code null}).
     *
     * @see PropertyChangeEvent
     * @see PropertyChangeListener
     */
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        if (listeners != null) {
            final PropertyChangeListener[] list = listeners.get(propertyName);
            if (list != null) {
                final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
                for (final PropertyChangeListener listener : list) {
                    listener.propertyChange(event);
                }
            }
        }
    }

    /**
     * Notifies all registered listeners that a property changed its value.
     * It is caller responsibility to verify that the event source and property name are valid.
     *
     * @param  event  the event to forward. Can not be null.
     *
     * @see PropertyChangeEvent
     * @see PropertyChangeListener
     */
    protected void firePropertyChange(final PropertyChangeEvent event) {
        ArgumentChecks.ensureNonNull("event", event);
        if (listeners != null) {
            final PropertyChangeListener[] list = listeners.get(event.getPropertyName());
            if (list != null) {
                for (final PropertyChangeListener listener : list) {
                    listener.propertyChange(event);
                }
            }
        }
    }
}
