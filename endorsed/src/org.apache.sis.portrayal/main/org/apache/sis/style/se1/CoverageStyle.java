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
package org.apache.sis.style.se1;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.sis.coverage.BandedCoverage;


/**
 * Defines the styling that is to be applied to a coverage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "CoverageStyleType")
@XmlRootElement(name = "CoverageStyle")
public class CoverageStyle extends AbstractStyle<BandedCoverage> {
    /**
     * The default style factory for coverages.
     */
    public static final StyleFactory<BandedCoverage> FACTORY =
            new StyleFactory<>(FeatureTypeStyle.FACTORY);

    /**
     * Creates an initially empty coverage style.
     */
    public CoverageStyle() {
        super(FACTORY);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public CoverageStyle(final CoverageStyle source) {
        super(source);
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public CoverageStyle clone() {
        final var clone = (CoverageStyle) super.clone();
        return clone;
    }
}
