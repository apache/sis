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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.StandardOrderProcess;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.apache.sis.internal.metadata.MetadataUtilities.toDate;
import static org.apache.sis.internal.metadata.MetadataUtilities.toMilliseconds;


/**
 * Common ways in which the resource may be obtained or received, and related instructions
 * and fee information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
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
     * Date and time when the dataset will be available,
     * in milliseconds elapsed since January 1st, 1970.
     */
    private long plannedAvailableDateTime;

    /**
     * General instructions, terms and services provided by the distributor.
     */
    private InternationalString orderingInstructions;

    /**
     * Typical turnaround time for the filling of an order.
     */
    private InternationalString turnaround;

    /**
     * Constructs an initially empty standard order process.
     */
    public DefaultStandardOrderProcess() {
        plannedAvailableDateTime = Long.MIN_VALUE;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(StandardOrderProcess)
     */
    public DefaultStandardOrderProcess(final StandardOrderProcess object) {
        super(object);
        fees                     = object.getFees();
        plannedAvailableDateTime = toMilliseconds(object.getPlannedAvailableDateTime());
        orderingInstructions     = object.getOrderingInstructions();
        turnaround               = object.getTurnaround();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     */
    public void setFees(final InternationalString newValue) {
        checkWritePermission();
        fees = newValue;
    }

    /**
     * Returns the date and time when the dataset will be available.
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
}
