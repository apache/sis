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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import org.opengis.util.Type;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.util.RecordSchema;
import org.opengis.util.RecordType;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.simple.SimpleAttributeType;
import org.apache.sis.internal.converter.SurjectiveConverter;


/**
 * A collection of record types in a given namespace.
 * This class works also as a factory for creating {@code RecordType} and {@code Record} instances.
 * The factory methods are:
 *
 * <ul>
 *   <li>{@link #createRecordType(CharSequence, Map)}</li>
 * </ul>
 *
 * Subclasses can modify the characteristics of the records to be created
 * by overriding the following methods:
 *
 * <ul>
 *   <li>{@link DefaultNameFactory#toTypeName(Class)} if the factory given to the constructor.</li>
 * </ul>
 *
 * <div class="section">Thread safety</div>
 * The same {@code DefaultRecordSchema} instance can be safely used by many threads without synchronization
 * on the part of the caller if the {@link NameFactory} given to the constructor is also thread-safe.
 * Subclasses should make sure that any overridden methods remain safe to call from multiple threads.
 *
 * <div class="section">Limitations</div>
 * This class is currently not serializable because {@code RecordSchema} contain an arbitrary amount of record
 * types in its {@linkplain #getDescription() description} map. Since each {@code RecordType} has a reference
 * to its schema, serializing a single {@code RecordType} could imply serializing all of them.
 * In order to reduce the risk of unexpected behavior, serialization is currently left to subclasses.
 * For example a subclass may define a {@code Object readResolve()} method (as documented in the
 * {@link java.io.Serializable} interface) returning a system-wide static constant for their schema.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultRecordType
 * @see DefaultRecord
 */
public class DefaultRecordSchema implements RecordSchema {
    /**
     * The factory to use for creating names.
     * This is the factory given at construction time.
     *
     * <div class="warning"><b>Upcoming API change</b> — generalization<br>
     * This field type will be changed to the {@link NameFactory} interface when that interface
     * will provide a {@code createMemberName(…)} method (tentatively in GeoAPI 3.1).
     * </div>
     */
    protected final DefaultNameFactory nameFactory;

    /**
     * The namespace of {@link RecordType} to be created by this class.
     * This is also (indirectly) the {@linkplain #getSchemaName() schema name}.
     */
    private final NameSpace namespace;

    /**
     * The record types in the namespace of this schema.
     */
    private final Map<TypeName,RecordType> description;

    /**
     * The pool of attribute types created so far.
     */
    private final ConcurrentMap<Class<?>,Type> attributeTypes;

    /**
     * The converter to use for converting Java {@link Class} to ISO 19103 {@link Type}.
     * This converter delegates its work to the {@link #toAttributeType(Class)} method.
     */
    private final ObjectConverter<Class<?>,Type> toTypes = new SurjectiveConverter<Class<?>, Type>() {
        @SuppressWarnings("unchecked")
        @Override public Class<Class<?>> getSourceClass() {return (Class) Class.class;}
        @Override public Class<Type>     getTargetClass() {return Type.class;}
        @Override public Type apply(Class<?> valueClass)  {return toAttributeType(valueClass);}
    };

    /**
     * Creates a new schema of the given name.
     *
     * <div class="warning"><b>Upcoming API change</b> — generalization<br>
     * This type of the first argument will be changed to the {@link NameFactory} interface when
     * that interface will provide a {@code createMemberName(…)} method (tentatively in GeoAPI 3.1).
     * </div>
     *
     * @param nameFactory The factory to use for creating names, or {@code null} for the default factory.
     * @param parent      The parent namespace, or {@code null} if none.
     * @param schemaName  The name of the new schema.
     */
    public DefaultRecordSchema(DefaultNameFactory nameFactory, final NameSpace parent, final CharSequence schemaName) {
        ArgumentChecks.ensureNonNull("schemaName", schemaName);
        if (nameFactory == null) {
            nameFactory = DefaultFactories.forBuildin(NameFactory.class, DefaultNameFactory.class);
        }
        this.nameFactory    = nameFactory;
        this.namespace      = nameFactory.createNameSpace(nameFactory.createLocalName(parent, schemaName), null);
        this.description    = new WeakValueHashMap<TypeName,RecordType>(TypeName.class);
        this.attributeTypes = new ConcurrentHashMap<Class<?>,Type>();
    }

