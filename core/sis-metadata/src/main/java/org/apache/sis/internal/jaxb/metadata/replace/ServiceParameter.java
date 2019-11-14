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

import java.util.Objects;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.TypeName;
import org.opengis.util.MemberName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDirection;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.jaxb.gco.GO_GenericName;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.util.iso.DefaultMemberName;
import org.apache.sis.util.iso.Names;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.Utilities.deepEquals;
import static org.apache.sis.internal.util.CollectionsExt.nonNull;


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
 * <h2>Note about raw-type usage</h2>
 * We use raw type (i.e. we implement {@code ParameterDescriptor} instead of {@code ParameterDescriptor<T>})
 * because there is no way we can know {@code <T>} for sure at unmarshalling time. This is not a recommended
 * practice, so <strong>this class shall not be in public API</strong>.  However it should be okay to create
 * {@code ServiceMetadata} instances in Apache SIS internal code if all methods creating such instances declare
 * {@code ParameterDescriptor<?>} as their return type.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
@SuppressWarnings("rawtypes")               // For the omission of <T> in ParameterDescriptor<T> - see javadoc.
@XmlType(name = "SV_Parameter_Type", namespace = Namespaces.SRV, propOrder = {
    "memberName",           // The  ISO 19115-3:2016 way to marshal name.
    "legacyName",           // Legacy ISO 19139:2007 way to marshal name.
    "direction",
    "description",
    "optionality",
    "optionalityLabel",     // Legacy ISO 19139:2007 way to marshal optionality.
    "repeatability",
    "valueType"
})
@XmlRootElement(name = "SV_Parameter", namespace = Namespaces.SRV)
public final class ServiceParameter extends SimpleIdentifiedObject implements ParameterDescriptor {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = -5335736212313243889L;

    /**
     * The name, as used by the service for this parameter. Note that in ISO 19115-3:2016, this element is
     * inside a {@code <gco:MemberName>} element  (i.e. ISO inserts the same kind of {@code Property_Type}
     * element as it does for all other attributes) while in ISO 19139:2007 it was not (i.e. name attributes
     * like {@code <gco:aName>} were marshalled directly, without wrapper). Example:
     *
     * {@preformat xml
     *   <srv:name>
     *     <gco:MemberName>
     *       <gco:aName>
     *         <gco:CharacterString>A parameter name</gco:CharacterString>
     *       </gco:aName>
     *     </gco:MemberName>
     *   </srv:name>
     * }
     *
     * @see #getLegacyName()
     * @see #getValueType()
     */
    @XmlElement(required=true, name="name")
    @XmlJavaTypeAdapter(GO_GenericName.Since2014.class)
    MemberName memberName;

    /**
     * Indication if the parameter is an input to the service, an output or both.
     */
    @XmlElement(required = true)
    ParameterDirection direction;

    /**
     * A narrative explanation of the role of the parameter.
     */
    @XmlElement
    InternationalString description;

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
     */
    public boolean optionality;

    /**
     * Indication if more than one value of the parameter may be provided.
     */
    @XmlElement(required = true)
    public boolean repeatability;

    /**
     * A copy of {@code this} as a fully-implemented parameter descriptor.
     * This is created when first needed for implementation of {@link #createValue()}.
     */
    private transient ParameterDescriptor descriptor;

    /**
     * Creates an initially empty parameter.
     * This constructor is needed by JAXB.
     *
     * <p><strong>Consider this constructor as private</strong> except for testing purpose.
     * See <cite>Note about raw-type usage</cite> in class javadoc.</p>
     */
    ServiceParameter() {
    }

