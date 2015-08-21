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

/**
 * Descriptions and values of parameters used by a coordinate operation or a process.
 * An explanation for this package is provided in the {@linkplain org.opengis.parameter OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * <p>There is three categories of classes in this package:</p>
 * <ul>
 *   <li><b>Parameter descriptors</b> are immutable types that describes the parameters needed by an operation or a
 *     process. Descriptors contain information like parameter name, optionality, repeatability and value type, but
 *     do not contain the actual parameter value.</li>
 *   <li><b>Parameter values</b> are (<var>descriptor</var>, <var>value</var>) tuples, together with convenience methods
 *     for performing unit conversions and getting the values as instances of some commonly used types.</li>
 *   <li><b>Builders</b>, <b>formatters</b> and search methods aim to simplify the creation of
 *     {@code ParameterDescriptor}s, the search for parameter values and visualizing them in a tabular format.</li>
 * </ul>
 *
 * <p>Parameters are organized in <cite>groups</cite>.
 * A group may be for example the set of all parameters needed for the definition of a <cite>Mercator projection</cite>.
 * Parameter groups have some similarities with {@code java.util.Map} where:</p>
 *
 * <ul>
 *   <li>Keys are (indirectly) parameter
 *       {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor#getName() names}.</li>
 *   <li>Values are (indirectly) typically of type {@code int}, {@code int[]}, {@code double}, {@code double[]},
 *       {@code boolean}, {@link java.lang.String}, {@link java.net.URI} or
 *       {@link org.apache.sis.metadata.iso.citation.DefaultCitation Citation}.</li>
 *   <li>Each parameter (equivalent to map entry) constraints the values to a base
 *       {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor#getValueClass() value class},
 *       and optionally to a {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor#getValueDomain()
 *       value domain} (i.e. minimum and maximum valid values) or an enumeration of
 *       {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor#getValidValues() valid values}.</li>
 *   <li>Each parameter can have a
 *       {@linkplain org.apache.sis.parameter.DefaultParameterDescriptor#getDefaultValue() default value} and a
 *       {@linkplain org.apache.sis.parameter.DefaultParameterValue#getUnit() unit of measurement}.</li>
 *   <li>Some parameters are mandatory ({@linkplain org.apache.sis.parameter.DefaultParameterDescriptor#getMinimumOccurs()
 *       minimum occurrence} = 1), meaning that they can not be removed from the group.
 *       They can be left to their default value however.</li>
 *   <li>Group may contain other groups.</li>
 * </ul>
 *
 * <div class="section">Usage</div>
 * When using this {@code org.apache.sis.parameter} package, the starting point is usually to obtain a
 * {@linkplain org.apache.sis.parameter.DefaultParameterDescriptorGroup parameter group descriptor} for
 * the operation of interest. Those groups are provided by the operation implementors, so users do not
 * need to create their own.
 *
 * <p>Given a group descriptor, users can obtain a new instance of parameter values by a call to the
 * {@link org.apache.sis.parameter.DefaultParameterDescriptorGroup#createValue() createValue()} method.
 * New value groups initially contain all mandatory parameters with their default values and no optional parameter.
 * A {@link org.apache.sis.parameter.DefaultParameterValueGroup#parameter(String) parameter(String)} convenience
 * method is provided for fetching a parameter regardless of whether it was present or not — optional parameters
 * are created when first needed.</p>
 *
 * <div class="note"><b>Example:</b> the following code snippet assumes that the implementor of a Mercator projection
 * provides a {@code ParameterDescriptorGroup} instance in a {@code PARAMETERS} static constant:
 *
 * {@preformat java
 *     ParameterValueGroup group = Mercator.PARAMETERS.createValue();
 *     group.parameter("Longitude of natural origin").setValue(-60);   // Using default units (e.g. degrees).
 *     group.parameter("False easting").setValue(200.0, SI.KILOMETRE); // Using explicit units.
 * }
 * </div>
 *
 * Calls to {@code parameter(…)} throw a {@link org.opengis.parameter.ParameterNotFoundException}
 * if the given name is unknown to the group.
 * Calls to {@code setValue(…)} throw a {@link org.opengis.parameter.InvalidParameterValueException}
 * if the given value is not assignable to the expected class or is not inside the value domain.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@XmlSchema(elementFormDefault= XmlNsForm.QUALIFIED, namespace = Namespaces.GML, xmlns = {
    @XmlNs(prefix = "gml", namespaceURI = Namespaces.GML),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CC_OperationParameter.class),
    @XmlJavaTypeAdapter(CC_OperationParameterGroup.class),
    @XmlJavaTypeAdapter(CC_GeneralOperationParameter.class),
    @XmlJavaTypeAdapter(CC_GeneralParameterValue.class)
})
package org.apache.sis.parameter;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.internal.jaxb.referencing.*;
import org.apache.sis.xml.Namespaces;
