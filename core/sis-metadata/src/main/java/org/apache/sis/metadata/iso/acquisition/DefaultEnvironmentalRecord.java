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
package org.apache.sis.metadata.iso.acquisition;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.acquisition.EnvironmentalRecord;
import org.opengis.util.InternationalString;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensureInRange;


/**
 * Information about the environmental conditions during the acquisition.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MI_EnvironmentalRecord}
 * {@code   ├─averageAirTemperature……………} Average air temperature along the flight pass during the photo flight.
 * {@code   ├─maxRelativeHumidity…………………} Maximum relative humidity along the flight pass during the photo flight.
 * {@code   ├─maxAltitude………………………………………} Maximum altitude during the photo flight.
 * {@code   └─meteorologicalConditions……} Meteorological conditions in the photo flight area, in particular clouds, snow and wind.</div>
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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.5
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@XmlType(name = "MI_EnvironmentalRecord_Type", propOrder = {
    "averageAirTemperature",
    "maxRelativeHumidity",
    "maxAltitude",
    "meteorologicalConditions"
})
@XmlRootElement(name = "MI_EnvironmentalRecord")
public class DefaultEnvironmentalRecord extends ISOMetadata implements EnvironmentalRecord {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3634348015931833471L;

    /**
     * Average air temperature along the flight pass during the photo flight.
     */
    private Double averageAirTemperature;

    /**
     * Maximum relative humidity along the flight pass during the photo flight.
     */
    private Double maxRelativeHumidity;

    /**
     * Maximum altitude during the photo flight.
     */
    private Double maxAltitude;

    /**
     * Meteorological conditions in the photo flight area, in particular clouds, snow and wind.
     */
    private InternationalString meteorologicalConditions;

    /**
     * Constructs an initially empty environmental record.
     */
    public DefaultEnvironmentalRecord() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * <div class="note"><b>Note on properties validation:</b>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     * </div>
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(EnvironmentalRecord)
     */
    public DefaultEnvironmentalRecord(final EnvironmentalRecord object) {
        super(object);
        if (object != null) {
            averageAirTemperature    = object.getAverageAirTemperature();
            maxRelativeHumidity      = object.getMaxRelativeHumidity();
            maxAltitude              = object.getMaxAltitude();
            meteorologicalConditions = object.getMeteorologicalConditions();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultEnvironmentalRecord}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultEnvironmentalRecord} instance is created using the
     *       {@linkplain #DefaultEnvironmentalRecord(EnvironmentalRecord) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultEnvironmentalRecord castOrCopy(final EnvironmentalRecord object) {
        if (object == null || object instanceof DefaultEnvironmentalRecord) {
            return (DefaultEnvironmentalRecord) object;
        }
        return new DefaultEnvironmentalRecord(object);
    }

    /**
     * Returns the average air temperature along the flight pass during the photo flight.
     *
     * @return average air temperature along the flight pass during the photo flight, or {@code null}.
     */
    @Override
    @XmlElement(name = "averageAirTemperature", required = true)
    public Double getAverageAirTemperature() {
        return averageAirTemperature;
    }

    /**
     * Sets the average air temperature along the flight pass during the photo flight.
     *
     * @param  newValue  the new average air temperature value.
     */
    public void setAverageAirTemperature(final Double newValue) {
        checkWritePermission();
        averageAirTemperature = newValue;
    }

    /**
     * Returns the maximum relative humidity along the flight pass during the photo flight.
     *
     * @return maximum relative humidity along the flight pass during the photo flight, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 0, maximum = 100)
    @XmlElement(name = "maxRelativeHumidity", required = true)
    public Double getMaxRelativeHumidity() {
        return maxRelativeHumidity;
    }

    /**
     * Sets the maximum relative humidity along the flight pass during the photo flight.
     *
     * @param  newValue  the new maximum relative humidity, or {@code null}.
     * @throws IllegalArgumentException if the given value is out of range.
     */
    public void setMaxRelativeHumidity(final Double newValue) {
        checkWritePermission();
        if (ensureInRange(DefaultEnvironmentalRecord.class, "maxRelativeHumidity", 0, 100, newValue)) {
            maxRelativeHumidity = newValue;
        }
    }

    /**
     * Returns the maximum altitude during the photo flight.
     *
     * @return maximum altitude during the photo flight, or {@code null}.
     */
    @Override
    @XmlElement(name = "maxAltitude", required = true)
    public Double getMaxAltitude() {
        return maxAltitude;
    }

    /**
     * Sets the maximum altitude value.
     *
     * @param  newValue  the new maximum altitude value.
     */
    public void setMaxAltitude(final Double newValue) {
        checkWritePermission();
        maxAltitude = newValue;
    }

    /**
     * Returns the meteorological conditions in the photo flight area, in particular clouds, snow and wind.
     *
     * @return meteorological conditions in the photo flight area, or {@code null}.
     */
    @Override
    @XmlElement(name = "meteorologicalConditions", required = true)
    public InternationalString getMeteorologicalConditions() {
        return meteorologicalConditions;
    }

    /**
     * Sets the meteorological conditions in the photo flight area, in particular clouds, snow and wind.
     *
     * @param  newValue  the meteorological conditions value.
     */
    public void setMeteorologicalConditions(final InternationalString newValue) {
        checkWritePermission();
        meteorologicalConditions = newValue;
    }
}
