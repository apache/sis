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
import java.util.Objects;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InvalidObjectException;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import org.opengis.util.Type;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.NameSpace;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.opengis.util.RecordSchema;
import org.opengis.util.InternationalString;
import org.apache.sis.pending.jdk.JDK19;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.converter.SurjectiveConverter;
import org.apache.sis.metadata.internal.Resources;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.NameFactory;


/**
 * An immutable definition of the type of a {@linkplain DefaultRecord record}.
 * A {@code RecordType} is identified by a {@linkplain #getTypeName() type name} and contains an
 * arbitrary number of {@linkplain #getMembers() members} (fields) as (<var>name</var>, <var>type</var>) pairs.
 * A {@code RecordType} may therefore contain another {@code RecordType} as a field.
 *
 * <div class="note"><b>Comparison with Java reflection:</b>
 * {@code RecordType} instances can be though as equivalent to instances of the Java {@link Class} class.
 * The set of fields in a {@code RecordType} can be though as equivalent to the set of fields in a class.
 * </div>
 *
 * <div class="warning"><b>Possible future change:</b>
 * This class is derived from ISO 19103:2005. The record attributes and methods have been modified
 * in ISO 19103:2015, then all classes related to records have been fully removed in ISO 19103:2024.
 * The implication for Apache <abbr>SIS</abbr> has not yet been determined.
 * This class may be replaced by a simple {@code FeatureType}.</div>
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus inherently thread-safe if the {@link TypeName}, the {@link RecordSchema}
 * and all ({@link MemberName}, {@link Type}) entries in the map given to the constructor are also immutable.
 * Subclasses shall make sure that any overridden methods remain safe to call from multiple threads and do not change
 * any public {@code RecordType} state.
 *
 * <h2>Serialization</h2>
 * This class is serializable if all elements given to the constructor are also serializable.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @see DefaultRecord
 * @see DefaultMemberName
 *
 * @since 0.3
 */
