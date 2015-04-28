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
package org.apache.sis.metadata.iso.spatial;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.Record;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.spatial.Georeferenceable;
import org.opengis.metadata.spatial.GeolocationInformation;
import org.apache.sis.xml.Namespaces;


/**
 * Grid with cells irregularly spaced in any given geographic/map projection coordinate reference system.
 * Individual cells can be geolocated using geolocation information supplied with the data but cannot be
 * geolocated from the grid properties alone.
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
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Georeferenceable_Type", propOrder = {
    "controlPointAvailable",
    "orientationParameterAvailable",
    "orientationParameterDescription",
    "parameterCitations",
    "geolocationInformation"
})
@XmlRootElement(name = "MD_Georeferenceable")
@XmlSeeAlso(org.apache.sis.internal.jaxb.gmi.MI_Georeferenceable.class)
public class DefaultGeoreferenceable extends DefaultGridSpatialRepresentation implements Georeferenceable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -334605303200205283L;

    /**
     * Mask for the {@code controlPointAvailable} boolean value.
     *
     * @see #booleans
     */
    private static final byte CONTROL_POINT_MASK = TRANSFORMATION_MASK << 1;

    /**
     * Mask for the {@code orientationParameterAvailable} boolean value.
     *
     * @see #booleans
     */
    private static final byte OPERATION_MASK = CONTROL_POINT_MASK << 1;

    /**
     * Description of parameters used to describe sensor orientation.
     */
    private InternationalString orientationParameterDescription;

    /**
     * Terms which support grid data georeferencing.
     */
    private Record georeferencedParameters;

    /**
     * Reference providing description of the parameters.
     */
    private Collection<Citation> parameterCitations;

    /**
     * Information that can be used to geolocate the data.
     */
    private Collection<GeolocationInformation> geolocationInformation;

    /**
     * Constructs an initially empty georeferenceable.
     */
    public DefaultGeoreferenceable() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Georeferenceable)
     */
    public DefaultGeoreferenceable(final Georeferenceable object) {
        super(object);
        if (object != null) {
            if (object.isControlPointAvailable()) {
                booleans |= CONTROL_POINT_MASK;
            }
            if (object.isOrientationParameterAvailable()) {
                booleans |= OPERATION_MASK;
            }
            orientationParameterDescription = object.getOrientationParameterDescription();
            parameterCitations              = copyCollection(object.getParameterCitations(), Citation.class);
            geolocationInformation          = copyCollection(object.getGeolocationInformation(), GeolocationInformation.class);
            georeferencedParameters         = object.getGeoreferencedParameters();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultGeoreferenceable}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultGeoreferenceable} instance is created using the
     *       {@linkplain #DefaultGeoreferenceable(Georeferenceable) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultGeoreferenceable castOrCopy(final Georeferenceable object) {
        if (object == null || object instanceof DefaultGeoreferenceable) {
            return (DefaultGeoreferenceable) object;
        }
        return new DefaultGeoreferenceable(object);
    }

    /**
     * Returns an indication of whether or not control point(s) exists.
     *
     * @return Whether or not control point(s) exists.
     */
    @Override
    @XmlElement(name = "controlPointAvailability", required = true)
    public boolean isControlPointAvailable() {
        return (booleans & CONTROL_POINT_MASK) != 0;
    }

    /**
     * Sets an indication of whether or not control point(s) exists.
     *
     * @param newValue {@code true} if control points are available.
     */
    public void setControlPointAvailable(final boolean newValue) {
       checkWritePermission();
        if (newValue) {
            booleans |= CONTROL_POINT_MASK;
        } else {
            booleans &= ~CONTROL_POINT_MASK;
        }
    }

    /**
     * Returns an indication of whether or not orientation parameters are available.
     *
     * @return Whether or not orientation parameters are available.
     */
    @Override
    @XmlElement(name = "orientationParameterAvailability", required = true)
    public boolean isOrientationParameterAvailable() {
        return (booleans & OPERATION_MASK) != 0;
    }

    /**
     * Sets an indication of whether or not orientation parameters are available.
     *
     * @param newValue {@code true} if orientation parameter are available.
     */
    public void setOrientationParameterAvailable(final boolean newValue) {
        checkWritePermission();
        if (newValue) {
            booleans |= OPERATION_MASK;
        } else {
            booleans &= ~OPERATION_MASK;
        }
    }

    /**
     * Returns a description of parameters used to describe sensor orientation.
     *
     * @return Description of parameters used to describe sensor orientation, or {@code null}.
     */
    @Override
    @XmlElement(name = "orientationParameterDescription")
    public InternationalString getOrientationParameterDescription() {
        return orientationParameterDescription;
    }

    /**
     * Sets a description of parameters used to describe sensor orientation.
     *
     * @param newValue The new orientation parameter description.
     */
    public void setOrientationParameterDescription(final InternationalString newValue) {
        checkWritePermission();
        orientationParameterDescription = newValue;
    }

    /**
     * Returns the terms which support grid data georeferencing.
     *
     * @return Terms which support grid data georeferencing, or {@code null}.
     */
    @Override
/// @XmlElement(name = "georeferencedParameters", required = true)
    public Record getGeoreferencedParameters() {
        return georeferencedParameters;
    }

    /**
     * Sets the terms which support grid data georeferencing.
     *
     * @param newValue The new georeferenced parameters.
     */
    public void setGeoreferencedParameters(final Record newValue) {
        checkWritePermission();
        georeferencedParameters = newValue;
    }

    /**
     * Returns a reference providing description of the parameters.
     *
     * @return Reference providing description of the parameters.
     */
    @Override
    @XmlElement(name = "parameterCitation")
    public Collection<Citation> getParameterCitations() {
        return parameterCitations = nonNullCollection(parameterCitations, Citation.class);
    }

    /**
     * Sets a reference providing description of the parameters.
     *
     * @param newValues The new parameter citations.
     */
    public void setParameterCitations(final Collection<? extends Citation> newValues) {
        parameterCitations = writeCollection(newValues, parameterCitations, Citation.class);
    }

    /**
     * Returns the information that can be used to geolocate the data.
     *
     * @return A geolocalisation of the data.
     */
    @Override
    @XmlElement(name = "geolocationInformation", namespace = Namespaces.GMI, required = true)
    public Collection<GeolocationInformation> getGeolocationInformation() {
        return geolocationInformation = nonNullCollection(geolocationInformation, GeolocationInformation.class);
    }

    /**
     * Sets the information that can be used to geolocate the data.
     *
     * @param newValues The new geolocation information values.
     */
    public void setGeolocationInformation(final Collection<? extends GeolocationInformation> newValues) {
        geolocationInformation = writeCollection(newValues, geolocationInformation, GeolocationInformation.class);
    }
}
