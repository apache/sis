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

import java.util.Collection;
import java.util.Comparator;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Comparator to sort Features with a given array of query SortBy[].
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class SortByComparator implements Comparator<Feature> {

    private final SortBy[] orders;

    public SortByComparator(final SortBy... orders) {
        if (orders == null || orders.length == 0) {
            throw new IllegalArgumentException("SortBy array can not be null or empty.");
        }

        this.orders = orders;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int compare(final Feature f1, final Feature f2) {

        for (final SortBy order : orders) {
            final PropertyName property = order.getPropertyName();
            Object val1 = property.evaluate(f1);
            Object val2 = property.evaluate(f2);
            if (val1 instanceof Collection) {
                //TODO find a correct way to compare collection values
                //pick the first value
                if (((Collection) val1).isEmpty()) {
                    val1 = null;
                } else {
                    val1 = ((Collection) val1).iterator().next();
                }
            }
            if (val2 instanceof Collection) {
                //TODO find a correct way to compare collection values
                //pick the first value
                if (((Collection) val2).isEmpty()) {
                    val2 = null;
                } else {
                    val2 = ((Collection) val2).iterator().next();
                }
            }

            final Comparable o1 = Comparable.class.cast(val1);
            final Comparable o2 = Comparable.class.cast(val2);

            if (o1 == null) {
                if (o2 == null) {
                    continue;
                }
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            final int result;
            if (order.getSortOrder() == SortOrder.ASCENDING) {
                result = o1.compareTo(o2);
            } else {
                result = o2.compareTo(o1);
            }

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

}
