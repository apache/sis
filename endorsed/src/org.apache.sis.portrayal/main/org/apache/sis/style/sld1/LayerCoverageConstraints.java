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
package org.apache.sis.style.sld1;

import java.util.List;
import java.util.ArrayList;


/**
 * A list of constraints restricting the coverages and coverage extents
 * that are shown for a {@link UserLayer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class LayerCoverageConstraints extends LayerConstraints<CoverageConstraint> {
    /**
     * The coverage constraints, as a live list.
     *
     * @see #constraints()
     */
    private List<CoverageConstraint> constraints;

    /**
     * Creates an initially empty list of coverage constraints.
     * Callers should add at least one {@linkplain #constraints() constraint} after construction.
     */
    public LayerCoverageConstraints() {
        constraints = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public LayerCoverageConstraints(final LayerCoverageConstraints source) {
        constraints = new ArrayList<>(source.constraints);
    }

    /**
     * Returns the coverage constraints.
     * Each constraint restricts the layer to a coverage,
     * optionally with a restriction on the coverage extent.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return the coverage constraints, as a live collection.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<CoverageConstraint> constraints() {
        return constraints;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {constraints};
    }

    /**
     * Returns a deep clone of this object.
     *
     * @return deep clone of all constraints.
     */
    @Override
    public LayerCoverageConstraints clone() {
        final var clone = (LayerCoverageConstraints) super.clone();
        clone.constraints = new ArrayList<>(constraints);
        clone.constraints.replaceAll(CoverageConstraint::clone);
        return clone;
    }
}
