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
import javax.measure.quantity.Length;
import static org.opengis.annotation.Specification.ISO_19107;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.geometries.math.Vector;


/**
 * A Surface is a 2-dimensional geometric object.
 *
 * A simple Surface may consists of a single “patch” that is associated with one “exterior boundary” and 0 or more
 * “interior” boundaries. A single such Surface patch in 3-dimensional space is isometric to planar Surfaces,
 * by a simple affine rotation matrix that rotates the patch onto the plane z = 0. If the patch is not vertical, the
 * projection onto the same plane is an isomorphism, and can be represented as a linear transformation, i.e. an affine.
 *
 * Polyhedral Surfaces are formed by “stitching” together such simple Surfaces patches along their common boundaries.
 * Such polyhedral Surfaces in a 3-dimensional space may not be planar as a whole, depending on the orientation of
 * their planar normals (Reference [1], sections 3.12.9.1, and 3.12.9.3). If all the patches are in alignment
 * (their normals are parallel), then the whole stitched polyhedral surface is co-planar and can be represented
 * as a single patch if it is connected.
 *
 * The boundary of a simple Surface is the set of closed Curves corresponding to its “exterior” and “interior”
 * boundaries (Reference [1], section 3.12.9.4).
 *
 * The only instantiable subclasses of Surface defined in this standard are Polygon and PolyhedralSurface.
 * A Polygon is a simple Surface that is planar. A PolyhedralSurface is a simple surface, consisting of some number
 * of Polygon patches or facets. If a PolyhedralSurface is closed, then it bounds a solid.
 * A MultiSurface containing a set of closed PolyhedralSurfaces can be used to represent a Solid object with holes.
 *
 * @author Johann Sorel (Geomatys)
 */
@UML(identifier="Surface", specification=ISO_19107) // section 6.4.25
public interface Surface extends Orientable {

    /**
     * The area of this Surface, as measured in the spatial reference system of this Surface.
     *
     * Difference with ISO 19107 : should return an Area instance.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @return area of the surface.
     */
    @UML(identifier="area", specification=ISO_19107) // section 6.4.25.7
    default double getArea() {
        throw new UnsupportedOperationException();
    }

    /**
     * The mathematical centroid for this Surface as a Point.
     * The result is not guaranteed to be on this Surface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @return centroid for this Surface
     */
    @Override
    default Point getCentroid() {
        throw new UnsupportedOperationException();
    }

    /**
     * A Point guaranteed to be on this Surface.
     *
     * @see OGC Simple Feature Access 1.2.1 - 6.1.10.2
     * @return point guaranteed to be on this Surface.
     */
    default Point getPointOnSurface() {
        throw new UnsupportedOperationException();
    }


    @UML(identifier="boundary", specification=ISO_19107) // section 6.4.25.2
    default Geometry getBoundary() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="interpolation", specification=ISO_19107) // section 6.4.25.3
    default List<SurfaceInterpolation> getInterpolation() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="numDerivativesBoundary", specification=ISO_19107) // section 6.4.25.4
    default Integer getNumDerivativesBoundary() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="numDerivativeInterior", specification=ISO_19107) // section 6.4.25.5
    default Integer getNumDerivativeInterior() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="perimeter", specification=ISO_19107) // section 6.4.25.6
    default Length getPerimeter() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="dataPoint", specification=ISO_19107) // section 6.4.25.8
    default List<DirectPosition> getDataPoints() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="controlPoint", specification=ISO_19107) // section 6.4.25.9
    default List<DirectPosition> getControlPoints() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="knot", specification=ISO_19107) // section 6.4.25.10
    default List<Knot> getKnots() {
        //TODO
        throw new UnsupportedOperationException();
    }

    @UML(identifier="upNormal", specification=ISO_19107) // section 6.4.25.11
    default Vector upNormal(DirectPosition point) {
        //TODO
        throw new UnsupportedOperationException();
    }
}
