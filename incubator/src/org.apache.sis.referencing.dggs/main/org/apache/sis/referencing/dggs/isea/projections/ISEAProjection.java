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
package org.apache.sis.referencing.dggs.isea.projections;

import org.apache.sis.referencing.dggs.isea.generic.Trigonometric;
import org.apache.sis.referencing.dggs.isea.geo.WGS84;
import org.apache.sis.referencing.dggs.isea.geometry.FaceCoordinates;
import org.apache.sis.referencing.dggs.isea.geometry.GeoCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Icosahedron Snyder equal-area (ISEA) projection
 *
 * The ISEA projection is a projects a sphere on the icosahedron. Thereby the size of areas mapped to the icosahedron
 * are preserved. Angles and distances are however slightly distorted. The angular distortion is below 17.27 degree, and
 * the scale variation is less than 16.3 per cent.
 *
 * The projection has been proposed and has been described in detail by:
 *
 * John P. Snyder: An equal-area map projection for polyhedral globes. Cartographica, 29(1), 10–21, 1992.
 * doi:10.3138/27H7-8K88-4882-1752
 *
 * Another description and improvements can be found in:
 *
 * Erika Harrison, Ali Mahdavi-Amiri, and Faramarz Samavati: Optimization of inverse Snyder polyhedral projection.
 * International Conference on Cyberworlds 2011. doi:10.1109/CW.2011.36
 *
 * Erika Harrison, Ali Mahdavi-Amiri, and Faramarz Samavati: Analysis of inverse Snyder optimizations.
 * In: Marina L. Gavrilova, and C. J. Kenneth Tan (Eds): Transactions on Computational Science XVI. Heidelberg,
 * Springer, 2012. pp. 134–148. doi:10.1007/978-3-642-32663-9_8
 *
 * @author Franz-Benjamin Mocnik
 */
public class ISEAProjection {
    // constants
    private static final double _goldenRatio = (1 + Math.sqrt(5)) / 2.;
    // radius
    private final double _RR_earth = (1 / (2 * Math.sqrt(5)) + 1 / 6.) * Math.sqrt(Math.PI * Math.sqrt(3)); // R' / R
    private final double _R; // R'
    private final double _R_earth = WGS84.radiusAuthalic; // R // authalic sphere radius for WGS84 [km]
    // faces
    private final int _numberOfFaces = 20;
    // orientation
    private double _orientationLat = 0;
    private double _orientationLon = 0;
    // spherical constants
    private final double _g; // g
    private final int _G = 36; // G
    private final int _theta = 30; // \theta
    // distortion
    private final double _omega = 17.27; // \omega
    private final double _a = 1.163; // a
    private final double _b = .860; // b
    // face constants
    private final double __E; // E
    private final double __F = Trigonometric.atan(1 / (2 * Math.pow(ISEAProjection._goldenRatio, 2))); // F = \atan(1 / (2 \phi^2)) where \phi = (1 + \sqrt{5}) / 2 is the golden ratio; needs some thinking to derive
    // alternative computation F = 90 + g - 2 * \atan(\phi); formula can easily be derived from the cartesian coordinates of the vertices of the icosahedron
    private final double __G; // G // this value incorporates R', and not R, as is stated wrongly in the paper by Snyder
    private final int __X = 36; // half the difference in latitude between two horizontally adjacent faces
    private final double[] __lats = new double[20];
    private final int[] __lons = new int[20];
    // precision
    private static final double _precision = 1e-9;
    private static final double _precisionPerDefinition = 1e-5;
    // computed values
    private final double _2R; // 2 R'
    private final double __EF; // E - F
    private final int _AzMax = 2 * (90 - this._theta); // 2 (90 - \theta)
    private final double _tan_g; // \tan g
    private final double _cosG = Trigonometric.cos(this._G); // \cos G
    private final double _cotTheta = Trigonometric.cot(this._theta); // \cot \theta
    private final double _2cotTheta = 2 * this._cotTheta; // 2 \cot \theta
    private final double _pi_R_earth2_180 = Math.PI * Math.pow(this._R_earth, 2) / 180; // \pi R^2 / 180
    private final double _R_tan_g; // R' \tan g
    private final double _R_tan_g_2; // R'^2 * \tan^2 g
    private final double _sinG_cos_g; // \sin G \cos g
    private final double _G_180 = this._G - 180.; // G - 180

