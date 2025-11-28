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

import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractList;
import java.util.Objects;
import java.io.Serializable;
import java.time.temporal.Temporal;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.apache.sis.system.Semaphores;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.internal.shared.CloneAccess;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;
import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.quality.EvaluationMethod;
import org.opengis.metadata.quality.DataEvaluation;
import org.opengis.metadata.quality.AggregationDerivation;


/**
 * Description of the evaluation method and procedure applied.
 * See the {@link EvaluationMethod} GeoAPI interface for more details.
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
 * @author  Alexis Gaillard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.3
 */
@XmlType(name = "DQ_EvaluationMethod_Type", propOrder = {
    "evaluationMethodType",
    "evaluationMethodDescription",
    "evaluationProcedure",
    "referenceDocuments",
    "dates"
})
@XmlRootElement(name = "DQ_EvaluationMethod")
@XmlSeeAlso({
    AbstractDataEvaluation.class,
    DefaultAggregationDerivation.class
})
public class DefaultEvaluationMethod extends ISOMetadata implements EvaluationMethod {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5196994626251088685L;

    /**
     * Type of method used to evaluate quality of the data.
     */
    private EvaluationMethodType evaluationMethodType;

    /**
     * Description of the evaluation method.
     */
    @SuppressWarnings("serial")
    private InternationalString evaluationMethodDescription;

    /**
     * Reference to the procedure information.
     */
    @SuppressWarnings("serial")
    private Citation evaluationProcedure;

    /**
     * Information on documents which are referenced in developing and applying a data quality evaluation method.
     */
    @SuppressWarnings("serial")
    private Collection<Citation> referenceDocuments;

    /**
     * Date or range of dates on which a data quality measure was applied.
     */
    private Dates dates;