    /**
     * Returns the schema name.
     *
     * @return The schema name.
     */
    @Override
    public LocalName getSchemaName() {
        return namespace.name().tip();
    }

    /**
     * Creates a new record type of the given name, which will contains the given members.
     * Members are declared in iteration order.
     *
     * @param  typeName The record type name.
     * @param  members  The name of each record member, together with the expected value types.
     * @return A record type of the given name and members.
     * @throws IllegalArgumentException If a record already exists for the given name but with different members.
     */
    public RecordType createRecordType(final CharSequence typeName, final Map<CharSequence,Class<?>> members)
            throws IllegalArgumentException
    {
        ArgumentChecks.ensureNonNull("typeName", typeName);
        ArgumentChecks.ensureNonNull("members",  members);
        final TypeName name = nameFactory.createTypeName(namespace, typeName);
        final Map<CharSequence,Type> memberTypes = ObjectConverters.derivedValues(members, CharSequence.class, toTypes);
        RecordType record;
        synchronized (description) {
            record = description.get(typeName);
            if (record == null) {
                record = new DefaultRecordType(name, this, memberTypes, nameFactory);
                description.put(name, record);
                return record;
            }
        }
        /*
         * If a record type already exists for the given name, verify that it contains the same members.
         */
        final Iterator<Map.Entry<CharSequence,Class<?>>> it1 = members.entrySet().iterator();
        final Iterator<Map.Entry<MemberName,Type>> it2 = record.getMemberTypes().entrySet().iterator();
        boolean hasNext;
        while ((hasNext = it1.hasNext()) == it2.hasNext()) {
            if (!hasNext) {
                return record; // Finished comparison successfully.
            }
            final Map.Entry<CharSequence,Class<?>> e1 = it1.next();
            final Map.Entry<MemberName,Type> e2 = it2.next();
            if (!e2.getKey().tip().toString().equals(e1.toString())) {
                break;      // Member names differ.
            }
            if (!((SimpleAttributeType) e2.getValue()).getValueClass().equals(e1.getValue())) {
                break;      // Value classes differ.
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.RecordAlreadyDefined_2, getSchemaName(), typeName));
    }

    /**
     * Suggests an attribute type for the given value class. The {@code TypeName} will use the UML identifier
     * of OGC/ISO specification when possible, e.g. {@code "GCO:CharacterString"} for {@code java.lang.String}.
     * See <cite>Mapping Java classes to type names</cite> in {@link DefaultTypeName} javadoc for more information.
     *
     * @param  valueClass The value class to represent as an attribute type.
     * @return Attribute type for the given value class.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    final Type toAttributeType(final Class<?> valueClass) {
        if (!TypeNames.isValid(valueClass)) {
            return null;
        }
        Type type = attributeTypes.get(valueClass);
        if (type == null) {
            if (valueClass == Void.TYPE) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "valueClass", "void"));
            }
            final TypeName name = nameFactory.toTypeName(valueClass);
            type = new SimpleAttributeType(name, valueClass);
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
     * @return All (<var>name</var>, <var>record type</var>) pairs in this schema.
     */
    @Override
    public Map<TypeName, RecordType> getDescription() {
        return Collections.unmodifiableMap(description);
    }

    /**
     * Returns the record type for the given name.
     * If the type name is not defined within this schema, then this method returns {@code null}.
     *
     * @param  name The name of the type to lookup.
     * @return The type for the given name, or {@code null} if none.
     */
    @Override
    public RecordType locate(final TypeName name) {
        return description.get(name);
    }

    /**
     * Returns a string representation of this schema for debugging purpose only.
     */
    @Debug
    @Override
    public String toString() {
        return "RecordSchema[“" + getSchemaName() + "”]";
    }
}
