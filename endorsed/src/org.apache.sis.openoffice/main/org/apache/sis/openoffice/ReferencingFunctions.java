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

import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XComponentContext;
import com.sun.star.lang.IllegalArgumentException;
import org.opengis.metadata.Metadata;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.io.wkt.Transliterator;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Locales;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.CodeType;

// Specific to the main and geoapi-4.0 branches:
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.ObjectDomain;


/**
 * Implements the {@link XReferencing} methods to make available to Apache OpenOffice.
 *
 * @author  Richard Deplanque (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.5
 * @since   0.8
 */
@SuppressWarnings("UseSpecificCatch")
public class ReferencingFunctions extends CalcAddins implements XReferencing {
    /**
     * The name for the registration of this component.
     */
    static final String SERVICE_NAME = "org.apache.sis.openoffice.Referencing";

    /**
     * The implementation name, which shall be the {@link ReferencingFunctions} class name.
     */
    static final String IMPLEMENTATION_NAME = "org.apache.sis.openoffice.ReferencingFunctions";

    /**
     * Constructs an implementation of {@code XReferencing} interface.
     *
     * @param context  the value to assign to the {@link #context} field.
     */
    public ReferencingFunctions(final XComponentContext context) {
        super(context);
    }

    /**
     * The service name that can be used to create such an object by a factory.
     *
     * @return unique name of the service.
     */
    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    /**
     * Provides the implementation name of the service.
     *
     * @return unique name of the implementation.
     */
    @Override
    public String getImplementationName() {
        return IMPLEMENTATION_NAME;
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
                        CRSAuthorityFactory factory = CRS.getAuthorityFactory(null);
                        if (factory instanceof GeodeticAuthorityFactory) {
                            object = ((GeodeticAuthorityFactory) factory).createObject(codeOrPath);
                        } else {
                            object = factory.createCoordinateReferenceSystem(codeOrPath);
                        }
                    } else if (type.isAuthorityCode) {
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
                            throw new FactoryException(Errors.forLocale(getJavaLocale()).getString(Errors.Keys.UnspecifiedCRS));
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
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @return the object name.
     */
    @Override
    public String getName(final String codeOrPath) {
        final InternationalString name;
        try {
            final IdentifiedObject object;
            final CodeType type = CodeType.guess(codeOrPath);
            Class<? extends IdentifiedObject> classe = CoordinateReferenceSystem.class;
            if (type.isAuthorityCode) {
                object = new CacheKey<>(IdentifiedObject.class, codeOrPath, null, null).peek();
                if (type.isURI) {
                    classe = IdentifiedObject.class;    // The actual type will be detected from the URI.
                }
            } else {
                object = getIdentifiedObject(codeOrPath, type);
            }
            if (object != null) {
                return object.getName().getCode();
            }
            // In Apache SIS implementation, `getDescriptionText(…)` returns the identified object name.
            name = CRS.getAuthorityFactory(null).getDescriptionText(classe, codeOrPath).orElse(null);
        } catch (Exception exception) {
            return getLocalizedMessage(exception);
        }
        return (name != null) ? name.toString(getJavaLocale()) : noResultString();
    }

    /**
     * Returns the identified object scope from an authority code.
     *
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @return the object scope.
     */
    @Override
    public String getScope(final String codeOrPath) {
        try {
            final IdentifiedObject object = getIdentifiedObject(codeOrPath, null);
            for (final ObjectDomain domain : object.getDomains()) {
                InternationalString scope = domain.getScope();
                if (scope != null) {
                    return scope.toString(getJavaLocale());
                }
            }
        } catch (Exception exception) {
            return getLocalizedMessage(exception);
        }
        return noResultString();
    }

    /**
     * Returns the domain of validity from an authority code.
     *
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @return the domain of validity.
     */
    @Override
    public String getDomainOfValidity(final String codeOrPath) {
        try {
            final IdentifiedObject object = getIdentifiedObject(codeOrPath, null);
            for (final ObjectDomain domain : object.getDomains()) {
                final Extent extent = domain.getDomainOfValidity();
                if (extent != null) {
                    final InternationalString description = extent.getDescription();
                    if (description != null) {
                        return description.toString(getJavaLocale());
                    }
                }
            }
        } catch (Exception exception) {
            return getLocalizedMessage(exception);
        }
        return noResultString();
    }

    /**
     * Returns the domain of validity as a geographic bounding box for an identified object.
     * This method returns a 2×2 matrix:
     * the first row contains the latitude and longitude of upper left corner,
     * and the second row contains the latitude and longitude of bottom right corner.
     * Units are degrees.
     *
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @return the object bounding box.
     */
    @Override
    public double[][] getGeographicArea(final String codeOrPath) {
        final CacheKey<GeographicBoundingBox> key = new CacheKey<>(GeographicBoundingBox.class, codeOrPath, null, null);
        GeographicBoundingBox area = key.peek();
        if (area == null) {
            final Cache.Handler<GeographicBoundingBox> handler = key.lock();
            try {
                area = handler.peek();
                if (area == null) try {
                    final IdentifiedObject object = getIdentifiedObject(codeOrPath, null);
                    for (final ObjectDomain domain : object.getDomains()) {
                        area = Extents.getGeographicBoundingBox(domain.getDomainOfValidity());
                        if (area != null) {
                            break;
                        }
                    }
                } catch (Exception exception) {
                    reportException("getGeographicArea", exception);
                }
            } finally {
                handler.putAndUnlock(area);
            }
        }
        if (area == null) {
            return new double[][] {};
        }
        return new double[][] {
            new double[] {area.getNorthBoundLatitude(), area.getWestBoundLongitude()},
            new double[] {area.getSouthBoundLatitude(), area.getEastBoundLongitude()}
        };
    }

    /**
     * Returns the axis name and units for the specified dimension in a coordinate reference system or coordinate system.
     * This method returns a short axis name as used in Well Known Text (WKT) format, for example <q>Latitude</q>
     * instead of <q>Geodetic latitude</q>.
     *
     * @param  codeOrPath  the code allocated by an authority, or the path to a file.
     * @param  dimension   the dimension (1, 2, …).
     * @return the name of the requested axis.
     */
    @Override
    public String getAxis(final String codeOrPath, final int dimension) {
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
                            return Errors.forLocale(getJavaLocale()).getString(Errors.Keys.UnexpectedTypeForReference_3,
                                    codeOrPath, CoordinateReferenceSystem.class, actual);
                        }
                        if (dimension >= 1 && dimension <= cs.getDimension()) {
                            axis = cs.getAxis(dimension - 1);
                        } else {
                            return Errors.forLocale(getJavaLocale()).getString(Errors.Keys.IndexOutOfBounds_1, dimension);
                        }
                    }
                    final String unit = axis.getUnit().toString();
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
        throw new FactoryException(Errors.forLocale(getJavaLocale()).getString(
                Errors.Keys.UnexpectedTypeForReference_3, codeOrPath,
                CoordinateReferenceSystem.class, Classes.getClass(object)));
    }

    /**
     * Returns the accuracy of a transformation between two coordinate reference systems.
     *
     * @param  sourceCRS       the authority code for the source coordinate reference system.
     * @param  targetCRS       the authority code for the target coordinate reference system.
     * @param  areaOfInterest  an optional bounding box of source coordinates to transform.
     * @throws IllegalArgumentException if {@code points} is not a {@code double[][]} value or void.
     * @return the operation accuracy.
     */
    @Override
    public double getAccuracy(final String sourceCRS, final String targetCRS, final Object areaOfInterest)
            throws IllegalArgumentException
    {
        final double[][] coordinates;
        if (AnyConverter.isVoid(areaOfInterest)) {
            coordinates = null;
        } else if (areaOfInterest instanceof double[][]) {
            coordinates = (double[][]) areaOfInterest;
        } else if (areaOfInterest instanceof Object[][]) {
            final Object[][] values = (Object[][]) areaOfInterest;
            coordinates = new double[values.length][];
            for (int j=0; j<values.length; j++) {
                final Object[] row = values[j];
                final double[] coord = new double[row.length];
                for (int i=0; i<row.length; i++) {
                    coord[i] = AnyConverter.toDouble(row[i]);
                }
                coordinates[j] = coord;
            }
        } else {
            throw new IllegalArgumentException();
        }
        try {
            return new Transformer(this, getCRS(sourceCRS), targetCRS, coordinates).getAccuracy();
        } catch (Exception exception) {
            reportException("getAccuracy", exception);
            return Double.NaN;
        }
    }

    /**
     * Transforms coordinates from the specified source CRS to the specified target CRS.
     *
     * @param  sourceCRS  the authority code for the source coordinate reference system.
     * @param  targetCRS  the authority code for the target coordinate reference system.
     * @param  points     the coordinates to transform.
     * @return the transformed coordinates.
     */
    @Override
    public double[][] transformPoints(final String sourceCRS, final String targetCRS, final double[][] points) {
        if (points == null || points.length == 0) {
            return new double[][] {};
        }
        double[][] result;
        Exception warning;
        try {
            final Transformer tr = new Transformer(this, getCRS(sourceCRS), targetCRS, points);
            result  = tr.transform(points);
            warning = tr.warning;
        } catch (Exception exception) {
            result  = new double[][] {};
            warning = exception;
        }
        if (warning != null) {
            reportException("transformPoints", warning);
        }
        return result;
    }

    /**
     * Transforms an envelope from the specified source CRS to the specified target CRS.
     *
     * @param  sourceCRS  the authority code for the source coordinate reference system.
     * @param  targetCRS  the authority code for the target coordinate reference system.
     * @param  envelope   points inside the envelope to transform.
     * @return the transformed envelope.
     */
    @Override
    public double[][] transformEnvelope(String sourceCRS, String targetCRS, double[][] envelope) {
        if (envelope != null && envelope.length != 0) try {
            return new Transformer(this, getCRS(sourceCRS), targetCRS, envelope).transformEnvelope(envelope);
        } catch (Exception exception) {
            reportException("transformEnvelope", exception);
        }
        return new double[][] {};
    }

    /**
     * Converts text in degrees-minutes-seconds to an angle in decimal degrees.
     * See {@link org.apache.sis.measure.AngleFormat} for pattern description.
     *
     * @param  text     the text to be converted to an angle.
     * @param  pattern  an optional text that describes the format (example: "D°MM.m'").
     * @param  locale   the convention to use (e.g. decimal separator symbol).
     * @return the angle parsed as a number.
     * @throws IllegalArgumentException if {@code pattern} is not a string value or void.
     */
    @Override
    public double[][] parseAngle(final String[][] text, final Object pattern, final Object locale)
            throws IllegalArgumentException
    {
        final AnglePattern p = new AnglePattern(pattern);
        final double[][] result = p.parse(text, AnyConverter.isVoid(locale)
                ? getJavaLocale() : Locales.parse(AnyConverter.toString(locale)));
        if (p.warning != null) {
            reportException("parseAngle", p.warning);
        }
        return result;
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
     * @param  value    the angle value (in decimal degrees) to be converted.
     * @param  pattern  an optional text that describes the format (example: "D°MM.m'").
     * @param  locale   the convention to use (e.g. decimal separator symbol).
     * @return the angle formatted as a string.
     * @throws IllegalArgumentException if {@code pattern} is not a string value or void.
     */
    @Override
    public String[][] formatAngle(final double[][] value, final Object pattern, final Object locale)
            throws IllegalArgumentException
    {
        return new AnglePattern(pattern).format(value, AnyConverter.isVoid(locale)
                ? getJavaLocale() : Locales.parse(AnyConverter.toString(locale)));
    }
}