@XmlType(name = "RecordType")
public class DefaultRecordType extends RecordDefinition implements RecordType, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1534515712654429099L;

    /**
     * The type name for a record having an unknown number of fields.
     * This is used at {@code <gco:RecordType>} unmarshalling time,
     * where the type is not well defined, by assuming one field per line.
     */
    private static final TypeName MULTILINE = DefaultRecordSchema.SIS.createRecordTypeName(
            Resources.formatInternational(Resources.Keys.MultilineRecord));

    /**
     * A type of record instances for holding a single {@link String} value.
     *
     * @since 1.6
     */
    public static final DefaultRecordType SINGLE_STRING;

    /**
     * A type of record instances for holding a single {@link Double} value.
     *
     * @since 1.6
     */
    public static final DefaultRecordType SINGLE_REAL;
    static {
        final InternationalString field = Vocabulary.formatInternational(Vocabulary.Keys.Value);
        SINGLE_STRING = singleton(Resources.Keys.SingleText,   field, String.class);
        SINGLE_REAL   = singleton(Resources.Keys.SingleNumber, field, Double.class);
    }

    /**
     * Creates a new record type of the given name with a single field of the given name.
     *
     * @param  typeName    the record type name as a {@link Resources.Keys} code.
     * @param  fieldName   the name of the singleton record field.
     * @param  valueClass  the expected value type for the singleton field.
     * @return a record type of the given name and field.
     */
    private static DefaultRecordType singleton(final short typeName, final InternationalString fieldName, final Class<?> valueClass) {
        return DefaultRecordSchema.SIS.createRecordType(Resources.formatInternational(typeName), Map.of(fieldName, valueClass));
    }

    /**
     * The name that identifies this record type.
     *
     * @see #getTypeName()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final TypeName typeName;

    /**
     * The schema that contains this record type, or {@code null} if none.
     *
     * @see #getContainer()
     */
    @SuppressWarnings({"serial", "deprecation"})
    private final RecordSchema container;

    /**
     * The type of each fields.
     *
     * @see #getFieldTypes()
     */
    private transient Type[] fieldTypes;

    /**
     * Creates a new record with the same names and fields than the given one.
     *
     * @param other  the {@code RecordType} to copy.
     */
    @SuppressWarnings({"deprecation", "this-escape"})
    public DefaultRecordType(final RecordType other) {
        typeName   = other.getTypeName();
        container  = other.getContainer();
        fieldTypes = computeTransientFields(other.getFieldTypes());
    }

    /**
     * Creates a new record type.
     *
     * @param typeName  the name that identifies this record type.
     * @param fields    the name and type of the fields to be included in this record type.
     *
     * @since 1.5
     */
    public DefaultRecordType(final TypeName typeName, final Map<? extends MemberName, ? extends Type> fields) {
        this(typeName, null, fields);
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
     * @param typeName   the name that identifies this record type.
     * @param container  the schema that contains this record type, or {@code null} if none.
     * @param fields     the name and type of the fields to be included in this record type.
     *
     * @see DefaultRecordSchema#createRecordType(CharSequence, Map)
     */
    @SuppressWarnings({"deprecation", "this-escape"})
    DefaultRecordType(final TypeName typeName, final RecordSchema container, final Map<? extends MemberName, ? extends Type> fields) {
        this.typeName   = Objects.requireNonNull(typeName);
        this.container  = container;
        this.fieldTypes = computeTransientFields(fields);
        /*
         * Ensure that the record namespace is equal to the schema name. For example if the schema
         * name is "MyNameSpace", then the record type name can be "MyNameSpace:MyRecordType".
         */
        final GenericName fullTypeName = typeName.toFullyQualifiedName();
        if (container != null) {
            final LocalName schemaName = container.getSchemaName();
            if (schemaName.compareTo(typeName.scope().name().tip()) != 0) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedNamespace_2, schemaName, fullTypeName));
            }
        }
        final int size = size();
        for (int i=0; i<size; i++) {
            final MemberName name = getName(i);
            final Type type = this.fieldTypes[i];
            if (type == null || name.getAttributeType().compareTo(type.getTypeName()) != 0) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalMemberType_2, name, type));
            }
            if (fullTypeName.compareTo(name.scope().name()) != 0) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnexpectedNamespace_2,
                        fullTypeName, name.toFullyQualifiedName()));
            }
        }
    }

    /**
     * Creates a new record from field names specified as character sequence.
     * This constructor builds the {@link MemberName} instance itself.
     *
     * @param typeName     the name that identifies this record type.
     * @param container    the schema that contains this record type.
     * @param fields       the name of the fields to be included in this record type.
     * @param nameFactory  the factory to use for instantiating {@link MemberName}.
     *
     * @deprecated To be removed after {@link DefaultRecordSchema} has been removed.
     */
    @Deprecated(since = "1.5")
    DefaultRecordType(final TypeName typeName, final RecordSchema container,
            final Map<? extends CharSequence, ? extends Type> fields, final NameFactory nameFactory)
    {
        this.typeName  = typeName;
        this.container = container;
        final NameSpace namespace = nameFactory.createNameSpace(typeName, null);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Map<MemberName,Type> fieldTypes = JDK19.newLinkedHashMap(fields.size());
        for (final Map.Entry<? extends CharSequence, ? extends Type> entry : fields.entrySet()) {
            final Type         type   = entry.getValue();
            final CharSequence name   = entry.getKey();
            final MemberName   member = nameFactory.createMemberName(namespace, name, type.getTypeName());
            if (fieldTypes.put(member, type) != null) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedElement_1, member));
            }
        }
        this.fieldTypes = computeTransientFields(fieldTypes);
    }

    /**
     * Invoked on deserialization for restoring the transient fields.
     * See {@link #writeObject(ObjectOutputStream)} for the stream data description.
     *
     * @param  in  the input stream from which to deserialize an object.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the module path.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final int size = in.readInt();
        final Map<MemberName,Type> fields = JDK19.newLinkedHashMap(size);
        for (int i=0; i<size; i++) {
            final var member = (MemberName) in.readObject();
            final Type type = (Type) in.readObject();
            if (fields.put(member, type) != null) {
                throw new InvalidObjectException(Errors.format(Errors.Keys.DuplicatedElement_1, member));
            }
        }
        fieldTypes = computeTransientFields(fields);
    }

    /**
     * Invoked on serialization for writing the field names and their type.
     *
     * @param  out  the output stream where to serialize this object.
     * @throws IOException if an I/O error occurred while writing.
     *
     * @serialData the number of fields as an {@code int}, followed by a
     *             ({@code MemberName}, {@code Type}) pair for each field.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        final int size = size();
        out.defaultWriteObject();
        out.writeInt(size);
        for (int i=0; i<size; i++) {
            out.writeObject(getName(i));
            out.writeObject(fieldTypes[i]);
        }
    }

    /**
     * Returns a SIS implementation with the name and fields of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of {@code DefaultRecordType},
     *       then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultRecordType} instance is created using the
     *       {@linkplain #DefaultRecordType(RecordType) copy constructor} and returned.
     *       Note that this is a shallow copy operation, since the fields contained
     *       in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  other  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the fields of the given object
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
     * Returns the name that identifies this record type.
     *
     * <div class="note"><b>Comparison with Java reflection:</b>
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance,
     * then this method can be think as the equivalent of the Java {@link Class#getName()} method.
     * </div>
     *
     * @return the name that identifies this record type.
     */
    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    /**
     * Returns the schema that contains this record type.
     *
     * @return the schema that contains this record type, or {@code null} if none.
     *
     * @deprecated The {@code RecordSchema} interface has been removed in the 2015 revision of ISO 19103 standard.
     */
    @Override
    @Deprecated(since="1.0")
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
     * @return the dictionary of (<var>name</var>, <var>type</var>) pairs, or an empty map if none.
     */
    @Override
    public Map<MemberName,Type> getFieldTypes() {
        return ObjectConverters.derivedValues(fieldIndices(), MemberName.class, new SurjectiveConverter<Integer,Type>() {
            @Override public Class<Integer> getSourceClass() {return Integer.class;}
            @Override public Class<Type>    getTargetClass() {return Type.class;}
            @Override public Type apply(final Integer index) {return getType(index);}
        });
    }

    /**
     * Returns the set of attribute names defined in this {@code RecordType}'s dictionary.
     * This method is functionally equivalent to the following code, but slightly more efficient:
     *
     * {@snippet lang="java" :
     *     getFieldTypes().keySet();
     *     }
     *
     * @return the set of field names, or an empty set if none.
     */
    @Override
    public Set<MemberName> getMembers() {
        return fieldIndices().keySet();
    }

    /**
     * Returns the type at the given index.
     */
    final Type getType(final int index) {
        return fieldTypes[index];
    }

    /**
     * Returns the type associated to the given attribute name, or {@code null} if none.
     * This method is functionally equivalent to (omitting the check for null value):
     *
     * {@snippet lang="java" :
     *     getFieldTypes().get(name).getTypeName();
     *     }
     *
     * <h4>Comparison with Java reflection</h4>
     * If we think about this {@code RecordType} as equivalent to a {@code Class} instance, then
     * this method can be though as related to the Java {@link Class#getField(String)} method.
     *
     * @param  fieldName  the attribute name for which to get the associated type name.
     * @return the associated type name, or {@code null} if none.
     */
    @Override
    public TypeName locate(final MemberName fieldName) {
        final Integer index = indexOf(fieldName);
        return (index != null) ? getType(index).getTypeName() : null;
    }

    /**
     * Determines if the given record is compatible with this record type. This method returns {@code true}
     * if the given {@code record} argument is non-null and the following condition is true:
     *
     * {@snippet lang="java" :
     *     Set<MemberName> attributeNames = record.getAttributes().keySet();
     *     boolean isInstance = getMembers().containsAll(attributeNames);
     *     }
     *
     * <h4>Implementation note</h4>
     * We do not require that {@code record.getRecordType() == this} in order to allow record
     * "sub-types" to define additional fields, in a way similar to Java sub-classing.
     *
     * @param  record  the record to test for compatibility with this type, or {@code null}.
     * @return {@code true} if the given record is non-null and compatible with this record type.
     */
    @Override
    public boolean isInstance(final Record record) {
        return (record != null) && getMembers().containsAll(record.getFields().keySet());
    }

    /**
     * Compares the given object with this {@code RecordType} for equality.
     *
     * @param  other  the object to compare with this {@code RecordType}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == getClass()) {
            final var that = (DefaultRecordType) other;
            return Objects.equals(typeName,   that.typeName)   &&
                   Objects.equals(container,  that.container)  &&
                   Arrays .equals(fieldTypes, that.fieldTypes) &&
                   fieldIndices().equals(that.fieldIndices());
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code RecordType}.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(typeName) + 31*(fieldIndices().hashCode() + 31*Arrays.hashCode(fieldTypes));
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
     * Constructs an initially empty type describing one field per line.
     * See {@link #setValue(String)} for a description of the supported XML content.
     */
    @SuppressWarnings("unused")
    private DefaultRecordType() {
        typeName  = MULTILINE;
        container = DefaultRecordSchema.SIS;
    }

    /**
     * Returns the record type value as a string. Current implementation returns the fields with
     * one field per line, but it may change in any future version for adapting to common practice.
     */
    @XmlValue
    @SuppressWarnings("unused")
    private String getValue() {
        switch (size()) {
            case 0:  return null;
            case 1:  return String.valueOf(fieldTypes[0]);
            default: return toString(null, null);
        }
    }

    /**
     * Sets the record type value as a string. Current implementation expects one field per line.
     * A record can be anything, but usages that we have seen so far write a character sequence
     * of what seems <var>key</var>-<var>description</var> pairs. Examples:
     *
     * {@snippet lang="xml" :
     *   <gco:RecordType>
     *     General Meteorological Products: General products include the baseline reflectivity and velocity,
     *     and also algorithmic graphic products spectrum width, vertical integrated liquid, and VAD wind profile.
     *   </gco:RecordType>
     *   }
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-419">SIS-419</a>
     */
    @SuppressWarnings("unused")
    private void setValue(final String value) {
        if (value != null) {
            final var fields = new LinkedHashMap<MemberName, Type>();
            for (CharSequence element : CharSequences.splitOnEOL(value)) {
                final int s = ((String) element).indexOf(':');
                if (s >= 0) {
                    element = element.subSequence(0, CharSequences.skipTrailingWhitespaces(element, 0, s));
                    // TODO: the part after ":" is the description. For now, we have no room for storing it.
                }
                final MemberName m = Names.createMemberName(typeName, element, String.class);
                fields.put(m, DefaultRecordSchema.SIS.toAttributeType(String.class));
            }
            fieldTypes = computeTransientFields(fields);
        }
    }
}
