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

import java.util.Optional;


/**
 * Restricts the layer's data to a specific coverage,
 * optionally further restricted to some time or range axis values.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class CoverageConstraint extends SLDElement {
    /**
     * Name of the coverage to which this constraint applies, or {@code null} if none.
     *
     * @see #getCoverageName()
     * @see #setCoverageName(String)
     */
    protected String coverageName;

    /**
     * Restriction on the time or range axis values of the coverage, or {@code null} if none.
     *
     * @see #getCoverageExtent()
     * @see #setCoverageExtent(CoverageExtent)
     */
    protected CoverageExtent coverageExtent;

    /**
     * Creates an initially empty coverage constraint.
     * Callers should set the {@linkplain #setCoverageName(String) coverage name}
     * after construction, since this property is mandatory.
     */
    public CoverageConstraint() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public CoverageConstraint(final CoverageConstraint source) {
        coverageName   = source.coverageName;
        coverageExtent = source.coverageExtent;
    }

    /**
     * Returns the name of the coverage to which this constraint applies.
     *
     * @return name of the coverage to which this constraint applies.
     */
    public Optional<String> getCoverageName() {
        return Optional.ofNullable(coverageName);
    }

    /**
     * Sets the name of the coverage to which this constraint applies.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name of the coverage, or {@code null} if none.
     */
    public void setCoverageName(final String value) {
        coverageName = value;
    }

    /**
     * Returns the restriction on the time or range axis values of the coverage.
     * If absent, the whole coverage is shown.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this constraint, and conversely.
     *
     * @return restriction on the time or range axis values of the coverage.
     */
    public Optional<CoverageExtent> getCoverageExtent() {
        return Optional.ofNullable(coverageExtent);
    }

    /**
     * Sets the restriction on the time or range axis values of the coverage.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new restriction on the coverage extent, or {@code null} if none.
     */
    public void setCoverageExtent(final CoverageExtent value) {
        coverageExtent = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {coverageName, coverageExtent};
    }

    /**
     * Returns a deep clone of this object.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public CoverageConstraint clone() {
        final var clone = (CoverageConstraint) super.clone();
        if (clone.coverageExtent != null) {
            clone.coverageExtent = clone.coverageExtent.clone();
        }
        return clone;
    }
}
