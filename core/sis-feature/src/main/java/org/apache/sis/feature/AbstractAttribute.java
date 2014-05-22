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

import java.util.Collection;
import java.util.Iterator;
import java.io.Serializable;
import org.opengis.util.GenericName;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * An instance of an {@linkplain DefaultAttributeType attribute type} containing the value of an attribute in a feature.
 * {@code Attribute} holds two main information:
 *
 * <ul>
 *   <li>A reference to an {@linkplain DefaultAttributeType attribute type}
 *       which define the base Java type and domain of valid values.</li>
 *   <li>A value, which may be a singleton ([0 … 1] cardinality) or multi-valued ([0 … ∞] cardinality).</li>
 * </ul>
 *
 * {@section Limitations}
 * <ul>
 *   <li><b>Multi-threading:</b> {@code AbstractAttribute} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> serialized objects of this class are not guaranteed to be compatible with future
 *       versions. Serialization should be used only for short term storage or RMI between applications running
 *       the same SIS version.</li>
 * </ul>
 *
 * @param <T> The type of attribute values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see DefaultAttributeType
 */
public abstract class AbstractAttribute<T> extends Property implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7442739120526654676L;

    /**
     * Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    final DefaultAttributeType<T> type;

    /**
     * Creates a new attribute of the given type.
     *
     * @param type Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    public AbstractAttribute(final DefaultAttributeType<T> type) {
        ArgumentChecks.ensureNonNull("type", type);
        this.type = type;
    }

    /**
     * Returns the name of this attribute as defined by its {@linkplain #getType() type}.
     * This convenience method delegates to {@link DefaultAttributeType#getName()}.
     *
     * @return The attribute name specified by its type.
     */
    @Override
    public GenericName getName() {
        return type.getName();
    }

    /**
     * Returns information about the attribute (base Java class, domain of values, <i>etc.</i>).
     *
     * <div class="warning"><b>Warning:</b> In a future SIS version, the return type may be changed
     * to {@code org.opengis.feature.AttributeType}. This change is pending GeoAPI revision.</div>
     *
     * @return Information about the attribute.
     */
    public DefaultAttributeType<T> getType() {
        return type;
    }

    /**
     * Returns the attribute value, or {@code null} if none. This convenience method can be invoked in
     * the common case where the {@linkplain DefaultAttributeType#getMaximumOccurs() maximum number}
     * of attribute values is restricted to 1 or 0.
     *
     * @return The attribute value (may be {@code null}).
     * @throws IllegalStateException if this attribute contains more than one value.
     *
     * @see AbstractFeature#getPropertyValue(String)
     */
    public abstract T getValue() throws IllegalStateException;

    /**
     * Returns all attribute values, or an empty collection if none.
     * The returned collection is <cite>live</cite>: changes in the returned collection
     * will be reflected immediately in this {@code Attribute} instance, and conversely.
     *
     * <p>The default implementation returns a collection which will delegate its work to
     * {@link #getValue()} and {@link #setValue(Object)}.</p>
     *
     * @return The attribute values in a <cite>live</cite> collection.
     */
    public Collection<T> getValues() {
        return new PropertySingleton<>(this);
    }

    /**
     * Sets the attribute value. All previous values are replaced by the given singleton.
     *
     * {@section Validation}
     * The amount of validation performed by this method is implementation dependent.
     * Usually, only the most basic constraints are verified. This is so for performance reasons
     * and also because some rules may be temporarily broken while constructing a feature.
     * A more exhaustive verification can be performed by invoking the {@link #quality()} method.
     *
     * @param value The new value, or {@code null} for removing all values from this attribute.
     *
     * @see AbstractFeature#setPropertyValue(String, Object)
     */
    public abstract void setValue(final T value);

    /**
     * Set the attribute values. All previous values are replaced by the given collection.
     *
     * <p>The default implementation ensures that the given collection contains at most one element,
     * then delegates to {@link #setValue(Object)}.</p>
     *
     * @param values The new values.
     */
    public void setValues(final Collection<? extends T> values) {
        T value = null;
        ArgumentChecks.ensureNonNull("values", values);
        final Iterator<? extends T> it = values.iterator();
        if (it.hasNext()) {
            value = it.next();
            if (it.hasNext()) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.TooManyOccurrences_2, 1, getName()));
            }
        }
        setValue(value);
    }

    /**
     * Evaluates the quality of this attribute at this method invocation time. The data quality reports
     * may include information about whether the attribute value mets the constraints defined by the
     * {@linkplain DefaultAttributeType attribute type}, or any other criterion at implementation choice.
     *
     * <p>The default implementation reports data quality with at least the following information:</p>
     * <ul>
     *   <li>
     *     The {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getScope() scope}
     *     {@linkplain org.apache.sis.metadata.iso.quality.DefaultScope#getLevel() level} is set to
     *     {@link org.opengis.metadata.maintenance.ScopeCode#ATTRIBUTE}.
     *   </li><li>
     *     At most one {@linkplain org.apache.sis.metadata.iso.quality.DefaultDomainConsistency domain consistency}
     *     element is added to the {@linkplain org.apache.sis.metadata.iso.quality.DefaultDataQuality#getReports()
     *     reports} list (implementations are free to omit that element if they have nothing to report).
     *     If a report is provided, then it will contain at least the following information:
     *     <ul>
     *       <li>
     *         <p>The {@linkplain #getName() attribute name} as the data quality
     *         {@linkplain org.apache.sis.metadata.iso.quality.DefaultDomainConsistency#getMeasureIdentification()
     *         measure identification}.</p>
     *
     *         <div class="note"><b>Note:</b> strictly speaking, {@code measureIdentification} identifies the
     *         <em>quality measurement</em>, not the “real” measurement itself. However this implementation
     *         uses the same set of identifiers for both for simplicity.</div>
     *       </li><li>
     *         <p>If the attribute {@linkplain #getValue() value} is not an {@linkplain Class#isInstance instance}
     *         of the expected {@linkplain DefaultAttributeType#getValueClass() value class}, or if the number
     *         of occurrences is not inside the cardinality range, or if any other constraint is violated, then
     *         a {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult conformance result} is
     *         added for each violation with an
     *         {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult#getExplanation() explanation}
     *         set to the error message.</p>
     *
     *         <div class="warning"><b>Note:</b> this is a departure from ISO intend, since {@code explanation}
     *         should be a statement about what a successful conformance means. This point may be reformulated
     *         in a future SIS version.</div>
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * This attribute is valid if this method does not report any
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult conformance result} having a
     * {@linkplain org.apache.sis.metadata.iso.quality.DefaultConformanceResult#pass() pass} value of {@code false}.
     *
     * <div class="note"><b>Example:</b> given an attribute named “population” with [1 … 1] cardinality,
     * if no value has been assigned to that attribute, then this {@code quality()} method will return
     * the following data quality report:
     *
     * {@preformat text
     *   Data quality
     *     ├─Scope
     *     │   └─Level………………………………………………… Attribute
     *     └─Report
     *         ├─Measure identification
     *         │   └─Code………………………………………… population
     *         ├─Evaluation method type…… Direct internal
     *         └─Result
     *             ├─Explanation……………………… Missing value for “population” property.
     *             └─Pass………………………………………… false
     * }
     * </div>
     *
     * @return Reports on all constraint violations found.
     *
     * @see AbstractFeature#quality()
     */
    public DataQuality quality() {
        final Validator v = new Validator(ScopeCode.ATTRIBUTE);
        v.validate(type, getValue());
        return v.quality;
    }

    /**
     * Returns a string representation of this attribute.
     * The returned string is for debugging purpose and may change in any future SIS version.
     *
     * @return A string representation of this attribute for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return type.toString("Attribute", Classes.getShortName(type.getValueClass()), getValues().iterator());
    }
}
