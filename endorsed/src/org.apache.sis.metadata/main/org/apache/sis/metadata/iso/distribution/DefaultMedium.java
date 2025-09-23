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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.measure.Unit;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.distribution.Medium;
import org.opengis.metadata.distribution.MediumFormat;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.bind.gco.GO_Real;
import org.apache.sis.xml.bind.metadata.CI_Citation;
import org.apache.sis.xml.bind.metadata.MD_Identifier;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.internal.shared.CollectionsExt;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ensurePositive;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.metadata.distribution.MediumName;
import org.apache.sis.util.internal.shared.CodeLists;
import org.apache.sis.metadata.iso.citation.DefaultCitation;

// Specific to the main branch:
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.OPTIONAL;
import static org.opengis.annotation.Specification.ISO_19115;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;


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
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MD_Medium_Type", propOrder = {
    "identifier",           // New in ISO 19115-3
    "name",
    "newName",              // From ISO 19115:2014
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
    private static final long serialVersionUID = 7751002701087451894L;

    /**
     * Name of the medium on which the resource can be received.
     */
    @SuppressWarnings("serial")
    private MediumName name;

    /**
     * Density at which the data is recorded.
     * If non-null, then the number shall be greater than zero.
     */
    @SuppressWarnings("serial")
    private Collection<Double> densities;

    /**
     * Units of measure for the recording density.
     */
    @SuppressWarnings("serial")
    private Unit<?> densityUnits;

    /**
     * Number of items in the media identified.
     */
    private Integer volumes;

    /**
     * Methods used to write to the medium.
     */
    @SuppressWarnings("serial")
    private Collection<MediumFormat> mediumFormats;

    /**
     * Description of other limitations or requirements for using the medium.
     */
    @SuppressWarnings("serial")
    private InternationalString mediumNote;

    /**
     * Constructs an initially empty medium.
     */
    public DefaultMedium() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * <h4>Note on properties validation</h4>
     * This constructor does not verify the property values of the given metadata (e.g. whether it contains
     * unexpected negative values). This is because invalid metadata exist in practice, and verifying their
     * validity in this copy constructor is often too late. Note that this is not the only hole, as invalid
     * metadata instances can also be obtained by unmarshalling an invalid XML document.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
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
     *       {@linkplain #DefaultMedium(Medium) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
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
     * <div class="warning"><b>Upcoming API change</b><br>
     * {@link MediumName} may be replaced by {@link Citation} in GeoAPI 4.0.
     * </div>
     *
     * @return name of the medium, or {@code null}.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-389">SIS-389</a>
     */
    @Override
    @XmlElement(name = "name", namespace = LegacyNamespaces.GMD)
    public MediumName getName() {
        return FilterByVersion.LEGACY_METADATA.accept() ? name : null;
    }

    /**
     * Sets the name of the medium on which the resource can be received.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * {@link MediumName} may be replaced by {@link Citation} in GeoAPI 4.0.
     * </div>
     *
     * @param  newValue  the new name.
     */
    public void setName(final MediumName newValue) {
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
    @XmlElement(name = "density")
    @XmlJavaTypeAdapter(GO_Real.Since2014.class)
    @ValueRange(minimum = 0, isMinIncluded = false)
    @UML(identifier="density", obligation=OPTIONAL, specification=ISO_19115)
    public Double getDensity() {
        return LegacyPropertyAdapter.getSingleton(densities, Double.class, null, DefaultMedium.class, "getDensity");
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
        checkWritePermission(valueIfDefined(densities));
        if (ensurePositive(DefaultMedium.class, "density", true, newValue)) {
            densities = writeCollection(CollectionsExt.singletonOrEmpty(newValue), densities, Double.class);
        }
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #getDensity()}.
     *
     * @return density at which the data is recorded, or {@code null}.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getDensity")
    @XmlElement(name = "density", namespace = LegacyNamespaces.GMD)
    public Collection<Double> getDensities() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return densities = nonNullCollection(densities, Double.class);
    }

    /**
     * @deprecated As of ISO 19115:2014, replaced by {@link #setDensity(Double)}.
     *
     * @param  newValues  the new densities.
     */
    @Deprecated(since="1.0")
    public void setDensities(final Collection<? extends Double> newValues) {
        densities = writeCollection(newValues, densities, Double.class);
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
    @XmlElement(name = "identifier")
    @XmlJavaTypeAdapter(MD_Identifier.Since2014.class)
    @UML(identifier="identifier", obligation=OPTIONAL, specification=ISO_19115)
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
     * Returns the medium name as a code list.
     */
    @XmlElement(name = "name")
    @XmlJavaTypeAdapter(CI_Citation.Since2014.class)
    private Citation getNewName() {
        return (name != null) ? new DefaultCitation(name.name()) : null;
    }

    /**
     * Sets the name of the medium on which the resource can be received.
     */
    private void setNewName(final Citation newValue) {
        if (newValue != null) {
            final InternationalString title = newValue.getTitle();
            if (title != null) {
                name = CodeLists.forCodeName(MediumName.class, title.toString());
            }
        }
    }
}
