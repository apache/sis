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
package org.apache.sis.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opengis.util.CodeList;

import static org.opengis.util.CodeList.valueOf;


/**
 * Indicates which optional behavior or information can be provided by a {@link Resource}.
 * Capabilities are used as flags returned by the {@link Resource#getCapabilities()} method.
 *
 * A commonly used value is {@link Capability#WRITABLE}, which should be declared by all
 * {@link Resource}s implementing {@code add(…)} and {@code update(…)} methods.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class Capability extends CodeList<Capability> {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -741484716063556569L;

    /**
     * List of all enumerations of this type.
     * Must be declared before any enum declaration.
     */
    private static final List<Capability> VALUES = new ArrayList<>(1);

    /**
     * The resource supports write operations.
     * The methods related to writing such as {@link org.apache.sis.storage.WritableFeatureSet#add(Iterator)}
     * should not throw any {@link UnsupportedOperationException} if this capability is set.
     */
    public static final Capability WRITABLE = new Capability("WRITABLE");

    /**
     * Constructs an element of the given name. The new element is
     * automatically added to the list returned by {@link #values()}.
     *
     * @param  name  the name of the new element.
     *         This name must not be in use by an other element of this type.
     */
    private Capability(final String name) {
        super(name, VALUES);
    }

    /**
     * Returns the list of {@code Capability}s.
     *
     * @return the list of codes declared in the current JVM.
     */
    public static Capability[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(new Capability[VALUES.size()]);
        }
    }

    /**
     * Returns the list of codes of the same kind than this code list element.
     * Invoking this method is equivalent to invoking {@link #values()}, except that
     * this method can be invoked on an instance of the parent {@code CodeList} class.
     *
     * @return all code {@linkplain #values() values} for this code list.
     */
    @Override
    public Capability[] family() {
        return values();
    }

    /**
     * Returns the capability that matches the given string, or returns a
     * new one if none match it. More specifically, this methods returns the first instance for
     * which <code>{@linkplain #name() name()}.{@linkplain String#equals equals}(code)</code>
     * returns {@code true}. If no existing instance is found, then a new one is created for
     * the given name.
     *
     * @param  code  the name of the code to fetch or to create.
     * @return a code matching the given name.
     */
    public static Capability valueOf(String code) {
        return valueOf(Capability.class, code);
    }
}
