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
package org.apache.sis.geometries.operation;

import org.apache.sis.util.internal.shared.DoubleDouble;

/**
 * Java port to Jonathan R. Shewchuk. Adaptive Precision Floating-Point Arithmetic and Fast Robust Predicates for Computational Geometry.
 * <p>
 * This class different slightly in it's algorithms by using DoubleDouble.
 *
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://www.cs.cmu.edu/~quake/robust.html">Adaptive Precision Floating-Point Arithmetic and Fast Robust Predicates for Computational Geometry</a>
 */
public final class ShewchukPredicates {

    private ShewchukPredicates(){}

    /**
     * Exact difference {@code a - b} of two doubles, captured as a {@link DoubleDouble}.
     * Plain {@code double} subtraction is not generally exact (Sterbenz's lemma does not
     * apply for arbitrary inputs), so the rounding error must be preserved for the
     * determinants below to be robust.
     */
    private static DoubleDouble diff(double a, double b) {
        return DoubleDouble.sum(a, -b);
    }

    /**
     * Test the orientation of point c relative to the line defined by points a and b.
     *
     * @param ax point a X
     * @param ay point a Y
     * @param bx point b X
     * @param by point b Y
     * @param cx point c X
     * @param cy point c Y
     * @return positive if a,b,c are in counterclockwise order,
     *         negative if a,b,c are in clockwise order,
     *         zero if a,b,c are collinear
     */
    public static double orient2d(double ax, double ay,
                                  double bx, double by,
                                  double cx, double cy) {
        final DoubleDouble acx = diff(ax, cx);
        final DoubleDouble bcx = diff(bx, cx);
        final DoubleDouble acy = diff(ay, cy);
        final DoubleDouble bcy = diff(by, cy);
        final DoubleDouble det = acx.multiply(bcy).subtract(acy.multiply(bcx));
        return det.doubleValue();
    }

    /**
     * Test the orientation of point d relative to the plane defined by points a, b and c.
     *
     * @param ax point a X
     * @param ay point a Y
     * @param az point a Z
     * @param bx point b X
     * @param by point b Y
     * @param bz point b Z
     * @param cx point c X
     * @param cy point c Y
     * @param cz point c Z
     * @param dx point d X
     * @param dy point d Y
     * @param dz point d Z
     * @return positive if d lies below the plane through a,b,c, such that a,b,c,d form
     *         a positively oriented tetrahedron ("below" meaning in the sense of the
     *         oriented normal of a,b,c seen counterclockwise),
     *         negative if d lies above,
     *         zero if a,b,c,d are coplanar
     */
    public static double orient3d(double ax, double ay, double az,
                                   double bx, double by, double bz,
                                   double cx, double cy, double cz,
                                   double dx, double dy, double dz) {
        final DoubleDouble adx = diff(ax, dx);
        final DoubleDouble ady = diff(ay, dy);
        final DoubleDouble adz = diff(az, dz);
        final DoubleDouble bdx = diff(bx, dx);
        final DoubleDouble bdy = diff(by, dy);
        final DoubleDouble bdz = diff(bz, dz);
        final DoubleDouble cdx = diff(cx, dx);
        final DoubleDouble cdy = diff(cy, dy);
        final DoubleDouble cdz = diff(cz, dz);

        final DoubleDouble det = adx.multiply(bdy.multiply(cdz).subtract(bdz.multiply(cdy)))
                .add(bdx.multiply(cdy.multiply(adz).subtract(cdz.multiply(ady))))
                .add(cdx.multiply(ady.multiply(bdz).subtract(adz.multiply(bdy))));
        return det.doubleValue();
    }

