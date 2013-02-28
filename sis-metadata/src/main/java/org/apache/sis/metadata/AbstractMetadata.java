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
import java.lang.reflect.Modifier;
import net.jcip.annotations.ThreadSafe;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.logging.Logging;


/**
 * Base class for metadata implementations, providing basic operations using Java reflection.
 * Available operations include the {@linkplain #AbstractMetadata(Object) copy constructor},
 * together with {@link #equals(Object)} and {@link #hashCode()} implementations.
 *
 * {@section Requirements for subclasses}
 * Subclasses need to implement the interfaces of some {@linkplain MetadataStandard metadata standard}
 * and return that standard in the {@link #getStandard()} method.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@ThreadSafe
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
     * Returns the class of the given metadata, ignoring SIS private classes
     * like {@link org.apache.sis.metadata.iso.citation.CitationConstant}.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-48">GEOTK-48</a>
     */
    private static Class<?> getClass(final Object metadata) {
        Class<?> type = metadata.getClass();
        while (!Modifier.isPublic(type.getModifiers()) && type.getName().startsWith("org.geotoolkit.metadata.iso.")) { // TODO
            type = type.getSuperclass();
        }
        return type;
    }

    /**
     * Compares this metadata with the specified object for equality. The default
     * implementation uses Java reflection. Subclasses may override this method
     * for better performances, or for comparing "hidden" attributes not specified
     * by the GeoAPI (or other standard) interface.
     *
     * <p>This method performs a <cite>deep</cite> comparison: if this metadata contains
     * other metadata, then the comparison will invoke the {@link Object#equals(Object)}
     * method on those children as well.</p>
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
        if (mode == ComparisonMode.STRICT) {
            if (object == null || getClass(object) != getClass(this)) {
                return false;
            }
        }
        // TODO: There is some code to port here.
        /*
         * DEADLOCK WARNING: A deadlock may occur if the same pair of objects is being compared
         * in an other thread (see http://jira.codehaus.org/browse/GEOT-1777). Ideally we would
         * synchronize on 'this' and 'object' atomically (RFE #4210659). Since we can't in Java
         * a workaround is to always get the locks in the same order. Unfortunately we have no
         * guarantee that the caller didn't looked the object himself. For now the safest approach
         * is to not synchronize at all.
         */
        // TODO: There is some code to port here.
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Performs a {@linkplain ComparisonMode#STRICT strict} comparison of this metadata with
     * the given object.
     *
     * @param object The object to compare with this metadata for equality.
     */
    @Override
    public final boolean equals(final Object object) {
        return equals(object, ComparisonMode.STRICT);
    }

    /**
     * Computes a hash code value for this metadata using Java reflection. The hash code
     * is defined as the sum of hash code values of all non-null properties. This is the
     * same contract than {@link java.util.Set#hashCode()} and ensure that the hash code
     * value is insensitive to the ordering of properties.
     */
    @Override
    public synchronized int hashCode() {
        // TODO: There is some code to port here.
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
