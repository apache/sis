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
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.Arrays;
import java.util.Collection;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CodeListSet;


/**
 * Performs deep copies of given metadata instances. This class performs a <em>copies</em>, not clones,
 * since the copied metadata may not be instances of the same class than the original metadata.
 * This class performs the following steps:
 *
 * <ul>
 *   <li>Get the {@linkplain MetadataStandard#getImplementation implementation class} of the given metadata instance.</li>
 *   <li>Create a {@linkplain Class#newInstance() new instance} of the implementation class using the public no-argument constructor.</li>
 *   <li>Invoke all non-deprecated setter methods on the new instance with the corresponding value from the given metadata.</li>
 *   <li>If any of the values copied in above step is itself a metadata, recursively performs deep copy on those metadata instances too.</li>
 * </ul>
 *
 * This class supports cyclic graphs in the metadata tree. It may return the given {@code metadata} object directly
 * if the {@linkplain MetadataStandard#getImplementation implementation class} does not provide any setter method.
 *
 * <p>This class is not thread-safe.
 * In multi-threads environment, each thread should use its own {@code MetadataCopier} instance.</p>
 *
 * <div class="note"><b>Recommended alternative:</b>
 * deep metadata copies are sometime useful when using an existing metadata as a template.
 * But the {@link ModifiableMetadata#unmodifiable()} method may provide a better way to use a metadata as a template,
 * as it returns a snapshot and allows the caller to continue to modify the original metadata object and create new
 * snapshots. Example:
 *
 * {@preformat java
 *   // Prepare a Citation to be used as a template.
 *   DefaultCitation citation = new DefaultCitation();
 *   citation.getCitedResponsibleParties(someAuthor);
 *
 *   // Set the title and get a first snapshot.
 *   citation.setTitle(new SimpleInternationalString("A title"));
 *   Citation myFirstCitation = (Citation) citation.unmodifiable();
 *
 *   // Change the title and get another snapshot.
 *   citation.setTitle(new SimpleInternationalString("Another title"));
 *   Citation mySecondCitation = (Citation) citation.unmodifiable();
 * }
 *
 * This approach allows sharing the children that have the same content, thus reducing memory usage. In above example,
 * the {@code someAuthor} {@linkplain org.apache.sis.metadata.iso.citation.DefaultCitation#getCitedResponsibleParties()
 * cited responsible party} is the same instance in both citations. In comparison, deep copy operations unconditionally
 * duplicate everything, no matter if it was needed or not. Nevertheless deep copies are still sometime useful,
 * for example when we do not have the original {@link ModifiableMetadata} instance anymore.
 *
 * <p>{@code MetadataCopier} is also useful for converting a metadata tree of unknown implementations (for example the
 * result of a call to {@link org.apache.sis.metadata.sql.MetadataSource#lookup(Class, String)}) into instances of the
 * public {@link AbstractMetadata} subclasses. But note that shallow copies as provided by the {@code castOrCopy(…)}
 * static methods in each {@code AbstractMetadata} subclass are sometime sufficient.</p>
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see ModifiableMetadata#unmodifiable()
 *
 * @since 0.8
 * @module
*/
public class MetadataCopier {
    /**
     * The default metadata standard to use for object that are not {@link AbstractMetadata} instances,
     * or {@code null} if none.
     */
    private final MetadataStandard standard;

    /**
     * The metadata objects that have been copied so far.
     * This is used for resolving cyclic graphs.
     */
    final Map<Object,Object> copies;

    /**
     * Creates a new metadata copier.
     *
     * @param standard  the default metadata standard to use for object that are not {@link AbstractMetadata} instances,
     *                  or {@code null} if none.
     */
    public MetadataCopier(final MetadataStandard standard) {
        this.standard = standard;
        copies = new IdentityHashMap<>();
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
        try {
            return copyRecursively(null, metadata);
        } finally {
            copies.clear();
        }
    }

    /**
     * Performs a potentially deep copy of the given metadata object.
     * This method is preferred to {@link #copy(Object)} when the type is known.
     *
     * @param  <T>       compile-time value of the {@code type} argument.
     * @param  type      the interface of the metadata object to copy.
     * @param  metadata  the metadata object to copy, or {@code null}.
     * @return a copy of the given metadata object, or {@code null} if the given argument is {@code null}.
     * @throws UnsupportedOperationException if there is no implementation class for a metadata to copy,
     *         or an implementation class does not provide a public default constructor.
     */
    public <T> T copy(final Class<T> type, final T metadata) {
        ArgumentChecks.ensureNonNull("type", type);
        try {
            return type.cast(copyRecursively(type, metadata));
        } finally {
            copies.clear();
        }
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
            MetadataStandard std = standard;
            if (metadata instanceof AbstractMetadata) {
                std = ((AbstractMetadata) metadata).getStandard();
            }
            if (std != null) {
                final PropertyAccessor accessor = std.getAccessor(new CacheKey(metadata.getClass(), type), false);
                if (accessor != null) try {
                    return accessor.copy(metadata, this);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    /*
                     * In our PropertyAccessor.copy(…) implementation, checked exceptions can only be thrown
                     * by the constructor.   Note that Class.newInstance() may throw more checked exceptions
                     * than the ones declared in its method signature,  so we really need to catch Exception
                     * (ReflectiveOperationException is not sufficient).
                     */
                    throw new UnsupportedOperationException(Errors.format(Errors.Keys.CanNotCopy_1, accessor.type), e);
                }
            }
        }
        return metadata;
    }

    /**
     * Verifies if the given metadata value is a map or a collection before to invoke
     * {@link #copyRecursively(Class, Object)} for metadata elements.  This method is
     * invoked by {@link PropertyAccessor#copy(Object, MetadataCopier)}.
     */
    final Object copyAny(final Class<?> type, final Object metadata) {
        if (!type.isInstance(metadata)) {
            if (metadata instanceof Collection<?>) {
                Collection<?> c = (Collection<?>) metadata;
                if (c.isEmpty()) {
                    return null;
                }
                if (c instanceof EnumSet<?> || c instanceof CodeListSet<?>) {
                    /*
                     * Enum and CodeList elements can not be cloned. Do not clone their collection neither;
                     * we presume that the setter method will do that itself.
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
            if (metadata instanceof Map<?,?>) {
                @SuppressWarnings("unchecked")
                final Map<Object,Object> copy = new LinkedHashMap<>((Map) metadata);
                for (final Map.Entry<Object,Object> entry : copy.entrySet()) {
                    entry.setValue(copyRecursively(type, entry.getValue()));
                }
                return copy;
            }
        }
        return copyRecursively(type, metadata);
    }
}
