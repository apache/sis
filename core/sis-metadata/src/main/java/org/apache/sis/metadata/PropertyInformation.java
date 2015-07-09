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
package org.apache.sis.metadata;

import java.util.Locale;
import java.util.Collection;
import java.util.Collections;
import java.lang.reflect.Method;
import org.opengis.annotation.UML;
import org.opengis.metadata.Datatype;
import org.opengis.metadata.Obligation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.simple.SimpleIdentifier;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.logging.Logging;


/**
 * Description of a metadata property inferred from Java reflection.
 * For a given metadata instances (typically an {@link AbstractMetadata} subclasses,
 * but other types are allowed), instances of {@code PropertyInformation} are obtained
 * indirectly by the {@link MetadataStandard#asInformationMap(Class, KeyNamePolicy)} method.
 *
 * <div class="note"><b>API note:</b>
 * The rational for implementing {@code CheckedContainer} is to consider each {@code ExtendedElementInformation}
 * instance as the set of all possible values for the property. If the information had a {@code contains(E)} method,
 * it would return {@code true} if the given value is valid for that property.</div>
 *
 * <div class="section">Immutability and thread safety</div>
 * This final class is immutable and thus thread-safe.
 *
 * @param <E> The value type, either the method return type if not a collection,
 *            or the type of elements in the collection otherwise.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see InformationMap
 * @see MetadataStandard#asInformationMap(Class, KeyNamePolicy)
 * @see <a href="https://issues.apache.org/jira/browse/SIS-80">SIS-80</a>
 */
