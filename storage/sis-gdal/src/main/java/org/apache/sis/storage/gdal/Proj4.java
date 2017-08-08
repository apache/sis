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
import javax.measure.format.ParserException;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.operation.AbstractCoordinateOperation;
import org.apache.sis.referencing.factory.UnavailableFactoryException;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.OS;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Static;
import org.apache.sis.util.iso.Types;
import org.apache.sis.measure.Units;


/**
 * Bindings to the {@literal Proj.4} library as static convenience methods.
 * The methods in this class allow to:
 *
 * <ul>
 *   <li>{@linkplain #createCRS Create a Coordinate Reference System instance from a Proj.4 definition string}.</li>
 *   <li>Conversely, {@linkplain #definition get a Proj.4 definition string from a Coordinate Reference System}.</li>
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
     * @return the Proj.4 release string, or {@code null} if the native library has been found.
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
     * However the returned definition string may differ depending on whether the Proj.4 library is available or not.
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
         *
         * The "+over" option is for disabling the default wrapping of output longitudes in the -180 to 180 range.
         * We do that for having the same behavior between Proj.4 and Apache SIS. No wrapping reduce discontinuity
         * problems with geometries that cross the anti-meridian.
         *
         * The "+no_defs" option is for ensuring that no defaults are read from "/usr/share/proj/proj_def.dat" file.
         * That file contains default values for various map projections, for example "+lat_1=29.5" and "+lat_2=45.5"
         * for the "aea" projection. Those defaults are assuming that users want Conterminous U.S. map.
         * This may cause surprising behavior for users outside USA.
         */
        final StringBuilder definition = new StringBuilder(100);
        definition.append(Proj4Factory.PROJ_PARAM).append(method);
        boolean hasSemiMajor = false;
        boolean hasSemiMinor = false;
        if (parameters != null) {
            definition.append(" +over +no_defs");                                       // See above comment
            for (final GeneralParameterValue parameter : parameters.values()) {
                if (parameter instanceof ParameterValue<?>) {
                    final ParameterValue<?> pv = (ParameterValue<?>) parameter;
                    final Object value;
                    Unit<?> unit = pv.getUnit();
                    if (unit != null) {
                        unit = Units.isAngular(unit) ? Units.DEGREE : unit.getSystemUnit();
                        value = pv.doubleValue(unit);       // Always in metres or degrees.
                    } else {
                        value = pv.getValue();
                        if (value == null) {
                            continue;
                        }
                    }
                    final String pn = name(parameter.getDescriptor());
                    hasSemiMajor |= pn.equals("+a");
                    hasSemiMinor |= pn.equals("+b");
                    definition.append(' ').append(pn).append('=').append(value);
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
         * Also extract axis units in the process.
         */
        final Unit<?>[] units = new Unit<?>[2];     // Horizontal at index 0, vertical at index 1.
        boolean validCS = true;
        definition.append(' ').append(Proj4Factory.AXIS_ORDER_PARAM);
        final int dimension = Math.min(cs.getDimension(), 3);
        boolean hasVertical = false;
        for (int i=0; i<dimension; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            final AxisDirection dir = axis.getDirection();
            int unitIndex = 0;
            if (!AxisDirections.isCardinal(dir)) {
                if (!AxisDirections.isVertical(dir)) {
                    throw new FactoryException(Errors.format(Errors.Keys.UnsupportedAxisDirection_1, dir));
                }
                hasVertical = true;
                unitIndex = 1;
            }
            final Unit<?> old  = units[unitIndex];
            units[unitIndex]   = axis.getUnit();
            validCS &= (old == null || old.equals(units[unitIndex]));
            definition.appendCodePoint(Character.toLowerCase(dir.name().codePointAt(0)));
        }
        if (!hasVertical && dimension < 3) {
            definition.append('u');                    // Add a UP direction if not already present.
        }
        /*
         * Append units of measurement, then verify the coordinate system validity.
         */
        for (int i=0; i<units.length; i++) {
            final Unit<?> unit = units[i];
            if (unit != null && !unit.equals(Units.DEGREE) && !unit.equals(Units.METRE)) {
                validCS &= Units.isLinear(unit);
                definition.append(" +");
                if (i == 1) definition.append('v');     // "+vto_meter" parameter.
                definition.append("to_meter=").append(Units.toStandardUnit(unit));
            }
        }
        /*
         * Append the "+towgs84" element if any. This is the last piece of information.
         * Note that the use of a "+towgs84" parameter is an "early binding" approach,
         * which is usually not recommended. But Proj4 works that way.
         */
        if (validCS) {
            if (datum instanceof DefaultGeodeticDatum) {
                for (final BursaWolfParameters bwp : ((DefaultGeodeticDatum) datum).getBursaWolfParameters()) {
                    if (Utilities.equalsIgnoreMetadata(CommonCRS.WGS84.datum(), bwp.getTargetDatum())) {
                        definition.append(" +towgs84=").append(bwp.tX).append(',').append(bwp.tY).append(',').append(bwp.tZ);
                        if (!bwp.isTranslation()) {
                            definition.append(',').append(bwp.rX).append(',').append(bwp.rY).append(',').append(bwp.rZ).append(',').append(bwp.dS);
                        }
                        break;
                    }
                }
            }
            return definition.toString();
        }
        /*
         * If we reach this point, we detected a coordinate system that we can not format as a
         * Proj.4 definition string. Format an error message with axis directions and units.
         */
        definition.setLength(0);
        definition.append('(');
        for (int i=0; i<units.length; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            if (i != 0) definition.append(", ");
            definition.append(axis.getUnit()).append(' ').append(Types.getCodeName(axis.getDirection()));
        }
        throw new FactoryException(Errors.format(Errors.Keys.IllegalCoordinateSystem_1, definition.append(')')));
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
     * Some examples of definition strings are:
     * <ul>
     *   <li>{@code "+init=epsg:3395"} (see warning below)</li>
     *   <li>{@code "+proj=latlong +datum=WGS84 +ellps=WGS84 +towgs84=0,0,0"}</li>
     *   <li>{@code "+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +ellps=WGS84 +towgs84=0,0,0"}</li>
     * </ul>
     *
     * <b>Warning:</b> despite the {@code "epsg"} word, coordinate reference systems created by {@code "+init=epsg:"}
     * syntax are not necessarily compliant with EPSG definitions. In particular, the axis order is often different.
     * Units of measurement may also differ.
     *
     * @param  definition  the Proj.4 definition string.
     * @param  dimension   the number of dimension of the CRS to create (2 or 3).
     * @return a CRS created from the given definition string and number of dimensions.
     * @throws NullPointerException if the definition string is {@code null}.
     * @throws IllegalArgumentException if the definition string is empty or the dimension argument is out of range.
     * @throws UnavailableFactoryException if the Proj.4 native library is not available.
     * @throws FactoryException if the CRS creation failed for another reason.
     *
     * @see Proj4Factory#createCoordinateReferenceSystem(String)
     */
    public static CoordinateReferenceSystem createCRS(String definition, final int dimension) throws FactoryException {
        ArgumentChecks.ensureNonEmpty("definition", definition);
        ArgumentChecks.ensureBetween("dimension", 2, 3, dimension);
        definition = definition.trim();
        try {
            return Proj4Factory.INSTANCE.createCRS(definition, dimension >= 3);
        } catch (IllegalArgumentException | ParserException e) {
            throw new InvalidGeodeticParameterException(canNotParse(definition), e);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new UnavailableFactoryException(unavailable(e), e);
        }
    }

    /**
     * Creates an operation for conversion or transformation between two coordinate reference systems.
     * The given CRSs should be instances created by this package. If not, then there is a choice:
     *
     * <ul>
     *   <li>If {@code force} is {@code false}, then this method returns {@code null}.</li>
     *   <li>Otherwise this method always uses Proj.4 for performing the coordinate operations,
     *       regardless if the given CRS were created from Proj.4 definition strings or not.
     *       This method fails if it can not map the given CRS to Proj.4 data structures.</li>
     * </ul>
     *
     * <p><b>Recommended alternative</b></p>
     * Provided that an <a href="http://sis.apache.org/epsg.html">EPSG database is available</a>,
     * Apache SIS {@link CRS#findOperation CRS.findOperation(…)} method produces results that are closer
     * to the authoritative definitions of coordinate operations (technically, Apache SIS referencing
     * engine is a <cite>late-binding</cite> implementation while Proj.4 is an <cite>early-binding</cite>
     * implementation — see EPSG guidance notes for a definition of late versus early-binding approaches).
     * Apache SIS also attaches metadata about
     * {@linkplain AbstractCoordinateOperation#getCoordinateOperationAccuracy() coordinate operation accuracy} and
     * {@linkplain AbstractCoordinateOperation#getDomainOfValidity() domain of validity}, have extended support of
     * multi-dimensional CRS and provides transform derivatives. This {@code Proj4.createOperation(…)} method should
     * be reserved to situations where an application needs to reproduce the same numerical results than Proj.4.
     *
     * @param  sourceCRS  the source coordinate reference system.
     * @param  targetCRS  the target coordinate reference system.
     * @param  force      whether to force the creation of a Proj.4 transform
     *                    even if the given CRS are not wrappers around Proj.4 data structures.
     * @return a coordinate operation for transforming coordinates from the given source CRS to the given target CRS, or
     *         {@code null} if the given CRS are not wrappers around Proj.4 data structures and {@code force} is false.
     * @throws UnavailableFactoryException if {@code force} is {@code true} and the Proj.4 native library is not available.
     * @throws FactoryException if {@code force} is {@code true} and this method can not create Proj.4 transform
     *         for the given pair of coordinate reference systems for another reason.
     *
     * @see Proj4Factory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, boolean)
     * @see CRS#findOperation(CoordinateReferenceSystem, CoordinateReferenceSystem, GeographicBoundingBox)
     */
    public static CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                                      final CoordinateReferenceSystem targetCRS,
                                                      final boolean force)
            throws FactoryException
    {
        ArgumentChecks.ensureNonNull("sourceCRS", sourceCRS);
        ArgumentChecks.ensureNonNull("targetCRS", targetCRS);
        try {
            return Proj4Factory.INSTANCE.createOperation(sourceCRS, targetCRS, force);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            throw new UnavailableFactoryException(unavailable(e), e);
        }
    }

    /**
     * Returns the error message for a {@literal Proj.4} not found.
     */
    static String unavailable(final Throwable e) {
        String message = e.getLocalizedMessage();
        if (message == null || message.indexOf(' ') < 0) {      // Keep existing message if it is a sentence.
            message = Errors.format(Errors.Keys.NativeInterfacesNotFound_2, OS.uname(), "libproj");
        }
        return message;
    }

    /**
     * Returns the error message for a {@literal Proj.4} string that can not be parsed.
     */
    static String canNotParse(final String code) {
        return Errors.format(Errors.Keys.CanNotParse_1, code);
    }
}
