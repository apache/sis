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
package org.apache.sis.internal.gui;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.property.ReadOnlyObjectProperty;


/**
 * A property for a value that never change.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <T>  the type of value stored in the property.
 *
 * @since 1.1
 * @module
 */
public class ImmutableObjectProperty<T> extends ReadOnlyObjectProperty<T> {
    /**
     * The object value.
     */
    private final T value;

    /**
     * Creates a new property for the given value.
     *
     * @param  value  the value.
     */
    public ImmutableObjectProperty(final T value) {
        this.value = value;
    }

    /**
     * Default to {@code null}.
     *
     * @return the object containing this property, or {@code null} if unspecified.
     */
    @Override
    public Object getBean() {
        return null;
    }

    /**
     * Default to {@code null}.
     *
     * @return the property name, or {@code null} if unspecified.
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * Returns the value specified at construction time.
     *
     * @return the property value.
     */
    @Override
    public T get() {
        return value;
    }

    /**
     * Does nothing because the given listener would never be notified.
     *
     * @param  listener  ignored.
     */
    @Override
    public void addListener(InvalidationListener listener) {
    }

    /**
     * Does nothing because the given listener would never be notified.
     *
     * @param  listener  ignored.
     */
    @Override
    public void addListener(ChangeListener<? super T> listener) {
    }

    /**
     * Does nothing because no listener is registered.
     *
     * @param  listener  ignored.
     */
    @Override
    public void removeListener(InvalidationListener listener) {
    }

    /**
     * Does nothing because no listener is registered.
     *
     * @param  listener  ignored.
     */
    @Override
    public void removeListener(ChangeListener<? super T> listener) {
    }
}
