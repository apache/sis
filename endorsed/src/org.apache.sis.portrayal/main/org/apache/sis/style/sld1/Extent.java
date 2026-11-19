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
 * A range or enumerated list of values for a feature attribute, used to restrict
 * the features shown by a {@link FeatureTypeConstraint} to some specific range or set of values.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class Extent extends SLDElement {
    /**
     * Name of the feature attribute constrained by this extent, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    protected String name;

    /**
     * Range or enumerated list of values, or {@code null} if none.
     *
     * @see #getValue()
     * @see #setValue(String)
     */
    protected String value;

    /**
     * Creates an initially empty extent.
     * Callers should set the {@linkplain #setName(String) name} and
     * the {@linkplain #setValue(String) value} after construction,
     * since those properties are mandatory.
     */
    public Extent() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Extent(final Extent source) {
        name  = source.name;
        value = source.value;
    }

    /**
     * Returns the name of the feature attribute constrained by this extent.
     *
     * @return name of the feature attribute constrained by this extent.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets the name of the feature attribute constrained by this extent.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name of the feature attribute, or {@code null} if none.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the range or enumerated list of values.
     *
     * @return range or enumerated list of values.
     */
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Sets the range or enumerated list of values.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new range or enumerated list of values, or {@code null} if none.
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
    public Extent clone() {
        return (Extent) super.clone();
    }
}
