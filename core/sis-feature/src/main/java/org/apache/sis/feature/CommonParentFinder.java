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
package org.apache.sis.feature;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Predicate;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * Finds a feature type common to all given types. This is either one of the given types, or a parent common to all types.
 * A feature <var>F</var> is considered a common parent if <code>F.{@link DefaultFeatureType#isAssignableFrom(FeatureType)
 * isAssignableFrom}(type)</code> returns {@code true} for all elements <var>type</var> in the given array.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class CommonParentFinder {
    /**
     * Finds a feature type common to all given types. See class javadoc.
     *
     * @param  types  types for which to find a common type.
     * @return a feature type which is assignable from all given types, or {@code null} if none.
     */
    static FeatureType select(final Iterable<? extends FeatureType> types) {
        /*
         * Get a set of unique feature types. Since the process done in this method may be relatively costly,
         * we want to avoid doing the same work twice. The purpose of Boolean value will be explained later.
         */
        final Map<FeatureType,Boolean> allTypes = new LinkedHashMap<>();
        types.forEach((type) -> allTypes.putIfAbsent(type, Boolean.FALSE));
        allTypes.remove(null);
        int count = allTypes.size();
        final FeatureType[] required = allTypes.keySet().toArray(new FeatureType[count]);
        /*
         * We are going to iterate over the `required` array many times. For performance reason, this array should be as
         * short as possible. We can make it shorter by removing any element A which is assignable to another element B,
         * since "B assignable from A" implies that a common parent assignable from B will also be assignable from A.
         * If this simplification results in an array of only one element, we are done.
         */
        for (int i=0; i<count; i++) {
            final FeatureType parent = required[i];
            for (int j=count; --j >= 0;) {                 // Reverse order so that removed elements do not impact index j.
                if (j != i && parent.isAssignableFrom(required[j])) {
                    System.arraycopy(required, j+1, required, j, --count - j);      // If A is assignable from B, remove B.
                    required[count] = null;
                    if (j < i) i--;                         // If removed element was before i, then i indices are shifted.
                }
            }
        }
        switch (count) {
            case 0: return null;
            case 1: return required[0];
        }
        /*
         * If we reach this point, no element in the given types is a parent of all other types.
         * We need to build a set of all parent types.
         */
        return new CommonParentFinder(allTypes, required, count).select();
    }

    /**
     * All feature types examined by this class. Values are whether a feature type is retained
     * as a candidate for common parent. Those values are initially {@link Boolean#FALSE} then
     * updated after we find that a type {@linkplain #isAssignableFromAll is assignable from
     * all required types}.
     */
    private final Map<FeatureType,Boolean> allTypes;

    /**
     * The features types which must be assignable to the common parent.
     * This array may not contain all feature types given by user, since we try to remove redundant elements.
     */
    private final FeatureType[] required;

    /**
     * Number of valid elements in the {@link #required} array.
     */
    private final int count;

    /**
     * Creates a finder for a common parent of the given types.
     * Invokes {@link #select()} after construction time for getting the parent.
     */
    private CommonParentFinder(final Map<FeatureType,Boolean> allTypes, final FeatureType[] required, final int count) {
        this.allTypes = allTypes;
        this.required = required;
        this.count    = count;
        for (int i=0; i<count; i++) {
            scanParents(required[i]);
        }
    }

    /**
     * Returns {@code true} if the given parent candidate is assignable from all required types.
     * The feature type to be returned by {@link #select()} must met that condition.
     */
    private boolean isAssignableFromAll(final FeatureType parent) {
        for (int i=0; i<count; i++) {
            final FeatureType type = required[i];
            if (type != parent && !parent.isAssignableFrom(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifies if the given type has a parent which is assignable from all required types.
     * This method verifies recursively parents of parents, skipping types that have already
     * been examined in a previous invocation of this method.
     */
    private void scanParents(final FeatureType type) {
        for (final FeatureType parent : type.getSuperTypes()) {
            if (allTypes.putIfAbsent(parent, Boolean.FALSE) == null) {
                if (isAssignableFromAll(parent)) {
                    allTypes.put(parent, Boolean.TRUE);     // Found a candidate.
                    skipParents(parent);                    // All parents of this candidate can be skipped.
                } else {
                    scanParents(parent);                    // Verify if a parent is assignable.
                }
            }
        }
    }

    /**
     * Invoked when the given feature type is assignable from all required types.
     * There is no need to verify the parents since they are not going to be a better match.
     */
    private void skipParents(final FeatureType type) {
        assert isAssignableFromAll(type);
        for (final FeatureType parent : type.getSuperTypes()) {
            if (Boolean.TRUE.equals(allTypes.put(parent, Boolean.FALSE))) {
                // If `parent` was previously a candidate, its parents have already been set to `FALSE`.
            } else {
                skipParents(parent);
            }
        }
    }

    /**
     * Invoked after all feature types have been examined. This method removes all features types that
     * are not parent of required types, then select the one having the greatest number of properties.
     */
    FeatureType select() {
        allTypes.values().removeIf(Predicate.isEqual(Boolean.FALSE));
        FeatureType best = null;
        int numProperties = 0;
        for (final FeatureType type : allTypes.keySet()) {
            final int n = type.getProperties(true).size();
            if (best == null || n > numProperties) {
                best = type;
                numProperties = n;
            }
        }
        return best;
    }
}
