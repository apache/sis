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
package org.apache.sis.metadata;

import java.util.Set;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.lang.reflect.Constructor;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.internal.metadata.Resources;


/**
 * Performs deep copies of given metadata instances. This class performs <em>copies</em>, not clones,
 * since the copied metadata may not be instances of the same class than the original metadata.
 * This class performs the following steps:
 *
 * <ul>
 *   <li>Get the {@linkplain MetadataStandard#getImplementation implementation class} of the given metadata instance.</li>
 *   <li>Create a {@linkplain Constructor#newInstance new instance} of the implementation class using the public no-argument constructor.</li>
 *   <li>Invoke all non-deprecated setter methods on the new instance with the corresponding value from the given metadata.</li>
 *   <li>If any of the values copied in above step is itself a metadata, recursively performs deep copy on those metadata instances too.</li>
 * </ul>
 *
 * This copier may be used for converting metadata tree of unknown implementations (for example the result of a call to
 * {@link org.apache.sis.metadata.sql.MetadataSource#lookup(Class, String)}) into instances of {@link AbstractMetadata}.
 * The copier may also be used if a {@linkplain ModifiableMetadata.State#EDITABLE modifiable} metadata is desired after
 * the original metadata has been made {@linkplain ModifiableMetadata.State#FINAL final}.
 *
 * <p>Default implementation copies all copiable children, regardless their {@linkplain ModifiableMetadata#state() state}.
 * Static factory methods allow to construct some variants, for example skipping the copy of unmodifiable metadata instances
 * since they can be safely shared.</p>
 *
 * <p>This class supports cyclic graphs in the metadata tree. It may return the given {@code metadata} object directly
 * if the {@linkplain MetadataStandard#getImplementation implementation class} does not provide any setter method.</p>
 *
 * <p>This class is not thread-safe.
 * In multi-threads environment, each thread should use its own {@code MetadataCopier} instance.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
*/
public class MetadataCopier extends MetadataVisitor<Object> {
    /**
     * The default metadata standard to use for object that are not {@link AbstractMetadata} instances,
     * or {@code null} if none.
     */
    private final MetadataStandard standard;

    /**
     * The current metadata instance where to copy the property values.
     */
    private Object target;

    /**
     * Creates a new metadata copier.
     *
     * @param standard  the default metadata standard to use for object that are not {@link AbstractMetadata} instances,
     *                  or {@code null} if none.
     */
    public MetadataCopier(final MetadataStandard standard) {
        this.standard = standard;
    }

    /**
     * Returns the metadata standard to use for the given metadata object, or {@code null} if unknown.
     */
    private MetadataStandard getStandard(final Object metadata) {
        if (metadata instanceof AbstractMetadata) {
            final MetadataStandard std = ((AbstractMetadata) metadata).getStandard();
            if (std != null) return std;
        }
        return standard;
    }

    /**
     * Creates a new metadata copier which avoid copying unmodifiable metadata.
     * More specifically, any {@link ModifiableMetadata} instance in
     * {@linkplain ModifiableMetadata.State#FINAL final state} will be kept <i>as-is</i>;
     * those final metadata will not be copied since they can be safely shared.
     *
     * @param  standard  the default metadata standard to use for object that are not {@link AbstractMetadata} instances,
     *                   or {@code null} if none.
     * @return a metadata copier which skip the copy of unmodifiable metadata.
     *
     * @since 1.0
     */
    public static MetadataCopier forModifiable(final MetadataStandard standard) {
        return new MetadataCopier(standard) {
            @Override protected Object copyRecursively(final Class<?> type, final Object metadata) {
                if (metadata instanceof ModifiableMetadata) {
                    final ModifiableMetadata.State state = ((ModifiableMetadata) metadata).state();
                    if (state != null && state.isUnmodifiable()) {
                        return metadata;
                    }
                }
                return super.copyRecursively(type, metadata);
            }
        };
    }

    /**
     * Performs a potentially deep copy of a metadata object of unknown type.
     * The return value does not need to be of the same class than the argument.
     *
     * @param  metadata  the metadata object to copy, or {@code null}.
     * @return a copy of the given metadata object, or {@code null} if the given argument is {@code null}.
     * @throws UnsupportedOperationException if there is no implementation class for a metadata to copy,
     *         or an implementation class does not provide a public default constructor.
     */
    public Object copy(final Object metadata) {
        return copyRecursively(null, metadata);
    }

