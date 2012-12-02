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
package org.apache.sis.internal.jaxb;

import java.util.UUID;
import org.apache.sis.util.Static;
import org.apache.sis.util.collection.WeakValueHashMap;


/**
 * Weak references to objects associated to a UUID in the current JVM.
 * The objects are typically instances of ISO 19115 (metadata) and ISO 19111 (referencing) types.
 * This class is convenient at XML marshalling and unmarshalling time for handling the {@code uuid}
 * and {@code uuidref} attributes. The {@code uuidref} attribute is used to refer to an XML element
 * that has a corresponding {@code uuid} attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.13)
 * @version 0.3
 * @module
 *
 * @see <a href="https://www.seegrid.csiro.au/wiki/bin/view/AppSchemas/GmlIdentifiers">GML identifiers</a>
 */
public final class UUIDs extends Static {
    /**
     * The objects for which a UUID has been created.
     */
    private static final WeakValueHashMap<UUID,Object> OBJECTS = new WeakValueHashMap<>(UUID.class);

    /**
     * Do not allow instantiation of this class.
     */
    private UUIDs() {
    }

    /**
     * Returns the object associated to the given UUID, or {@code null} if none.
     *
     * @param  uuid The UUID for which to look for an object (can be {@code null}).
     * @return The object associated to the given UUID, or {@code null} if none.
     */
    public static Object lookup(final UUID uuid) {
        return OBJECTS.get(uuid);
    }

    /**
     * Keep a weak references to the given object for the given UUID.
     * If an object is already mapped to the given UUID, then the mapping is <strong>not</strong>
     * modified and the currently mapped object is returned. The returned object may or may not be
     * equals to the object given in argument to this method.
     *
     * @param  uuid   The UUID to associate to the object.
     * @param  object The object to associate to the UUID.
     * @return If an object is already mapped to the given UUID, that object. Otherwise {@code null}.
     */
    public static Object store(final UUID uuid, final Object object) {
        return OBJECTS.putIfAbsent(uuid, object);
    }
}
