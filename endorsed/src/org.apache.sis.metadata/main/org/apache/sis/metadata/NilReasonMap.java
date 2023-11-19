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
package org.apache.sis.metadata;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.opengis.annotation.Obligation;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.NilObject;


/**
 * A view of nil reasons as a map. Keys are property names and each value is the reason
 * why the value returned by the {@code getFoo()} method (using reflection) is missing.
 * This map contains only the properties that are mandatory.
 *
 * <p>Contrarily to other map views, this map may contain entries associated to null values.
 * It happens when a mandatory property is missing, but nevertheless no reason is provided.
 * So {@code containsValue(null)} can be used for checking if a metadata is invalid.</p>
 *
 * <p>Contrarily to other map views, this map is state-full.
 * Only one instance should be created per metadata object.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see MetadataStandard#asNilReasonMap(Object, Class, KeyNamePolicy)
 */
final class NilReasonMap extends PropertyMap<NilReason> {
    /**
     * The metadata object to wrap.
     */
    private final Object metadata;

    /**
     * The reasons why a mandatory property is missing when that reason cannot be expressed by {@link NilObject}.
     * This is needed for value objects such as {@link Integer}. This map shall always be checked in last resort,
     * after we verified that the {@linkplain #metadata} does not have any value for the corresponding property.
     * If {@link #metadata} changed without updating this {@code reasons} map, the metadata value prevails.
     */
    private final Map<Integer,NilReason> nilReasons;

    /**
     * Creates a map of nil reasons for the specified metadata and accessor.
     *
     * @param metadata   the metadata object to wrap.
     * @param accessor   the accessor to use for the metadata.
     * @param keyPolicy  determines the string representation of keys in the map.
     */
    NilReasonMap(final Object metadata, final PropertyAccessor accessor, final KeyNamePolicy keyPolicy) {
        super(accessor, keyPolicy);
        this.metadata = metadata;
        if (metadata instanceof AbstractMetadata) {
            final var c = (AbstractMetadata) metadata;
            synchronized (c) {
                if (c.nilReasons == null) {
                    c.nilReasons = new HashMap<>(4);
                }
                nilReasons = c.nilReasons;
            }
        } else {
            nilReasons = new HashMap<>(4);
        }
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        final int count = accessor.count();
        for (int i=0; i<count; i++) {
            if (contains(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of entries in this map.
     * Note that some entries may be associated to null values.
     */
    @Override
    public int size() {
        int n = 0;
        final int count = accessor.count();
        for (int i=0; i<count; i++) {
            if (contains(i)) n++;
        }
        return n;
    }

    /**
     * Returns whether this map contains an entry for the property at the given index.
     * The value associated to that entry may be null.
     *
     * @param  index   property index, using the numbering for all properties.
     * @return whether this map contains a value, potentially null, for the specified property.
     */
    @Override
    final boolean contains(final int index) {
        final Object value = accessor.get(index, metadata);
        if (value != null) {
            nilReasons.remove(index);
            return NilReason.forObject(value) != null;
        }
        return nilReasons.containsKey(index) || accessor.obligation(index) == Obligation.MANDATORY;
    }

    /**
     * Returns the nil reason for the property at the specified index.
     */
    @Override
    final NilReason getReflectively(final int index) {
        final Object value = accessor.get(index, metadata);
        if (value != null) {
            nilReasons.remove(index);
            return NilReason.forObject(value);
        }
        return nilReasons.get(index);
    }

    /**
     * Associates the specified nil reason with the specified key in this map.
     * The given value will replace any previous value including non-nil ones,
     * unless the given value is null while the previous value is non-nil.
     * In the latter case, this method does nothing (i.e. the non-nil value is not discarded).
     *
     * <p>Note that the latter exception is not really an exception from the point of view of
     * a {@code NilReasonMap} user, because non-nil values are returned as {@code null} values.
     * Consequently, {@code put(key, null)} behaves as if {@code null} has been stored in this
     * map even if the underlying {@linkplain #metadata} object has a non-nil value.</p>
     *
     * @throws IllegalArgumentException if the given key is not the name of a property in the metadata.
     * @throws ClassCastException if the given value is not of the expected type.
     * @throws UnmodifiableMetadataException if the property for the given key is read-only.
     */
    @Override
    final NilReason setReflectively(final int index, final NilReason value) {
        if (value == null) {
            final Object    oldObject = accessor.get(index, metadata);
            final NilReason oldReason = NilReason.forObject(oldObject);
            if (oldReason == null) {
                NilReason oldStored = nilReasons.remove(index);
                return (oldObject == null) ? oldStored : null;
            }
            accessor.set(index, metadata, null, PropertyAccessor.RETURN_NULL);
            nilReasons.remove(index);   // Shall do only if `set(…)` succeeded.
            return oldReason;
        }
        final Class<?>  type      = accessor.type(index, TypeValuePolicy.PROPERTY_TYPE);
        final Object    nilObject = NilReason.isSupported(type) ? value.createNilObject(type) : null;
        final Object    oldObject = accessor.set(index, metadata, nilObject, PropertyAccessor.RETURN_PREVIOUS);
        final NilReason oldReason = NilReason.forObject(oldObject);
        final NilReason oldStored = (nilObject == null) ? nilReasons.put(index, value) : nilReasons.remove(index);
        return (oldReason != null) ? oldReason : oldStored;
    }

    /**
     * Returns an iterator over the entries contained in this map.
     */
    @Override
    final Iterator<Map.Entry<String,NilReason>> iterator() {
        return new ReflectiveIterator();
    }
}
