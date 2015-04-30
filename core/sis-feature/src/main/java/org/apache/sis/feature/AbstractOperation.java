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
package org.apache.sis.feature;

import java.util.Map;
import org.opengis.util.GenericName;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.Debug;

// Branch-dependent imports
import java.util.Objects;
import java.util.function.BiFunction;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureAssociation;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Operation;
import org.opengis.feature.Property;


/**
 * Describes the behaviour of a feature type as a function or a method.
 * Operations can:
 *
 * <ul>
 *   <li>Compute values from the attributes.</li>
 *   <li>Perform actions that change the attribute values.</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b> a mutator operation may raise the height of a dam. This changes
 * may affect other properties like the watercourse and the reservoir associated with the dam.</div>
 *
 * The value is computed, or the operation is executed, by {@link #apply(Feature, ParameterValueGroup)}.
 * If the value is modifiable, new value can be set by call to {@link Attribute#setValue(Object)}.
 *
 * <div class="warning"><b>Warning:</b> this class is experimental and may change after we gained more
 * experience on this aspect of ISO 19109.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public abstract class AbstractOperation extends AbstractIdentifiedType implements Operation,
        BiFunction<Feature, ParameterValueGroup, Property>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -179930765502963170L;

    /**
     * Constructs an operation from the given properties. The identification map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     *
     * @param identification The name and other information to be given to this operation.
     */
    public AbstractOperation(final Map<String,?> identification) {
        super(identification);
    }

    /**
     * Returns a description of the input parameters.
     *
     * @return Description of the input parameters.
     */
    @Override
    public abstract ParameterDescriptorGroup getParameters();

    /**
     * Returns the expected result type, or {@code null} if none.
     *
     * @return The type of the result, or {@code null} if none.
     */
    @Override
    public abstract IdentifiedType getResult();

    /**
     * Executes the operation on the specified feature with the specified parameters.
     * The value returned by this method depends on the value returned by {@link #getResult()}:
     *
     * <ul>
     *   <li>If {@code getResult()}} returns {@code null},
     *       then this method should return {@code null}.</li>
     *   <li>If {@code getResult()}} returns an instance of {@link AttributeType},
     *       then this method shall return an instance of {@link Attribute}
     *       and the {@code Attribute.getType() == getResult()} relation should hold.</li>
     *   <li>If {@code getResult()}} returns an instance of {@link FeatureAssociationRole},
     *       then this method shall return an instance of {@link FeatureAssociation}
     *       and the {@code FeatureAssociation.getRole() == getResult()} relation should hold.</li>
     * </ul>
     *
     * <div class="note"><b>Analogy:</b>
     * if we compare {@code Operation} to {@link Method} in the Java language, then this method is equivalent
     * to {@link Method#apply(Object, Object...)}. The {@code Feature} argument is equivalent to {@code this}
     * in the Java language, and may be {@code null} if the operation does not need a feature instance
     * (like static methods in the Java language).</div>
     *
     * @param  feature    The feature on which to execute the operation.
     *                    Can be {@code null} if the operation does not need feature instance.
     * @param  parameters The parameters to use for executing the operation.
     *                    Can be {@code null} if the operation does not take any parameters.
     * @return The operation result, or {@code null} if this operation does not produce any result.
     */
    @Override
    public abstract Property apply(Feature feature, ParameterValueGroup parameters);

    /**
     * Returns a hash code value for this operation.
     * The default implementation computes a hash code from the {@linkplain #getParameters() parameters}
     * and {@linkplain #getResult() result type}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hashCode(getParameters()) + Objects.hashCode(getResult());
    }

    /**
     * Compares this operation with the given object for equality.
     * The default implementation compares the {@linkplain #getParameters() parameters}
     * and {@linkplain #getResult() result type}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final AbstractOperation that = (AbstractOperation) obj;
            return Objects.equals(getParameters(), that.getParameters()) &&
                   Objects.equals(getResult(),     that.getResult());
        }
        return false;
    }

    /**
     * Returns a string representation of this operation.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this operation for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(40).append("Operation").append('[');
        final GenericName name = getName();
        if (name != null) {
            buffer.append('“');
        }
        buffer.append(name);
        if (name != null) {
            buffer.append('”');
        }
        String separator = " (";
        for (final GeneralParameterDescriptor param : getParameters().descriptors()) {
            buffer.append(separator).append(IdentifiedObjects.toString(param.getName()));
            separator = ", ";
        }
        if (separator == ", ") { // Identity comparaison is okay here.
            buffer.append(')');
        }
        final IdentifiedType result = getResult();
        if (result != null) {
            buffer.append(" : ").append(result.getName());
        }
        return buffer.append(']').toString();
    }
}
