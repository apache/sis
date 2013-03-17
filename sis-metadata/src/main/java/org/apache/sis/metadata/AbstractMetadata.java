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

import java.util.Map;
import java.util.logging.Logger;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.logging.Logging;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * Provides basic operations using Java reflection for metadata implementations.
 * All {@code AbstractMetadata} instances shall be associated to a {@link MetadataStandard}.
 * The metadata standard is given by the {@link #getStandard()} method and is typically a
 * constant fixed by the subclass.
 *
 * <p>There is a large number of {@code AbstractMetadata} subclasses (not necessarily as direct children)
 * for the same standard, where each subclass implement one Java interface defined by the metadata standard.
 * This base class reduces the effort required to implement those metadata interfaces by providing
 * {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()} implementations.
 * Those methods are implemented using Java reflection for invoking the getter methods
 * defined by the {@code MetadataStandard}.</p>
 *
 * {@note This class does not synchronize the methods that perform deep traversal of the metadata tree
 * (like <code>equals(Object)</code>, <code>hashCode()</code> or <code>toString()</code>) because such
 * synchronizations are deadlock prone. For example if subclasses synchronize their getter methods,
 * then many locks may be acquired in various orders.}
 *
 * {@code AbstractMetadata} subclasses may be read-only or read/write, at implementation choice.
 * The methods that modify the metadata may throw {@link UnmodifiableMetadataException} if the
 * metadata does not support the operation. Those methods are:
 *
 * <ul>
 *   <li>{@link #prune()}</li>
 *   <li>{@link #shallowCopy(Object)}</li>
 *   <li>{@link #asMap()} with {@code put} operations</li>
 * </ul>
 *
 * Read-only operations operating on metadata values are:
 *
 * <ul>
 *   <li>{@link #isEmpty()}</li>
 *   <li>{@link #asMap()} with {@code get} operations</li>
 *   <li>{@link #asTreeTable()}</li>
 *   <li>{@link #equals(Object, ComparisonMode)}</li>
 * </ul>
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
     * A view of this metadata as a map. Will be created only when first needed.
     *
     * @see #asMap()
     */
    private transient Map<String,Object> asMap;

    /**
     * Creates an initially empty metadata.
     */
    protected AbstractMetadata() {
    }

    /**
     * Returns the metadata standard implemented by subclasses.
     * Subclasses will typically return a hard-coded constant such as
     * {@link MetadataStandard#ISO_19115}.
     *
     * @return The metadata standard implemented.
     */
    public abstract MetadataStandard getStandard();

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
     * Returns {@code true} if this metadata contains only {@code null} or empty properties.
     * A property is considered empty in any of the following cases:
     *
     * <ul>
     *   <li>An empty {@linkplain CharSequence character sequences}.</li>
     *   <li>An {@linkplain java.util.Collection#isEmpty() empty collection} or an empty array.</li>
     *   <li>A collection or array containing only {@code null} or empty elements.</li>
     *   <li>An other metadata object containing only {@code null} or empty attributes.</li>
     * </ul>
     *
     * Note that empty properties can be removed by calling the {@link ModifiableMetadata#prune()}
     * method.
     *
     * {@section Note for implementors}
     * The default implementation uses Java reflection indirectly, by iterating over all entries
     * returned by {@link MetadataStandard#asMap(Object, KeyNamePolicy, ValueExistencePolicy)}.
     * Subclasses that override this method should usually not invoke {@code super.isEmpty()},
     * because the Java reflection will discover and process the properties defined in the
     * subclasses - which is usually not the intend when overriding a method.
     *
     * @return {@code true} if this metadata is empty.
     *
     * @see org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox#isEmpty()
     */
    public boolean isEmpty() {
        return Pruner.isEmpty(this, false);
    }

    /**
     * Removes all references to empty properties. The default implementation iterates over all
     * {@linkplain ValueExistencePolicy#NON_NULL non null} properties, and sets to {@code null}
     * the properties for which {@link #isEmpty()} returned {@code true}.
     *
     * @throws UnmodifiableMetadataException If this metadata is not modifiable.
     */
    public void prune() {
        Pruner.isEmpty(this, true);
    }

    /**
     * Copies the values from the specified metadata. The {@code source} metadata must implements
     * the same metadata interface (defined by the {@linkplain #getStandard() standard}) than this
     * class, but doesn't need to be the same implementation class.
     * The default implementation performs the copy using Java reflections.
     *
     * {@note This method is intended to provide the functionality of a <cite>copy constructor</cite>.
     * We do not provide copy constructor directly because usage of Java reflection in this context
     * is unsafe (we could invoke subclass methods before the subclasses construction is completed).}
     *
     * @param  source The metadata to copy values from.
     * @throws ClassCastException if the specified metadata doesn't implements the expected
     *         metadata interface.
     * @throws UnmodifiableMetadataException if this class doesn't define {@code set*(…)} methods
     *         corresponding to the {@code get*()} methods found in the implemented interface, or
     *         if this instance is not modifiable for some other reason.
     */
    public void shallowCopy(final Object source) throws ClassCastException, UnmodifiableMetadataException {
        ensureNonNull("source", source);
        getStandard().shallowCopy(source, this);
    }

    /**
     * Returns a view of the property values in a {@link Map}. The map is backed by this metadata
     * object, so changes in the underlying metadata object are immediately reflected in the map
     * and conversely.
     *
     * <p>The map supports the {@link Map#put(Object, Object) put(…)} and {@link Map#remove(Object)
     * remove(…)} operations if the underlying metadata object contains setter methods.
     * The keys are case-insensitive and can be either the JavaBeans property name or
     * the UML identifier.</p>
     *
     * <p>The default implementation is equivalent to the following method call:</p>
     * {@preformat java
     *   return getStandard().asMap(this, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
     * }
     *
     * @return A view of this metadata object as a map.
     *
     * @see MetadataStandard#asMap(Object, KeyNamePolicy, ValueExistencePolicy)
     */
    public synchronized Map<String,Object> asMap() {
        if (asMap == null) {
            asMap = getStandard().asMap(this, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
        }
        return asMap;
    }

    /**
     * Returns the property types and values as a tree table.
     * In the current implementation, the tree is not live (i.e. changes in metadata are not
     * reflected in the tree). However it may be improved in a future SIS implementation.
     *
     * @return The property types and values as a tree table.
     */
    public TreeTable asTreeTable() {
        return getStandard().asTreeTable(this);
    }

    /**
     * Compares this metadata with the specified object for equality. The default
     * implementation uses Java reflection. Subclasses may override this method
     * for better performances, or for comparing "hidden" properties not specified
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
     * {@note This method does not cache the value because current implementation has no notification
     *        mechanism for tracking changes in children properties. If this metadata is known to be
     *        immutable, then subclasses may consider caching the hash code value at their choice.}
     *
     * @see MetadataStandard#hashCode(Object)
     */
    @Override
    public int hashCode() {
        return getStandard().hashCode(this);
    }

    /**
     * Returns a string representation of this metadata.
     * The default implementation is as below:
     *
     * {@preformat java
     *     return asTreeTable().toString();
     * }
     *
     * Note that this make extensive use of Unicode characters
     * and is better rendered with a monospaced font.
     */
    @Override
    public String toString() {
        return asTreeTable().toString();
    }
}
