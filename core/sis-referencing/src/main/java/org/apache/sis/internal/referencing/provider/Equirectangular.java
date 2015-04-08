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
package org.apache.sis.internal.referencing.provider;

import java.awt.geom.AffineTransform;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.referencing.j2d.ParameterizedAffine;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.resources.Messages;

import static java.lang.Math.*;


/**
 * The provider for "<cite>Equidistant Cylindrical (Spherical)</cite>" projection
 * (EPSG:1029, <span class="deprecated">EPSG:9823</span>).
 *
 * At the difference of most other map projection providers, this class does not extend {@link MapProjection}
 * because it does not create non-liner kernel. Instead, the projection created by this class is implemented
 * by an affine transform.
 *
 * <p>This provider is <strong>not</strong> suitable for the <cite>Equidistant Cylindrical</cite> projection
 * (EPSG:1028, <span class="deprecated">EPSG:9842</span>). EPSG defines Equidistant Cylindrical projection as
 * the ellipsoidal case of this projection, which uses a more complicated formula than the affine transform
 * used here.</p>
 *
 * <div class="note"><b>Note:</b>
 * EPSG:1028 and 1029 are the current codes, while EPSG:9842 and 9823 are deprecated codes.
 * The new and deprecated definitions differ only by their parameters. In the Apache SIS implementation,
 * both current and legacy definitions are known, but the legacy names are marked as deprecated.</div>
 *
 * @author  John Grange
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/equirectangular.html">Equirectangular on RemoteSensing.org</a>
 */
public final class Equirectangular extends AbstractProvider {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -278288251842178001L;

    /*
     * ACCESS POLICY: Only formal EPSG parameters shall be public.
     * Parameters that we add ourselves should be package-privated.
     */

    /**
     * The operation parameter descriptor for the <cite>Latitude of 1st standard parallel</cite> (φ₁) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL;

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     *
     * <p>In theory, this parameter should not be used and its value should be 0 in all cases.
     * This parameter is included for completeness in CRS labelling only, and is declared optional.</p>
     */
    static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        STANDARD_PARALLEL = createLatitude(builder
                .addIdentifier("8823")
                .addName("Latitude of 1st standard parallel")
                .addName(Citations.OGC,     Constants.STANDARD_PARALLEL_1)
                .addName(Citations.ESRI,    "Standard_Parallel_1")
                .addName(Citations.NETCDF,  "standard_parallel")
                .addName(Citations.GEOTIFF, "ProjStdParallel1")
                .addName(Citations.PROJ4,   "lat_ts"), false);

        CENTRAL_MERIDIAN = createLongitude(builder
                .addIdentifier("8802")
                .addName("Longitude of natural origin")
                .addName(Citations.OGC,     Constants.CENTRAL_MERIDIAN)
                .addName(Citations.ESRI,    "Central_Meridian")
                .addName(Citations.NETCDF,  "longitude_of_projection_origin")
                .addName(Citations.GEOTIFF, "ProjCenterLong")
                .addName(Citations.PROJ4,   "lon_0"));

        FALSE_EASTING = createShift(builder
                .addIdentifier("8806")
                .addName("False easting")
                .addName(Citations.OGC,     Constants.FALSE_EASTING)
                .addName(Citations.ESRI,    "False_Easting")
                .addName(Citations.NETCDF,  "false_easting")
                .addName(Citations.GEOTIFF, "FalseEasting")
                .addName(Citations.PROJ4,   "x_0"));

        FALSE_NORTHING = createShift(builder
                .addIdentifier("8807")
                .addName("False northing")
                .addName(Citations.OGC,     Constants.FALSE_NORTHING)
                .addName(Citations.ESRI,    "False_Northing")
                .addName(Citations.NETCDF,  "false_northing")
                .addName(Citations.GEOTIFF, "FalseNorthing")
                .addName(Citations.PROJ4,   "y_0"));
        /*
         * "Latitude of natural origin" is not formally parameters of the "Equidistant Cylindrical (Spherical)"
         * projection according EPSG:1029.  But we declare it anyway (as an optional parameter) because it was
         * part of the now deprecated EPSG:9823 definition (and also EPSG:9842, the ellipsoidal case),  and we
         * still see it in use sometime. However, taking inspiration from the practice done in "Mercator (1SP)"
         * projection, we require that the parameter value must be zero.
         */
        LATITUDE_OF_ORIGIN = createConstant(builder     // Was used by EPSG:9823 (also EPSG:9842).
                .addIdentifier("8801")
                .addName("Latitude of natural origin")
                .addName(Citations.OGC,     "latitude_of_origin")
                .addName(Citations.ESRI,    "Latitude_Of_Origin")
                .addName(Citations.NETCDF,  "latitude_of_projection_origin")
                .addName(Citations.GEOTIFF, "ProjCenterLat")
                .addName(Citations.PROJ4,   "lat_0")
                .setRemarks(Messages.formatInternational(Messages.Keys.ConstantProjParameterValue_1, 0))
                .setRequired(false), 0.0);

