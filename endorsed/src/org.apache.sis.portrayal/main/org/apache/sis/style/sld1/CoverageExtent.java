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
import java.util.Optional;


/**
 * Restriction on the values of a coverage shown by a {@link CoverageConstraint}.
 * The restriction is expressed either by values along some range axes, or by a time period.
 * The XML schema allows only one of those two alternatives in the same element.
 * This class does not enforce that rule; it is caller's responsibility to set only one of them.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class CoverageExtent extends SLDElement {
    /**
     * Values along some range axes, as a live list.
     *
     * @see #rangeAxes()
     */
    private List<RangeAxis> rangeAxes;

    /**
     * Time period of the coverage values to show, or {@code null} if none.
     *
     * @see #getTimePeriod()
     * @see #setTimePeriod(String)
     */
    protected String timePeriod;

    /**
     * Creates an initially empty coverage extent.
     */
    public CoverageExtent() {
        rangeAxes = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public CoverageExtent(final CoverageExtent source) {
        rangeAxes  = new ArrayList<>(source.rangeAxes);
        timePeriod = source.timePeriod;
    }

    /**
     * Returns the values along some range axes.
     * Should be empty if a {@linkplain #getTimePeriod() time period} is specified.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return values along some range axes, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<RangeAxis> rangeAxes() {
        return rangeAxes;
    }

    /**
     * Returns the time period of the coverage values to show.
     * This is a string such as {@code "2005-05-17T00:00:00Z/2005-05-23T00:00:00Z"}.
     * Should be absent if some {@linkplain #rangeAxes() range axes} are specified.
     *
     * @return time period of the coverage values to show.
     */
    public Optional<String> getTimePeriod() {
        return Optional.ofNullable(timePeriod);
    }

    /**
     * Sets the time period of the coverage values to show.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new time period, or {@code null} if none.
     */
    public void setTimePeriod(final String value) {
        timePeriod = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {rangeAxes, timePeriod};
    }

    /**
     * Returns a deep clone of this object.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public CoverageExtent clone() {
        final var clone = (CoverageExtent) super.clone();
        clone.rangeAxes = new ArrayList<>(rangeAxes);
        clone.rangeAxes.replaceAll(RangeAxis::clone);
        return clone;
    }
}