    public ISEAProjection() {
        // computations
        this._g = this.__F + 2 * Trigonometric.atan(ISEAProjection._goldenRatio) - 90;
        this._R = this._RR_earth * this._R_earth;
        this.__E = 90 - this._g;
        this.__G = this._R * Trigonometric.tan(this._g) * Math.sqrt(3) / 2.;
        this._2R = 2 * this._R;
        this.__EF = this.__E - this.__F;
        this._tan_g = Trigonometric.tan(this._g);
        this._R_tan_g = this._R * this._tan_g;
        this._R_tan_g_2 = Math.pow(this._R_tan_g, 2);
        this._sinG_cos_g = Trigonometric.sin(this._G) * Trigonometric.cos(this._g);

        // angles of the faces
        this.__lats[0] = this.__E;
        this.__lats[1] = this.__E;
        this.__lats[2] = this.__E;
        this.__lats[3] = this.__E;
        this.__lats[4] = this.__E;
        this.__lats[5] = this.__F;
        this.__lats[6] = this.__F;
        this.__lats[7] = this.__F;
        this.__lats[8] = this.__F;
        this.__lats[9] = this.__F;
        this.__lats[10] = -this.__F;
        this.__lats[11] = -this.__F;
        this.__lats[12] = -this.__F;
        this.__lats[13] = -this.__F;
        this.__lats[14] = -this.__F;
        this.__lats[15] = -this.__E;
        this.__lats[16] = -this.__E;
        this.__lats[17] = -this.__E;
        this.__lats[18] = -this.__E;
        this.__lats[19] = -this.__E;
        this.__lons[0] = -4 * this.__X;
        this.__lons[1] = -2 * this.__X;
        this.__lons[2] = 0;
        this.__lons[3] = 2 * this.__X;
        this.__lons[4] = 4 * this.__X;
        this.__lons[5] = -4 * this.__X;
        this.__lons[6] = -2 * this.__X;
        this.__lons[7] = 0;
        this.__lons[8] = 2 * this.__X;
        this.__lons[9] = 4 * this.__X;
        this.__lons[10] = -3 * this.__X;
        this.__lons[11] = -this.__X;
        this.__lons[12] = this.__X;
        this.__lons[13] = 3 * this.__X;
        this.__lons[14] = 5 * this.__X;
        this.__lons[15] = -3 * this.__X;
        this.__lons[16] = -this.__X;
        this.__lons[17] = this.__X;
        this.__lons[18] = 3 * this.__X;
        this.__lons[19] = 5 * this.__X;
    }

    /**
     * @return spherical distance from center of a face to any of its vertices on the sphere; in degrees
     */
    public double sphericalDistanceFromCenterToVerticesOnSphere() {
        return this._g;
    }

    /**
     * @return number of faces of the icosahedron
     */
    public int numberOfFaces() {
        return this._numberOfFaces;
    }

    /**
     * Sets the orientation of the icosahedron.
     *
     * One corner of the icosahedron is, by default, facing to the north pole, and one to the south pole. The provided
     * orientation is relative to the default orientation.
     *
     * The orientation shifts every location by the angle <code>orientationLon</code> in direction of positive
     * longitude, and thereafter by the angle <code>orientationLat</code> in direction of positive latitude.
     *
     * @param orientationLat
     * @param orientationLon
     */
    public void setOrientation(double orientationLat, double orientationLon) {
        this._orientationLat = orientationLat;
        this._orientationLon = orientationLon;
    }

    /**
     * Sets the orientation of the icosahedron such that the north and the south poles are mapped to the edge midpoints
     * of the icosahedron. The equator is thus mapped symmetrically.
     */
    public void setOrientationSymmetricEquator() {
        this.setOrientation((this.__E + this.__F) / 2., -11.25);
    }

