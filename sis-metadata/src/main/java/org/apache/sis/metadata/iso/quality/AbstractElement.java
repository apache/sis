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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Usability;
import org.opengis.metadata.quality.Completeness;
import org.opengis.metadata.quality.TemporalAccuracy;
import org.opengis.metadata.quality.ThematicAccuracy;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.LogicalConsistency;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.resources.Errors;


/**
 * Type of test applied to the data specified by a data quality scope.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "AbstractDQ_Element_Type", propOrder = {
    "namesOfMeasure",
    "measureIdentification",
    "measureDescription",
    "evaluationMethodType",
    "evaluationMethodDescription",
    "evaluationProcedure",
    "dates",
    "results"
})
@XmlRootElement(name = "DQ_Element")
@XmlSeeAlso({
    AbstractCompleteness.class,
    AbstractLogicalConsistency.class,
    AbstractPositionalAccuracy.class,
    AbstractThematicAccuracy.class,
    AbstractTemporalAccuracy.class,
    DefaultUsability.class
})
public class AbstractElement extends ISOMetadata implements Element {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3542504624077298894L;

    /**
     * Name of the test applied to the data.
     */
    private Collection<InternationalString> namesOfMeasure;

    /**
     * Code identifying a registered standard procedure, or {@code null} if none.
     */
    private Identifier measureIdentification;

    /**
     * Description of the measure being determined.
     */
    private InternationalString measureDescription;

    /**
     * Type of method used to evaluate quality of the dataset, or {@code null} if unspecified.
     */
    private EvaluationMethodType evaluationMethodType;

    /**
     * Description of the evaluation method.
     */
    private InternationalString evaluationMethodDescription;

    /**
     * Reference to the procedure information, or {@code null} if none.
     */
    private Citation evaluationProcedure;

    /**
     * Start time ({@code date1}) and end time ({@code date2}) on which a data quality measure
     * was applied. Value is {@link Long#MIN_VALUE} if this information is not available.
     */
    private long date1, date2;

    /**
     * Value (or set of values) obtained from applying a data quality measure or the out
     * come of evaluating the obtained value (or set of values) against a specified
     * acceptable conformance quality level.
     */
    private Collection<Result> results;

    /**
     * Constructs an initially empty element.
     */
    public AbstractElement() {
        date1 = Long.MIN_VALUE;
        date2 = Long.MIN_VALUE;
    }

    /**
     * Creates an element initialized to the given result.
     *
     * @param result The value obtained from applying a data quality measure against a specified
     *               acceptable conformance quality level.
     */
    public AbstractElement(final Result result) {
        this(); // Initialize date fields.
        results = singleton(result, Result.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Element)
     */
    public AbstractElement(final Element object) {
        super(object);
        namesOfMeasure              = copyCollection(object.getNamesOfMeasure(), InternationalString.class);
        measureIdentification       = object.getMeasureIdentification();
        measureDescription          = object.getMeasureDescription();
        evaluationMethodType        = object.getEvaluationMethodType();
        evaluationMethodDescription = object.getEvaluationMethodDescription();
        evaluationProcedure         = object.getEvaluationProcedure();
        results                     = copyCollection(object.getResults(), Result.class);
        writeDates(object.getDates());
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is is an instance of {@link PositionalAccuracy},
     *       {@link TemporalAccuracy}, {@link ThematicAccuracy}, {@link LogicalConsistency},
     *       {@link Completeness} or {@link Usability}, then this method delegates to the
     *       {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code AbstractElement}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code AbstractElement} instance is created using the
     *       {@linkplain #AbstractElement(Element) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractElement castOrCopy(final Element object) {
        if (object instanceof PositionalAccuracy) {
            return AbstractPositionalAccuracy.castOrCopy((PositionalAccuracy) object);
        }
        if (object instanceof TemporalAccuracy) {
            return AbstractTemporalAccuracy.castOrCopy((TemporalAccuracy) object);
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
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof AbstractElement) {
            return (AbstractElement) object;
        }
        return new AbstractElement(object);
    }

    /**
     * Returns the name of the test applied to the data.
     */
    @Override
    @XmlElement(name = "nameOfMeasure")
    public synchronized Collection<InternationalString> getNamesOfMeasure() {
        return namesOfMeasure = nonNullCollection(namesOfMeasure, InternationalString.class);
    }

    /**
     * Sets the name of the test applied to the data.
     *
     * @param newValues The new name of measures.
     */
    public synchronized void setNamesOfMeasure(final Collection<? extends InternationalString> newValues) {
        namesOfMeasure = writeCollection(newValues, namesOfMeasure, InternationalString.class);
    }

    /**
     * Returns the code identifying a registered standard procedure, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "measureIdentification")
    public synchronized Identifier getMeasureIdentification() {
        return measureIdentification;
    }

    /**
     * Sets the code identifying a registered standard procedure.
     *
     * @param newValue The new measure identification.
     */
    public synchronized void setMeasureIdentification(final Identifier newValue)  {
        checkWritePermission();
        measureIdentification = newValue;
    }

    /**
     * Returns the description of the measure being determined.
     */
    @Override
    @XmlElement(name = "measureDescription")
    public synchronized InternationalString getMeasureDescription() {
        return measureDescription;
    }

    /**
     * Sets the description of the measure being determined.
     *
     * @param newValue The new measure description.
     */
    public synchronized void setMeasureDescription(final InternationalString newValue)  {
        checkWritePermission();
        measureDescription = newValue;
    }

    /**
     * Returns the type of method used to evaluate quality of the dataset,
     * or {@code null} if unspecified.
     */
    @Override
    @XmlElement(name = "evaluationMethodType")
    public synchronized EvaluationMethodType getEvaluationMethodType() {
        return evaluationMethodType;
    }

    /**
     * Sets the type of method used to evaluate quality of the dataset.
     *
     * @param newValue The new evaluation method type.
     */
    public synchronized void setEvaluationMethodType(final EvaluationMethodType newValue)  {
        checkWritePermission();
        evaluationMethodType = newValue;
    }

    /**
     * Returns the description of the evaluation method.
     */
    @Override
    @XmlElement(name = "evaluationMethodDescription")
    public synchronized InternationalString getEvaluationMethodDescription() {
        return evaluationMethodDescription;
    }

    /**
     * Sets the description of the evaluation method.
     *
     * @param newValue The new evaluation method description.
     */
    public synchronized void setEvaluationMethodDescription(final InternationalString newValue)  {
        checkWritePermission();
        evaluationMethodDescription = newValue;
    }

    /**
     * Returns the reference to the procedure information, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "evaluationProcedure")
    public synchronized Citation getEvaluationProcedure() {
        return evaluationProcedure;
    }

    /**
     * Sets the reference to the procedure information.
     *
     * @param newValue The new evaluation procedure.
     */
    public synchronized void setEvaluationProcedure(final Citation newValue) {
        checkWritePermission();
        evaluationProcedure = newValue;
    }

    /**
     * Returns the date or range of dates on which a data quality measure was applied.
     * The collection size is 1 for a single date, or 2 for a range.
     * Returns an empty collection if this information is not available.
     */
    @Override
    @XmlElement(name = "dateTime")
    public synchronized Collection<Date> getDates() {
        if (date1 == Long.MIN_VALUE) {
            return Collections.emptyList();
        }
        if (date2 == Long.MIN_VALUE) {
            return Collections.singleton(new Date(date1));
        }
        return Arrays.asList(
            new Date[] {new Date(date1), new Date(date2)}
        );
    }

    /**
     * Sets the date or range of dates on which a data quality measure was applied.
     * The collection size is 1 for a single date, or 2 for a range.
     *
     * @param newValues The new dates, or {@code null}.
     */
    public synchronized void setDates(final Collection<? extends Date> newValues) {
        checkWritePermission();
        writeDates(newValues);
    }

    /**
     * Implementation of {@link #setDates(Collection)}.
     */
    private void writeDates(final Collection<? extends Date> newValues) {
        date1 = date2 = Long.MIN_VALUE;
        if (newValues != null) {
            final Iterator<? extends Date> it = newValues.iterator();
            if (it.hasNext()) {
                date1 = it.next().getTime();
                if (it.hasNext()) {
                    date2 = it.next().getTime();
                    if (it.hasNext()) {
                        throw new IllegalArgumentException(Errors.format(
                                Errors.Keys.ExcessiveArgumentSize_3, "dates", 2, newValues.size()));
                    }
                }
            }
        }
    }

    /**
     * Returns the value (or set of values) obtained from applying a data quality measure or
     * the out come of evaluating the obtained value (or set of values) against a specified
     * acceptable conformance quality level.
     */
    @Override
    @XmlElement(name = "result", required = true)
    public synchronized Collection<Result> getResults() {
        return results = nonNullCollection(results, Result.class);
    }

    /**
     * Sets the value (or set of values) obtained from applying a data quality measure or
     * the out come of evaluating the obtained value (or set of values) against a specified
     * acceptable conformance quality level.
     *
     * @param newValues The new results.
     */
    public synchronized void setResults(final Collection<? extends Result> newValues) {
        results = writeCollection(newValues, results, Result.class);
    }
}