        // Do not declare the ESRI "Equidistant_Cylindrical" projection name below,
        // for avoiding confusion with EPSG "Equidistant Cylindrical" ellipsoidal projection.
        PARAMETERS = builder
            .addIdentifier(             "1029")
            .addDeprecatedIdentifier(   "9823", "1029")  // Using deprecated parameter names
            .addName(                   "Equidistant Cylindrical (Spherical)")
            .addName(                   "Plate Carrée")  // Not formally defined by EPSG, but cited in documentation.
            .addName(Citations.OGC,     "Equirectangular")
            .addName(Citations.GEOTIFF, "CT_Equirectangular")
            .addName(Citations.PROJ4,   "eqc")
            .addIdentifier(Citations.GEOTIFF, "17")
            .createGroupForMapProjection(
                    STANDARD_PARALLEL,
                    LATITUDE_OF_ORIGIN,     // Not formally an Equirectangular parameter.
                    CENTRAL_MERIDIAN,
                    FALSE_EASTING,
                    FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public Equirectangular() {
        super(2, 2, PARAMETERS);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code CylindricalProjection.class}
     */
    @Override
    public Class<CylindricalProjection> getOperationType() {
        return CylindricalProjection.class;
    }

    /**
     * Creates an Equirectangular projection from the specified group of parameter values. This method is an
     * adaptation of {@link org.apache.sis.referencing.operation.projection.NormalizedProjection} constructor,
     * reproduced in this method because we will create an affine transform instead than the usual projection
     * classes.
     *
     * @param  factory The factory to use if this constructor needs to create other math transforms.
     * @param  parameters The parameter values that define the transform to create.
     * @return The map projection created from the given parameter values.
     * @throws FactoryException if an error occurred while creating the math transform.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup parameters)
            throws FactoryException
    {
        final Parameters p = Parameters.castOrWrap(parameters);
        final ContextualParameters context = new ContextualParameters(this);
        double a  = MapProjection.getAndStore(p, context, MapProjection.SEMI_MAJOR);
        double b  = MapProjection.getAndStore(p, context, MapProjection.SEMI_MINOR);
        double λ0 = MapProjection.getAndStore(p, context, CENTRAL_MERIDIAN);
        double φ0 = MapProjection.getAndStore(p, context, LATITUDE_OF_ORIGIN);
        double φ1 = MapProjection.getAndStore(p, context, STANDARD_PARALLEL);
        double fe = MapProjection.getAndStore(p, context, FALSE_EASTING);
        double fn = MapProjection.getAndStore(p, context, FALSE_NORTHING);
        /*
         * Perform following transformation, in that order. Note that following
         * AffineTransform convention, the Java code appears in reverse order:
         *
         *   1) Subtract φ0 to the latitude.
         *   2) Subtract λ0 to the longitude.
         *   3) Convert degrees to radians.
         *   4) Scale longitude by cos(φ1).
         */
        φ1 = toRadians(φ1);
        context.getMatrix(true).concatenate(0, cos(φ1), null);
        context.normalizeGeographicInputs(λ0)
               .concatenate(1, null, -φ0);
        /*
         * At this point, we usually invoke scaleAndTranslate2D(false, a, fe, fn) where 'a' (the semi-major
         * axis length) is taken as the Earth radius (R). However quoting EPSG: "If the figure of the earth
         * used is an ellipsoid rather than a sphere then R should be calculated as the radius of the conformal
         * sphere at the projection origin at latitude φ1 using the formula for RC given in section 1.2, table 3".
         */
        if (a != b) {
            final double rs = b / a;
            final double sinφ1 = sin(φ1);
            a = b / (1 - (1 - rs*rs) * (sinφ1*sinφ1));
        }
        context.scaleAndTranslate2D(false, a, fe, fn);
        /*
         * Creates the ConcatenatedTransform, letting the factory returns the cached instance
         * if the caller already invoked this method previously (which usually do not happen).
         */
        MathTransform mt = context.completeTransform(factory, MathTransforms.identity(2));
        if (mt instanceof AffineTransform) {  // Always true in Apache SIS implementation.
            mt = new ParameterizedAffine((AffineTransform) mt, context, true);
        }
        return mt;
    }
}
