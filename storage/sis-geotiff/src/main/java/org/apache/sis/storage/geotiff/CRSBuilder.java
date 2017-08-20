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

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.NoSuchElementException;
import java.lang.reflect.Array;
import java.io.IOException;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.UnitConverter;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.referencing.CoordinateOperations;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Utilities;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.iso.DefaultNameSpace;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.Debug;

import static org.apache.sis.util.Utilities.equalsIgnoreMetadata;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;


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
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see GeoKeys
 *
 * @since 0.8
 * @module
 */
final class CRSBuilder {
    /**
     * Number of {@code short} values in each GeoKey entry.
     */
    private static final int ENTRY_LENGTH = 4;

    /**
     * The character used as a separator in {@link String} multi-values.
     */
    private static final char SEPARATOR = '|';

    /**
     * Index where to store the name of the geodetic CRS, the datum, the ellipsoid and the prime meridian.
     * The GeoTIFF specification has only one key, {@link GeoKeys#GeogCitation}, for the geographic CRS and
     * its components. But some GeoTIFF files encode the names of all components in the value associated to
     * that key, as in the following example:
     *
     * {@preformat text
     *   GCS Name = wgs84|Datum = unknown|Ellipsoid = WGS_1984|Primem = Greenwich|
     * }
     *
     * In such case, we will split the name into the components names to be stored in an array at indices
     * given by {@code GCRS}, {@code DATUM}, {@code ELLIPSOID} and {@code PRIMEM}.
     */
    static final int PRIMEM = 0, ELLIPSOID = 1, DATUM = 2, GCRS = 3;
    // PRIMEM must be first and GCRS must be last.

    /**
     * Keys that may be used in the value associated to {@link GeoKeys#GeogCitation}.
     * For each element in this array at index {@code i}, the {@code i/2} value is equal to the
     * {@code DATUM}, {@code ELLIPSOID} or {@code PRIMEM} constant for the corresponding type.
     */
    private static final String[] NAME_KEYS = {
        // WKT 1               WKT 2
        WKTKeywords.PrimeM,    WKTKeywords.PrimeMeridian,
        WKTKeywords.Spheroid,  WKTKeywords.Ellipsoid,
        WKTKeywords.Datum,     WKTKeywords.GeodeticDatum
    };

    /**
     * Minimal length that a key in a name must have before we compare them to the {@link #NAME_KEYS}.
     * For example a value of 5 means that {@link #splitName(String)} will accept {@code "Ellip"},
     * {@code "Ellips"}, {@code "Ellipso"} and {@code "Ellipsoi"} as if they were {@code "Ellipsoid"}.
     * This length shall not be greater than the length of the shortest string in {@link #NAME_KEYS}.
     */
    private static final int MIN_KEY_LENGTH = 5;

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
     * Each value shall be used at most once. This allow us to remove value after usage,
     * so we can easily detect at the end of the parsing process which GeoTIFF keys were
     * unrecognized or ignored.
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
    private DefaultCoordinateOperationFactory operationFactory;

    /**
     * Name of the last object created. This is used by {@link #properties(Object)} for reusing existing instance
     * if possible. This is useful in GeoTIFF files since the same name is used for different geodetic components,
     * for example the datum and the ellipsoid.
     */
    private Identifier lastName;

    /**
     * {@code true} when an exception has been thrown but this {@code CRSBuilder} already reported a warning,
     * so there is no need for the caller to report a warning again. {@code CRSBuilder} sometime reports warnings
     * itself when it can provide a better warning message than what the caller can do.
     */
    boolean alreadyReported;

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
    private DefaultCoordinateOperationFactory operationFactory() {
        if (operationFactory == null) {
            operationFactory = CoordinateOperations.factory();
        }
        return operationFactory;
    }