    /**
     * Test whether point d lies inside, outside or exactly on the circle passing
     * through points a, b and c.
     * Points a, b, c are expected to be in counterclockwise order; if they are in
     * clockwise order the sign of the result is reversed.
     *
     * @param ax point a X
     * @param ay point a Y
     * @param bx point b X
     * @param by point b Y
     * @param cx point c X
     * @param cy point c Y
     * @param dx point d X
     * @param dy point d Y
     * @return positive if d is inside the circle through a,b,c,
     *         negative if d is outside,
     *         zero if d lies exactly on the circle
     */
    public static double inCircle(double ax, double ay,
                                  double bx, double by,
                                  double cx, double cy,
                                  double dx, double dy) {
        final DoubleDouble adx = diff(ax, dx);
        final DoubleDouble ady = diff(ay, dy);
        final DoubleDouble bdx = diff(bx, dx);
        final DoubleDouble bdy = diff(by, dy);
        final DoubleDouble cdx = diff(cx, dx);
        final DoubleDouble cdy = diff(cy, dy);

        final DoubleDouble alift = adx.multiply(adx).add(ady.multiply(ady));
        final DoubleDouble blift = bdx.multiply(bdx).add(bdy.multiply(bdy));
        final DoubleDouble clift = cdx.multiply(cdx).add(cdy.multiply(cdy));

        final DoubleDouble det = alift.multiply(bdx.multiply(cdy).subtract(bdy.multiply(cdx)))
                       .subtract(blift.multiply(adx.multiply(cdy).subtract(ady.multiply(cdx))))
                            .add(clift.multiply(adx.multiply(bdy).subtract(ady.multiply(bdx))));
        return det.doubleValue();
    }

    /**
     * Test whether point e lies inside, outside or exactly on the sphere passing
     * through points a, b, c and d.
     * Points a, b, c, d are expected to be positively oriented (as tested by
     * {@link #orient3d}); if they are negatively oriented the sign of the result
     * is reversed.
     *
     * @param ax point a X
     * @param ay point a Y
     * @param az point a Z
     * @param bx point b X
     * @param by point b Y
     * @param bz point b Z
     * @param cx point c X
     * @param cy point c Y
     * @param cz point c Z
     * @param dx point d X
     * @param dy point d Y
     * @param dz point d Z
     * @param ex point e X
     * @param ey point e Y
     * @param ez point e Z
     * @return positive if e is inside the sphere through a,b,c,d,
     *         negative if e is outside,
     *         zero if e lies exactly on the sphere
     */
    public static double inSphere(double ax, double ay, double az,
                                   double bx, double by, double bz,
                                   double cx, double cy, double cz,
                                   double dx, double dy, double dz,
                                   double ex, double ey, double ez) {
        final DoubleDouble aex = diff(ax, ex);
        final DoubleDouble aey = diff(ay, ey);
        final DoubleDouble aez = diff(az, ez);
        final DoubleDouble bex = diff(bx, ex);
        final DoubleDouble bey = diff(by, ey);
        final DoubleDouble bez = diff(bz, ez);
        final DoubleDouble cex = diff(cx, ex);
        final DoubleDouble cey = diff(cy, ey);
        final DoubleDouble cez = diff(cz, ez);
        final DoubleDouble dex = diff(dx, ex);
        final DoubleDouble dey = diff(dy, ey);
        final DoubleDouble dez = diff(dz, ez);

        final DoubleDouble ab = aex.multiply(bey).subtract(bex.multiply(aey));
        final DoubleDouble bc = bex.multiply(cey).subtract(cex.multiply(bey));
        final DoubleDouble cd = cex.multiply(dey).subtract(dex.multiply(cey));
        final DoubleDouble da = dex.multiply(aey).subtract(aex.multiply(dey));
        final DoubleDouble ac = aex.multiply(cey).subtract(cex.multiply(aey));
        final DoubleDouble bd = bex.multiply(dey).subtract(dex.multiply(bey));

        final DoubleDouble abc = aez.multiply(bc).subtract(bez.multiply(ac)).add(cez.multiply(ab));
        final DoubleDouble bcd = bez.multiply(cd).subtract(cez.multiply(bd)).add(dez.multiply(bc));
        final DoubleDouble cda = cez.multiply(da).add(dez.multiply(ac)).add(aez.multiply(cd));
        final DoubleDouble dab = dez.multiply(ab).add(aez.multiply(bd)).add(bez.multiply(da));

        final DoubleDouble alift = aex.multiply(aex).add(aey.multiply(aey)).add(aez.multiply(aez));
        final DoubleDouble blift = bex.multiply(bex).add(bey.multiply(bey)).add(bez.multiply(bez));
        final DoubleDouble clift = cex.multiply(cex).add(cey.multiply(cey)).add(cez.multiply(cez));
        final DoubleDouble dlift = dex.multiply(dex).add(dey.multiply(dey)).add(dez.multiply(dez));

        final DoubleDouble det = dlift.multiply(abc).subtract(clift.multiply(dab))
                .add(blift.multiply(cda)).subtract(alift.multiply(bcd));
        return det.doubleValue();
    }
}
