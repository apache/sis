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
import java.util.Objects;
import java.io.Serializable;
import java.lang.reflect.Array;
import jakarta.xml.bind.annotation.XmlValue;
import org.opengis.util.MemberName;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.AbstractMapEntry;


/**
 * A list of logically related elements as (<var>name</var>, <var>value</var>) pairs in a dictionary.
 * By definition, all record fields have a [1 … 1] multiplicity
 * (for a more flexible construct, see {@linkplain org.apache.sis.feature features}).
 * Since all fields are expected to be assigned a value, the initial values on {@code DefaultRecord}
 * instantiation are unspecified. Some may be null, or some may be zero.
 *
 * <div class="warning"><b>Possible future change:</b>
 * This class is derived from ISO 19103:2005. The record attributes and methods have been modified
 * in ISO 19103:2015, then all classes related to records have been fully removed in ISO 19103:2024.
 * The implication for Apache <abbr>SIS</abbr> has not yet been determined.
 * This class may be replaced by a simple {@code Feature}.</div>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code DefaultRecord} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> this class is serializable only if the associated {@code RecordType}
 *        and all values are also serializable.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.6
 * @since   0.5
 */
public class DefaultRecord implements Record, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5293250754663538325L;

    /**
     * The type definition of this record. Cannot be {@code null}.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    final RecordDefinition definition;

    /**
     * The record values in an array. May be an array of primitive type for compactness,
     * which is why the type is not {@code Object[]}. Should never be {@code null}, except
     * temporarily during XML unmarshalling.
     */
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    private Object values;

    /**
     * Creates a new record for the given record type.
     * The initial values are unspecified - they may be null or zero.
     * Callers can assign values by a call to {@link #setAll(Object[])}.
     *
     * @param type  the type definition of the new record.
     */
    public DefaultRecord(final RecordType type) {
        if (Objects.requireNonNull(type) instanceof RecordDefinition) {
            definition = (RecordDefinition) type;
        } else {
            definition = new RecordDefinition.Adapter(type);
        }
        values = Array.newInstance(definition.baseValueClass(), definition.size());
    }

    /**
     * Creates a new record initialized to a shallow copy of the given record.
     * The fields contained in the given record are <strong>not</strong> recursively copied.
     *
     * @param  record  the record to copy (cannot be null).
     *
     * @since 0.8
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public DefaultRecord(final Record record) {
        this(record.getRecordType());
        if (record instanceof DefaultRecord) {
            final Object source = ((DefaultRecord) record).values;
            System.arraycopy(source, 0, values, 0, Array.getLength(source));
        } else {
            for (final Map.Entry<MemberName,Integer> entry : definition.fieldIndices().entrySet()) {
                final MemberName name = entry.getKey();
                final Object value = record.locate(name);
                if (value != null) {
                    final int index = entry.getValue();
                    final Class<?> valueClass = definition.getValueClass(index);
                    if (valueClass != null && !valueClass.isInstance(value)) {
                        throw new ClassCastException(Errors.format(Errors.Keys.IllegalPropertyValueClass_3,
                                name, valueClass, value.getClass()));
                    }
                    Array.set(values, index, value);
                }
            }
        }
    }

    /**
     * Returns a SIS implementation with the name and fields of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of {@code DefaultRecord},
     *       then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRecord} instance is created using the
     *       {@linkplain #DefaultRecord(Record) copy constructor} and returned.
     *       Note that this is a shallow copy operation, since the fields contained
     *       in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  other The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the fields of the given object
     *         (may be the given object itself), or {@code null} if the argument was {@code null}.
     *
     * @since 0.8
     */
    public static DefaultRecord castOrCopy(final Record other) {
        if (other == null || other instanceof DefaultRecord) {
            return (DefaultRecord) other;
        } else {
            return new DefaultRecord(other);
        }
    }

    /**
     * Returns the type definition of this record.
     *
     * @return the type definition of this record.
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
     * @return the dictionary of all (<var>name</var>, <var>value</var>) pairs in this record.
     *
     * @see RecordType#getFieldTypes()
     */
    @Override
    public Map<MemberName, Object> getFields() {
        if (values == null) {                         // Should never be null, except temporarily at XML unmarshalling time.
            return Map.of();
        }
        return new AbstractMap<MemberName, Object>() {
            /** Returns the number of fields in the record. */
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
        /** Returns the number of fields in the record. */
        @Override
        public int size() {
            return definition.size();
        }

        /** Returns an iterator over all record fields. */
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
        /** Index of the next record field to return in the iteration. */
        private int index;

        /** Returns {@code true} if there is more record fields to iterate over. */
        @Override
        public boolean hasNext() {
            return index < definition.size();
        }

        /** Returns an entry containing the name and value of the next record field. */
        @Override
        public Map.Entry<MemberName, Object> next() {
            if (hasNext()) {
                return new Entry(index++);
            }
            throw new NoSuchElementException();
        }
    }

    /**
     * A single entry in the map returned by {@code DefaultRecord.getAttributes()}.
     * Operations on this entry delegate to {@link DefaultRecord#locate(MemberName)}
     * and {@link DefaultRecord#set(MemberName, Object)} methods.
     */
    private final class Entry extends AbstractMapEntry<MemberName,Object> {
        /** Index of the record field represented by this entry. */
        private final int index;

        /** Creates a new entry for the record field at the given index. */
        Entry(final int index) {
            this.index = index;
        }

        /** Returns the name of the record field contained in this entry. */
        @Override
        public MemberName getKey() {
            return definition.getName(index);
        }

        /** Returns the current record field value. */
        @Override
        public Object getValue() {
            return locate(getKey());
        }

        /** Sets the record field value and returns the previous value. */
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
     * @param  name  the name of the attribute to lookup.
     * @return the value of the attribute for the given name.
     */
    @Override
    public Object locate(final MemberName name) {
        final Integer index = definition.indexOf(name);
        return (index != null) ? Array.get(values, index) : null;
    }

    /**
     * Sets the value for the attribute of the specified name.
     *
     * @param  name   the name of the attribute to modify.
     * @param  value  the new value for the attribute.
     * @throws IllegalArgumentException if the given name is not a field of this record.
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
     * @param  newValues  the attribute values.
     * @throws IllegalArgumentException if the given number of values does not match the expected number.
     * @throws ClassCastException if a value is not an instance of the expected type for this record.
     */
    public void setAll(final Object... newValues) {
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
     * @param  object the object to compare with this record for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;                            // Slight optimization for a common case.
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
     * @return a hash code value for this record.
     */
    @Override
    public int hashCode() {
        return Utilities.deepHashCode(values) ^ definition.getRecordType().hashCode();
    }

    /**
     * Returns a string representation of this record.
     * The string representation is for debugging purpose and may change in any future SIS version.
     *
     * @return a string representation of this record.
     */
    @Override
    public String toString() {
        return definition.toString("Record", values);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Constructs an initially empty record expecting exactly one value as a string.
     * See {@link #setValue(String)} for a description of the supported XML content.
     */
    @SuppressWarnings("unused")
    private DefaultRecord() {
        definition = DefaultRecordType.SINGLE_STRING;
    }

    /**
     * Returns the record value as a string.
     */
    @XmlValue
    @SuppressWarnings("unused")
    private String getValue() {
        if (values != null) {
            switch (Array.getLength(values)) {
                case 0:  break;
                case 1:  return String.valueOf(Array.get(values, 0));
                default: return definition.toString(null, values);
            }
        }
        return null;
    }

    /**
     * Sets the record value as a string. This method is invoked at unmarshalling time.
     * A record can be anything, but usages that we have seen so far write a character
     * sequence or a code list. Examples:
     *
     * {@snippet lang="xml" :
     *   <gco:Record>Alphanumeric values: Product is alphanumeric.</gco:Record>
     *   <gco:Record>Alphanumeric Text: Message contains alphanumeric text.</gco:Record>
     *   <gco:Record>Part A: Reflectivity presented as a tabular listing of alphanumerics.</gco:Record>
     *   <gco:Record>
     *     <gmd:CodeListValue codelist="someURL#DataQualityAssessment" codeListValue="intermediate">intermediate</gmd:CodeListValue>
     *   </gco:Record>
     *   }
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-419">SIS-419</a>
     */
    @SuppressWarnings("unused")
    private void setValue(String value) {
        value = Strings.trimOrNull(value);
        if (value != null) {
            values = new String[] {value};
        }
    }
}
