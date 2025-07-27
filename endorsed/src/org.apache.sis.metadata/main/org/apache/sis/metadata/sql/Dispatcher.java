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
package org.apache.sis.metadata.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.privy.CollectionsExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.system.Semaphores;
import org.apache.sis.metadata.internal.Dependencies;


/**
 * The handler for metadata proxy that implement (indirectly) metadata interfaces like
 * {@link org.opengis.metadata.Metadata}, {@link org.opengis.metadata.citation.Citation},
 * <i>etc</i>.
 *
 * Any call to a method in a metadata interface is redirected toward the {@link #invoke} method.
 * This method uses reflection in order to find the caller's method and class name. The class
 * name is translated into a table name, and the method name is translated into a column name.
 * Then the information is fetched in the underlying metadata database.
 *
 * <p>There is usually a one-to-one correspondence between invoked methods and the columns to be read, but not always.
 * Some method invocations may actually trig a computation using the values of other columns. This happen for example
 * when invoking a deprecated method which computes its value from non-deprecated methods. Such situations happen in
 * the transition from ISO 19115:2003 to ISO 19115:2014 and may happen again in the future as standards are revised.
 * The algorithms are encoded in implementation classes like the ones in {@link org.apache.sis.metadata.iso} packages,
 * and access to those implementation classes is enabled by the {@link #cache} field (which, consequently, is more than
 * only a cache).</p>
 *
 * <p>Instance of this class shall be thread-safe.</p>
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class Dispatcher implements InvocationHandler {
    /**
     * The identifier used in order to locate the record for this metadata entity in the database.
     * This is usually the primary key in the table which contains this entity.
     */
    final String identifier;

    /**
     * The connection to the database. All metadata handlers created from a single database
     * should share the same source.
     */
    private final MetadataSource source;

    /**
     * Index in the {@code CachedStatement} cache array where to search first. This is only a hint for increasing
     * the chances to find quickly a {@code CachedStatement} instance for the right type and identifier.
     *
     * <h4>Design note</h4>
     * This field is declared in this {@code Dispatcher} class instead of {@link CachedStatement} because we need
     * it before a {@code CachedStatement} instance can be found. Furthermore, two {@code Dispatcher} instances may
     * have different {@code preferredIndex} values even if their {@link CachedStatement#type} value is the same,
     * since their {@link #identifier} values are different.
     */
    int preferredIndex;

    /**
     * The metadata instance where to store the property (column) values, or {@code null} if not yet created.
     * For ISO 19115, this is an instance of one of the classes defined in {@link org.apache.sis.metadata.iso}
     * package or sub-packages. The intent is not only to cache the property values, but also to leverage
     * implementations that compute automatically some property values from other properties.
     * The main usage is computing the value of a deprecated property from the values of non-deprecated ones,
     * e.g. for transition from ISO 19115:2003 to ISO 19115:2014.
     */
    private transient volatile Object cache;

    /**
     * A bitmask of properties having null values. Cached for avoiding to query the database many times.
     * Bit indices are given by {@link LookupInfo#asIndexMap(MetadataStandard)}. If a metadata contains
     * more than 64 properties, no "null value" information will be stored for the extra properties.
     * No damage will happen except more database accesses than needed.
     *
     * <p>We do not need to synchronize this field because it is only an optimization. It is okay if a bit
     * is wrongly zero; the only consequence is that it will cause one more database access than needed.</p>
     */
    private transient long nullValues;

    /**
     * Creates a new metadata handler.
     *
     * @param identifier  the identifier used in order to locate the record for this metadata entity in the database.
     *                    This is usually the primary key in the table which contains this entity.
     * @param source      the connection to the table which contains this entity.
     */
    public Dispatcher(final String identifier, final MetadataSource source) {
        this.identifier = identifier;
        this.source     = source;
        preferredIndex  = -1;
    }

    /**
     * Invoked when any method from a metadata interface is invoked.
     *
     * @param  proxy   the object on which the method is invoked.
     * @param  method  the method invoked.
     * @param  args    the argument given to the method.
     * @return the value to be returned from the public method invoked by the method.
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
        final int n = (args != null) ? args.length : 0;
        switch (method.getName()) {
            case "toString": {
                if (n != 0) break;
                return toString(method.getDeclaringClass());
            }
            case "hashCode": {
                if (n != 0) break;
                return System.identityHashCode(proxy);
            }
            case "equals": {
                if (n != 1) break;
                return proxy == args[0];
            }
            case "identifier": {
                if (n != 1) break;
                return (args[0] == source) ? identifier : null;
            }
            default: {
                if (n != 0) break;
                /*
                 * The invoked method is a method from the metadata interface.
                 * Consequently, the information should exist in the database.
                 * First, we will check the cache. If the value is not present, we will query the database and
                 * fetch the cache again (because the class that implement the cache may perform some computation).
                 */
                Object value;
                try {
                    value = fetchValue(source.getLookupInfo(method.getDeclaringClass()), method);
                } catch (ReflectiveOperationException | SQLException | MetadataStoreException e) {
                    throw new BackingStoreException(error(method), e);
                }
                /*
                 * At this point we got the metadata property value, which may be null.
                 * If the method returns a collection, replace null value by empty set or empty list.
                 */
                if (value == null) {
                    final Class<?> returnType = method.getReturnType();
                    if (Collection.class.isAssignableFrom(returnType)) {
                        value = CollectionsExt.empty(returnType);
                    }
                }
                return value;
            }
        }
        /*
         * Unknown method invoked, or wrong number of arguments.
         */
        throw new BackingStoreException(Errors.format(Errors.Keys.UnsupportedOperation_1,
                    Classes.getShortName(method.getDeclaringClass()) + '.' + method.getName()));
    }

    /**
     * Gets, computes or read from the database a metadata property value.
     * This method returns the first non-null value in the following choices:
     *
     * <ol>
     *   <li>If the property value is present in the {@linkplain #cache}, the cached value.</li>
     *   <li>If the "cache" can compute the value from other property values, the result of that computation.
     *       This case happen mostly for deprecated properties that are replaced by one or more newer properties.</li>
     *   <li>The value stored in the database. The database is queried only once for the requested property
     *       and the result is cached for future reuse.</li>
     * </ol>
     *
     * @param  info    information related to the <em>interface</em> of the metadata object for which a property
     *                 value is requested. This is used for fetching information from the {@link MetadataStandard}.
     * @param  method  the method to be invoked. The class given by {@link Method#getDeclaringClass()} is usually
     *                 the same as the one given by {@link LookupInfo#getMetadataType()}, but not necessarily.
     *                 The two classes may differ if the method is declared only in the implementation class.
     * @return the property value, or {@code null} if none.
     * @throws ReflectiveOperationException if an error occurred while querying the {@link #cache}.
     * @throws SQLException if an error occurred while querying the database.
     * @throws MetadataStoreException if a value was not found or cannot be converted to the expected type.
     */
    private Object fetchValue(final LookupInfo info, final Method method)
            throws ReflectiveOperationException, SQLException, MetadataStoreException
    {
        Object value = null;
        final long nullBit = Numerics.bitmask(info.asIndexMap(source.standard).get(method.getName()));     // Okay even if overflow.
        /*
         * The NULL_COLLECTION semaphore prevents creation of new empty collections by getter methods
         * (a consequence of lazy instantiation). The intent is to avoid creation of unnecessary objects
         * for all unused properties. Users should not see behavioral difference.
         */
        if ((nullValues & nullBit) == 0) {
            final Class<?> type = info.getMetadataType();
            final boolean allowNull = Semaphores.queryAndSet(Semaphores.NULL_COLLECTION);
            try {
                Object cache = this.cache;
                if (cache != null) {
                    synchronized (cache) {
                        value = method.invoke(cache);
                    }
                }
                if (value == null) {
                    info.setMetadataType(type);     // Precaution in case method.invoke(cache) fetched other metadata.
                    value = source.readColumn(info, method, this);
                    if (value != null) {
                        if (cache == null) {
                            final Class<?> impl = source.standard.getImplementation(type);
                            if (impl == null) {
                                return value;
                            }
                            cache = impl.getDeclaredConstructor().newInstance();
                            if (cache instanceof ModifiableMetadata) {
                                ((ModifiableMetadata) cache).transitionTo(ModifiableMetadata.State.COMPLETABLE);
                            }
                            /*
                             * We do not use AtomicReference because it is okay if the cache is instantiated twice.
                             * It would cause us to query the database twice, but we should get the same information.
                             */
                            this.cache = cache;
                        }
                        final Map<String, Object> map = source.standard.asValueMap(cache, type,
                                    KeyNamePolicy.METHOD_NAME, ValueExistencePolicy.ALL);
                        synchronized (cache) {
                            value = map.putIfAbsent(method.getName(), value);
                            if (value == null) {
                                value = method.invoke(cache);
                            }
                        }
                    } else {
                        /*
                         * If we found no explicit value for the requested property, maybe it is a deprecated property
                         * computed from other property values and those other properties have not yet been stored in
                         * the cache object (because that "cache" is also the object computing deprecated properties).
                         */
                        final Class<?> impl = source.standard.getImplementation(type);
                        if (impl != null) {
                            final Dependencies dependencies = impl.getMethod(method.getName()).getAnnotation(Dependencies.class);
                            if (dependencies != null) {
                                boolean hasValue = false;
                                for (final String dep : dependencies.value()) {
                                    info.setMetadataType(type);
                                    hasValue |= (fetchValue(info, impl.getMethod(dep)) != null);
                                }
                                if (hasValue) {
                                    cache = this.cache;             // Created by recursive `invoke(…)` call above.
                                    if (cache != null) {
                                        synchronized (cache) {
                                            value = method.invoke(cache);             // Attempt a new computation.
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                Semaphores.clearIfFalse(Semaphores.NULL_COLLECTION, allowNull);
            }
        }
        if (value == null) {
            nullValues |= nullBit;
        }
        return value;
    }

    /**
     * Returns the error message for a failure to query the database for the property identified by the given method.
     */
    final String error(final Method method) {
        Class<?> returnType = method.getReturnType();
        if (Classes.isParameterizedProperty(returnType) || Collection.class.isAssignableFrom(returnType)) {
            final Class<?> elementType = Classes.boundOfParameterizedProperty(method);
            if (elementType != null) {
                returnType = elementType;
            }
        }
        return Errors.format(Errors.Keys.DatabaseError_2, returnType, identifier);
    }

    /**
     * Returns a string representation of a metadata of the given type.
     */
    private String toString(final Class<?> type) {
        return Classes.getShortName(type) + "[id=“" + identifier + "”]";
    }

    /**
     * Returns a string representation of this handler.
     * This is mostly for debugging purpose.
     */
    @Override
    public String toString() {
        return toString(getClass());
    }
}
