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
package org.apache.sis.metadata.iso.distribution;

import java.util.Currency;
import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.annotation.UML;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.StandardOrderProcess;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;
import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Common ways in which the resource may be obtained or received, and related instructions
 * and fee information.
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
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_StandardOrderProcess_Type", propOrder = {
    "fees",
    "plannedAvailableDateTime",
    "orderingInstructions",
    "turnaround"
})
@XmlRootElement(name = "MD_StandardOrderProcess")
public class DefaultStandardOrderProcess extends ISOMetadata implements StandardOrderProcess {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6107884863471045743L;

    /**
     * Fees and terms for retrieving the resource.
     * Include monetary units (as specified in ISO 4217).
     */
    private InternationalString fees;

    /**
     * The {@link #fees} currency, or {@code null} if unknown or unspecified.
     */
    private Currency currency;

    /**
     * Date and time when the dataset will be available,
     * in milliseconds elapsed since January 1st, 1970.
     */
    private long plannedAvailableDateTime = Long.MIN_VALUE;

    /**
     * General instructions, terms and services provided by the distributor.
     */
    private InternationalString orderingInstructions;

    /**
     * Typical turnaround time for the filling of an order.
     */
    private InternationalString turnaround;

    /**
     * Description of the order options record.
     */
    private RecordType orderOptionType;

    /**
     * Request/purchase choices.
     */
    private Record orderOptions;

    /**
     * Constructs an initially empty standard order process.
     */
    public DefaultStandardOrderProcess() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(StandardOrderProcess)
     */
    public DefaultStandardOrderProcess(final StandardOrderProcess object) {
        super(object);
        if (object != null) {
            fees                     = object.getFees();
            plannedAvailableDateTime = toMilliseconds(object.getPlannedAvailableDateTime());
            orderingInstructions     = object.getOrderingInstructions();
            turnaround               = object.getTurnaround();
            if (object instanceof DefaultStandardOrderProcess) {
                orderOptionType = ((DefaultStandardOrderProcess) object).getOrderOptionType();
                orderOptions    = ((DefaultStandardOrderProcess) object).getOrderOptions();
            }
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultStandardOrderProcess}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultStandardOrderProcess} instance is created using the
     *       {@linkplain #DefaultStandardOrderProcess(StandardOrderProcess) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultStandardOrderProcess castOrCopy(final StandardOrderProcess object) {
        if (object == null || object instanceof DefaultStandardOrderProcess) {
            return (DefaultStandardOrderProcess) object;
        }
        return new DefaultStandardOrderProcess(object);
    }

    /**
     * Returns fees and terms for retrieving the resource.
     * Include monetary units (as specified in ISO 4217).
     * The monetary units may also be available with {@link #getCurrency()}.
     *
     * @return Fees and terms for retrieving the resource, or {@code null}.
     *
     * @see #getCurrency()
     */
    @Override
    @XmlElement(name = "fees")
    public InternationalString getFees() {
        return fees;
    }

    /**
     * Sets fees and terms for retrieving the resource.
     * Include monetary units (as specified in ISO 4217).
     *
     * @param newValue The new fees.
     *
     * @see #setCurrency(Currency)
     */
    public void setFees(final InternationalString newValue) {
        checkWritePermission();
        fees = newValue;
    }

    /**
     * Returns the monetary units of the {@link #getFees() fees} (as specified in ISO 4217).
     *
     * <p><b>Constraints:</b><br>
     * For ISO 19115 compatibility reasons, this method is <strong>not</strong> required to return
     * a non-null value even if the text returned by {@link #getFees()} contains a currency units.
     * However if this method returns a non-null value, then that value is required to be consistent
     * with the fees text.</p>
     *
     * @return The fees monetary units, or {@code null} if none or unknown.
     *
     * @since 0.5
     *
     * @see #getFees()
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Sets the monetary units of the {@link #getFees() fees} (as specified in ISO 4217).
     * Callers should ensure that the given currency is consistent with the currency
     * in the {@linkplain #getFees() fees} text.
     *
     * @param newValue The new currency.
     *
     * @since 0.5
     *
     * @see #setFees(InternationalString)
     */
    public void setCurrency(final Currency newValue) {
        checkWritePermission();
        currency = newValue;
    }

    /**
     * Returns the date and time when the dataset will be available.
     *
     * @return Date and time when the dataset will be available, or {@code null}.
     */
    @Override
    @XmlElement(name = "plannedAvailableDateTime")
    public Date getPlannedAvailableDateTime() {
        return toDate(plannedAvailableDateTime);
    }

    /**
     * Sets the date and time when the dataset will be available.
     *
     * @param newValue The new planned available time.
     */
    public void setPlannedAvailableDateTime(final Date newValue) {
        checkWritePermission();
        plannedAvailableDateTime = toMilliseconds(newValue);
    }

    /**
     * Returns general instructions, terms and services provided by the distributor.
     *
     * @return General instructions, terms and services provided by the distributor, or {@code null}.
     */
    @Override
    @XmlElement(name = "orderingInstructions")
    public InternationalString getOrderingInstructions() {
        return orderingInstructions;
    }

    /**
     * Sets general instructions, terms and services provided by the distributor.
     *
     * @param newValue The new ordering instructions.
     */
    public void setOrderingInstructions(final InternationalString newValue) {
        checkWritePermission();
        orderingInstructions = newValue;
    }

    /**
     * Returns typical turnaround time for the filling of an order.
     *
     * @return Typical turnaround time for the filling of an order, or {@code null}.
     */
    @Override
    @XmlElement(name = "turnaround")
    public InternationalString getTurnaround() {
        return turnaround;
    }

    /**
     * Sets typical turnaround time for the filling of an order.
     *
     * @param newValue The new turnaround.
     */
    public void setTurnaround(final InternationalString newValue) {
        checkWritePermission();
        turnaround = newValue;
    }

    /**
     * Returns the description of the {@linkplain #getOrderOptions() order options} record.
     *
     * @return Description of the order options record, or {@code null} if none.
     *
     * @since 0.5
     *
     * @see org.apache.sis.util.iso.DefaultRecord#getRecordType()
     */
/// @XmlElement(name = "orderOptionType")
    @UML(identifier="orderOptionType", obligation=OPTIONAL, specification=ISO_19115)
    public RecordType getOrderOptionType() {
        return orderOptionType;
    }

    /**
     * Sets the description of the {@linkplain #getOrderOptions() order options} record.
     *
     * @param newValue New description of the order options record.
     *
     * @since 0.5
     */
    public void setOrderOptionType(final RecordType newValue) {
        checkWritePermission();
        orderOptionType = newValue;
    }

    /**
     * Returns the request/purchase choices.
     *
     * @return Request/purchase choices.
     *
     * @since 0.5
     *
     * @todo We presume that this record is filled by the vendor for describing the options chosen by the client
     *       when he ordered the resource. We presume that this is not a record to be filled by the user for new
     *       orders, otherwise this method would need to be a factory rather than a getter.
     */
/// @XmlElement(name = "orderOptions")
    @UML(identifier="orderOptions", obligation=OPTIONAL, specification=ISO_19115)
    public Record getOrderOptions() {
        return orderOptions;
    }

    /**
     * Sets the request/purchase choices.
     *
     * @param newValue the new request/purchase choices.
     *
     * @since 0.5
     */
    public void setOrderOptions(final Record newValue) {
        checkWritePermission();
        orderOptions = newValue;
    }
}
