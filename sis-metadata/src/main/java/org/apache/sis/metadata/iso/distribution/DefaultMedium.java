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
 *
 * This package contains documentation from OGC specifications.
 * Open Geospatial Consortium's work is fully acknowledged here.
 */
package org.apache.sis.metadata.iso.distribution;

import java.util.Collection;
import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.Medium;
import org.opengis.metadata.distribution.MediumName;
import org.opengis.metadata.distribution.MediumFormat;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Information about the media on which the resource can be distributed.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
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
    private static final long serialVersionUID = -2838122926367921673L;

    /**
     * Name of the medium on which the resource can be received.
     */
    private MediumName name;

    /**
     * Density at which the data is recorded.
     * If non-null, then the numbers shall be greater than zero.
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
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultMedium castOrCopy(final Medium object) {
        if (object == null || object instanceof DefaultMedium) {
            return (DefaultMedium) object;
        }
        final DefaultMedium copy = new DefaultMedium();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the name of the medium on which the resource can be received.
     */
    @Override
    @XmlElement(name = "name")
    public synchronized MediumName getName() {
        return name;
    }

    /**
     * Sets the name of the medium on which the resource can be received.
     *
     * @param newValue The new name.
     */
    public synchronized void setName(final MediumName newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns the density at which the data is recorded.
     * The numbers shall be greater than zero.
     */
    @Override
    @XmlElement(name = "density")
    @ValueRange(minimum=0, isMinIncluded=false)
    public synchronized Collection<Double> getDensities() {
        return densities = nonNullCollection(densities, Double.class);
    }

    /**
     * Sets density at which the data is recorded.
     * The numbers shall be greater than zero.
     *
     * @param newValues The new densities.
     */
    public synchronized void setDensities(final Collection<? extends Double> newValues) {
        densities = copyCollection(newValues, densities, Double.class);
    }

    /**
     * Returns the units of measure for the recording density.
     */
    @Override
    @XmlElement(name = "densityUnits")
    public synchronized Unit<?> getDensityUnits() {
        return densityUnits;
    }

    /**
     * Sets the units of measure for the recording density.
     *
     * @param newValue The new density units.
     */
    public synchronized void setDensityUnits(final Unit<?> newValue) {
        checkWritePermission();
        densityUnits = newValue;
    }

    /**
     * Returns the number of items in the media identified.
     */
    @Override
    @ValueRange(minimum=0)
    @XmlElement(name = "volumes")
    public synchronized Integer getVolumes() {
        return volumes;
    }

    /**
     * Sets the number of items in the media identified.
     *
     * @param newValue The new volumes.
     */
    public synchronized void setVolumes(final Integer newValue) {
        checkWritePermission();
        volumes = newValue;
    }

    /**
     * Returns the method used to write to the medium.
     */
    @Override
    @XmlElement(name = "mediumFormat")
    public synchronized Collection<MediumFormat> getMediumFormats() {
        return mediumFormats = nonNullCollection(mediumFormats, MediumFormat.class);
    }

    /**
     * Sets the method used to write to the medium.
     *
     * @param newValues The new medium formats.
     */
    public synchronized void setMediumFormats(final Collection<? extends MediumFormat> newValues) {
        mediumFormats = copyCollection(newValues, mediumFormats, MediumFormat.class);
    }

    /**
     * Returns a description of other limitations or requirements for using the medium.
     */
    @Override
    @XmlElement(name = "mediumNote")
    public synchronized InternationalString getMediumNote() {
        return mediumNote;
    }

    /**
     * Sets a description of other limitations or requirements for using the medium.
     *
     * @param newValue The new medium note.
     */
    public synchronized void setMediumNote(final InternationalString newValue) {
        checkWritePermission();
        mediumNote = newValue;
    }
}
