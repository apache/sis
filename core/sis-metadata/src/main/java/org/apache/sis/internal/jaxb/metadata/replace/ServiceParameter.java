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
package org.apache.sis.internal.jaxb.metadata.replace;

import java.util.Set;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.measure.unit.Unit;
import org.opengis.util.TypeName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDirection;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;
import org.apache.sis.internal.jaxb.metadata.direct.GO_MemberName;
import org.apache.sis.xml.Namespaces;

import static org.apache.sis.internal.util.CollectionsExt.nonNull;
import static org.apache.sis.internal.jaxb.gco.PropertyType.LEGACY_XML;


/**
 * Parameter information conform to the ISO 19115:2014 specification.
 * GeoAPI tries to provides a single API for the parameter classes defined in various specifications
 * (ISO 19111, ISO 19115, Web Processing Service). But we still need separated representations at XML
 * (un)marshalling time. This class is for the ISO 19115:2014 case.
 *
 * <p>Note that this implementation is simple and serves no other purpose than being a container for XML
 * parsing and formatting. For real parameter framework, consider using {@link org.apache.sis.parameter}
 * package instead.</p>
 *
 * @param <T> The type of parameter values.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
@XmlType(name = "SV_Parameter_Type", namespace = Namespaces.SRV, propOrder = {
    "name",
    "direction",
    "description",
    "optionality",
    "repeatability",
    "valueType"
})
@XmlRootElement(name = "SV_Parameter", namespace = Namespaces.SRV)
public final class ServiceParameter<T> extends SimpleIdentifiedObject implements ParameterDescriptor<T> {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -5335736212313243889L;

    /**
     * The name, as used by the service for this parameter.
     *
     * @see #getValueType()
     */
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(GO_MemberName.class)
    public MemberName name;

    /**
     * Indication if the parameter is an input to the service, an output or both.
     */
    @XmlElement
    public ParameterDirection direction;

    /**
     * A narrative explanation of the role of the parameter.
     */
    @XmlElement
    public InternationalString description;

    /**
     * Indication if the parameter is required.
     *
     * <ul>
     *   <li>In ISO 19115, this is represented by "{@code true}" or "{@code false}".</li>
     *   <li>In ISO 19119, this is marshalled as "{@code Optional}" or "{@code Mandatory}".</li>
     * </ul>
     *
     * @see #getOptionality()
     * @see #setOptionality(String)
     */
    public boolean optionality;

    /**
     * Indication if more than one value of the parameter may be provided.
     */
    @XmlElement(required = true)
    public boolean repeatability;

    /**
     * Creates an initially empty parameter.
     * This constructor is needed by JAXB.
     */
    public ServiceParameter() {
    }

    /**
     * Creates a parameter initialized to the values of the given one.
     */
    private ServiceParameter(final ParameterDescriptor<T> parameter) {
        name          = getName(parameter);
        direction     = parameter.getDirection();
        description   = parameter.getDescription();
        optionality   = parameter.getMinimumOccurs() > 0;
        repeatability = parameter.getMaximumOccurs() > 1;
    }

    /**
     * Returns the given parameter as an instance of {@code ServiceParameter}.
     *
     * @param  parameter The parameter (may be {@code null}).
     * @return The service parameter, or {@code null} if the given argument was null.
     */
    public static ServiceParameter<?> castOrCopy(final ParameterDescriptor<?> parameter) {
        if (parameter == null || parameter instanceof ServiceParameter<?>) {
            return (ServiceParameter<?>) parameter;
        }
        return new ServiceParameter<>(parameter);
    }

    /**
     * Gets the parameter name as a {@code MemberName}. This method first check if the primary name is an instance of
     * {@code MemberName}. If not, this method searches for the first alias which is an instance of {@code MemberName}.
     * If none is found, then this method tries to build a member name from the primary name and value class.
     *
     * @param  parameter The parameter from which to get the name (may be {@code null}).
     * @return The member name, or {@code null} if none.
     */
    public static MemberName getName(final ParameterDescriptor<?> parameter) {
        if (parameter != null) {
            final ReferenceIdentifier id = parameter.getName();
            if (id instanceof MemberName) {
                return (MemberName) id;
            }
            for (final GenericName alias : nonNull(parameter.getAlias())) {
                if (alias instanceof MemberName) {
                    return (MemberName) alias;
                }
            }
            if (id != null) {
                final Class<?> valueClass = parameter.getValueClass();
                if (valueClass != null) {
                    final String code = id.getCode();
                    if (code != null) {
                        // TODO return Names.createMemberName(id.getCodeSpace(), ":", code, valueClass);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Class<T> getValueClass() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }

    /**
     * For JAXB marhalling of ISO 19119 document only.
     * Note that there is not setter method, since we expect the same information
     * to be provided in the {@link #name} attribute type.
     */
    @XmlElement(name = "valueType")
    final TypeName getValueType() {
        return (LEGACY_XML && name != null) ? name.getAttributeType() : null;
    }

    @Override
    public ParameterDirection getDirection() {
        return direction;
    }

    @Override
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Returns {@code true} if {@link #optionality} is "{@code true}" or "{@code Optional}",
     * or {@code false} otherwise.
     */
    @XmlElement(name = "optionality", required = true)
    final String getOptionality() {
        return LEGACY_XML ? (optionality ? "Optional" : "Mandatory") : Boolean.toString(optionality);
    }

    /**
     * Sets whether this parameter is optional.
     */
    final void setOptionality(final String optional) {
        if (optional != null) {
            optionality = Boolean.parseBoolean(optional) || optional.equalsIgnoreCase("Optional");
        }
    }

    @Override
    public int getMinimumOccurs() {
        return optionality ? 0 : 1;
    }

    @Override
    public int getMaximumOccurs() {
        return repeatability ? Integer.MAX_VALUE : 1;
    }

    @Override public Set<T>        getValidValues()  {return null;}
    @Override public Comparable<T> getMinimumValue() {return null;}
    @Override public Comparable<T> getMaximumValue() {return null;}
    @Override public T             getDefaultValue() {return null;}
    @Override public Unit<?>       getUnit()         {return null;}

    @Override
    public ParameterValue<T> createValue() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO
    }
}
