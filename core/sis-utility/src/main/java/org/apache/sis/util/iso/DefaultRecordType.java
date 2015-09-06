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
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InvalidObjectException;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.Type;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.opengis.util.RecordSchema;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.internal.converter.SurjectiveConverter;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An immutable definition of the type of a {@linkplain DefaultRecord record}.
 * A {@code RecordType} is identified by a {@linkplain #getTypeName() type name} and contains an
 * arbitrary amount of {@linkplain #getMembers() members} as (<var>name</var>, <var>type</var>) pairs.
 * A {@code RecordType} may therefore contain another {@code RecordType} as a member.
 *
 * <div class="note"><b>Comparison with Java reflection:</b>
 * {@code RecordType} instances can be though as equivalent to instances of the Java {@link Class} class.
 * The set of members in a {@code RecordType} can be though as equivalent to the set of fields in a class.
 * </div>
 *
 * <div class="section">Instantiation</div>
 * The easiest way to create {@code DefaultRecordType} instances is to use the
 * {@link DefaultRecordSchema#createRecordType(CharSequence, Map)} method.
 * Example:
 *
 * <div class="note">
 * {@preformat java
 *     DefaultRecordSchema schema = new DefaultRecordSchema(null, null, "MySchema");
 *     // The same instance can be reused for all records to create in that schema.
 *
 *     Map<CharSequence,Class<?>> members = new LinkedHashMap<>();
 *     members.put("city",        String .class);
 *     members.put("latitude",    Double .class);
 *     members.put("longitude",   Double .class);
 *     members.put("population",  Integer.class);
 *     RecordType record = schema.createRecordType("MyRecordType", members);
 * }
 * </div>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe if the {@link TypeName}, the {@link RecordSchema}
 * and all ({@link MemberName}, {@link Type}) entries in the map given to the constructor are also immutable.
 * Subclasses shall make sure that any overridden methods remain safe to call from multiple threads and do not change
 * any public {@code RecordType} state.
 *
 * <div class="section">Serialization</div>
 * This class is serializable if all elements given to the constructor are also serializable.
 * Note in particular that {@link DefaultRecordSchema} is currently <strong>not</strong> serializable,
 * so users wanting serialization may need to provide their own schema.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see DefaultRecord
 * @see DefaultRecordSchema
 * @see DefaultMemberName
 */
@XmlType(name = "RecordType")
public class DefaultRecordType extends RecordDefinition implements RecordType, Serializable {
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
     * The type of each members.
     *
     * @see #getMemberTypes()
     */
    private transient Type[] memberTypes;

    /**
     * Creates a new record with the same names and members than the given one.
     *
     * @param other The {@code RecordType} to copy.
     */
    public DefaultRecordType(final RecordType other) {
        typeName    = other.getTypeName();
        container   = other.getContainer();
        memberTypes = computeTransientFields(other.getMemberTypes());
    }

    /**
     * Creates a new record in the given schema.
     * It is caller responsibility to add the new {@code RecordType} in the container
     * {@linkplain RecordSchema#getDescription() description} map, if desired.
     *
     * <p>This constructor is provided mostly for developers who want to create {@code DefaultRecordType}
     * instances in their own {@code RecordSchema} implementation. Otherwise if the default record schema
     * implementation is sufficient, the {@link DefaultRecordSchema#createRecordType(CharSequence, Map)}
     * method provides an easier alternative.</p>
     *
     * @param typeName  The name that identifies this record type.
     * @param container The schema that contains this record type.
     * @param members   The name and type of the members to be included in this record type.
     *
     * @see DefaultRecordSchema#createRecordType(CharSequence, Map)
     */
    public DefaultRecordType(final TypeName typeName, final RecordSchema container,
            final Map<? extends MemberName, ? extends Type> members)
    {
        ArgumentChecks.ensureNonNull("typeName",  typeName);
        ArgumentChecks.ensureNonNull("container", container);
        ArgumentChecks.ensureNonNull("members",   members);
        this.typeName    = typeName;
        this.container   = container;
        this.memberTypes = computeTransientFields(members);
        /*
         * Ensure that the record namespace is equals to the schema name. For example if the schema
         * name is "MyNameSpace", then the record type name can be "MyNameSpace:MyRecordType".
         */
        final LocalName   schemaName   = container.getSchemaName();
        final GenericName fullTypeName = typeName.toFullyQualifiedName();
        if (schemaName.compareTo(typeName.scope().name().tip()) != 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.InconsistentNamespace_2, schemaName, fullTypeName));
        }
        final int size = size();
        for (int i=0; i<size; i++) {
            final MemberName name = getName(i);
            final Type type = this.memberTypes[i];
            if (type == null || name.getAttributeType().compareTo(type.getTypeName()) != 0) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalMemberType_2, name, type));
            }
            if (fullTypeName.compareTo(name.scope().name()) != 0) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.InconsistentNamespace_2,
                        fullTypeName, name.toFullyQualifiedName()));
            }
        }
    }

    /**
     * Creates a new record from member names specified as character sequence.
     * This constructor builds the {@link MemberName} instance itself.
     *
     * @param typeName    The name that identifies this record type.
     * @param container   The schema that contains this record type.
     * @param members     The name of the members to be included in this record type.
     * @param nameFactory The factory to use for instantiating {@link MemberName}.
     */
    DefaultRecordType(final TypeName typeName, final RecordSchema container,
            final Map<? extends CharSequence, ? extends Type> members, final DefaultNameFactory nameFactory)
    {
        this.typeName  = typeName;
        this.container = container;
        final NameSpace namespace = nameFactory.createNameSpace(typeName, null);
        final Map<MemberName,Type> memberTypes = new LinkedHashMap<MemberName,Type>(Containers.hashMapCapacity(members.size()));
        for (final Map.Entry<? extends CharSequence, ? extends Type> entry : members.entrySet()) {
            final Type         type   = entry.getValue();
            final CharSequence name   = entry.getKey();
            final MemberName   member = nameFactory.createMemberName(namespace, name, type.getTypeName());
            if (memberTypes.put(member, type) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedElement_1, member));
            }
        }
        this.memberTypes = computeTransientFields(memberTypes);
    }

    /**
     * Invoked on deserialization for restoring the transient fields.
     * See {@link #writeObject(ObjectOutputStream)} for the stream data description.
     *
     * @param  in The input stream from which to deserialize an object.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final int size = in.readInt();
        final Map<MemberName,Type> members = new LinkedHashMap<MemberName,Type>(Containers.hashMapCapacity(size));
        for (int i=0; i<size; i++) {
            final MemberName member = (MemberName) in.readObject();
            final Type type = (Type) in.readObject();
            if (members.put(member, type) != null) {
                throw new InvalidObjectException(Errors.format(Errors.Keys.DuplicatedElement_1, member));
            }
        }
        memberTypes = computeTransientFields(members);
    }

    /**
     * Invoked on serialization for writing the member names and their type.
     *
     * @serialData The number of members as an {@code int}, followed by a
     *             ({@code MemberName}, {@code Type}) pair for each member.
     *
     * @param  out The output stream where to serialize this object.
     * @throws IOException If an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        final int size = size();
        out.defaultWriteObject();
        out.writeInt(size);
        for (int i=0; i<size; i++) {
            out.writeObject(getName(i));
            out.writeObject(memberTypes[i]);
        }
    }

    /**
     * Returns a SIS implementation with the name and members of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
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
     * Returns {@code this} since {@link RecordDefinition} is the definition of this record type.
     */
    @Override
    final RecordType getRecordType() {
        return this;
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
     * <div class="note"><b>Comparison with Java reflection:</b>
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance,
     * then this method can be think as the equivalent of the Java {@link Class#getName()} method.
     * </div>
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
     * <div class="note"><b>Comparison with Java reflection:</b>
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance, then
     * this method can be though as the related to the Java {@link Class#getFields()} method.
     * </div>
     *
     * @return The dictionary of (<var>name</var>, <var>type</var>) pairs, or an empty map if none.
     */
    @Override
    public Map<MemberName,Type> getMemberTypes() {
        return ObjectConverters.derivedValues(memberIndices(), MemberName.class, new SurjectiveConverter<Integer,Type>() {
            @Override public Class<Integer> getSourceClass() {return Integer.class;}
            @Override public Class<Type>    getTargetClass() {return Type.class;}
            @Override public Type apply(final Integer index) {return getType(index);}
        });
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
        return memberIndices().keySet();
    }

    /**
     * Returns the type at the given index.
     */
    final Type getType(final int index) {
        return memberTypes[index];
    }

    /**
     * Returns the type associated to the given attribute name, or {@code null} if none.
     * This method is functionally equivalent to (omitting the check for null value):
     *
     * {@preformat java
     *     getMemberTypes().get(memberName).getTypeName();
     * }
     *
     * <div class="note"><b>Comparison with Java reflection:</b>
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance, then
     * this method can be though as related to the Java {@link Class#getField(String)} method.
     * </div>
     *
     * @param  memberName The attribute name for which to get the associated type name.
     * @return The associated type name, or {@code null} if none.
     */
    @Override
    public TypeName locate(final MemberName memberName) {
        final Integer index = indexOf(memberName);
        return (index != null) ? getType(index).getTypeName() : null;
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
     * <div class="note"><b>Implementation note:</b>
     * We do not require that {@code record.getRecordType() == this} in order to allow record
     * "sub-types" to define additional fields, in a way similar to Java sub-classing.</div>
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
            return Objects.equals(typeName,    that.typeName)    &&
                   Objects.equals(container,   that.container)   &&
                   Arrays .equals(memberTypes, that.memberTypes) &&
                   memberIndices().equals(that.memberIndices());
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code RecordType}.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(typeName) + 31*(memberIndices().hashCode() + 31*Arrays.hashCode(memberTypes));
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Empty constructor only used by JAXB.
     */
    private DefaultRecordType() {
        typeName  = null;
        container = null;
    }
}
