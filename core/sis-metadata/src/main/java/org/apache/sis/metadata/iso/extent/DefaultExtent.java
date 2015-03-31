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
package org.apache.sis.metadata.iso.extent;

import java.util.Collection;
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
import org.apache.sis.util.iso.Types;
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
 *   <li>{@link Extents#getGeographicBoundingBox(Extent)} for extracting a global geographic bounding box.</li>
 * </ul>
 *
 * <div class="section">Limitations</div>
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
 *
 * @see Extents#getGeographicBoundingBox(Extent)
 * @see org.apache.sis.referencing.AbstractReferenceSystem#getDomainOfValidity()
 * @see org.apache.sis.referencing.datum.AbstractDatum#getDomainOfValidity()
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
    private static final long serialVersionUID = 2979058128422252800L;

    /**
     * The spatial and temporal extent for the referring object.
     */
    private InternationalString description;

    /**
     * Provides geographic component of the extent of the referring object.
     */
    private Collection<GeographicExtent> geographicElements;

    /**
     * Provides vertical component of the extent of the referring object.
     */
    private Collection<VerticalExtent> verticalElements;

    /**
     * Provides temporal component of the extent of the referring object.
     */
    private Collection<TemporalExtent> temporalElements;

    /**
     * Constructs an initially empty extent.
     */
    public DefaultExtent() {
    }

    /**
     * Constructs an extent initialized to the given description or components.
     * Any argument given to this constructor can be {@code null}.
     * While a valid {@code Extent} requires at least one component to be non-null,
     * this constructor does not perform such verification.
     *
     * @param description        A description, or {@code null} if none.
     * @param geographicElements A geographic component, or {@code null} if none.
     * @param verticalElements   A vertical component, or {@code null} if none.
     * @param temporalElements   A temporal component, or {@code null} if none.
     */
    public DefaultExtent(final CharSequence     description,
                         final GeographicExtent geographicElements,
                         final VerticalExtent   verticalElements,
                         final TemporalExtent   temporalElements)
    {
        this.description        = Types.toInternationalString(description);
        this.geographicElements = singleton(geographicElements, GeographicExtent.class);
        this.verticalElements   = singleton(verticalElements,   VerticalExtent.class);
        this.temporalElements   = singleton(temporalElements,   TemporalExtent.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Extent)
     */
    public DefaultExtent(final Extent object) {
        super(object);
        if (object != null) {
            description        = object.getDescription();
            geographicElements = copyCollection(object.getGeographicElements(), GeographicExtent.class);
            temporalElements   = copyCollection(object.getTemporalElements(),   TemporalExtent.class);
            verticalElements   = copyCollection(object.getVerticalElements(),   VerticalExtent.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultExtent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultExtent} instance is created using the
     *       {@linkplain #DefaultExtent(Extent) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultExtent castOrCopy(final Extent object) {
        if (object == null || object instanceof DefaultExtent) {
            return (DefaultExtent) object;
        }
        return new DefaultExtent(object);
    }

    /**
     * Returns the spatial and temporal extent for the referring object.
     *
     * @return The spatial and temporal extent, or {@code null} in none.
     */
    @Override
    @XmlElement(name = "description")
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the spatial and temporal extent for the referring object.
     *
     * @param newValue The new description.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Provides geographic component of the extent of the referring object
     *
     * @return The geographic extent, or an empty set if none.
     */
    @Override
    @XmlElement(name = "geographicElement")
    public Collection<GeographicExtent> getGeographicElements() {
        return geographicElements = nonNullCollection(geographicElements, GeographicExtent.class);
    }

    /**
     * Sets geographic component of the extent of the referring object.
     *
     * @param newValues The new geographic elements.
     */
    public void setGeographicElements(final Collection<? extends GeographicExtent> newValues) {
        geographicElements = writeCollection(newValues, geographicElements, GeographicExtent.class);
    }

    /**
     * Provides vertical component of the extent of the referring object.
     *
     * @return The vertical extent, or an empty set if none.
     */
    @Override
    @XmlElement(name = "verticalElement")
    public Collection<VerticalExtent> getVerticalElements() {
        return verticalElements = nonNullCollection(verticalElements, VerticalExtent.class);
    }

    /**
     * Sets vertical component of the extent of the referring object.
     *
     * @param newValues The new vertical elements.
     */
    public void setVerticalElements(final Collection<? extends VerticalExtent> newValues) {
        verticalElements = writeCollection(newValues, verticalElements, VerticalExtent.class);
    }

    /**
     * Provides temporal component of the extent of the referring object.
     *
     * @return The temporal extent, or an empty set if none.
     */
    @Override
    @XmlElement(name = "temporalElement")
    public Collection<TemporalExtent> getTemporalElements() {
        return temporalElements = nonNullCollection(temporalElements, TemporalExtent.class);
    }

    /**
     * Sets temporal component of the extent of the referring object.
     *
     * @param newValues The new temporal elements.
     */
    public void setTemporalElements(final Collection<? extends TemporalExtent> newValues) {
        temporalElements = writeCollection(newValues, temporalElements, TemporalExtent.class);
    }

    /**
     * Adds geographic, vertical or temporal extents inferred from the given envelope.
     * This method inspects the {@linkplain Envelope#getCoordinateReferenceSystem() envelope CRS}
     * and creates a {@link GeographicBoundingBox}, {@link VerticalExtent} or {@link TemporalExtent}
     * elements as needed.
     *
     * <p><b>Note:</b> this method is available only if the referencing module is on the classpath.</p>
     *
     * @param  envelope The envelope to use for inferring the additional extents.
     * @throws UnsupportedOperationException if the referencing module is not on the classpath.
     * @throws TransformException if a coordinate transformation was required and failed.
     *
     * @see DefaultGeographicBoundingBox#setBounds(Envelope)
     * @see DefaultVerticalExtent#setBounds(Envelope)
     * @see DefaultTemporalExtent#setBounds(Envelope)
     */
    public void addElements(final Envelope envelope) throws TransformException {
        checkWritePermission();
        ReferencingServices.getInstance().addElements(envelope, this);
    }
}
