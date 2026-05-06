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


/**
 * Base class of {@code Keys} inner classes declaring key constants.
 * This base class provides methods for fetching a key numeric value
 * from its name and conversely.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see IndexedResourceBundle#getKeyConstants()
 */
public abstract class KeyConstants {
    /**
     * The key names in the exact same order as {@link IndexedResourceBundle#values}.
     * This is usually not needed, but may be created from the {@code Keys} inner class in some occasions.
     *
     * @see #getKeyNames()
     * @see #getKeyName(short)
     */
    private transient String[] keys;

    /**
     * For sub-classes constructors only.
     */
    protected KeyConstants() {
    }

    /**
     * Returns the value of the given static field.
     * Subclasses should implement this method as below:
     *
     * {@snippet lang="java" :
     *     return field.get(null);
     *     }
     *
     * This implementation must be provided by subclasses for Java security reason.
     * This is because this {@code KeyConstants} class is not allowed to perform the
     * above call if the module that provides the {@code KeyConstants} subclass does
     * not export the package that contain the subclass.
     *
     * @param  field  the field for which to get the value.
     * @return value of the given field.
     * @throws IllegalAccessException if access to the field is denied.
     */
    protected abstract Object getStaticValue(Field field) throws IllegalAccessException;

    /**
     * Returns the internal array of key names. <strong>Do not modify the returned array.</strong>
     * This method should usually not be invoked, in order to avoid loading the inner Keys class.
     * The keys names are used only in rare situation, like {@link IndexedResourceBundle#list(Appendable)}
     * or in log records.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final synchronized String[] getKeyNames() {
        if (keys == null) {
            String[] names;
            int length = 0;
            try {
                final Field[] fields = getClass().getFields();
                names = new String[fields.length];
                for (final Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) && field.getType() == Short.TYPE) {
                        final int index = Short.toUnsignedInt((Short) getStaticValue(field)) - IndexedResourceBundle.FIRST;
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
                throw new IllegalCallerException(e);
            }
            keys = ArraysExt.resize(names, length);
        }
        return keys;
    }

    /**
     * Returns the name of the key at the given index. If there is no name at that given
     * index, formats the index as a decimal number. Those decimal numbers are parsed by
     * our {@link IndexedResourceBundle#handleGetObject(String)} implementation.
     */
    final String getKeyName(final short index) {
        final int i = Short.toUnsignedInt(index) - IndexedResourceBundle.FIRST;
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final String[] keys = getKeyNames();
        if (i >= 0 && i < keys.length) {
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
        return (Short) getStaticValue(getClass().getField(name));
    }
}
