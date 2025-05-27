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
package org.apache.sis.geometries;

import java.util.List;
import java.util.Map;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Parent interface of any geometry.
 * <p>
 * Based on specification :
 * <ul>
 *  <li>ISO 19107</li>
 *  <li>OGC Simple Feature Access - https://www.ogc.org/standards/sfa</li>
 *  <li>Khronos GLTF-2 - https://github.com/KhronosGroup/glTF/tree/main/specification/2.0</li>
 *  <li>Khronos ANARI-1 - https://www.khronos.org/anari/</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Geometry", specification=ISO_19107) // section 6.4.4
public interface Geometry extends TransfiniteSet {

    /**
     * Get geometry coordinate system.
     *
     * Difference with ISO 19107 :
     * In 19107 their may be multiple RSID, the folloing RSID are used in special
     * kind of curves, for the sake of simplicity we only store the first rsid until
     * implementations of such curves will happen.
     *
     * @return never null
     */
    @UML(identifier="rsid", specification=ISO_19107) // section 6.4.4.20
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Set coordinate system in which the coordinates are declared.
     * This method does not transform the coordinates.
     *
     * @param crs , not null
     * @Throws IllegalArgumentException if coordinate system is not compatible with geometrie.
     */
    void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException;

    /**
     * Difference with ISO 19107 :
     * we return a single Metadata instead of a list of URI.
     */
    @UML(identifier="metadata", specification=ISO_19107) // section 6.4.4.18
    default Metadata getMetadata() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get geometry attributes type.
     *
     * @return attributes type, never null
     */
    AttributesType getAttributesType();

    /**
     * Get the geometry number of dimensions.<br>
     * This is the same as coordinate system dimension.
     *
     * @return number of dimension
     */
    @UML(identifier="coordinateDimension", specification=ISO_19107) // section 6.4.4.11
    default int getDimension() {
        return getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    @UML(identifier="dimension", specification=ISO_19107) // section 6.4.4.25
    default int getDimension(DirectPosition point) {
        throw new UnsupportedOperationException();
    }

    @UML(identifier="is3D", specification=ISO_19107) // section 6.4.4.13
    default boolean is3D() {
        return getDimension() == 3;
    }

    @UML(identifier="spatialDimension", specification=ISO_19107) // section 6.4.4.21
    default int getSpatialDimension() {
        return getDimension();
    }

    @UML(identifier="topologicalDimension", specification=ISO_19107) // section 6.4.4.22
    default int getTopologicDimension() {
        if (this instanceof Empty) {
            return -1;
        } else if (this instanceof Point) {
            return 0;
        } else if (this instanceof Curve) {
            return 1;
        } else if (this instanceof Surface) {
            return 2;
        } else if (this instanceof Solid) {
            return 3;
        }
        throw new UnsupportedOperationException();
    }

    @UML(identifier="boundaryType", specification=ISO_19107) // section 6.4.8
    default BoundaryType getBoundaryType() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the name of the instantiable subtype of Geometry of which this geometric object is an instantiable member.<br>
     * The name of the subtype of Geometry is returned as a string.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return geometry subtype name.
     */
    String getGeometryType();

    @UML(identifier="type", specification=ISO_19107) // section 6.4.4.23
    default List<GeometryType> getGeometryType2() {
        //TODO merge with getGeometryType
        throw new UnsupportedOperationException();
    }

    /**
     * The minimum bounding box for this Geometry, returned as a Geometry.<br>
     * The polygon is defined by the corner points of the bounding box [(MINX, MINY), (MAXX, MINY), (MAXX, MAXY), (MINX, MAXY), (MINX, MINY)].<br>
     * Minimums for Z and M may be added.<br>
     * The simplest representation of an Envelope is as two direct positions, one containing all the minimums, and another all the maximums.<br>
     * In some cases, this coordinate will be outside the range of validity for the Spatial Reference System.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return Envelope in geometry coordinate reference system.
     */
    @UML(identifier="envelope", specification=ISO_19107) // section 6.4.4.12
    Envelope getEnvelope();

    /**
     * The mathematical centroid for this Geometry as a Point.
     * The result is not guaranteed to be on this Geometry.
     *
     * Difference from OGC SFA : this method in declared on Surface and MultiSurface
     * but in ISO 19107 it is on Geometry.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @return centroid for this Geometry
     */
    @UML(identifier="centroid", specification=ISO_19107) // section 6.4.4.8
    default Point getCentroid() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    @UML(identifier="representativePoint", specification=ISO_19107) // section 6.4.4.19
    default Point getRepresentativePoint() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    @UML(identifier="convexHull", specification=ISO_19107) // section 6.4.4.10
    default Geometry getConvexHull() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    @UML(identifier="closure", specification=ISO_19107) // section 6.4.4.9
    default Geometry getClosure() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Exports this geometric object to a specific Well-known Text Representation of Geometry.
     *
     * Difference with ISO 19107 :
     * - this method is located on Encoding sub interface in the standard, it is placed
     *   on Geometry to match OGC SFA.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return this geometry in Well-known Text
     */
    @UML(identifier="asText", specification=ISO_19107) // section 6.4.4.5
    default String asText() {
        //TODO remove this method default when all classes implement it.
        return this.getClass().getSimpleName();
    }

    /**
     * Exports this geometric object to a specific Well-known Binary Representation of Geometry.
     *
     * Difference with ISO 19107 :
     * - this method is located on Encoding sub interface in the standard, it is placed
     *   on Geometry to match OGC SFA.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return this geometry in Well-known Binary
     */
    @UML(identifier="asBinary", specification=ISO_19107) // section 6.4.4.3
    default byte[] asBinary() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns TRUE if this geometric object is the empty Geometry.
     * If true, then this geometric object represents the empty point set ∅ for the coordinate space.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return true if empty.
     */
    @UML(identifier="isEmpty", specification=ISO_19107) // section 6.4.4.2
    boolean isEmpty();

    /**
     * Returns TRUE if this geometric object has no anomalous geometric points, such as self intersection or self tangency.
     * The description of each instantiable geometric class will include the specific conditions that cause an instance
     * of that class to be classified as not simple.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return true if geometry is simple
     */
    @UML(identifier="isSimple", specification=ISO_19107) // section 6.4.4.15
    default boolean isSimple() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    @UML(identifier="isCycle", specification=ISO_19107) // section 6.4.4.14
    default boolean isCycle() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    @UML(identifier="isValid", specification=ISO_19107) // section 6.4.4.16
    default boolean isValid() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the closure of the combinatorial boundary of this geometric object (Reference [1], section 3.12.2).
     * Because the result of this function is a closure, and hence topologically closed, the resulting boundary can be
     * represented using representational Geometry primitives (Reference [1], section 3.12.2).
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.2.2
     * @return boundary of the geometry
     */
    @UML(identifier="boundary", specification=ISO_19107) // section 6.4.4.7
    default Geometry boundary() {
        //TODO remove this method default when all classes implement it.
        throw new UnsupportedOperationException();
    }

    /**
     * Map of properties for user needs.
     * Those informations may be lost in geometry processes.
     *
     * @return Map, can be null if the geometry can not store additional informations.
     */
    default Map<String,Object> userProperties() {
        return null;
    }

}
