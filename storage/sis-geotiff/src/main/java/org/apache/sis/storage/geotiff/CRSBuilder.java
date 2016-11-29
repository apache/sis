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

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.NoSuchElementException;
import java.lang.reflect.Array;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Utilities;
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
import org.apache.sis.util.Characters;

import static org.apache.sis.util.Utilities.equalsIgnoreMetadata;


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
     * Number of {@code short} values in each GeoKey entry.
     */
    private static final int ENTRY_LENGTH = 4;

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
     * Name of the last object created. This is used by {@link #properties(String)} for reusing existing instance
     * if possible. This is useful in GeoTIFF file since they do not use different names for geographic CRS,
     * the datum and the ellipsoid.
     */
    private Identifier lastName;

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
    private Map<String,?> properties(final String name) {
        Object value = name;
        if (name == null) {
            value = NilReferencingObject.UNNAMED;
        } else if (lastName != null && lastName.getCode().equals(name)) {
            value = lastName;
        }
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, value);
    }

    /**
     * Returns the value for the given key as a singleton (not an array).
     * If the value was an array, a warning is reported and the first element is returned.
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return the singleton value for the given key, or {@code null} if none.
     */
    private Object getSingleton(final short key) {
        Object value = geoKeys.get(key);
        if (value != null && value.getClass().isArray()) {
            warning(Resources.Keys.UnexpectedListOfValues_2, GeoKeys.name(key), Array.getLength(value));
            value = Array.get(value, 0);    // No need to verify length because we do not store empty arrays.
        }
        return value;
    }

    /**
     * Returns a {@link GeoKeys} value as a character string, or {@code null} if none.
     * Value for the given key should be a sequence of characters. If it is one or more numbers instead,
     * then this method formats those numbers in a comma-separated list. Such sequence of numbers would
     * be unusual, but we do see strange GeoTIFF files in practice.
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return a string representation of the value for the given key, or {@code null} if the key was not found.
     */
    private String getAsString(final short key) {
        Object value = geoKeys.get(key);
        if (value != null) {
            if (value.getClass().isArray()) {
                final int length = Array.getLength(value);
                final StringJoiner buffer = new StringJoiner(", ");
                for (int i=0; i<length; i++) {
                    buffer.add(String.valueOf(Array.get(value, i)));
                }
                value = buffer;
            }
            return value.toString();
        }
        return null;
    }

    /**
     * Returns a {@link GeoKeys} value as an integer. This is used for fetching enumeration values.
     * The value returned by this method is typically one of the {@link GeoCodes} values.
     *
     * @param  key the GeoTIFF key for which to get a value.
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
            throw e;
        }
    }

    /**
     * Returns a {@link GeoKeys} value as a floating point number, or {@code NaN} if none.
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
            throw e;
        }
    }

    /**
     * Returns a {@link GeoKeys} value as a character string, or throws an exception if none.
     * A warning is reported before to throw the exception. There is no attempt to provide a
     * good message in the exception since is should be caught by {@link ImageFileDirectory}.
     *
     * @param  key        the GeoTIFF key for which to get a value.
     * @param  mandatory  whether a value is mandatory for the given key.
     * @return a string representation of the value for the given key.
     * @throws NoSuchElementException if no value has been found.
     */
    private String getMandatoryString(final short key) {
        final String value = getAsString(key);
        if (value != null) {
            return value;
        }
        throw new NoSuchElementException(missingValue(key));
    }

    /**
     * Returns a {@link GeoKeys} value as a floating point number, or throws an exception if none.
     * A warning is reported before to throw the exception. There is no attempt to provide a good
     * message in the exception since is should be caught by {@link ImageFileDirectory}.
     *
     * @param  key        the GeoTIFF key for which to get a value.
     * @param  mandatory  whether a value is mandatory for the given key.
     * @return the floating point value for the given key.
     * @throws NoSuchElementException if no value has been found.
     * @throws NumberFormatException if the value was stored as a string and can not be parsed.
     */
    private double getMandatoryDouble(final short key) {
        final double value = getAsDouble(key);
        if (!Double.isNaN(value)) {
            return value;
        }
        throw new NoSuchElementException(missingValue(key));
    }

    /**
     * Reports a warning about missing value for the given key.
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


    // ---------------------------- geokeys parsing -----------------------------


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
            throws DataStoreContentException, FactoryException
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
                    if (count != 0 && asciiParameters.charAt(upper - 1) == '|') {
                        upper--;    // Skip trailing pipe, interpreted as C/C++ NUL character.
                    }
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
                    warning(Resources.Keys.IgnoredGeoKey_1, GeoKeys.name(key));
                    continue;
                }
            }
            geoKeys.put(key, value);
        }
        /*
         * At this point we finished copying all GeoTIFF keys in CRSBuilder.geoKeys map.
         */
        final int crsType = getAsInteger(GeoKeys.ModelType);
        switch (crsType) {
            case GeoCodes.undefined:           return null;
            case GeoCodes.ModelTypeProjected:  return createProjectedCRS();
            case GeoCodes.ModelTypeGeographic: return createGeographicCRS();
            case GeoCodes.ModelTypeGeocentric: // TODO
            default: {
                warning(Resources.Keys.UnsupportedCoordinateSystemKind_1, crsType);
                return null;
            }
        }
    }

    /**
     * Completes ISO 19115 metadata with some GeoTIFF values that are for documentation purposes.
     * Those values do not participate directly to the construction of the Coordinate Reference System objects.
     *
     * <p><b>Pre-requite:</b></p>
     * <ul>
     *   <li>{@link #build(Vector, Vector, String)} must have been invoked before this method.</li>
     *   <li>{@link ImageFileDirectory} must have filled its part of metadata before to invoke this method.</li>
     * </ul>
     *
     * @param  metadata  the helper class where to write metadata values.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     */
    final void complete(final MetadataBuilder metadata) {
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
         * ASCII reference to published documentation on the overall configuration of the GeoTIFF file.
         */
        final String title = getAsString(GeoKeys.Citation);
        if (title != null) {
            if (!metadata.hasTitle()) {
                metadata.addTitle(title);
            } else {
                metadata.setGridToCRS(title);
            }
        }
    }


    // -------------------------- geodetic components ---------------------------


    /**
     * Creates units of measurement for projected or geographic coordinate reference systems.
     * The units may either be specified as a standard EPSG recognized unit, or may be user defined.
     * If the first case (EPSG code), the {@code keyUser} is ignored. In the second case (user-defined),
     * the unit of measurement is defined by a conversion factor from metre or radian base unit.
     *
     * @param  keyEPSG       the {@link GeoKeys} for a unit of measurement defined by an EPSG code.
     * @param  keyUser       the {@link GeoKeys} for a unit of measurement defined by a scale applied on a base unit.
     * @param  quantity      {@link Length} for a linear unit, or {@link Angle} for an angular unit.
     * @param  defaultValue  the unit of measurement to return if no value is found in the GeoTIFF file.
     * @return the unit of measurement associated to the given {@link GeoKeys}, or the default value.
     *
     * @throws NoSuchElementException if {@code keyEPSG} value is {@link GeoCodes#userDefined} and no value is associated to {@code keyUser}.
     * @throws NumberFormatException  if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException     if the unit of measurement identified by the EPSG code is not of the expected quantity.
     */
    private <Q extends Quantity<Q>> Unit<Q> createUnit(final short keyEPSG, final short keyUser,
            final Class<Q> quantity, final Unit<Q> defaultValue) throws FactoryException
    {
        final int epsg = getAsInteger(keyEPSG);
        switch (epsg) {
            case GeoCodes.undefined: {
                return defaultValue;
            }
            case GeoCodes.userDefined: {
                return defaultValue.getSystemUnit().multiply(getMandatoryDouble(keyUser));
            }
            default: {
                return epsgFactory().createUnit(String.valueOf(epsg)).asType(quantity);
            }
        }
    }

    /**
     * Creates a geodetic datum from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#GeogGeodeticDatum}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#GeogCitation},</li>
     *     <li>all values required by {@link #createPrimeMeridian(Unit)} (optional),</li>
     *     <li>all values required by {@link #createEllipsoid(Unit)}.</li>
     *   </ul></li>
     * </ul>
     *
     * @param  name         the name to use if the geodetic datum is user-defined, or {@code null} if unnamed.
     * @param  angularUnit  the angular unit of the longitude value relative to Greenwich.
     * @param  linearUnit   the linear unit of the ellipsoid semi-axis lengths.
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createPrimeMeridian(Unit)
     * @see #createEllipsoid(Unit)
     */
    private GeodeticDatum createGeodeticDatum(String name, final Unit<Angle> angularUnit, final Unit<Length> linearUnit)
            throws FactoryException
    {
        final int epsg = getAsInteger(GeoKeys.GeogGeodeticDatum);
        switch (epsg) {
            case GeoCodes.undefined: {
                throw new NoSuchElementException(missingValue(GeoKeys.GeogGeodeticDatum));
            }
            default: {
                // Geodetic Datum defined by an EPSG code.
                return epsgFactory().createGeodeticDatum(String.valueOf(epsg));
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
                final Ellipsoid     ellipsoid = createEllipsoid(name, linearUnit);
                final PrimeMeridian meridian  = createPrimeMeridian(angularUnit);
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
        }
    }

    /**
     * Creates the prime meridian from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#GeogPrimeMeridian}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a prime meridian value given by {@link GeoKeys#GeogPrimeMeridianLong}.</li>
     *   </ul></li>
     * </ul>
     *
     * If no prime-meridian is defined, then the default is Greenwich as per GeoTIFF specification.
     *
     * @param  unit  the angular unit of the longitude value relative to Greenwich.
     * @return a prime meridian created from the given {@link Unit} and the above-cited GeoTIFF keys.
     *
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private PrimeMeridian createPrimeMeridian(final Unit<Angle> unit) throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.GeogPrimeMeridian);
        switch (epsg) {
            case GeoCodes.undefined: break;         // If not specified, default to Greenwich.
            default: {                              // Prime Meridian defined by an EPSG code.
                return epsgFactory().createPrimeMeridian(String.valueOf(epsg));
            }
            case GeoCodes.userDefined: {
                final double longitude = getAsDouble(GeoKeys.GeogPrimeMeridianLong);
                if (Double.isNaN(longitude)) {
                    missingValue(GeoKeys.GeogPrimeMeridianLong);
                } else if (longitude != 0) {
                    /*
                     * If the prime meridian is not Greenwich, create that meridian without name.
                     * We do not use the name given by GeoKeys.GeogCitation because that name is
                     * for the CRS (e.g. "WGS84") while the prime meridian names are very different
                     * (e.g. "Paris", "Madrid", etc).
                     */
                    return objectFactory().createPrimeMeridian(properties(null), longitude, unit);
                }
            }
        }
        return CommonCRS.WGS84.primeMeridian();
    }

    /**
     * Creates the ellipsoid from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#GeogEllipsoid} tag.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#GeogCitation},</li>
     *     <li>a semi major axis value given by {@link GeoKeys#GeogSemiMajorAxis},</li>
     *     <li>one of:<ul>
     *       <li>an inverse flattening factor given by {@link GeoKeys#GeogInvFlattening},</li>
     *       <li>or a semi major axis value given by {@link GeoKeys#GeogSemiMinorAxis}.</li>
     *     </ul></li>
     *   </ul></li>
     * </ul>
     *
     * @param  name  the name to use if the ellipsoid is user-defined, or {@code null} if unnamed.
     * @param  unit  the linear unit of the semi-axis lengths.
     * @return an ellipsoid created from the given {@link Unit} and the above-cited GeoTIFF keys.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and can not be parsed.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private Ellipsoid createEllipsoid(final String name, final Unit<Length> unit) throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.GeogEllipsoid);
        switch (epsg) {
            case GeoCodes.undefined: {
                throw new NoSuchElementException(missingValue(GeoKeys.GeogGeodeticDatum));
            }
            default: {
                // Ellipsoid defined by an EPSG code.
                return epsgFactory().createEllipsoid(String.valueOf(epsg));
            }
            case GeoCodes.userDefined: {
                /*
                 * Try to build ellipsoid from others parameters. Those parameters are the
                 * semi-major axis and either semi-minor axis or inverse flattening factor.
                 */
                final Map<String,?> properties = properties(name);
                final double semiMajor = getMandatoryDouble(GeoKeys.GeogSemiMajorAxis);
                double inverseFlattening = getAsDouble(GeoKeys.GeogInvFlattening);
                final Ellipsoid ellipsoid;
                if (!Double.isNaN(inverseFlattening)) {
                    ellipsoid = objectFactory().createFlattenedSphere(properties, semiMajor, inverseFlattening, unit);
                } else {
                    /*
                     * If the inverse flattening factory was not defined, fallback on semi-major axis length.
                     * This is a less common way to define ellipsoid (the most common way uses flattening).
                     */
                    final double semiMinor = getMandatoryDouble(GeoKeys.GeogSemiMinorAxis);
                    ellipsoid = objectFactory().createEllipsoid(properties, semiMajor, semiMinor, unit);
                }
                lastName = ellipsoid.getName();
                return ellipsoid;
            }
        }
    }

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

        final String projCode = getAsString(GeoKeys.ProjectedCSType);

        //-- getting the linear unit used by this coordinate reference system.
        final Unit<Length> linearUnit = createUnit(GeoKeys.ProjLinearUnits, GeoKeys.ProjLinearUnitSize, Length.class, Units.METRE);

        //--------------------------- USER DEFINE -----------------------------//
        //-- if it's user defined, we have to parse many informations and
        //-- try to build appropriate projected CRS from theses parsed informations.
        //-- like base gcrs, datum, unit ...
        if (projCode == null
         || projCode.equals(GeoKeys.GTUserDefined_String))
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
            pcrs = objectFactory().createProjectedCRS(properties(IdentifiedObjects.getName(pcrs, new DefaultCitation("EPSG"))),
                    pcrs.getBaseCRS(),
                    pcrs.getConversionFromBase(),
                    replaceLinearUnit(pcrs.getCoordinateSystem(), linearUnit));
            lastName = pcrs.getName();
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
    private ProjectedCRS createUserDefinedProjectedCRS(final Unit<Length> linearUnit)
            throws FactoryException, DataStoreContentException {
        //-- get projected CRS Name
        String projectedCrsName = getAsString(GeoKeys.PCSCitation);
        if (projectedCrsName == null) {
            projectedCrsName = "User Defined unnamed ProjectedCRS";
        }
        //--------------------------------------------------------------------//
        //                   get the GEOGRAPHIC BASE CRS                      //
        //--------------------------------------------------------------------//
        final GeographicCRS gcs = createGeographicCRS();

        //-- get the projection code if exist
        final String projCode = getAsString(GeoKeys.Projection);

        //-- is it user defined?
        final Conversion projection;
        if (projCode == null || projCode.equals(GeoKeys.GTUserDefined_String)) {

            //-- get Operation Method from proj key
            final String coordTrans               = getMandatoryString(GeoKeys.ProjCoordTrans);
            final OperationMethod operationMethod = operationFactory().getOperationMethod(coordTrans);
            final ParameterValueGroup parameters  = operationMethod.getParameters().createValue();
            projection                            = operationFactory().createDefiningConversion(properties(projectedCrsName), operationMethod, parameters);
            lastName = projection.getName();
        } else {
            projection = (Conversion) epsgFactory().createCoordinateOperation(String.valueOf(projCode));
        }

        CartesianCS predefineCartesianCS = epsgFactory().createCartesianCS("EPSG:4400");
        //-- manage unit if necessary
        if (linearUnit != null) {
            predefineCartesianCS = replaceLinearUnit(predefineCartesianCS, linearUnit);
        }
        return objectFactory().createProjectedCRS(properties(projectedCrsName), gcs, projection, predefineCartesianCS);
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
        final String tempCode = getAsString(GeoKeys.GeographicType);
        //-- Angular units used in this geotiff image
        Unit<Angle> angularUnit = createUnit(GeoKeys.GeogAngularUnits, GeoKeys.GeogAngularUnitSize, Angle.class, Units.DEGREE);
        //-- Geographic CRS is "UserDefine", we have to parse many informations from other geokeys.
        if (tempCode == null || tempCode.equals(GeoKeys.GTUserDefined_String)) {

            final Unit<Length> linearUnit = createUnit(GeoKeys.GeogLinearUnits, GeoKeys.GeogLinearUnitSize, Length.class, Units.METRE);

            ///-- Geographic CRS given name from tiff tag (GeogCitation)
            String name = getAsString(GeoKeys.GeogCitation);
            if (name == null) name = "User-defined Geographic CRS";

            final GeodeticDatum datum = createGeodeticDatum(name, angularUnit, linearUnit);
            //-- make the user defined GCS from all the components...
            return objectFactory().createGeographicCRS(properties(name), datum,
                    replaceAngularUnit(CommonCRS.defaultGeographic().getCoordinateSystem(), angularUnit));
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
            geoCRS = objectFactory().createGeographicCRS(properties(IdentifiedObjects.getName(geoCRS, new DefaultCitation("EPSG"))),
                                                        ((GeodeticCRS)geoCRS).getDatum(),
                                                        CommonCRS.defaultGeographic().getCoordinateSystem());
            lastName = geoCRS.getName();
        }
        //-- in case where tiff define unit does not match
        if (angularUnit != null
        && !angularUnit.equals(geoCRS.getCoordinateSystem().getAxis(0).getUnit())) {
            geoCRS = objectFactory().createGeographicCRS(properties(IdentifiedObjects.getName(geoCRS, new DefaultCitation("EPSG"))),
                    ((GeographicCRS)geoCRS).getDatum(), replaceAngularUnit(CommonCRS.defaultGeographic().getCoordinateSystem(), angularUnit));
            lastName = geoCRS.getName();
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
                                ? GeoKeys.name(Short.parseShort(coordTransCode))
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
//                final double standard_parallel_1 = metadata.getAsDouble(ProjStdParallel1);
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
//                final double longitudeOfOrigin = metadata.getAsDouble(ProjStraightVertPoleLong);
//
//                /*
//                * For polar Stereographic variant A only latitudeOfNaturalOrigin expected values are {-90; +90}.
//                * In some case, standard parallele is stipulate into latitudeOfNaturalOrigin tiff tag by error.
//                * To avoid CRS problem creation, try to anticipe this comportement by switch latitudeOfNaturalOrigin into standard parallele.
//                * HACK FOR USGS LANDSAT 8 difference between geotiff tag and Landsat 8 metadata MTL.txt file.
//                */
//                double standardParallel                 = metadata.getAsDouble(ProjStdParallel1);
//                final double latitudeOfNaturalOrigin    = metadata.getAsDouble(ProjNatOriginLat);
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
//                    parameters.parameter(code(PolarStereographicA.SCALE_FACTOR)).setValue(metadata.getAsDouble(ProjScaleAtNatOrigin));
//                    parameters.parameter(code(PolarStereographicA.FALSE_EASTING)).setValue(metadata.getAsDouble(ProjFalseEasting));
//                    parameters.parameter(code(PolarStereographicA.FALSE_NORTHING)).setValue(metadata.getAsDouble(ProjFalseNorthing));
//
//                } else {
//
//                    //-- Variant B and C share STANDARD_PARALLEL
//
//                    final double falseOriginEasting = metadata.getAsDouble(ProjFalseOriginEasting);
//                    if (Double.isNaN(falseOriginEasting)) {
//                        //-- no false Origin Easting : PolarStereoGraphic VARIANT B
//                        final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                              .getOperationMethod("Polar Stereographic (variant B)");
//
//                        parameters = method.getParameters().createValue();
//                        parameters.parameter(code(PolarStereographicB.STANDARD_PARALLEL)).setValue(standardParallel);
//                        parameters.parameter(code(PolarStereographicB.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                        parameters.parameter(code(PolarStereographicB.FALSE_EASTING)).setValue(metadata.getAsDouble(ProjFalseEasting));
//                        parameters.parameter(code(PolarStereographicB.FALSE_NORTHING)).setValue(metadata.getAsDouble(ProjFalseNorthing));
//                    } else {
//                        //-- PolarStereoGraphic VARIANT C
//                        final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                              .getOperationMethod("Polar Stereographic (variant C)");
//
//                        parameters = method.getParameters().createValue();
//                        parameters.parameter(code(PolarStereographicB.STANDARD_PARALLEL)).setValue(standardParallel);
//                        parameters.parameter(code(PolarStereographicB.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                        parameters.parameter(code(PolarStereographicC.EASTING_AT_FALSE_ORIGIN)).setValue(metadata.getAsDouble(ProjFalseOriginEasting));
//                        parameters.parameter(code(PolarStereographicC.NORTHING_AT_FALSE_ORIGIN)).setValue(metadata.getAsDouble(ProjFalseNorthing));
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
            strBuild.append(GeoKeys.name(key)).append(" (").append(key).append(") = ").append(entry.getValue());
            strBuild.append('\n');
        }
        return strBuild.toString();
    }
}
