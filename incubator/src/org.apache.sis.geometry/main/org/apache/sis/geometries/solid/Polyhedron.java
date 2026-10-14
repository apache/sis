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
package org.apache.sis.geometries.solid;

import java.util.List;
import javax.measure.Quantity;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Solid;
import org.apache.sis.geometries.SolidInterpolation;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.internal.shared.ConcatenatedDataPoints;
import org.apache.sis.geometries.internal.shared.DefaultPolyhedron;
import org.apache.sis.geometries.solid.polyhedron.AbstractPolyhedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;


/**
 * A solid is defined by its bounding surfaces. Each bounding surface is a closed, simple surface, also called a shell.
 *
 * Each solid has a unique exterior shell and any number of shells that are inside the exterior shell
 * and that describe voids. The interior shells do not intersect each other and cannot contain another interior shell.
 *
 * A polyhedron is a solid where each shell is a multi-polygon. 'Closed' means that the multi-polygon
 * shell is watertight, it splits space into two distinct regions: inside and outside of the shell. 'Simple' means
 * that the polygons that make up the shell do not intersect, they only touch each other along their common boundaries.
 *
 *
 * @author Johann Sorel (Geomatys)
 * @see https://docs.ogc.org/DRAFTS/21-045r1.html#polyhedron
 */
public sealed interface Polyhedron extends Solid
        permits AbstractPolyhedron,
                DefaultPolyhedron
{

    static final String TYPE = "POLYHEDRON";

    @Override
    default GeometryType getGeometryType() {
        return GeometryType.POLYHEDRON;
    }

    @Override
    default DataPointsType getDataPointsType() {
        return getExteriorShell().getDataPointsType();
    }

    /**
     * Returns the positions of the shells bounding this solid, the exterior one first,
     * then the interior ones in the order this solid holds them.
     */
    @Override
    default DataPoints getDataPoints() {
        return ConcatenatedDataPoints.of(getExteriorShell(), getInteriorShells());
    }

    /**
     * Returns {@link SolidInterpolation#NONE}: a polyhedron is defined by its boundary shells,
     * which leaves its interior unspecified.
     *
     * @see ISO 19107:2019 - 6.4.30
     */
    @Override
    default SolidInterpolation getInterpolation() {
        return SolidInterpolation.NONE;
    }

    /**
     * Returns the volume enclosed by the exterior shell minus the volumes enclosed by the interior shells.
     *
     * <p>The volume enclosed by a shell is computed with the divergence theorem: each face is split in
     * triangles, and the signed volumes of the tetrahedra joining those triangles to a common origin are
     * summed. The faces of a shell shall have consistent orientations, either all outward or all inward.
     * The holes of a face are subtracted whatever their orientation.</p>
     *
     * @throws UnsupportedOperationException if this polyhedron is not 3-dimensional.
     *
     * @see ISO 19107:2019 - 6.4.28.4
     */
    @Override
    default Quantity<?> getVolume() {
        if (getDimension() != 3) {
            throw new UnsupportedOperationException("The volume of a polyhedron is computed in 3 dimensions only.");
        }
        final GeometryCollection<Polygon> exterior = getExteriorShell();
        double volume = 0;
        if (exterior.getNumGeometries() != 0) {
            /*
             * The volume enclosed by a closed shell does not depend on the origin of the tetrahedra.
             * Taking a position of the shell avoids losing precision with large coordinate values.
             */
            final double[] origin = exterior.getGeometryN(0).getExteriorRing().getDataPoints().getPosition(0).toArrayDouble();
            volume = Math.abs(signedVolume(exterior, origin));
            for (final GeometryCollection<Polygon> interior : getInteriorShells()) {
                volume -= Math.abs(signedVolume(interior, origin));
            }
        }
        return Quantities.create(volume, Geometries.getVolumeUnit(this));
    }

    /**
     * Returns the signed volume enclosed by the given shell, positive if the faces are oriented outward.
     */
    private static double signedVolume(final GeometryCollection<Polygon> shell, final double[] origin) {
        double volume = 0;
        for (int i = 0, n = shell.getNumGeometries(); i < n; i++) {
            final Polygon face = shell.getGeometryN(i);
            double v = signedVolume(face.getExteriorRing().getDataPoints(), origin);
            for (final LinearRing hole : face.getInteriorRings()) {
                // A hole lies in the plane of its face, so its contribution has the sign of the face if both have the same orientation.
                final double h = signedVolume(hole.getDataPoints(), origin);
                v += (Math.signum(h) == Math.signum(v)) ? -h : h;
            }
            volume += v;
        }
        return volume;
    }

    /**
     * Returns the signed volume of the cone joining the given origin to the given planar ring.
     * The ring is split in a fan of triangles from its first position, and the volumes of the
     * tetrahedra joining the origin to those triangles are summed.
     */
    private static double signedVolume(final DataPoints ring, final double[] origin) {
        final int n = ring.size();
        if (n < 3) {
            return 0;
        }
        Vector<?> p = ring.getPosition(0);
        final double ax = p.get(0) - origin[0];
        final double ay = p.get(1) - origin[1];
        final double az = p.get(2) - origin[2];
        p = ring.getPosition(1);
        double bx = p.get(0) - origin[0];
        double by = p.get(1) - origin[1];
        double bz = p.get(2) - origin[2];
        double sum = 0;
        for (int i = 2; i < n; i++) {
            p = ring.getPosition(i);
            final double cx = p.get(0) - origin[0];
            final double cy = p.get(1) - origin[1];
            final double cz = p.get(2) - origin[2];
            sum += ax * (by*cz - bz*cy) + ay * (bz*cx - bx*cz) + az * (bx*cy - by*cx);
            bx = cx;
            by = cy;
            bz = cz;
        }
        return sum / 6;
    }

    /**
     * Returns the exterior shell of this polyhedron.
     *
     * @return exterior shell of this polyhedron.
     */
    GeometryCollection<Polygon> getExteriorShell();

    /**
     * Returns a list of void shell inside the polyhedron.
     *
     * @return all void shell inside the polyhedron
     */
    List<GeometryCollection<Polygon>> getInteriorShells();

}
