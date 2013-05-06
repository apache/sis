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
 * {@code AbstractMetadata} subclasses may be read-only or read/write, at implementation choice.
 * The methods that modify the metadata may throw {@link UnmodifiableMetadataException} if the
 * metadata does not support the operation. Those methods are:
 *
 * <table class="sis">
 * <tr>
 *   <th>Read-only operations</th>
 *   <th class="sep">Read/write operations</th>
 * </tr>
 * <tr>
 *   <td><ul>
 *     <li>{@link #isEmpty()}</li>
 *     <li>{@link #asMap()} with {@code get} operations</li>
 *     <li>{@link #asTreeTable()} with {@code getValue} operations</li>
 *     <li>{@link #equals(Object, ComparisonMode)}</li>
 *   </ul></td>
 *   <td class="sep"><ul>
 *     <li>{@link #prune()}</li>
 *     <li>{@link #asMap()} with {@code put} operations</li>
 *     <li>{@link #asTreeTable()} with {@code setValue} operations</li>
 *   </ul></td>
 * </tr>
 * </table>
 *
 * Instances of this class are <strong>not</strong> synchronized for multi-threading.
 * Synchronization, if needed, is caller's responsibility. Note that synchronization locks
 * are not necessarily the metadata instances. For example an other common approach is to
 * use a single lock for the whole metadata tree (including children).
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
     * Subclasses will typically return a hard-coded constant such as
     * {@link MetadataStandard#ISO_19115}.
     *
     * {@note Implementation of this method shall not depend on the object state,
     *        since this method may be indirectly invoked by copy constructors.}
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
     *   <li>An other metadata object containing only {@code null} or empty properties.</li>
     * </ul>
     *
     * Note that empty properties can be removed by calling the {@link ModifiableMetadata#prune()}
     * method.
     *
     * {@section Note for implementors}
     * The default implementation uses Java reflection indirectly, by iterating over all entries
     * returned by {@link MetadataStandard#asValueMap(Object, KeyNamePolicy, ValueExistencePolicy)}.
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
     * Returns a view of the property values in a {@link Map}. The map is backed by this metadata
     * object, so changes in the underlying metadata object are immediately reflected in the map
     * and conversely.
     *
     * {@section Supported operations}
     * The map supports the {@link Map#put(Object, Object) put(…)} and {@link Map#remove(Object)
     * remove(…)} operations if the underlying metadata object contains setter methods.
     * The {@code remove(…)} method is implemented by a call to {@code put(…, null)}.
     *
     * {@section Keys and values}
     * The keys are case-insensitive and can be either the JavaBeans property name, the getter method name
     * or the {@linkplain org.opengis.annotation.UML#identifier() UML identifier}. The value given to a call
     * to the {@code put(…)} method shall be an instance of the type expected by the corresponding setter method,
     * or an instance of a type {@linkplain org.apache.sis.util.ObjectConverters#find(Class, Class) convertible}
     * to the expected type.
     *
     * {@section Multi-values entries}
     * Calls to {@code put(…)} replace the previous value, with one noticeable exception: if the metadata
     * property associated to the given key is a {@link java.util.Collection} but the given value is a single
     * element (not a collection), then the given value is {@linkplain java.util.Collection#add(Object) added}
     * to the existing collection. In other words, the returned map behaves as a <cite>multi-values map</cite>
     * for the properties that allow multiple values. If the intend is to unconditionally discard all previous
     * values, then make sure that the given value is a collection when the associated metadata property expects
     * such collection.
     *
     * {@section Default implementation}
     * The default implementation is equivalent to the following method call:
     *
     * {@preformat java
     *   return getStandard().asValueMap(this, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
     * }
     *
     * @return A view of this metadata object as a map.
     *
     * @see MetadataStandard#asValueMap(Object, KeyNamePolicy, ValueExistencePolicy)
     */
    public Map<String,Object> asMap() {
        return getStandard().asValueMap(this, KeyNamePolicy.JAVABEANS_PROPERTY, ValueExistencePolicy.NON_EMPTY);
    }

    /**
     * Returns the property types and values as a tree table.
     * The tree table is backed by the metadata object using Java reflection, so changes in the
     * underlying metadata object are immediately reflected in the tree table and conversely.
     *
     * <p>The returned {@code TreeTable} instance contains the following columns:</p>
     * <ul>
     *   <li><p>{@link org.apache.sis.util.collection.TableColumn#IDENTIFIER}<br>
     *       The {@linkplain org.opengis.annotation.UML#identifier() UML identifier} if any,
     *       or the Java Beans property name otherwise, of a metadata property. For example
     *       in a tree table view of {@link org.apache.sis.metadata.iso.citation.DefaultCitation},
     *       there is a node having the {@code "title"} identifier.</p></li>
     *
     *   <li><p>{@link org.apache.sis.util.collection.TableColumn#INDEX}<br>
     *       If the metadata property is a collection, then the zero-based index of the element in that collection.
     *       Otherwise {@code null}. For example in a tree table view of {@code DefaultCitation}, if the
     *       {@code "alternateTitle"} collection contains two elements, then there is a node with index 0
     *       for the first element and an other node with index 1 for the second element.</p>
     *
     *       <p>The {@code (IDENTIFIER, INDEX)} pair can be used as a primary key for uniquely identifying
     *       a node in a list of children. Note that the uniqueness is guaranteed only for the children of
     *       a given node; the same keys may appear in the children of any other nodes.</p></li>
     *
     *   <li><p>{@link org.apache.sis.util.collection.TableColumn#NAME}<br>
     *       A human-readable name for the node, derived from the identifier and the index.
     *       This is the column shown in the default {@link #toString()} implementation and
     *       may be localizable.</p></li>
     *
     *   <li><p>{@link org.apache.sis.util.collection.TableColumn#TYPE}<br>
     *       The base type of the value (usually an interface).</p></li>
     *
     *   <li><p>{@link org.apache.sis.util.collection.TableColumn#VALUE}<br>
     *       The metadata value for the node. Values in this column are writable if the underlying
     *       metadata class have a setter method for the property represented by the node.</p></li>
     * </ul>
     *
     * {@section Write operations}
     * Only the {@code VALUE} column may be writable, with one exception: newly created children need
     * to have their {@code IDENTIFIER} set before any other operation. For example the following code
     * adds a title to a citation:
     *
     * {@preformat java
     *     TreeTable.Node node = ...; // The node for a DefaultCitation.
     *     TreeTable.Node child = node.newChild();
     *     child.setValue(TableColumn.IDENTIFIER, "title");
     *     child.setValue(TableColumn.VALUE, "Le petit prince");
     *     // Nothing else to do - the child node has been added.
     * }
     *
     * Nodes can be removed by invoking the {@link java.util.Iterator#remove()} method on the
     * {@linkplain org.apache.sis.util.collection.TreeTable.Node#getChildren() children} iterator.
     *
     * {@section Default implementation}
     * The default implementation is equivalent to the following method call:
     *
     * {@preformat java
     *   return getStandard().asTreeTable(this, ValueExistencePolicy.NON_EMPTY);
     * }
     *
     * @return A tree table representation of the specified metadata.
     *
     * @see MetadataStandard#asTreeTable(Object, ValueExistencePolicy)
     */
    public TreeTable asTreeTable() {
        return getStandard().asTreeTable(this, ValueExistencePolicy.NON_EMPTY);
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
