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
import java.util.Set;
import java.util.Objects;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.util.Classes;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;


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
 * The value is computed, or the operation is executed, by {@code apply(Feature, ParameterValueGroup)}.
 * If the value is modifiable, new value can be set by call to {@code Attribute.setValue(Object)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see DefaultFeatureType
 *
 * @since 0.6
 */
public abstract class AbstractOperation extends AbstractIdentifiedType
        implements BiFunction<AbstractFeature, ParameterValueGroup, Object>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -179930765502963170L;

    /**
     * The prefix for result identification entries in the {@code identification} map.
     * This prefix is documented in {@link FeatureOperations} javadoc.
     */
    static final String RESULT_PREFIX = "result.";

    /**
     * Constructs an operation from the given properties. The identification map is given unchanged to
     * the {@linkplain AbstractIdentifiedType#AbstractIdentifiedType(Map) super-class constructor}.
     * The following table is a reminder of main (not all) recognized map entries:
     *
     * <table class="sis">
     *   <caption>Recognized map entries (non exhaustive list)</caption>
     *   <tr>
     *     <th>Map key</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#NAME_KEY}</td>
     *     <td>{@link GenericName} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEFINITION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDefinition()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESIGNATION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDesignation()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DESCRIPTION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr><tr>
     *     <td>{@value org.apache.sis.feature.AbstractIdentifiedType#DEPRECATED_KEY}</td>
     *     <td>{@link Boolean}</td>
     *     <td>{@link #isDeprecated()}</td>
     *   </tr>
     * </table>
     *
     * @param  identification  the name and other information to be given to this operation.
     */
    public AbstractOperation(final Map<String,?> identification) {
        super(identification);
    }

    /**
     * Returns a map that can be used for creating the {@link #getResult()} type.
     * This method can be invoked for subclass constructor with the user supplied map in argument.
     * If the given map contains at least one key prefixed by {@value #RESULT_PREFIX}, then the values
     * associated to those keys will be used.
     *
     * @param  identification  the map given by user to sub-class constructor.
     */
    final Map<String,Object> resultIdentification(final Map<String,?> identification) {
        final var properties = new HashMap<String,Object>(6);
        for (final Map.Entry<String,?> entry : identification.entrySet()) {
            final String key = entry.getKey();
            if (key != null && key.startsWith(RESULT_PREFIX)) {
                properties.put(key.substring(RESULT_PREFIX.length()), entry.getValue());
            }
        }
        if (properties.isEmpty()) {
            properties.put(NAME_KEY,        super.getName());           // Do not invoke user-overrideable method.
            properties.put(DEFINITION_KEY,  super.getDefinition());
            super.getDesignation().ifPresent((i18n) -> properties.put(DESIGNATION_KEY, i18n));
            super.getDescription().ifPresent((i18n) -> properties.put(DESCRIPTION_KEY, i18n));
        }
        return properties;
    }

    /**
     * Returns a description of the input parameters.
     *
     * @return description of the input parameters.
     */
    public abstract ParameterDescriptorGroup getParameters();

    /**
     * Returns the expected result type, or {@code null} if none.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.IdentifiedType}. This change is pending GeoAPI revision.</div>
     *
     * @return the type of the result, or {@code null} if none.
     */
    public abstract AbstractIdentifiedType getResult();

    /**
     * Executes the operation on the specified feature with the specified parameters.
     * The value returned by this method depends on the value returned by {@link #getResult()}:
     *
     * <ul>
     *   <li>If {@code getResult()} returns {@code null},
     *       then this method should return {@code null}.</li>
     *   <li>If {@code getResult()} returns an instance of {@code AttributeType},
     *       then this method shall return an instance of {@code Attribute}
     *       and the {@code Attribute.getType() == getResult()} relation should hold.</li>
     *   <li>If {@code getResult()} returns an instance of {@code FeatureAssociationRole},
     *       then this method shall return an instance of {@code FeatureAssociation}
     *       and the {@code FeatureAssociation.getRole() == getResult()} relation should hold.</li>
     * </ul>
     *
     * <div class="note"><b>Analogy:</b>
     * if we compare {@code Operation} to {@link java.lang.reflect.Method} in the Java language, then this method is equivalent
     * to {@link java.lang.reflect.Method#invoke(Object, Object...)}. The {@code Feature} argument is equivalent to {@code this}
     * in the Java language, and may be {@code null} if the operation does not need a feature instance
     * (like static methods in the Java language).</div>
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the parameter type and return value may
     * be changed to {@code org.opengis.feature.Feature} and {@code org.opengis.feature.Property} respectively.
     * This change is pending GeoAPI revision.</div>
     *
     * @param  feature     the feature on which to execute the operation.
     *                     Can be {@code null} if the operation does not need feature instance.
     * @param  parameters  the parameters to use for executing the operation.
     *                     Can be {@code null} if the operation does not take any parameters.
     * @return the operation result, or {@code null} if this operation does not produce any result.
     */
    @Override
    public abstract Object apply(AbstractFeature feature, ParameterValueGroup parameters);

    /**
     * Returns the names of feature properties that this operation needs for performing its task.
     * This method does not resolve transitive dependencies, i.e. if a dependency is itself an operation having
     * other dependencies, the returned set will contain the name of that operation but not the names of the
     * dependencies of that operation (unless they are the same that the direct dependencies of {@code this}).
     *
     * <h4>Purpose</h4>
     * This information is needed for writing the {@code SELECT} SQL statement to send to a database server.
     * The requested columns will typically be all attributes declared in a {@code FeatureType}, but also
     * any additional columns needed for the operation while not necessarily included in the {@code FeatureType}.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns an empty set.
     *
     * @return the names of feature properties needed by this operation for performing its task.
     */
    public Set<String> getDependencies() {
        return Set.of();
    }

    /**
     * Returns the same operation but using different properties as inputs.
     * The keys in the given map should be values returned by {@link #getDependencies()},
     * and the associated values shall be the properties to use instead of the current dependencies.
     * If any key in the given map is not a member of the {@linkplain #getDependencies() dependency set},
     * then the entry is ignored. Conversely, if any member of the dependency set is not contained in the
     * given map, then the associated dependency is unchanged.
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.Operation}. This change is pending GeoAPI revision.</div>
     *
     * <h4>Purpose</h4>
     * This method is needed by {@link org.apache.sis.feature.builder.FeatureTypeBuilder} when some properties
     * are operations inherited from another feature type. Even if the dependencies are properties of the same
     * name, some {@link DefaultAttributeType#characteristics() characteristics} may be different.
     * For example, the <abbr>CRS</abbr> may change as a result of a change of <abbr>CRS</abbr>.
     *
     * <h4>Default implementation</h4>
     * The default implementation returns {@code this}.
     * This is consistent with the default implementation of {@link #getDependencies()} returning an empty set.
     *
     * @param  dependencies  the new properties to use as operation inputs.
     * @return the new operation, or {@code this} if unchanged.
     *
     * @see #INHERIT_FROM_KEY
     *
     * @since 1.5
     */
    public AbstractOperation updateDependencies(final Map<String, AbstractIdentifiedType> dependencies) {
        return this;
    }

    /**
     * Returns a hash code value for this operation.
     * The default implementation computes a hash code from the {@linkplain #getParameters() parameters descriptor}
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
     * The default implementation compares the {@linkplain #getParameters() parameters descriptor}
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
            final var that = (AbstractOperation) obj;
            return Objects.equals(getParameters(), that.getParameters()) &&
                   Objects.equals(getResult(),     that.getResult());
        }
        return false;
    }

    /**
     * Returns a string representation of this operation.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return a string representation of this operation for debugging purpose.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder(40).append(Classes.getShortClassName(this)).append('[');
        final GenericName name = getName();
        if (name != null) {
            buffer.append('“');
        }
        buffer.append(name);
        if (name != null) {
            buffer.append('”');
        }
        final AbstractIdentifiedType result = getResult();
        if (result != null) {
            final Object type;
            if (result instanceof DefaultAttributeType<?>) {
                type = Classes.getShortName(((DefaultAttributeType<?>) result).getValueClass());
            } else {
                type = result.getName();
            }
            buffer.append(" : ").append(type);
        }
        try {
            formatResultFormula(buffer.append("] = "));
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we write in a StringBuilder.
        }
        return buffer.toString();
    }

    /**
     * Appends a string representation of the "formula" used for computing the result.
     * The "formula" may be for example a link to another property.
     *
     * @param  buffer  where to format the "formula".
     * @throws IOException if an error occurred while writing in {@code buffer}.
     */
    void formatResultFormula(Appendable buffer) throws IOException {
        defaultFormula(getParameters(), buffer);
    }

    /**
     * Default implementation of {@link #formatResultFormula(Appendable)},
     * to be used also for operations that are not instance of {@link AbstractOperation}.
     */
    static void defaultFormula(final ParameterDescriptorGroup parameters, final Appendable buffer) throws IOException {
        buffer.append(parameters != null ? name(parameters.getName()) : "operation").append('(');
        if (parameters != null) {
            boolean hasMore = false;
            for (GeneralParameterDescriptor p : parameters.descriptors()) {
                if (p != null) {
                    if (hasMore) buffer.append(", ");
                    buffer.append(name(p.getName()));
                    hasMore = true;
                }
            }
        }
        buffer.append(')');
    }

    /**
     * Returns a short string representation of the given identifier, or {@code null} if none.
     */
    private static String name(final Identifier id) {
        return (id != null) ? id.getCode() : null;
    }

    /**
     * Creates a parameter descriptor in the Apache SIS namespace. This convenience method shall not
     * be in public <abbr>API</abbr>, because users should define operations in their own namespace.
     *
     * @param  name        the parameter group name, typically the same as operation name.
     * @param  parameters  the parameters, or an empty array if none.
     * @return description of the parameters group.
     */
    static ParameterDescriptorGroup parameters(final String name, final ParameterDescriptor<?>... parameters) {
        final var properties = new HashMap<String,Object>(4);
        properties.put(ParameterDescriptorGroup.NAME_KEY, name);
        properties.put(Identifier.AUTHORITY_KEY, Citations.SIS);
        return new DefaultParameterDescriptorGroup(properties, 1, 1);
    }
}
