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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import org.opengis.util.Type;
import org.opengis.util.TypeName;
import org.opengis.util.LocalName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.util.RecordType;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.simple.SimpleAttributeType;
import org.apache.sis.internal.converter.SurjectiveConverter;


/**
 * A factory for creating {@code RecordType} and {@code Record} instances.
 * This factory provides the following methods for creating instances:
 *
 * <ul>
 *   <li>{@link #createRecordType(TypeName, Map)}</li>
 * </ul>
 *
 * Subclasses can modify the characteristics of the records to be created
 * by overriding the following methods:
 *
 * <ul>
 *   <li>{@link #toTypeName(Class)}</li>
 * </ul>
 *
 * {@section Thread safety}
 * The same {@code DefaultRecordFactory} instance can be safely used by many threads without synchronization
 * on the part of the caller if the given {@code NameFactory} is also thread-safe. Subclasses should make sure
 * that any overridden methods remain safe to call from multiple threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DefaultRecordFactory {
    /**
     * The factory to use for creating names.
     */
    protected final NameFactory nameFactory;

    /**
     * The pool of attribute types created so far.
     * Shall be used only in synchronized blocks.
     */
    private final Map<Class<?>,Type> attributeTypes;

    /**
     * The record schemas created by this factory.
     */
    private final Map<LocalName,DefaultRecordSchema> schemas;

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
     * Creates a new record factory which will use the {@linkplain DefaultNameFactory default name factory}.
     */
    public DefaultRecordFactory() {
        this(DefaultFactories.NAMES);
    }

    /**
     * Creates a new record factory which will use the given name factory.
     *
     * @param nameFactory The factory to use for creating names.
     */
    public DefaultRecordFactory(final NameFactory nameFactory) {
        ArgumentChecks.ensureNonNull("nameFactory", nameFactory);
        this.nameFactory    = nameFactory;
        this.attributeTypes = new HashMap<>();
        this.schemas        = new HashMap<>();
    }

    /**
     * Returns the schema for the namespace (scope) of the given name.
     *
     * @param  typeName The name of the type for which to get a schema.
     * @return The schema for the namespace (scope) of the given name.
     * @throws IllegalArgumentException if the given name has no namespace or a global namespace.
     */
    private DefaultRecordSchema schemaForScope(final GenericName typeName) throws IllegalArgumentException {
        final NameSpace namespace = typeName.scope();
        if (namespace == null || namespace.isGlobal()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MissingNamespace_1, typeName));
        }
        final LocalName schemaName = namespace.name().tip();
        DefaultRecordSchema schema;
        synchronized (schemas) {
            schema = schemas.get(schemaName);
            if (schema == null) {
                schema = new DefaultRecordSchema(schemaName);
                schemas.put(schemaName, schema);
            }
        }
        return schema;
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
    public RecordType createRecordType(final TypeName typeName, final Map<CharSequence,Class<?>> members)
            throws IllegalArgumentException
    {
        ArgumentChecks.ensureNonNull("typeName", typeName);
        ArgumentChecks.ensureNonNull("members",  members);
        final Map<CharSequence,Type> memberTypes = ObjectConverters.derivedValues(members, CharSequence.class, toTypes);
        final DefaultRecordSchema container = schemaForScope(typeName);
        RecordType record;
        synchronized (container.description) {
            record = container.description.get(typeName);
            if (record == null) {
                record = new DefaultRecordType(typeName, container, memberTypes, nameFactory);
                container.description.put(typeName, record);
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
                break; // Member names differ.
            }
            if (!((SimpleAttributeType) e2.getValue()).getValueClass().equals(e1.getValue())) {
                break; // Value classes differ.
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.RecordAlreadyDefined_2,
                container.getSchemaName(), typeName));
    }

    /**
     * Suggests an attribute type for the given value class. For a short list of known classes,
     * this method returns the ISO 19103 type as used in XML documents. Examples:
     *
     * <table class="sis">
     *   <caption>Attribute types for Java classes (non exhaustive list)</caption>
     *   <tr><th>Java class</th>        <th>Attribute type</th></tr>
     *   <tr><td>{@link String}</td>    <td>{@code gco:CharacterString}</td></tr>
     *   <tr><td>{@link Date}</td>      <td>{@code gco:DateTime}</td></tr>
     *   <tr><td>{@link Double}</td>    <td>{@code gco:Real}</td></tr>
     *   <tr><td>{@link Integer}</td>   <td>{@code gco:Integer}</td></tr>
     *   <tr><td>{@link Boolean}</td>   <td>{@code gco:Boolean}</td></tr>
     * </table>
     *
     * @param  valueClass The value class to represent as an attribute type.
     * @return Attribute type for the given value class.
     */
    private Type toAttributeType(final Class<?> valueClass) {
        Type type;
        synchronized (attributeTypes) {
            type = attributeTypes.get(valueClass);
        }
        if (type == null) {
            type = new SimpleAttributeType<>(toTypeName(valueClass), valueClass);
            synchronized (attributeTypes) {
                final Type old = attributeTypes.put(valueClass, type);
                if (old != null) { // May happen if the type has been computed concurrently.
                    attributeTypes.put(valueClass, old);
                    return old;
                }
            }
        }
        return type;
    }

    /**
     * Suggests a type name for the given value class. For a short list of known classes,
     * this method returns the ISO 19103 type name as used in XML documents. Examples:
     *
     * <table class="sis">
     *   <caption>Type names for Java classes (non exhaustive list)</caption>
     *   <tr><th>Java class</th>        <th>Type name</th></tr>
     *   <tr><td>{@link String}</td>    <td>"{@code gco:CharacterString}"</td></tr>
     *   <tr><td>{@link Date}</td>      <td>"{@code gco:DateTime}"</td></tr>
     *   <tr><td>{@link Double}</td>    <td>"{@code gco:Real}"</td></tr>
     *   <tr><td>{@link Integer}</td>   <td>"{@code gco:Integer}"</td></tr>
     *   <tr><td>{@link Boolean}</td>   <td>"{@code gco:Boolean}"</td></tr>
     * </table>
     *
     * Subclasses can override this method for defining more type names.
     *
     * @param  valueClass The value class for which to get a type name.
     * @return Type name for the given value class.
     */
    protected TypeName toTypeName(final Class<?> valueClass) {
        String ns = "gco";
        final String name;
        if (CharSequence.class.isAssignableFrom(valueClass)) {
            name = "CharacterString";
        } else if (Number.class.isAssignableFrom(valueClass)) {
            name = Numbers.isInteger(valueClass) ? "Integer" : "Real";
        } else if (Date.class.isAssignableFrom(valueClass)) {
            name = "DateTime";
        } else if (valueClass == Boolean.class) {
            name = "Boolean";
        } else {
            ns   = "java";
            name = valueClass.getCanonicalName();
        }
        return nameFactory.createTypeName(nameFactory.createNameSpace(
                nameFactory.createLocalName(null, ns), null), name);
    }
}
