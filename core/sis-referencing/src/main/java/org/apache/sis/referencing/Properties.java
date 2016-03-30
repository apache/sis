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
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.apache.sis.internal.util.AbstractMap;
import org.apache.sis.util.Deprecable;


/**
 * An immutable map fetching all properties from the specified identified object.
 * Calls to {@code get} methods are forwarded to the appropriate {@link IdentifiedObject} method.
 * This map does not contain null value. Collections are converted to arrays.
 *
 * <p>This map is read-only. Whether it is serializable, immutable or thread-safe depends if the
 * underlying {@code IdentifiedObject} instance is itself serializable, immutable or thread-safe.</p>
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4
 * @version 0.7
 * @module
 */
final class Properties extends AbstractMap<String,Object> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6391635771714311314L;

    /**
     * The keys to search for. The index of each element in this array must matches the index searched
     * by {@link #getAt(IdentifiedObject, int)}. In other words, this array performs the reverse mapping
     * of {@link #INDICES}.
     */
    private static final String[] KEYS = {
        /*[0]*/ IdentifiedObject        .NAME_KEY,
        /*[1]*/ IdentifiedObject        .IDENTIFIERS_KEY,
        /*[2]*/ IdentifiedObject        .ALIAS_KEY,
        /*[3]*/ IdentifiedObject        .REMARKS_KEY,
        /*[4]*/ CoordinateOperation     .SCOPE_KEY,                     // same in Datum and ReferenceSystem
        /*[5]*/ CoordinateOperation     .DOMAIN_OF_VALIDITY_KEY,        // same in Datum and ReferenceSystem
        /*[6]*/ CoordinateOperation     .OPERATION_VERSION_KEY,
        /*[7]*/ CoordinateOperation     .COORDINATE_OPERATION_ACCURACY_KEY,
        /*[8]*/ OperationMethod         .FORMULA_KEY,
        /*[9]*/ AbstractIdentifiedObject.DEPRECATED_KEY

        /*
         * The current implementation does not look for minimum and maximum values in ParameterDescriptor
         * and CoordinateSystemAxis, because their interpretation depends on the unit of measurement.
         * Including those properties in this map causes more harm than good.
         */
    };

    /**
     * The mapping from key names to the index expected by the {@link #getAt(IdentifiedObject, int)} method.
     * This map shall not be modified after construction (for multi-thread safety without synchronization).
     */
    private static final Map<String,Integer> INDICES = new HashMap<String,Integer>(16);
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
    final IdentifiedObject object;

    /**
     * The bitmask of properties to exclude.
     */
    final int excludeMask;

    /**
     * Creates new properties from the specified identified object.
     */
    Properties(final IdentifiedObject object, final String[] excludes) {
        this.object = object;
        int excludeMask = 0;
        for (final String exclude : excludes) {
            final Integer i = INDICES.get(exclude);
            if (i != null) {
                excludeMask |= (1 << i);
            }
        }
        this.excludeMask = excludeMask;
    }

    /**
     * Returns the value to which this map maps the specified index.
     * Returns null if the map contains no mapping for the given index.
     *
     * @param key The property index, as one of the values in the {@link #INDICES} map.
     */
    final Object getAt(final int key) {
        if ((excludeMask & (1 << key)) == 0) {
            switch (key) {
                case 0: {   // NAME_KEY
                    return object.getName();
                }
                case 1: {   // IDENTIFIERS_KEY
                    final Collection<ReferenceIdentifier> c = object.getIdentifiers();
                    if (c != null) {
                        final int size = c.size();
                        if (size != 0) {
                            return c.toArray(new ReferenceIdentifier[size]);
                        }
                    }
                    break;
                }
                case 2: {   // ALIAS_KEY
                    final Collection<GenericName> c = object.getAlias();
                    if (c != null) {
                        final int size = c.size();
                        if (size != 0) {
                            return c.toArray(new GenericName[size]);
                        }
                    }
                    break;
                }
                case 3: {   // REMARKS_KEY
                    return object.getRemarks();
                }
                case 4: {   // SCOPE_KEY
                    if (object instanceof ReferenceSystem) {
                        return ((ReferenceSystem) object).getScope();
                    } else if (object instanceof Datum) {
                        return ((Datum) object).getScope();
                    } else if (object instanceof CoordinateOperation) {
                        return ((CoordinateOperation) object).getScope();
                    }
                    break;
                }
                case 5: {   // DOMAIN_OF_VALIDITY_KEY
                    if (object instanceof ReferenceSystem) {
                        return ((ReferenceSystem) object).getDomainOfValidity();
                    } else if (object instanceof Datum) {
                        return ((Datum) object).getDomainOfValidity();
                    } else if (object instanceof CoordinateOperation) {
                        return ((CoordinateOperation) object).getDomainOfValidity();
                    }
                    break;
                }
                case 6: {   // OPERATION_VERSION_KEY
                    if (object instanceof CoordinateOperation) {
                        return ((CoordinateOperation) object).getOperationVersion();
                    }
                    break;
                }
                case 7: {   // COORDINATE_OPERATION_ACCURACY_KEY
                    if (object instanceof CoordinateOperation) {
                        final Collection<PositionalAccuracy> c = ((CoordinateOperation) object).getCoordinateOperationAccuracy();
                        if (c != null) {
                            final int size = c.size();
                            if (size != 0) {
                                return c.toArray(new PositionalAccuracy[size]);
                            }
                        }
                    }
                    break;
                }
                case 8: {   // FORMULA_KEY
                    if (object instanceof OperationMethod) {
                        return ((OperationMethod) object).getFormula();
                    }
                    break;
                }
                case 9: {   // DEPRECATED_KEY
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
