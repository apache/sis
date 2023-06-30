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
import org.apache.sis.filter.DefaultFilterFactory;

// Branch-dependent imports
import org.opengis.feature.Feature;


/**
 * Defines the styling that is to be applied to a single feature type.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "FeatureTypeStyleType")
@XmlRootElement(name = "FeatureTypeStyle")
public class FeatureTypeStyle extends AbstractStyle<Feature> {
    /**
     * The default style factory for features.
     */
    public static final StyleFactory<Feature> FACTORY =
            new StyleFactory<>(DefaultFilterFactory.forFeatures());

    /**
     * Creates an initially empty feature type style.
     * Callers should set the following properties after construction:
     *
     * <ul>
     *   <li>Either the {@linkplain #setFeatureTypeName feature type name}
     *       or {@linkplain #semanticTypeIdentifiers() semantic type identifiers}, or both.</li>
     *   <li>At least one {@linkplain #rules() rule} should be added.</li>
     * </ul>
     */
    public FeatureTypeStyle() {
        super(FACTORY);
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public FeatureTypeStyle(final FeatureTypeStyle source) {
        super(source);
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public FeatureTypeStyle clone() {
        final var clone = (FeatureTypeStyle) super.clone();
        return clone;
    }
}
