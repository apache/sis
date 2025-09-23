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
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.SpatialTemporalExtent;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;


/**
 * Extent with respect to date/time and spatial boundaries.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code EX_SpatialTemporalExtent}
 * {@code   ├─extent……………………………………} The date and time for the content of the dataset.
 * {@code   └─spatialExtent…………………} The spatial extent component of composite spatial and temporal extent.</div>
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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "EX_SpatialTemporalExtent_Type")
@XmlRootElement(name = "EX_SpatialTemporalExtent")
public class DefaultSpatialTemporalExtent extends DefaultTemporalExtent implements SpatialTemporalExtent {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2461142677245013474L;

    /**
     * The spatial extent component of composite
     * spatial and temporal extent.
     */
    @SuppressWarnings("serial")
    private Collection<GeographicExtent> spatialExtent;

    /**
     * Vertical extent component.
     */
    @SuppressWarnings("serial")
    private VerticalExtent verticalExtent;

    /**
     * Constructs an initially empty spatial-temporal extent.
     */
    public DefaultSpatialTemporalExtent() {
    }

    /**
     * Constructs a new spatial-temporal extent initialized to the specified values.
     *
     * @param spatialExtent   the spatial extent component of composite spatial and temporal extent.
     * @param verticalExtent  the vertical extent component, or {@code null} if none.
     * @param extent          the date and time for the content of the dataset, or {@code null} if unspecified.
     *
     * @since 0.5
     */
    public DefaultSpatialTemporalExtent(final GeographicExtent spatialExtent,
                                        final VerticalExtent verticalExtent,
                                        final TemporalExtent extent)
    {
        super(extent);
        this.verticalExtent = verticalExtent;
        this.spatialExtent  = singleton(spatialExtent, GeographicExtent.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(SpatialTemporalExtent)
     */
    public DefaultSpatialTemporalExtent(final SpatialTemporalExtent object) {
        super(object);
        if (object != null) {
            spatialExtent  = copyCollection(object.getSpatialExtent(), GeographicExtent.class);
            verticalExtent = object.getVerticalExtent();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultSpatialTemporalExtent}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultSpatialTemporalExtent} instance is created using the
     *       {@linkplain #DefaultSpatialTemporalExtent(SpatialTemporalExtent) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSpatialTemporalExtent castOrCopy(final SpatialTemporalExtent object) {
        if (object == null || object instanceof DefaultSpatialTemporalExtent) {
            return (DefaultSpatialTemporalExtent) object;
        }
        return new DefaultSpatialTemporalExtent(object);
    }

    /**
     * Returns the spatial extent component of composite spatial and temporal extent.
     *
     * @return the list of geographic extents (never {@code null}).
     */
    @Override
    @XmlElement(name = "spatialExtent", required = true)
    public Collection<GeographicExtent> getSpatialExtent() {
        return spatialExtent = nonNullCollection(spatialExtent, GeographicExtent.class);
    }

    /**
     * Sets the spatial extent component of composite spatial and temporal extent.
     *
     * @param  newValues  the new spatial extent.
     */
    public void setSpatialExtent(final Collection<? extends GeographicExtent> newValues) {
        spatialExtent = writeCollection(newValues, spatialExtent, GeographicExtent.class);
    }

    /**
     * Returns the vertical extent component.
     *
     * @return vertical extent component, or {@code null} if none.
     *
     * @since 0.5
     */
    @Override
    @XmlElement(name = "verticalExtent")
    public VerticalExtent getVerticalExtent() {
        return verticalExtent;
    }

    /**
     * Sets the vertical extent component.
     *
     * @param  newValue  the new vertical extent component.
     *
     * @since 0.5
     */
    public void setVerticalExtent(final VerticalExtent newValue) {
        checkWritePermission(verticalExtent);
        verticalExtent = newValue;
    }

    /**
     * Sets this spatiotemporal extent to values inferred from the specified envelope.
     * The given envelope shall have at least a spatial, vertical or temporal component.
     *
     * <p>The spatial component is handled as below:</p>
     * <ul>
     *   <li>If the given envelope has an horizontal component, then:
     *     <ul>
     *       <li>If the collection of {@linkplain #getSpatialExtent() spatial extents} contains a
     *           {@link GeographicBoundingBox}, then that bounding box will be updated or replaced
     *           by a bounding box containing the spatial component of the given envelope.</li>
     *       <li>Otherwise a new {@link DefaultGeographicBoundingBox} with the spatial component
     *           of the given envelope is added to the list of spatial extents.</li>
     *     </ul>
     *   </li>
     *   <li>All extraneous geographic extents are removed.
     *       Non-geographic extents (e.g. descriptions and polygons) are left unchanged.</li>
     * </ul>
     *
     * <p>Other dimensions are handled in a more straightforward way:</p>
     * <ul>
     *   <li>The {@linkplain #getVerticalExtent() vertical extent} is set to the vertical component
     *       of the given envelope, or {@code null} if none.</li>
     *   <li>The {@linkplain #getExtent() temporal extent} is set to the temporal component
     *       of the given envelope, or {@code null} if none.</li>
     * </ul>
     *
     * <b>Note:</b> This method is available only if the {@code org.apache.sis.referencing} module
     * is available on the module path.
     *
     * @param  envelope  the envelope to use for setting this spatiotemporal extent.
     * @throws UnsupportedOperationException if the referencing module is not on the module path.
     * @throws TransformException if the envelope cannot be transformed to a temporal extent.
     */
    @Override
    public void setBounds(final Envelope envelope) throws TransformException {
        checkWritePermission(valueIfDefined(spatialExtent));
        if (!ReferencingServices.getInstance().setBounds(envelope, this)) {
            throw new NotSpatioTemporalException(3, envelope);
        }
    }
}
