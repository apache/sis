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
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.util.Type;
import org.opengis.util.RecordType;
import org.opengis.util.MemberName;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.pending.jdk.JDK19;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;


/**
 * Holds a {@code Record} definition in a way more convenient for Apache SIS than
 * what the {@code RecordType} interface provides.
 *
 * <h2>Serialization</h2>
 * This base class is intentionally not serializable, and all private fields are marked as transient for making
 * this decision more visible. This is because the internal details of this class are quite arbitrary, so we do
 * not want to expose them in serialization for compatibility reasons. Furthermore, some information are redundant,
 * so a serialization performed by subclasses may be more compact. Serialization of all necessary data shall be
 * performed by subclasses, and the transient fields shall be reconstructed by a call to
 * {@link #computeTransientFields(Map)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
abstract class RecordDefinition {                                       // Intentionally not Serializable.
    /**
     * {@code RecordDefinition} implementation used as a fallback when the user supplied {@link RecordType}
     * is not an instance of {@link DefaultRecordType}. So this adapter is used only if Apache SIS is mixed
     * with other implementations.
     *
     * <h2>Serialization</h2>
     * This class is serializable if the {@code RecordType} given to the constructor is also serializable.
     */
    static final class Adapter extends RecordDefinition implements Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 3739362257927222288L;

        /**
         * The wrapped record type.
         */
        @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
        private final RecordType recordType;            // This is the only serialized field in this file.

        /**
         * Creates a new adapter for the given record type.
         */
        Adapter(final RecordType recordType) {
            this.recordType = recordType;
            computeTransientFields(recordType.getFieldTypes());
        }

        /**
         * Invoked on deserialization for restoring the transient fields.
         *
         * @param  in  the input stream from which to deserialize an attribute.
         * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
         * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
         */
        private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            computeTransientFields(recordType.getFieldTypes());
        }

        /**
         * Returns the wrapped record type.
         */
        @Override
        RecordType getRecordType() {
            return recordType;
        }
    }

    /**
     * Indices of field names. Created on construction or deserialization,
     * and shall be considered final and unmodifiable after that point.
     *
     * @see #fieldIndices()
     */
    private transient Map<MemberName,Integer> fieldIndices;

    /**
     * Field names. Created on construction or deserialization, and shall be considered
     * final and unmodifiable after that point.
     */
    private transient MemberName[] fieldNames;

    /**
     * Classes of expected values, or {@code null} if there is no restriction. Created on
     * construction or deserialization, and shall be considered final and unmodifiable after that point.
     */
    private transient Class<?>[] valueClasses;

    /**
     * The common parent of all value classes. May be a primitive type.
     */
    private transient Class<?> baseValueClass;

    /**
     * Creates a new instance. Subclasses shall invoke {@link #computeTransientFields(Map)} in their constructor.
     */
    RecordDefinition() {
    }

    /**
     * Returns the record type for which this object is a definition.
     */
    abstract RecordType getRecordType();

    /**
     * Invoked on construction or deserialization for computing the transient fields.
     *
     * @param  fieldTypes  the (<var>name</var>, <var>type</var>) pairs in this record type.
     * @return the values in the given map. This information is not stored in {@code RecordDefinition}
     *         because not needed by this class, but the {@link DefaultRecordType} subclass will store it.
     */
    final Type[] computeTransientFields(final Map<? extends MemberName, ? extends Type> fieldTypes) {
        final int size = fieldTypes.size();
        fieldNames   = new MemberName[size];
        fieldIndices = JDK19.newLinkedHashMap(size);
        final Type[] types = new Type[size];
        int i = 0;
        for (final Map.Entry<? extends MemberName, ? extends Type> entry : fieldTypes.entrySet()) {
            final Type type = entry.getValue();
            if (type instanceof AttributeType) {
                final Class<?> c = ((AttributeType) type).getValueClass();
                if (c != Object.class) {
                    if (valueClasses == null) {
                        valueClasses = new Class<?>[size];
                    }
                    valueClasses[i] = c;
                    baseValueClass = Classes.findCommonClass(baseValueClass, c);
                }
            }
            final MemberName name = entry.getKey();
            fieldNames[i] = name;
            fieldIndices.put(name, i);
            types[i] = type;
            i++;
        }
        fieldIndices = Containers.unmodifiable(fieldIndices);
        baseValueClass = (baseValueClass != null) ? Numbers.wrapperToPrimitive(baseValueClass) : Object.class;
        return types;
    }

    /**
     * Returns the common parent of all value classes. May be a primitive type.
     */
    final Class<?> baseValueClass() {
        return baseValueClass;
    }

    /**
     * Read-only access to the map of field indices.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<MemberName,Integer> fieldIndices() {
        return fieldIndices;
    }

    /**
     * Returns the number of elements in records.
     */
    final int size() {
        // `fieldNames` should not be null, but let be safe.
        return (fieldNames != null) ? fieldNames.length : 0;
    }

    /**
     * Returns the index of the given name, or {@code null} if none.
     */
    final Integer indexOf(final MemberName fieldName) {
        return fieldIndices.get(fieldName);
    }

    /**
     * Returns the name of the field at the given index.
     */
    final MemberName getName(final int index) {
        return fieldNames[index];
    }

    /**
     * Returns the expected class of values in the given column, or {@code null} if unspecified.
     */
    final Class<?> getValueClass(final int index) {
        return (valueClasses != null) ? valueClasses[index] : null;
    }

    /**
     * Returns a string representation of this object.
     * The string representation is for debugging purpose and may change in any future SIS version.
     *
     * @return a string representation of this record type.
     */
    @Override
    public String toString() {
        return toString("RecordType", null);
    }

    /**
     * Returns a string representation of a {@code Record} or {@code RecordType}.
     *
     * @param  head    either {@code "Record"} or {@code "RecordType"} or {@code null}.
     * @param  values  the values as an array, or {@code null} for writing the types instead.
     * @return the string representation.
     */
    final String toString(final String head, final Object values) {
        final StringBuilder buffer = new StringBuilder(250);
        final String lineSeparator = System.lineSeparator();
        final String[] names = new String[size()];
        final String margin;
        int width = 0;
        for (int i=0; i<names.length; i++) {
            width = Math.max(width, (names[i] = fieldNames[i].toString()).length());
        }
        if (head == null) {
            width  = 0;         // Ignore the width computation, but we still need the names in the array.
            margin = "";
        } else {
            buffer.append(head).append("[“").append(getRecordType().getTypeName()).append("”] {").append(lineSeparator);
            margin = "    ";
        }
        for (int i=0; i<names.length; i++) {
            final String name = names[i];
            buffer.append(margin).append(name);
            final Object value = (values != null) ? Array.get(values, i) : fieldNames[i].getAttributeType();
            if (value != null) {
                buffer.append(CharSequences.spaces(width - name.length())).append(" : ").append(value);
            }
            buffer.append(lineSeparator);
        }
        if (head != null) {
            buffer.append('}').append(lineSeparator);
        }
        return buffer.toString();
    }
}
