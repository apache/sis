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
import java.util.Iterator;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.opengis.util.Type;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.util.RecordSchema;
import org.opengis.util.RecordType;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.metadata.simple.SimpleAttributeType;
import org.apache.sis.converter.SurjectiveConverter;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Strings;


/**
 * A factory for creating {@code RecordType} instances.
 *
 * <div class="warning"><b>Possible future change:</b>
 * This class is derived from ISO 19103:2005. The record attributes and methods have been modified
 * in ISO 19103:2015, then all classes related to records have been fully removed in ISO 19103:2024.
 * The implication for Apache <abbr>SIS</abbr> has not yet been determined.
 * In the meantime, this class should be considered as merely a factory of {@link DefaultRecordType} instances.</div>
 *
 * <h2>Thread safety</h2>
 * The same {@code DefaultRecordSchema} instance can be safely used by many threads without synchronization
 * on the part of the caller if the {@link NameFactory} given to the constructor is also thread-safe.
 * Subclasses should make sure that any overridden methods remain safe to call from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @see DefaultRecordType
 * @see DefaultRecord
 *
 * @since 0.5
 */
@SuppressWarnings("deprecation")
public final class DefaultRecordSchema implements RecordSchema, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4381124182735566127L;

    /**
     * The schema used for creating records in <abbr>SIS</abbr> namespace.
     */
    static final DefaultRecordSchema SIS = new DefaultRecordSchema(Constants.SIS);

    /**
     * On serialization, returns a singleton instance if possible.
     *
     * @return the object to use after deserialization.
     * @throws ObjectStreamException if the serialized object defines an unknown data type.
     */
    final Object readResolve() throws ObjectStreamException {
        final String schemaName = getSchemaName().toString();
        return Constants.SIS.equals(schemaName) ? SIS : new DefaultRecordSchema(schemaName);
    }

    /**
     * The namespace of {@link RecordType} to be created by this class.
     * This is also (indirectly) the {@linkplain #getSchemaName() schema name}.
     */
    @SuppressWarnings("serial")     // SIS implementations are serializable.
    private final NameSpace namespace;

    /**
     * The record types in the namespace of this schema.
     */
    private final transient Map<TypeName, DefaultRecordType> description;

    /**
     * The pool of attribute types created so far.
     */
    private final transient ConcurrentMap<Class<?>, Type> attributeTypes;

    /**
     * The converter to use for converting Java {@link Class} to ISO 19103 {@link Type}.
     * This converter delegates its work to the {@link #toAttributeType(Class)} method.
     */
    private final transient ObjectConverter<Class<?>,Type> toTypes = new SurjectiveConverter<Class<?>, Type>() {
        @SuppressWarnings("unchecked")
        @Override public Class<Class<?>> getSourceClass() {return (Class) Class.class;}
        @Override public Class<Type>     getTargetClass() {return Type.class;}
        @Override public Type apply(Class<?> valueClass)  {return toAttributeType(valueClass);}
    };

    /**
     * Creates a new schema of the given name.
     *
     * @param schemaName  the name of the new schema.
     *
     * @since 1.6
     */
    public DefaultRecordSchema(final CharSequence schemaName) {
        final DefaultNameFactory nameFactory = DefaultNameFactory.provider();
        this.namespace      = nameFactory.createNameSpace(nameFactory.createLocalName(null, schemaName), null);
        this.description    = new WeakValueHashMap<>(TypeName.class);
        this.attributeTypes = new ConcurrentHashMap<>();
    }

    /**
     * Returns the schema name.
     *
     * @return the schema name.
     */
    @Override
    public LocalName getSchemaName() {
        return namespace.name().tip();
    }

    /**
     * Creates the name of a record.
     *
     * @param  typeName  name of the record type to create.
     * @return name of a record type.
     *
     * @since 1.3
     */
    public TypeName createRecordTypeName(final CharSequence typeName) {
        final DefaultNameFactory nameFactory = DefaultNameFactory.provider();
        return nameFactory.createTypeName(namespace, Objects.requireNonNull(typeName), null);
    }

    /**
     * Creates a new record type of the given name, which will contain the given fields.
     * Fields are declared in iteration order.
     *
     * @param  typeName  name of the record type to create.
     * @param  fields    the name of each record field, together with the expected value types.
     * @return a record type of the given name and fields.
     * @throws IllegalArgumentException if a record already exists for the given name but with different fields.
     */
    public DefaultRecordType createRecordType(final CharSequence typeName, final Map<CharSequence,Class<?>> fields)
            throws IllegalArgumentException
    {
        ArgumentChecks.ensureNonNull("fields", fields);
        final TypeName name = createRecordTypeName(typeName);
        final Map<CharSequence,Type> fieldTypes = ObjectConverters.derivedValues(fields, CharSequence.class, toTypes);
        DefaultRecordType record;
        synchronized (description) {
            record = description.get(name);
            if (record == null) {
                final DefaultNameFactory nameFactory = DefaultNameFactory.provider();
                record = new DefaultRecordType(name, this, fieldTypes, nameFactory);
                description.put(name, record);
                return record;
            }
        }
        /*
         * If a record type already exists for the given name, verify that it contains the same fields.
         */
        final Iterator<Map.Entry<CharSequence,Class<?>>> it1 = fields.entrySet().iterator();
        final Iterator<Map.Entry<MemberName,Type>> it2 = record.getMemberTypes().entrySet().iterator();
        boolean hasNext;
        while ((hasNext = it1.hasNext()) == it2.hasNext()) {
            if (!hasNext) {
                return record;                                          // Finished comparison successfully.
            }
            final Map.Entry<CharSequence,Class<?>> e1 = it1.next();
            final Map.Entry<MemberName,Type> e2 = it2.next();
            if (!e2.getKey().tip().toString().equals(e1.toString())) {
                break;                                                  // Member names differ.
            }
            if (!((SimpleAttributeType) e2.getValue()).getValueClass().equals(e1.getValue())) {
                break;                                                  // Value classes differ.
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.RecordAlreadyDefined_2, getSchemaName(), typeName));
    }

    /**
     * Suggests an attribute type for the given value class. The {@code TypeName} will use the UML identifier
     * of OGC/ISO specification when possible, e.g. {@code "GCO:CharacterString"} for {@code java.lang.String}.
     * See <cite>Mapping Java classes to type names</cite> in {@link DefaultTypeName} javadoc for more information.
     *
     * @param  valueClass  the value class to represent as an attribute type.
     * @return attribute type for the given value class.
     */
    final Type toAttributeType(final Class<?> valueClass) {
        if (!TypeNames.isValid(valueClass)) {
            return null;
        }
        Type type = attributeTypes.get(valueClass);
        if (type == null) {
            if (valueClass == Void.TYPE) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "valueClass", "void"));
            }
            final DefaultNameFactory nameFactory = DefaultNameFactory.provider();
            type = new SimpleAttributeType<>(nameFactory.toTypeName(valueClass), valueClass);
            final Type old = attributeTypes.putIfAbsent(valueClass, type);
            if (old != null) {      // May happen if the type has been computed concurrently.
                return old;
            }
        }
        return type;
    }

    /**
     * Returns the dictionary of all (<var>name</var>, <var>record type</var>) pairs in this schema.
     *
     * @return all (<var>name</var>, <var>record type</var>) pairs in this schema.
     */
    @Override
    public Map<TypeName, RecordType> getDescription() {
        return Collections.unmodifiableMap(description);
    }

    /**
     * Returns the record type for the given name.
     * If the type name is not defined within this schema, then this method returns {@code null}.
     *
     * @param  name  the name of the type to lookup.
     * @return the type for the given name, or {@code null} if none.
     */
    @Override
    public RecordType locate(final TypeName name) {
        return description.get(name);
    }

    /**
     * Compares the given object with this schema for equality.
     *
     * @param  other  the object to compare with this schema.
     * @return {@code true} if both objects are equal.
     * @hidden
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof DefaultRecordSchema) {
            final var that = (DefaultRecordSchema) other;
            return namespace.equals(that.namespace);
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code RecordType}.
     *
     * @hidden
     */
    @Override
    public int hashCode() {
        return namespace.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Returns a string representation of this schema for debugging purpose only.
     *
     * @hidden
     */
    @Override
    public String toString() {
        return Strings.bracket("RecordSchema", getSchemaName());
    }
}