    /**
     * Returns a map with the given name associated to {@value org.opengis.referencing.IdentifiedObject#NAME_KEY}.
     * The given name shall be either an instance of {@link String} or {@link Identifier}.
     * This is an helper method for creating geodetic objects with {@link #objectFactory}.
     */
    private Map<String,?> properties(Object name) {
        if (name == null) {
            name = NilReferencingObject.UNNAMED;
        } else if (lastName != null && lastName.getCode().equals(name)) {
            name = lastName;
        }
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Removes and returns the value for the given key as a singleton (not an array).
     * If the value was an array, a warning is reported and the first element is returned.
     *
     * <p>The given element is removed from the map so that each element is used only once
     * (for example it would be redundant to have the {@code verify(…)} methods to compare
     * the same values than the ones we used at geodetic object construction time). It also
     * allow us to check which {@link GeoKeys} were ignored by looking at what is remaining
     * in the map after CRS creation.</p>
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return the singleton value for the given key, or {@code null} if none.
     */
    private Object getSingleton(final short key) {
        Object value = geoKeys.remove(key);
        if (value != null && value.getClass().isArray()) {
            warning(Resources.Keys.UnexpectedListOfValues_2, GeoKeys.name(key), Array.getLength(value));
            value = Array.get(value, 0);    // No need to verify length because we do not store empty arrays.
        }
        return value;
    }

    /**
     * Removes and returns a {@link GeoKeys} value as a character string, or {@code null} if none.
     * Value for the given key should be a sequence of characters. If it is one or more numbers instead,
     * then this method formats those numbers in a comma-separated list. Such sequence of numbers would
     * be unusual, but we do see strange GeoTIFF files in practice.
     *
     * <p>See {@link #getSingleton(short)} for a discussion about why the value is removed from the map.</p>
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return a string representation of the value for the given key, or {@code null} if the key was not found.
     */
    private String getAsString(final short key) {
        Object value = geoKeys.remove(key);
        if (value != null) {
            if (value.getClass().isArray()) {
                final int length = Array.getLength(value);
                final StringBuffer buffer = new StringBuffer();
                for (int i=0; i<length; i++) {
                    if (buffer.length() != 0) {
                        buffer.append(", ");
                    }
                    buffer.append(Array.get(value, i));
                }
                value = buffer;
            }
            return value.toString();
        }
        return null;
    }

    /**
     * Removes and returns a {@link GeoKeys} value as an integer. This is used for fetching enumeration values.
     * The value returned by this method is typically one of the {@link GeoCodes} values.
     *
     * <p>See {@link #getSingleton(short)} for a discussion about why the value is removed from the map.</p>
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return the integer value for the given key, or {@link GeoCodes#undefined} if the key was not found.
     * @throws NumberFormatException if the value was stored as a string and can not be parsed.
     */
    private int getAsInteger(final short key) {
        final Object value = getSingleton(key);
        if (value == null) {
            return GeoCodes.undefined;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            invalidValue(key, value);
            alreadyReported = true;
            throw e;
        }
    }

    /**
     * Removes and returns a {@link GeoKeys} value as a floating point number, or {@code NaN} if none.
     * See {@link #getSingleton(short)} for a discussion about why the value is removed from the map.
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return the floating point value for the given key, or {@link Double#NaN} if the key was not found.
     * @throws NumberFormatException if the value was stored as a string and can not be parsed.
     */
    private double getAsDouble(final short key) {
        final Object value = getSingleton(key);
        if (value == null) {
            return Double.NaN;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            invalidValue(key, value);
            alreadyReported = true;
            throw e;
        }
    }

    /**
     * Removes and returns a {@link GeoKeys} value as a character string, or throws an exception if none.
     * A warning is reported before to throw the exception. There is no attempt to provide a good message
     * in the exception since is should be caught by {@link ImageFileDirectory}.
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return a string representation of the value for the given key.
     * @throws NoSuchElementException if no value has been found.
     */
    private String getMandatoryString(final short key) {
        final String value = getAsString(key);
        if (value != null) {
            return value;
        }
        alreadyReported = true;
        throw new NoSuchElementException(missingValue(key));
    }

    /**
     * Removes and returns a {@link GeoKeys} value as a floating point number, or throws an exception if none.
     * A warning is reported before to throw the exception. There is no attempt to provide a good message in
     * the exception since is should be caught by {@link ImageFileDirectory}.
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return the floating point value for the given key.
     * @throws NoSuchElementException if no value has been found.
     * @throws NumberFormatException if the value was stored as a string and can not be parsed.
     */
    private double getMandatoryDouble(final short key) {
        final double value = getAsDouble(key);
        if (!Double.isNaN(value)) {
            return value;
        }
        alreadyReported = true;
        throw new NoSuchElementException(missingValue(key));
    }

    /**
     * Reports a warning about missing value for the given key. The key name is opportunistically returned for
     * building the {@link NoSuchElementException} message, but it is not the main purpose of this method.
     */
    private String missingValue(final short key) {
        final String name = GeoKeys.name(key);
        warning(Resources.Keys.MissingGeoValue_1, name);
        return name;
    }

    /**
     * Reports a warning about an invalid value for the given key.
     */
    private void invalidValue(final short key, final Object value) {
        warning(Resources.Keys.InvalidGeoValue_2, GeoKeys.name(key), value);
    }

    /**
     * Verifies that a value found in the GeoTIFF file is approximatively equal to the expected value.
     * This method is invoked when a CRS component is defined both explicitely and by EPSG code,
     * in which case we expect the given value to be equal to the value fetched from the EPSG database.
     * If the values do not match, a warning is reported and the caller should use the EPSG value.
     *
     * @param  epsg      the EPSG object, to be used for formatting the warning in case of mismatched values.
     * @param  expected  the expected value for an object identified by the {@code epsg} code.
     * @param  key       the GeoTIFF key of the user-defined value to compare with the expected one.
     * @param  unit      the unit of measurement for {@code expected} and {@code actual}, or {@code null} if none.
     */
    private void verify(final IdentifiedObject epsg, final double expected, final short key, final Unit<?> unit) {
        final double actual = getAsDouble(key);         // May be NaN.
        if (Math.abs(expected - actual) > expected * Numerics.COMPARISON_THRESHOLD) {
            String symbol = "";
            if (unit != null) {
                symbol = unit.toString();
                if (!symbol.isEmpty() && Character.isLetterOrDigit(symbol.codePointAt(0))) {
                    symbol = ' ' + symbol;    // Add a space before "m" but not before "°".
                }
            }
            /*
             * Use Double.toString(…) instead of NumberFormat because the later does not show
             * enough significant digits for parameters like inverse flattening.
             */
            warning(Resources.Keys.NotTheEpsgValue_5, IdentifiedObjects.getIdentifierOrName(epsg),
                    String.valueOf(expected), GeoKeys.name(key), String.valueOf(actual), symbol);
        }
    }

    /**
     * Verifies that the EPSG code found in the GeoTIFF file is equal to the expected value.
     * This method is invoked when a CRS component is defined by an EPSG code, in which case
     * there is no need to specify the EPSG codes of the components, but the file still supply
     * those EPSG codes. If the values do not match, a warning is reported.
     *
     * @param  parent  the parent which contains the {@code epsg} object
     * @param  epsg    the object created from the EPSG geodetic dataset.
     * @param  key     the GeoTIFF key for the EPSG code of the given {@code epsg} object.
     */
    private void verifyIdentifier(final IdentifiedObject parent, final IdentifiedObject epsg, final short key) {
        final int code = getAsInteger(key);
        if (code > GeoCodes.undefined && code < GeoCodes.userDefined) {
            final Identifier id = IdentifiedObjects.getIdentifier(epsg, Citations.EPSG);
            if (id != null) {
                final int expected;
                try {
                    expected = Integer.parseInt(id.getCode());
                } catch (NumberFormatException e) {
                    reader.owner.warning(null, e);                  // Should not happen.
                    return;
                }
                if (code != expected) {
                    warning(Resources.Keys.NotTheEpsgValue_5, IdentifiedObjects.getIdentifierOrName(parent),
                            Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + expected, GeoKeys.name(key),
                            Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + code, "");
                }
            }
        }
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                                 GeoKeys parsing                                  ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Decodes all the given GeoTIFF keys, then creates a coordinate reference system.
     * An overview of the key directory structure is given in {@linkplain CRSBuilder class javadoc}.
     *
     * @param  keyDirectory       the GeoTIFF keys to be associated to values. Can not be null.
     * @param  numericParameters  a vector of {@code double} parameters, or {@code null} if none.
     * @param  asciiParameters    the sequence of characters from which to build strings, or {@code null} if none.
     * @return the coordinate reference system created from the given GeoTIFF keys.
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    @SuppressWarnings("null")
    final CoordinateReferenceSystem build(final Vector keyDirectory, final Vector numericParameters, final String asciiParameters)
            throws FactoryException
    {
        final int numberOfKeys;
        final int directoryLength = keyDirectory.size();
        if (directoryLength >= ENTRY_LENGTH) {
            final int version = keyDirectory.intValue(0);
            if (version != 1) {
                warning(Resources.Keys.UnsupportedGeoKeyDirectory_1, version);
                return null;
            }
            majorRevision = keyDirectory.shortValue(1);
            minorRevision = keyDirectory.shortValue(2);
            numberOfKeys  = keyDirectory.intValue(3);
        } else {
            numberOfKeys = 0;
        }
        /*
         * The key directory may be longer than needed for the amount of keys, but not shorter.
         * If shorter, report a warning and stop the parsing since we have no way to know if the
         * missing information were essentiel or not.
         *
         *     (number of key + head) * 4    ---    1 entry = 4 short values.
         */
        final int expectedLength = (numberOfKeys + 1) * ENTRY_LENGTH;
        if (directoryLength < expectedLength) {
            warning(Resources.Keys.ListTooShort_3, "GeoKeyDirectory", expectedLength, directoryLength);
            return null;
        }
        final int numberOfDoubles = (numericParameters != null) ? numericParameters.size() : 0;
        final int numberOfChars   =   (asciiParameters != null) ? asciiParameters.length() : 0;
        /*
         * Now iterate over all GeoKey values. The values are copied in a HashMap for convenience,
         * because the CRS creation may use them out of order.
         */
        for (int i=1; i <= numberOfKeys; i++) {
            final int p = i * ENTRY_LENGTH;
            final short key       = keyDirectory.shortValue(p);
            final int tagLocation = keyDirectory.intValue(p+1);
            final int count       = keyDirectory.intValue(p+2);
            final int valueOffset = keyDirectory.intValue(p+3);
            if (valueOffset < 0 || count < 0) {
                missingValue(key);
                continue;
            }
            final Object value;
            switch (tagLocation) {
                /*
                 * tagLocation == 0 means that 'valueOffset' actually contains the value,
                 * thus avoiding the need to allocate a separated storage location for it.
                 * The count should be 1.
                 */
                case 0: {
                    switch (count) {
                        case 0:  continue;
                        case 1:  break;          // Expected value.
                        default: warning(Resources.Keys.UnexpectedListOfValues_2, GeoKeys.name(key), count); break;
                    }
                    value = valueOffset;
                    break;
                }
                /*
                 * Values of type 'short' are stored in the same vector than the key directory;
                 * the specification does not allocate a separated vector for them. We use the
                 * 'int' type if needed for allowing storage of unsigned short values.
                 */
                case Tags.GeoKeyDirectory & 0xFFFF: {
                    if (valueOffset + count > keyDirectory.size()) {
                        missingValue(key);
                        continue;
                    }
                    switch (count) {
                        case 0:  continue;
                        case 1:  value = keyDirectory.get(valueOffset); break;
                        default: final int[] array = new int[count];
                                 for (int j=0; j<count; j++) {
                                     array[j] = keyDirectory.intValue(valueOffset + j);
                                 }
                                 value = array;
                                 break;
                    }
                    break;
                }
                /*
                 * Values of type 'double' are read from a separated vector, 'numericParameters'.
                 * Result is stored in a Double wrapper or in an array of type 'double[]'.
                 */
                case Tags.GeoDoubleParams & 0xFFFF: {
                    if (valueOffset + count > numberOfDoubles) {
                        missingValue(key);
                        continue;
                    }
                    switch (count) {
                        case 0:  continue;
                        case 1:  value = numericParameters.get(valueOffset); break;
                        default: final double[] array = new double[count];
                                 for (int j=0; j<count; j++) {
                                     array[j] = numericParameters.doubleValue(valueOffset + j);
                                 }
                                 value = array;
                                 break;
                    }
                    break;
                }
                /*
                 * ASCII encoding use the pipe ('|') character as a replacement for the NUL character
                 * used in C/C++ programming languages. We need to omit those trailing characters.
                 */
                case Tags.GeoAsciiParams & 0xFFFF: {
                    int upper = valueOffset + count;
                    if (upper > numberOfChars) {
                        missingValue(key);
                        continue;
                    }
                    upper = CharSequences.skipTrailingWhitespaces(asciiParameters, valueOffset, upper);
                    while (upper > valueOffset && asciiParameters.charAt(upper - 1) == SEPARATOR) {
                        upper--;    // Skip trailing pipe, interpreted as C/C++ NUL character.
                    }
                    // Use String.trim() for skipping C/C++ NUL character in addition of whitespaces.
                    final String s = asciiParameters.substring(valueOffset, upper).trim();
                    if (s.isEmpty()) continue;
                    value = s;
                    break;
                }
                /*
                 * GeoKeys are not expected to use other storage mechanism. If this happen anyway, report a warning
                 * and continue on the assumption that if the value that we are ignoring was critical information,
                 * it would have be stored in one of the standard GeoTIFF tags.
                 */
                default: {
                    warning(Resources.Keys.UnsupportedGeoKeyStorage_1, GeoKeys.name(key));
                    continue;
                }
            }
            geoKeys.put(key, value);
        }
        /*
         * At this point we finished copying all GeoTIFF keys in 'CRSBuilder.geoKeys' map.
         * First create the main coordinate reference system, as determined by 'ModelType'.
         * Then if a vertical CRS exists and the main CRS is not geocentric (in which case
         * adding a vertical CRS would make no sense), create a three-dimensional compound CRS.
         */
        CoordinateReferenceSystem crs;
        final int crsType = getAsInteger(GeoKeys.ModelType);
        switch (crsType) {
            case GeoCodes.undefined:           return null;
            case GeoCodes.ModelTypeProjected:  crs = createProjectedCRS(); break;
            case GeoCodes.ModelTypeGeocentric: return createGeocentricCRS();        // Ignore vertical CRS.
            case GeoCodes.ModelTypeGeographic: {
                crs = createGeographicCRS(true,
                        createUnit(GeoKeys.AngularUnits, GeoKeys.AngularUnitSize, Angle.class, Units.DEGREE));
                break;
            }
            default: {
                warning(Resources.Keys.UnsupportedCoordinateSystemKind_1, crsType);
                return null;
            }
        }
        final VerticalCRS vertical = createVerticalCRS();
        if (vertical != null) {
            crs = objectFactory().createCompoundCRS(Collections.singletonMap(IdentifiedObject.NAME_KEY, crs.getName()), crs, vertical);
        }
        return crs;
    }

    /**
     * Completes ISO 19115 metadata with some GeoTIFF values that are for documentation purposes.
     * Those values do not participate directly to the construction of the Coordinate Reference System objects.
     *
     * <p><b>Pre-requite:</b></p>
     * <ul>
     *   <li>{@link #build(Vector, Vector, String)} must have been invoked before this method.</li>
     *   <li>{@link ImageFileDirectory} must have filled its part of metadata before to invoke this method.</li>
     *   <li>{@link MetadataBuilder#newGridRepresentation(MetadataBuilder.GridType)} should have been invoked
     *       with the appropriate {@code GEORECTIFIED} or {@code GEOREFERENCEABLE} type.</li>
     * </ul>
     *
     * After execution, this method emits a warning for unprocessed GeoTIFF tags.
     *
     * @param  metadata  the helper class where to write metadata values.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     */
    final void complete(final MetadataBuilder metadata) {
        /*
         * ASCII reference to published documentation on the overall configuration of the GeoTIFF file.
         * Often the projected CRS name, despite GeoKeys.PCSCitation being already for that purpose.
         * Checked first because this code is unlikely to throw an exception, while other parsings may
         * interrupt this method with an exception.
         */
        final String title = getAsString(GeoKeys.Citation);
        if (title != null) {
            metadata.setGridToCRS(title);
        }
        /*
         * Whether the pixel value is thought of as filling the cell area or is considered as point measurements at
         * the vertices of the grid (not in the interior of a cell).  This is determined by the value associated to
         * GeoKeys.RasterType, which can be GeoCodes.RasterPixelIsArea or GeoCodes.RasterPixelIsPoint.
         */
        CellGeometry     cg = null;
        PixelOrientation po = null;
        int code = getAsInteger(GeoKeys.RasterType);
        switch (code) {
            case GeoCodes.undefined: break;
            case GeoCodes.RasterPixelIsArea:  cg = CellGeometry.AREA;  po = PixelOrientation.CENTER;     break;
            case GeoCodes.RasterPixelIsPoint: cg = CellGeometry.POINT; po = PixelOrientation.UPPER_LEFT; break;
            default: invalidValue(GeoKeys.RasterType, code); break;
        }
        metadata.setCellGeometry(cg);
        metadata.setPointInPixel(po);
        /*
         * Build a list of remaining GeoKeys.
         */
        if (!geoKeys.isEmpty()) {
            final StringBuilder joiner = new StringBuilder();
            for (final short key : remainingKeys()) {
                if (joiner.length() != 0) {
                    joiner.append(", ");
                }
                joiner.append(GeoKeys.name(key));
            }
            warning(Resources.Keys.IgnoredGeoKeys_1, joiner.toString());
        }
    }

    /**
     * Returns all remaining keys, sorted in increasing order.
     */
    private Short[] remainingKeys() {
        final Short[] keys = geoKeys.keySet().toArray(new Short[geoKeys.size()]);
        Arrays.sort(keys);
        return keys;
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                   Geodetic components (datum, ellipsoid, etc.)                   ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a coordinate system (CS) with the same axis directions than the given CS but potentially different units.
     * If a coordinate system exists in the EPSG database with the requested characteristics, that CS will be returned
     * in order to have a richer set of metadata (name, minimal and maximal values, <i>etc</i>). Otherwise an CS with
     * an arbitrary name will be returned.
     *
     * @see CoordinateSystems#replaceLinearUnit(CoordinateSystem, Unit)
     */
    private CartesianCS replaceLinearUnit(final CartesianCS cs, final Unit<Length> unit) throws FactoryException {
        final Integer epsg = CoordinateSystems.getEpsgCode(unit, CoordinateSystems.getAxisDirections(cs));
        if (epsg != null) try {
            return epsgFactory().createCartesianCS(epsg.toString());
        } catch (NoSuchAuthorityCodeException e) {
            reader.owner.warning(null, e);
        }
        return (CartesianCS) CoordinateSystems.replaceLinearUnit(cs, unit);
    }

    /**
     * Returns a coordinate system (CS) with the same axis directions than the given CS but potentially different units.
     * If a coordinate system exists in the EPSG database with the requested characteristics, that CS will be returned
     * in order to have a richer set of metadata (name, minimal and maximal values, <i>etc</i>). Otherwise an CS with
     * an arbitrary name will be returned.
     *
     * @see CoordinateSystems#replaceAngularUnit(CoordinateSystem, Unit)
     */
    private EllipsoidalCS replaceAngularUnit(final EllipsoidalCS cs, final Unit<Angle> unit) throws FactoryException {
        final Integer epsg = CoordinateSystems.getEpsgCode(unit, CoordinateSystems.getAxisDirections(cs));
        if (epsg != null) try {
            return epsgFactory().createEllipsoidalCS(epsg.toString());
        } catch (NoSuchAuthorityCodeException e) {
            reader.owner.warning(null, e);
        }
        return (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(cs, unit);
    }

    /**
     * Creates units of measurement for projected or geographic coordinate reference systems.
     * The units may either be specified as a standard EPSG recognized unit, or may be user defined.
     * If the first case (EPSG code), the {@code keyUser} is ignored. In the second case (user-defined),
     * the unit of measurement is defined by a conversion factor from metre or radian base unit.
     *
     * @param  codeKey       the {@link GeoKeys} for a unit of measurement defined by an EPSG code.
     * @param  scaleKey      the {@link GeoKeys} for a unit of measurement defined by a scale applied on a base unit.
     * @param  quantity      {@link Length} for a linear unit, or {@link Angle} for an angular unit.
     * @param  defaultValue  the unit of measurement to return if no value is found in the GeoTIFF file.
     * @return the unit of measurement associated to the given {@link GeoKeys}, or the default value.
     *
     * @throws NoSuchElementException if {@code keyEPSG} value is {@link GeoCodes#userDefined} and no value is associated to {@code keyUser}.
     * @throws NumberFormatException  if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException     if the unit of measurement identified by the EPSG code is not of the expected quantity.
     */
    private <Q extends Quantity<Q>> Unit<Q> createUnit(final short codeKey, final short scaleKey,
            final Class<Q> quantity, final Unit<Q> defaultValue) throws FactoryException
    {
        final int epsg = getAsInteger(codeKey);
        switch (epsg) {
            case GeoCodes.undefined: {
                return defaultValue;
            }
            case GeoCodes.userDefined: {
                if (scaleKey == 0) return defaultValue;
                return defaultValue.getSystemUnit().multiply(getMandatoryDouble(scaleKey));
            }
            default: {
                /*
                 * Unit defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also provide the scale value, we will verify that the value
                 * is consistent with what we would expect for a unit of the given EPSG code.
                 */
                final Unit<Q> unit = epsgFactory().createUnit(String.valueOf(epsg)).asType(quantity);
                if (scaleKey != 0) {
                    final double scale = getAsDouble(scaleKey);
                    if (!Double.isNaN(scale)) {
                        final double expected = unit.getConverterTo(defaultValue.getSystemUnit()).convert(1d);
                        if (Math.abs(expected - scale) > expected * Numerics.COMPARISON_THRESHOLD) {
                            warning(Resources.Keys.NotTheEpsgValue_5, (Constants.EPSG + ':') + epsg,
                                    expected, GeoKeys.name(scaleKey), scale, "");
                        }
                    }
                }
                return unit;
            }
        }
    }

    /**
     * Creates a prime meridian from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#PrimeMeridian}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a prime meridian value given by {@link GeoKeys#PrimeMeridianLong}.</li>
     *   </ul></li>
     * </ul>
     *
     * If no prime-meridian is defined, then the default is Greenwich as per GeoTIFF specification.
     *
     * @param  names  the component names to use if the prime meridian is user-defined.
     * @param  unit   the angular unit of the longitude value relative to Greenwich.
     * @return a prime meridian created from the given {@link Unit} and the above-cited GeoTIFF keys.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private PrimeMeridian createPrimeMeridian(final String[] names, final Unit<Angle> unit) throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.PrimeMeridian);
        switch (epsg) {
            case GeoCodes.undefined:      // If not specified, should default to Greenwich but we nevertheless verify.
            case GeoCodes.userDefined: {
                final double longitude = getAsDouble(GeoKeys.PrimeMeridianLong);
                if (Double.isNaN(longitude)) {
                    if (epsg != GeoCodes.undefined) {
                        missingValue(GeoKeys.PrimeMeridianLong);
                    }
                } else if (longitude != 0) {
                    /*
                     * If the prime meridian is not Greenwich, create that meridian but do not use the
                     * GeoKeys.GeogCitation value (unless it had a sub-element for the prime meridian).
                     * This is because the citation value is for the CRS (e.g. "WGS84") while the prime
                     * meridian names are very different (e.g. "Paris", "Madrid", etc).
                     */
                    return objectFactory().createPrimeMeridian(properties(names[PRIMEM]), longitude, unit);
                }
                break;                      // Default to Greenwich.
            }
            default: {
                /*
                 * Prime meridian defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also provide the longitude value, verify that the value is consistent
                 * with what we would expect for a prime meridian of the given EPSG code.
                 */
                final PrimeMeridian pm = epsgFactory().createPrimeMeridian(String.valueOf(epsg));
                verify(pm, unit);
                return pm;
            }
        }
        return CommonCRS.WGS84.primeMeridian();
    }

