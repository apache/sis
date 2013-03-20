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
package org.apache.sis.metadata.iso.extent;

import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.internal.metadata.ReferencingServices;


/**
 * Information about spatial, vertical, and temporal extent.
 * This interface has four optional attributes:
 * {@linkplain #getGeographicElements() geographic elements},
 * {@linkplain #getTemporalElements() temporal elements},
 * {@linkplain #getVerticalElements() vertical elements} and
 * {@linkplain #getDescription() description}.
 * At least one of the four shall be used.
 *
 * <p>In addition to the standard properties, SIS provides the following methods:</p>
 * <ul>
 *   <li>{@link #addElements(Envelope)} for adding extents inferred from the given envelope.</li>
 *   <li>{@link org.apache.sis.metadata.iso.MetadataObjects#getGeographicBoundingBox(Extent)}
 *       for extracting a global geographic bounding box.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.metadata.iso.MetadataObjects#getGeographicBoundingBox(Extent)
 */
@XmlType(name = "EX_Extent_Type", propOrder = {
    "description",
    "geographicElements",
    "temporalElements",
    "verticalElements"
})
@XmlRootElement(name = "EX_Extent")
public class DefaultExtent extends ISOMetadata implements Extent {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7812213837337326257L;

    /**
     * A geographic extent ranging from 180°W to 180°E and 90°S to 90°N.
     */
    public static final Extent WORLD;
    static {
        final DefaultExtent world = new DefaultExtent();
        world.setGeographicElements(Collections.singleton(DefaultGeographicBoundingBox.WORLD));
        world.freeze();
        WORLD = world;
    }

    /**
     * Returns the spatial and temporal extent for the referring object.
     */
    private InternationalString description;

    /**
     * Provides geographic component of the extent of the referring object
     */
    private Collection<GeographicExtent> geographicElements;

    /**
     * Provides temporal component of the extent of the referring object
     */
    private Collection<TemporalExtent> temporalElements;

    /**
     * Provides vertical component of the extent of the referring object
     */
    private Collection<VerticalExtent> verticalElements;

    /**
     * Constructs an initially empty extent.
     */
    public DefaultExtent() {
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
    public static DefaultExtent castOrCopy(final Extent object) {
        if (object == null || object instanceof DefaultExtent) {
            return (DefaultExtent) object;
        }
        final DefaultExtent copy = new DefaultExtent();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the spatial and temporal extent for the referring object.
     */
    @Override
    @XmlElement(name = "description")
    public synchronized InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the spatial and temporal extent for the referring object.
     *
     * @param newValue The new description.
     */
    public synchronized void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Provides geographic component of the extent of the referring object
     */
    @Override
    @XmlElement(name = "geographicElement")
    public synchronized Collection<GeographicExtent> getGeographicElements() {
        return geographicElements = nonNullCollection(geographicElements, GeographicExtent.class);
    }

    /**
     * Sets geographic component of the extent of the referring object.
     *
     * @param newValues The new geographic elements.
     */
    public synchronized void setGeographicElements(final Collection<? extends GeographicExtent> newValues) {
        geographicElements = copyCollection(newValues, geographicElements, GeographicExtent.class);
    }

    /**
     * Provides temporal component of the extent of the referring object.
     */
    @Override
    @XmlElement(name = "temporalElement")
    public synchronized Collection<TemporalExtent> getTemporalElements() {
        return temporalElements = nonNullCollection(temporalElements, TemporalExtent.class);
    }

    /**
     * Sets temporal component of the extent of the referring object.
     *
     * @param newValues The new temporal elements.
     */
    public synchronized void setTemporalElements(final Collection<? extends TemporalExtent> newValues) {
        temporalElements = copyCollection(newValues, temporalElements, TemporalExtent.class);
    }

    /**
     * Provides vertical component of the extent of the referring object.
     */
    @Override
    @XmlElement(name = "verticalElement")
    public synchronized Collection<VerticalExtent> getVerticalElements() {
        return verticalElements = nonNullCollection(verticalElements, VerticalExtent.class);
    }

    /**
     * Sets vertical component of the extent of the referring object.
     *
     * @param newValues The new vertical elements.
     */
    public synchronized void setVerticalElements(final Collection<? extends VerticalExtent> newValues) {
        verticalElements = copyCollection(newValues, verticalElements, VerticalExtent.class);
    }

    /**
     * Adds geographic, vertical or temporal extents inferred from the given envelope.
     * This method inspects the {@linkplain Envelope#getCoordinateReferenceSystem() envelope CRS}
     * and creates a {@link GeographicBoundingBox}, {@link VerticalExtent} or {@link TemporalExtent}
     * elements as needed.
     *
     * <p><b>Note:</b> This method is available only if the referencing module is on the classpath.</p>
     *
     * @param  envelope The envelope to use for inferring the additional extents.
     * @throws UnsupportedOperationException if the referencing module is not on the classpath.
     * @throws TransformException If a coordinate transformation was required and failed.
     *
     * @see DefaultGeographicBoundingBox#setBounds(Envelope)
     * @see DefaultVerticalExtent#setBounds(Envelope)
     * @see DefaultTemporalExtent#setBounds(Envelope)
     */
    public synchronized void addElements(final Envelope envelope) throws TransformException {
        checkWritePermission();
        ReferencingServices.getInstance().addElements(envelope, this);
    }
}
