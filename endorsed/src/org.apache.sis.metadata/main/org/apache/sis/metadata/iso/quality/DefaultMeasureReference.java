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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.quality.Element;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.iso.Types;

// Specific to the main branch:
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Obligation.CONDITIONAL;
import static org.opengis.annotation.Specification.UNSPECIFIED;


/**
 * Reference to the measure used.
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
 * @version 1.5
 * @since   1.3
 */
@XmlType(name = "DQ_MeasureReference_Type", propOrder = {
    "measureIdentification",
    "namesOfMeasure",
    "measureDescription"
})
@XmlRootElement(name = "DQ_MeasureReference")
@UML(identifier="DQ_MeasureReference", specification=UNSPECIFIED)
public class DefaultMeasureReference extends ISOMetadata {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1841855681786546466L;

    /**
     * Value uniquely identifying the measure within a namespace.
     */
    @SuppressWarnings("serial")
    private Identifier measureIdentification;

    /**
     * Name of the test applied to the data.
     */
    @SuppressWarnings("serial")
    private Collection<InternationalString> namesOfMeasure;

    /**
     * Description of the measure.
     */
    @SuppressWarnings("serial")
    private InternationalString measureDescription;

    /**
     * Constructs an initially empty measure reference.
     */
    public DefaultMeasureReference() {
    }

    /**
     * Constructs a measure reference initialized with the given name.
     *
     * @param name  the name of the measure as a {@link String} or an {@link InternationalString} object,
     *              or {@code null} if none.
     * @since 1.5
     */
    public DefaultMeasureReference(final CharSequence name) {
        namesOfMeasure = singleton(Types.toInternationalString(name), InternationalString.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     */
    public DefaultMeasureReference(final DefaultMeasureReference object) {
        super(object);
        if (object != null) {
            measureIdentification = object.getMeasureIdentification();
            measureDescription    = object.getMeasureDescription();
            namesOfMeasure        = copyCollection(object.getNamesOfMeasure(), InternationalString.class);
        }
    }

    /**
     * Initializes a measure reference from the deprecated properties of the given element.
     * This is used for transition from legacy ISO 19115 to newer ISO 19157 model.
     */
    @SuppressWarnings("deprecation")
    final boolean setLegacy(final Element element) {
        return (null != (measureIdentification = element.getMeasureIdentification()))
             | (null != (namesOfMeasure        = copyCollection(element.getNamesOfMeasure(), InternationalString.class)))
             | (null != (measureDescription    = Containers.peekFirst(element.getNamesOfMeasure())));
    }

    /**
     * Returns a value uniquely identifying the measure within a namespace.
     *
     * @return code identifying a registered measure, or {@code null} if none.
     */
    @XmlElement(name = "measureIdentification")
    @UML(identifier="measureIdentification", obligation=OPTIONAL, specification=UNSPECIFIED)
    public Identifier getMeasureIdentification() {
        return measureIdentification;
    }

    /**
     * Sets the identifier of the measure.
     *
     * @param  newValue  the new measure identification.
     */
    public void setMeasureIdentification(final Identifier newValue)  {
        checkWritePermission(measureIdentification);
        measureIdentification = newValue;
    }

    /**
     * Returns the names of the test applied to the data.
     *
     * @return names of the test applied to the data.
     */
    @XmlElement(name = "nameOfMeasure")
    @UML(identifier="nameOfMeasure", obligation=CONDITIONAL, specification=UNSPECIFIED)
    public Collection<InternationalString> getNamesOfMeasure() {
        return namesOfMeasure = nonNullCollection(namesOfMeasure, InternationalString.class);
    }

    /**
     * Sets the names of the test applied to the data.
     *
     * @param  newValues  the new name of measures.
     */
    public void setNamesOfMeasure(final Collection<? extends InternationalString> newValues) {
        namesOfMeasure = writeCollection(newValues, namesOfMeasure, InternationalString.class);
    }

    /**
     * Returns the description of the measure.
     *
     * @return description of the measure, or {@code null}.
     */
    @XmlElement(name = "measureDescription")
    @UML(identifier="measureDescription", obligation=OPTIONAL, specification=UNSPECIFIED)
    public InternationalString getMeasureDescription() {
        return measureDescription;
    }

    /**
     * Sets the description of the measure.
     *
     * @param  newValue  the new measure description.
     */
    public void setMeasureDescription(final InternationalString newValue)  {
        checkWritePermission(measureDescription);
        measureDescription = newValue;
    }
}
