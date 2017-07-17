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
package org.apache.sis.storage.gdal;

import javax.measure.Unit;
import javax.measure.quantity.Angle;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.OS;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.apache.sis.measure.Units;


/**
 * Bindings to the {@literal Proj.4} library as static convenience methods.
 * The methods in this class allow to:
 *
 * <ul>
 *   <li>{@linkplain #createCRS Create a Coordinate Reference System instance from a Proj.4 definition string}.</li>
 *   <li>Conversely, {@link #definition get a Proj.4 definition string from a Coordinate Reference System}.</li>
 *   <li>{@linkplain #createOperation Create a coordinate operation backed by Proj.4 between two arbitrary CRS}.</li>
 * </ul>
 *
 * Most methods in this class delegate to {@link Proj4Factory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class Proj4 extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private Proj4() {
    }

    /**
     * Returns the version number of the {@literal Proj.4} library.
     * Returns {@code null} if Proj.4 is not installed on the current system.
     *
     * <div class="note"><b>Example:</b> Rel. 4.9.3, 15 August 2016</div>
     *
     * @return the Proj.4 release string, or {@code null} if no installation has been found.
     */
    public static String version() {
        try {
            return PJ.getRelease();
        } catch (UnsatisfiedLinkError e) {
            // Thrown the first time that we try to use the library.
            Logging.unexpectedException(Logging.getLogger(Modules.GDAL), Proj4.class, "version", e);
        } catch (NoClassDefFoundError e) {
            // Thrown on all attempts after the first one.
            Logging.recoverableException(Logging.getLogger(Modules.GDAL), Proj4.class, "version", e);
        }
        return null;
    }

    /**
     * Infers a {@literal Proj.4} definition from the given projected, geographic or geocentric coordinate reference system.
     * This method does not need the Proj.4 native library; it can be used in a pure Java application.
     *
     * @param  crs  the coordinate reference system for which to create a Proj.4 definition.
     * @return the definition of the given CRS in a Proj.4 format.
     * @throws FactoryException if the Proj.4 definition string can not be created from the given CRS.
     */
    public static String definition(final CoordinateReferenceSystem crs) throws FactoryException {
        ArgumentChecks.ensureNonNull("crs", crs);
        /*
         * If the given CRS object is associated to a Proj.4 structure, let Proj.4 formats itself
         * the definition string. Note that this operation may fail if there is no Proj.4 library
         * in the current system, or no JNI bindings to that library.
         */
        try {
            for (final Identifier id : crs.getIdentifiers()) {
                if (id instanceof PJ) {
                    return ((PJ) id).getCode();
                }
            }
        } catch (UnsatisfiedLinkError e) {
            // Thrown the first time that we try to use the library.
            Logging.unexpectedException(Logging.getLogger(Modules.GDAL), Proj4.class, "definition", e);
        } catch (NoClassDefFoundError e) {
            // Thrown on all attempts after the first one.
            Logging.recoverableException(Logging.getLogger(Modules.GDAL), Proj4.class, "definition", e);
        }
        /*
         * If we found no Proj.4 structure, formats the definition string ourself. The string may differ from
         * what Proj.4 would have given. In particular, we do not provide "+init=" or "+datum=" parameter.
         * But the definition should still be semantically equivalent.
         */
        final String method;
        final GeodeticDatum datum;
        final ParameterValueGroup parameters;
        final CoordinateSystem cs = crs.getCoordinateSystem();
        if (crs instanceof GeodeticCRS) {
            if (cs instanceof EllipsoidalCS) {
                method = "latlon";
            } else if (cs instanceof CartesianCS) {
                method = "geocent";
            } else {
                throw new FactoryException(Errors.format(Errors.Keys.UnsupportedCoordinateSystem_1, cs.getClass()));
            }
            datum      = ((GeodeticCRS) crs).getDatum();
            parameters = null;
        } else if (crs instanceof ProjectedCRS) {
            Projection c = ((ProjectedCRS) crs).getConversionFromBase();
            datum        = ((ProjectedCRS) crs).getDatum();
            method       = name(c.getMethod());
            parameters   = c.getParameterValues();
        } else {
            throw new FactoryException(Errors.format(Errors.Keys.UnsupportedType_1, crs.getClass()));
        }
        /*
         * Append the map projection parameters. Those parameters may include axis lengths (a and b),
         * but not necessarily. If axis lengths are specified, then we will ignore the Ellipsoid instance
         * associated to the CRS.
         */
        final StringBuilder definition = new StringBuilder(100);
        definition.append("+proj=").append(method);
        boolean hasSemiMajor = false;
        boolean hasSemiMinor = false;
        if (parameters != null) {
            for (final GeneralParameterValue parameter : parameters.values()) {
                if (parameter instanceof ParameterValue) {
                    final Object value = ((ParameterValue) parameter).getValue();
                    if (value != null) {
                        final String pn = name(parameter.getDescriptor());
                        if (pn.equals("+a")) hasSemiMajor = true;
                        if (pn.equals("+b")) hasSemiMinor = true;
                        definition.append(' ').append(pn).append('=').append(value);
                    }
                }
            }
        }
        /*
         * Append datum information: axis lengths if they were not part of the parameters, then prime meridian.
         */
        final Ellipsoid ellipsoid = datum.getEllipsoid();
        if (!hasSemiMajor) definition.append(" +a=").append(ellipsoid.getSemiMajorAxis());
        if (!hasSemiMinor) definition.append(" +b=").append(ellipsoid.getSemiMinorAxis());
        final PrimeMeridian pm = datum.getPrimeMeridian();
        if (pm != null) {
            double lon = pm.getGreenwichLongitude();
            final Unit<Angle> unit = pm.getAngularUnit();
            if (unit != null) {
                lon = unit.getConverterTo(Units.DEGREE).convert(lon);
            }
            definition.append(" +pm=").append(lon);
        }
        /*
         * Appends axis directions. This method always format a vertical direction (up or down)
         * even if the coordinate system is two-dimensional, because Proj.4 seems to require it.
         */
        definition.append(' ').append(Proj4Factory.AXIS_ORDER_PARAM);
        final int dimension = Math.min(cs.getDimension(), 3);
        boolean hasVertical = false;
        for (int i=0; i<dimension; i++) {
            final AxisDirection dir = cs.getAxis(i).getDirection();
            if (!AxisDirections.isCardinal(dir)) {
                if (!AxisDirections.isVertical(dir)) {
                    throw new FactoryException(Errors.format(Errors.Keys.UnsupportedAxisDirection_1, dir));
                }
                hasVertical = true;
            }
            definition.appendCodePoint(Character.toLowerCase(dir.name().codePointAt(0)));
        }
        if (!hasVertical && dimension < 3) {
            definition.append('u');                    // Add a UP direction if not already present.
        }
        return definition.toString();
    }

    /**
     * Returns the {@literal Proj.4} name for the given parameter,
     * or throws an exception if the {@literal Proj.4} name is unknown.
     */
    private static String name(final IdentifiedObject object) throws FactoryException {
        final String name = IdentifiedObjects.getName(object, Citations.PROJ4);
        if (name == null) {
            throw new FactoryException(Errors.format(Errors.Keys.CanNotSetParameterValue_1, object.getName()));
        }
        return name;
    }

    /**
     * Creates a new CRS from the given {@literal Proj.4} definition string.
     *
     * @param  definition  the Proj.4 definition string.
     * @param  dimension   the number of dimension of the CRS to create (2 or 3).
     * @return a CRS created from the given definition string and number of dimensions.
     * @throws NullPointerException if the definition string is {@code null}.
     * @throws FactoryException if one of the given argument has an invalid value.
     *
     * @see Proj4Factory#createCoordinateReferenceSystem(String)
     */
    public static CoordinateReferenceSystem createCRS(String definition, final int dimension) throws FactoryException {
        definition = definition.trim();
        ArgumentChecks.ensureNonEmpty(definition, definition);
        ArgumentChecks.ensureBetween("dimension", 2, 3, dimension);
        try {
            return Proj4Factory.INSTANCE.createCRS(definition, dimension >= 3);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new UnavailableFactoryException(Errors.format(Errors.Keys.NativeInterfacesNotFound_2, OS.uname(), "libproj"), e);
        }
    }

    /**
     * Creates an operation for conversion or transformation between two coordinate reference systems.
     * This implementation always uses Proj.4 for performing the coordinate operations, regardless if
     * the given CRS were created from a Proj.4 definition string or not. This method fails if it can
     * not map the given CRS to Proj.4 structures.
     *
     * @param  sourceCRS   the source coordinate reference system.
     * @param  targetCRS   the target coordinate reference system.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS.
     * @throws FactoryException if an error occurred while creating the coordinate operation.
     *
     * @see Proj4Factory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)
     */
    public static CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                                      final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        try {
            return Proj4Factory.INSTANCE.createOperation(sourceCRS, targetCRS);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new UnavailableFactoryException(Errors.format(Errors.Keys.NativeInterfacesNotFound_2, OS.uname(), "libproj"), e);
        }
    }
}
