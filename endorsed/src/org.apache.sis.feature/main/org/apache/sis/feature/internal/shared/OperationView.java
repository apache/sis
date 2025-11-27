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
package org.apache.sis.feature.internal.shared;

import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.internal.Resources;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Optional;
import java.io.Serializable;
import org.opengis.util.InternationalString;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureOperationException;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyNotFoundException;
import org.apache.sis.util.Deprecable;


/**
 * An operation wrapper for hiding its dependencies. Because Apache <abbr>SIS</abbr>
 * fetches operation dependencies only from instances of {@link AbstractOperation},
 * we only need a wrapper that do <em>not</em> extend {@code AbstractOperation}.
 * This class should be used only together with {@link FeatureView}.
 *
 * <h2>Purpose</h2>
 * The {@link DefaultFeatureType} constructor verifies that all dependencies of all operations exist.
 * This verification can block us from constructing the {@link FeatureView#source} type if the view
 * does not include all dependencies needed by an operation. By wrapping the operation, we prevent
 * {@link DefaultFeatureType} from doing this verification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OperationView implements Operation, Deprecable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4679426189917900959L;

    /**
     * The instance doing the actual operation.
     */
    private final AbstractOperation source;

    /**
     * The name of feature type expected by the operation.
     */
    @SuppressWarnings("serial")     // Most Apache SIS implementations are serializable.
    private final GenericName valueType;

    /**
     * Creates a new operation wrapper.
     *
     * @param  valueType  the name of feature type expected by the operation.
     * @param  source     the operation instance to make opaque.
     */
    OperationView(final AbstractOperation source, final GenericName valueType) {
        this.source = source;
        this.valueType = valueType;
    }

    /**
     * Returns the name of the wrapped operation.
     */
    @Override
    public GenericName getName() {
        return source.getName();
    }

    /**
     * Returns a concise definition of the operation.
     */
    @Override
    public InternationalString getDefinition() {
        return source.getDefinition();
    }

    /**
     * Returns a natural language designator for the operation.
     * This can be used as an alternative to the {@linkplain #getName() name} in user interfaces.
     */
    @Override
    public Optional<InternationalString> getDesignation() {
        return source.getDesignation();
    }

    /**
     * Returns optional information beyond that required for concise definition of the element.
     * The description may assist in understanding the element scope and application.
     */
    @Override
    public Optional<InternationalString> getDescription() {
        return source.getDescription();
    }

    /**
     * If this instance is deprecated, the reason or the alternative to use.
     */
    @Override
    public InternationalString getRemarks() {
        return source.getRemarks();
    }

    /**
     * Returns {@code true} if this instance is deprecated.
     */
    @Override
    public boolean isDeprecated() {
        return source.isDeprecated();
    }

    /**
     * Returns a description of the input parameters.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return source.getParameters();
    }

    /**
     * Returns the expected result type.
     */
    @Override
    public IdentifiedType getResult() {
        return source.getResult();
    }

    /**
     * Executes the operation on the specified feature with the specified parameters.
     * This method is not strictly compliant with the contract of the public interface,
     * because it requires that the given feature is an instance of {@link #valueType}.
     * If not the case, this method tries to produce a more helpful exception message.
     *
     * @param  instance    the feature instance on which to execute the operation.
     *                     Can be {@code null} if the operation does not need feature instance.
     * @param  parameters  the parameters to use for executing the operation.
     *                     Can be {@code null} if the operation does not take any parameters.
     * @return the operation result.
     */
    @Override
    public Property apply(final Feature instance, final ParameterValueGroup parameters) {
        try {
            return source.apply(instance, parameters);
        } catch (PropertyNotFoundException e) {
            throw new FeatureOperationException(Resources.format(
                    Resources.Keys.IllegalFeatureType_4, 1, getName(), valueType, instance.getType().getName()), e);
        }
    }

    /**
     * Returns a string representation of this operation.
     */
    @Override
    public String toString() {
        return source.toString();
    }

    /**
     * Returns a hash code value for this operation.
     */
    @Override
    public int hashCode() {
        return source.hashCode() * 37;
    }

    /**
     * Compares this operation with the given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof OperationView) && source.equals(((OperationView) obj).source);
    }
}
