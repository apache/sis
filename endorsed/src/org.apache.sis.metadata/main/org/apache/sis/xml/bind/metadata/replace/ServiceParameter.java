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
package org.apache.sis.xml.bind.metadata.replace;

import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.TypeName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.gco.GO_GenericName;
import org.apache.sis.metadata.internal.shared.NameToIdentifier;
import org.apache.sis.util.iso.DefaultMemberName;
import org.apache.sis.util.iso.Names;
import static org.apache.sis.util.collection.Containers.nonNull;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.ReferenceIdentifier;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.parameter.ParameterDirection;
import org.opengis.metadata.Identifier;


/**
 * Parameter information conform to the ISO 19115:2014 specification.
 * GeoAPI tries to provides a single API for the parameter classes defined in various specifications
 * (ISO 19111, ISO 19115, ISO 19157, Web Processing Service).
 * But we still need separated representations at XML (un)marshalling time.
 * This class is for the ISO 19115:2014 case.
 *
 * <p>Note that this implementation is simple and serves no other purpose than being a container for XML
 * parsing and formatting. For real parameter framework, consider using {@link org.apache.sis.parameter}
 * package instead.</p>
 *
 * <h2>Note about raw-type usage</h2>
 * We use raw type (i.e. we implement {@code ParameterDescriptor} instead of {@code ParameterDescriptor<T>})
 * because there is no way we can know {@code <T>} for sure at unmarshalling time. This is not a recommended
 * practice, so <strong>this class shall not be in public API</strong>. However, it should be okay to create
 * {@code ServiceParameter} instances in Apache SIS internal code if all methods creating such instances
 * declare {@code ParameterDescriptor<?>} as their return type.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("rawtypes")   // For the omission of <T> in Parameter<T> - see javadoc.
@XmlType(name = "SV_Parameter_Type", namespace = Namespaces.SRV, propOrder = {
    "memberName",           // The  ISO 19115-3:2016 way to marshal name.
    "legacyName",           // Legacy ISO 19139:2007 way to marshal name.
    "direction",
    "description",
    "optionality",
    "optionalityLabel",     // Legacy ISO 19139:2007 way to marshal optionality.
    "repeatability",
    "legacyValueType"
})
@XmlRootElement(name = "SV_Parameter", namespace = Namespaces.SRV)
public final class ServiceParameter extends Parameter {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8979265876109276877L;

    /**
     * The name, as used by the service for this parameter. Note that in ISO 19115-3:2016, this element is
     * inside a {@code <gco:MemberName>} element  (i.e. ISO inserts the same kind of {@code Property_Type}
     * element as it does for all other attributes) while in ISO 19139:2007 it was not (i.e. name attributes
     * like {@code <gco:aName>} were marshalled directly, without wrapper). Example:
     *
     * {@snippet lang="xml" :
     *   <srv:name>
     *     <gco:MemberName>
     *       <gco:aName>
     *         <gco:CharacterString>A parameter name</gco:CharacterString>
     *       </gco:aName>
     *     </gco:MemberName>
     *   </srv:name>
     * }
     *
     * @see #getName()
     * @see #getLegacyName()
     * @see #getValueType()
     */
    @XmlElement(required=true, name="name")
    @XmlJavaTypeAdapter(GO_GenericName.Since2014.class)
    @SuppressWarnings("serial")                 // Most Apache SIS implementations are serializable.
    public MemberName memberName;

    /**
     * Indication if the parameter is an input to the service, an output or both.
     *
     * @see #getDirection()
     */
    @XmlElement(required = true)
    public ParameterDirection direction;

    /**
     * A narrative explanation of the role of the parameter.
     *
     * @see #getDescription()
     */
    @XmlElement
    @SuppressWarnings("serial")                 // Most Apache SIS implementations are serializable.
    public InternationalString description;

    /**
     * Indication if the parameter is required.
     *
     * <ul>
     *   <li>In ISO 19115-3:2016, this is represented by "{@code true}" or "{@code false}".</li>
     *   <li>In ISO 19139:2007, this was marshalled as "{@code Optional}" or "{@code Mandatory}".</li>
     * </ul>
     *
     * @see #getOptionality()
     * @see #setOptionality(Boolean)
     * @see #getMinimumOccurs()
     */
    boolean optionality;

    /**
     * Indication if more than one value of the parameter may be provided.
     *
     * @see #getMaximumOccurs()
     */
    @XmlElement(required = true)
    public boolean repeatability;

    /**
     * Creates an initially empty parameter.
     * This constructor is needed by JAXB at unmarshalling time.
     */
    public ServiceParameter() {
    }

    /**
     * Creates a parameter initialized to the values of the given one.
     * This is used for marshalling an arbitrary parameter as an ISO 19115 parameter.
     *
     * @see #castOrCopy(ParameterDescriptor)
     */
    @SuppressWarnings("unchecked")
    private ServiceParameter(final ParameterDescriptor<?> parameter) {
        super(parameter);
        memberName    = getMemberName(parameter);
        direction     = parameter.getDirection();
        description   = parameter.getDescription().orElse(null);
        optionality   = parameter.getMinimumOccurs() > 0;
        repeatability = parameter.getMaximumOccurs() > 1;
    }

    /**
     * Returns the given parameter as an instance of {@code ServiceParameter}.
     *
     * @param  parameter  the parameter (may be {@code null}).
     * @return the service parameter, or {@code null} if the given argument was null.
     */
    public static ServiceParameter castOrCopy(final ParameterDescriptor<?> parameter) {
        if (parameter == null || parameter instanceof ServiceParameter) {
            return (ServiceParameter) parameter;
        }
        return new ServiceParameter(parameter);
    }

    /**
     * Gets the parameter name as an instance of {@code MemberName}.
     * This method performs the following checks:
     *
     * <ul>
     *   <li>If the {@linkplain ParameterDescriptor#getName() primary name} is an instance of {@code MemberName},
     *       returns that primary name.</li>
     *   <li>Otherwise this method searches for the first {@linkplain ParameterDescriptor#getAlias() alias}
     *       which is an instance of {@code MemberName}. If found, that alias is returned.</li>
     *   <li>If no alias is found, then this method tries to build a member name from the primary name and the
     *       {@linkplain ParameterDescriptor#getValueType() value type} (if available) or the
     *       {@linkplain ParameterDescriptor#getValueClass() value class}.</li>
     * </ul>
     *
     * This method can be used as a bridge between the parameter object
     * defined by ISO 19111 (namely {@code CC_OperationParameter}) and the one
     * defined by ISO 19115 (namely {@code SV_Parameter}).
     *
     * @param  parameter  the parameter from which to get the name (may be {@code null}).
     * @return the member name, or {@code null} if none.
     */
    public static MemberName getMemberName(final ParameterDescriptor<?> parameter) {
        if (parameter != null) {
            final Identifier id = parameter.getName();
            if (id instanceof MemberName) {
                return (MemberName) id;
            }
            for (final GenericName alias : nonNull(parameter.getAlias())) {
                if (alias instanceof MemberName) {
                    return (MemberName) alias;
                }
            }
            if (id != null) {
                final String code = id.getCode();
                if (code != null) {
                    final String namespace = id.getCodeSpace();
                    final TypeName type = parameter.getValueType();
                    if (type != null) {
                        return Names.createMemberName(namespace, null, code, type);
                    } else {
                        final Class<?> valueClass = parameter.getValueClass();
                        if (valueClass != null) {
                            return Names.createMemberName(namespace, null, code, valueClass);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the name as an {@code Identifier}, which is the type requested by ISO 19111.
     * Note that this is different than the type requested by ISO 19115, which is {@link MemberName}.
     *
     * This method is the converse of {@link #getMemberName(ParameterDescriptor)}.
     *
     * @return the parameter name as an identifier (the type specified by ISO 19111).
     */
    @Override
    public synchronized ReferenceIdentifier getName() {
        if (name == null && memberName != null) {
            if (memberName instanceof ReferenceIdentifier) {
                name = (ReferenceIdentifier) memberName;
            } else {
                name = new NameToIdentifier(memberName);
            }
        }
        return name;
    }

    /**
     * Returns the name to be marshalled in the ISO 19139:2007 way. Example:
     *
     * {@snippet lang="xml" :
     *   <srv:name>
     *     <gco:aName>
     *       <gco:CharacterString>A parameter name</gco:CharacterString>
     *     </gco:aName>
     *   </srv:name>
     * }
     *
     * @return the name if marshalling legacy ISO 19139:2007 format, or {@code null} otherwise.
     */
    @XmlElement(name = "name", namespace = LegacyNamespaces.SRV)
    public DefaultMemberName getLegacyName() {
        return FilterByVersion.LEGACY_METADATA.accept() ? DefaultMemberName.castOrCopy(memberName) : null;
    }

    /**
     * Sets the value from the {@code <gco:aName>} (legacy ISO 19139:2007 format).
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  value  the new name.
     * @throws IllegalStateException if a name is already defined.
     */
    public void setLegacyName(final DefaultMemberName value) {
        if (memberName == null) {
            memberName = value;
        }
    }

    /**
     * For JAXB marshalling of ISO 19139:2007 document only.
     * Note that there is not setter method, because we expect that
     * the same information is provided in the {@link #memberName} attribute type.
     *
     * @return the type name of value component(s) in this parameter.
     */
    @XmlElement(name = "valueType", namespace = LegacyNamespaces.SRV)
    @XmlJavaTypeAdapter(GO_GenericName.class)    // Not in package-info because shall not be applied to getLegacyName().
    public TypeName getLegacyValueType() {
        return FilterByVersion.LEGACY_METADATA.accept() ? getValueType() : null;
    }

    /**
     * Returns the name that describes the type of parameter values.
     *
     * @return the type name of value component(s) in this parameter.
     */
    @Override
    public TypeName getValueType() {
        TypeName type = super.getValueType();
        if (type == null && memberName != null) {
            type = memberName.getAttributeType();
        }
        return type;
    }

    /**
     * Infers the value class from the attribute type.
     * This method is the reason why we cannot parameterize this {@code ServiceParameter} class
     * (see <cite>Note about raw-type usage</cite> in class javadoc), because there is no way we
     * can ensure that the class inferred from {@link MemberName#getAttributeType()} is really
     * for type {@code <T>}.
     *
     * @return the value class inferred from the attribute type, or {@code null} if unknown.
     */
    @Override
    public Class<?> getValueClass() {
        final Class<?> type = super.getValueClass();
        return (type != null) ? type : Names.toClass(getValueType());
    }

    /**
     * Returns an indication if the parameter is an input to the service, an output or both.
     *
     * @return indication if the parameter is an input or output to the service, or {@code null} if unspecified.
     */
    @Override
    public ParameterDirection getDirection() {
        return direction;
    }

    /**
     * Returns a narrative explanation of the role of the parameter.
     *
     * @return a narrative explanation of the role of the parameter.
     */
    @Override
    public Optional<InternationalString> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the optionality as a boolean (ISO 19115-3:2016 way).
     *
     * @return optionality of this parameter.
     */
    @XmlElement(name = "optionality", required = true)
    public Boolean getOptionality() {
        return FilterByVersion.CURRENT_METADATA.accept() ? optionality : null;
    }

    /**
     * Sets whether this parameter is optional.
     *
     * @param  optional  optionality of this parameter.
     */
    public void setOptionality(final Boolean optional) {
        if (optional != null) optionality = optional;
    }

    /**
     * Returns {@code "Optional"} if {@link #optionality} is {@code true} or {@code "Mandatory"} otherwise.
     * This is the legacy ISO 19139:2007 way to marshal optionality.
     *
     * @return optionality of this parameter.
     */
    @XmlElement(name = "optionality", namespace = LegacyNamespaces.SRV)
    public String getOptionalityLabel() {
        return FilterByVersion.LEGACY_METADATA.accept() ? (optionality ? "Optional" : "Mandatory") : null;
    }

    /**
     * Sets whether this parameter is optional.
     *
     * @param  optional  optionality of this parameter.
     */
    public void setOptionalityLabel(final String optional) {
        if (optional != null) {
            optionality = Boolean.parseBoolean(optional) || optional.equalsIgnoreCase("Optional");
        }
    }

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     *
     * @return the minimum occurrence.
     */
    @Override
    public int getMinimumOccurs() {
        return optionality ? 0 : 1;
    }

    /**
     * The maximum number of times that values for this parameter group or parameter can be included.
     *
     * @return the maximum occurrence.
     */
    @Override
    public int getMaximumOccurs() {
        return repeatability ? Integer.MAX_VALUE : 1;
    }
}