final class PropertyInformation<E> extends SimpleIdentifier
        implements ExtendedElementInformation, CheckedContainer<E>
{
    /**
     * For cross-versions compatibility.
     */
    private static final long serialVersionUID = 6279709738674566891L;

    /**
     * The interface which contain this property.
     *
     * @see #getParentEntity()
     */
    private final Class<?> parent;

    /**
     * The value type, either the method return type if not a collection,
     * or the type of elements in the collection otherwise.
     *
     * @see #getDataType()
     * @see #getElementType()
     */
    private final Class<E> elementType;

    /**
     * The minimum number of occurrences.
     * A {@code minimumOccurs} value of -1 means that the property is conditional,
     * i.e. the actual {@code minimumOccurs} value can either 0 or 1 depending on
     * the value of another property.
     *
     * @see #getObligation()
     */
    private final byte minimumOccurs;

    /**
     * The maximum number of occurrences as an unsigned number.
     * Value 255 (or -1 as a signed number) shall be understood as {@link Integer#MAX_VALUE}.
     *
     * @see #getMaximumOccurrence()
     */
    private final byte maximumOccurs;

    /**
     * The domain of valid values, or {@code null} if none. If non-null, then this is set to an
     * instance of {@link ValueRange} at construction time, then replaced by an instance of
     * {@link DomainRange} when first needed by the {@link #getDomainValue()} method.
     *
     * @see #getDomainValue()
     */
    private Object domainValue;

    /**
     * Creates a new {@code PropertyInformation} instance from the annotations on the given getter method.
     *
     * @param  standard    The international standard that define the property, or {@code null} if none.
     * @param  property    The property name as defined by the international {@code standard}.
     * @param  getter      The getter method defined in the interface.
     * @param  elementType The value type, either the method return type if not a collection,
     *                     or the type of elements in the collection otherwise.
     * @param  range       The range of valid values, or {@code null} if none. This information is associated to the
     *                     implementation method rather than the interface one, because it is specific to SIS.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    PropertyInformation(final Citation standard, final String property, final Method getter,
            final Class<E> elementType, final ValueRange range)
    {
        super(standard, property, getter.isAnnotationPresent(Deprecated.class));
        parent = getter.getDeclaringClass();
        this.elementType = elementType;
        final UML uml = getter.getAnnotation(UML.class);
        byte minimumOccurs = 0;
        byte maximumOccurs = 1;
        if (uml != null) {
            switch (uml.obligation()) {
                case MANDATORY:   minimumOccurs =  1; break;
                case FORBIDDEN:   maximumOccurs =  0; break;
                case CONDITIONAL: minimumOccurs = -1; break;
            }
        }
        if (maximumOccurs != 0) {
            final Class<?> c = getter.getReturnType();
            if (c.isArray() || Collection.class.isAssignableFrom(c)) {
                maximumOccurs = -1;
            }
        }
        this.minimumOccurs = minimumOccurs;
        this.maximumOccurs = maximumOccurs;
        this.domainValue   = range;
    }

    /**
     * Returns the primary name by which this metadata element is identified.
     */
    @Override
    public String getName() {
        return code;
    }

    /**
     * Returns the ISO name of the class containing the property,
     * or the simple class name if the ISO name is undefined.
     *
     * @see #getParentEntity()
     */
    @Override
    public final String getCodeSpace() {
        String codespace = Types.getStandardName(parent);
        if (codespace == null) {
            codespace = parent.getSimpleName();
        }
        return codespace;
    }

    /**
     * Unconditionally returns {@code null}.
     *
     * @deprecated This property was defined in the 2003 edition of ISO 19115,
     *             but has been removed in the 2014 edition.
     */
    @Override
    @Deprecated
    public String getShortName() {
        return null;
    }

    /**
     * Unconditionally returns {@code null}.
     *
     * @deprecated This property was defined in the 2003 edition of ISO 19115,
     *             but has been removed in the 2014 edition.
     */
    @Override
    @Deprecated
    public Integer getDomainCode() {
        return null;
    }

    /**
     * Returns the definition of this property, or {@code null} if none.
     */
    @Override
    public final InternationalString getDefinition() {
        return Types.getDescription(parent, code);
    }

    /**
     * Returns the obligation of the element.
     */
    @Override
    public Obligation getObligation() {
        switch (minimumOccurs) {
            case -1: return Obligation.CONDITIONAL;
            case  0: return Obligation.OPTIONAL;
            default: return Obligation.MANDATORY;
        }
    }

    /**
     * Returns the condition under which the extended element is mandatory.
     * Current implementation always return {@code null}, since the condition
     * is not yet documented programmatically.
     */
    @Override
    public InternationalString getCondition() {
        return null;
    }

    /**
     * Returns the kind of value provided in the extended element.
     * This is a generic code that describe the element type.
     * For more accurate information, see {@link #getElementType()}.
     */
    @Override
    public Datatype getDataType() {
        if (CharSequence.class.isAssignableFrom(elementType)) return Datatype.CHARACTER_STRING;
        if (CodeList    .class.isAssignableFrom(elementType)) return Datatype.CODE_LIST;
        if (Enum        .class.isAssignableFrom(elementType)) return Datatype.ENUMERATION;
        if (Numbers.isInteger(elementType)) {
            return Datatype.INTEGER;
        }
        // TODO: check the org.opengis.annotation.Classifier annotation here.
        return Datatype.TYPE_CLASS;
    }

    /**
     * Returns the case type of values to be stored in the property.
     * If the property type is an array or a collection, then this method
     * returns the type of elements in the array or collection.
     *
     * @see TypeValuePolicy#ELEMENT_TYPE
     */
    @Override
    public Class<E> getElementType() {
        return elementType;
    }

    /**
     * Returns the maximum number of times that values are required.
     * This method returns 0 if the property is forbidden, {@link Integer#MAX_VALUE}
     * if the property is an array or a collection, or 1 otherwise.
     */
    @Override
    public Integer getMaximumOccurrence() {
        final int n = maximumOccurs & 0xFF;
        return (n == 0xFF) ? Integer.MAX_VALUE : n;
    }

    /**
     * Returns valid values that can be assigned to the extended element, or {@code null} if none.
     * In the particular case of SIS implementation, this method may return a subclass of {@link NumberRange}.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public InternationalString getDomainValue() {
        Object domain = domainValue;
        if (domain != null) {
            if (!(domain instanceof DomainRange)) {
                try {
                    domain = new DomainRange(elementType, (ValueRange) domain);
                } catch (IllegalArgumentException e) {
                    /*
                     * May happen only if a ValueRange annotation is applied on the wrong method.
                     * The JUnit tests ensure that this never happen at least for the SIS metadata
                     * implementation. If this error happen anyway, the user probably doesn't expect
                     * to have an IllegalArgumentException while he didn't provided any argument.
                     * Returning null as a fallback is compliant with the method contract.
                     */
                    Logging.unexpectedException(Logging.getLogger(Modules.METADATA),
                            PropertyInformation.class, "getDomainValue", e);
                    domain = null;
                }
                domainValue = domain;
            }
        }
        return (DomainRange) domain;
    }

    /**
     * Returns the name of the metadata entity under which this metadata element may appear.
     * The name may be standard metadata element or other extended metadata element.
     *
     * @see #getCodeSpace()
     */
    @Override
    public Collection<String> getParentEntity() {
        return Collections.singleton(getCodeSpace());
    }

    /**
     * Specifies how the extended element relates to other existing elements and entities.
     * The current implementation always return {@code null}.
     */
    @Override
    public InternationalString getRule() {
        return null;
    }

    /**
     * Unconditionally returns {@code null}.
     */
    public InternationalString getRationale() {
        return null;
    }

    /**
     * Unconditionally returns an empty list.
     */
    @Override
    @Deprecated
    public Collection<InternationalString> getRationales() {
        return Collections.emptyList();
    }

    /**
     * Returns the name of the person or organization creating the element.
     */
    @Override
    public Collection<? extends ResponsibleParty> getSources() {
        return authority.getCitedResponsibleParties();
    }

    /**
     * Compares the given object with this element information for equality.
     *
     * @param  obj The object to compare with this element information for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final PropertyInformation<?> that = (PropertyInformation<?>) obj;
            return this.parent        == that.parent        &&
                   this.elementType   == that.elementType   &&
                   this.minimumOccurs == that.minimumOccurs &&
                   this.maximumOccurs == that.maximumOccurs;

        }
        return false;
    }

    /**
     * Computes a hash code value only from the code space and property name.
     * We don't need to use the other properties, because the fully qualified
     * property name should be a sufficient discriminator.
     */
    @Override
    public final int hashCode() {
        return (parent.hashCode() + 31 * code.hashCode()) ^ (int) serialVersionUID;
    }

    /**
     * Invoked by {@link #toString()} in order to append additional information
     * after the identifier.
     */
    @Override
    protected void appendToString(final StringBuilder buffer) {
        buffer.append(" : ").append(Types.getCodeLabel(getDataType()))
              .append(", ").append(getObligation().name().toLowerCase(Locale.US))
              .append(", maxOccurs=");
        final int n = getMaximumOccurrence();
        if (n != Integer.MAX_VALUE) {
            buffer.append(n);
        } else {
            buffer.append('∞');
        }
        final InternationalString domainValue = getDomainValue();
        if (domainValue != null) {
            buffer.append(", domain=").append(domainValue);
        }
    }
}
