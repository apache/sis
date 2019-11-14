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
package org.apache.sis.internal.storage.query;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.apache.sis.util.collection.Containers;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.expression.Expression;


/**
 * Comparator sorting features by an array of {@code SortBy} expressions, applied in order.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 *
 * @todo Current implementation has unchecked casts. Fixing that may require a revision of filter interfaces.
 *       See <a href="https://github.com/opengeospatial/geoapi/issues/32">GeoAPI issue #32</a>.
 */
final class SortByComparator implements Comparator<Feature> {
    /**
     * The expression to evaluate for getting the values to sort.
     */
    private final Expression[] properties;

    /**
     * {@code false} for ascending order, {@code true} for descending order.
     * If unspecified or unknown, we assume ascending order.
     */
    private final boolean[] descending;

    /**
     * Creates a new comparator for the given sort expressions.
     * It is caller responsibility to ensure that the given array is non-empty.
     */
    SortByComparator(final SortBy[] orders) {
        properties = new Expression[orders.length];
        descending = new boolean   [orders.length];
        for (int i=0; i<orders.length; i++) {
            final SortBy order = orders[i];
            properties[i] = order.getPropertyName();
            descending[i] = SortOrder.DESCENDING.equals(order.getSortOrder());
        }
    }

    /**
     * Compares two features for order. Returns -1 if {@code f1} should be sorted before {@code f2},
     * +1 if {@code f2} should be after {@code f1}, or 0 if both are equal. Null features are sorted
     * after all non-null features, regardless sorting order.
     */
    @Override
    public int compare(final Feature f1, final Feature f2) {
        if (f1 != f2) {
            if (f1 == null) return +1;
            if (f2 == null) return -1;
            for (int i=0; i<properties.length; i++) {
                final Expression property = properties[i];
                Object o1 = property.evaluate(f1);
                Object o2 = property.evaluate(f2);
                if (o1 != o2) {
                    if (o1 == null) return +1;
                    if (o2 == null) return -1;
                    final int result;
                    /*
                     * No @SuppressWarnings("unchecked") below: those casts are really unsafe;
                     * we can not make them safe with current Filter API. See GeoAPI issue #32.
                     */
                    if (o1 instanceof Iterable<?>) {
                        result = Containers.compare(((Iterable) o1).iterator(), iterator(o2));
                    } else if (o2 instanceof Iterable<?>) {
                        result = Containers.compare(iterator(o1), ((Iterable) o2).iterator());
                    } else {
                        result = ((Comparable) o1).compareTo(o2);
                    }
                    if (result != 0) {
                        return descending[i] ? -result : result;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Returns an iterator for the given object.
     *
     * @todo Intentionally raw return type, but no {@literal @SuppressWarning} annotation because this
     *       is a real problem with current Filter API which needs to be fixed. See GeoAPI issue #32.
     */
    private static Iterator iterator(final Object o) {
        return (o instanceof Iterable<?>) ? ((Iterable<?>) o).iterator() : Collections.singleton(o).iterator();
    }
}
