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
package org.apache.sis.referencing.operation.transform;

import java.util.Random;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;

import static java.lang.StrictMath.*;
import static org.apache.sis.internal.metadata.ReferencingServices.AUTHALIC_RADIUS;

// Test imports
import org.apache.sis.referencing.datum.GeodeticDatumMock;


/**
 * The domain of input coordinates.
 * This class can generate random number suitable for their domain.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.6
 * @module
 */
public strictfp class CoordinateDomain {
    /**
     * Latitude of the Article circle, which is 66°33′45.7″ as of March 30, 2015
     * Note that this value fluctuate by 2° over 40,000-year periods.
     */
    private static final double ARTICLE_CIRCLE = 66 + (33 + 45.7 / 60) / 60;

    /**
     * Maximum altitude to be used in this class, in metres.
     */
    private static final double HEIGHT = 10000;

    /**
     * Geocentric input coordinates. The input dimension must be 3.
     */
    public static final CoordinateDomain GEOCENTRIC = new CoordinateDomain(
            -(AUTHALIC_RADIUS + HEIGHT), (AUTHALIC_RADIUS + HEIGHT),
            -(AUTHALIC_RADIUS + HEIGHT), (AUTHALIC_RADIUS + HEIGHT),
            -(AUTHALIC_RADIUS + HEIGHT), (AUTHALIC_RADIUS + HEIGHT))
    {
        @Override
        public double[] generateRandomInput(final Random random, final int dimension, final int numPts) {
            if (dimension != 3) {
                throw new IllegalArgumentException();
            }
            final double axis = GeodeticDatumMock.SPHERE.getEllipsoid().getSemiMajorAxis();
            final double[] ordinates = GEOGRAPHIC.generateRandomInput(random, dimension, numPts);
            for (int i=0; i<ordinates.length;) {
                final double phi    = toRadians(ordinates[i  ]);
                final double theta  = toRadians(ordinates[i+1]);
                final double radius = axis  +   ordinates[i+2];
                final double radXY  = radius * cos(theta);
                ordinates[i++] = radXY  * cos(phi);
                ordinates[i++] = radXY  * sin(phi);
                ordinates[i++] = radius * sin(theta);
            }
            return ordinates;
        }
    };

    /**
     * Geographic input coordinates with angles in decimal degrees.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC = new CoordinateDomain(
            Longitude.MIN_VALUE, Longitude.MAX_VALUE,
            Latitude .MIN_VALUE, Latitude .MAX_VALUE,
                        -HEIGHT,             HEIGHT);

    /**
     * Geographic input coordinates avoiding poles and anti-meridian.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_SAFE = new CoordinateDomain(
            Longitude.MIN_VALUE + 1, Longitude.MAX_VALUE -  1,
                -ARTICLE_CIRCLE,         +ARTICLE_CIRCLE,
                        -HEIGHT,                 HEIGHT);

    /**
     * Geographic input coordinates close to the poles.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_POLES = new CoordinateDomain(
            Longitude.MIN_VALUE, Longitude.MAX_VALUE,
            Latitude .MIN_VALUE, Latitude .MAX_VALUE,
                        -HEIGHT,              HEIGHT)
    {
        @Override
        public double[] generateRandomInput(final Random random, final int dimension, final int numPts) {
            final double[] ordinates = super.generateRandomInput(random, dimension, numPts);
            for (int i=1; i < ordinates.length; i += dimension) {
                final double φ = ordinates[i];
                double a = abs(φ);
                if (a < ARTICLE_CIRCLE) {
                    a -= floor(a / (Latitude.MAX_VALUE - ARTICLE_CIRCLE)) * (Latitude.MAX_VALUE - ARTICLE_CIRCLE);
                    a = (Latitude.MAX_VALUE - a);
                    ordinates[i] = copySign(a, φ);
                }
            }
            return ordinates;
        }
    };

    /**
     * Geographic input coordinates close to the north pole.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_NORTH_POLE = new CoordinateDomain(
            Longitude.MIN_VALUE, Longitude.MAX_VALUE,
                 ARTICLE_CIRCLE, Latitude .MAX_VALUE,
                        -HEIGHT,              HEIGHT);

    /**
     * Geographic input coordinates close to the south pole.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_SOUTH_POLE = new CoordinateDomain(
            Longitude.MIN_VALUE, Longitude.MAX_VALUE,
            Latitude .MIN_VALUE,     -ARTICLE_CIRCLE,
                        -HEIGHT,              HEIGHT);

    /**
     * Geographic input coordinates with angles in radians.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_RADIANS = new CoordinateDomain(
            -PI,     PI,
            -PI/2,   PI/2,
            -HEIGHT, HEIGHT);

    /**
     * Geographic input coordinates with angles in radians and only half of the longitude range.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_RADIANS_HALF_λ = new CoordinateDomain(
            -PI/2,   PI/2,
            -PI/2,   PI/2,
            -HEIGHT, HEIGHT);

    /**
     * Geographic input coordinates with angles in radians in the North hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_RADIANS_NORTH = new CoordinateDomain(
            -PI,     PI,
             0,      PI/2,
            -HEIGHT, HEIGHT);

    /**
     * Geographic input coordinates with angles in radians in the South hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_RADIANS_SOUTH = new CoordinateDomain(
            -PI,     PI,
            -PI/2,   0,
            -HEIGHT, HEIGHT);

    /**
     * Geographic input coordinates with angles in radians in the East hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_RADIANS_EAST = new CoordinateDomain(
             0,      PI,
            -PI/2,   PI/2,
            -HEIGHT, HEIGHT);

    /**
     * Geographic input coordinates with angles in radians in the West hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain GEOGRAPHIC_RADIANS_WEST = new CoordinateDomain(
            -PI,     0,
            -PI/2,   PI/2,
            -HEIGHT, HEIGHT);

    /**
     * Projected input coordinates in a range suitable for UTM projections.
     * Ordinates are in (<var>easting</var>, <var>northing</var>, <var>height</var>) order.
     */
    public static final CoordinateDomain PROJECTED = new CoordinateDomain(
             -350000,    350000,    // Easting
            -8000000,   8000000,    // Northing
             -HEIGHT,   HEIGHT);   // Ellipsoidal height

    /**
     * Values in the -10 to 10 range in all dimensions.
     */
    public static final CoordinateDomain RANGE_10 = new CoordinateDomain(
            -10, 10,
            -10, 10,
            -10, 10);

    /**
     * The domain of the coordinates to test.
     */
    final double xmin, xmax, ymin, ymax, zmin, zmax;

    /**
     * Creates a new domain with the given bounding box.
     */
    private CoordinateDomain(final double xmin, final double xmax,
                             final double ymin, final double ymax,
                             final double zmin, final double zmax)
    {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.zmin = zmin;
        this.zmax = zmax;
    }

    /**
     * Generates random input coordinates.
     *
     * @param  random    The random number generator to use.
     * @param  dimension The number of dimension of the points to generate.
     * @param  numPts    The number of points to generate.
     * @return An array of length {@code numPts*dimension} filled with random input ordinate values.
     */
    public double[] generateRandomInput(final Random random, final int dimension, final int numPts) {
        final double[] ordinates = new double[numPts * dimension];
        for (int j=0; j<dimension; j++) {
            final double min, max;
            switch (j) {
                case 0:  min = xmin; max = xmax; break;
                case 1:  min = ymin; max = ymax; break;
                case 2:  min = zmin; max = zmax; break;
                default: {
                    for (int i=j; i<ordinates.length; i += dimension) {
                        ordinates[i] = random.nextGaussian();
                    }
                    continue;
                }
            }
            final double range = max - min;
            for (int i=j; i<ordinates.length; i += dimension) {
                ordinates[i] = min + range * random.nextDouble();
            }
        }
        return ordinates;
    }
}
