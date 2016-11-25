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
package org.apache.sis.storage.geotiff;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.Unit;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.storage.DataStoreContentException;


/**
 * Helper class for building a {@link CoordinateReferenceSystem} from information found in TIFF tags.
 * A {@code CRSBuilder} receives as inputs the values of the following TIFF tags:
 *
 * <ul>
 *   <li>{@link Tags#GeoKeyDirectory} — array of unsigned {@code short} values grouped into blocks of 4.</li>
 *   <li>{@link Tags#GeoDoubleParams} — array of {@double} values referenced by {@code GeoKeyDirectory} elements.</li>
 *   <li>{@link Tags#GeoAsciiParams}  — array of characters referenced by {@code GeoKeyDirectory} elements.</li>
 * </ul>
 *
 * For example, consider the following values for the above-cited tags:
 *
 * <table class="sis">
 *   <caption>GeoKeyDirectory(34735) values</caption>
 *   <tr><td>    1 </td><td>     1 </td><td>  2 </td><td>     6 </td></tr>
 *   <tr><td> 1024 </td><td>     0 </td><td>  1 </td><td>     2 </td></tr>
 *   <tr><td> 1026 </td><td> 34737 </td><td>  0 </td><td>    12 </td></tr>
 *   <tr><td> 2048 </td><td>     0 </td><td>  1 </td><td> 32767 </td></tr>
 *   <tr><td> 2049 </td><td> 34737 </td><td> 14 </td><td>    12 </td></tr>
 *   <tr><td> 2050 </td><td>     0 </td><td>  1 </td><td>     6 </td></tr>
 *   <tr><td> 2051 </td><td> 34736 </td><td>  1 </td><td>     0 </td></tr>
 * </table>
 *
 * {@preformattext
 *   GeoDoubleParams(34736) = {1.5}
 *   GeoAsciiParams(34737) = "Custom File|My Geographic|"
 * }
 *
 * <p>The first number in the {@code GeoKeyDirectory} table indicates that this is a version 1 GeoTIFF GeoKey directory.
 * This version will only change if the key structure is changed. The other numbers on the first line said that the file
 * uses revision 1.2 of the set of keys and that there is 6 key values.</p>
 *
 * <p>The next line indicates that the first key (1024 = {@code ModelType}) has the value 2 (Geographic),
 * explicitly placed in the entry list since the TIFF tag location is 0.
 * The next line indicates that the key 1026 ({@code Citation}) is listed in the {@code GeoAsciiParams(34737)} array,
 * starting at offset 0 (the first in array), and running for 12 bytes and so has the value "Custom File".
 * The "|" character is converted to a null delimiter at the end in C/C++ libraries.</p>
 *
 * <p>Going further down the list, the key 2051 ({@code GeogLinearUnitSize}) is located in {@code GeoDoubleParams(34736)}
 * at offset 0 and has the value 1.5; the value of key 2049 ({@code GeogCitation}) is "My Geographic".</p>
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see GeoKeys
 */
final class CRSBuilder {
    /**
     * The reader for which we will create coordinate reference systems.
     * This is used for reporting warnings.
     */
    private final Reader reader;

    /**
     * Version of the set of keys declared in the {@code GeoKeyDirectory} header.
     */
    private short majorRevision, minorRevision;

    /**
     * All values found in the {@code GeoKeyDirectory} after the header.
     */
    private final Map<Short,Object> geoKeys = new HashMap<>();

    /**
     * Factory for creating geodetic objects from EPSG codes, or {@code null} if not yet fetched.
     * The EPSG code for a complete CRS definition can be stored in a single {@link GeoKeys}.
     *
     * <div class="note"><b>Note:</b> we do not yet split this field into 3 separated fields for datums,
     * coordinate systems and coordinate reference systems objects because it is not needed with Apache SIS
     * implementation of those factories. However we may revisit this choice if we want to let the user specify
     * his own factories.</div>
     *
     * @see #epsgFactory()
     */
    private GeodeticAuthorityFactory epsgFactory;

    /**
     * Factory for creating geodetic objects from their components, or {@code null} if not yet fetched.
     * Constructing a CRS from its components requires parsing many {@link GeoKeys}.
     *
     * <div class="note"><b>Note:</b> we do not yet split this field into 3 separated fields for datums,
     * coordinate systems and coordinate reference systems objects because it is not needed with Apache SIS
     * implementation of those factories. However we may revisit this choice if we want to let the user specify
     * his own factories.</div>
     *
     * @see #objectFactory()
     */
    private GeodeticObjectFactory objectFactory;

    /**
     * Factory for fetching operation methods and creating defining conversions.
     * This is needed only for user-defined projected coordinate reference system.
     *
     * @see #operationFactory()
     */
    private CoordinateOperationFactory operationFactory;

    /**
     * Creates a new builder of coordinate reference systems.
     *
     * @param reader  where to report warnings if any.
     */
    CRSBuilder(final Reader reader) {
        this.reader = reader;
    }

    /**
     * Reports a warning with a message built from the given resource keys and arguments.
     *
     * @param  key   one of the {@link Resources.Keys} constants.
     * @param  args  arguments for the log message.
     *
     * @see Resources
     */
    private void warning(final short key, final Object... args) {
        final LogRecord r = reader.resources().getLogRecord(Level.WARNING, key, args);
        reader.owner.warning(r);
    }

    /**
     * Returns the factory for creating geodetic objects from EPSG codes.
     * The factory is fetched when first needed.
     *
     * @return the EPSG factory (never {@code null}).
     */
    private GeodeticAuthorityFactory epsgFactory() throws FactoryException {
        if (epsgFactory == null) {
            epsgFactory = (GeodeticAuthorityFactory) CRS.getAuthorityFactory(Constants.EPSG);
        }
        return epsgFactory;
    }

    /**
     * Returns the factory for creating geodetic objects from their components.
     * The factory is fetched when first needed.
     *
     * @return the object factory (never {@code null}).
     */
    private GeodeticObjectFactory objectFactory() {
        if (objectFactory == null) {
            objectFactory = DefaultFactories.forBuildin(CRSFactory.class, GeodeticObjectFactory.class);
        }
        return objectFactory;
    }

    /**
     * Returns the factory for fetching operation methods and creating defining conversions.
     * The factory is fetched when first needed.
     *
     * @return the operation factory (never {@code null}).
     */
    private CoordinateOperationFactory operationFactory() {
        if (operationFactory == null) {
            operationFactory = DefaultFactories.forBuildin(CoordinateOperationFactory.class);
        }
        return operationFactory;
    }

