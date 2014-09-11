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
package org.apache.sis.util.iso;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;
import org.opengis.util.NameFactory;
import org.apache.sis.util.Numbers;


/**
 * Implements the mapping between {@link Class} and {@link TypeName} documented in {@link DefaultTypeName}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class TypeNames {
    /**
     * The "OGC" namespace.
     */
    private final NameSpace ogcNS;

    /**
     * The "class" namespace.
     */
    private final NameSpace classNS;

    /**
     * Creates a new factory of type names.
     */
    TypeNames(final NameFactory factory) {
        final Map<String,String> properties = new HashMap<>(4);
        properties.put(DefaultNameFactory.SEPARATOR_KEY, ".");
        properties.put(DefaultNameFactory.HEAD_SEPARATOR_KEY, DefaultNameSpace.DEFAULT_SEPARATOR_STRING);
        ogcNS   = factory.createNameSpace(factory.createLocalName(null, "OGC"),   properties);
        classNS = factory.createNameSpace(factory.createLocalName(null, "class"), properties);
    }

    /**
     * Infers the type name from the given class.
     *
     * @param  valueClass The value class for which to get a type name.
     */
    final TypeName toTypeName(final NameFactory factory, final Class<?> valueClass) {
        String name;
        NameSpace ns = ogcNS;
        if (CharSequence.class.isAssignableFrom(valueClass)) {
            name = "CharacterString";
        } else if (Number.class.isAssignableFrom(valueClass)) {
            name = Numbers.isInteger(valueClass) ? "Integer" : "Real";
        } else if (Date.class.isAssignableFrom(valueClass)) {
            name = "DateTime";
        } else if (valueClass == Boolean.class) {
            name = "Boolean";
        } else {
            name = Types.getStandardName(valueClass);
            if (name == null) {
                ns = classNS;
                name = valueClass.getCanonicalName();
                final int s = name.lastIndexOf('.');
                if (s >= 0) {
                    ns = factory.createNameSpace(factory.parseGenericName(ns, name.substring(0, s)),
                            Collections.singletonMap(DefaultNameFactory.SEPARATOR_KEY, "."));
                    name = name.substring(s+1);
                }
            }
        }
        return factory.createTypeName(ns, name);
    }
}