    /**
     * Only for internal use!
     * Changes the orientation of geocoordinates, that is, rotates the coordinate system
     *
     * @param c
     * @return
     * @throws Exception
     */
    public GeoCoordinates _changeOrientation(GeoCoordinates c) throws Exception {
        if (this._orientationLat == 0 && this._orientationLon == 0) return c;
        double sinOrientationLat = Trigonometric.sin(this._orientationLat);
        double cosOrientationLat = Trigonometric.cos(this._orientationLat);
        double sinLat1 = Trigonometric.sin(c.getLat());
        double cosLat1 = Trigonometric.cos(c.getLat());
        double lon1 = c.getLon() + this._orientationLon;
        double sinLon1 = Trigonometric.sin(lon1);
        double cosLon1 = Trigonometric.cos(lon1);
        double lat2 = Trigonometric.asin(sinLat1 * cosOrientationLat + cosLon1 * cosLat1 * sinOrientationLat);
        double lon2 = Trigonometric.atan2(sinLon1 * cosLat1, cosLon1 * cosLat1 * cosOrientationLat - sinLat1 * sinOrientationLat);
        return new GeoCoordinates(lat2, lon2);
    }

    /**
     * Only for internal use!
     * Inverse of _changeOrientation
     *
     * @param c
     * @return
     * @throws Exception
     */
    public GeoCoordinates _revertOrientation(GeoCoordinates c) throws Exception {
        if (this._orientationLat == 0 && this._orientationLon == 0) return (c.getLat() < -90 + ISEAProjection._precisionPerDefinition || c.getLat() > 90 - ISEAProjection._precisionPerDefinition) ? new GeoCoordinates(c.getLat(), 0.) : c;
        double lon = c.getLon();
        if (c.getLat() < -90 + ISEAProjection._precisionPerDefinition || c.getLat() > 90 - ISEAProjection._precisionPerDefinition) lon = 0;
        if (this._orientationLat == 0 && this._orientationLon == 0) return c;
        double sinOrientationLat = Trigonometric.sin(-this._orientationLat);
        double cosOrientationLat = Trigonometric.cos(this._orientationLat);
        double sinLat1 = Trigonometric.sin(c.getLat());
        double cosLat1 = Trigonometric.cos(c.getLat());
        double sinLon1 = Trigonometric.sin(lon);
        double cosLon1 = Trigonometric.cos(lon);
        double lat2 = Trigonometric.asin(sinLat1 * cosOrientationLat + cosLon1 * cosLat1 * sinOrientationLat);
        double lon2 = Trigonometric.atan2(sinLon1 * cosLat1, cosLon1 * cosLat1 * cosOrientationLat - sinLat1 * sinOrientationLat);
        return new GeoCoordinates(lat2, lon2 - this._orientationLon);
    }

    /**
     * The projection distorts angles. This method returns the maximum angular distortion.
     *
     * @return maximum angular distortion
     */
    public double maximumAngularDistortion() {
        return this._omega;
    }

    /**
     * The projection distorts scale. This method returns the maximum scale variation.
     *
     * @return maximum scale variation
     */
    public double maximumScaleVariation() {
        return this._a;
    }

    /**
     * The projection distorts scale. This method returns the minimum scale variation.
     *
     * @return minimum scale variation
     */
    public double miniumScaleVariation() {
        return this._b;
    }

    /**
     * Returns the length of the bases of the triangles of the icosahedron.
     *
     * @return length of the bases of the triangles
     */
    public double lengthOfTriangleBase() {
        return 2 * this.__G;
    }

    /**
     * Converts geographic coordinates to coordinates on the icosahedron.
     *
     * @param c geographic coordinates
     * @return coordinates on the icosahedron
     * @throws Exception
     */
    public FaceCoordinates sphereToIcosahedron(GeoCoordinates c) throws Exception {
        return this._sphereToIcosahedron(this._changeOrientation(c));
    }

    /**
     * Converts geographic coordinates to coordinates on the icosahedron.
     *
     * @param c coordinates on the icosahedron
     * @return geographic coordinates
     * @throws Exception
     */
    public GeoCoordinates icosahedronToSphere(FaceCoordinates c) throws Exception {
        return this._revertOrientation(this._icosahedronToSphere(c));
    }

