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
 * Grid with cells irregularly spaced in any given geographic/map projection coordinate
 * system, whose individual cells can be geolocated using geolocation information
 * supplied with the data but cannot be geolocated from the grid properties alone.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
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
    private static final long serialVersionUID = 7369639367164358759L;

    /**
     * Indication of whether or not control point(s) exists.
     */
    private boolean controlPointAvailable;

    /**
     * Indication of whether or not orientation parameters are available.
     */
    private boolean orientationParameterAvailable;

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
    public static DefaultGeoreferenceable castOrCopy(final Georeferenceable object) {
        if (object == null || object instanceof DefaultGeoreferenceable) {
            return (DefaultGeoreferenceable) object;
        }
        final DefaultGeoreferenceable copy = new DefaultGeoreferenceable();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns an indication of whether or not control point(s) exists.
     */
    @Override
    @XmlElement(name = "controlPointAvailability", required = true)
    public synchronized boolean isControlPointAvailable() {
        return controlPointAvailable;
    }

    /**
     * Sets an indication of whether or not control point(s) exists.
     *
     * @param newValue {@code true} if control points are available.
     */
    public synchronized void setControlPointAvailable(final boolean newValue) {
       checkWritePermission();
       controlPointAvailable = newValue;
    }

    /**
     * Returns an indication of whether or not orientation parameters are available.
     */
    @Override
    @XmlElement(name = "orientationParameterAvailability", required = true)
    public synchronized boolean isOrientationParameterAvailable() {
        return orientationParameterAvailable;
    }

    /**
     * Sets an indication of whether or not orientation parameters are available.
     *
     * @param newValue {@code true} if orientation parameter are available.
     */
    public synchronized void setOrientationParameterAvailable(final boolean newValue) {
        checkWritePermission();
        orientationParameterAvailable = newValue;
    }

    /**
     * Returns a description of parameters used to describe sensor orientation.
     */
    @Override
    @XmlElement(name = "orientationParameterDescription")
    public synchronized InternationalString getOrientationParameterDescription() {
        return orientationParameterDescription;
    }

    /**
     * Sets a description of parameters used to describe sensor orientation.
     *
     * @param newValue The new orientation parameter description.
     */
    public synchronized void setOrientationParameterDescription(final InternationalString newValue) {
        checkWritePermission();
        orientationParameterDescription = newValue;
    }

    /**
     * Returns the terms which support grid data georeferencing.
     */
    @Override
/// @XmlElement(name = "georeferencedParameters", required = true)
    public synchronized Record getGeoreferencedParameters() {
        return georeferencedParameters;
    }

    /**
     * Sets the terms which support grid data georeferencing.
     *
     * @param newValue The new georeferenced parameters.
     */
    public synchronized void setGeoreferencedParameters(final Record newValue) {
        checkWritePermission();
        georeferencedParameters = newValue;
    }

    /**
     * Returns a reference providing description of the parameters.
     */
    @Override
    @XmlElement(name = "parameterCitation")
    public synchronized Collection<Citation> getParameterCitations() {
        return parameterCitations = nonNullCollection(parameterCitations, Citation.class);
    }

    /**
     * Sets a reference providing description of the parameters.
     *
     * @param newValues The new parameter citations.
     */
    public synchronized void setParameterCitations(final Collection<? extends Citation> newValues) {
        parameterCitations = writeCollection(newValues, parameterCitations, Citation.class);
    }

    /**
     * Returns the information that can be used to geolocate the data.
     *
     * @todo This attribute is declared as mandatory in ISO 19115-2. However metadata compliant
     *       with ISO 19115 (without the -2 part) do not contains this attribute. How should we
     *       handle the XML formatting for this one?
     */
    @Override
    @XmlElement(name = "geolocationInformation", namespace = Namespaces.GMI, required = true)
    public synchronized Collection<GeolocationInformation> getGeolocationInformation() {
        return geolocationInformation = nonNullCollection(geolocationInformation, GeolocationInformation.class);
    }

    /**
     * Sets the information that can be used to geolocate the data.
     *
     * @param newValues The new geolocation information values.
     */
    public synchronized void setGeolocationInformation(final Collection<? extends GeolocationInformation> newValues) {
        geolocationInformation = writeCollection(newValues, geolocationInformation, GeolocationInformation.class);
    }
}
