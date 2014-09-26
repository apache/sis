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
package org.apache.sis.util.resources;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;


/**
 * Base class of {@code Keys} inner classes declaring key constants.
 * This base class provides methods for fetching a key numeric value
 * from its name and conversely.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
class KeyConstants {
    /**
     * The class that defines key constants.
     */
    private final Class<?> keysClass;

    /**
     * The key names. This is usually not needed, but may be created from the {@code Keys}
     * inner class in some occasions.
     *
     * @see #getKeyNames()
     * @see #getKeyName(short)
     */
    private transient String[] keys;

    /**
     * For sub-classes constructors only.
     */
    protected KeyConstants() {
        keysClass = getClass();
    }

    /**
     * Creates a new instance for key constants defined in an independent class.
     */
    KeyConstants(final Class<?> keysClass) {
        this.keysClass = keysClass;
    }

    /**
     * Returns the internal array of key names. <strong>Do not modify the returned array.</strong>
     * This method should usually not be invoked, in order to avoid loading the inner Keys class.
     * The keys names are used only in rare situation, like {@link IndexedResourceBundle#list(Appendable)}
     * or in log records.
     */
    final synchronized String[] getKeyNames() {
        if (keys == null) {
            String[] names;
            int length = 0;
            try {
                final Field[] fields = keysClass.getFields();
                names = new String[fields.length];
                for (final Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType() == Short.TYPE) {
                        final int index = ((Short) field.get(null)) & 0xFFFF;
                        if (index >= length) {
                            length = index + 1;
                            if (length > names.length) {
                                // Usually don't happen, except for incomplete bundles.
                                names = Arrays.copyOf(names, length*2);
                            }
                        }
                        names[index] = field.getName();
                    }
                }
            } catch (IllegalAccessException e) {
                names = CharSequences.EMPTY_ARRAY;
            }
            keys = ArraysExt.resize(names, length);
        }
        return keys;
    }

    /**
     * Returns the name of the key at the given index. If there is no name at that given
     * index, format the index as a decimal number. Those decimal numbers are parsed by
     * our {@link IndexedResourceBundle#handleGetObject(String)} implementation.
     */
    final String getKeyName(final short index) {
        final int i = index & 0xFFFF;
        final String[] keys = getKeyNames();
        if (i < keys.length) {
            final String key = keys[i];
            if (key != null) {
                return key;
            }
        }
        return String.valueOf(index);
    }

    /**
     * Returns the numerical value for the key of the given name.
     */
    final short getKeyValue(final String name) throws NoSuchFieldException, IllegalAccessException {
        return (Short) keysClass.getField(name).get(null);
    }
}
