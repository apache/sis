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

import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.Type;
import org.opengis.util.TypeName;
import org.opengis.util.MemberName;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.opengis.util.RecordSchema;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.CollectionsExt;

// Related to JDK7
import java.util.Objects;


/**
 * An immutable definition of the type of a {@linkplain DefaultRecord record}.
 * A {@code RecordType} is identified by a {@linkplain #getTypeName() type name} and contains an
 * arbitrary amount of {@linkplain #getMembers() members} as (<var>name</var>, <var>type</var>) pairs.
 * A {@code RecordType} may therefore contain another {@code RecordType} as a member.
 *
 * {@section Comparison with Java reflection}
 * {@code RecordType} instances can be though as equivalent to instances of the Java {@link Class} class.
 * The set of members in a {@code RecordType} can be though as equivalent to the set of fields in a class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@Immutable
@XmlType(name = "RecordType")
public class DefaultRecordType implements RecordType, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1534515712654429099L;

    /**
     * The name that identifies this record type.
     *
     * @see #getTypeName()
     */
    private final TypeName typeName;

    /**
     * The schema that contains this record type.
     *
     * @see #getContainer()
     */
    private final RecordSchema container;

    /**
     * The dictionary of (<var>name</var>, <var>type</var>) pairs.
     *
     * @see #getMembers()
     * @see #getMemberTypes()
     */
    private final Map<MemberName,Type> memberTypes;

    /**
     * Empty constructor only used by JAXB.
     */
    private DefaultRecordType() {
        typeName    = null;
        container   = null;
        memberTypes = Collections.emptyMap();
    }

    /**
     * Creates a new record with the same names and members than the given one.
     *
     * @param other The {@code RecordType} to copy.
     */
    public DefaultRecordType(final RecordType other) {
        typeName    = other.getTypeName();
        container   = other.getContainer();
        memberTypes = other.getMemberTypes();
    }

    /**
     * Creates a new record.
     *
     * @param typeName    The name that identifies this record type.
     * @param container   The schema that contains this record type.
     * @param memberTypes The name of the members to be included in this record type.
     */
    public DefaultRecordType(final TypeName typeName, final RecordSchema container, Map<MemberName,Type> memberTypes) {
        ArgumentChecks.ensureNonNull("typeName",    typeName);
        ArgumentChecks.ensureNonNull("container",   container);
        ArgumentChecks.ensureNonNull("memberTypes", memberTypes);
        memberTypes = new LinkedHashMap<>(memberTypes);
        memberTypes.remove(null);
        for (final Map.Entry<MemberName,Type> entry : memberTypes.entrySet()) {
            final MemberName name = entry.getKey();
            final Type type = entry.getValue();
            if (type == null || !name.getAttributeType().equals(type.getTypeName())) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalMemberType_2, name, type));
            }
        }
        this.typeName    = typeName;
        this.container   = container;
        this.memberTypes = CollectionsExt.unmodifiableOrCopy(memberTypes);
    }

    /**
     * Returns a SIS implementation with the name and members of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of {@code DefaultRecordType},
     *       then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRecordType} instance is created using the
     *       {@linkplain #DefaultRecordType(RecordType) copy constructor} and returned.
     *       Note that this is a shallow copy operation, since the members contained
     *       in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  other The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the members of the given object
     *         (may be the given object itself), or {@code null} if the argument was {@code null}.
     */
    public static DefaultRecordType castOrCopy(final RecordType other) {
        if (other == null || other instanceof DefaultRecordType) {
            return (DefaultRecordType) other;
        } else {
            return new DefaultRecordType(other);
        }
    }

    /**
     * Returns the name that identifies this record type. If this {@code RecordType} is contained in a
     * {@linkplain DefaultRecordSchema record schema}, then the record type name shall be valid in the
     * {@linkplain DefaultNameSpace name space} of the record schema:
     *
     * {@preformat java
     *     NameSpace namespace = getContainer().getSchemaName().scope()
     * }
     *
     * {@section Comparison with Java reflection}
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance,
     * then this method can be think as the equivalent of the Java {@link Class#getName()} method.
     *
     * @return The name that identifies this record type.
     */
    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    /**
     * Returns the schema that contains this record type.
     *
     * @return The schema that contains this record type.
     */
    @Override
    public RecordSchema getContainer() {
        return container;
    }

    /**
     * Returns the dictionary of all (<var>name</var>, <var>type</var>) pairs in this record type.
     * The returned map is unmodifiable.
     *
     * {@section Comparison with Java reflection}
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance, then
     * this method can be though as the related to the Java {@link Class#getFields()} method.
     *
     * @return The dictionary of (<var>name</var>, <var>type</var>) pairs, or an empty map if none.
     */
    @Override
    public Map<MemberName, Type> getMemberTypes() {
        return memberTypes;
    }

    /**
     * Returns the set of attribute names defined in this {@code RecordType}'s dictionary.
     * This method is functionally equivalent to:
     *
     * {@preformat java
     *     getMemberTypes().keySet();
     * }
     *
     * @return The set of attribute names, or an empty set if none.
     */
    @Override
    public Set<MemberName> getMembers() {
        return memberTypes.keySet();
    }

    /**
     * Returns the type associated to the given attribute name, or {@code null} if none.
     * This method is functionally equivalent to:
     *
     * {@preformat java
     *     getMemberTypes().get(name);
     * }
     *
     * {@section Comparison with Java reflection}
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance, then
     * this method can be though as related to the Java {@link Class#getField(String)} method.
     *
     * @param  memberName The attribute name for which to get the associated type name.
     * @return The associated type name, or {@code null} if none.
     */
    @Override
    public TypeName locate(final MemberName memberName) {
        final Type type = memberTypes.get(memberName);
        return (type != null) ? type.getTypeName() : null;
    }

    /**
     * Determines if the given record is compatible with this record type. This method returns {@code true}
     * if the given {@code record} argument is non-null and the following condition holds:
     *
     * {@preformat java
     *     Set<MemberName> attributeNames = record.getAttributes().keySet();
     *     boolean isInstance = getMembers().containsAll(attributeNames);
     * }
     *
     * {@note We do not require that {@code record.getRecordType() == this} in order to allow record
     *        "sub-types" to define additional fields, in a way similar to Java sub-classing.}
     *
     * @param  record The record to test for compatibility.
     * @return {@code true} if the given record is compatible with this {@code RecordType}.
     */
    @Override
    public boolean isInstance(final Record record) {
        return (record != null) && getMembers().containsAll(record.getAttributes().keySet());
    }

    /**
     * Compares the given object with this {@code RecordType} for equality.
     *
     * @param  other The object to compare with this {@code RecordType}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == getClass()) {
            final DefaultRecordType that = (DefaultRecordType) other;
            return Objects.equals(typeName,    that.typeName)  &&
                   Objects.equals(container,   that.container) &&
                   Objects.equals(memberTypes, that.memberTypes);
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code RecordType}.
     */
    @Override
    public int hashCode() {
        int code = memberTypes.hashCode();
        if (typeName  != null) code = 31*code + typeName .hashCode();
        if (container != null) code = 31*code + container.hashCode();
        return code;
    }

    /**
     * Returns a string representation of this {@code RecordType}.
     * The string representation is for debugging purpose and may change in any future SIS version.
     */
    @Debug
    @Override
    public String toString() {
        return "RecordType[\"" + typeName + "\"]";
    }
}
