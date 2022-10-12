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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.TypeName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.Parameter;
import org.opengis.metadata.quality.Description;
import org.opengis.metadata.quality.ValueStructure;
import org.apache.sis.xml.Namespaces;


/**
 * Data quality parameter.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQM_Parameter}
 * {@code   ├─name……………………} Name of the data quality parameter.
 * {@code   ├─definition……} Definition of the data quality parameter.
 * {@code   └─valueType………} Value type of the data quality parameter (shall be one of the data types defined in ISO/TS 19103:2005).</div>
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
 * @module
 */
@XmlType(name = "DQM_Parameter_Type", namespace = Namespaces.DQM, propOrder = {
    "name",
    "definition",
    "description",
    "valueType",
    "valueStructure"
})
@XmlRootElement(name = "DQM_Parameter", namespace = Namespaces.DQM)
public class DefaultParameter extends ISOMetadata implements Parameter {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5063371334668144677L;

    /**
     * Name of the data quality parameter.
     */
    @SuppressWarnings("serial")
    private InternationalString name;

    /**
     * Definition of the data quality parameter.
     */
    @SuppressWarnings("serial")
    private InternationalString definition;

     /**
     * Description of the data quality parameter.
     */
    @SuppressWarnings("serial")
    private Description description;

     /**
     * Value type of the data quality parameter (shall be one of the data types defined in ISO/TS 19103:2005).
     */
    @SuppressWarnings("serial")
    private TypeName valueType;

    /**
     * Structure of the data quality parameter.
     */
    private ValueStructure valueStructure;

    /**
     * Constructs an initially empty element.
     */
    public DefaultParameter() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Parameter)
     */
    public DefaultParameter(final Parameter object) {
        super(object);
        if (object != null) {
            name           = object.getName();
            definition     = object.getDefinition();
            description    = object.getDescription();
            valueType      = object.getValueType();
            valueStructure = object.getValueStructure();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultParameter}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultParameter} instance is created using the
     *       {@linkplain #DefaultParameter(Parameter) copy constructor} and returned.
     *       Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultParameter castOrCopy(final Parameter object) {
        if (object == null || object instanceof DefaultParameter) {
            return (DefaultParameter) object;
        }
        return new DefaultParameter(object);
    }

    /**
     * Returns the name of the data quality parameter.
     *
     * @return name of the data quality parameter.
     */
    @Override
    @XmlElement(name = "name", required = true)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the data quality parameter.
     *
     * @param  newValue  the new parameter name.
     */
    public void setName(final InternationalString newValue)  {
        checkWritePermission(name);
        name = newValue;
    }

    /**
     * Returns the definition of the data quality parameter.
     *
     * @return definition of the data quality parameter.
     */
    @Override
    @XmlElement(name = "definition", required = true)
    public InternationalString getDefinition() {
        return definition;
    }

    /**
     * Sets the definition of the data quality parameter.
     *
     * @param  newValue  the new parameter definition.
     */
    public void setDefinition(final InternationalString newValue)  {
        checkWritePermission(definition);
        definition = newValue;
    }

    /**
     * Returns the description of the data quality parameter.
     *
     * @return description of the data quality parameter, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "description")
    public Description getDescription() {
       return description;
    }

    /**
     * Sets the description of the data quality parameter.
     *
     * @param  newValue  the new parameter description.
     */
    public void setDescription(final Description newValue)  {
        checkWritePermission(description);
        description = newValue;
    }

    /**
     * Returns the value type of the data quality parameter.
     *
     * @return value type of the data quality parameter.
     */
    @Override
    @XmlElement(name = "valueType", required = true)
    public TypeName getValueType() {
        return valueType;
    }

    /**
     * Sets the value type of the data quality parameter.
     *
     * @param  newValue  the new parameter value type.
     */
    public void setValueType(final TypeName newValue)  {
        checkWritePermission(valueType);
        valueType = newValue;
    }

    /**
     * Returns the structure of the data quality parameter.
     *
     * @return structure of the data quality parameter, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "valueStructure")
    public ValueStructure getValueStructure() {
        return valueStructure;
    }

    /**
     * Sets the structure of the data quality parameter.
     *
     * @param  newValue  the new parameter value structure.
     */
    public void setValueStructure(final ValueStructure newValue)  {
        checkWritePermission(valueStructure);
        valueStructure = newValue;
    }
}