    /**
     * Verifies if the user-defined prime meridian created from GeoTIFF values
     * matches the given prime meridian created from the EPSG geodetic dataset.
     * This method does not verify the EPSG code.
     *
     * @param  pm    the prime meridian created from the EPSG geodetic dataset.
     * @param  unit  the unit of measurement declared in the GeoTIFF file.
     */
    private void verify(final PrimeMeridian pm, final Unit<Angle> unit) {
        verify(pm, ReferencingUtilities.getGreenwichLongitude(pm, unit), GeoKeys.PrimeMeridianLong, unit);
    }

    /**
     * Creates an ellipsoid from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#Ellipsoid} tag.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#GeogCitation},</li>
     *     <li>a semi major axis value given by {@link GeoKeys#SemiMajorAxis},</li>
     *     <li>one of:<ul>
     *       <li>an inverse flattening factor given by {@link GeoKeys#InvFlattening},</li>
     *       <li>or a semi major axis value given by {@link GeoKeys#SemiMinorAxis}.</li>
     *     </ul></li>
     *   </ul></li>
     * </ul>
     *
     * @param  names  the component names to use if the ellipsoid is user-defined.
     * @param  unit   the linear unit of the semi-axis lengths.
     * @return an ellipsoid created from the given {@link Unit} and the above-cited GeoTIFF keys.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private Ellipsoid createEllipsoid(final String[] names, final Unit<Length> unit) throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.Ellipsoid);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.GeodeticDatum));
            }
            case GeoCodes.userDefined: {
                /*
                 * Try to build ellipsoid from others parameters. Those parameters are the
                 * semi-major axis and either semi-minor axis or inverse flattening factor.
                 */
                final Map<String,?> properties = properties(getOrDefault(names, ELLIPSOID));
                final double semiMajor = getMandatoryDouble(GeoKeys.SemiMajorAxis);
                double inverseFlattening = getAsDouble(GeoKeys.InvFlattening);
                final Ellipsoid ellipsoid;
                if (!Double.isNaN(inverseFlattening)) {
                    ellipsoid = objectFactory().createFlattenedSphere(properties, semiMajor, inverseFlattening, unit);
                } else {
                    /*
                     * If the inverse flattening factory was not defined, fallback on semi-major axis length.
                     * This is a less common way to define ellipsoid (the most common way uses flattening).
                     */
                    final double semiMinor = getMandatoryDouble(GeoKeys.SemiMinorAxis);
                    ellipsoid = objectFactory().createEllipsoid(properties, semiMajor, semiMinor, unit);
                }
                lastName = ellipsoid.getName();
                return ellipsoid;
            }
            default: {
                /*
                 * Ellipsoid defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also provide defining parameter values, verify that those values
                 * are consistent with what we would expect for an ellipsoid of the given EPSG code.
                 */
                final Ellipsoid ellipsoid = epsgFactory().createEllipsoid(String.valueOf(epsg));
                verify(ellipsoid, unit);
                return ellipsoid;
            }
        }
    }

    /**
     * Verifies if the user-defined ellipsoid created from GeoTIFF values
     * matches the given ellipsoid created from the EPSG geodetic dataset.
     * This method does not verify the EPSG code.
     *
     * @param  ellipsoid  the ellipsoid created from the EPSG geodetic dataset.
     * @param  unit       the unit of measurement declared in the GeoTIFF file.
     */
    private void verify(final Ellipsoid ellipsoid, final Unit<Length> unit) {
        final UnitConverter uc = ellipsoid.getAxisUnit().getConverterTo(unit);
        verify(ellipsoid, uc.convert(ellipsoid.getSemiMajorAxis()), GeoKeys.SemiMajorAxis, unit);
        verify(ellipsoid, uc.convert(ellipsoid.getSemiMinorAxis()), GeoKeys.SemiMinorAxis, unit);
        verify(ellipsoid, ellipsoid.getInverseFlattening(),         GeoKeys.InvFlattening, null);
    }

    /**
     * Creates a geodetic datum from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#GeodeticDatum}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#GeogCitation},</li>
     *     <li>all values required by {@link #createPrimeMeridian(String[], Unit)} (optional),</li>
     *     <li>all values required by {@link #createEllipsoid(String[], Unit)}.</li>
     *   </ul></li>
     * </ul>
     *
     * @param  names        the component names to use if the geodetic datum is user-defined.
     * @param  angularUnit  the angular unit of the longitude value relative to Greenwich.
     * @param  linearUnit   the linear unit of the ellipsoid semi-axis lengths.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createPrimeMeridian(String[], Unit)
     * @see #createEllipsoid(String[], Unit)
     */
    private GeodeticDatum createGeodeticDatum(final String[] names, final Unit<Angle> angularUnit, final Unit<Length> linearUnit)
            throws FactoryException
    {
        final int epsg = getAsInteger(GeoKeys.GeodeticDatum);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.GeodeticDatum));
            }
            case GeoCodes.userDefined: {
                /*
                 * Create the ellipsoid and the prime meridian, then assemble those components into a datum.
                 * The datum name however may not be appropriate, since GeoTIFF provides only a global name
                 * for the whole CRS. The name does not matter so much, except for datums where the name is
                 * taken in account when determining if two datums are equal. In order to get better names,
                 * after datum construction we will compare the datum with a few well-known cases defined in
                 * CommonCRS. We use the CRS name given in the GeoTIFF file for that purpose, exploiting the
                 * fact that it is often a name that can be mapped to a CommonCRS name like "WGS84".
                 */
                String              name      = getOrDefault(names, DATUM);
                final Ellipsoid     ellipsoid = createEllipsoid(names, linearUnit);
                final PrimeMeridian meridian  = createPrimeMeridian(names, angularUnit);
                final GeodeticDatum datum     = objectFactory().createGeodeticDatum(properties(name), ellipsoid, meridian);
                name = Utilities.toUpperCase(name, Characters.Filter.LETTERS_AND_DIGITS);
                lastName = datum.getName();
                try {
                    final GeodeticDatum predefined = CommonCRS.valueOf(name).datum();
                    if (equalsIgnoreMetadata(predefined.getEllipsoid(), ellipsoid) &&
                        equalsIgnoreMetadata(predefined.getPrimeMeridian(), meridian))
                    {
                        return predefined;
                    }
                } catch (IllegalArgumentException e) {
                    // Not a name that can be mapped to CommonCRS. Ignore.
                }
                return datum;
            }
            default: {
                /*
                 * Datum defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are
                 * consistent with what we would expect for a datum of the given EPSG code.
                 */
                final GeodeticDatum datum = epsgFactory().createGeodeticDatum(String.valueOf(epsg));
                verify(datum, angularUnit, linearUnit);
                return datum;
            }
        }
    }

    /**
     * Verifies if the user-defined datum created from GeoTIFF values
     * matches the given datum created from the EPSG geodetic dataset.
     * This method does not verify the EPSG code of the given datum.
     *
     * @param  datum        the datum created from the EPSG geodetic dataset.
     * @param  angularUnit  unit of measurement declared in the GeoTIFF file.
     * @param  linearUnit   unit of measurement declared in the GeoTIFF file.
     */
    private void verify(final GeodeticDatum datum, final Unit<Angle> angularUnit, final Unit<Length> linearUnit) {
        final PrimeMeridian pm = datum.getPrimeMeridian();
        verifyIdentifier(datum, pm, GeoKeys.PrimeMeridian);
        verify(pm, angularUnit);
        final Ellipsoid ellipsoid = datum.getEllipsoid();
        verifyIdentifier(datum, ellipsoid, GeoKeys.Ellipsoid);
        verify(ellipsoid, linearUnit);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                     Geodetic CRS (geographic and geocentric)                     ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Splits the {@link GeoKeys#GeogCitation} value into its prime meridian, ellipsoid, datum and CRS name components.
     * This method is intended to parse geographic CRS names written like below:
     *
     * {@preformat text
     *   GCS Name = wgs84|Datum = unknown|Ellipsoid = WGS_1984|Primem = Greenwich|
     * }
     *
     * The keywords of both Well Known Text (WKT) version 1 and 2 are accepted as keys for the datum, ellipsoid and
     * prime meridian names. All other keys are presumed to be for the geographic CRS name. If the given string does
     * not contain the pipe ({@code '|'}) separator, then the whole string is presumed to be the geographic CRS name.
     *
     * <p>This method returns an array where component names are stored at index {@link #PRIMEM}, {@link #ELLIPSOID},
     * {@link #DATUM} and {@link #GCRS}. Any array element can be null if no name were found for that component.</p>
     */
    static String[] splitName(final String name) {
        final String[] names = new String[GCRS + 1];
        final String[] components = (String[]) CharSequences.split(name, SEPARATOR);
        switch (components.length) {
            case 0: break;
            case 1: names[GCRS] = name; break;
            default: {
                for (String value : components) {
                    final int s = value.indexOf('=');
                    int type = GCRS;
                    if (s >= 0) {
                        final int length = CharSequences.skipTrailingWhitespaces(value, 0, s);
                        if (length >= MIN_KEY_LENGTH) {
                            for (int t=0; t < NAME_KEYS.length; t++) {
                                if (value.regionMatches(true, 0, NAME_KEYS[t], 0, length)) {
                                    type = t/2;
                                    break;
                                }
                            }
                        }
                        value = value.substring(CharSequences.skipLeadingWhitespaces(value, s+1, value.length()));
                    }
                    if (!value.isEmpty()) {
                        if (names[type] != null) {
                            value = names[type] + ' ' + value;
                        }
                        names[type] = value;
                    }
                }
                break;
            }
        }
        return names;
    }

    /**
     * Returns the name at the given index if non-null. If that name is null, search for a name in a sister element
     * (e.g. the datum name or the geographic CRS name). If none is found, returns {@code null}.
     */
    private static String getOrDefault(final String[] names, int component) {
        String c = names[component];
        if (c == null) {
            // Prime meridian must be excluded in the search below.
            for (component = PRIMEM; ++component < names.length; component++) {
                c = names[component];
                if (c != null) {
                    break;
                }
            }
        }
        return c;
    }

    /**
     * Creates a geographic CRS from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#GeographicType}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a prime meridian value given by {@link GeoKeys#PrimeMeridianLong},</li>
     *     <li>a CRS name given by {@link GeoKeys#GeogCitation},</li>
     *     <li>a datum definition.</li>
     *   </ul></li>
     *   <li>A unit code given by {@link GeoKeys#AngularUnits} (optional).</li>
     *   <li>A unit scale factor given by {@link GeoKeys#AngularUnitSize} (optional).</li>
     * </ul>
     *
     * @param  rightHanded  whether to force longitude before latitude axis.
     * @param  angularUnit  the angular unit of the latitude and longitude values.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createGeodeticDatum(String[], Unit, Unit)
     */
    private GeographicCRS createGeographicCRS(final boolean rightHanded, final Unit<Angle> angularUnit) throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.GeographicType);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.GeographicType));
            }
            case GeoCodes.userDefined: {
                /*
                 * Creates the geodetic datum, then a geographic CRS assuming (longitude, latitude) axis order.
                 * We use the coordinate system of CRS:84 as a template and modify its unit of measurement if needed.
                 */
                final String[] names = splitName(getAsString(GeoKeys.GeogCitation));
                final Unit<Length> linearUnit = createUnit(GeoKeys.GeogLinearUnits, GeoKeys.GeogLinearUnitSize, Length.class, Units.METRE);
                final GeodeticDatum datum = createGeodeticDatum(names, angularUnit, linearUnit);
                EllipsoidalCS cs = CommonCRS.defaultGeographic().getCoordinateSystem();
                if (!Units.DEGREE.equals(angularUnit)) {
                    cs = replaceAngularUnit(cs, angularUnit);
                }
                final GeographicCRS crs = objectFactory().createGeographicCRS(properties(getOrDefault(names, GCRS)), datum, cs);
                lastName = crs.getName();
                return crs;
            }
            default: {
                /*
                 * Geographic CRS defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are consistent
                 * with what we would expect for a CRS of the given EPSG code.
                 */
                GeographicCRS crs = epsgFactory().createGeographicCRS(String.valueOf(epsg));
                if (rightHanded) {
                    crs = DefaultGeographicCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
                }
                verify(crs, angularUnit);
                return crs;
            }
        }
    }

    /**
     * Verifies if the user-defined CRS created from GeoTIFF values
     * matches the given CRS created from the EPSG geodetic dataset.
     * This method does not verify the EPSG code of the given CRS.
     *
     * @param  crs          the CRS created from the EPSG geodetic dataset.
     * @param  angularUnit  the angular unit of the latitude and longitude values.
     */
    private void verify(final GeographicCRS crs, final Unit<Angle> angularUnit) throws FactoryException {
        /*
         * Note: current createUnit(…) implementation does not allow us to distinguish whether METRE ou DEGREE units
         * were specified in the GeoTIFF file or if we got the default values. We do not compare units of that reason.
         */
        final Unit<Length> linearUnit = createUnit(GeoKeys.GeogLinearUnits, GeoKeys.GeogLinearUnitSize, Length.class, Units.METRE);
        final GeodeticDatum datum = crs.getDatum();
        verifyIdentifier(crs, datum, GeoKeys.GeodeticDatum);
        verify(datum, angularUnit, linearUnit);
    }

    /**
     * Creates a geocentric CRS from user-defined parameters.
     * The GeoTIFF values used by this method are the same than the ones used by {@code createGeographicCRS(…)}.
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createGeodeticDatum(String[], Unit, Unit)
     */
    private GeocentricCRS createGeocentricCRS() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.GeographicType);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.GeographicType));
            }
            case GeoCodes.userDefined: {
                /*
                 * Creates the geodetic datum, then a geocentric CRS. We use the coordinate system of
                 * the WGS84 geocentric CRS as a template and modify its unit of measurement if needed.
                 */
                final String[] names = splitName(getAsString(GeoKeys.GeogCitation));
                final Unit<Length> linearUnit = createUnit(GeoKeys.GeogLinearUnits, GeoKeys.GeogLinearUnitSize, Length.class, Units.METRE);
                final Unit<Angle> angularUnit = createUnit(GeoKeys.AngularUnits, GeoKeys.AngularUnitSize, Angle.class, Units.DEGREE);
                final GeodeticDatum datum = createGeodeticDatum(names, angularUnit, linearUnit);
                CartesianCS cs = (CartesianCS) CommonCRS.WGS84.geocentric().getCoordinateSystem();
                if (!Units.METRE.equals(linearUnit)) {
                    cs = replaceLinearUnit(cs, linearUnit);
                }
                final GeocentricCRS crs = objectFactory().createGeocentricCRS(properties(getOrDefault(names, GCRS)), datum, cs);
                lastName = crs.getName();
                return crs;
            }
            default: {
                /*
                 * Geocentric CRS defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are consistent
                 * with what we would expect for a CRS of the given EPSG code.
                 */
                final GeocentricCRS crs = epsgFactory().createGeocentricCRS(String.valueOf(epsg));
                verify(crs);
                return crs;
            }
        }
    }

    /**
     * Verifies if the user-defined CRS created from GeoTIFF values
     * matches the given CRS created from the EPSG geodetic dataset.
     * This method does not verify the EPSG code of the given CRS.
     *
     * @param  crs  the CRS created from the EPSG geodetic dataset.
     */
    private void verify(final GeocentricCRS crs) throws FactoryException {
        /*
         * Note: current createUnit(…) implementation does not allow us to distinguish whether METRE ou DEGREE units
         * were specified in the GeoTIFF file or if we got the default values. We do not compare units of that reason.
         */
        final Unit<Length> linearUnit = createUnit(GeoKeys.GeogLinearUnits, GeoKeys.GeogLinearUnitSize, Length.class, Units.METRE);
        final Unit<Angle> angularUnit = createUnit(GeoKeys.AngularUnits, GeoKeys.AngularUnitSize, Angle.class, Units.DEGREE);
        final GeodeticDatum datum = crs.getDatum();
        verifyIdentifier(crs, datum, GeoKeys.GeodeticDatum);
        verify(datum, angularUnit, linearUnit);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                                  Projected CRS                                   ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Map projection parameters to be considered as aliases. This table is used for reading GeoTIFF files
     * that are not really well-formed, but for which we can reasonably guess what was the producer intend
     * and which parameters were confused. See {@link #aliases(Map)} for more explanation.
     */
    private static final short[][] PARAMETER_ALIASES = {
        {GeoKeys.NatOriginLong, GeoKeys.FalseOriginLong,     GeoKeys.CenterLong},
        {GeoKeys.NatOriginLat,  GeoKeys.FalseOriginLat,      GeoKeys.CenterLat},
        {GeoKeys.FalseEasting,  GeoKeys.FalseOriginEasting,  GeoKeys.CenterEasting},
        {GeoKeys.FalseNorthing, GeoKeys.FalseOriginNorthing, GeoKeys.CenterNorthing},
        {GeoKeys.ScaleAtNatOrigin,                           GeoKeys.ScaleAtCenter}
    };

    /**
     * Updates a mapping from GeoTIFF numerical identifiers to parameter names by adding parameter aliases.
     * This method adds to the given map some GeoTIFF keys to be considered synonymous to an existing key.
     * Those "synonymous" parameters are strictly speaking not for the map projection that we are parsing,
     * but it is common to see GeoTIFF files with "wrong" projection parameter codes. For example:
     *
     * <ul>
     *   <li>The {@code "CT_LambertConfConic_1SP"} projection uses a {@code "NatOriginLong"} parameter.</li>
     *   <li>The {@code "CT_LambertConfConic_2SP"} projection uses a {@code "FalseOriginLong"} parameter.</li>
     * </ul>
     *
     * but we sometime see {@code "NatOriginLong"} parameter used for the {@code "CT_LambertConfConic_2SP"} projection.
     * Semantically those two parameters are for two different things but mathematically they are used in the same way.
     * Those "synonymous" will be invisible to the user; the map projection that (s)he will get uses the names defined
     * in the descriptor (not in the GeoTIFF file).
     */
    private static void aliases(final Map<Integer,String> mapping) {
        for (final short[] codes : PARAMETER_ALIASES) {
            for (int i=0; i<codes.length; i++) {
                final String name = mapping.get(codes[i] & 0xFFFF);
                if (name != null) {
                    for (int j=0; j<codes.length; j++) {
                        if (j != i) {
                            JDK8.putIfAbsent(mapping, codes[j] & 0xFFFF, name);
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Creates a projected CRS from an EPSG code or from user-defined parameters.
     * Some GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#ProjectedCSType}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#PCSCitation},</li>
     *     <li>a {@link CoordinateOperation} given by {@link GeoKeys#Projection},</li>
     *     <li>an {@link OperationMethod} given by {@link GeoKeys#CoordTrans}.</li>
     *   </ul></li>
     *   <li>A unit code given by {@link GeoKeys#LinearUnits} (optional).</li>
     *   <li>A unit scale factor given by {@link GeoKeys#LinearUnitSize} (optional).</li>
     * </ul>
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createGeographicCRS(boolean, Unit)
     * @see #createConversion(String, Unit, Unit)
     */
    private ProjectedCRS createProjectedCRS() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.ProjectedCSType);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.ProjectedCSType));
            }
            case GeoCodes.userDefined: {
                /*
                 * If the CRS is user-defined, we have to parse many components (base CRS, datum, unit, etc.)
                 * and build the projected CRS from them. Note that some GeoTIFF files put the projection name
                 * in the Citation key instead of PCSCitation.
                 */
                String name = getAsString(GeoKeys.PCSCitation);
                if (name == null) {
                    name = getAsString(GeoKeys.Citation);
                    // Note that Citation has been removed from the map, so it will not be used by 'complete(MetadataBuilder).
                }
                final Unit<Length>  linearUnit  = createUnit(GeoKeys.LinearUnits,  GeoKeys.LinearUnitSize, Length.class, Units.METRE);
                final Unit<Angle>   angularUnit = createUnit(GeoKeys.AngularUnits, GeoKeys.AngularUnitSize, Angle.class, Units.DEGREE);
                final GeographicCRS baseCRS     = createGeographicCRS(false, angularUnit);
                final Conversion    projection  = createConversion(name, angularUnit, linearUnit);
                CartesianCS cs = epsgFactory().createCartesianCS(String.valueOf(Constants.EPSG_PROJECTED_CS));
                if (!Units.METRE.equals(linearUnit)) {
                    cs = replaceLinearUnit(cs, linearUnit);
                }
                final ProjectedCRS crs = objectFactory().createProjectedCRS(properties(name), baseCRS, projection, cs);
                lastName = crs.getName();
                return crs;
            }
            default: {
                /*
                 * Projected CRS defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are consistent
                 * with what we would expect for a CRS of the given EPSG code.
                 */
                final ProjectedCRS crs = epsgFactory().createProjectedCRS(String.valueOf(epsg));
                verify(crs);
                return crs;
            }
        }
    }

    /**
     * Verifies if the user-defined CRS created from GeoTIFF values
     * matches the given CRS created from the EPSG geodetic dataset.
     * This method does not verify the EPSG code of the given CRS.
     *
     * @param  crs  the CRS created from the EPSG geodetic dataset.
     */
    private void verify(final ProjectedCRS crs) throws FactoryException {
        final Unit<Length> linearUnit  = createUnit(GeoKeys.LinearUnits,  GeoKeys.LinearUnitSize, Length.class, Units.METRE);
        final Unit<Angle>  angularUnit = createUnit(GeoKeys.AngularUnits, GeoKeys.AngularUnitSize, Angle.class, Units.DEGREE);
        final GeographicCRS baseCRS = crs.getBaseCRS();
        verifyIdentifier(crs, baseCRS, GeoKeys.GeographicType);
        verify(baseCRS, angularUnit);
        final Conversion projection = crs.getConversionFromBase();
        verifyIdentifier(crs, projection, GeoKeys.Projection);
        verify(projection, angularUnit, linearUnit);
    }

    /**
     * Creates a defining conversion from an EPSG code or from user-defined parameters.
     *
     * @param  angularUnit  the angular unit of the latitude and longitude values.
     * @param  linearUnit   the linear unit of easting and northing values.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ParameterNotFoundException if the GeoTIFF file defines an unexpected map projection parameter.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private Conversion createConversion(final String name, final Unit<Angle> angularUnit, final Unit<Length> linearUnit)
            throws FactoryException
    {
        final int epsg = getAsInteger(GeoKeys.Projection);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.Projection));
            }
            case GeoCodes.userDefined: {
                final Unit<Angle>         azimuthUnit = createUnit(GeoKeys.AzimuthUnits, (short) 0, Angle.class, Units.DEGREE);
                final String              type        = getMandatoryString(GeoKeys.CoordTrans);
                final OperationMethod     method      = operationFactory().getOperationMethod(Constants.GEOTIFF + ':' + type);
                final ParameterValueGroup parameters  = method.getParameters().createValue();
                final Map<Integer,String> toNames     = ReferencingUtilities.identifierToName(parameters.getDescriptor(), Citations.GEOTIFF);
                final Map<Object,Number>  paramValues = new HashMap<>();    // Keys: [String|Short] instances for [known|unknown] parameters.
                final Map<Short,Unit<?>>  deferred    = new HashMap<>();    // Only unknown parameters.
                final Iterator<Map.Entry<Short,Object>> it = geoKeys.entrySet().iterator();
                while (it.hasNext()) {
                    final Unit<?> unit;
                    final Map.Entry<Short,?> entry = it.next();
                    final Short key = entry.getKey();
                    switch (GeoKeys.unitOf(key)) {
                        case GeoKeys.RATIO:   unit = Units.UNITY; break;
                        case GeoKeys.LINEAR:  unit = linearUnit;  break;
                        case GeoKeys.ANGULAR: unit = angularUnit; break;
                        case GeoKeys.AZIMUTH: unit = azimuthUnit; break;
                        default: continue;
                    }
                    final Number value = (Number) entry.getValue();
                    it.remove();
                    final String paramName = toNames.get(key & 0xFFFF);
                    if (paramName != null) {
                        paramValues.put(paramName, value);
                        parameters.parameter(paramName).setValue(value.doubleValue(), unit);
                    } else {
                        paramValues.put(key, value);
                        deferred.put(key, unit);
                    }
                }
                /*
                 * At this point we finished to set all known map projection parameters. Sometime GeoTIFF files
                 * set the same parameter many times using different names as a safety for GeoTIFF readers that
                 * expect wrong parameters. If this is the case, verify that the parameter values are consistent.
                 * It is also possible that we found new parameters (actually parameters using the wrong names).
                 */
                if (!deferred.isEmpty()) {
                    aliases(toNames);
                    for (final Map.Entry<Short,Unit<?>> entry : deferred.entrySet()) {
                        final Short key = entry.getKey();
                        String paramName = toNames.get(key & 0xFFFF);
                        if (paramName == null) {
                            paramName = GeoKeys.name(key);
                            throw new ParameterNotFoundException(reader.errors().getString(
                                    Errors.Keys.UnexpectedParameter_1, paramName), paramName);
                        }
                        final Number value  = paramValues.get(key);
                        final Number actual = JDK8.putIfAbsent(paramValues, paramName, value);
                        if (actual == null) {
                            parameters.parameter(paramName).setValue(value.doubleValue(), entry.getValue());
                        } else if (!actual.equals(value)) {
                            warning(Resources.Keys.InconsistentMapProjParameter_4, paramName, actual, GeoKeys.name(key), value);
                        }
                    }
                }
                final Conversion c = operationFactory().createDefiningConversion(properties(name), method, parameters);
                lastName = c.getName();
                return c;
            }
            default: {
                /*
                 * Conversion defined by EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are
                 * consistent with what we would expect for a conversion of the given EPSG code.
                 */
                final Conversion projection = (Conversion) epsgFactory().createCoordinateOperation(String.valueOf(epsg));
                verify(projection, angularUnit, linearUnit);
                return projection;
            }
        }
    }

    /**
     * Verifies if the user-defined conversion created from GeoTIFF values
     * matches the given conversion created from the EPSG geodetic dataset.
     * This method does not verify the EPSG code of the given conversion.
     *
     * @param  projection  the conversion created from the EPSG geodetic dataset.
     */
    private void verify(final Conversion projection, final Unit<Angle> angularUnit, final Unit<Length> linearUnit)
            throws FactoryException
    {
        final Unit<Angle> azimuthUnit = createUnit(GeoKeys.AzimuthUnits, (short) 0, Angle.class, Units.DEGREE);
        final String type = getAsString(GeoKeys.CoordTrans);
        if (type != null) {
            /*
             * Compare the name of the map projection declared in the GeoTIFF file with the name
             * of the projection used by the EPSG geodetic dataset.
             */
            final OperationMethod method = projection.getMethod();
            if (!IdentifiedObjects.isHeuristicMatchForName(method, type)) {
                Identifier expected = IdentifiedObjects.getIdentifier(method, Citations.GEOTIFF);
                if (expected == null) {
                    expected = IdentifiedObjects.getIdentifier(method, null);
                }
                warning(Resources.Keys.NotTheEpsgValue_5, IdentifiedObjects.getIdentifierOrName(projection),
                        expected.getCode(), GeoKeys.name(GeoKeys.CoordTrans), type, "");
            }
            /*
             * Compare the parameter values with the ones declared in the EPSG geodetic dataset.
             */
            final ParameterValueGroup parameters = projection.getParameterValues();
            for (final short key : remainingKeys()) {
                final Unit<?> unit;
                switch (GeoKeys.unitOf(key)) {
                    case GeoKeys.RATIO:   unit = Units.UNITY; break;
                    case GeoKeys.LINEAR:  unit = linearUnit;  break;
                    case GeoKeys.ANGULAR: unit = angularUnit; break;
                    case GeoKeys.AZIMUTH: unit = azimuthUnit; break;
                    default: continue;
                }
                try {
                    verify(projection, parameters.parameter("GeoTIFF:" + key).doubleValue(unit), key, unit);
                } catch (ParameterNotFoundException e) {
                    warning(Resources.Keys.UnexpectedParameter_2, type, GeoKeys.name(key));
                }
            }
        }
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                                   Vertical CRS                                   ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a vertical datum.
     */
    private VerticalDatum createVerticalDatum() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.VerticalDatum);
        switch (epsg) {
            case GeoCodes.undefined:
            case GeoCodes.userDefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.VerticalDatum));
            }
            default: {
                return epsgFactory().createVerticalDatum(String.valueOf(epsg));
            }
        }
    }

    /**
     * Creates an optional vertical CRS, or returns {@code null} if no vertical CRS definition is found.
     * This method is different from the other {@code createFooCRS()} methods in that the vertical CRS
     * may be defined <em>in addition</em> of another CRS. Some GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#VerticalCSType}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#VerticalCitation},</li>
     *     <li>a {@link VerticalDatum} given by {@link GeoKeys#VerticalDatum}.</li>
     *   </ul></li>
     *   <li>A unit code given by {@link GeoKeys#VerticalUnits} (optional).</li>
     * </ul>
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private VerticalCRS createVerticalCRS() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.VerticalCSType);
        switch (epsg) {
            case GeoCodes.undefined: {
                return null;
            }
            case GeoCodes.userDefined: {
                final String name = getAsString(GeoKeys.VerticalCitation);
                final VerticalDatum datum = createVerticalDatum();
                final Unit<Length> unit = createUnit(GeoKeys.VerticalUnits, (short) 0, Length.class, Units.METRE);
                VerticalCS cs = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs().getCoordinateSystem();
                if (!Units.METRE.equals(unit)) {
                    cs = (VerticalCS) CoordinateSystems.replaceLinearUnit(cs, unit);
                }
                return objectFactory().createVerticalCRS(properties(name), datum, cs);
            }
            default: {
                return epsgFactory().createVerticalCRS(String.valueOf(epsg));
            }
        }
    }

    /**
     * Returns a string representation of the keys and associated values in this {@code CRSBuilder}.
     */
    @Debug
    @Override
    public final String toString() {
        final StringBuilder buffer = new StringBuilder("GeoTIFF keys ").append(majorRevision).append('.')
                .append(minorRevision).append(" in ").append(reader.input.filename).append(System.lineSeparator());
        final TableAppender table = new TableAppender(buffer, " ");
        for (Map.Entry<Short,Object> entry : geoKeys.entrySet()) {
            final short key = entry.getKey();
            table.append(String.valueOf(key)).nextColumn();
            table.append(GeoKeys.name(key)).nextColumn();
            table.append(" = ").append(String.valueOf(entry.getValue())).nextLine();
        }
        try {
            table.flush();
        } catch (IOException e) {
            throw new AssertionError(e);        // Should never happen since we wrote to a StringBuffer.
        }
        return buffer.toString();
    }
}
