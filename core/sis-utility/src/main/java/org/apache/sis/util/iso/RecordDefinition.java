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
import java.util.LinkedHashMap;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.Type;
import org.opengis.util.RecordType;
import org.opengis.util.MemberName;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.util.CollectionsExt;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;
import org.apache.sis.internal.simple.SimpleAttributeType;


/**
 * Holds a {@code Record} definition in a way more convenient for Apache SIS than
 * what the {@code RecordType} interface provides.
 *
 * <div class="section">Serialization</div>
 * This base class is intentionally not serializable, and all private fields are marked as transient for making
 * this decision more visible. This is because the internal details of this class are quite arbitrary, so we do
 * not want to expose them in serialization for compatibility reasons. Furthermore some information are redundant,
 * so a serialization performed by subclasses may be more compact. Serialization of all necessary data shall be
 * performed by subclasses, and the transient fields shall be reconstructed by a call to
 * {@link #computeTransientFields(Map)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlTransient
abstract class RecordDefinition { // Intentionally not Serializable.
    /**
     * {@code RecordDefinition} implementation used as a fallback when the user-supplied {@link RecordType}
     * is not an instance of {@link DefaultRecordType}. So this adapter is used only if Apache SIS is mixed
     * with other implementations.
     *
     * <div class="section">Serialization</div>
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
        private final RecordType recordType; // This is the only serialized field in this file.

        /**
         * Creates a new adapter for the given record type.
         */
        Adapter(final RecordType recordType) {
            this.recordType = recordType;
            computeTransientFields(recordType.getMemberTypes());
        }

        /**
         * Invoked on deserialization for restoring the transient fields.
         *
         * @param  in The input stream from which to deserialize an attribute.
         * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
         * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
         */
        private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            computeTransientFields(recordType.getMemberTypes());
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
     * Indices of member names. Created on construction or deserialization,
     * and shall be considered final and unmodifiable after that point.
     *
     * @see #memberIndices()
     */
    private transient Map<MemberName,Integer> memberIndices;

    /**
     * Member names. Created on construction or deserialization, and shall be considered
     * final and unmodifiable after that point.
     */
    private transient MemberName[] members;

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
     * @param  memberTypes The (<var>name</var>, <var>type</var>) pairs in this record type.
     * @return The values in the given map. This information is not stored in {@code RecordDefinition}
     *         because not needed by this class, but the {@link DefaultRecordType} subclass will store it.
     */
    final Type[] computeTransientFields(final Map<? extends MemberName, ? extends Type> memberTypes) {
        final int size = memberTypes.size();
        members       = new MemberName[size];
        memberIndices = new LinkedHashMap<MemberName,Integer>(Containers.hashMapCapacity(size));
        final Type[] types = new Type[size];
        int i = 0;
        for (final Map.Entry<? extends MemberName, ? extends Type> entry : memberTypes.entrySet()) {
            final Type type = entry.getValue();
            if (type instanceof SimpleAttributeType) {
                final Class<?> c = ((SimpleAttributeType) type).getValueClass();
                if (c != Object.class) {
                    if (valueClasses == null) {
                        valueClasses = new Class<?>[size];
                    }
                    valueClasses[i] = c;
                    baseValueClass = Classes.findCommonClass(baseValueClass, c);
                }
            }
            final MemberName name = entry.getKey();
            members[i] = name;
            memberIndices.put(name, i);
            types[i] = type;
            i++;
        }
        memberIndices = CollectionsExt.unmodifiableOrCopy(memberIndices);
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
     * Read-only access to the map of member indices.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Map<MemberName,Integer> memberIndices() {
        return memberIndices;
    }

    /**
     * Returns the number of elements in records.
     */
    final int size() {
        // 'members' should not be null, but let be safe.
        return (members != null) ? members.length : 0;
    }

    /**
     * Returns the index of the given name, or {@code null} if none.
     */
    final Integer indexOf(final MemberName memberName) {
        return memberIndices.get(memberName);
    }

    /**
     * Returns the name of the member at the given index.
     */
    final MemberName getName(final int index) {
        return members[index];
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
     * @return A string representation of this record type.
     */
    @Debug
    @Override
    public String toString() {
        return toString("RecordType", null);
    }

    /**
     * Returns a string representation of a {@code Record} or {@code RecordType}.
     *
     * @param  head   Either {@code "Record"} or {@code "RecordType"}.
     * @param  values The values as an array, or {@code null} for writing the types instead.
     * @return The string representation.
     */
    final String toString(final String head, final Object values) {
        final StringBuilder buffer = new StringBuilder(250);
        final String lineSeparator = JDK7.lineSeparator();
        final String[] names = new String[size()];
        int width = 0;
        buffer.append(head).append("[“").append(getRecordType().getTypeName()).append("”] {").append(lineSeparator);
        for (int i=0; i<names.length; i++) {
            width = Math.max(width, (names[i] = members[i].toString()).length());
        }
        for (int i=0; i<names.length; i++) {
            final String name = names[i];
            buffer.append("    ").append(name);
            final Object value = (values != null) ? Array.get(values, i) : members[i].getAttributeType();
            if (value != null) {
                buffer.append(CharSequences.spaces(width - name.length())).append(" : ").append(value);
            }
            buffer.append(lineSeparator);
        }
        return buffer.append('}').append(lineSeparator).toString();
    }
}
