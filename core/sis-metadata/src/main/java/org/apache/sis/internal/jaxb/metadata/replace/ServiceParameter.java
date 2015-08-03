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
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;
import org.apache.sis.internal.jaxb.metadata.direct.GO_MemberName;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.iso.Names;
import org.apache.sis.xml.Namespaces;

import static org.apache.sis.internal.util.CollectionsExt.nonNull;
import static org.apache.sis.internal.jaxb.gco.PropertyType.LEGACY_XML;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


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
 * <div class="section">Note about raw-type usage</div>
 * We use raw type (i.e. we implement {@code ParameterDescriptor} instead of {@code ParameterDescriptor<T>})
 * because there is no way we can know {@code <T>} for sure at unmarshalling time. This is not a recommended
 * practice, so <strong>this class shall not be in public API</strong>.  However it should be okay to create
 * {@code ServiceMetadata} instances in Apache SIS internal code if all methods creating such instances declare
 * {@code ParameterDescriptor<?>} as their return type.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.5
 * @module
 */
@SuppressWarnings("rawtypes") // For the omission of <T> in ParameterDescriptor<T> - see javadoc.
@XmlType(name = "SV_Parameter_Type", namespace = Namespaces.SRV, propOrder = {
    "memberName",
    "description",
    "optionality",
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
     * The name, as used by the service for this parameter.
     *
     * @see #getValueType()
     */
    @XmlElement(required=true, name="name")
    @XmlJavaTypeAdapter(GO_MemberName.class)
    MemberName memberName;

    /**
     * A narrative explanation of the role of the parameter.
     */
    @XmlElement
    InternationalString description;

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
        optionality   = parameter.getMinimumOccurs() > 0;
        repeatability = parameter.getMaximumOccurs() > 1;
    }

    /**
     * Returns the given parameter as an instance of {@code ServiceParameter}.
     *
     * @param  parameter The parameter (may be {@code null}).
     * @return The service parameter, or {@code null} if the given argument was null.
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
     * @param  parameter The parameter from which to get the name (may be {@code null}).
     * @return The member name, or {@code null} if none.
     */
    public static MemberName getMemberName(final ParameterDescriptor<?> parameter) {
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
                        return Names.createMemberName(id.getCodeSpace(), ":", code, valueClass);
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
     * @return The parameter name as an identifier (the type specified by ISO 19111).
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
     * Infers the value class from the attribute type.
     * This method is the reason why we can not parameterize this {@code ServiceParameter} class
     * (see <cite>Note about raw-type usage</cite> in class javadoc), since there is no way we
     * can ensure that the returned class is really for type {@code <T>}.
     *
     * @return The value class inferred from the attribute type, or {@code null} if unknown.
     */
    @Override
    public Class<?> getValueClass() {
        return (memberName != null) ? Names.toClass(memberName.getAttributeType()) : null;
    }

    /**
     * For JAXB marhalling of ISO 19119 document only.
     * Note that there is not setter method, since we expect the same information
     * to be provided in the {@link #name} attribute type.
     */
    @XmlElement(name = "valueType")
    final TypeName getValueType() {
        return (LEGACY_XML && memberName != null) ? memberName.getAttributeType() : null;
    }

    /**
     * Returns a narrative explanation of the role of the parameter.
     *
     * @return A narrative explanation of the role of the parameter, or {@code null} if none.
     */
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

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     *
     * @return The minimum occurrence.
     */
    @Override
    public int getMinimumOccurs() {
        return optionality ? 0 : 1;
    }

    /**
     * The maximum number of times that values for this parameter group or parameter can be included.
     *
     * @return The maximum occurrence.
     */
    @Override
    public int getMaximumOccurs() {
        return repeatability ? Integer.MAX_VALUE : 1;
    }

    /**
     * Optional properties.
     * @return {@code null}.
     */
    @Override public Set<?>        getValidValues()  {return null;} // Really null, not an empty set. See method contract.
    @Override public Comparable<?> getMinimumValue() {return null;}
    @Override public Comparable<?> getMaximumValue() {return null;}
    @Override public Object        getDefaultValue() {return null;}
    @Override public Unit<?>       getUnit()         {return null;}

    /**
     * Creates a new instance of {@code ParameterValue}.
     * This method delegates the work to {@link org.apache.sis.parameter.DefaultParameterDescriptor}
     * since this {@code ServiceParameter} class is not a full-featured parameter descriptor implementation.
     *
     * @return A new instance of {@code ParameterValue}.
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
     * @param  object The object to compare with this reference system.
     * @param  mode The strictness level of the comparison.
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
                return that.getMinimumOccurs() == getMinimumOccurs() &&
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
