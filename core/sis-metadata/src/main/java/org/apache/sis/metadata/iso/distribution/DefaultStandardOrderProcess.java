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

import java.util.Date;
import java.util.Currency;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.Record;
import org.opengis.util.RecordType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.StandardOrderProcess;
import org.apache.sis.internal.jaxb.gco.GO_RecordType;
import org.apache.sis.internal.jaxb.gco.GO_Record;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Common ways in which the resource may be obtained or received, and related instructions
 * and fee information.
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
 * @version 1.0
 * @since   0.3
 * @module
 */
@XmlType(name = "MD_StandardOrderProcess_Type", propOrder = {
    "fees",
    "plannedAvailableDateTime",
    "orderingInstructions",
    "turnaround",
    "orderOptionsType",             // New in ISO 19115-3
    "orderOptions"                  // New in ISO 19115-3
})
@XmlRootElement(name = "MD_StandardOrderProcess")
public class DefaultStandardOrderProcess extends ISOMetadata implements StandardOrderProcess {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1948951192071039775L;

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
    private RecordType orderOptionsType;

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
     * @param  object  the metadata to copy values from, or {@code null} if none.
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
            orderOptionsType         = object.getOrderOptionsType();
            orderOptions             = object.getOrderOptions();
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
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * @return fees and terms for retrieving the resource, or {@code null}.
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
     * @param  newValue  the new fees.
     *
     * @see #setCurrency(Currency)
     */
    public void setFees(final InternationalString newValue) {
        checkWritePermission(fees);
        fees = newValue;
    }

    /**
     * Returns the monetary units of the {@link #getFees() fees} (as specified in ISO 4217).
     *
     * <h4>Constraints</h4>
     * For ISO 19115 compatibility reasons, this method is <strong>not</strong> required to return
     * a non-null value even if the text returned by {@link #getFees()} contains a currency units.
     * However if this method returns a non-null value, then that value is required to be consistent
     * with the fees text.
     *
     * @return the fees monetary units, or {@code null} if none or unknown.
     *
     * @since 0.5
     *
     * @see #getFees()
     */
    @Override
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Sets the monetary units of the {@link #getFees() fees} (as specified in ISO 4217).
     * Callers should ensure that the given currency is consistent with the currency
     * in the {@linkplain #getFees() fees} text.
     *
     * @param  newValue  the new currency.
     *
     * @see #setFees(InternationalString)
     *
     * @since 0.5
     */
    public void setCurrency(final Currency newValue) {
        checkWritePermission(currency);
        currency = newValue;
    }

    /**
     * Returns the date and time when the dataset will be available.
     *
     * @return date and time when the dataset will be available, or {@code null}.
     */
    @Override
    @XmlElement(name = "plannedAvailableDateTime")
    public Date getPlannedAvailableDateTime() {
        return toDate(plannedAvailableDateTime);
    }

    /**
     * Sets the date and time when the dataset will be available.
     *
     * @param  newValue  the new planned available time.
     */
    public void setPlannedAvailableDateTime(final Date newValue) {
        checkWritePermission(plannedAvailableDateTime);
        plannedAvailableDateTime = toMilliseconds(newValue);
    }

    /**
     * Returns general instructions, terms and services provided by the distributor.
     *
     * @return general instructions, terms and services provided by the distributor, or {@code null}.
     */
    @Override
    @XmlElement(name = "orderingInstructions")
    public InternationalString getOrderingInstructions() {
        return orderingInstructions;
    }

    /**
     * Sets general instructions, terms and services provided by the distributor.
     *
     * @param  newValue  the new ordering instructions.
     */
    public void setOrderingInstructions(final InternationalString newValue) {
        checkWritePermission(orderingInstructions);
        orderingInstructions = newValue;
    }

    /**
     * Returns typical turnaround time for the filling of an order.
     *
     * @return typical turnaround time for the filling of an order, or {@code null}.
     */
    @Override
    @XmlElement(name = "turnaround")
    public InternationalString getTurnaround() {
        return turnaround;
    }

    /**
     * Sets typical turnaround time for the filling of an order.
     *
     * @param  newValue  the new turnaround.
     */
    public void setTurnaround(final InternationalString newValue) {
        checkWritePermission(turnaround);
        turnaround = newValue;
    }

    /**
     * Returns the description of the {@linkplain #getOrderOptions() order options} record.
     *
     * @return description of the order options record, or {@code null} if none.
     *
     * @since 1.0
     *
     * @see org.apache.sis.util.iso.DefaultRecord#getRecordType()
     */
    @Override
    @XmlElement(name = "orderOptionsType")
    @XmlJavaTypeAdapter(GO_RecordType.Since2014.class)
    public RecordType getOrderOptionsType() {
        return orderOptionsType;
    }

    /**
     * @deprecated Renamed {@link #getOrderOptionsType()} for ISO 19115 conformance.
     *
     * @return description of the order options record, or {@code null} if none.
     *
     * @since 0.5
     */
    @Deprecated
    public RecordType getOrderOptionType() {
        return getOrderOptionsType();
    }

    /**
     * Sets the description of the {@linkplain #getOrderOptions() order options} record.
     *
     * @param  newValue  new description of the order options record.
     *
     * @since 1.0
     */
    public void setOrderOptionsType(final RecordType newValue) {
        checkWritePermission(orderOptionsType);
        orderOptionsType = newValue;
    }

    /**
     * @deprecated Renamed {@link #setOrderOptionsType(RecordType)} for ISO 19115 conformance.
     *
     * @param  newValue  new description of the order options record.
     *
     * @since 0.5
     */
    @Deprecated
    public void setOrderOptionType(final RecordType newValue) {
        setOrderOptionsType(newValue);
    }

    /**
     * Returns the request/purchase choices.
     *
     * @return request/purchase choices.
     *
     * @since 0.5
     *
     * @todo We presume that this record is filled by the vendor for describing the options chosen by the client
     *       when he ordered the resource. We presume that this is not a record to be filled by the user for new
     *       orders, otherwise this method would need to be a factory rather than a getter.
     */
    @Override
    @XmlElement(name = "orderOptions")
    @XmlJavaTypeAdapter(GO_Record.Since2014.class)
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
        checkWritePermission(orderOptions);
        orderOptions = newValue;
    }
}