    /**
     * Performs a potentially deep copy of the given metadata object.
     * This method is preferred to {@link #copy(Object)} when the type is known.
     * The specified type should be an interface, not an implementation
     * (for example {@link org.opengis.metadata.Metadata}, not
     * {@link org.apache.sis.metadata.iso.DefaultMetadata}).
     *
     * @param  <T>       compile-time value of the {@code type} argument.
     * @param  type      the interface of the metadata object to copy.
     * @param  metadata  the metadata object to copy, or {@code null}.
     * @return a copy of the given metadata object, or {@code null} if the given argument is {@code null}.
     * @throws IllegalArgumentException if {@code type} is an implementation class instead of interface.
     * @throws UnsupportedOperationException if there is no implementation class for a metadata to copy,
     *         or an implementation class does not provide a public default constructor.
     */
    public <T> T copy(final Class<T> type, final T metadata) {
        ArgumentChecks.ensureNonNull("type", type);
        if (metadata instanceof AbstractMetadata) {
            final Class<?> interfaceType = ((AbstractMetadata) metadata).getInterface();
            if (!type.isAssignableFrom(interfaceType)) {
                /*
                 * In case the user specified an implementation despite the documentation warning.
                 * We could replace `type` by `interfaceType` and it would work most of the time,
                 * but we would still have some ClassCastExceptions for example if the given type
                 * is a java.lang.reflect.Proxy. It is probably better to let users know soon that
                 * they should specify an interface.
                 */
                throw new IllegalArgumentException(Resources.format(Resources.Keys.ExpectedInterface_2, interfaceType, type));
            }
        }
        return type.cast(copyRecursively(type, metadata));
    }

    /**
     * Performs the actual copy operation on a single metadata instance.
     * This method is invoked by all public {@code copy(…)} method with the root {@code metadata}
     * object in argument, then is invoked recursively for all properties in that metadata object.
     * If a metadata property is a collection, then this method is invoked for each element in the collection.
     *
     * <p>Subclasses can override this method if they need some control on the copy process.</p>
     *
     * @param  type      the interface of the metadata object to copy, or {@code null} if unspecified.
     * @param  metadata  the metadata object to copy, or {@code null}.
     * @return a copy of the given metadata object, or {@code null} if the given argument is {@code null}.
     * @throws UnsupportedOperationException if there is no implementation class for a metadata to copy,
     *         or an implementation class does not provide a public default constructor.
     */
    protected Object copyRecursively(final Class<?> type, final Object metadata) {
        if (metadata != null) {
            final MetadataStandard std = getStandard(metadata);
            if (std != null) {
                final Object result = walk(std, type, metadata, false);
                if (result != null) {
                    return result;
                }
            }
        }
        return metadata;
    }

    /**
     * Invoked before the properties of a metadata instance are visited. This method creates a new instance,
     * to be returned by {@link #result()}, and returns {@link Filter#WRITABLE_RESULT} for notifying the caller
     * that write operations need to be performed on that {@code result} object.
     */
    @Override
    final Filter preVisit(final PropertyAccessor accessor) {
        if (accessor.isWritable()) try {
            target = accessor.implementation.getConstructor().newInstance();
            return Filter.WRITABLE_RESULT;
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.CanNotCopy_1, accessor.type), Exceptions.unwrap(e));
        } else {
            target = null;
            return Filter.NONE;
        }
    }

    /**
     * Returns the metadata instance resulting from the copy. This method is invoked <strong>before</strong>
     * metadata properties are visited. The returned value is a new, initially empty, metadata instance
     * created by {@link #preVisit(PropertyAccessor)}.
     */
    @Override
    final Object result() {
        return target;
    }

    /**
     * Verifies if the given metadata value is a map or a collection before to invoke
     * {@link #copyRecursively(Class, Object)} for metadata elements.  This method is
     * invoked by {@link PropertyAccessor#walkWritable(MetadataVisitor, Object, Object)}.
     */
    @Override
    final Object visit(final Class<?> type, final Object metadata) {
        if (!type.isInstance(metadata)) {
            if (metadata instanceof Collection<?>) {
                Collection<?> c = (Collection<?>) metadata;
                if (c.isEmpty()) {
                    return null;
                }
                if (c instanceof EnumSet<?> || c instanceof CodeListSet<?>) {
                    /*
                     * Enum and CodeList elements can not be cloned. Do not clone their collection neither;
                     * we presume that the setter method (to be invoked by reflection) will do that itself.
                     */
                } else {
                    final Object[] array = c.toArray();
                    for (int i=0; i < array.length; i++) {
                        array[i] = copyRecursively(type, array[i]);
                    }
                    c = Arrays.asList(array);
                    if (metadata instanceof Set<?>) {
                        c = new LinkedHashSet<>(c);
                    }
                }
                return c;
            }
            /*
             * Maps are rare in GeoAPI interfaces derived from ISO 19115. The main one
             * is `Map<Locale,Charset>` returned by `Metadata.getLocalesAndCharsets()`.
             * We can not copy those entries because the `type` argument is `Map.Entry`,
             * which is not enough information. Recursive copy should not be necessary
             * anyway because we do not use `Map` for storing other metadata objects.
             * We do not clone the map because it should be done by the setter method.
             */
            if (metadata instanceof Map<?,?>) {
                return metadata;
            }
        }
        return copyRecursively(type, metadata);
    }

    /**
     * Returns the path to the currently copied property.
     * Each element in the list is the UML identifier of a property.
     * Element at index 0 is the name of the property of the root metadata object being copied.
     * Element at index 1 is the name of a property which is a children of above property, <i>etc.</i>
     *
     * <p>The returned list is valid only during {@link #copyRecursively(Class, Object)} method execution.
     * The content of this list become undetermined after the {@code copyRecursively} method returned.</p>
     *
     * @return the path to the currently copied property.
     *
     * @since 1.0
     */
    @Override
    protected List<String> getCurrentPropertyPath() {
        return super.getCurrentPropertyPath();
    }
}