    /**
     * The start and end times as a list of O, 1 or 2 elements.
     */
    private static final class Dates extends AbstractList<Temporal>
            implements CheckedContainer<Temporal>, CloneAccess, Serializable
    {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 1210175223467194009L;

        /**
         * Start time ({@code date1}) and end time ({@code date2}) on which a data quality measure was applied.
         * Value is {@code null} if this information is not available.
         */
        @SuppressWarnings("serial")
        private Temporal date1, date2;

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
        public Class<Temporal> getElementType() {
            return Temporal.class;
        }

        /**
         * Removes all dates in this list.
         */
        @Override
        public void clear() {
            date1 = null;
            date2 = null;
        }

        /**
         * Returns the number of elements in this list.
         */
        @Override
        public int size() {
            if (date2 != null) return 2;
            if (date1 != null) return 1;
            return 0;
        }

        /**
         * Returns the value at the given index.
         */
        @Override
        public Temporal get(final int index) {
            Temporal date;
            switch (index) {
                case 0:  date = date1; break;
                case 1:  date = date2; break;
                default: date = null;  break;
            }
            if (date == null) {
                throw new IndexOutOfBoundsException(Errors.format(Errors.Keys.IndexOutOfBounds_1, index));
            }
            return date;
        }

        /**
         * Sets the value at the given index.
         * Null values are not allowed.
         */
        @Override
        public Temporal set(final int index, final Temporal date) {
            Objects.requireNonNull(date);
            final Temporal previous = get(index);
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
        public Temporal remove(final int index) {
            final Temporal previous = get(index);
            switch (index) {
                case 0: date1 = date2;  // Fallthrough
                case 1: date2 = null; break;
            }
            modCount++;
            return previous;
        }

        /**
         * Adds a date at the given position.
         * Null values are not allowed.
         */
        @Override
        public void add(final int index, final Temporal date) {
            if (date2 == null) {
                switch (index) {
                    case 0: {
                        date2 = date1;
                        date1 = date;
                        modCount++;
                        return;
                    }
                    case 1: {
                        if (date1 == null) break;   // Exception will be thrown below.
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
        public boolean addAll(final Collection<? extends Temporal> dates) {
            final int c = modCount;
            if (dates != null) {
                final Iterator<? extends Temporal> it = dates.iterator();
                switch (size()) {               // Fallthrough everywhere.
                    case 0:  if (!it.hasNext()) break;
                             date1 = it.next();
                             modCount++;
                    case 1:  if (!it.hasNext()) break;
                             date2 = it.next();
                             modCount++;
                    default: if (!it.hasNext()) break;
                             throw new IllegalArgumentException(Errors.format(
                                     Errors.Keys.TooManyCollectionElements_3, "dates", 2, dates.size()));
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
     * Constructs an initially empty evaluation method.
     */
    public DefaultEvaluationMethod() {
    }

    /**
     * Constructs an evaluation method initialized to the given description.
     *
     * @param type  the method type, or {@code null} if none.
     * @param name  the method description as a {@link String} or an {@link InternationalString} object,
     *              or {@code null} if none.
     * @since 1.5
     */
    public DefaultEvaluationMethod(final EvaluationMethodType type, final CharSequence description) {
        evaluationMethodType = type;
        evaluationMethodDescription = Types.toInternationalString(description);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(EvaluationMethod)
     */
    public DefaultEvaluationMethod(final EvaluationMethod object) {
        super(object);
        if (object != null) {
            evaluationMethodType        = object.getEvaluationMethodType();
            evaluationMethodDescription = object.getEvaluationMethodDescription();
            evaluationProcedure         = object.getEvaluationProcedure();
            referenceDocuments          = copyCollection(object.getReferenceDocuments(), Citation.class);
            writeDates(object.getDates());
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is an instance of {@link DataEvaluation} or {@link AggregationDerivation},
     *       then this method delegates to the {@code castOrCopy(…)} method of the corresponding SIS subclass.
     *       Note that if the given object implements more than one of the above-cited interfaces,
     *       then the {@code castOrCopy(…)} method to be used is unspecified.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultEvaluationMethod}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultEvaluationMethod} instance is created using the
     *       {@linkplain #DefaultEvaluationMethod(EvaluationMethod) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultEvaluationMethod castOrCopy(final EvaluationMethod object) {
        if (object instanceof DataEvaluation) {
            return AbstractDataEvaluation.castOrCopy((DataEvaluation) object);
        }
        if (object instanceof AggregationDerivation) {
            return DefaultAggregationDerivation.castOrCopy((AggregationDerivation) object);
        }
        // Intentionally tested after the sub-interfaces.
        if (object == null || object instanceof DefaultEvaluationMethod) {
            return (DefaultEvaluationMethod) object;
        }
        return new DefaultEvaluationMethod(object);
    }

    /**
     * Returns the type of method used to evaluate quality of the data.
     *
     * @return type of method used to evaluate quality, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "evaluationMethodType")
    public EvaluationMethodType getEvaluationMethodType() {
        return evaluationMethodType;
    }

    /**
     * Sets the type of method used to evaluate quality of the data.
     *
     * @param  newValue  the new evaluation method type.
     */
    public void setEvaluationMethodType(final EvaluationMethodType newValue)  {
        checkWritePermission(evaluationMethodType);
        evaluationMethodType = newValue;
    }

    /**
     * Returns the description of the evaluation method.
     *
     * @return description of the evaluation method, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "evaluationMethodDescription")
    public InternationalString getEvaluationMethodDescription() {
        return evaluationMethodDescription;
    }

    /**
     * Sets the description of the evaluation method.
     *
     * @param  newValue  the new evaluation method description.
     */
    public void setEvaluationMethodDescription(final InternationalString newValue)  {
        checkWritePermission(evaluationMethodDescription);
        evaluationMethodDescription = newValue;
    }

    /**
     * Returns the reference to the procedure information.
     *
     * @return reference to the procedure information, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "evaluationProcedure")
    public Citation getEvaluationProcedure() {
        return evaluationProcedure;
    }

    /**
     * Sets the reference to the procedure information.
     *
     * @param  newValue  the new evaluation procedure.
     */
    public void setEvaluationProcedure(final Citation newValue) {
        checkWritePermission(evaluationProcedure);
        evaluationProcedure = newValue;
    }

    /**
     * Returns information on documents which are referenced in developing and applying a data quality evaluation method.
     *
     * @return documents referenced in data quality evaluation method.
     */
    @Override
    @XmlElement(name = "referenceDoc")
    public Collection<Citation> getReferenceDocuments() {
        return referenceDocuments = nonNullCollection(referenceDocuments, Citation.class);
    }

    /**
     * Sets the information on documents referenced in data quality evaluation method.
     *
     * @param  newValues  the new name of measures.
     */
    public void setReferenceDocuments(final Collection<? extends Citation> newValues) {
        referenceDocuments = writeCollection(newValues, referenceDocuments, Citation.class);
    }

    /**
     * Returns the date or range of dates on which a data quality measure was applied.
     * The collection size is 1 for a single date, or 2 for a range.
     * Returns an empty collection if this information is not available.
     *
     * @return date or range of dates on which a data quality measure was applied.
     */
    @Override
    @XmlElement(name = "dateTime")
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<Temporal> getDates() {
        if (Semaphores.NULL_FOR_EMPTY_COLLECTION.get()) {
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
     * @param  newValues  the new dates, or {@code null}.
     */
    public void setDates(final Collection<? extends Temporal> newValues) {
        if (newValues != dates) {               // Mandatory check for avoiding the call to 'dates.clear()'.
            checkWritePermission(valueIfDefined(dates));
            writeDates(newValues);
        }
    }

    /**
     * Implementation of {@link #setDates(Collection)}.
     */
    private void writeDates(final Collection<? extends Temporal> newValues) {
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
}