    /**
     * Creates a parameter initialized to the values of the given one.
     */
    private ServiceParameter(final ParameterDescriptor<?> parameter) {
        super(parameter);
        memberName    = getMemberName(parameter);
        direction     = parameter.getDirection();
        description   = parameter.getDescription();
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
     * Gets the parameter name as a {@code MemberName}. This method first checks if the primary name is an instance of
     * {@code MemberName}. If not, this method searches for the first alias which is an instance of {@code MemberName}.
     * If none is found, then this method tries to build a member name from the primary name and value class.
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
                final Class<?> valueClass = parameter.getValueClass();
                if (valueClass != null) {
                    final String code = id.getCode();
                    if (code != null) {
                        return Names.createMemberName(id.getCodeSpace(), null, code, valueClass);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the name to be marshalled in the ISO 19139:2007 way. Example:
     *
     * {@preformat xml
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
    private DefaultMemberName getLegacyName() {
        return FilterByVersion.LEGACY_METADATA.accept() ? DefaultMemberName.castOrCopy(memberName) : null;
    }

    /**
     * Sets the value from the {@code <gco:aName>} (legacy ISO 19139:2007 format).
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  value  the new name.
     * @throws IllegalStateException if a name is already defined.
     */
    @SuppressWarnings("unused")
    private void setLegacyName(final DefaultMemberName value) {
        ensureUndefined();
        memberName = value;
    }

    /**
     * Ensures that the {@linkplain #memberName} is not already defined.
     *
     * @throws IllegalStateException if a name is already defined.
     */
    private void ensureUndefined() throws IllegalStateException {
        if (memberName != null) {
            throw new IllegalStateException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, "name"));
        }
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
    public synchronized Identifier getName() {
        if (name == null && memberName != null) {
            if (memberName instanceof Identifier) {
                name = (Identifier) memberName;
            } else {
                name = new NameToIdentifier(memberName);
            }
        }
        return name;
    }

    /**
     * Infers the value class from the attribute type.
     * This method is the reason why we can not parameterize this {@code ServiceParameter} class
     * (see <cite>Note about raw-type usage</cite> in class javadoc), since there is no way we
     * can ensure that the returned class is really for type {@code <T>}.
     *
     * @return the value class inferred from the attribute type, or {@code null} if unknown.
     */
    @Override
    public Class<?> getValueClass() {
        return (memberName != null) ? Names.toClass(memberName.getAttributeType()) : null;
    }

    /**
     * For JAXB marshalling of ISO 19139:2007 document only.
     * Note that there is not setter method, since we expect the same information
     * to be provided in the {@link #name} attribute type.
     */
    @XmlElement(name = "valueType", namespace = LegacyNamespaces.SRV)
    @XmlJavaTypeAdapter(GO_GenericName.class)    // Not in package-info because shall not be applied to getLegacyName().
    final TypeName getValueType() {
        if (memberName != null && FilterByVersion.LEGACY_METADATA.accept()) {
            return memberName.getAttributeType();
        }
        return null;
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
     * @return a narrative explanation of the role of the parameter, or {@code null} if none.
     */
    @Override
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Returns the optionality as a boolean (ISO 19115-3:2016 way).
     */
    @XmlElement(name = "optionality", required = true)
    final Boolean getOptionality() {
        return FilterByVersion.CURRENT_METADATA.accept() ? optionality : null;
    }

    /**
     * Sets whether this parameter is optional.
     */
    final void setOptionality(final Boolean optional) {
        if (optional != null) optionality = optional;
    }

    /**
     * Returns {@code "Optional"} if {@link #optionality} is {@code true} or {@code "Mandatory"} otherwise.
     * This is the legacy ISO 19139:2007 way to marshal optionality.
     */
    @XmlElement(name = "optionality", namespace = LegacyNamespaces.SRV)
    final String getOptionalityLabel() {
        return FilterByVersion.LEGACY_METADATA.accept() ? (optionality ? "Optional" : "Mandatory") : null;
    }

    /**
     * Sets whether this parameter is optional.
     */
    final void setOptionalityLabel(final String optional) {
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

    /**
     * Creates a new instance of {@code ParameterValue}.
     * This method delegates the work to {@link org.apache.sis.parameter.DefaultParameterDescriptor}
     * since this {@code ServiceParameter} class is not a full-featured parameter descriptor implementation.
     *
     * @return a new instance of {@code ParameterValue}.
     */
    @Override
    public ParameterValue<?> createValue() {
        ParameterDescriptor<?> desc;
        synchronized (this) {
            desc = descriptor;
            if (desc == null) {
                descriptor = desc = ReferencingServices.getInstance().toImplementation(this);
            }
        }
        return desc.createValue();
    }

    /**
     * Compares this object with the given one for equality.
     *
     * @param  object  the object to compare with this reference system.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode) && object instanceof ParameterDescriptor<?>) {
            final ParameterDescriptor<?> that = (ParameterDescriptor<?>) object;
            if (that.getUnit()         == null &&
                that.getDefaultValue() == null &&
                that.getValueClass()   == getValueClass())
            {
                if (mode.isIgnoringMetadata()) {
                    return Objects.equals(toString(getName()), toString(that.getName()));
                    // super.equals(…) already compared 'getName()' in others mode.
                }
                return deepEquals(that.getDescription(), getDescription(), mode) &&
                                  that.getDirection()     == getDirection()     &&
                                  that.getMinimumOccurs() == getMinimumOccurs() &&
                                  that.getMaximumOccurs() == getMaximumOccurs() &&
                                  that.getValidValues()   == null &&
                                  that.getMinimumValue()  == null &&
                                  that.getMaximumValue()  == null;
            }
        }
        return false;
    }

    /**
     * Null-safe string representation of the given identifier, for comparison purpose.
     * We ignore codespace because they can not be represented in ISO 19139 XML documents.
     */
    private static String toString(final Identifier identifier) {
        return (identifier != null) ? identifier.toString() : null;
    }
}
