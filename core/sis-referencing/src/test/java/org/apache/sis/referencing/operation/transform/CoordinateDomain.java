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
import org.apache.sis.test.mock.GeodeticDatumMock;

import static java.lang.StrictMath.*;


/**
 * The domain of input coordinates.
 * This class can generate random number suitable for their domain.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5 (derived from geotk-3.00)
 * @version 0.5
 * @module
 */
public strictfp enum CoordinateDomain {
    /**
     * Geocentric input coordinates. The input dimension must be 3.
     */
    GEOCENTRIC {
        @Override double[] generateRandomInput(final Random random, final int dimension, final int numPts) {
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
    },

    /**
     * Geographic input coordinates with angles in decimal degrees.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    GEOGRAPHIC {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range = Longitude.MAX_VALUE; break; // Full longitude range.
                case 1:  range = Latitude.MAX_VALUE;  break; // Full latitude range.
                case 2:  range = 10000;               break; // Ellipsoidal height.
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Geographic input coordinates avoiding poles and anti-meridian.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_SAFE {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range = Longitude.MAX_VALUE - 1; break; // Longitude, avoiding anti-meridian.
                case 1:  range = Latitude.MAX_VALUE - 20; break; // Latitude, avoiding pole.
                case 2:  range = 5000;                    break; // Ellipsoidal height.
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Geographic input coordinates close to the poles.
     * Ordinates are in (<var>longitude</var>, <var>latitude</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_POLES {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range = Longitude.MAX_VALUE; break;
                case 1:  range =   20; break;
                case 2:  range = 5000; break;
                default: return super.generate(random, dimension);
            }
            double value = random.nextDouble() * (2*range) - range;
            if (dimension == 1) {
                if (value <= 0) {
                    value += Latitude.MAX_VALUE;
                } else {
                    value += Latitude.MIN_VALUE;
                }
            }
            return value;
        }
    },

    /**
     * Geographic input coordinates with angles in radians.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_RADIANS {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range = PI;    break; // Longitude.
                case 1:  range = PI/2;  break; // Latitude.
                case 2:  range = 10000; break; // Ellipsoidal height.
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Geographic input coordinates with angles in radians and only half of the longitude range.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_RADIANS_HALF {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range = PI/2;  break; // Longitude.
                case 1:  range = PI/2;  break; // Latitude.
                case 2:  range = 10000; break; // Ellipsoidal height.
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Geographic input coordinates with angles in radians in the North hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_RADIANS_NORTH {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range = PI; break;
                case 1:  return +PI/2*random.nextDouble();
                case 2:  range = 10000; break;
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Geographic input coordinates with angles in radians in the South hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_RADIANS_SOUTH {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range = PI; break;
                case 1:  return -PI/2*random.nextDouble();
                case 2:  range = 10000; break;
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Geographic input coordinates with angles in radians in the East hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_RADIANS_EAST {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  return +PI*random.nextDouble();
                case 1:  range = PI/2;  break;
                case 2:  range = 10000; break;
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Geographic input coordinates with angles in radians in the West hemisphere only.
     * Ordinates are in (<var>lambda</var>, <var>phi</var>, <var>height</var>) order.
     */
    GEOGRAPHIC_RADIANS_WEST {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  return -PI*random.nextDouble();
                case 1:  range = PI/2;  break;
                case 2:  range = 10000; break;
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Projected input coordinates in a range suitable for UTM projections.
     * Ordinates are in (<var>easting</var>, <var>northing</var>, <var>height</var>) order.
     */
    PROJECTED {
        @Override double generate(final Random random, final int dimension) {
            final double range;
            switch (dimension) {
                case 0:  range =  350000; break; // Easting.
                case 1:  range = 8000000; break; // Northing.
                case 2:  range =   10000; break; // Ellipsoidal height.
                default: return super.generate(random, dimension);
            }
            return random.nextDouble() * (2*range) - range;
        }
    },

    /**
     * Gaussian numbers: can be positives or negatives, mostly close to zero but some
     * numbers can be arbitrarily large.
     */
    GAUSSIAN;

    /**
     * Generates random input coordinates.
     *
     * @param  random    The random number generator to use.
     * @param  dimension The number of dimension of the points to generate.
     * @param  numPts    The number of points to generate.
     * @return An array of length {@code numPts*dimension} filled with random input ordinate values.
     */
    double[] generateRandomInput(final Random random, final int dimension, final int numPts) {
        final double[] ordinates = new double[numPts * dimension];
        for (int i=0; i<ordinates.length; i++) {
            ordinates[i] = generate(random, i % dimension);
        }
        return ordinates;
    }

    /**
     * Generates a random number for the given dimension.
     *
     * @param  random    The random number generator to use.
     * @param  dimension The dimension for which to generate a random number.
     * @return A random number suitable for the given dimension.
     */
    double generate(final Random random, final int dimension) {
        return random.nextGaussian();
    }
}
