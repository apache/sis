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
package org.apache.sis.metadata.iso;

import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Datatype;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.util.internal.shared.CollectionsExt;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ensurePositive;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.AbstractSet;
import java.util.Iterator;

// Specific to the geoapi-4.0 branch:
import org.opengis.annotation.Obligation;
import org.opengis.metadata.citation.Responsibility;


/**
 * New metadata element, not found in ISO 19115, which is required to describe geographic data.
 * Metadata elements are contained in a {@linkplain DefaultMetadataExtensionInformation metadata extension information}.
 * The following properties are mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_ExtendedElementInformation}
 * {@code   ├─name………………………………………………………} Name of the extended metadata element.
 * {@code   ├─definition………………………………………} Definition of the extended element.
 * {@code   ├─obligation………………………………………} Obligation of the extended element.
 * {@code   ├─condition…………………………………………} Condition under which the extended element is mandatory.
 * {@code   ├─dataType……………………………………………} Code which identifies the kind of value provided in the extended element.
 * {@code   ├─maximumOccurrence……………………} Maximum occurrence of the extended element.
 * {@code   ├─domainValue……………………………………} Valid values that can be assigned to the extended element.
 * {@code   ├─parentEntity…………………………………} Name of the metadata entity(s) under which this extended metadata element may appear.
 * {@code   ├─rule………………………………………………………} Specifies how the extended element relates to other existing elements and entities.
 * {@code   └─source…………………………………………………} Name of the person or organisation creating the extended element.
 * {@code       ├─party…………………………………………} Information about the parties.
 * {@code       │   └─name…………………………………} Name of the party.
 * {@code       └─role……………………………………………} Function performed by the responsible party.</div>
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@TitleProperty(name = "name")
@XmlType(name = "MD_ExtendedElementInformation_Type", namespace = Namespaces.MEX, propOrder = {
    "name",
    "shortName",
    "domainCode",
    "definition",
    "obligation",
    "condition",
    "dataType",
    "maxOccurs",
    "domainValue",
    "parentEntity",
    "rule",
    "rationale",
    "sources"
})
@XmlRootElement(name = "MD_ExtendedElementInformation", namespace = Namespaces.MEX)
public class DefaultExtendedElementInformation extends ISOMetadata implements ExtendedElementInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 489138542195499530L;

    /**
     * Name of the extended metadata element.
     */
    private String name;

    /**
     * Short form suitable for use in an implementation method such as XML or SGML.
     */
    @Deprecated(since="1.0")
    private String shortName;

    /**
     * Three digit code assigned to the extended element.
     * Non-null only if the {@linkplain #getDataType() data type}
     * is {@linkplain Datatype#CODE_LIST_ELEMENT code list element}.
     */
    @Deprecated(since="1.0")
    private Integer domainCode;

    /**
     * Definition of the extended element.
     */
    @SuppressWarnings("serial")
    private InternationalString definition;

    /**
     * Obligation of the extended element.
     */
    private Obligation obligation;

    /**
     * Condition under which the extended element is mandatory.
     * Non-null value only if the {@linkplain #getObligation() obligation}
     * is {@linkplain Obligation#CONDITIONAL conditional}.
     */
    @SuppressWarnings("serial")
    private InternationalString condition;

    /**
     * Code which identifies the kind of value provided in the extended element.
     */
    private Datatype dataType;

    /**
     * Maximum occurrence of the extended element.
     * Returns {@code null} if it doesn't apply, for example if the
     * {@linkplain #getDataType data type} is {@linkplain Datatype#ENUMERATION enumeration},
     * {@linkplain Datatype#CODE_LIST code list} or {@linkplain Datatype#CODE_LIST_ELEMENT
     * code list element}.
     */
    private Integer maximumOccurrence;

    /**
     * Valid values that can be assigned to the extended element.
     * Returns {@code null} if it doesn't apply, for example if the
     * {@linkplain #getDataType data type} is {@linkplain Datatype#ENUMERATION enumeration},
     * {@linkplain Datatype#CODE_LIST code list} or {@linkplain Datatype#CODE_LIST_ELEMENT
     * code list element}.
     */
    @SuppressWarnings("serial")
    private InternationalString domainValue;

    /**
     * Name of the metadata entity(s) under which this extended metadata element may appear.
     * The name(s) may be standard metadata element(s) or other extended metadata element(s).
     */
    @SuppressWarnings("serial")
    private Collection<String> parentEntity;

    /**
     * Specifies how the extended element relates to other existing elements and entities.
     */
    @SuppressWarnings("serial")
    private InternationalString rule;

    /**
     * Reason for creating the extended element.
     */
    @SuppressWarnings("serial")
    private InternationalString rationale;

    /**
     * Name of the person or organization creating the extended element.
     */
    @SuppressWarnings("serial")
    private Collection<Responsibility> sources;

    /**
     * Construct an initially empty extended element information.
     */
    public DefaultExtendedElementInformation() {
    }

    /**
     * Create an extended element information initialized to the given values.
     *
     * @param name          the name of the extended metadata element.
     * @param definition    the definition of the extended element.
     * @param condition     the condition under which the extended element is mandatory.
     * @param dataType      the code which identifies the kind of value provided in the extended element.
     * @param parentEntity  the name of the metadata entity(s) under which this extended metadata element may appear.
     * @param rule          how the extended element relates to other existing elements and entities.
     * @param source        the name of the person or organization creating the extended element.
     */
    public DefaultExtendedElementInformation(final String       name,
                                             final CharSequence definition,
                                             final CharSequence condition,
                                             final Datatype     dataType,
                                             final String       parentEntity,
                                             final CharSequence rule,
                                             final Responsibility source)
    {
        this.name         = name;
        this.definition   = Types.toInternationalString(definition);
        this.condition    = Types.toInternationalString(condition);
        this.dataType     = dataType;
        this.parentEntity = singleton(parentEntity, String.class);
        this.rule         = Types.toInternationalString(rule);
        this.sources      = singleton(source, Responsibility.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * <h4>Note on properties validation</h4>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ExtendedElementInformation)
     */
    @SuppressWarnings("deprecation")
    public DefaultExtendedElementInformation(final ExtendedElementInformation object) {
        super(object);
        if (object != null) {
            name              = object.getName();
            shortName         = object.getShortName();
            domainCode        = object.getDomainCode();
            definition        = object.getDefinition();
            obligation        = object.getObligation();
            condition         = object.getCondition();
            dataType          = object.getDataType();
            maximumOccurrence = object.getMaximumOccurrence();
            domainValue       = object.getDomainValue();
            parentEntity      = copyCollection(object.getParentEntity(), String.class);
            rule              = object.getRule();
            rationale         = object.getRationale();
            sources           = copyCollection(object.getSources(), Responsibility.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultExtendedElementInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultExtendedElementInformation} instance is created using the
     *       {@linkplain #DefaultExtendedElementInformation(ExtendedElementInformation) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultExtendedElementInformation castOrCopy(final ExtendedElementInformation object) {
        if (object == null || object instanceof DefaultExtendedElementInformation) {
            return (DefaultExtendedElementInformation) object;
        }
        return new DefaultExtendedElementInformation(object);
    }

    /**
     * Name of the extended metadata element.
     *
     * @return name of the extended metadata element, or {@code null}.
     */
    @Override
    @XmlElement(name = "name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the extended metadata element.
     *
     * @param  newValue  the new name.
     */
    public void setName(final String newValue) {
        checkWritePermission(name);
        name = newValue;
    }

    /**
     * Short form suitable for use in an implementation method such as XML or SGML.
     *
     * @return short form suitable for use in an implementation method such as XML or SGML, or {@code null}.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Override
    @Deprecated(since="1.0")
    @XmlElement(name = "shortName", namespace = LegacyNamespaces.GMD)
    public String getShortName()  {
        return FilterByVersion.LEGACY_METADATA.accept() ? shortName : null;
    }

    /**
     * Sets a short form suitable for use in an implementation method such as XML or SGML.
     *
     * @param  newValue  the new short name.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Deprecated(since="1.0")
    public void setShortName(final String newValue)  {
        checkWritePermission(shortName);
        shortName = newValue;
    }

    /**
     * Three digit code assigned to the extended element.
     * Returns a non-null value only if the {@linkplain #getDataType() data type}
     * is {@linkplain Datatype#CODE_LIST_ELEMENT code list element}.
     *
     * @return three digit code assigned to the extended element, or {@code null}.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Override
    @Deprecated(since="1.0")
    @XmlElement(name = "domainCode", namespace = LegacyNamespaces.GMD)
    public Integer getDomainCode() {
        return FilterByVersion.LEGACY_METADATA.accept() ? domainCode : null;
    }

    /**
     * Sets a three digit code assigned to the extended element.
     *
     * @param  newValue  the new domain code.
     *
     * @deprecated Removed as of ISO 19115:2014.
     */
    @Deprecated(since="1.0")
    public void setDomainCode(final Integer newValue) {
        checkWritePermission(domainCode);
        domainCode = newValue;
    }

    /**
     * Definition of the extended element.
     *
     * @return definition of the extended element, or {@code null}.
     */
    @Override
    @XmlElement(name = "definition", required = true)
    public InternationalString getDefinition()  {
        return definition;
    }

    /**
     * Sets the definition of the extended element.
     *
     * @param  newValue  the new definition.
     */
    public void setDefinition(final InternationalString newValue)  {
        checkWritePermission(definition);
        definition = newValue;
    }

    /**
     * Obligation of the extended element.
     *
     * @return obligation of the extended element, or {@code null}.
     */
    @Override
    @XmlElement(name = "obligation")
    public Obligation getObligation()  {
        return obligation;
    }

    /**
     * Sets the obligation of the extended element.
     *
     * @param  newValue  the new obligation.
     */
    public void setObligation(final Obligation newValue)  {
        checkWritePermission(obligation);
        obligation = newValue;
    }

    /**
     * Condition under which the extended element is mandatory.
     * Returns a non-null value only if the {@linkplain #getObligation() obligation}
     * is {@linkplain Obligation#CONDITIONAL conditional}.
     *
     * @return the condition under which the extended element is mandatory, or {@code null}.
     */
    @Override
    @XmlElement(name = "condition")
    public InternationalString getCondition() {
        return condition;
    }

    /**
     * Sets the condition under which the extended element is mandatory.
     *
     * @param  newValue  the new condition.
     */
    public void setCondition(final InternationalString newValue) {
        checkWritePermission(condition);
        condition = newValue;
    }

    /**
     * Code which identifies the kind of value provided in the extended element.
     *
     * @return the kind of value provided in the extended element, or {@code null}.
     */
    @Override
    @XmlElement(name = "dataType", required = true)
    public Datatype getDataType() {
        return dataType;
    }

    /**
     * Sets the code which identifies the kind of value provided in the extended element.
     *
     * @param  newValue  the new data type.
     */
    public void setDataType(final Datatype newValue) {
        checkWritePermission(dataType);
        dataType = newValue;
    }

    /**
     * Maximum occurrence of the extended element.
     * Returns {@code null} if it does not apply, for example if the
     * {@linkplain #getDataType() data type} is {@linkplain Datatype#ENUMERATION enumeration},
     * {@linkplain Datatype#CODE_LIST code list} or {@linkplain Datatype#CODE_LIST_ELEMENT
     * code list element}.
     *
     * @return maximum occurrence of the extended element, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 0)
    public Integer getMaximumOccurrence() {
        return maximumOccurrence;
    }

    /**
     * Sets the maximum occurrence of the extended element.
     *
     * @param  newValue  the new maximum occurrence, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setMaximumOccurrence(final Integer newValue) {
        checkWritePermission(maximumOccurrence);
        if (ensurePositive(DefaultExtendedElementInformation.class, "maximumOccurrence", false, newValue)) {
            maximumOccurrence = newValue;
        }
    }

    /**
     * Valid values that can be assigned to the extended element.
     * Returns {@code null} if it doesn't apply, for example if the
     * {@linkplain #getDataType() data type} is {@linkplain Datatype#ENUMERATION enumeration},
     * {@linkplain Datatype#CODE_LIST code list} or {@linkplain Datatype#CODE_LIST_ELEMENT
     * code list element}.
     *
     * @return valid values that can be assigned to the extended element, or {@code null}.
     */
    @Override
    @XmlElement(name = "domainValue")
    public InternationalString getDomainValue() {
        return domainValue;
    }

    /**
     * Sets the valid values that can be assigned to the extended element.
     *
     * @param  newValue  the new domain value.
     */
    public void setDomainValue(final InternationalString newValue) {
        checkWritePermission(domainValue);
        domainValue = newValue;
    }

    /**
     * Name of the metadata entity(s) under which this extended metadata element may appear.
     * The name(s) may be standard metadata element(s) or other extended metadata element(s).
     *
     * @return name of the metadata entity(s) under which this extended metadata element may appear.
     */
    @Override
    @XmlElement(name = "parentEntity", required = true)
    public Collection<String> getParentEntity() {
        return parentEntity = nonNullCollection(parentEntity, String.class);
    }

    /**
     * Sets the name of the metadata entity(s) under which this extended metadata element may appear.
     *
     * @param  newValues  the new parent entity.
     */
    public void setParentEntity(final Collection<? extends String> newValues) {
        parentEntity = writeCollection(newValues, parentEntity, String.class);
    }

    /**
     * Specifies how the extended element relates to other existing elements and entities.
     *
     * @return how the extended element relates to other existing elements and entities, or {@code null}.
     */
    @Override
    @XmlElement(name = "rule", required = true)
    public InternationalString getRule() {
        return rule;
    }

    /**
     * Sets how the extended element relates to other existing elements and entities.
     *
     * @param  newValue  the new rule.
     */
    public void setRule(final InternationalString newValue) {
        checkWritePermission(rule);
        rule = newValue;
    }

    /**
     * Returns the reason for creating the extended element.
     *
     * @return reason for creating the extended element, or {@code null}.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "rationale")
    public InternationalString getRationale() {
        return rationale;
    }

    /**
     * Sets the reason for creating the extended element.
     *
     * @param  newValue  the new rationale.
     *
     * @since 0.5
     */
    public void setRationale(final InternationalString newValue) {
        checkWritePermission(rationale);
        rationale = newValue;
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #getRationale()}.
     *
     * @return reason for creating the extended element.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getRationale")
    public Collection<InternationalString> getRationales() {
        return new AbstractSet<InternationalString>() {
            /** Returns 0 if empty, or 1 if a density has been specified. */
            @Override public int size() {
                return getRationale() != null ? 1 : 0;
            }

            /** Returns an iterator over 0 or 1 element. Current iterator implementation is unmodifiable. */
            @Override public Iterator<InternationalString> iterator() {
                return CollectionsExt.singletonOrEmpty(getRationale()).iterator();
            }

            /** Adds an element only if the set is empty. This method is invoked by JAXB at unmarshalling time. */
            @Override public boolean add(final InternationalString newValue) {
                if (isEmpty()) {
                    setRationale(newValue);
                    return true;
                } else {
                    LegacyPropertyAdapter.warnIgnoredExtraneous(InternationalString.class,
                            DefaultExtendedElementInformation.class, "setRationales");
                    return false;
                }
            }
        };
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #setRationale(InternationalString)}.
     *
     * @param  newValues  the new rationales.
     */
    @Deprecated(since="1.0")
    public void setRationales(final Collection<? extends InternationalString> newValues) {
        setRationale(LegacyPropertyAdapter.getSingleton(newValues, InternationalString.class,
                null, DefaultExtendedElementInformation.class, "setRationales"));
    }

    /**
     * Name of the person or organization creating the extended element.
     *
     * @return name of the person or organization creating the extended element.
     */
    @Override
    @XmlElement(name = "source", required = true)
    public Collection<Responsibility> getSources() {
        return sources = nonNullCollection(sources, Responsibility.class);
    }

    /**
     * Sets the name of the person or organization creating the extended element.
     *
     * @param  newValues  the new sources.
     */
    public void setSources(final Collection<? extends Responsibility> newValues) {
        sources = writeCollection(newValues, sources, Responsibility.class);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Returns the maximum occurrence as a string, since it is the way that ISO 19115 represents
     * this information. This method is invoked by JAXB at marshalling time.
     */
    @XmlElement(name = "maximumOccurrence")
    private String getMaxOccurs() {
        final Integer value = getMaximumOccurrence();
        if (value == null) {
            return null;
        }
        final NilReason nil = NilReason.forObject(value);
        if (nil != null) {
            return nil.createNilObject(String.class);
        }
        return value.toString();
    }

    /**
     * Sets the maximum occurrence from a string.
     * This method is invoked by JAXB at unmarshalling time.
     */
    @SuppressWarnings("unused")
    private void setMaxOccurs(final String value) {
        if (value != null) {
            final Integer n;
            final NilReason nil = NilReason.forObject(value);
            if (nil != null) {
                n = nil.createNilObject(Integer.class);
            } else try {
                n = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                Context.warningOccured(Context.current(), DefaultExtendedElementInformation.class, "setMaximumOccurrence", e, true);
                return;
            }
            setMaximumOccurrence(n);
        }
    }
}
