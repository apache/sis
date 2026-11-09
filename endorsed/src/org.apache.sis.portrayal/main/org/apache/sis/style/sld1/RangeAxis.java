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
 * A value along a coverage range axis, used to restrict the coverage values shown
 * by a {@link CoverageExtent}. Examples of range axes are bands or time.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class RangeAxis extends SLDElement {
    /**
     * Name of the range axis, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    protected String name;

    /**
     * Value along the range axis, or {@code null} if none.
     *
     * @see #getValue()
     * @see #setValue(String)
     */
    protected String value;

    /**
     * Creates an initially empty range axis.
     * Callers should set the {@linkplain #setName(String) name} and
     * the {@linkplain #setValue(String) value} after construction,
     * since those properties are mandatory.
     */
    public RangeAxis() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public RangeAxis(final RangeAxis source) {
        name  = source.name;
        value = source.value;
    }

    /**
     * Returns the name of the range axis.
     *
     * @return name of the range axis.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets the name of the range axis.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name of the range axis, or {@code null} if none.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the value along the range axis.
     *
     * @return value along the range axis.
     */
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Sets the value along the range axis.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new value along the range axis, or {@code null} if none.
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {name, value};
    }

    /**
     * Returns a clone of this object.
     *
     * @return a clone of this element.
     */
    @Override
    public RangeAxis clone() {
        return (RangeAxis) super.clone();
    }
}
