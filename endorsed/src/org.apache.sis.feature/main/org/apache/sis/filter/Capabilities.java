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
package org.apache.sis.filter;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Optional;
import org.opengis.util.LocalName;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.capability.Conformance;
import org.opengis.filter.capability.IdCapabilities;
import org.opengis.filter.capability.AvailableFunction;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.capability.ScalarCapabilities;
import org.opengis.filter.capability.SpatialCapabilities;
import org.opengis.filter.capability.TemporalCapabilities;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.feature.internal.shared.AttributeConvention;


/**
 * Metadata about the specific elements that Apache SIS implementation supports.
 *
 * @todo Missing {@link SpatialCapabilities} and {@link TemporalCapabilities}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class Capabilities implements FilterCapabilities, Conformance, IdCapabilities, ScalarCapabilities {
    /**
     * The filter factory which is providing the functions.
     *
     * @see #getFunctions()
     */
    private final DefaultFilterFactory<?,?,?> factory;

    /**
     * Creates a new capability document.
     *
     * @param  factory  the filter factory which is providing functions.
     */
    Capabilities(final DefaultFilterFactory<?,?,?> factory) {
        this.factory = factory;
    }

    /**
     * Declaration of which conformance classes a particular implementation supports.
     */
    @Override
    public Conformance getConformance() {
        return this;
    }

    /**
     * Returns whether the implementation supports the <i>Resource Identification</i> conformance level.
     */
    @Override
    public boolean implementsResourceld() {
        return true;
    }

    /**
     * Provides capabilities used to convey supported identifier operators.
     */
    @Override
    public Optional<IdCapabilities> getIdCapabilities() {
        return Optional.of(this);
    }

    /**
     * Declares the names of the properties used for resource identifiers.
     */
    @Override
    public Collection<LocalName> getResourceIdentifiers() {
        return Set.of(AttributeConvention.IDENTIFIER_PROPERTY.tip());
    }

    /**
     * Returns whether the implementation supports the <i>Standard Filter</i> conformance level.
     * A value of {@code true} means that all the logical operators ({@code And}, {@code Or}, {@code Not})
     * are supported, together with all the standard {@link ComparisonOperatorName}. Those operators shall
     * be listed in the {@linkplain FilterCapabilities#getScalarCapabilities() scalar capabilities}.
     */
    @Override
    public boolean implementsStandardFilter() {
        return true;
    }

    /**
     * Indicates that SIS supports <i>And</i>, <i>Or</i> and <i>Not</i> operators.
     */
    @Override
    public boolean hasLogicalOperators() {
        return true;
    }

    /**
     * Advertises which logical, comparison and arithmetic operators the service supports.
     */
    @Override
    public Optional<ScalarCapabilities> getScalarCapabilities() {
        return Optional.of(this);
    }

    /**
     * Advertises that SIS supports all comparison operators.
     */
    @Override
    public Set<ComparisonOperatorName> getComparisonOperators() {
        return new CodeListSet<>(ComparisonOperatorName.class, true);
    }

    /**
     * Indicates that Apache SIS supports the <i>Spatial Filter</i> conformance level.
     *
     * @todo Need to implement {@linkplain FilterCapabilities#getSpatialCapabilities() temporal capabilities}.
     */
    @Override
    public boolean implementsSpatialFilter() {
        return true;
    }

    /**
     * Indicates that Apache SIS supports the <i>Temporal Filter</i> conformance level.
     *
     * @todo Need to implement {@linkplain FilterCapabilities#getTemporalCapabilities() temporal capabilities}.
     */
    @Override
    public boolean implementsTemporalFilter() {
        return true;
    }

    /**
     * Indicates that Apache SIS supports the <i>Sorting</i> conformance level.
     */
    @Override
    public boolean implementsSorting() {
        return true;
    }

    /**
     * Enumerates the functions that may be used in filter expressions.
     *
     * @return the function that may be used in filter expressions.
     */
    @Override
    public Map<String,AvailableFunction> getFunctions() {
        return factory.new Functions();
    }
}
