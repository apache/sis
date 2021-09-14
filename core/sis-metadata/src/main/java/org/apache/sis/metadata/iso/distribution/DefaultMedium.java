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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import javax.measure.Unit;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.distribution.Medium;
import org.opengis.metadata.distribution.MediumFormat;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.jaxb.gco.GO_Real;
import org.apache.sis.internal.jaxb.FilterByVersion;
import org.apache.sis.internal.jaxb.metadata.CI_Citation;
import org.apache.sis.internal.jaxb.metadata.MD_Identifier;
import org.apache.sis.internal.metadata.legacy.MediumName;
import org.apache.sis.internal.metadata.Dependencies;
import org.apache.sis.internal.metadata.legacy.LegacyPropertyAdapter;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.util.CollectionsExt;

import static org.apache.sis.internal.metadata.MetadataUtilities.ensurePositive;


/**
 * Information about the media on which the resource can be distributed.
 * The following property is mandatory or conditional (i.e. mandatory under some circumstances)
 * in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_Medium}
 * {@code   └─densityUnits……} Units of measure for the recording density.</div>
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
@XmlType(name = "MD_Medium_Type", propOrder = {
    "identifier",           // New in ISO 19115-3
    "name",
    "legacyName",           // From ISO 19115:2003
    "density",
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
    private static final long serialVersionUID = -460355952171320089L;

    /**
     * Name of the medium on which the resource can be received.
     */
    private Citation name;

    /**
     * Density at which the data is recorded.
     * If non-null, then the number shall be greater than zero.
     */
    private Double density;

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
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Medium)
     */
    public DefaultMedium(final Medium object) {
        super(object);
        if (object != null) {
            name          = object.getName();
            density       = object.getDensity();
            densityUnits  = object.getDensityUnits();
            volumes       = object.getVolumes();
            mediumFormats = copyCollection(object.getMediumFormats(), MediumFormat.class);
            mediumNote    = object.getMediumNote();
            identifiers   = singleton(object.getIdentifier(), Identifier.class);
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
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
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
     * @return name of the medium, or {@code null}.
     */
    @Override
    @XmlElement(name = "name")
    @XmlJavaTypeAdapter(CI_Citation.Since2014.class)
    public Citation getName() {
        return name;
    }

    /**
     * Sets the name of the medium on which the resource can be received.
     *
     * @param  newValue  the new name.
     */
    public void setName(final Citation newValue) {
        checkWritePermission(name);
        name = newValue;
    }

    /**
     * Returns the density at which the data is recorded.
     * The number shall be greater than zero.
     *
     * @return density at which the data is recorded, or {@code null}.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "density")
    @XmlJavaTypeAdapter(GO_Real.Since2014.class)
    @ValueRange(minimum = 0, isMinIncluded = false)
    public Double getDensity() {
        return density;
    }

    /**
     * Sets density at which the data is recorded.
     * The number shall be greater than zero.
     *
     * @param  newValue  the new density.
     * @throws IllegalArgumentException if the given value is NaN, zero or negative.
     *
     * @since 0.5
     */
    public void setDensity(final Double newValue) {
        checkWritePermission(density);
        if (ensurePositive(DefaultMedium.class, "density", true, newValue)) {
            density = newValue;
        }
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #getDensity()}.
     *
     * @return density at which the data is recorded, or {@code null}.
     */
    @Override
    @Deprecated
    @Dependencies("getDensity")
    @XmlElement(name = "density", namespace = LegacyNamespaces.GMD)
    public Collection<Double> getDensities() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new AbstractSet<Double>() {
            /** Returns 0 if empty, or 1 if a density has been specified. */
            @Override public int size() {
                return getDensity() != null ? 1 : 0;
            }

            /** Returns an iterator over 0 or 1 element. Current iterator implementation is unmodifiable. */
            @Override public Iterator<Double> iterator() {
                return CollectionsExt.singletonOrEmpty(getDensity()).iterator();
            }

            /** Adds an element only if the set is empty. This method is invoked by JAXB at unmarshalling time. */
            @Override public boolean add(final Double newValue) {
                if (isEmpty()) {
                    setDensity(newValue);
                    return true;
                } else {
                    LegacyPropertyAdapter.warnIgnoredExtraneous(Double.class, DefaultMedium.class, "setDensities");
                    return false;
                }
            }
        };
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #setDensity(Double)}.
     *
     * @param  newValues  the new densities.
     */
    @Deprecated
    public void setDensities(final Collection<? extends Double> newValues) {
        setDensity(LegacyPropertyAdapter.getSingleton(newValues, Double.class, null, DefaultMedium.class, "setDensities"));
    }

    /**
     * Returns the units of measure for the recording density.
     *
     * @return units of measure for the recording density, or {@code null}.
     */
    @Override
    @XmlElement(name = "densityUnits")
    public Unit<?> getDensityUnits() {
        return densityUnits;
    }

    /**
     * Sets the units of measure for the recording density.
     *
     * @param  newValue  the new density units.
     */
    public void setDensityUnits(final Unit<?> newValue) {
        checkWritePermission(densityUnits);
        densityUnits = newValue;
    }

    /**
     * Returns the number of items in the media identified.
     *
     * @return number of items in the media identified, or {@code null}.
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
     * @param  newValue  the new volumes, or {@code null}.
     * @throws IllegalArgumentException if the given value is negative.
     */
    public void setVolumes(final Integer newValue) {
        checkWritePermission(volumes);
        if (ensurePositive(DefaultMedium.class, "volumes", false, newValue)) {
            volumes = newValue;
        }
    }

    /**
     * Returns the method used to write to the medium.
     *
     * @return method used to write to the medium, or {@code null}.
     */
    @Override
    @XmlElement(name = "mediumFormat")
    public Collection<MediumFormat> getMediumFormats() {
        return mediumFormats = nonNullCollection(mediumFormats, MediumFormat.class);
    }

    /**
     * Sets the method used to write to the medium.
     *
     * @param  newValues  the new medium formats.
     */
    public void setMediumFormats(final Collection<? extends MediumFormat> newValues) {
        mediumFormats = writeCollection(newValues, mediumFormats, MediumFormat.class);
    }

    /**
     * Returns a description of other limitations or requirements for using the medium.
     *
     * @return description of other limitations for using the medium, or {@code null}.
     */
    @Override
    @XmlElement(name = "mediumNote")
    public InternationalString getMediumNote() {
        return mediumNote;
    }

    /**
     * Sets a description of other limitations or requirements for using the medium.
     *
     * @param  newValue  the new medium note.
     */
    public void setMediumNote(final InternationalString newValue) {
        checkWritePermission(mediumNote);
        mediumNote = newValue;
    }

    /**
     * Returns a unique identifier for an instance of the medium.
     *
     * @return unique identifier, or {@code null} if none.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "identifier")
    @XmlJavaTypeAdapter(MD_Identifier.Since2014.class)
    public Identifier getIdentifier() {
        return super.getIdentifier();
    }

    /**
     * Sets a unique identifier for an instance of the medium.
     *
     * @param  newValue  the new identifier.
     *
     * @since 0.5
     */
    @Override
    public void setIdentifier(final Identifier newValue) {
        super.setIdentifier(newValue);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the medium name as a code list.
     */
    @XmlElement(name = "name", namespace = LegacyNamespaces.GMD)
    private MediumName getLegacyName() {
        return FilterByVersion.LEGACY_METADATA.accept() ? MediumName.castOrWrap(name) : null;
    }

    /**
     * Sets the name of the medium on which the resource can be received.
     */
    private void setLegacyName(final MediumName newValue) {
        name = newValue;
    }
}
