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
package org.apache.sis.openoffice;

import java.text.ParseException;

import org.opengis.metadata.Metadata;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;

import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.io.wkt.Transliterator;
import org.apache.sis.util.Classes;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.PatchedUnitFormat;
import org.apache.sis.internal.storage.CodeType;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;


/**
 * Implements the {@link XReferencing} methods to make available to Apache OpenOffice.
 *
 * @author  Richard Deplanque (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public class Referencing extends CalcAddins implements XReferencing {
    /**
     * The name for the registration of this component.
     * <strong>NOTE:</strong> OpenOffice expects a field with exactly that name; do not rename!
     */
    public static final String __serviceName = "org.apache.sis.openoffice.Referencing";

    /**
     * Constructs a default implementation of {@code XReferencing} interface.
     */
    public Referencing() {
        methods.put("getName", new MethodInfo("Referencing", "CRS.NAME",
            "Returns a description for an object identified by the given authority code.",
            new String[] {
                "xOptions",   "Provided by OpenOffice.",
                "code",       "The code allocated by authority."
        }));
        methods.put("getGeographicArea", new MethodInfo("Referencing", "GEOGRAPHIC.AREA",
            "Returns the valid area as a geographic bounding box for an identified object.",
            new String[] {
                "xOptions",   "Provided by OpenOffice.",
                "code",       "The code allocated by authority."
        }));
        methods.put("getAxis", new MethodInfo("Referencing", "CRS.AXIS",
            "Returns the axis name for the specified dimension in an identified object.",
            new String[] {
                "xOptions",   "Provided by OpenOffice.",
                "code",       "The code allocated by authority.",
                "dimension",  "The dimension (1, 2, …)."
        }));
        methods.put("getAccuracy", new MethodInfo("Referencing", "TRANSFORM.ACCURACY",
            "Returns the accuracy of a transformation between two coordinate reference systems.",
            new String[] {
                "xOptions",    "Provided by OpenOffice.",
                "source CRS",  "The source coordinate reference system.",
                "target CRS",  "The target coordinate reference system.",
                "coordinates", "The coordinate values to transform."
        }));
        methods.put("transform", new MethodInfo("Referencing", "TRANSFORM.POINTS",
            "Transform coordinates from the given source CRS to the given target CRS.",
            new String[] {
                "xOptions",    "Provided by OpenOffice.",
                "source CRS",  "The source coordinate reference system.",
                "target CRS",  "The target coordinate reference system.",
                "coordinates", "The coordinate values to transform."
        }));
        methods.put("parseAngle", new MethodInfo("Text", "VALUE.ANGLE",
            "Converts text in degrees-minutes-seconds to an angle in decimal degrees.",
            new String[] {
                "xOptions",   "Provided by OpenOffice.",
                "text",       "The text to be converted to an angle.",
                "pattern",    "The text that describes the format (example: \"D°MM.m'\")."
        }));
        methods.put("formatAngle", new MethodInfo("Text", "TEXT.ANGLE",
            "Converts an angle to text according to a given format.",
            new String[] {
                "xOptions",   "Provided by OpenOffice.",
                "value",      "The angle value (in decimal degrees) to be converted.",
                "pattern",    "The text that describes the format (example: \"D°MM.m'\")."
        }));
    }

    /**
     * The service name that can be used to create such an object by a factory.
     *
     * @return value of {@code __serviceName}.
     */
    @Override
    public String getServiceName() {
        return __serviceName;
    }

    /**
     * Gets the CRS or other kind of object from the given code.
     * If the code is a URN, then it can be any kind of object.
     * Otherwise a Coordinate Reference System is assumed.
     * This method caches the result.
     *
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @param  type        how to interpret {@code codeOrPath}, or {@code null} for guessing.
     * @return the identified object for the given code.
     * @throws FactoryException if an error occurred while creating the object.
     * @throws DataStoreException if an error occurred while reading a data file.
     */
    private IdentifiedObject getIdentifiedObject(final String codeOrPath, CodeType type)
            throws FactoryException, DataStoreException
    {
        final CacheKey<IdentifiedObject> key = new CacheKey<>(IdentifiedObject.class, codeOrPath, null, null);
        IdentifiedObject object = key.peek();
        if (object == null) {
            final Cache.Handler<IdentifiedObject> handler = key.lock();
            try {
                object = handler.peek();
                if (object == null) {
                    if (type == null) {
                        type = CodeType.guess(codeOrPath);
                    }
                    if (type.equals(CodeType.URN)) {
                        object = CRS.getAuthorityFactory(null).createObject(codeOrPath);
                    } else if (type.isCRS) {
                        object = CRS.forCode(codeOrPath);
                    } else {
                        /*
                         * Apparently not an AUTHORITY:CODE string.
                         * Try to read a dataset from a file or URL, then get its CRS.
                         */
                        final Metadata metadata;
                        try (DataStore store = DataStores.open(codeOrPath)) {
                            metadata = store.getMetadata();
                        }
                        if (metadata != null) {
                            for (final ReferenceSystem rs : metadata.getReferenceSystemInfo()) {
                                if (rs instanceof CoordinateReferenceSystem) {
                                    return rs;
                                } else if (object == null) {
                                    object = rs;                // Will be used as a fallback if we find no CRS.
                                }
                            }
                        }
                        if (object == null) {
                            throw new FactoryException(Errors.getResources(getJavaLocale()).getString(Errors.Keys.UnspecifiedCRS));
                        }
                    }
                }
            } finally {
                handler.putAndUnlock(object);
            }
        }
        return object;
    }

    /**
     * Returns the identified object name from an authority code.
     *
     * @param  xOptions    provided by OpenOffice.
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @return the object name.
     */
    @Override
    public String getName(final XPropertySet xOptions, final String codeOrPath) {
        final InternationalString name;
        try {
            final IdentifiedObject object;
            final CodeType type = CodeType.guess(codeOrPath);
            if (type.isCRS) {
                object = new CacheKey<>(IdentifiedObject.class, codeOrPath, null, null).peek();
            } else {
                object = getIdentifiedObject(codeOrPath, type);
            }
            if (object != null) {
                return object.getName().getCode();
            }
            // In Apache SIS implementation, 'getDescriptionText' returns the name.
            name = CRS.getAuthorityFactory(null).getDescriptionText(codeOrPath);
        } catch (Exception exception) {
            return getLocalizedMessage(exception);
        }
        return (name != null) ? name.toString(getJavaLocale()) : noResultString();
    }

    /**
     * Returns the axis name and units for the specified dimension in a coordinate reference system or coordinate system.
     * This method returns a short axis name as used in Well Known Text (WKT) format, for example <cite>"Latitude"</cite>
     * instead of <cite>"Geodetic latitude"</cite>.
     *
     * @param  xOptions    provided by OpenOffice.
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @param  dimension   the dimension (1, 2, …).
     * @return the name of the requested axis.
     */
    @Override
    public String getAxis(final XPropertySet xOptions, final String codeOrPath, final int dimension) {
        final CacheKey<String> key = new CacheKey<>(String.class, codeOrPath, dimension, null);
        String name = key.peek();
        if (name == null) {
            final Cache.Handler<String> handler = key.lock();
            try {
                name = handler.peek();
                if (name == null) {
                    final IdentifiedObject object;
                    try {
                        object = getIdentifiedObject(codeOrPath, null);
                    } catch (Exception exception) {
                        return getLocalizedMessage(exception);
                    }
                    CoordinateSystem cs = null;
                    final CoordinateSystemAxis axis;
                    if (object instanceof CoordinateSystemAxis) {
                        axis = (CoordinateSystemAxis) object;
                    } else {
                        if (object instanceof CoordinateReferenceSystem) {
                            cs = ((CoordinateReferenceSystem) object).getCoordinateSystem();
                        } else if (object instanceof CoordinateSystem) {
                            cs = (CoordinateSystem) object;
                        } else {
                            final Class<?> actual;
                            if (object instanceof AbstractIdentifiedObject) {
                                actual = ((AbstractIdentifiedObject) object).getInterface();
                            } else {
                                actual = Classes.getClass(object);
                            }
                            return Errors.getResources(getJavaLocale()).getString(Errors.Keys.UnexpectedTypeForReference_3,
                                    codeOrPath, CoordinateReferenceSystem.class, actual);
                        }
                        if (dimension >= 1 && dimension <= cs.getDimension()) {
                            axis = cs.getAxis(dimension - 1);
                        } else {
                            return Errors.getResources(getJavaLocale()).getString(Errors.Keys.IndexOutOfBounds_1, dimension);
                        }
                    }
                    final String unit = PatchedUnitFormat.toString(axis.getUnit());
                    name = Transliterator.DEFAULT.toShortAxisName(cs, axis.getDirection(), axis.getName().getCode());
                    if (unit != null && !unit.isEmpty()) {
                        name = name + " (" + unit + ')';
                    }
                }
            } finally {
                handler.putAndUnlock(name);
            }
        }
        return name;
    }

    /**
     * Returns the domain of validity as a geographic bounding box for an identified object.
     * This method returns a 2×2 matrix:
     * the first row contains the latitude and longitude of upper left corner,
     * and the second row contains the latitude and longitude of bottom right corner.
     * Units are degrees.
     *
     * @param  xOptions    provided by OpenOffice.
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @return the object bounding box.
     */
    @Override
    public double[][] getGeographicArea(final XPropertySet xOptions, final String codeOrPath) {
        final CacheKey<GeographicBoundingBox> key = new CacheKey<>(GeographicBoundingBox.class, codeOrPath, null, null);
        GeographicBoundingBox area = key.peek();
        if (area == null) {
            final Cache.Handler<GeographicBoundingBox> handler = key.lock();
            try {
                area = handler.peek();
                if (area == null) try {
                    final IdentifiedObject object = getIdentifiedObject(codeOrPath, null);
                    final Object domain = IdentifiedObjects.getProperties(object).get(ReferenceSystem.DOMAIN_OF_VALIDITY_KEY);
                    if (domain instanceof Extent) {
                        area = Extents.getGeographicBoundingBox((Extent) domain);
                    }
                } catch (Exception exception) {
                    reportException("getGeographicArea", exception, THROW_EXCEPTION);
                }
            } finally {
                handler.putAndUnlock(area);
            }
        }
        if (area == null) {
            return getFailure(4,4);
        }
        return new double[][] {
            new double[] {area.getNorthBoundLatitude(), area.getWestBoundLongitude()},
            new double[] {area.getSouthBoundLatitude(), area.getEastBoundLongitude()}
        };
    }

    /**
     * Gets the {@code IdentifiedObject} for the given code as a {@code CoordinateReferenceSystem}.
     *
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @return the coordinate reference system for the given code.
     * @throws FactoryException if an error occurred while creating the object.
     * @throws DataStoreException if an error occurred while reading a data file.
     */
    final CoordinateReferenceSystem getCRS(final String codeOrPath) throws FactoryException, DataStoreException {
        final IdentifiedObject object = getIdentifiedObject(codeOrPath, null);
        if (object == null || object instanceof CoordinateReferenceSystem) {
            return (CoordinateReferenceSystem) object;
        }
        throw new FactoryException(Errors.getResources(getJavaLocale()).getString(
                Errors.Keys.UnexpectedTypeForReference_3, codeOrPath,
                CoordinateReferenceSystem.class, Classes.getClass(object)));
    }

    /**
     * Returns the accuracy of a transformation between two coordinate reference systems.
     *
     * @param  xOptions   provided by OpenOffice.
     * @param  sourceCRS  the authority code for the source coordinate reference system.
     * @param  targetCRS  the authority code for the target coordinate reference system.
     * @param  points     the coordinates to transform (for computing area of interest).
     * @return the operation accuracy.
     */
    @Override
    public double getAccuracy(final XPropertySet xOptions,
            final String sourceCRS, final String targetCRS, final double[][] points)
    {
        try {
            return new Transformer(this, getCRS(sourceCRS), targetCRS, points).getAccuracy();
        } catch (Exception exception) {
            reportException("getAccuracy", exception, THROW_EXCEPTION);
            return Double.NaN;
        }
    }

    /**
     * Transforms coordinates from the specified source CRS to the specified target CRS.
     *
     * @param  xOptions   provided by OpenOffice.
     * @param  sourceCRS  the authority code for the source coordinate reference system.
     * @param  targetCRS  the authority code for the target coordinate reference system.
     * @param  points     the coordinates to transform.
     * @return The transformed coordinates.
     */
    @Override
    public double[][] transformCoordinates(final XPropertySet xOptions,
            final String sourceCRS, final String targetCRS, final double[][] points)
    {
        if (points == null || points.length == 0) {
            return new double[][] {};
        } else try {
            return new Transformer(this, getCRS(sourceCRS), targetCRS, points).transform(points);
        } catch (Exception exception) {
            reportException("transformCoordinates", exception, THROW_EXCEPTION);
            return getFailure(points.length, 2);
        }
    }

    /**
     * Converts text in degrees-minutes-seconds to an angle in decimal degrees.
     * See {@link org.apache.sis.measure.AngleFormat} for pattern description.
     *
     * @param  xOptions  provided by OpenOffice.
     * @param  text      the text to be converted to an angle.
     * @param  pattern   an optional text that describes the format (example: "D°MM.m'").
     * @return the angle parsed as a number.
     * @throws IllegalArgumentException if {@code pattern} is illegal.
     */
    @Override
    public double parseAngle(final XPropertySet xOptions,
                             final String       text,
                             final Object       pattern)
            throws IllegalArgumentException
    {
        try {
            return new AnglePattern(pattern).parse(text, getJavaLocale());
        } catch (ParseException exception) {
            reportException("parseAngle", exception, THROW_EXCEPTION);
            return Double.NaN;
        }
    }

    /**
     * Converts an angle to text according to a given format. This method uses the pattern
     * described by {@link org.apache.sis.measure.AngleFormat} with the following extension:
     *
     * <ul>
     *   <li>If the pattern ends with E or W, then the angle is formatted as a longitude.</li>
     *   <li>If the pattern ends with N or S, then the angle is formatted as a latitude.</li>
     * </ul>
     *
     * @param  xOptions  provided by OpenOffice.
     * @param  value     the angle value (in decimal degrees) to be converted.
     * @param  pattern   an optional text that describes the format (example: "D°MM.m'").
     * @return the angle formatted as a string.
     * @throws IllegalArgumentException if {@code pattern} is illegal.
     */
    @Override
    public String formatAngle(final XPropertySet xOptions,
                              final double       value,
                              final Object       pattern)
            throws IllegalArgumentException
    {
        return new AnglePattern(pattern).format(value, getJavaLocale());
    }
}
