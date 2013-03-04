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

import java.util.logging.Logger;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.logging.Logging;


/**
 * Base class for metadata implementations, providing basic operations using Java reflection.
 * All {@code AbstractMetadata} instances shall be associated to a {@link MetadataStandard},
 * which is provided by subclasses in the {@link #getStandard()} method. There is typically
 * a large number of {@code AbstractMetadata} subclasses (not necessarily as direct children)
 * for the same standard.
 *
 * <p>This base class reduces the effort required to implement metadata interface by providing
 * {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()} implementations.
 * Those methods are implemented using Java reflection for invoking the getter methods
 * defined by the {@code MetadataStandard}.</p>
 *
 * <p>{@code AbstractMetadata} may be read-only or read/write, at implementation choice.
 * The {@link ModifiableMetadata} subclass provides the basis of most SIS metadata classes
 * having writing capabilities.</p>
 *
 * {@section Synchronization}
 * The methods in this class are not synchronized. Synchronizations may be done by getter and
 * setter methods in subclasses, at implementation choice. We never synchronize the methods that
 * perform deep traversal of the metadata tree (like {@code equals(Object)}, {@code hashCode()}
 * or {@code toString()}) because such synchronizations are deadlock prone. For example if
 * subclasses synchronize their getter methods, then many locks may be acquired in various orders.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
public abstract class AbstractMetadata implements LenientComparable {
    /**
     * The logger for messages related to metadata implementations.
     */
    protected static final Logger LOGGER = Logging.getLogger(AbstractMetadata.class);

    /**
     * Creates an initially empty metadata.
     */
    protected AbstractMetadata() {
    }

    /**
     * Returns the metadata standard implemented by subclasses.
     *
     * @return The metadata standard implemented.
     *
     * @todo This method returns {@link MetadataStandard#ISO_19115} for now,
     *       but will become an abstract method soon.
     */
    public MetadataStandard getStandard() {
        return MetadataStandard.ISO_19115;
    }

    /**
     * Returns the metadata interface implemented by this class. It should be one of the
     * interfaces defined in the {@linkplain #getStandard() metadata standard} implemented
     * by this class.
     *
     * @return The standard interface implemented by this implementation class.
     *
     * @see MetadataStandard#getInterface(Class)
     */
    public Class<?> getInterface() {
        // No need to sychronize, since this method does not depend on property values.
        return getStandard().getInterface(getClass());
    }

    /**
     * Compares this metadata with the specified object for equality. The default
     * implementation uses Java reflection. Subclasses may override this method
     * for better performances, or for comparing "hidden" attributes not specified
     * by the GeoAPI (or other standard) interface.
     *
     * @param  object The object to compare with this metadata.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if the given object is equal to this metadata.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (mode == ComparisonMode.STRICT) {
            if (object.getClass() != getClass()) {
                return false;
            }
        }
        final MetadataStandard standard = getStandard();
        if (mode != ComparisonMode.STRICT) {
            if (!getInterface().isInstance(object)) {
                return false;
            }
        }
        /*
         * DEADLOCK WARNING: A deadlock may occur if the same pair of objects is being compared
         * in an other thread (see http://jira.codehaus.org/browse/GEOT-1777). Ideally we would
         * synchronize on 'this' and 'object' atomically (RFE #4210659). Since we can't in Java
         * a workaround is to always get the locks in the same order. Unfortunately we have no
         * guarantee that the caller didn't looked the object himself. For now the safest approach
         * is to not synchronize at all.
         *
         * Edit: actually, even if we could synchronize the two objects atomically, a deadlock
         *       risk would still exists for the reason documented in this class's javadoc.
         */
        return standard.equals(this, object, mode);
    }

    /**
     * Performs a {@linkplain ComparisonMode#STRICT strict} comparison of this metadata with
     * the given object. This method is implemented as below:
     *
     * {@preformat java
     *     public final boolean equals(final Object object) {
     *         return equals(object, ComparisonMode.STRICT);
     *     }
     * }
     *
     * If a subclass needs to override the behavior of this method, then
     * override {@link #equals(Object, ComparisonMode)} instead.
     *
     * @param  object The object to compare with this metadata for equality.
     * @return {@code true} if the given object is strictly equals to this metadata.
     */
    @Override
    public final boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Computes a hash code value for this metadata using Java reflection. The hash code
     * is defined as the sum of hash code values of all non-empty properties. This is a
     * similar contract than {@link java.util.Set#hashCode()} and ensures that the hash code
     * value is insensitive to the ordering of properties.
     *
     * {@section Performance note}
     * This method does not cache the value because current implementation has no notification
     * mechanism for tracking changes in children properties. If this metadata is known to be
     * immutable, then subclasses may consider caching the hash code value at their choice.
     *
     * @see MetadataStandard#hashCode(Object)
     */
    @Override
    public int hashCode() {
        return getStandard().hashCode(this);
    }
}
