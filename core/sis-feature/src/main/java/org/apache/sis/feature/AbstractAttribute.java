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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InvalidObjectException;
import java.io.IOException;
import org.opengis.util.GenericName;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.JDK7;


/**
 * An instance of an {@linkplain DefaultAttributeType attribute type} containing the value of an attribute in a feature.
 * {@code Attribute} holds three main information:
 *
 * <ul>
 *   <li>A {@linkplain #getType() reference to an attribute type}
 *       which define the base Java type and domain of valid values.</li>
 *   <li>One or more {@linkplain #getValues() values}, which may be a singleton ([0 … 1] cardinality)
 *       or multi-valued ([0 … ∞] cardinality).</li>
 *   <li>Optional {@linkplain #characteristics() characteristics} about the attribute
 *       (e.g. a <var>temperature</var> attribute may have a characteristic holding the measurement <var>accuracy</var>).
 *       Characteristics are often, but not necessarily, constant for all attributes of the same type in a dataset.</li>
 * </ul>
 *
 * {@code AbstractAttribute} can be instantiated by calls to {@link DefaultAttributeType#newInstance()}.
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li><b>Multi-threading:</b> {@code AbstractAttribute} instances are <strong>not</strong> thread-safe.
 *       Synchronization, if needed, shall be done externally by the caller.</li>
 *   <li><b>Serialization:</b> serialized objects of this class are not guaranteed to be compatible with future
 *       versions. Serialization should be used only for short term storage or RMI between applications running
 *       the same SIS version.</li>
 * </ul>
 *
 * @param <V> The type of attribute values.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 *
 * @see DefaultAttributeType#newInstance()
 */
