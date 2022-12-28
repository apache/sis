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
package org.apache.sis.metadata.iso.quality;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.TypeName;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.metadata.Identifier;
import org.apache.sis.xml.Namespaces;

// Branch-dependent imports
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Data quality measure.
 * The following properties are mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQM_Measure}
 * {@code   ├─measureIdentifier……} Value uniquely identifying the measure within a namespace.
 * {@code   ├─name………………………………………} Name of the data quality measure applied to the data.
 * {@code   ├─elementName……………………} Name of the data quality element for which quality is reported.
 * {@code   ├─definition………………………} Definition of the fundamental concept for the data quality measure.
 * {@code   └─valueType…………………………} Value type for reporting a data quality result (shall be one of the data types defined in ISO/19103:2005).</div>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Alexis Gaillard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 */
@XmlType(name = "DQM_Measure_Type", namespace = Namespaces.DQM, propOrder = {
    "measureIdentifier",
    "name",
    "aliases",
    "elementNames",
    "definition",
    "description",
    "valueType",
    "examples",
    "basicMeasure",
    "sourceReferences",
    "parameters"
})
@XmlRootElement(name = "DQM_Measure", namespace = Namespaces.DQM)
@UML(identifier="DQM_Measure", specification=UNSPECIFIED)
public class DefaultQualityMeasure extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2004468907779670827L;

    /**
     * Value uniquely identifying the measure within a namespace.
     */
    @SuppressWarnings("serial")
    private Identifier measureIdentifier;

    /**
     * Name of the data quality measure applied to the data.
     */
    @SuppressWarnings("serial")
    private InternationalString name;

    /**
     * Another recognized name, an abbreviation or a short name for the same data quality measure.
     */
    @SuppressWarnings("serial")
    private Collection<InternationalString> aliases;

    /**
     * Name of the data quality element for which quality is reported.
     */
    @SuppressWarnings("serial")
    private Collection<TypeName> elementNames;

    /**
     * Definition of the fundamental concept for the data quality measure.
     */
    @SuppressWarnings("serial")
    private DefaultBasicMeasure basicMeasure;

    /**
     * Definition of the fundamental concept for the data quality measure.
     */
    @SuppressWarnings("serial")
    private InternationalString definition;

    /**
     * Description of the data quality measure.
     * Includes methods of calculation, with all formulae and/or illustrations
     * needed to establish the result of applying the measure.
     */
    @SuppressWarnings("serial")
    private DefaultMeasureDescription description;

    /**
     * Reference to the source of an item that has been adopted from an external source.
     */
    @SuppressWarnings("serial")
    private Collection<DefaultSourceReference> sourceReferences;

    /**
     * Value type for reporting a data quality result.
     */
    @SuppressWarnings("serial")
    private TypeName valueType;

    /**
     * Auxiliary variable used by the data quality measure, including its name, definition and optionally its description.
     */
    @SuppressWarnings("serial")
    private Collection<ParameterDescriptor<?>> parameters;

    /**
     * Illustration of the use of a data quality measure.
     */
    @SuppressWarnings("serial")
    private Collection<DefaultMeasureDescription> examples;

    /**
     * Constructs an initially empty element.
     */
    public DefaultQualityMeasure() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultQualityMeasure(final DefaultQualityMeasure object) {
        super(object);
        if (object != null) {
            measureIdentifier = object.getMeasureIdentifier();
            name              = object.getName();
            aliases           = copyCollection(object.getAliases(), InternationalString.class);
            elementNames      = copyCollection(object.getElementNames(), TypeName.class);
            definition        = object.getDefinition();
            description       = object.getDescription();
            valueType         = object.getValueType();
            examples          = copyCollection(object.getExamples(), DefaultMeasureDescription.class);
            basicMeasure      = object.getBasicMeasure();
            sourceReferences  = copyCollection(object.getSourceReferences(), DefaultSourceReference.class);
            parameters        = copyCollection(object.getParameters(), (Class) ParameterDescriptor.class);
        }
    }

    /**
     * Returns the value uniquely identifying the measure within a namespace.
     *
     * @return value uniquely identifying the measure within a namespace.
     */
    @XmlElement(name = "measureIdentifier", required = true)
    @UML(identifier="measureIdentifier", obligation=MANDATORY, specification=UNSPECIFIED)
    public Identifier getMeasureIdentifier() {
        return measureIdentifier;
    }

    /**
     * Sets the value uniquely identifying the measure within a namespace.
     *
     * @param  newValue  the new measure identification.
     */
    public void setMeasureIdentifier(final Identifier newValue)  {
        checkWritePermission(measureIdentifier);
        measureIdentifier = newValue;
    }

    /**
     * Returns the name of the data quality measure applied to the data.
     *
     * @return name of the data quality measure applied to the data.
     */
    @XmlElement(name = "name", required = true)
    @UML(identifier="name", obligation=MANDATORY, specification=UNSPECIFIED)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the data quality measure applied to the data.
     *
     * @param  newValue  the new quality measure name.
     */
    public void setName(final InternationalString newValue)  {
        checkWritePermission(name);
        name = newValue;
    }

    /**
     * Returns other recognized names, abbreviations or short names for the same data quality measure.
     * It may be a different commonly used name, or an abbreviation, or a short name.
     *
     * @return others recognized names, abbreviations or short names.
     */
    @XmlElement(name = "alias")
    @UML(identifier="alias", obligation=OPTIONAL, specification=UNSPECIFIED)
    public Collection<InternationalString> getAliases() {
        return aliases = nonNullCollection(aliases, InternationalString.class);
    }

    /**
     * Sets other recognized names or abbreviations for the same data quality measure.
     *
     * @param  newValues  the new measure aliases.
     */
    public void setAliases(final Collection<? extends InternationalString> newValues)  {
        aliases = writeCollection(newValues, aliases, InternationalString.class);
    }

    /**
     * Returns the names of the data quality element to which a measure applies.
     *
     * @return names of the data quality element for which quality is reported.
     */
    @XmlElement(name = "elementName", required = true)
    @UML(identifier="elementName", obligation=MANDATORY, specification=UNSPECIFIED)
    public Collection<TypeName> getElementNames() {
        return elementNames = nonNullCollection(elementNames, TypeName.class);
    }

    /**
     * Sets the name of the data quality element for which quality is reported.
     *
     * @param  newValues  the new measure element names.
     */
    public void setElementNames(final Collection<? extends TypeName> newValues)  {
        elementNames = writeCollection(newValues, elementNames, TypeName.class);
    }

    /**
     * Returns predefined basic measure on which this measure is based.
     *
     * @return predefined basic measure on which this measure is based, or {@code null} if none.
     */
    @XmlElement(name = "basicMeasure")
    @UML(identifier="basicMeasure", obligation=CONDITIONAL, specification=UNSPECIFIED)
    public DefaultBasicMeasure getBasicMeasure() {
        return basicMeasure;
    }

    /**
     * Sets the predefined basic measure on which this measure is based.
     *
     * @param  newValue  the new basic measure.
     */
    public void setBasicMeasure(final DefaultBasicMeasure newValue)  {
        checkWritePermission(basicMeasure);
        basicMeasure = newValue;
    }

    /**
     * Returns the definition of the fundamental concept for the data quality measure.
     * If the measure is derived from a {@linkplain #getBasicMeasure() basic measure},
     * the definition is based on the basic measure definition and specialized for this measure.
     *
     * @return definition of the fundamental concept for the data quality measure.
     */
    @XmlElement(name = "definition", required = true)
    @UML(identifier="definition", obligation=MANDATORY, specification=UNSPECIFIED)
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Sets the definition of the fundamental concept for the data quality measure.
     *
     * @param  newValue  the new measure definition.
     */
    public void setDefinition(final InternationalString newValue)  {
        checkWritePermission(definition);
        definition = newValue;
    }

    /**
     * Description of the data quality measure.
     * Includes methods of calculation, with all formulae and/or illustrations
     * needed to establish the result of applying the measure.
     *
     * @return description of data quality measure, or {@code null} if none.
     */
    @XmlElement(name = "description")
    @UML(identifier="description", obligation=CONDITIONAL, specification=UNSPECIFIED)
    public DefaultMeasureDescription getDescription() {
       return description;
    }

    /**
     * Sets the description of the data quality measure.
     *
     * @param  newValue  the new measure description.
     */
    public void setDescription(final DefaultMeasureDescription newValue)  {
        checkWritePermission(description);
        description = newValue;
    }

    /**
     * Returns references to the source of an item that has been adopted from an external source.
     *
     * @return references to the source.
     */
    @XmlElement(name = "sourceReference")
    @UML(identifier="sourceReference", obligation=CONDITIONAL, specification=UNSPECIFIED)
    public Collection<DefaultSourceReference> getSourceReferences() {
        return sourceReferences = nonNullCollection(sourceReferences, DefaultSourceReference.class);
    }

    /**
     * Sets the reference to the source of an item that has been adopted from an external source.
     *
     * @param  newValues  the new source references.
     */
    public void setSourceReferences(final Collection<? extends DefaultSourceReference> newValues) {
        sourceReferences = writeCollection(newValues, sourceReferences, DefaultSourceReference.class);
    }

    /**
     * Returns the value type for reporting a data quality result.
     *
     * @return value type for reporting a data quality result.
     */
    @XmlElement(name = "valueType", required = true)
    @UML(identifier="valueType", obligation=MANDATORY, specification=UNSPECIFIED)
    public TypeName getValueType() {
        return valueType;
    }

    /**
     * Sets the value type for reporting a data quality result.
     *
     * @param  newValue  the new measure value type.
     */
    public void setValueType(final TypeName newValue)  {
        checkWritePermission(valueType);
        valueType = newValue;
    }

    /**
     * Returns auxiliary variable(s) used by the data quality measure.
     * It shall include its name, definition and value type.
     *
     * <h4>Unified parameter API</h4>
     * In GeoAPI, the {@code DQM_Parameter} type defined by ISO 19157 is replaced by {@link ParameterDescriptor}
     * in order to provide a single parameter API. See {@link org.opengis.parameter} for more information.
     *
     * @return auxiliary variable(s) used by data quality measure.
     */
    @XmlElement(name = "parameter")
    @SuppressWarnings({"unchecked", "rawtypes"})
    @UML(identifier="parameter", obligation=CONDITIONAL, specification=UNSPECIFIED)
    public Collection<ParameterDescriptor<?>> getParameters() {
        return parameters = nonNullCollection(parameters, (Class) ParameterDescriptor.class);
    }

    /**
     * Sets the auxiliary variable used by the data quality measure.
     *
     * @param  newValues  the new measure parameters.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setParameters(final Collection<? extends ParameterDescriptor<?>> newValues) {
        parameters = writeCollection(newValues, parameters, (Class) ParameterDescriptor.class);
    }

    /**
     * Returns illustrations of the use of a data quality measure.
     *
     * @return examples of applying the measure or the result obtained for the measure.
     */
    @XmlElement(name = "example")
    @UML(identifier="example", obligation=OPTIONAL, specification=UNSPECIFIED)
    public Collection<DefaultMeasureDescription> getExamples() {
        return examples = nonNullCollection(examples, DefaultMeasureDescription.class);
    }

    /**
     * Sets the illustrations of the use of a data quality measure.
     *
     * @param  newValues  the new examples.
     */
    public void setExamples(final Collection<? extends DefaultMeasureDescription> newValues) {
        examples = writeCollection(newValues, examples, DefaultMeasureDescription.class);
    }
}
