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
import org.opengis.geometry.Geometry;
import org.opengis.metadata.extent.BoundingPolygon;


/**
 * Boundary enclosing the dataset, expressed as the closed set of
 * (<var>x</var>,<var>y</var>) coordinates of the polygon.
 * The last point replicates first point.
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
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "EX_BoundingPolygon_Type")
@XmlRootElement(name = "EX_BoundingPolygon")
public class DefaultBoundingPolygon extends AbstractGeographicExtent implements BoundingPolygon {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3404580901560754370L;

    /**
     * The sets of points defining the bounding polygon.
     */
    private Collection<Geometry> polygons;

    /**
     * Constructs an initially empty bounding polygon.
     */
    public DefaultBoundingPolygon() {
    }

    /**
     * Creates a bounding polygon initialized to the specified polygon.
     *
     * @param polygon The sets of points defining the bounding polygon.
     */
    public DefaultBoundingPolygon(final Geometry polygon) {
        polygons = singleton(polygon, Geometry.class);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(BoundingPolygon)
     */
    public DefaultBoundingPolygon(final BoundingPolygon object) {
        super(object);
        if (object != null) {
            polygons = copyCollection(object.getPolygons(), Geometry.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultBoundingPolygon}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultBoundingPolygon} instance is created using the
     *       {@linkplain #DefaultBoundingPolygon(BoundingPolygon) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultBoundingPolygon castOrCopy(final BoundingPolygon object) {
        if (object == null || object instanceof DefaultBoundingPolygon) {
            return (DefaultBoundingPolygon) object;
        }
        return new DefaultBoundingPolygon(object);
    }

    /**
     * Returns the sets of points defining the bounding polygon or other geometry.
     *
     * @return The sets of points defining the resource boundary.
     */
    @Override
    @XmlElement(name = "polygon", required = true)
    public Collection<Geometry> getPolygons() {
        return polygons = nonNullCollection(polygons, Geometry.class);
    }

    /**
     * Sets the sets of points defining the resource boundary.
     *
     * @param newValues The new boundaries.
     */
    public void setPolygons(final Collection<? extends Geometry> newValues) {
        polygons = writeCollection(newValues, polygons, Geometry.class);
    }
}
