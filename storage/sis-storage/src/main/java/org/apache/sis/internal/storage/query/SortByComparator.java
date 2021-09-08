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

import java.util.List;
import java.io.Serializable;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.util.UnmodifiableArrayList;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.SortBy;
import org.opengis.filter.SortProperty;


/**
 * Comparator sorting features using an array of {@link SortProperty} elements applied in order.
 * This is restricted to comparator of {@link Feature} instances for now because this is the only
 * comparator that we currently need, and it makes {@linkplain #SortByComparator(SortByComparator,
 * SortByComparator) concatenations} type-safe. We may generalize in the future if needed.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public final class SortByComparator implements SortBy<Feature>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7964849249532212389L;

    /**
     * The sort order specified to the constructor.
     *
     * @see #getSortProperties()
     */
    private final SortProperty<Feature>[] properties;

    /**
     * Creates a new comparator for the given sort expressions.
     * It is caller responsibility to ensure that the given array is non-empty.
     */
    SortByComparator(SortProperty<Feature>[] properties) {
        properties = properties.clone();
        this.properties = properties;
        for (int i=0; i < properties.length; i++) {
            ArgumentChecks.ensureNonNullElement("properties", i, properties[i]);
        }
    }

    /**
     * Creates a new comparator as the concatenation of the two given comparators.
     *
     * @param  s1  the first "sort by" to concatenate.
     * @param  s2  the second "sort by" to concatenate.
     */
    public SortByComparator(final SortByComparator s1, final SortByComparator s2) {
        properties = ArraysExt.concatenate(s1.properties, s2.properties);
    }

    /**
     * Returns the properties whose values are used for sorting.
     * The list shall have a minimum of one element.
     */
    @Override
    public List<SortProperty<Feature>> getSortProperties() {
        return UnmodifiableArrayList.wrap(properties);
    }

    /**
     * Compares two resources for order. Returns a negative number if {@code r1} should be sorted before {@code r2},
     * a positive number if {@code r2} should be after {@code r1}, or 0 if both resources are equal.
     * The ordering of null resources or null property values is unspecified.
     */
    @Override
    public int compare(final Feature r1, final Feature r2) {
        for (final SortProperty<Feature> p : properties) {
            final int c = p.compare(r1, r2);
            if (c != 0) return c;
        }
        return 0;
    }
}