    /**
     * Returns a map with the given name associated to the {@value org.opengis.referencing.IdentifiedObject#NAME_KEY} key.
     * This is an helper method for creating geodetic objects with {@link #objectFactory}.
     */
    private static Map<String,?> name(final String name) {
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Returns a {@link GeoKeys} value as a character string, or {@code null} if none.
     *
     * @param  key        the GeoTIFF key for which to get a value.
     * @param  mandatory  whether a value is mandatory for the given key.
     * @return a string representation of the value for the given key, or {@code null} if the key was not found.
     * @throws DataStoreContentException if a value for the given key is mandatory but no value has been found.
     */
    private String getAsString(final short key, final boolean mandatory) throws DataStoreContentException {
        final Object value = geoKeys.get(key);
        if (value != null) {
            final String s = value.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        if (!mandatory) {
            return null;
        }
        throw new DataStoreContentException(missingKey(key));
    }

    /**
     * Returns a {@link GeoKeys} value as a character string.
     *
     * @param  key        the GeoTIFF key for which to get a value.
     * @return A integer representing the value, or {@code Integer.minValue} if the key was not
     *         found or failed to parse.
     */
    private int getAsInteger(final short key) {
        final Object value = geoKeys.get(key);

        if (value == null)           return Integer.MIN_VALUE;
        if (value instanceof Number) return ((Number)value).intValue();

        try {
            final String geoKey = value.toString();
            return Integer.parseInt(geoKey);
        }  catch (Exception e) {
            warning(Resources.Keys.UnexpectedKeyValue_3, GeoKeys.getName(key)+" ("+key+")",
                    "Integer value", value.getClass().getName()+" --> "+value);
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Returns a {@link GeoKeys} value as a floating point number, or {@code null} if none.
     *
     * @param  key        the GeoTIFF key for which to get a value.
     * @param  mandatory  whether a value is mandatory for the given key.
     * @return the floating point value for the given key, or {@link Double#NaN} if the key was not found.
     * @throws DataStoreContentException if a value for the given key is mandatory but no value has been found.
     */
    private double getAsDouble(final short key, final boolean mandatory) throws DataStoreContentException {
        final Object value = geoKeys.get(key);
        if (value != null) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                warning(Resources.Keys.UnexpectedKeyValue_3, GeoKeys.getName(key)+" ("+key+")",
                        "Double value", value.getClass().getName()+" --> "+value);
            }
        }
        if (!mandatory) {
            return Double.NaN;
        }
        throw new DataStoreContentException(missingKey(key));
    }

    /**
     * Returns the error message to put in {@link DataStoreContentException} for missing keys.
     */
    private String missingKey(final short key) {
        return reader.resources().getString(Resources.Keys.MissingValue_2, reader.input.filename, GeoKeys.getName(key));
    }

    //---------------------------- geokeys parsing -----------------------------
    /**
     * Contain "brut" needed keys to build appropriate {@link CoordinateReferenceSystem}.<br>
     * To know how to parse this key see {@link #TiffCRSBuilder(org.apache.sis.storage.geotiff.Reader) } header class.<br>
     * Some of short values which define CRS appropriate behavior to build it.
     *
     * @return built CRS.
     * @throws FactoryException if problem during factory CRS creation.
     * @throws DataStoreContentException if problem during geokey parsing or CRS creation.(missing needed geokeys for example).
     */
    final CoordinateReferenceSystem build(final Vector keyDirectory, final Vector numericParameters, final String asciiParameters)
            throws DataStoreContentException, FactoryException
    {
        final int geoKeyDirectorySize = keyDirectory.size();
        if (geoKeyDirectorySize < 4)
            throw new DataStoreContentException(reader.resources().getString(
                    Resources.Keys.MismatchedLength_4, "GeoKeyDirectoryTag size", "GeoKeyDirectoryTag",
                    "> 4", keyDirectory.size()));

        final short kDV = keyDirectory.shortValue(0);
        if (kDV != 1) {
            throw new DataStoreContentException(reader.resources().getString(Resources.Keys.UnexpectedKeyValue_3, "KeyDirectoryVersion", 1, kDV));
        }
        majorRevision = keyDirectory.shortValue(1);
        minorRevision = keyDirectory.shortValue(2);
        final int numberOfKeys  = keyDirectory.intValue(3);

        final int expectedGeoKeyDirectorySize = ((numberOfKeys + 1) << 2);//-- (number of key + head) * 4 --- 1 key = 4 informations
        if (geoKeyDirectorySize != expectedGeoKeyDirectorySize) {
            warning(Resources.Keys.MismatchedLength_4,
                    "GeoKeyDirectoryTag size", "GeoKeyDirectoryTag", expectedGeoKeyDirectorySize, geoKeyDirectorySize);
        }

        //-- build Coordinate Reference System keys
        int p   = 4;
        int key = 0;
        while (p < geoKeyDirectorySize && key++ < numberOfKeys) {
            final short keyID     = keyDirectory.shortValue(p++);
            final int tagLocation = keyDirectory.intValue(p++);
            final int count       = keyDirectory.intValue(p++);
            final int valueOffset = keyDirectory.intValue(p++);
            if (tagLocation == 0) {
                //-- tiff taglocation = 0 mean offset is the stored value
                //-- and count normaly equal to 1
                assert count == 1;//-- maybe warning
                geoKeys.put(keyID, valueOffset);
            } else {
                switch (tagLocation) {
                    case Tags.GeoDoubleParams: {
                        assert count == 1;
                        geoKeys.put(keyID, numericParameters.doubleValue(valueOffset));
                        break;
                    }
                    case Tags.GeoAsciiParams: {
                        geoKeys.put(keyID, asciiParameters.substring(valueOffset, valueOffset + count));
                        break;
                    }
                }
            }
        }

        final int crsType = getAsInteger(GeoKeys.GTModelTypeGeoKey);

        switch (crsType) {
            case GeoKeys.ModelTypeProjected  : return createProjectedCRS();
            case GeoKeys.ModelTypeGeographic : return createGeographicCRS();
            case GeoKeys.ModelTypeGeocentric : throw new DataStoreContentException("not implemented yet: Geocentric CRS");
            default: {
                return null;
            }
        }
    }

    //----------------------------- GEO UTILS ----------------------------------
    /**
     * This code creates an <code>javax.Units.Unit</code> object out of the
     * <code>ProjLinearUnitsGeoKey</code> and the
     * <code>ProjLinearUnitSizeGeoKey</code>. The unit may either be
     * specified as a standard EPSG recognized unit, or may be user defined.
     *
     * @param key
     * @param userDefinedKey
     * @param base
     * @param def
     * @return <code>Unit</code> object representative of the tags in the file.
     * @throws IOException
     *             if the<code>ProjLinearUnitsGeoKey</code> is not specified
     *             or if unit is user defined and
     *             <code>ProjLinearUnitSizeGeoKey</code> is either not defined
     *             or does not contain a number.
     */
    private Unit<?> createUnit(final short key, final short userDefinedKey, final Unit<?> base, final Unit<?> def)
            throws FactoryException, DataStoreContentException
    {
        String unitCode = getAsString(key, false);

        // If not defined, return the default unit of measure.
        if (unitCode == null) {
            return def;
        }
        /*
         * If specified, retrieve the appropriate unit code. There is two cases to take into account:
         * First case is when the unit of measure has an EPSG code, second case is when it can be
         * instantiated as a conversion from meter.
         */
        if (unitCode.equals(GeoKeys.GTUserDefinedGeoKey_String)) {
            return base.multiply(getAsDouble(userDefinedKey, true));
        }

        //-- using epsg code for this unit
        return epsgFactory().createUnit(String.valueOf(unitCode));
    }

    /**
     * Creating a Geodetic Datum for the {@link #createUserDefinedGCRS(javax.measure.Unit, javax.measure.Unit) } method
     * we are creating at an higher level.<br>
     * As usual this method tries to follow the geotiff specification<br>
     * Needed tags are :
     * <ul>
     * <li> a code definition given by {@link GeoKeys.GeogGeodeticDatumGeoKey} tag </li>
     * <li> a name given by {@link GeoKeys.GeogCitationGeoKey} </li>
     * <li> required prime meridian tiff tags </li>
     * <li> required ellipsoid tiff tags </li>
     * </ul>
     *
     * @param unit to use for building this {@link GeodeticDatum}.
     * @return a {@link GeodeticDatum}.
     * @throws DataStoreContentException if datum code from relative tiff tag is missing ({@code null}).
     * @throws FactoryException if factory problem during Datum creation.
     * @see #createPrimeMeridian(javax.measure.Unit)
     * @see #createEllipsoid(javax.measure.Unit)
     */
    private GeodeticDatum createGeodeticDatum(final Unit unit)
            throws DataStoreContentException, FactoryException {

        // lookup the datum (w/o PrimeMeridian).
        String datumCode = getAsString(GeoKeys.GeogGeodeticDatumGeoKey, true);

        //-- Geodetic Datum define as an EPSG code.
        if (!datumCode.equals(GeoKeys.GTUserDefinedGeoKey_String))
            return epsgFactory().createGeodeticDatum(String.valueOf(datumCode));

        //-- USER DEFINE Geodetic Datum creation
        {
            //-- Datum name
            assert datumCode.equals(GeoKeys.GTUserDefinedGeoKey_String);
            String datumName = getAsString(GeoKeys.GeogCitationGeoKey, false);
            if (datumName == null) {
                datumName = "Unamed User Defined Geodetic Datum";
            }

            //-- particularity case
            if (datumName.equalsIgnoreCase("WGS84")) return CommonCRS.WGS84.datum();

            //-- ELLIPSOID creation
            final Ellipsoid ellipsoid = createEllipsoid(unit);

            //-- PRIME MERIDIAN
            final PrimeMeridian primeMeridian = createPrimeMeridian(unit);

            //-- factory Datum creation
            return objectFactory().createGeodeticDatum(name(datumName), ellipsoid, primeMeridian);
        }
    }

    /**
     * Creating a prime meridian for the {@link #createGeodeticDatum(javax.measure.Unit) } method
     * we are creating at an higher level.<br>
     * As usual this method tries to follow the geotiff specification<br>
     * Needed tags are :
     * <ul>
     * <li> a code definition given by {@link GeoKeys.GeogPrimeMeridianGeoKey} tag </li>
     * <li> a name given by {@link GeoKeys.GeogCitationGeoKey} </li>
     * <li> a prime meridian value given by {@link GeoKeys.GeogPrimeMeridianLongGeoKey} </li>
     * </ul>
     *
     * @param linearUnit use for building this {@link PrimeMeridian}.
     * @return a {@link PrimeMeridian} built using the provided {@link Unit} and
     *         the provided metadata.
     * @throws FactoryException if problem during factory Prime Meridian creation.
     */
    private PrimeMeridian createPrimeMeridian(final Unit linearUnit) throws DataStoreContentException, FactoryException {
        //-- prime meridian :
        //-- could be an EPSG code
        //-- or could be user defined
        //-- or not defined = greenwich
        String pmCode = getAsString(GeoKeys.GeogPrimeMeridianGeoKey, false);

        //-- if Prime Meridian code not define, assume WGS84
        if (pmCode == null) return CommonCRS.WGS84.primeMeridian();

        //-- if Prime Meridian define as an EPSG code.
        if (!pmCode.equals(GeoKeys.GTUserDefinedGeoKey_String)) {
            return epsgFactory().createPrimeMeridian(String.valueOf(pmCode));
        }
        //-- user define Prime Meridian creation
        {
            assert pmCode.equals(GeoKeys.GTUserDefinedGeoKey_String);

            double pmValue = getAsDouble(GeoKeys.GeogPrimeMeridianLongGeoKey, false);
            if (Double.isNaN(pmValue)) {
                warning(Resources.Keys.UnexpectedKeyValue_3, "GeogPrimeMeridianLongGeoKey (2061)","non null","null");
                pmValue = 0;
            }

            //-- if user define prime meridian is not define, assume WGS84
            if (pmValue == 0) return CommonCRS.WGS84.primeMeridian();

            final String name = getAsString(GeoKeys.GeogCitationGeoKey, false);
            return objectFactory().createPrimeMeridian(
                    name((name == null) ? "User Defined GEOTIFF Prime Meridian" : name), pmValue, linearUnit);
        }
    }

    /**
     * Creating an {@link Ellipsoid} following the GeoTiff spec.<br>
     * Creating a Ellipsoid for the {@link #createGeodeticDatum(javax.measure.Unit) } method
     * we are creating at an higher level.<br>
     * As usual this method tries to follow the geotiff specification<br>
     * Needed tags are :
     * <ul>
     * <li> a code definition given by {@link GeoKeys.GeogEllipsoidGeoKey} tag </li>
     * <li> a name given by {@link GeoKeys.GeogCitationGeoKey} </li>
     * <li> a semi major axis value given by {@link GeoKeys.GeogSemiMajorAxisGeoKey} </li>
     * <li> a semi major axis value given by {@link GeoKeys.GeogInvFlatteningGeoKey} </li>
     * <li> a semi major axis value given by {@link GeoKeys.GeogSemiMinorAxisGeoKey} </li>
     * </ul>
     *
     * @param unit use for building this {@link Ellipsoid}.
     * @return a {@link Ellipsoid} built using the provided {@link Unit} and
     *         the provided metadata.
     * @throws FactoryException if problem during factory Prime Meridian creation.
     * @throws DataStoreContentException if missing needed geokeys.
     */
    private Ellipsoid createEllipsoid(final Unit unit)
            throws FactoryException, DataStoreContentException {

        //-- ellipsoid key
        final String ellipsoidKey = getAsString(GeoKeys.GeogEllipsoidGeoKey, false);

        //-- if ellipsoid key NOT "user define" decode EPSG code.
        if (ellipsoidKey != null && !ellipsoidKey.equalsIgnoreCase(GeoKeys.GTUserDefinedGeoKey_String))
            return epsgFactory().createEllipsoid(ellipsoidKey);

        //-- User define Ellipsoid creation
        {
            String nameEllipsoid = getAsString(GeoKeys.GeogCitationGeoKey, false);
            if (nameEllipsoid == null) {
                nameEllipsoid = "User define unamed Ellipsoid";
            }
            //-- particularity case
            if (nameEllipsoid.equalsIgnoreCase("WGS84"))
                return CommonCRS.WGS84.ellipsoid();

            //-- try to build ellipsoid from others parameters.
            //-- get semi Major axis and, semi minor or invertflattening

            //-- semi Major
            final double semiMajorAxis = getAsDouble(GeoKeys.GeogSemiMajorAxisGeoKey, true);

            //-- try to get inverseFlattening
            double inverseFlattening = getAsDouble(GeoKeys.GeogInvFlatteningGeoKey, false);
            if (Double.isNaN(inverseFlattening)) {
                //-- get semi minor axis to build missing inverseFlattening
                final double semiMinSTR = getAsDouble(GeoKeys.GeogSemiMinorAxisGeoKey, true);
                inverseFlattening = semiMajorAxis / (semiMajorAxis - semiMinSTR);
            }

            //-- ellipsoid creation
            return objectFactory().createFlattenedSphere(name(nameEllipsoid), semiMajorAxis, inverseFlattening, unit);
        }
    }

    /**
     * Returns a Cartesian CS which is a combination of given base CS and tiff unit key if exist.<br>
     * The returned CS is search and retrieved from epsg base if exist.<br>
     * If don't exist we call {@link CoordinateSystems#replaceLinearUnit(org.opengis.referencing.cs.CoordinateSystem, javax.measure.Unit) }
     * to build expected CS.<br>
     * The retrieved Cartesian CS are :<br>
     *
     * <ul>
     * <li> epsg : 4400 [Cartesian 2D CS. Axes: easting, northing (E,N). Orientations: east, north. UoM: m.] </li>
     * <li> epsg : 4491 [Cartesian 2D CS. Axes: westing, northing (W,N). Orientations: west, north. UoM: m.] </li>
     * <li> epsg : 6503 [Cartesian 2D CS. Axes: westing, southing (Y,X). Orientations: west, south. UoM: m.] </li>
     * <li> epsg : 4500 [Cartesian 2D CS. Axes: northing, easting (N,E). Orientations: north, east. UoM: m.] </li>
     * <li> epsg : 4501 [Cartesian 2D CS. Axes: northing, westing (N,E). Orientations: north, west. UoM: m.] </li>
     * <li> epsg : 6501 [Cartesian 2D CS. Axes: southing, westing (X,Y). Orientations: south, west. UoM: m.] </li>
     * <li> epsg : 1039 [Cartesian 2D CS. Axes: easting, northing (E,N). Orientations: east, north. UoM: ft.]</li>
     * <li> epsg : 1029 [Cartesian 2D CS. Axes: northing, easting (N,E). Orientations: north, east. UoM: ft.] </li>
     * <li> epsg : 4497 [Cartesian 2D CS. Axes: easting, northing (X,Y). Orientations: east, north. UoM: ftUS.]</li>
     * <li> epsg : 4403 [Cartesian 2D CS. Axes: easting, northing (E,N). Orientations: east, north. UoM: ftCla.]</li>
     * <li> epsg : 4502 [Cartesian 2D CS. Axes: northing, easting (N,E). Orientations: north, east. UoM: ftCla.]</li>
     * </ul>
     *
     * @param baseCS
     * @param fallBackUnit
     * @return
     */
    private CartesianCS retrieveCartesianCS(final short unitKey, final CartesianCS baseCS, final Unit fallBackUnit)
            throws DataStoreContentException, FactoryException
    {
        assert baseCS.getDimension() == 2;
        CoordinateSystemAxis axis0 = baseCS.getAxis(0);
        CoordinateSystemAxis axis1 = baseCS.getAxis(1);

        //-------- Axis Number ----------------------
        // axisnumber integer reference couple axis direction on 3 bits.
        // higher's of the 3 bits, define axis combinaisons 1 for [E,N] or [W,S]
        // and 0 for [N,E] or [S,W].
        // secondly higher bit position define sens for first axis
        // third and last bit position define sens for second axis.
        // examples :
        // [E,N] : axisNumber = 111
        // [E,S] : axisNumber = 110
        // [N,W] : axisNumber = 010
        // [S,W] : axisNumber = 000 etc
        //
        //-------------------------------------------
        int axisNumber = 0;
        if (axis0.getDirection().equals(AxisDirection.EAST)
         || axis0.getDirection().equals(AxisDirection.WEST)) {
            axisNumber = 0b0100;
            if (axis0.getDirection().equals(AxisDirection.EAST))  axisNumber |= 0b0010;
            if (axis1.getDirection().equals(AxisDirection.NORTH)) axisNumber |= 0b0001;
        } else if (axis0.getDirection().equals(AxisDirection.NORTH)
                || axis0.getDirection().equals(AxisDirection.SOUTH)) {
            if (axis0.getDirection().equals(AxisDirection.EAST))  axisNumber |= 0b0010;
            if (axis1.getDirection().equals(AxisDirection.NORTH)) axisNumber |= 0b0001;
        } else {
            return (CartesianCS) CoordinateSystems.replaceLinearUnit(baseCS, fallBackUnit);
        }

        //-- get the Unit epsg code if exist
        String unitCode = getAsString(unitKey, false);
        if (unitCode == null || unitCode.equalsIgnoreCase(GeoKeys.GTUserDefinedGeoKey_String)) {
            return (CartesianCS) CoordinateSystems.replaceLinearUnit(baseCS, fallBackUnit);
        }

        if (unitCode.startsWith("epsg:") || unitCode.startsWith("EPSG:"))
            unitCode = unitCode.substring(5, unitCode.length());

        int intCode = Integer.parseInt(unitCode);
        String epsgCSCode = null;
        switch (intCode) {
            // Linear_Meter = 9001
            case 9001 : {
                switch (axisNumber) {
                    case 0b0111 : { //-- [E,N]
                        epsgCSCode = "4400";
                        break;
                    }
                    case 0b0110 : { //-- [E,S]
                        //-- no relative CS found into epsg base
                        break;
                    }
                    case 0b0101 : { //-- [W,N]
                        epsgCSCode = "4491";
                        break;
                    }
                    case 0b0100 : { //-- [W,S]
                        epsgCSCode = "6503";
                        break;
                    }
                    case 0b0011 : { //-- [N,E]
                        epsgCSCode = "4500";
                        break;
                    }
                    case 0b0010 : { //-- [N,W]
                        epsgCSCode = "4501";//-- or 6507
                        break;
                    }
                    case 0b0001 : { //-- [S,E]
                        //-- no relative CS found into epsg base
                        break;
                    }
                    case 0b0000 : { //-- [S,W]
                        epsgCSCode = "6501";
                        break;
                    }
                }
                break;
            }
            // Linear_Foot = 9002
            case 9002 : {
                switch (axisNumber) {
                    case 0b0111 : { //-- [E,N]
                        epsgCSCode = "1039";
                        break;
                    }
                    case 0b0011 : { //-- [N,E]
                        epsgCSCode = "1029";
                        break;
                    }
                    default :
                }
                break;
            }
            // Linear_Foot_US_Survey = 9003
            case 9003 : {
                switch (axisNumber) {
                    case 0b0111 : { //-- [E,N]
                        epsgCSCode = "4497";
                        break;
                    }
                    default :
                }
                break;
            }
            // Linear_Foot_Modified_American = 9004
            case 9004 : {
                break;
            }
            // Linear_Foot_Clarke = 9005
            case 9005 : {
                switch (axisNumber) {
                    case 0b0111 : { //-- [E,N]
                        epsgCSCode = "4403";
                        break;
                    }
                    case 0b0011 : { //-- [N,E]
                        epsgCSCode = "4502";
                        break;
                    }
                    default :
                }
                break;
            }
            //-- no related CS found from following unit code into epsg base
            // Linear_Foot_Indian = 9006
            case 9006 :
            // Linear_Link = 9007
            case 9007 :
            // Linear_Link_Benoit = 9008
            case 9008 :
            // Linear_Link_Sears = 9009
            case 9009 :
            // Linear_Chain_Benoit = 9010
            case 9010 :
            // Linear_Chain_Sears = 9011
            case 9011 :
            // Linear_Yard_Sears = 9012
            case 9012 :
            // Linear_Yard_Indian = 9013
            case 9013 :
            // Linear_Fathom = 9014
            case 9014 :
            // Linear_Mile_International_Nautical = 9015
            case 9015 :
            default :
        }

        if (epsgCSCode == null)
            //-- epsg CS + Unit not found
            return (CartesianCS) CoordinateSystems.replaceLinearUnit(baseCS, fallBackUnit);

        epsgCSCode = "EPSG:".concat(epsgCSCode);

        return epsgFactory().createCartesianCS(epsgCSCode);
    }

    /**
     * Returns a {@link EllipsoidalCS} which is a combination of given base CS and tiff unit key if exist.<br>
     * The returned CS is searched and retrieved from epsg base if exist.<br>
     * If don't exist, we call {@link CoordinateSystems#replaceAngularUnit(org.opengis.referencing.cs.CoordinateSystem, javax.measure.Unit) }
     * to build expected CS.<br>
     * The retrieved Angular CS are :<br>
     *
     * <ul>
     * <li> epsg : 6428 [Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: rad] </li>
     * <li> epsg : 6429 [Ellipsoidal 2D CS. Axes: longitude, latitude. Orientations: east, north. UoM: rad] </li>
     * <li> epsg : 6422 [Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: degree] </li>
     * <li> epsg : 6424 [Ellipsoidal 2D CS. Axes: longitude, latitude. Orientations: east, north. UoM: degree] </li>
     * <li> epsg : 6403 [Ellipsoidal 2D CS. Axes: latitude, longitude. Orientations: north, east. UoM: grads.] </li>
     * <li> epsg : 6425 [Ellipsoidal 2D CS. Axes: longitude, latitude. Orientations: east, north. UoM: grads.] </li>
     * </ul>
     *
     * @param baseCS
     * @param fallBackUnit
     * @return
     */
    private EllipsoidalCS retrieveEllipsoidalCS(final short unitKey, final EllipsoidalCS baseCS, final Unit fallBackUnit)
            throws DataStoreContentException, FactoryException
    {
        assert baseCS.getDimension() == 2;
        CoordinateSystemAxis axis0 = baseCS.getAxis(0);
        CoordinateSystemAxis axis1 = baseCS.getAxis(1);

        //-------- Axis Number ----------------------
        // axisnumber integer reference couple axis direction on 3 bits.
        // higher's of the 3 bits, define axis combinaisons 1 for [E,N] or [W,S]
        // and 0 for [N,E] or [S,W].
        // secondly higher bit position define sens for first axis
        // third and last bit position define sens for second axis.
        // examples :
        // [E,N] : axisNumber = 111
        // [E,S] : axisNumber = 110
        // [N,W] : axisNumber = 010
        // [S,W] : axisNumber = 000 etc
        //
        //-------------------------------------------
        int axisNumber = 0;
        if (axis0.getDirection().equals(AxisDirection.EAST)
         || axis0.getDirection().equals(AxisDirection.WEST)) {
            axisNumber = 0b0100;
            if (axis0.getDirection().equals(AxisDirection.EAST))  axisNumber |= 0b0010;
            if (axis1.getDirection().equals(AxisDirection.NORTH)) axisNumber |= 0b0001;
        } else if (axis0.getDirection().equals(AxisDirection.NORTH)
                || axis0.getDirection().equals(AxisDirection.SOUTH)) {
            if (axis0.getDirection().equals(AxisDirection.EAST))  axisNumber |= 0b0010;
            if (axis1.getDirection().equals(AxisDirection.NORTH)) axisNumber |= 0b0001;
        } else {
            return (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(baseCS, fallBackUnit);
        }

        //-- get the Unit epsg code if exist
        String unitCode = getAsString(unitKey, false);
        if (unitCode == null || unitCode.equalsIgnoreCase(GeoKeys.GTUserDefinedGeoKey_String)) {
            return (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(baseCS, fallBackUnit);
        }

        if (unitCode.startsWith("epsg:") || unitCode.startsWith("EPSG:"))
            unitCode = unitCode.substring(5, unitCode.length());

        int intCode = Integer.parseInt(unitCode);
        String epsgCSCode = null;
        switch (intCode) {
            // Angular_Radian = 9101
            case 9101 : {
                switch (axisNumber) {
                    case 0b0111 : { //-- [E,N]
                        epsgCSCode = "6429";
                        break;
                    }
                    case 0b0011 : { //-- [N,E]
                        epsgCSCode = "6428";
                        break;
                    }
                    default :
                }
                break;
            }
            //-- Angular_Degree = 9102
            //-- into epsg base, 9102 angular degree was replaced by 9122
            //-- following returned CS will be built with internal 9122 CS.
            case 9102 : {
                switch (axisNumber) {
                    case 0b0111 : { //-- [E,N]
                        epsgCSCode = "6424";
                        break;
                    }
                    case 0b0011 : { //-- [N,E]
                        epsgCSCode = "6422";
                        break;
                    }
                    default :
                }
                break;
            }
            // Angular_Arc_Minute = 9103
            case 9103 :
            // Angular_Arc_Second = 9104
            case 9104 : {
                break;
            }
            // Angular_Grad = 9105
            case 9105 : {
                switch (axisNumber) {
                    case 0b0111 : { //-- [E,N]
                        epsgCSCode = "6425";
                        break;
                    }
                    case 0b0011 : { //-- [N,E]
                        epsgCSCode = "6403";
                        break;
                    }
                    default :
                }
                break;
            }
            // Angular_Gon = 9106
            case 9106 :
            // Angular_DMS = 9107
            case 9107 :
            //-- Angular_DMS_Hemisphere = 9108
            case 9108 :
            default :
        }

        if (epsgCSCode == null)
            //-- epsg CS + Unit not found
            return (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(baseCS, fallBackUnit);

        epsgCSCode = "EPSG:".concat(epsgCSCode);

        return epsgFactory().createEllipsoidalCS(epsgCSCode);
    }


    ////////////////////////////////////////////////////////////////////////////
    //-------------------------- PROJECTED CRS -------------------------------//
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creating a {@linkplain CoordinateReferenceSystem Projected CRS} following the GeoTiff spec.<br>
     * As usual this method tries to follow the geotiff specification<br>
     * Needed tags are :
     * <ul>
     * <li> a code definition given by {@link GeoKeys.ProjectedCSTypeGeoKey} tag </li>
     * <li> a unit value given by {@link GeoKeys.ProjLinearUnitsGeoKey} </li>
     * <li> a unit key property given by {@link GeoKeys.ProjLinearUnitSizeGeoKey} </li>
     * </ul>
     *
     * @return a {@link CoordinateReferenceSystem} built using the provided {@link Unit}.
     * @throws FactoryException if problem during factory Projected CRS creation.
     * @throws DataStoreContentException if missing needed geokeys.
     */
    private CoordinateReferenceSystem createProjectedCRS()
            throws FactoryException, DataStoreContentException {

        final String projCode = getAsString(GeoKeys.ProjectedCSTypeGeoKey, false);

        //-- getting the linear unit used by this coordinate reference system.
        final Unit linearUnit = createUnit(GeoKeys.ProjLinearUnitsGeoKey,
                                GeoKeys.ProjLinearUnitSizeGeoKey, Units.METRE, Units.METRE);

        //--------------------------- USER DEFINE -----------------------------//
        //-- if it's user defined, we have to parse many informations and
        //-- try to build appropriate projected CRS from theses parsed informations.
        //-- like base gcrs, datum, unit ...
        if (projCode == null
         || projCode.equals(GeoKeys.GTUserDefinedGeoKey_String))
            return createUserDefinedProjectedCRS(linearUnit);
        //---------------------------------------------------------------------//

        //---------------------- EPSG CODE PERTINENCY -------------------------//
        //-- do a decode
        final StringBuffer epsgProjCode = new StringBuffer(projCode);
        if (!projCode.startsWith("EPSG") && !projCode.startsWith("epsg"))
            epsgProjCode.insert(0, "EPSG:");

        ProjectedCRS pcrs = epsgFactory().createProjectedCRS(epsgProjCode.toString());

        //-- if 'tiff defined unit' does not match with decoded Projected CRS, build another converted projected CRS.
        if (linearUnit != null && !linearUnit.equals(pcrs.getCoordinateSystem().getAxis(0).getUnit())) {
            //-- Creating a new projected CRS
            pcrs = objectFactory().createProjectedCRS(name(IdentifiedObjects.getName(pcrs, new DefaultCitation("EPSG"))),
                                                      (GeographicCRS) pcrs.getBaseCRS(),
                                                      pcrs.getConversionFromBase(),
                                                      retrieveCartesianCS(GeoKeys.ProjLinearUnitsGeoKey, pcrs.getCoordinateSystem(), linearUnit));
        }
        return pcrs;
    }

    /**
     * Creating a User Define {@linkplain CoordinateReferenceSystem Projected CRS} following the GeoTiff spec.<br>
     * As usual this method tries to follow the geotiff specification<br>
     * Needed tags are :
     * <ul>
     * <li> a name given by {@link GeoKeys.PCSCitationGeoKey} </li>
     * <li> a {@link CoordinateOperation} given by {@link GeoKeys.ProjectionGeoKey} </li>
     * <li> an {@link OperationMethod} given by {@link GeoKeys.ProjCoordTransGeoKey} </li>
     * </ul>
     *
     * @param linearUnit is the UoM that this {@link ProjectedCRS} will use. It could be {@code null}.
     *
     * @return a user-defined {@link ProjectedCRS}.
     * @throws DataStoreContentException if missing needed geoKey.
     * @throws FactoryException if problem during projected CRS factory build.
     */
    private ProjectedCRS createUserDefinedProjectedCRS(final Unit linearUnit)
            throws FactoryException, DataStoreContentException {
        //-- get projected CRS Name
        String projectedCrsName = getAsString(GeoKeys.PCSCitationGeoKey, false);
        if (projectedCrsName == null) {
            projectedCrsName = "User Defined unnamed ProjectedCRS";
        }
        //--------------------------------------------------------------------//
        //                   get the GEOGRAPHIC BASE CRS                      //
        //--------------------------------------------------------------------//
        final GeographicCRS gcs = createGeographicCRS();

        //-- get the projection code if exist
        final String projCode = getAsString(GeoKeys.ProjectionGeoKey, false);

        //-- is it user defined?
        final Conversion projection;
        if (projCode == null || projCode.equals(GeoKeys.GTUserDefinedGeoKey_String)) {

            //-- get Operation Method from proj key
            final String coordTrans               = getAsString(GeoKeys.ProjCoordTransGeoKey, true);
            final OperationMethod operationMethod = operationFactory().getOperationMethod(coordTrans);
            final ParameterValueGroup parameters  = operationMethod.getParameters().createValue();
            projection                            = operationFactory().createDefiningConversion(name(projectedCrsName), operationMethod, parameters);
        } else {
            projection = (Conversion) epsgFactory().createCoordinateOperation(String.valueOf(projCode));
        }

        CartesianCS predefineCartesianCS = epsgFactory().createCartesianCS("EPSG:4400");
        //-- manage unit if necessary
        if (linearUnit != null && !linearUnit.equals(Units.METRE))
            predefineCartesianCS = retrieveCartesianCS(GeoKeys.ProjLinearUnitsGeoKey, predefineCartesianCS, linearUnit);

        return objectFactory().createProjectedCRS(name(projectedCrsName), gcs, projection, predefineCartesianCS);
    }


    ////////////////////////////////////////////////////////////////////////////
    //-------------------------- GEOGRAPHIC CRS ------------------------------//
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Creating a {@linkplain CoordinateReferenceSystem Geographic CRS} following the GeoTiff spec.<br>
     * As usual this method tries to follow the geotiff specification<br>
     * Needed tags are :
     * <ul>
     * <li> a code definition given by {@link GeoKeys.GeographicTypeGeoKey} tag </li>
     * <li> a unit value given by {@link GeoKeys.GeogAngularUnitsGeoKey} </li>
     * <li> a unit key property given by {@link GeoKeys.GeogAngularUnitSizeGeoKey} </li>
     * </ul>
     * <br>
     * and for User Define Geographic CRS :
     * <ul>
     * <li> a citation given by {@link GeoKeys.GeogCitationGeoKey}</li>
     * <li> a datum definition geokeys </li>
     * </ul>
     *
     *
     * @param unit use for building this {@link CoordinateReferenceSystem}.
     * @return a {@link CoordinateReferenceSystem} built using the provided {@link Unit}.
     * @throws FactoryException if problem during factory Geographic CRS creation.
     * @throws DataStoreContentException if missing needed geokeys.
     * @return built Geographic CRS.
     */
    private GeographicCRS createGeographicCRS()
            throws FactoryException, DataStoreContentException {

        //-- Get the crs code
        final String tempCode = getAsString(GeoKeys.GeographicTypeGeoKey, false);
        //-- Angular units used in this geotiff image
        Unit angularUnit = createUnit(GeoKeys.GeogAngularUnitsGeoKey,
                    GeoKeys.GeogAngularUnitSizeGeoKey, Units.RADIAN,
                    Units.DEGREE);
        //-- Geographic CRS is "UserDefine", we have to parse many informations from other geokeys.
        if (tempCode == null || tempCode.equals(GeoKeys.GTUserDefinedGeoKey_String)) {

            //-- linear unit
            final Unit linearUnit = createUnit(GeoKeys.GeogLinearUnitsGeoKey,
                                    GeoKeys.GeogLinearUnitSizeGeoKey, Units.METRE,
                                    Units.METRE);

            ///-- Geographic CRS given name from tiff tag (GeogCitationGeoKey)
            String name = getAsString(GeoKeys.GeogCitationGeoKey, false);
            if (name == null) name = "User Define Geographic CRS";

            final GeodeticDatum datum = createGeodeticDatum(linearUnit);
            //-- make the user defined GCS from all the components...
            return objectFactory().createGeographicCRS(name(name),
                                                       datum,
                                                       retrieveEllipsoidalCS(GeoKeys.GeogAngularUnitsGeoKey,
                                                               CommonCRS.defaultGeographic().getCoordinateSystem(),
                                                               angularUnit));
//                                       (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(CommonCRS.defaultGeographic().getCoordinateSystem(),
//                                                                                                       angularUnit));
        }

        //---------------------------------------------------------------------//
        // If it's not user defined, just use the EPSG factory to create
        // the coordinate system but check if the user specified a
        // different angular unit. In this case we need to create a
        // user-defined GCRS.
        //---------------------------------------------------------------------//
        final StringBuffer geogCode = new StringBuffer(tempCode);
        if (!tempCode.startsWith("EPSG") && !tempCode.startsWith("epsg"))
            geogCode.insert(0, "EPSG:");

        CoordinateReferenceSystem geoCRS = CRS.forCode(geogCode.toString());
        //-- all CRS must be Geodetic
        if (!(geoCRS instanceof GeodeticCRS))
            throw new IllegalArgumentException("Impossible to define CRS from none Geodetic base. found : "+geoCRS.toWKT());

        if (!(geoCRS instanceof GeographicCRS)) {
            warning(Resources.Keys.UnexpectedGeoCRS_1, reader.input.filename);
            geoCRS = objectFactory().createGeographicCRS(name(IdentifiedObjects.getName(geoCRS, new DefaultCitation("EPSG"))),
                                                        ((GeodeticCRS)geoCRS).getDatum(),
                                                        CommonCRS.defaultGeographic().getCoordinateSystem());
        }
        //-- in case where tiff define unit does not match
        if (angularUnit != null
        && !angularUnit.equals(geoCRS.getCoordinateSystem().getAxis(0).getUnit())) {
            geoCRS = objectFactory().createGeographicCRS(name(IdentifiedObjects.getName(geoCRS, new DefaultCitation("EPSG"))),
                                                        (GeodeticDatum) ((GeographicCRS)geoCRS).getDatum(),
                                                        retrieveEllipsoidalCS(GeoKeys.GeogAngularUnitsGeoKey,
                                                                CommonCRS.defaultGeographic().getCoordinateSystem(),
                                                                angularUnit));
//                    (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(CommonCRS.defaultGeographic().getCoordinateSystem(), angularUnit));
        }
        return (GeographicCRS) geoCRS;
    }

    //------------------------------- GEOCENTRIQUE -----------------------------
    /**
     * Not implemented yet.
     * @return nothing
     * @throws IllegalStateException not implemented.
     */
    private CoordinateReferenceSystem createGeocentricCRS() {
        throw new IllegalStateException("GeocentricCRS : Not implemented yet.");
    }

    //------------------------------------------------------------------------------
    //                      TODO SEE LATER PARTICULARITY CASE CRS WITH
    //                      MERCATOR1SP AND MERCATOR2SP
    //                      POLAR STEREOGRAPHIC VARIANT A B AND C
    //------------------------------------------------------------------------------
    /**
     * Set the projection parameters basing its decision on the projection name.
     * I found a complete list of projections on the geotiff website at address
     * http://www.remotesensing.org/geotiff/proj_list.
     *
     * I had no time to implement support for all of them therefore you will not
     * find all of them. If you want go ahead and add support for the missing
     * ones. I have tested this code against some geotiff files you can find on
     * the geotiff website under the ftp sample directory but I can say that
     * they are a real mess! I am respecting the specification strictly while
     * many of those fields do not! I could make this method trickier and use
     * workarounds in order to be less strict but I will not do this, since I
     * believe it is may lead us just on a very dangerous path.
     *
     *
     * @param name
     * @param metadata to use fo building this {@link ParameterValueGroup}.
     * @param coordTrans
     *            a {@link ParameterValueGroup} that can be used to trigger this
     *            projection.
     *
     * @return
     * @throws GeoTiffException
     */
    private ParameterValueGroup setParametersForProjection(final String name, final String coordTransCode)
            throws NoSuchIdentifierException, DataStoreContentException {

        if (name == null && coordTransCode == null)
            throw new DataStoreContentException("bla bla bla");

        final String projName = (name == null)
                                ? GeoKeys.getName(Short.parseShort(coordTransCode))
                                : name;

        final ParameterValueGroup parameters = null;//mtFactory.getDefaultParameters(projName);

        //-- particularity cases
//        for (short key : geoKeys.keySet()) {
//            if (GeoKeys.contain(key)) {
//                String keyName = GeoKeys.getName(key);
//                keyName = keyName.substring(4, keyName.length());
//                parameters.parameter(keyName).setValue(getAsString(key));
//            }
//        }

        //-- maybe particularity case
//            /**
//             * Mercator_1SP
//             * Mercator_2SP
//             */
//            if (name.equalsIgnoreCase("mercator_1SP")
//                    || name.equalsIgnoreCase("Mercator_2SP")
//                    || code == CT_Mercator) {
//
//                final double standard_parallel_1 = metadata.getAsDouble(ProjStdParallel1GeoKey);
//                boolean isMercator2SP = false;
//                if (!Double.isNaN(standard_parallel_1)) {
//                    parameters = mtFactory.getDefaultParameters("Mercator_2SP");
//                    isMercator2SP = true;
//                } else {
//                    parameters = mtFactory.getDefaultParameters("Mercator_1SP");
//                }
//
//                parameters.parameter(code(Mercator1SP.LONGITUDE_OF_ORIGIN)).setValue(getOriginLong(metadata));
//                parameters.parameter(code(Mercator1SP.LATITUDE_OF_ORIGIN)).setValue(getOriginLat(metadata));
//                parameters.parameter(code(Mercator2SP.FALSE_EASTING)).setValue(getFalseEasting(metadata));
//                parameters.parameter(code(Mercator2SP.FALSE_NORTHING)).setValue(getFalseNorthing(metadata));
//                if (isMercator2SP) {
//                    parameters.parameter(code(Mercator2SP.STANDARD_PARALLEL)).setValue(standard_parallel_1);
//                } else {
//                    parameters.parameter(code(Mercator1SP.SCALE_FACTOR)).setValue(getScaleFactor(metadata));
//                }
//                return parameters;
//            }
//
//            /**
//             * POLAR_STEREOGRAPHIC variant A B and C
//             */
//            if (code == CT_PolarStereographic) {
//
//                /**
//                 * They exist 3 kind of polar StereoGraphic projections,define the case
//                 * relative to existing needed attributs
//                 */
//                //-- set the mutual projection attributs
//                //-- all polar stereographic formulas share LONGITUDE_OF_ORIGIN
//                final double longitudeOfOrigin = metadata.getAsDouble(ProjStraightVertPoleLongGeoKey);
//
//                /*
//                * For polar Stereographic variant A only latitudeOfNaturalOrigin expected values are {-90; +90}.
//                * In some case, standard parallele is stipulate into latitudeOfNaturalOrigin tiff tag by error.
//                * To avoid CRS problem creation, try to anticipe this comportement by switch latitudeOfNaturalOrigin into standard parallele.
//                * HACK FOR USGS LANDSAT 8 difference between geotiff tag and Landsat 8 metadata MTL.txt file.
//                */
//                double standardParallel                 = metadata.getAsDouble(ProjStdParallel1GeoKey);
//                final double latitudeOfNaturalOrigin    = metadata.getAsDouble(ProjNatOriginLatGeoKey);
//                final boolean isVariantALatitudeConform = (Math.abs(Latitude.MAX_VALUE - Math.abs(latitudeOfNaturalOrigin)) <  Formulas.ANGULAR_TOLERANCE);
//
//                if (!isVariantALatitudeConform && Double.isNaN(standardParallel)) {
//                    LOGGER.log(Level.WARNING, "The latitudeOfNaturalOrigin for Polar Stereographic variant A is not conform.\n"
//                            + "Expected values are {-90; +90}, found : "+latitudeOfNaturalOrigin+"\n"
//                            + "Switch latitudeOfNaturalOrigin by Latitude of standard parallel to try building of Polar Stereographic Variant B or C.");
//                    standardParallel = latitudeOfNaturalOrigin;
//                }
//
//                if (Double.isNaN(standardParallel)) {
//                    //-- no standard parallele : PolarStereoGraphic VARIANT A
//                    final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                    .getOperationMethod("Polar Stereographic (variant A)");
//
//                    parameters = method.getParameters().createValue();
//                    parameters.parameter(code(PolarStereographicA.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                    parameters.parameter(code(PolarStereographicA.LATITUDE_OF_ORIGIN)).setValue(latitudeOfNaturalOrigin);
//                    parameters.parameter(code(PolarStereographicA.SCALE_FACTOR)).setValue(metadata.getAsDouble(ProjScaleAtNatOriginGeoKey));
//                    parameters.parameter(code(PolarStereographicA.FALSE_EASTING)).setValue(metadata.getAsDouble(ProjFalseEastingGeoKey));
//                    parameters.parameter(code(PolarStereographicA.FALSE_NORTHING)).setValue(metadata.getAsDouble(ProjFalseNorthingGeoKey));
//
//                } else {
//
//                    //-- Variant B and C share STANDARD_PARALLEL
//
//                    final double falseOriginEasting = metadata.getAsDouble(ProjFalseOriginEastingGeoKey);
//                    if (Double.isNaN(falseOriginEasting)) {
//                        //-- no false Origin Easting : PolarStereoGraphic VARIANT B
//                        final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                              .getOperationMethod("Polar Stereographic (variant B)");
//
//                        parameters = method.getParameters().createValue();
//                        parameters.parameter(code(PolarStereographicB.STANDARD_PARALLEL)).setValue(standardParallel);
//                        parameters.parameter(code(PolarStereographicB.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                        parameters.parameter(code(PolarStereographicB.FALSE_EASTING)).setValue(metadata.getAsDouble(ProjFalseEastingGeoKey));
//                        parameters.parameter(code(PolarStereographicB.FALSE_NORTHING)).setValue(metadata.getAsDouble(ProjFalseNorthingGeoKey));
//                    } else {
//                        //-- PolarStereoGraphic VARIANT C
//                        final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                              .getOperationMethod("Polar Stereographic (variant C)");
//
//                        parameters = method.getParameters().createValue();
//                        parameters.parameter(code(PolarStereographicB.STANDARD_PARALLEL)).setValue(standardParallel);
//                        parameters.parameter(code(PolarStereographicB.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                        parameters.parameter(code(PolarStereographicC.EASTING_AT_FALSE_ORIGIN)).setValue(metadata.getAsDouble(ProjFalseOriginEastingGeoKey));
//                        parameters.parameter(code(PolarStereographicC.NORTHING_AT_FALSE_ORIGIN)).setValue(metadata.getAsDouble(ProjFalseNorthingGeoKey));
//                    }
//                }
//            }

        return parameters;
    }

    @Override
    public final String toString() {
        final StringBuilder strBuild = new StringBuilder("GeoKeys for CoordinateReferenceSystem")
                .append('\n')
                .append('\n')
                .append("geo keys revision: ").append(majorRevision)
                .append('\n')
                .append("minor revision: ").append(minorRevision)
                .append('\n')
                .append('\n');

        for (Map.Entry<Short,Object> entry : geoKeys.entrySet()) {
            final short key = entry.getKey();
            strBuild.append(GeoKeys.getName(key)).append(" (").append(key).append(") = ").append(entry.getValue());
            strBuild.append('\n');
        }
        return strBuild.toString();
    }
}
