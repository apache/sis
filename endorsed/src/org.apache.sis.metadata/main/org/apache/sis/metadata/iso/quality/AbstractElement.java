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

import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Usability;
import org.opengis.metadata.quality.Completeness;
import org.opengis.metadata.quality.ThematicAccuracy;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.LogicalConsistency;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.gco.InternationalStringAdapter;
import org.apache.sis.metadata.iso.legacy.DateToTemporal;
import org.apache.sis.metadata.iso.legacy.TemporalToDate;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.quality.TemporalQuality;
import org.opengis.metadata.quality.EvaluationMethod;
import org.opengis.metadata.quality.MeasureReference;
import org.opengis.metadata.quality.Metaquality;


/**
 * Aspect of quantitative quality information.
 * See the {@link Element} GeoAPI interface for more details.
 * The following property is mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_Element}
 * {@code   └─result……………} Value obtained from applying a data quality measure.</div>
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
 * @author  Guilhem Legal (Geomatys)
 * @author  Alexis Gaillard (Geomatys)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "AbstractDQ_Element_Type", propOrder = {
    "standaloneQualityReportDetails",
    "measureReference",
    "evaluationMethod",
    "namesOfMeasure",
    "measureIdentification",
    "measureDescription",
    "evaluationMethodType",
    "evaluationMethodDescription",
    "evaluationProcedure",
    "dates",
    "results",
    "derivedElement"
})
@XmlRootElement(name = "AbstractDQ_Element")
@XmlSeeAlso({
    AbstractCompleteness.class,
    AbstractLogicalConsistency.class,
    AbstractPositionalAccuracy.class,
    AbstractThematicAccuracy.class,
    AbstractTemporalQuality.class,
    DefaultUsability.class,
    AbstractMetaquality.class,
    DefaultQualityMeasure.class     // Not a subclass, but "weakly" associated.
})
@SuppressWarnings("deprecation")
public class AbstractElement extends ISOMetadata implements Element {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -406229448295586970L;

    /**
     * Clause in the standalone quality report where this data quality element is described.
     */
    @SuppressWarnings("serial")
    private InternationalString standaloneQualityReportDetails;

    /**
     * Reference to measure used.
     */
    @SuppressWarnings("serial")
    private MeasureReference measureReference;

    /**
     * Evaluation information.
     */
    @SuppressWarnings("serial")
    private EvaluationMethod evaluationMethod;

    /**
     * Value (or set of values) obtained from applying a data quality measure.
     */
    @SuppressWarnings("serial")
    private Collection<Result> results;

    /**
     * In case of aggregation or derivation, indicates the original element.
     */
    @SuppressWarnings("serial")
    private Collection<Element> derivedElements;

    /**
     * Constructs an initially empty element.
     */
    public AbstractElement() {
    }

    /**
     * Creates an element initialized to the given result.
     *
     * @param result  the value obtained from applying a data quality measure against a specified
     *                acceptable conformance quality level.
     */
    public AbstractElement(final Result result) {
        results = singleton(result, Result.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Element)
     */
    public AbstractElement(final Element object) {
        super(object);
        if (object != null) {
            standaloneQualityReportDetails = object.getStandaloneQualityReportDetails();
            if ((measureReference = object.getMeasureReference()) == null) {
                final var candidate = new DefaultMeasureReference();
                if (candidate.setLegacy(object)) measureReference = candidate;
            }
            evaluationMethod = object.getEvaluationMethod();
            results          = copyCollection(object.getResults(), Result.class);
            derivedElements  = copyCollection(object.getDerivedElements(), Element.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link PositionalAccuracy},
     *       {@link TemporalQuality}, {@link ThematicAccuracy}, {@link LogicalConsistency},
     *       {@link Completeness}, {@link Usability} or {@link Metaquality},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractElement}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractElement} instance is created using the
     *       {@linkplain #AbstractElement(Element) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractElement castOrCopy(final Element object) {
        if (object instanceof PositionalAccuracy) {
            return AbstractPositionalAccuracy.castOrCopy((PositionalAccuracy) object);
        }
        if (object instanceof TemporalQuality) {
            return AbstractTemporalQuality.castOrCopy((TemporalQuality) object);
        }
        if (object instanceof ThematicAccuracy) {
            return AbstractThematicAccuracy.castOrCopy((ThematicAccuracy) object);
        }
        if (object instanceof LogicalConsistency) {
            return AbstractLogicalConsistency.castOrCopy((LogicalConsistency) object);
        }
        if (object instanceof Completeness) {
            return AbstractCompleteness.castOrCopy((Completeness) object);
        }
        if (object instanceof Usability) {
            return DefaultUsability.castOrCopy((Usability) object);
        }
        if (object instanceof Metaquality) {
            return AbstractMetaquality.castOrCopy((Metaquality) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractElement) {
            return (AbstractElement) object;
        }
        return new AbstractElement(object);
    }

    /**
     * Returns the clause in the standalone quality report where this data quality element is described.
     * May apply to any related data quality element (original results in case of derivation or aggregation).
     *
     * @return clause where this data quality element is described, or {@code null} if none.
     *
     * @since 1.3
     */
    @Override
    @XmlElement(name = "standaloneQualityReportDetails")
    @XmlJavaTypeAdapter(InternationalStringAdapter.Since2014.class)
    public InternationalString getStandaloneQualityReportDetails() {
        return standaloneQualityReportDetails;
    }

    /**
     * Sets the clause in the standalone quality report where this data quality element is described.
     *
     * @param  newValue  the clause in the standalone quality report.
     *
     * @since 1.3
     */
    public void setStandaloneQualityReportDetails(final InternationalString newValue)  {
        checkWritePermission(standaloneQualityReportDetails);
        standaloneQualityReportDetails = newValue;
    }

    /**
     * Returns an identifier of a measure fully described elsewhere.
     *
     * @return reference to the measure used, or {@code null} if none.
     *
     * @since 1.3
     */
    @Override
    @XmlElement(name = "measure", required = false)
    public MeasureReference getMeasureReference() {
        return (measureReference != null) ? measureReference : Element.super.getMeasureReference();
    }

    /**
     * Sets an identifier of a measure fully described elsewhere.
     *
     * @param  newValues  the new measure identifier.
     *
     * @since 1.3
     */
    public void setMeasureReference(final MeasureReference newValues) {
        checkWritePermission(measureReference);
        measureReference = newValues;
    }

    /**
     * Returns the value of a {@link #measureReference} property.
     * This is used only for deprecated setter methods from older ISO 19115 version.
     *
     * @see #getEvaluationMethodProperty(Function)
     */
    private <V> V getMeasureReferenceProperty(final Function<MeasureReference,V> getter) {
        final MeasureReference m = getMeasureReference();
        return (m != null) && FilterByVersion.LEGACY_METADATA.accept() ? getter.apply(m) : null;
    }

    /**
     * Sets the value of a {@link #measureReference} property.
     * This is used only for deprecated setter methods from older ISO 19115 version.
     *
     * @see #setEvaluationMethodProperty(BiConsumer, Object)
     */
    private <V> void setMeasureReferenceProperty(final BiConsumer<DefaultMeasureReference,V> setter, final V newValue) {
        if (newValue != null) {
            if (!(measureReference instanceof DefaultMeasureReference)) {
                measureReference = new DefaultMeasureReference(measureReference);
            }
            setter.accept((DefaultMeasureReference) measureReference, newValue);
        }
    }

    /**
     * Returns the name of the test applied to the data.
     *
     * @return name of the test applied to the data.
     *
     * @deprecated Replaced by {@link DefaultMeasureReference#getNamesOfMeasure()}.
     */
    @Override
    @Deprecated(since="1.3")
    @Dependencies("getMeasureReference")
    @XmlElement(name = "nameOfMeasure", namespace = LegacyNamespaces.GMD)
    public Collection<InternationalString> getNamesOfMeasure() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) {
            return null;
        }
        MeasureReference m = getMeasureReference();
        if (m == null) {
            if (state() == State.FINAL) {
                return Collections.emptyList();
            }
            setMeasureReference(m = new DefaultMeasureReference());
        }
        if (m instanceof DefaultMeasureReference) {
            return ((DefaultMeasureReference) m).getNamesOfMeasure();
        }
        return Collections.unmodifiableCollection(m.getNamesOfMeasure());
    }

    /**
     * Sets the name of the test applied to the data.
     *
     * @param  newValues  the new name of measures.
     *
     * @deprecated Replaced by {@link DefaultMeasureReference#setNamesOfMeasure(Collection)}.
     */
    @Deprecated(since="1.3")
    public void setNamesOfMeasure(final Collection<? extends InternationalString> newValues) {
        if (!isNullOrEmpty(newValues)) {
            setMeasureReferenceProperty(DefaultMeasureReference::setNamesOfMeasure, newValues);
        }
    }

    /**
     * Returns the code identifying a registered standard procedure, or {@code null} if none.
     *
     * @return code identifying a registered standard procedure, or {@code null}.
     *
     * @deprecated Replaced by {@link DefaultMeasureReference#getMeasureIdentification()}.
     */
    @Override
    @Deprecated(since="1.3")
    @Dependencies("getMeasureReference")
    @XmlElement(name = "measureIdentification", namespace = LegacyNamespaces.GMD)
    public Identifier getMeasureIdentification() {
        return getMeasureReferenceProperty(MeasureReference::getMeasureIdentification);
    }

    /**
     * Sets the code identifying a registered standard procedure.
     *
     * @param  newValue  the new measure identification.
     *
     * @deprecated Replaced by {@link DefaultMeasureReference#setMeasureIdentification(Identifier)}.
     */
    @Deprecated(since="1.3")
    public void setMeasureIdentification(final Identifier newValue)  {
        setMeasureReferenceProperty(DefaultMeasureReference::setMeasureIdentification, newValue);
    }

    /**
     * Returns the description of the measure being determined.
     *
     * @return description of the measure being determined, or {@code null}.
     *
     * @deprecated Replaced by {@link DefaultMeasureReference#getMeasureDescription()}.
     */
    @Override
    @Deprecated(since="1.3")
    @Dependencies("getMeasureReference")
    @XmlElement(name = "measureDescription", namespace = LegacyNamespaces.GMD)
    public InternationalString getMeasureDescription() {
        return getMeasureReferenceProperty(MeasureReference::getMeasureDescription);
    }

    /**
     * Sets the description of the measure being determined.
     *
     * @param  newValue  the new measure description.
     *
     * @deprecated Replaced by {@link DefaultMeasureReference#setMeasureDescription(InternationalString)}.
     */
    @Deprecated(since="1.3")
    public void setMeasureDescription(final InternationalString newValue)  {
        setMeasureReferenceProperty(DefaultMeasureReference::setMeasureDescription, newValue);
    }

    /**
     * Returns the evaluation information.
     *
     * @return information about the evaluation method, or {@code null} if none.
     *
     * @since 1.3
     */
    @Override
    @XmlElement(name = "evaluationMethod", required = false)
    public EvaluationMethod getEvaluationMethod() {
        return evaluationMethod;
    }

    /**
     * Sets the evaluation information.
     *
     * @param  newValue  the new evaluation information.
     *
     * @since 1.3
     */
    public void setEvaluationMethod(final EvaluationMethod newValue) {
        checkWritePermission(evaluationMethod);
        evaluationMethod = newValue;
    }

    /**
     * Returns the value of a {@link #evaluationMethod} property.
     * This is used only for deprecated setter methods from older ISO 19115 version.
     *
     * @see #getMeasureReferenceProperty(Function)
     */
    private <V> V getEvaluationMethodProperty(final Function<EvaluationMethod,V> getter) {
        final EvaluationMethod m = getEvaluationMethod();
        return (m != null) && FilterByVersion.LEGACY_METADATA.accept() ? getter.apply(m) : null;
    }

    /**
     * Sets the value of a {@link #evaluationMethod} property.
     * This is used only for deprecated setter methods from older ISO 19115 version.
     *
     * @see #setMeasureReferenceProperty(BiConsumer, Object)
     */
    private <V> void setEvaluationMethodProperty(final BiConsumer<DefaultEvaluationMethod,V> setter, final V newValue) {
        if (newValue != null) {
            if (!(evaluationMethod instanceof DefaultEvaluationMethod)) {
                evaluationMethod = new DefaultEvaluationMethod(evaluationMethod);
            }
            setter.accept((DefaultEvaluationMethod) evaluationMethod, newValue);
        }
    }

    /**
     * Returns the type of method used to evaluate quality of the dataset.
     *
     * @return type of method used to evaluate quality, or {@code null}.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#getEvaluationMethodType()}.
     */
    @Override
    @Deprecated(since="1.3")
    @Dependencies("getEvaluationMethod")
    @XmlElement(name = "evaluationMethodType", namespace = LegacyNamespaces.GMD)
    public EvaluationMethodType getEvaluationMethodType() {
        return getEvaluationMethodProperty(EvaluationMethod::getEvaluationMethodType);
    }

    /**
     * Sets the type of method used to evaluate quality of the dataset.
     *
     * @param  newValue  the new evaluation method type.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#setEvaluationMethodType(EvaluationMethodType)}.
     */
    @Deprecated(since="1.3")
    public void setEvaluationMethodType(final EvaluationMethodType newValue)  {
        setEvaluationMethodProperty(DefaultEvaluationMethod::setEvaluationMethodType, newValue);
    }

    /**
     * Returns the description of the evaluation method.
     *
     * @return description of the evaluation method, or {@code null}.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#getEvaluationMethodDescription()}.
     */
    @Override
    @Deprecated(since="1.3")
    @Dependencies("getEvaluationMethod")
    @XmlElement(name = "evaluationMethodDescription", namespace = LegacyNamespaces.GMD)
    public InternationalString getEvaluationMethodDescription() {
        return getEvaluationMethodProperty(EvaluationMethod::getEvaluationMethodDescription);
    }

    /**
     * Sets the description of the evaluation method.
     *
     * @param  newValue  the new evaluation method description.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#setEvaluationMethodDescription(InternationalString)}.
     */
    @Deprecated(since="1.3")
    public void setEvaluationMethodDescription(final InternationalString newValue)  {
        setEvaluationMethodProperty(DefaultEvaluationMethod::setEvaluationMethodDescription, newValue);
    }

    /**
     * Returns the reference to the procedure information, or {@code null} if none.
     *
     * @return reference to the procedure information, or {@code null}.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#getEvaluationProcedure()}.
     */
    @Override
    @Deprecated(since="1.3")
    @Dependencies("getEvaluationMethod")
    @XmlElement(name = "evaluationProcedure", namespace = LegacyNamespaces.GMD)
    public Citation getEvaluationProcedure() {
        return getEvaluationMethodProperty(EvaluationMethod::getEvaluationProcedure);
    }

    /**
     * Sets the reference to the procedure information.
     *
     * @param  newValue  the new evaluation procedure.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#setEvaluationProcedure(Citation)}.
     */
    @Deprecated(since="1.3")
    public void setEvaluationProcedure(final Citation newValue) {
        setEvaluationMethodProperty(DefaultEvaluationMethod::setEvaluationProcedure, newValue);
    }

    /**
     * Returns the date or range of dates on which a data quality measure was applied.
     * The collection size is 1 for a single date, or 2 for a range.
     * Returns an empty collection if this information is not available.
     *
     * @return date or range of dates on which a data quality measure was applied.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#getDates()}.
     */
    @Override
    @Deprecated(since="1.3")
    @Dependencies("getEvaluationMethod")
    @XmlElement(name = "dateTime", namespace = LegacyNamespaces.GMD)
    public Collection<Date> getDates() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            EvaluationMethod m = getEvaluationMethod();
            if (m == null) {
                if (state() == State.FINAL) {
                    return Collections.emptyList();
                }
                setEvaluationMethod(m = new DefaultEvaluationMethod());
            }
            Collection<? extends Temporal> dates = m.getDates();
            if (dates != null) return new TemporalToDate(dates);
        }
        return null;
    }

    /**
     * Sets the date or range of dates on which a data quality measure was applied.
     * The collection size is 1 for a single date, or 2 for a range.
     *
     * @param  newValues  the new dates, or {@code null}.
     *
     * @deprecated Replaced by {@link DefaultEvaluationMethod#setDates(Collection)}.
     */
    @Deprecated(since="1.3")
    public void setDates(final Collection<? extends Date> newValues) {
        if (!isNullOrEmpty(newValues)) {
            setEvaluationMethodProperty(DefaultEvaluationMethod::setDates, new DateToTemporal(newValues));
        }
    }

    /**
     * Returns the value(s) obtained from applying a data quality measure.
     * May be an outcome of evaluating the obtained value (or set of values)
     * against a specified acceptable conformance quality level.
     *
     * @return set of values obtained from applying a data quality measure.
     */
    @Override
    @XmlElement(name = "result", required = true)
    public Collection<Result> getResults() {
        return results = nonNullCollection(results, Result.class);
    }

    /**
     * Sets the value(s) obtained from applying a data quality measure.
     *
     * @param  newValues  the new set of value.
     */
    public void setResults(final Collection<? extends Result> newValues) {
        results = writeCollection(newValues, results, Result.class);
    }

    /**
     * Returns the original elements in case of aggregation or derivation.
     *
     * @return original element(s) when there is an aggregation or derivation.
     *
     * @since 1.3
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Element> getDerivedElements() {
        return derivedElements = nonNullCollection(derivedElements, Element.class);
    }

    /**
     * Sets the original elements in case of aggregation or derivation.
     *
     * @param  newValues  the new elements.
     *
     * @since 1.3
     */
    public void setDerivedElements(final Collection<? extends Element> newValues) {
        derivedElements = writeCollection(newValues, derivedElements, Element.class);
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
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19157:2013 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "derivedElement")
    private Collection<Element> getDerivedElement() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getDerivedElements() : null;
    }
}