public abstract class AbstractAttribute<V> extends Field<V> implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7442739120526654676L;

    /**
     * Information about the attribute (base Java class, domain of values, <i>etc.</i>).
     */
    final DefaultAttributeType<V> type;

    /**
     * Other attributes that describes this attribute, or {@code null} if not yet created.
     *
     * <div class="note"><b>Design note:</b>
     * We could question if it is a good idea to put this field here, given that this field add a slight cost
     * to all attribute implementations while only a small fraction of them will want attribute characteristics.
     * Since attributes may exist in a very large amount, that question may be significant.
     * However {@link AbstractFeature} tries hard to not create {@code Attribute} instances at all (it tries to
     * store only their value instead), so we presume that peoples who ask for {@code Attribute} instances are
     * willing to accept their cost.</div>
     *
     * @see #characteristics()
     */
    private transient Map<String,AbstractAttribute<?>> characteristics;

    /**
     * Creates a new attribute of the given type.
     *
     * @param type  information about the attribute (base Java class, domain of values, <i>etc.</i>).
     *
     * @see #create(DefaultAttributeType)
     */
    protected AbstractAttribute(final DefaultAttributeType<V> type) {
        this.type = type;
    }

    /**
     * Creates a new attribute of the given type initialized to the
     * {@linkplain DefaultAttributeType#getDefaultValue() default value}.
     *
     * @param  <V>   the type of attribute values.
     * @param  type  information about the attribute (base Java class, domain of values, <i>etc.</i>).
     * @return the new attribute.
     *
     * @see DefaultAttributeType#newInstance()
     */
    public static <V> AbstractAttribute<V> create(final DefaultAttributeType<V> type) {
        ArgumentChecks.ensureNonNull("type", type);
        return isSingleton(type.getMaximumOccurs())
               ? new SingletonAttribute<V>(type)
               : new MultiValuedAttribute<V>(type);
    }

    /**
     * Creates a new attribute of the given type initialized to the given value.
     * Note that a {@code null} value may not be the same as the default value.
     *
     * @param  <V>    the type of attribute values.
     * @param  type   information about the attribute (base Java class, domain of values, <i>etc.</i>).
     * @param  value  the initial value (may be {@code null}).
     * @return The new attribute.
     */
    static <V> AbstractAttribute<V> create(final DefaultAttributeType<V> type, final Object value) {
        ArgumentChecks.ensureNonNull("type", type);
        return isSingleton(type.getMaximumOccurs())
               ? new SingletonAttribute<V>(type, value)
               : new MultiValuedAttribute<V>(type, value);
    }

    /**
     * Invoked on serialization for saving the {@link #characteristics} field.
     *
     * @param  out  the output stream where to serialize this attribute.
     * @throws IOException if an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        final AbstractAttribute<?>[] characterizedBy;
        if (characteristics instanceof CharacteristicMap) {
            characterizedBy = characteristics.values().toArray(new AbstractAttribute<?>[characteristics.size()]);
        } else {
            characterizedBy = null;
        }
        out.writeObject(characterizedBy);
    }

    /**
     * Invoked on deserialization for restoring the {@link #characteristics} field.
     *
     * @param  in  the input stream from which to deserialize an attribute.
     * @throws IOException if an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException if the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            final AbstractAttribute<?>[] characterizedBy = (AbstractAttribute<?>[]) in.readObject();
            if (characterizedBy != null) {
                characteristics = newCharacteristicsMap();
                characteristics.values().addAll(Arrays.asList(characterizedBy));
            }
        } catch (RuntimeException e) { // At least ClassCastException, NullPointerException, IllegalArgumentException and IllegalStateException.
            throw (IOException) new InvalidObjectException(e.getMessage()).initCause(e);
        }
    }

    /**
     * Returns the name of this attribute as defined by its {@linkplain #getType() type}.
     * This convenience method delegates to {@link DefaultAttributeType#getName()}.
     *
     * @return the attribute name specified by its type.
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
     * @return information about the attribute.
     */
    public DefaultAttributeType<V> getType() {
        return type;
    }

    /**
     * Returns the attribute value, or {@code null} if none. This convenience method can be invoked in
     * the common case where the {@linkplain DefaultAttributeType#getMaximumOccurs() maximum number}
     * of attribute values is restricted to 1 or 0.
     *
     * @return the attribute value (may be {@code null}).
     * @throws IllegalStateException if this attribute contains more than one value.
     *
     * @see AbstractFeature#getPropertyValue(String)
     */
    @Override
    public abstract V getValue() throws IllegalStateException;

    /**
     * Returns all attribute values, or an empty collection if none.
     * The returned collection is <cite>live</cite>: changes in the returned collection
     * will be reflected immediately in this {@code Attribute} instance, and conversely.
     *
     * <p>The default implementation returns a collection which will delegate its work to
     * {@link #getValue()} and {@link #setValue(Object)}.</p>
     *
     * @return the attribute values in a <cite>live</cite> collection.
     */
    @Override
    public Collection<V> getValues() {
        return super.getValues();
    }

    /**
     * Sets the attribute value. All previous values are replaced by the given singleton.
     *
     * <div class="section">Validation</div>
     * The amount of validation performed by this method is implementation dependent.
     * Usually, only the most basic constraints are verified. This is so for performance reasons
     * and also because some rules may be temporarily broken while constructing a feature.
     * A more exhaustive verification can be performed by invoking the {@link #quality()} method.
     *
     * @param  value  the new value, or {@code null} for removing all values from this attribute.
     * @throws IllegalArgumentException if this method verifies argument validity and the given value
     *         does not met the attribute constraints.
     *
     * @see AbstractFeature#setPropertyValue(String, Object)
     */
    @Override
    public abstract void setValue(final V value) throws IllegalArgumentException;

    /**
     * Sets the attribute values. All previous values are replaced by the given collection.
     *
     * <p>The default implementation ensures that the given collection contains at most one element,
     * then delegates to {@link #setValue(Object)}.</p>
     *
     * @param  values  the new values.
     * @throws IllegalArgumentException if the given collection contains too many elements.
     */
    @Override
    public void setValues(final Collection<? extends V> values) throws IllegalArgumentException {
        super.setValues(values);
    }

    /**
     * Other attributes that describes this attribute. For example if this attribute carries a measurement,
     * then a characteristic of this attribute could be the measurement accuracy.
     * See <cite>"Attribute characterization"</cite> in {@link DefaultAttributeType} Javadoc for more information.
     *
     * <p>The map returned by this method contains only the characteristics explicitely defined for this attribute.
     * If the map contains no characteristic for a given name, a {@linkplain DefaultAttributeType#getDefaultValue()
     * default value} may still exist.
     * In such cases, callers may also need to inspect the {@link DefaultAttributeType#characteristics()}
     * as shown in the <cite>Reading a characteristic</cite> section below.</p>
     *
     * <div class="note"><b>Rational:</b>
     * Very often, all attributes of a given type in the same dataset have the same characteristics.
     * For example it is very common that all temperature measurements in a dataset have the same accuracy,
     * and setting a different accuracy for a single measurement is relatively rare.
     * Consequently, {@code characteristics.isEmpty()} is a convenient way to check that an attribute have
     * all the "standard" characteristics and need no special processing.</div>
     *
     * <div class="section">Reading a characteristic</div>
     * The characteristic values are enumerated in the {@linkplain Map#values() map values}.
     * The {@linkplain Map#keySet() map keys} are the {@code String} representations of characteristics
     * {@linkplain DefaultAttributeType#getName() name}, for more convenient lookups.
     *
     * <p>If an attribute is known to be a measurement with a characteristic named "accuracy"
     * of type {@link Float}, then the accuracy value could be read as below:</p>
     *
     * {@preformat java
     *     Float getAccuracy(Attribute<?> measurement) {
     *         Attribute<?> accuracy = measurement.characteristics().get("accuracy");
     *         if (accuracy != null) {
     *             return (Float) accuracy.getValue(); // Value may be null.
     *         } else {
     *             return (Float) measurement.getType().characteristics().get("accuracy").getDefaultValue();
     *             // A more sophisticated implementation would probably cache the default value somewhere.
     *         }
     *     }
     * }
     *
     * <div class="section">Adding a characteristic</div>
     * A new characteristic can be added in the map in three different ways:
     * <ol>
     *   <li>Putting the (<var>name</var>, <var>characteristic</var>) pair explicitely.
     *     If an older characteristic existed for that name, it will be replaced.
     *     Example:
     *
     *     {@preformat java
     *       Attribute<?> accuracy = ...; // To be created by the caller.
     *       characteristics.put("accuracy", accuracy);
     *     }</li>
     *
     *   <li>Adding the new characteristic to the {@linkplain Map#values() values} collection.
     *     The name is inferred automatically from the characteristic type.
     *     If an older characteristic existed for the same name, an {@link IllegalStateException} will be thrown.
     *     Example:
     *
     *     {@preformat java
     *       Attribute<?> accuracy = ...; // To be created by the caller.
     *       characteristics.values().add(accuracy);
     *     }</li>
     *
     *   <li>Adding the characteristic name to the {@linkplain Map#keySet() key set}.
     *     If no characteristic existed for that name, a default one will be created.
     *     Example:
     *
     *     {@preformat java
     *       characteristics.keySet().add("accuracy"); // Ensure that an entry will exist for that name.
     *       Attribute<?> accuracy = characteristics.get("accuracy");
     *       Features.cast(accuracy, Float.class).setValue(...); // Set new accuracy value here as a float.
     *     }</li>
     * </ol>
     *
     * @return other attribute types that describes this attribute type, or an empty map if none.
     *
     * @see DefaultAttributeType#characteristics()
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String,AbstractAttribute<?>> characteristics() {
        if (characteristics == null) {
            characteristics = newCharacteristicsMap();
        }
        return characteristics;                                 // Intentionally modifiable
    }

    /**
     * Creates an initially empty map of characteristics for this attribute.
     * This method does not store the new map in the {@link #characteristics} field;
     * it is caller responsibility to do so if desired.
     */
    private Map<String,AbstractAttribute<?>> newCharacteristicsMap() {
        if (type instanceof DefaultAttributeType<?>) {
            Map<String, DefaultAttributeType<?>> map = type.characteristics();
            if (!map.isEmpty()) {
                if (!(map instanceof CharacteristicTypeMap)) {
                    final Collection<DefaultAttributeType<?>> types = map.values();
                    map = CharacteristicTypeMap.create(type, types.toArray(new DefaultAttributeType<?>[types.size()]));
                }
                return new CharacteristicMap(this, (CharacteristicTypeMap) map);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the characteristics, or an empty map if the characteristics have not yet been built.
     * Contrarily to {@link #characteristics()}, this method does not create the map. This method
     * is suitable when then caller only wants to read the map and does not plan to write anything.
     */
    final Map<String,AbstractAttribute<?>> characteristicsReadOnly() {
        return (characteristics != null) ? characteristics : Collections.<String,AbstractAttribute<?>>emptyMap();
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
     * @return reports on all constraint violations found.
     *
     * @see AbstractFeature#quality()
     */
    public DataQuality quality() {
        final Validator v = new Validator(ScopeCode.ATTRIBUTE);
        v.validate(type, getValues());
        return v.quality;
    }

    /**
     * Returns a string representation of this attribute.
     * The returned string is for debugging purpose and may change in any future SIS version.
     * The current implementation is like below:
     *
     * {@preformat text
     *     Attribute[“temperature” : Float] = {20.3, 17.8, 21.1}
     *     └─ characteristics: units=°C, accuracy=0.1
     * }
     *
     * @return a string representation of this attribute for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        final StringBuilder buffer = FieldType.toString("Attribute", type,
                Classes.getShortName(type.getValueClass()), getValues().iterator());
        if (characteristics != null && !characteristics.isEmpty()) {
            buffer.append(JDK7.lineSeparator());
            String separator = "└─ characteristics: ";
            for (final Map.Entry<String,AbstractAttribute<?>> entry : characteristics.entrySet()) {
                buffer.append(separator).append(entry.getKey()).append('=').append(entry.getValue().getValue());
                separator = ", ";
            }
        }
        return buffer.toString();
    }

    /**
     * Returns a copy of this attribute.
     * The default implementation returns a <em>shallow</em> copy:
     * the attribute {@linkplain #getValue() value} and {@linkplain #characteristics() characteristics}
     * are <strong>not</strong> cloned.
     * However subclasses may choose to do otherwise.
     *
     * @return a clone of this attribute.
     * @throws CloneNotSupportedException if this attribute, the {@linkplain #getValue() value}
     *         or one of its {@linkplain #characteristics() characteristics} can not be cloned.
     */
    @Override
    @SuppressWarnings("unchecked")
    public AbstractAttribute<V> clone() throws CloneNotSupportedException {
        final AbstractAttribute<V> clone = (AbstractAttribute<V>) super.clone();
        final Map<String,AbstractAttribute<?>> c = clone.characteristics;
        if (c instanceof CharacteristicMap) {
            clone.characteristics = ((CharacteristicMap) c).clone();
        }
        return clone;
    }
}