    private FaceCoordinates _sphereToIcosahedron(GeoCoordinates c) {
        double sinLat = Trigonometric.sin(c.getLat());
        double cosLat = Trigonometric.cos(c.getLat());
        for (int face : this._guessFace(c)) {
            double lat0 = this.getLat(face);
            double lon0 = this.getLon(face);
            double sinLat0 = Trigonometric.sin(lat0);
            double cosLat0 = Trigonometric.cos(lat0);
            double sinLonLon0 = Trigonometric.sin(c.getLon() - lon0 );
            double cosLonLon0 = Trigonometric.cos(c.getLon() - lon0 );
            double Az_earth = Trigonometric.atan2(cosLat * sinLonLon0, cosLat0 * sinLat - sinLat0 * cosLat * cosLonLon0); // Az
            double AzAdjustment = (this.faceOrientation(face) > 0) ? 0 : 180;
            Az_earth += AzAdjustment;
            while (Az_earth < 0) {
                AzAdjustment += this._AzMax;
                Az_earth += this._AzMax;
            }
            while (Az_earth > this._AzMax) {
                AzAdjustment -= this._AzMax;
                Az_earth -= this._AzMax;
            }
            double sinAz_earth = Trigonometric.sin(Az_earth); // \sin Az
            double cosAz_earth = Trigonometric.cos(Az_earth); // \cos Az
            double z = Trigonometric.acos(sinLat0 * sinLat + cosLat0 * cosLat * cosLonLon0); // z
            double q = Trigonometric.atan2(this._tan_g,  cosAz_earth + sinAz_earth * this._cotTheta); // q
            if (z > q + ISEAProjection._precision) continue;
            double H = this._compute_H(sinAz_earth, cosAz_earth); // H
            double area = (Az_earth + this._G_180 + H) * this._pi_R_earth2_180; // A_G and A_{ABD}
            double Az = Trigonometric.atan2(2 * area, this._R_tan_g_2 - area * this._2cotTheta); // Az'
            double sinAz = Trigonometric.sin(Az); // \sin Az'
            double cosAz = Trigonometric.cos(Az); // \cos Az'
            double f = this._compute_f(sinAz, cosAz, sinAz_earth, cosAz_earth); // f
            double rho = this._2R * f * Trigonometric.sin(z / 2.); // \rho
            Az -= AzAdjustment;
            double x = rho * Trigonometric.sin(Az); // x
            double y = rho * Trigonometric.cos(Az); // y
            return new FaceCoordinates(face, x, y);
        }
        return null;
    }

    private List<Integer> _guessFace(GeoCoordinates c) {
        List<Integer> result = new ArrayList<>();
        if (c.getLat() > this.__EF) {
            if (c.getLon() < -108) {
                result.add(0);
                result.add(5);
            } else if (c.getLon() < -36) {
                result.add(1);
                result.add(6);
            } else if (c.getLon() < 36) {
                result.add(2);
                result.add(7);
            } else if (c.getLon() < 108) {
                result.add(3);
                result.add(8);
            } else {
                result.add(4);
                result.add(9);
            }
        } else if (c.getLat() < -this.__EF) {
            if (c.getLon() < -144) {
                result.add(19);
                result.add(14);
            } else if (c.getLon() < -72) {
                result.add(15);
                result.add(10);
            } else if (c.getLon() < 0) {
                result.add(16);
                result.add(11);
            } else if (c.getLon() < 72) {
                result.add(17);
                result.add(12);
            } else if (c.getLon() < 144) {
                result.add(18);
                result.add(13);
            } else {
                result.add(19);
                result.add(14);
            }
        } else {
            if (c.getLon() < -144) {
                result.add(5);
                result.add(14);
            } else if (c.getLon() < -108) {
                result.add(5);
                result.add(10);
            } else if (c.getLon() < -72) {
                result.add(6);
                result.add(10);
            } else if (c.getLon() < -36) {
                result.add(6);
                result.add(11);
            } else if (c.getLon() < 0) {
                result.add(7);
                result.add(11);
            } else if (c.getLon() < 36) {
                result.add(7);
                result.add(12);
            } else if (c.getLon() < 72) {
                result.add(8);
                result.add(12);
            } else if (c.getLon() < 108) {
                result.add(8);
                result.add(13);
            } else if (c.getLon() < 144) {
                result.add(9);
                result.add(13);
            } else {
                result.add(9);
                result.add(14);
            }
        }
        for (int f = 0; f < this._numberOfFaces; f++) result.add(f);
        return result;
    }

