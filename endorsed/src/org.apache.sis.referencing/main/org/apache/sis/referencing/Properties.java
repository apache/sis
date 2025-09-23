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
package org.apache.sis.referencing;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.function.IntFunction;
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.internal.shared.AbstractMap;
import org.apache.sis.referencing.internal.shared.CoordinateOperations;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;

// Specific to the geoapi-4.0 branch:
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;


/**
 * An immutable map fetching all properties from the specified identified object.
 * Calls to {@code get} methods are forwarded to the appropriate {@link IdentifiedObject} method.
 * This map does not contain null value. Collections are converted to arrays.
 *
 * <p>This map is read-only. Whether it is serializable, immutable or thread-safe depends if the
 * underlying {@code IdentifiedObject} instance is itself serializable, immutable or thread-safe.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
final class Properties extends AbstractMap<String,Object> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 836068852472172370L;

    /**
     * The keys to search for. The index of each element in this array must matches the index searched by
     * {@link #getAt(int)}. In other words, this array performs the reverse mapping of {@link #INDICES}.
     */
    private static final String[] KEYS = {
        /*[ 0]*/ IdentifiedObject        .NAME_KEY,
        /*[ 1]*/ IdentifiedObject        .IDENTIFIERS_KEY,
        /*[ 2]*/ IdentifiedObject        .ALIAS_KEY,
        /*[ 3]*/ IdentifiedObject        .DOMAINS_KEY,
        /*[ 4]*/ IdentifiedObject        .REMARKS_KEY,
        /*[ 5]*/ ObjectDomain            .SCOPE_KEY,
        /*[ 6]*/ ObjectDomain            .DOMAIN_OF_VALIDITY_KEY,
        /*[ 7]*/ CoordinateOperation     .OPERATION_VERSION_KEY,
        /*[ 8]*/ CoordinateOperation     .COORDINATE_OPERATION_ACCURACY_KEY,
        /*[ 9]*/ OperationMethod         .FORMULA_KEY,
        /*[10]*/ CoordinateOperations    .PARAMETERS_KEY,
        /*[11]*/ AbstractIdentifiedObject.DEPRECATED_KEY

        /*
         * The current implementation does not look for minimum and maximum values in ParameterDescriptor
         * and CoordinateSystemAxis, because their interpretation depends on the unit of measurement.
         * Including those properties in this map causes more harm than good.
         */
    };

    /**
     * The mapping from key names to the index expected by the {@link #getAt(int)} method.
     * This map shall not be modified after construction (for multi-thread safety without synchronization).
     */
    private static final Map<String,Integer> INDICES = new HashMap<>(16);
    static {
        for (int i=0; i<KEYS.length; i++) {
            if (INDICES.put(KEYS[i], i) != null) {
                throw new AssertionError(i);
            }
        }
    }

    /**
     * The object where all properties come from.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    final IdentifiedObject object;

    /**
     * The bitmask of properties to exclude.
     */
    final int excludeMask;

    /**
     * Creates new properties from the specified identified object.
     */
    Properties(final IdentifiedObject object, final String[] excludes) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        int excludeMask = 0;
        for (final String exclude : excludes) {
            final Integer i = INDICES.get(exclude);
            if (i != null) {
                excludeMask |= (1 << i);
            }
        }
        this.excludeMask = excludeMask;
        this.object = object;
    }

    /**
     * Returns the value to which this map maps the specified index.
     * Returns null if the map contains no mapping for the given index.
     *
     * @param  key  the property index, as one of the values in the {@link #INDICES} map.
     */
    final Object getAt(final int key) {
        if ((excludeMask & (1 << key)) == 0) {
            switch (key) {
                case 0: return         object.getName();                                // NAME_KEY
                case 1: return toArray(object.getIdentifiers(), Identifier[]::new);     // IDENTIFIERS_KEY
                case 2: return toArray(object.getAlias(),      GenericName[]::new);     // ALIAS_KEY
                case 3: return toArray(object.getDomains(),   ObjectDomain[]::new);     // DOMAINS_KEY
                case 4: return         object.getRemarks().orElse(null);                // REMARKS_KEY
                case 5: {   // SCOPE_KEY
                    for (final ObjectDomain domain : object.getDomains()) {
                        InternationalString scope = domain.getScope();
                        if (scope != null) return scope;
                    }
                    break;
                }
                case 6: {   // DOMAIN_OF_VALIDITY_KEY
                    for (final ObjectDomain domain : object.getDomains()) {
                        Extent extent = domain.getDomainOfValidity();
                        if (extent != null) return extent;
                    }
                    break;
                }
                case 7: {   // OPERATION_VERSION_KEY
                    if (object instanceof CoordinateOperation) {
                        return ((CoordinateOperation) object).getOperationVersion().orElse(null);
                    }
                    break;
                }
                case 8: {   // COORDINATE_OPERATION_ACCURACY_KEY
                    if (object instanceof CoordinateOperation) {
                        return toArray(((CoordinateOperation) object).getCoordinateOperationAccuracy(), PositionalAccuracy[]::new);
                    }
                    break;
                }
                case 9: {   // FORMULA_KEY
                    if (object instanceof OperationMethod) {
                        return ((OperationMethod) object).getFormula();
                    }
                    break;
                }
                case 10: {   // PARAMETERS_KEY
                    if (object instanceof SingleOperation) {
                        return ((SingleOperation) object).getParameterValues();
                    }
                    break;
                }
                case 11: {  // DEPRECATED_KEY
                    if (object instanceof Deprecable) {
                        return ((Deprecable) object).isDeprecated();
                    }
                    break;
                }
                default: throw new AssertionError(key);
            }
        }
        return null;
    }

    /**
     * Returns the collection content as an array if the collection is non-null and non-empty.
     * Otherwise returns {@code null}.
     *
     * @param  <E>        type of elements in the collection.
     * @param  c          the collection, or {@code null}.
     * @param  generator  function to invoke for creating an initially empty array.
     * @return array of collection elements, or {@code null}.
     */
    private static <E> E[] toArray(final Collection<E> c, final IntFunction<E[]> generator) {
        return (c == null || c.isEmpty()) ? null : c.toArray(generator);
    }

    /**
     * Returns {@code false} if this map contains at least one element, or {@code true} otherwise.
     */
    @Override
    public boolean isEmpty() {
        for (int i=0; i<KEYS.length; i++) {
            if (getAt(i) != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of non-null properties in this map.
     */
    @Override
    public int size() {
        int n = 0;
        for (int i=0; i<KEYS.length; i++) {
            if (getAt(i) != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the value to which this map maps the specified key.
     * Returns {@code null} if the map contains no mapping for this key.
     */
    @Override
    public Object get(final Object key) {
        final Integer i = INDICES.get(key);
        return (i != null) ? getAt(i) : null;
    }

    /**
     * Iterates over the {@link #KEYS}, returning only the entry having a non-null value.
     */
    @Override
    protected EntryIterator<String,Object> entryIterator() {
        return new EntryIterator<String,Object>() {
            /**
             * Index of the next element to inspect.
             */
            private int nextIndex;

            /**
             * Index of the value to be returned by {@link #next()}, or {@code null} if not yet computed.
             */
            private Object value;

            /**
             * Returns {@code true} if there is a value to return.
             */
            @Override
            protected boolean next() {
                while (nextIndex < KEYS.length) {
                    value = getAt(nextIndex++);
                    if (value != null) {
                        return true;
                    }
                }
                return false;
            }

            /**
             * Returns the key at the current position.
             */
            @Override
            protected String getKey() {
                return KEYS[nextIndex - 1];
            }

            /**
             * Returns the value at the current position.
             */
            @Override
            protected Object getValue() {
                return value;
            }
        };
    }
}
