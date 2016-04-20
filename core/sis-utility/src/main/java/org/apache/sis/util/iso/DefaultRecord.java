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

import java.util.Map;
import java.util.Set;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.Serializable;
import java.lang.reflect.Array;
import org.opengis.util.MemberName;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.AbstractMapEntry;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A list of logically related elements as (<var>name</var>, <var>value</var>) pairs in a dictionary.
 * By definition, all record members have a [1 … 1] cardinality
 * (for a more flexible construct, see {@linkplain org.apache.sis.feature features}).
 * Since all members are expected to be assigned a value, the initial values on {@code DefaultRecord}
 * instantiation are unspecified. Some may be null, or some may be zero.
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code DefaultRecord} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> this class is serializable if the associated {@code RecordType} and all
 *        values are also serializable. Note in particular that {@link DefaultRecordSchema} is currently
 *        <strong>not</strong> serializable, so users wanting serialization may need to define their own
 *        schema implementation.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultRecordType
 * @see DefaultRecordSchema
 */
public class DefaultRecord implements Record, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5293250754663538325L;

    /**
     * The type definition of this record.
     */
    final RecordDefinition definition;

    /**
     * The record values in an array. May be an array of primitive type for compactness,
     * which is why the type is not {@code Object[]}.
     */
    private final Object values;

    /**
     * Creates a new record for the given record type.
     * The initial values are unspecified - they may be null or zero.
     * Callers can assign values by a call to {@link #setAll(Object[])}.
     *
     * @param type The type definition of the new record.
     */
    public DefaultRecord(final RecordType type) {
        ArgumentChecks.ensureNonNull("type", type);
        if (type instanceof RecordDefinition) {
            definition = (RecordDefinition) type;
        } else {
            definition = new RecordDefinition.Adapter(type);
        }
        values = Array.newInstance(definition.baseValueClass(), definition.size());
    }

    /**
     * Returns the type definition of this record.
     *
     * @return The type definition of this record.
     */
    @Override
    public RecordType getRecordType() {
        return definition.getRecordType();
    }

    /**
     * Returns the dictionary of all (<var>name</var>, <var>value</var>) pairs in this record.
     * This method returns a view which will delegate all {@code get} and {@code put} operations to
     * the {@link #locate(MemberName)} and {@link #set(MemberName, Object)} methods respectively.
     *
     * @return The dictionary of all (<var>name</var>, <var>value</var>) pairs in this record.
     *
     * @see RecordType#getMemberTypes()
     */
    @Override
    public Map<MemberName, Object> getAttributes() {
        return new AbstractMap<MemberName, Object>() {
            /** Returns the number of members in the record. */
            @Override
            public int size() {
                return definition.size();
            }

            /** Delegates to {@code DefaultRecord.locate(name)}. */
            @Override
            public Object get(final Object name) {
                return (name instanceof MemberName) ? locate((MemberName) name) : null;
            }

            /** Delegates to {@code DefaultRecord.set(name, value)}. */
            @Override
            public Object put(final MemberName name, final Object value) {
                final Object previous = locate(name);
                set(name, value);
                return previous;
            }

            /** Returns a set containing all (<var>name</var>, <var>value</var>) pairs in the record. */
            @Override
            public Set<Map.Entry<MemberName, Object>> entrySet() {
                return new Entries();
            }
        };
    }

    /**
     * The set of map entries to be returned by {@code DefaultRecord.getAttributes().entrySet()}.
     * {@link AbstractMap} uses this set for providing a default implementation of most methods.
     */
    private final class Entries extends AbstractSet<Map.Entry<MemberName,Object>> {
        /** Returns the number of members in the record. */
        @Override
        public int size() {
            return definition.size();
        }

        /** Returns an iterator over all record members. */
        @Override
        public Iterator<Map.Entry<MemberName, Object>> iterator() {
            return new Iter();
        }
    }

    /**
     * The iterator to be returned by {@code DefaultRecord.getAttributes().entrySet().iterator()}.
     * {@link AbstractMap} (indirectly) and {@link AbstractSet} use this iterator for providing a
     * default implementation of most methods.
     */
    private final class Iter implements Iterator<Map.Entry<MemberName,Object>> {
        /** Index of the next record member to return in the iteration. */
        private int index;

        /** Returns {@code true} if there is more record members to iterate over. */
        @Override
        public boolean hasNext() {
            return index < definition.size();
        }

        /** Returns an entry containing the name and value of the next record member. */
        @Override
        public Map.Entry<MemberName, Object> next() {
            if (hasNext()) {
                return new Entry(index++);
            }
            throw new NoSuchElementException();
        }

        /** Unsupported operation. */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A single entry in the map returned by {@code DefaultRecord.getAttributes()}.
     * Operations on this entry delegate to {@link DefaultRecord#locate(MemberName)}
     * and {@link DefaultRecord#set(MemberName, Object)} methods.
     */
    private final class Entry extends AbstractMapEntry<MemberName,Object> {
        /** Index of the record member represented by this entry. */
        private final int index;

        /** Creates a new entry for the record member at the given index. */
        Entry(final int index) {
            this.index = index;
        }

        /** Returns the name of the record member contained in this entry. */
        @Override
        public MemberName getKey() {
            return definition.getName(index);
        }

        /** Returns the current record member value. */
        @Override
        public Object getValue() {
            return locate(getKey());
        }

        /** Sets the record member value and returns the previous value. */
        @Override
        public Object setValue(final Object value) {
            final MemberName name = getKey();
            final Object previous = locate(name);
            set(name, value);
            return previous;
        }
    }

    /**
     * Returns the value for an attribute of the specified name.
     *
     * @param name The name of the attribute to lookup.
     * @return The value of the attribute for the given name.
     */
    @Override
    public Object locate(final MemberName name) {
        final Integer index = definition.indexOf(name);
        return (index != null) ? Array.get(values, index) : null;
    }

    /**
     * Sets the value for the attribute of the specified name.
     *
     * @param  name  The name of the attribute to modify.
     * @param  value The new value for the attribute.
     * @throws IllegalArgumentException if the given name is not a member of this record.
     * @throws ClassCastException if the given value is not an instance of the expected type for this record.
     */
    @Override
    public void set(final MemberName name, final Object value) {
        final Integer index = definition.indexOf(name);
        if (index == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.PropertyNotFound_2,
                    getRecordType().getTypeName(), name));
        }
        if (value != null) {
            final Class<?> valueClass = definition.getValueClass(index);
            if (valueClass != null && !valueClass.isInstance(value)) {
                throw new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                        name, valueClass, value.getClass()));
            }
        }
        Array.set(values, index, value);
    }

    /**
     * Sets all attribute values in this record, in attribute order.
     *
     * @param  newValues The attribute values.
     * @throws IllegalArgumentException if the given number of values does not match the expected number.
     * @throws ClassCastException if a value is not an instance of the expected type for this record.
     */
    public void setAll(final Object... newValues) {
        ArgumentChecks.ensureNonNull("values", newValues);
        final int length = Array.getLength(values);
        if (newValues.length != length) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.UnexpectedArrayLength_2, length, newValues.length));
        }
        for (int i=0; i<newValues.length; i++) {
            final Object value = newValues[i];
            if (value != null) {
                final Class<?> valueClass = definition.getValueClass(i);
                if (valueClass != null && !valueClass.isInstance(value)) {
                    throw new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                            definition.getName(i), valueClass, value.getClass()));
                }
            }
            Array.set(values, i, value);
        }
    }

    /**
     * Compares this record with the given object for equality.
     *
     * @param  object The object to compare with this record for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true; // Slight optimization for a common case.
        }
        if (object != null && object.getClass() == getClass()) {
            final DefaultRecord that = (DefaultRecord) object;
            return definition.getRecordType().equals(that.definition.getRecordType()) &&
                   Objects.deepEquals(values, that.values);
        }
        return false;
    }

    /**
     * Returns a hash code value for this record.
     *
     * @return A hash code value for this record.
     */
    @Override
    public int hashCode() {
        return Utilities.deepHashCode(values) ^ definition.getRecordType().hashCode();
    }

    /**
     * Returns a string representation of this record.
     * The string representation is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this record.
     */
    @Debug
    @Override
    public String toString() {
        return definition.toString("Record", values);
    }
}