    private GeoCoordinates _icosahedronToSphere(FaceCoordinates c) throws Exception {
        double Az = Trigonometric.atan2(c.getX(), c.getY()); // Az'
        double rho = Math.sqrt(Math.pow(c.getX(), 2) + Math.pow(c.getY(), 2)); // \rho
        double AzAdjustment = (this.faceOrientation(c) > 0) ? 0 : 180;
        Az += AzAdjustment;
        while (Az < 0) {
            AzAdjustment += this._AzMax;
            Az += this._AzMax;
        }
        while (Az > this._AzMax) {
            AzAdjustment -= this._AzMax;
            Az -= this._AzMax;
        }
        double sinAz = Trigonometric.sin(Az); // \sin Az'
        double cosAz = Trigonometric.cos(Az); // \cos Az'
        double cotAz = cosAz / sinAz; // \cot Az'
        double area = this._R_tan_g_2 / (2 * (cotAz + this._cotTheta)); // A_G or A_{ABD}
        double deltaAz = 10 * ISEAProjection._precision;
        double area_pi_R_earth2_180_G_180 = area / this._pi_R_earth2_180 - this._G_180;
        double Az_earth = Az;
        while (Math.abs(deltaAz) > ISEAProjection._precision) {
            double H = this._compute_H(Trigonometric.sin(Az_earth), Trigonometric.cos(Az_earth)); // H
            double FAz_earth = area_pi_R_earth2_180_G_180 - H - Az_earth; // F(Az) or g(Az)
            double F2Az_earth = (Trigonometric.cos(Az_earth) * this._sinG_cos_g + Trigonometric.sin(Az_earth) * this._cosG) / Trigonometric.sin(H) - 1; // F'(Az) or g'(Az)
            deltaAz = - FAz_earth / F2Az_earth; // \Delta Az^0 or \Delta Az
            Az_earth += deltaAz;
        }
        double sinAz_earth = Trigonometric.sin(Az_earth); // \sin Az
        double cosAz_earth = Trigonometric.cos(Az_earth); // \cos Az
        double f = this._compute_f(sinAz, cosAz, sinAz_earth, cosAz_earth); // f
        double z = 2 * Trigonometric.asin(rho / (this._2R * f)); // z
        Az_earth -= AzAdjustment;
        double sinLat0 = Trigonometric.sin(this._getLat(c)); // \sin \phi_0
        double cosLat0 = Trigonometric.cos(this._getLat(c)); // \cos \phi_0
        double sinZ = Trigonometric.sin(z); // \sin z
        double cosZ = Trigonometric.cos(z); // \cos z
        double lat = Trigonometric.asin(sinLat0 * cosZ + cosLat0 * sinZ * Trigonometric.cos(Az_earth)); // \phi
        double lon = this._getLon(c) + Trigonometric.atan2(Trigonometric.sin(Az_earth) * sinZ * cosLat0, cosZ - sinLat0 * Trigonometric.sin(lat)); // \lambda
        return new GeoCoordinates(lat, lon);
    }

    /**
     * Returns orientation of a face.
     *
     * @param fc
     * @return 1 for upright, and -1 for upside down
     */
    public int faceOrientation(FaceCoordinates fc) {
        return this.faceOrientation(fc.getFace());
    }

    /**
     * Returns orientation of a face.
     *
     * @param face
     * @return 1 for upright, and -1 for upside down
     */
    public int faceOrientation(int face) {
        return (face <= 4 || (10 <= face && face <= 14)) ? 1 : -1;
    }

    private double _compute_H(double sinAz_earth, double cosAz_earth) {
        return Trigonometric.acos(sinAz_earth * this._sinG_cos_g - cosAz_earth * this._cosG); // H
    }
    private double _compute_f(double sinAz, double cosAz, double sinAz_earth, double cosAz_earth) {
        return this._compute_d(sinAz, cosAz) / (2 * this._R * Trigonometric.sin(this._compute_q(sinAz_earth, cosAz_earth) / 2)); // f
    }
    private double _compute_d(double sinAz, double cosAz) {
        return this._R_tan_g / (cosAz + sinAz * this._cotTheta); // d'
    }
    private double _compute_q(double sinAz_earth, double cosAz_earth) {
        return Trigonometric.atan2(this._tan_g, (cosAz_earth + sinAz_earth * this._cotTheta)); // q
    }

    private double _getLat(FaceCoordinates c) {
        return this.getLat(c.getFace());
    }
    private double _getLon(FaceCoordinates c) {
        return this.getLon(c.getFace());
    }

    /**
     * Returns latitude for face
     *
     * @param face
     * @return latitude
     */
    public double getLat(int face) {
        return this.__lats[face];
    }

    /**
     * Returns longitude for face
     *
     * @param face
     * @return longitude
     */
    public double getLon(int face) {
        return this.__lons[face];
    }
}
