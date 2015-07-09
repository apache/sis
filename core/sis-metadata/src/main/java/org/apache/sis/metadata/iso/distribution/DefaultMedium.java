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

import java.util.Collection;
import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.annotation.UML;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.distribution.Medium;
import org.opengis.metadata.distribution.MediumName;
import org.opengis.metadata.distribution.MediumFormat;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.NonMarshalledAuthority;
import org.apache.sis.internal.metadata.LegacyPropertyAdapter;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;

// Branch-specific imports
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;


/**
 * Information about the media on which the resource can be distributed.
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
@XmlType(name = "MD_Medium_Type", propOrder = {
    "name",
    "densities",
    "densityUnits",
    "volumes",
    "mediumFormats",
    "mediumNote"
})
@XmlRootElement(name = "MD_Medium")
public class DefaultMedium extends ISOMetadata implements Medium {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7751002701087451894L;

    /**
     * Name of the medium on which the resource can be received.
     */
    private MediumName name;

    /**
     * Density at which the data is recorded.
     * If non-null, then the number shall be greater than zero.
     */
    private Collection<Double> densities;

    /**
     * Units of measure for the recording density.
     */
    private Unit<?> densityUnits;

    /**
     * Number of items in the media identified.
     */
    private Integer volumes;

    /**
     * Methods used to write to the medium.
     */
    private Collection<MediumFormat> mediumFormats;

    /**
     * Description of other limitations or requirements for using the medium.
     */
    private InternationalString mediumNote;

    /**
     * Constructs an initially empty medium.
     */
    public DefaultMedium() {
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
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Medium)
     */
    public DefaultMedium(final Medium object) {
        super(object);
        if (object != null) {
            name          = object.getName();
            densities     = copyCollection(object.getDensities(), Double.class);
            densityUnits  = object.getDensityUnits();
            volumes       = object.getVolumes();
            mediumFormats = copyCollection(object.getMediumFormats(), MediumFormat.class);
            mediumNote    = object.getMediumNote();
            if (object instanceof DefaultMedium) {
                identifiers = singleton(((DefaultMedium) object).getIdentifier(), Identifier.class);
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
     *       {@code DefaultMedium}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultMedium} instance is created using the
     *       {@linkplain #DefaultMedium(Medium) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultMedium castOrCopy(final Medium object) {
        if (object == null || object instanceof DefaultMedium) {
            return (DefaultMedium) object;
        }
        return new DefaultMedium(object);
    }

    /**
     * Returns the name of the medium on which the resource can be received.
     *
     * @return Name of the medium, or {@code null}.
     */
    @Override
    @XmlElement(name = "name")
    public MediumName getName() {
        return name;
    }

    /**
     * Sets the name of the medium on which the resource can be received.
     *
     * @param newValue The new name.
     */
    public void setName(final MediumName newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns the density at which the data is recorded.
     * The number shall be greater than zero.
     *
     * @return Density at which the data is recorded, or {@code null}.
     *
     * @since 0.5
     */
    @ValueRange(minimum = 0, isMinIncluded = false)
    @UML(identifier="density", obligation=OPTIONAL, specification=ISO_19115)
    public Double getDensity() {
        return LegacyPropertyAdapter.getSingleton(densities, Double.class, null, DefaultMedium.class, "getDensity");
    }

    /**
     * Sets density at which the data is recorded.
     * The number shall be greater than zero.
     *
     * @param newValue The new density.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @since 0.5
     */
    public void setDensity(final Double newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultMedium.class, "density", true, newValue)) {
            densities = writeCollection(LegacyPropertyAdapter.asCollection(newValue), densities, Double.class);
        }
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #getDensity()}.
     *
     * @return Density at which the data is recorded, or {@code null}.
     */
    @Override
    @Deprecated
    @XmlElement(name = "density")
    public Collection<Double> getDensities() {
        return densities = nonNullCollection(densities, Double.class);
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #setDensity(Double)}.
     *
     * @param newValues The new densities.
     */
    @Deprecated
    public void setDensities(final Collection<? extends Double> newValues) {
        densities = writeCollection(newValues, densities, Double.class);
    }

    /**
     * Returns the units of measure for the recording density.
     *
     * @return Units of measure for the recording density, or {@code null}.
     */
    @Override
    @XmlElement(name = "densityUnits")
    public Unit<?> getDensityUnits() {
        return densityUnits;
    }

    /**
     * Sets the units of measure for the recording density.
     *
     * @param newValue The new density units.
     */
    public void setDensityUnits(final Unit<?> newValue) {
        checkWritePermission();
        densityUnits = newValue;
    }

    /**
     * Returns the number of items in the media identified.
     *
     * @return Number of items in the media identified, or {@code null}.
     */
    @Override
    @ValueRange(minimum = 0)
    @XmlElement(name = "volumes")
    public Integer getVolumes() {
        return volumes;
    }

    /**
     * Sets the number of items in the media identified.
     *
     * @param newValue The new volumes, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setVolumes(final Integer newValue) {
        checkWritePermission();
        if (ensurePositive(DefaultMedium.class, "volumes", false, newValue)) {
            volumes = newValue;
        }
    }

    /**
     * Returns the method used to write to the medium.
     *
     * @return Method used to write to the medium, or {@code null}.
     */
    @Override
    @XmlElement(name = "mediumFormat")
    public Collection<MediumFormat> getMediumFormats() {
        return mediumFormats = nonNullCollection(mediumFormats, MediumFormat.class);
    }

    /**
     * Sets the method used to write to the medium.
     *
     * @param newValues The new medium formats.
     */
    public void setMediumFormats(final Collection<? extends MediumFormat> newValues) {
        mediumFormats = writeCollection(newValues, mediumFormats, MediumFormat.class);
    }

    /**
     * Returns a description of other limitations or requirements for using the medium.
     *
     * @return Description of other limitations for using the medium, or {@code null}.
     */
    @Override
    @XmlElement(name = "mediumNote")
    public InternationalString getMediumNote() {
        return mediumNote;
    }

    /**
     * Sets a description of other limitations or requirements for using the medium.
     *
     * @param newValue The new medium note.
     */
    public void setMediumNote(final InternationalString newValue) {
        checkWritePermission();
        mediumNote = newValue;
    }

    /**
     * Returns a unique identifier for an instance of the medium.
     *
     * @return Unique identifier, or {@code null} if none.
     *
     * @since 0.5
     */
/// @XmlElement(name = "identifier")
    @UML(identifier="identifier", obligation=OPTIONAL, specification=ISO_19115)
    public Identifier getIdentifier() {
        return NonMarshalledAuthority.getMarshallable(identifiers);
    }

    /**
     * Sets a unique identifier for an instance of the medium.
     *
     * @param newValue The new identifier.
     *
     * @since 0.5
     */
    public void setIdentifier(final Identifier newValue) {
        checkWritePermission();
        identifiers = nonNullCollection(identifiers, Identifier.class);
        NonMarshalledAuthority.setMarshallable(identifiers, newValue);
    }
}
