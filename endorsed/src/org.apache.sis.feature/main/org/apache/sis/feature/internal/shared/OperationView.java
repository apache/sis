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
import org.apache.sis.feature.internal.Resources;

// Specific to the main branch:
import java.util.Map;
import java.util.Set;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.AbstractIdentifiedType;


/**
 * An operation wrapper for hiding its dependencies.
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
final class OperationView extends AbstractOperation {
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
        super(Map.of(INHERIT_FROM_KEY, source));
        this.source = source;
        this.valueType = valueType;
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
    public AbstractIdentifiedType getResult() {
        return source.getResult();
    }

    /**
     * Returning an empty set here is the whole purpose of this class.
     */
    @Override
    public Set<String> getDependencies() {
        return Set.of();
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
    public Object apply(final AbstractFeature instance, final ParameterValueGroup parameters) {
        try {
            return source.apply(instance, parameters);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(Resources.format(
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
