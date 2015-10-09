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
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractList;
import java.io.Serializable;
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
import org.apache.sis.internal.system.Semaphores;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;


/**
 * Type of test applied to the data specified by a data quality scope.
 *
 * <p><b>Limitations:</b></p>
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
 * @since   0.3
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
    private static final long serialVersionUID = 3963454452767190970L;

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
     * Start time ({@code date1}) and end time ({@code date2}) on which a data quality measure was applied.
     */
    private Dates dates;

    /**
     * The start and end times as a list of O, 1 or 2 elements.
     */
    private static final class Dates extends AbstractList<Date>
            implements CheckedContainer<Date>, Cloneable, Serializable
    {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1210175223467194009L;

        /**
         * Start time ({@code date1}) and end time ({@code date2}) on which a data quality measure
         * was applied. Value is {@link Long#MIN_VALUE} if this information is not available.
         */
        private long date1, date2;

        /**
         * Creates a new list initialized with no dates.
         */
        Dates() {
            clear();
        }

        /**
         * Returns the type of elements in this list.
         */
        @Override
        public Class<Date> getElementType() {
            return Date.class;
        }

        /**
         * Removes all dates in this list.
         */
        @Override
        public void clear() {
            date1 = Long.MIN_VALUE;
            date2 = Long.MIN_VALUE;
        }

        /**
         * Returns the number of elements in this list.
         */
        @Override
        public int size() {
            if (date2 != Long.MIN_VALUE) return 2;
            if (date1 != Long.MIN_VALUE) return 1;
            return 0;
        }

        /**
         * Returns the value at the given index.
         */
        @Override
        @SuppressWarnings("fallthrough")
        public Date get(final int index) {
            long date = date1;
            switch (index) {
                case 1:  date = date2; // Fall through
                case 0:  if (date != Long.MIN_VALUE) return new Date(date); // else fallthrough.
                default: throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
            }
        }

        /**
         * Sets the value at the given index.
         * Null values are not allowed.
         */
        @Override
        public Date set(final int index, final Date value) {
            final long date = value.getTime();
            final Date previous = get(index);
            switch (index) {
                case 0: date1 = date; break;
                case 1: date2 = date; break;
            }
            modCount++;
            return previous;
        }

        /**
         * Removes the value at the given index.
         */
        @Override
        @SuppressWarnings("fallthrough")
        public Date remove(final int index) {
            final Date previous = get(index);
            switch (index) {
                case 0: date1 = date2; // Fallthrough
                case 1: date2 = Long.MIN_VALUE; break;
            }
            modCount++;
            return previous;
        }

        /**
         * Adds a date at the given position.
         * Null values are not allowed.
         */
        @Override
        public void add(final int index, final Date value) {
            final long date = value.getTime();
            if (date2 == Long.MIN_VALUE) {
                switch (index) {
                    case 0: {
                        date2 = date1;
                        date1 = date;
                        modCount++;
                        return;
                    }
                    case 1: {
                        if (date1 == Long.MIN_VALUE) {
                            break; // Exception will be thrown below.
                        }
                        date2 = date;
                        modCount++;
                        return;
                    }
                }
            }
            throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
        }

        /**
         * Adds all content from the given collection into this collection.
         */
        @Override
        @SuppressWarnings("fallthrough")
        public boolean addAll(final Collection<? extends Date> dates) {
            final int c = modCount;
            if (dates != null) {
                final Iterator<? extends Date> it = dates.iterator();
                switch (size()) { // Fallthrough everywhere.
                    case 0:  if (!it.hasNext()) break;
                             date1 = it.next().getTime();
                             modCount++;
                    case 1:  if (!it.hasNext()) break;
                             date2 = it.next().getTime();
                             modCount++;
                    default: if (!it.hasNext()) break;
                             throw new IllegalArgumentException(Errors.format(
                                     Errors.Keys.ExcessiveArgumentSize_3, "dates", 2, dates.size()));
                }
            }
            return modCount != c;
        }

        /**
         * Returns a clone of this list.
         */
        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

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
    }

    /**
     * Creates an element initialized to the given result.
     *
     * @param result The value obtained from applying a data quality measure against a specified
     *               acceptable conformance quality level.
     */
    public AbstractElement(final Result result) {
        results = singleton(result, Result.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Element)
     */
    public AbstractElement(final Element object) {
        super(object);
        if (object != null) {
            namesOfMeasure              = copyCollection(object.getNamesOfMeasure(), InternationalString.class);
            measureIdentification       = object.getMeasureIdentification();
            measureDescription          = object.getMeasureDescription();
            evaluationMethodType        = object.getEvaluationMethodType();
            evaluationMethodDescription = object.getEvaluationMethodDescription();
            evaluationProcedure         = object.getEvaluationProcedure();
            results                     = copyCollection(object.getResults(), Result.class);
            writeDates(object.getDates());
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link PositionalAccuracy},
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
     *
     * @return Name of the test applied to the data.
     */
    @Override
    @XmlElement(name = "nameOfMeasure")
    public Collection<InternationalString> getNamesOfMeasure() {
        return namesOfMeasure = nonNullCollection(namesOfMeasure, InternationalString.class);
    }

    /**
     * Sets the name of the test applied to the data.
     *
     * @param newValues The new name of measures.
     */
    public void setNamesOfMeasure(final Collection<? extends InternationalString> newValues) {
        namesOfMeasure = writeCollection(newValues, namesOfMeasure, InternationalString.class);
    }

    /**
     * Returns the code identifying a registered standard procedure, or {@code null} if none.
     *
     * @return Code identifying a registered standard procedure, or {@code null}.
     */
    @Override
    @XmlElement(name = "measureIdentification")
    public Identifier getMeasureIdentification() {
        return measureIdentification;
    }

    /**
     * Sets the code identifying a registered standard procedure.
     *
     * @param newValue The new measure identification.
     */
    public void setMeasureIdentification(final Identifier newValue)  {
        checkWritePermission();
        measureIdentification = newValue;
    }

    /**
     * Returns the description of the measure being determined.
     *
     * @return Description of the measure being determined, or {@code null}.
     */
    @Override
    @XmlElement(name = "measureDescription")
    public InternationalString getMeasureDescription() {
        return measureDescription;
    }

    /**
     * Sets the description of the measure being determined.
     *
     * @param newValue The new measure description.
     */
    public void setMeasureDescription(final InternationalString newValue)  {
        checkWritePermission();
        measureDescription = newValue;
    }

    /**
     * Returns the type of method used to evaluate quality of the dataset.
     *
     * @return Type of method used to evaluate quality, or {@code null}.
     */
    @Override
    @XmlElement(name = "evaluationMethodType")
    public EvaluationMethodType getEvaluationMethodType() {
        return evaluationMethodType;
    }

    /**
     * Sets the type of method used to evaluate quality of the dataset.
     *
     * @param newValue The new evaluation method type.
     */
    public void setEvaluationMethodType(final EvaluationMethodType newValue)  {
        checkWritePermission();
        evaluationMethodType = newValue;
    }

    /**
     * Returns the description of the evaluation method.
     *
     * @return Description of the evaluation method, or {@code null}.
     */
    @Override
    @XmlElement(name = "evaluationMethodDescription")
    public InternationalString getEvaluationMethodDescription() {
        return evaluationMethodDescription;
    }

    /**
     * Sets the description of the evaluation method.
     *
     * @param newValue The new evaluation method description.
     */
    public void setEvaluationMethodDescription(final InternationalString newValue)  {
        checkWritePermission();
        evaluationMethodDescription = newValue;
    }

    /**
     * Returns the reference to the procedure information, or {@code null} if none.
     *
     * @return Reference to the procedure information, or {@code null}.
     */
    @Override
    @XmlElement(name = "evaluationProcedure")
    public Citation getEvaluationProcedure() {
        return evaluationProcedure;
    }

    /**
     * Sets the reference to the procedure information.
     *
     * @param newValue The new evaluation procedure.
     */
    public void setEvaluationProcedure(final Citation newValue) {
        checkWritePermission();
        evaluationProcedure = newValue;
    }

    /**
     * Returns the date or range of dates on which a data quality measure was applied.
     * The collection size is 1 for a single date, or 2 for a range.
     * Returns an empty collection if this information is not available.
     *
     * @return Date or range of dates on which a data quality measure was applied.
     */
    @Override
    @XmlElement(name = "dateTime")
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<Date> getDates() {
        if (Semaphores.query(Semaphores.NULL_COLLECTION)) {
            return isNullOrEmpty(dates) ? null : dates;
        }
        if (dates == null) {
            dates = new Dates();
        }
        return dates;
    }

    /**
     * Sets the date or range of dates on which a data quality measure was applied.
     * The collection size is 1 for a single date, or 2 for a range.
     *
     * @param newValues The new dates, or {@code null}.
     */
    public void setDates(final Collection<? extends Date> newValues) {
        checkWritePermission();
        if (newValues != dates) { // Mandatory check for avoiding the call to 'dates.clear()'.
            writeDates(newValues);
        }
    }

    /**
     * Implementation of {@link #setDates(Collection)}.
     */
    private void writeDates(final Collection<? extends Date> newValues) {
        if (isNullOrEmpty(newValues)) {
            dates = null;
        } else {
            if (dates == null) {
                dates = new Dates();
            }
            dates.clear();
            dates.addAll(newValues);
        }
    }

    /**
     * Returns the value (or set of values) obtained from applying a data quality measure or
     * the out come of evaluating the obtained value (or set of values) against a specified
     * acceptable conformance quality level.
     *
     * @return Set of values obtained from applying a data quality measure.
     */
    @Override
    @XmlElement(name = "result", required = true)
    public Collection<Result> getResults() {
        return results = nonNullCollection(results, Result.class);
    }

    /**
     * Sets the value (or set of values) obtained from applying a data quality measure or
     * the out come of evaluating the obtained value (or set of values) against a specified
     * acceptable conformance quality level.
     *
     * @param newValues The new results.
     */
    public void setResults(final Collection<? extends Result> newValues) {
        results = writeCollection(newValues, results, Result.class);
    }
}
