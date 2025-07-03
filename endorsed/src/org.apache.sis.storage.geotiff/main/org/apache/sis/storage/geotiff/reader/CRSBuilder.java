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
package org.apache.sis.storage.geotiff.reader;

import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.NoSuchElementException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.lang.reflect.Array;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.UnitConverter;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.GeodeticCRS;
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
import org.opengis.util.FactoryException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.privy.NilReferencingObject;
import org.apache.sis.referencing.privy.ReferencingUtilities;
import org.apache.sis.referencing.privy.ReferencingFactoryContainer;
import org.apache.sis.referencing.operation.provider.PolarStereographicA;
import org.apache.sis.referencing.operation.provider.PolarStereographicB;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Characters;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.privy.Strings;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.storage.geotiff.base.Resources;
import org.apache.sis.storage.geotiff.base.GeoCodes;
import org.apache.sis.storage.geotiff.base.GeoKeys;
import org.apache.sis.storage.geotiff.base.UnitKey;
import static org.apache.sis.util.Utilities.equalsIgnoreMetadata;


/**
 * Helper class for building a {@link CoordinateReferenceSystem} from information found in TIFF tags.
 * GeoKeys are loaded by {@link GeoKeysLoader} and consumed by this class.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see GeoKeys
 * @see GeoKeysLoader
 */
public final class CRSBuilder extends ReferencingFactoryContainer {
    /**
     * Index where to store the name of the geodetic CRS, the datum, the ellipsoid and the prime meridian.
     * The GeoTIFF specification has only one key, {@link GeoKeys#GeodeticCitation}, for the geographic CRS
     * and its components. But some GeoTIFF files encode the names of all components in the value associated
     * to that key, as in the following example:
     *
     * <pre class="text">
     *   GCS Name = wgs84|Datum = unknown|Ellipsoid = WGS_1984|Primem = Greenwich|</pre>
     *
     * In such case, we will split the name into the components names to be stored in an array at indices
     * given by {@code GCRS}, {@code DATUM}, {@code ELLIPSOID} and {@code PRIMEM}.
     *
     * <h4>Implementation note:</h4>
     * {@link #PRIMEM} must be the first element and {@link #GCRS} must be the last one.
     * The {@link #getOrDefault(String[], int)} implementation depends on this assumption.
     *
     * @see #splitName(String)
     * @see #getOrDefault(String[], int)
     */
    static final int PRIMEM = 0, ELLIPSOID = 1, DATUM = 2, GCRS = 3;

    /**
     * Keys that may be used in the value associated to {@link GeoKeys#GeodeticCitation}.
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
     * For example, a value of 5 means that {@link #splitName(String)} will accept {@code "Ellip"},
     * {@code "Ellips"}, {@code "Ellipso"} and {@code "Ellipsoi"} as if they were {@code "Ellipsoid"}.
     * This length shall not be greater than the length of the shortest string in {@link #NAME_KEYS}.
     */
    private static final int MIN_KEY_LENGTH = 5;

    /**
     * The listeners where to report warnings.
     */
    private final StoreListeners listeners;

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
    private final Map<Short,Object> geoKeys;

    /**
     * Missing GeoKeys, used for avoiding to report the same warning twice.
     *
     * @see #missingValue(short)
     */
    private final Set<String> missingGeoKeys;

    /**
     * Name of the last object created. This is used by {@link #properties(Object)} for reusing existing instance
     * if possible. This is useful in GeoTIFF files since the same name is used for different geodetic components,
     * for example the datum and the ellipsoid.
     */
    private Identifier lastName;

    /**
     * Suggested value for a general description of the transformation form grid coordinates to "real world" coordinates.
     * This is computed by {@link #build(Vector, Vector, String)} and made available as additional information to the caller.
     */
    public String description;

    /**
     * {@code POINT} if {@link GeoKeys#RasterType} is {@link GeoCodes#RasterPixelIsPoint},
     * {@code AREA} if it is {@link GeoCodes#RasterPixelIsArea}, or null if unspecified.
     * This is computed by {@link #build(Vector, Vector, String)} and made available to the caller.
     */
    public CellGeometry cellGeometry;

    /**
     * {@code true} when an exception has been thrown but this {@code CRSBuilder} already reported a warning,
     * so there is no need for the caller to report a warning again. {@code CRSBuilder} sometimes reports warnings
     * itself when it can provide a better warning message than what the caller can do.
     */
    boolean alreadyReported;

    /**
     * Creates a new builder of coordinate reference systems.
     *
     * @param  listeners  the listeners where to report warnings.
     */
    public CRSBuilder(final StoreListeners listeners) {
        this.listeners = listeners;
        geoKeys = new HashMap<>(32);
        missingGeoKeys = new HashSet<>();
    }

    /**
     * Reports a warning with a message built from the given resource keys and arguments.
     * Note that the record will not necessarily be sent to the logging framework.
     * If the user has registered at least one listener, then the record will be sent to the listeners instead.
     *
     * <p>This method sets the {@linkplain LogRecord#setSourceClassName(String) source class name} and
     * {@linkplain LogRecord#setSourceMethodName(String) source method name} to hard-coded values.
     * Those values assume that the warnings occurred indirectly from a call to {@link #getMetadata()}
     * in this class. We do not report private classes or methods as the source of warnings.</p>
     *
     * @param  key   one of the {@link Resources.Keys} constants.
     * @param  args  arguments for the log message.
     *
     * @see Resources
     * @see GeoKeysLoader#warning(short, Object...)
     */
    final void warning(final short key, final Object... args) {
        LogRecord record = Resources.forLocale(getLocale()).createLogRecord(Level.WARNING, key, args);
        // Logger name will be set by listeners.warning(record).
        record.setSourceClassName(GeoTiffStore.class.getName());
        record.setSourceMethodName("getMetadata");
        listeners.warning(record);
    }

    /**
     * Returns a map with the given name associated to {@value org.opengis.referencing.IdentifiedObject#NAME_KEY}.
     * The given name shall be either an instance of {@link String} or {@link Identifier}.
     * This is a helper method for creating geodetic objects with {@link #getCRSFactory()}.
     */
    private Map<String,?> properties(Object name) {
        if (name == null) {
            name = NilReferencingObject.UNNAMED;
        } else if (lastName != null && lastName.getCode().equals(name)) {
            name = lastName;
        }
        return Map.of(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * Removes and returns the value for the given key as a singleton (not an array).
     * If the value was an array, a warning is reported and the first element is returned.
     *
     * <p>The given element is removed from the map so that each element is used only once
     * (for example it would be redundant to have the {@code verify(…)} methods to compare
     * the same values as the ones we used at geodetic object construction time). It also
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
     * Removes and returns a {@link GeoKeys} value as an integer. This is used for fetching enumeration values.
     * The value returned by this method is typically one of the {@link GeoCodes} values.
     *
     * <p>See {@link #getSingleton(short)} for a discussion about why the value is removed from the map.</p>
     *
     * @param  key  the GeoTIFF key for which to get a value.
     * @return the integer value for the given key, or {@link GeoCodes#undefined} if the key was not found.
     * @throws NumberFormatException if the value was stored as a string and cannot be parsed.
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
     * @throws NumberFormatException if the value was stored as a string and cannot be parsed.
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
     * @throws NumberFormatException if the value was stored as a string and cannot be parsed.
     */
    private double getMandatoryDouble(final short key) {
        final double value = getAsDouble(key);
        if (Double.isFinite(value)) {
            return value;
        }
        alreadyReported = true;
        throw new NoSuchElementException(missingValue(key));
    }

    /**
     * Reports a warning about missing value for the given key. The key name is opportunistically returned for
     * building the {@link NoSuchElementException} message, but it is not the main purpose of this method.
     *
     * @see GeoKeysLoader#missingValue(short)
     */
    final String missingValue(final short key) {
        final String name = GeoKeys.name(key);
        if (missingGeoKeys.add(name)) {
            warning(Resources.Keys.MissingGeoValue_1, name);
        }
        return name;
    }

    /**
     * Reports a warning about an invalid value for the given key.
     */
    private void invalidValue(final short key, final Object value) {
        warning(Resources.Keys.InvalidGeoValue_2, GeoKeys.name(key), value);
    }

    /**
     * Moves the value of a projection parameter to a new GeoKey.
     * This is used for handling erroneous map projection definitions.
     * A warning is emitted.
     *
     * @param  projection  name of the map projection to report in the warning.
     * @param  oldKey      old map projection key.
     * @param  newKey      new map projection key, or 0 if none.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-572">SIS-572</a>
     */
    private void moveParameter(final String projection, final short oldKey, final short newKey) {
        final Object value = geoKeys.remove(oldKey);
        if (value != null) {
            final Object name;
            if (newKey != 0) {
                geoKeys.put(newKey, value);
                name = GeoKeys.name(newKey);
            } else {
                name = Vocabulary.formatInternational(Vocabulary.Keys.None);
            }
            warning(Resources.Keys.ReassignedParameter_3, GeoKeys.name(oldKey), name, projection);
        }
    }

    /**
     * Verifies that a value found in the GeoTIFF file is approximately equal to the expected value.
     * This method is invoked when a CRS component is defined both explicitly and by EPSG code,
     * in which case we expect the given value to be equal to the value fetched from the EPSG database.
     * If the values do not match, a warning is reported and the caller should use the EPSG value.
     *
     * @param  epsg      the EPSG object, to be used for formatting the warning in case of mismatched values.
     * @param  expected  the expected value for an object identified by the {@code epsg} code, or NaN for no comparison.
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
             * Use Double.toString(…) instead of NumberFormat because the latter does not show
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
                    listeners.warning(e);               // Should not happen.
                    return;
                }
                if (code != expected) {
                    warning(Resources.Keys.NotTheEpsgValue_5, IdentifiedObjects.getIdentifierOrName(parent),
                            Constants.EPSG + Constants.DEFAULT_SEPARATOR + expected, GeoKeys.name(key),
                            Constants.EPSG + Constants.DEFAULT_SEPARATOR + code, "");
                }
            }
        }
    }




    //  ╔════════════════════════════════════════════════════════════╗
    //  ║                                                            ║
    //  ║                      GeoKeys parsing                       ║
    //  ║                                                            ║
    //  ╚════════════════════════════════════════════════════════════╝

    /**
     * Decodes all the given GeoTIFF keys, then creates a coordinate reference system.
     * An overview of the key directory structure is given in {@linkplain CRSBuilder class javadoc}.
     * The {@link #description} and {@link #cellGeometry} fields are set as a side-effect.
     * A warning is emitted if any GeoTIFF tags were ignored.
     *
     * @param  source  the {@code keyDirectory}, {@code numericParameters} and {@code asciiParameters} tags.
     * @return the coordinate reference system created from the given GeoTIFF keys, or {@code null} if undefined.
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    public CoordinateReferenceSystem build(final GeoKeysLoader source) throws FactoryException {
        try {
            source.logger = this;
            if (!source.load(geoKeys)) {
                return null;
            }
        } finally {
            source.logger = null;
            this.majorRevision = source.majorRevision;
            this.minorRevision = source.minorRevision;
        }
        /*
         * At this point we finished copying all GeoTIFF keys in the `geoKeys` map. Before to create the CRS,
         * store a few metadata. The first one is an ASCII reference to published documentation on the overall
         * configuration of the GeoTIFF file. In practice it seems to be often the projected CRS name, despite
         * GeoKeys.ProjectedCitation being already for that purpose.
         */
        description = getAsString(GeoKeys.Citation);
        int code = getAsInteger(GeoKeys.RasterType);
        switch (code) {
            case GeoCodes.undefined: break;
            case GeoCodes.RasterPixelIsArea:  cellGeometry = CellGeometry.AREA;  break;
            case GeoCodes.RasterPixelIsPoint: cellGeometry = CellGeometry.POINT; break;
            default: invalidValue(GeoKeys.RasterType, code); break;
        }
        /*
         * First create the main coordinate reference system, as determined by `ModelType`.
         * Then if a vertical CRS exists and the main CRS is not geocentric (in which case
         * adding a vertical CRS would make no sense), create a three-dimensional compound CRS.
         */
        CoordinateReferenceSystem crs = null;
        final int crsType = getAsInteger(GeoKeys.ModelType);
        switch (crsType) {
            case GeoCodes.undefined:           break;
            case GeoCodes.ModelTypeProjected:  crs = createProjectedCRS();  break;
            case GeoCodes.ModelTypeGeocentric: crs = createGeocentricCRS(); break;
            case GeoCodes.ModelTypeGeographic: crs = createGeographicCRS(); break;
            default: warning(Resources.Keys.UnsupportedCoordinateSystemKind_1, crsType); break;
        }
        if (crsType != GeoCodes.ModelTypeGeocentric) try {
            final VerticalCRS vertical = createVerticalCRS();
            if (vertical != null) {
                if (crs == null) {
                    missingValue(GeoKeys.GeodeticCRS);
                } else {
                    crs = getCRSFactory().createCompoundCRS(Map.of(IdentifiedObject.NAME_KEY, crs.getName()), crs, vertical);
                }
            }
        } catch (Exception e) {
            if (crs == null) {
                throw e;
            }
            // A vertical CRS is not strictly needed with GeoTIFF if we have the horizontal component.
            if (!alreadyReported) {
                listeners.warning(e);
            }
        }
        /*
         * At this point we finished parsing all GeoTIFF tags, both for metadata purpose or for CRS construction.
         * Emits a warning for unprocessed GeoTIFF tags. A single warning is emitted for all ignored tags.
         */
        if (!geoKeys.isEmpty()) {
            final StringJoiner joiner = new StringJoiner(", ");
            final Short[] keys = geoKeys.keySet().toArray(Short[]::new);
            Arrays.sort(keys);
            for (final short key : keys) {
                joiner.add(GeoKeys.name(key));
            }
            warning(Resources.Keys.IgnoredGeoKeys_1, joiner.toString());
        }
        return crs;
    }




    //  ╔════════════════════════════════════════════════════════════╗
    //  ║                                                            ║
    //  ║        Geodetic components (datum, ellipsoid, etc.)        ║
    //  ║                                                            ║
    //  ╚════════════════════════════════════════════════════════════╝

    /**
     * Returns a coordinate system (CS) with the same axis directions as the given CS but potentially different units.
     * If a coordinate system exists in the EPSG database with the requested characteristics, that CS will be returned
     * in order to have a richer set of metadata (name, minimal and maximal values, <i>etc</i>). Otherwise an CS with
     * an arbitrary name will be returned.
     *
     * @see CoordinateSystems#replaceLinearUnit(CoordinateSystem, Unit)
     */
    private CartesianCS replaceLinearUnit(final CartesianCS cs, final Unit<Length> unit) throws FactoryException {
        final Integer epsg = CoordinateSystems.getEpsgCode(unit, CoordinateSystems.getAxisDirections(cs));
        if (epsg != null) try {
            return getCSAuthorityFactory().createCartesianCS(epsg.toString());
        } catch (NoSuchAuthorityCodeException e) {
            listeners.warning(e);
        }
        return (CartesianCS) CoordinateSystems.replaceLinearUnit(cs, unit);
    }

    /**
     * Returns a coordinate system (CS) with the same axis directions as the given CS but potentially different units.
     * If a coordinate system exists in the EPSG database with the requested characteristics, that CS will be returned
     * in order to have a richer set of metadata (name, minimal and maximal values, <i>etc</i>). Otherwise an CS with
     * an arbitrary name will be returned.
     *
     * @see CoordinateSystems#replaceAngularUnit(CoordinateSystem, Unit)
     */
    private EllipsoidalCS replaceAngularUnit(final EllipsoidalCS cs, final Unit<Angle> unit) throws FactoryException {
        final Integer epsg = CoordinateSystems.getEpsgCode(unit, CoordinateSystems.getAxisDirections(cs));
        if (epsg != null) try {
            return getCSAuthorityFactory().createEllipsoidalCS(epsg.toString());
        } catch (NoSuchAuthorityCodeException e) {
            listeners.warning(e);
        }
        return (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(cs, unit);
    }

    /**
     * Shortcut for {@code createUnit(…)} when the unit is angular.
     * If no unit is specified, the default is degrees.
     *
     * @param  key  key of the unit to fetch.
     * @return the unit of measurement associated to the given key, or the default value.
     */
    private Unit<Angle> createAngularUnit(final UnitKey key) throws FactoryException {
        return createUnit(key, Angle.class, Units.DEGREE);
    }

    /**
     * Shortcut for {@code createUnit(…)} when the unit is linear.
     * If no unit is specified, the default is metre.
     *
     * @param  key  key of the unit to fetch.
     * @return the unit of measurement associated to the given key, or the default value.
     */
    private Unit<Length> createLinearUnit(final UnitKey key) throws FactoryException {
        return createUnit(key, Length.class, Units.METRE);
    }

    /**
     * Creates units of measurement for projected or geographic coordinate reference systems.
     * The units may either be specified as a standard EPSG recognized unit, or may be user defined.
     * In the first case (EPSG code), the {@code scaleKey} is ignored. In the second case (user-defined),
     * the unit of measurement is defined by a conversion factor from metre or radian base unit.
     *
     * @param  <Q>           the compile-time value of {@code quantity}.
     * @param  key           information about the keys to use for fetching the unit of measurement.
     * @param  quantity      {@link Length} for a linear unit, or {@link Angle} for an angular unit.
     * @param  defaultValue  the unit of measurement to return if no value is found in the GeoTIFF file.
     * @return the unit of measurement associated to the given key, or the default value.
     *
     * @throws NoSuchElementException if {@code codeKey} value is {@link GeoCodes#userDefined} and no value is associated to {@code scaleKey}.
     * @throws NumberFormatException  if a numeric value was stored as a string and cannot be parsed.
     * @throws ClassCastException     if the unit of measurement identified by the EPSG code is not of the expected quantity.
     */
    private <Q extends Quantity<Q>> Unit<Q> createUnit(final UnitKey key,
            final Class<Q> quantity, final Unit<Q> defaultValue) throws FactoryException
    {
        final short scaleKey = key.scaleKey;
        final int epsg = getAsInteger(key.codeKey);
        switch (epsg) {
            case GeoCodes.undefined: {
                return defaultValue;
            }
            case GeoCodes.userDefined: {
                if (scaleKey == 0) return defaultValue;
                return defaultValue.getSystemUnit().multiply(getMandatoryDouble(scaleKey));
            }
            case GeoCodes.missing & 0xFFFF: {
                if (scaleKey != 0) {
                    final double scale = getAsDouble(scaleKey);
                    if (Double.isFinite(scale)) {
                        return defaultValue.getSystemUnit().multiply(scale);
                    }
                }
                return defaultValue;
            }
            default: {
                /*
                 * Unit defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also provide the scale value, we will verify that the value
                 * is consistent with what we would expect for a unit of the given EPSG code.
                 */
                final Unit<Q> unit = getCSAuthorityFactory().createUnit(String.valueOf(epsg)).asType(quantity);
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
     *     <li>a prime meridian value given by {@link GeoKeys#PrimeMeridianLongitude}.</li>
     *   </ul></li>
     * </ul>
     *
     * If no prime-meridian is defined, then the default is Greenwich as per GeoTIFF specification.
     *
     * @param  names  the component names to use if the prime meridian is user-defined.
     * @param  unit   the angular unit of the longitude value relative to Greenwich.
     * @return a prime meridian created from the given {@link Unit} and the above-cited GeoTIFF keys.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private PrimeMeridian createPrimeMeridian(final String[] names, final Unit<Angle> unit) throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.PrimeMeridian);
        switch (epsg) {
            case GeoCodes.undefined:      // If not specified, should default to Greenwich but we nevertheless verify.
            case GeoCodes.userDefined: {
                final double longitude = getAsDouble(GeoKeys.PrimeMeridianLongitude);
                if (Double.isNaN(longitude)) {
                    if (epsg != GeoCodes.undefined) {
                        missingValue(GeoKeys.PrimeMeridianLongitude);
                    }
                } else if (longitude != 0) {
                    /*
                     * If the prime meridian is not Greenwich, create that meridian but do not use the
                     * GeoKeys.GeodeticCitation value (unless it had a sub-element for the prime meridian).
                     * This is because the citation value is for the CRS (e.g. "WGS84") while the prime
                     * meridian names are very different (e.g. "Paris", "Madrid", etc).
                     */
                    return getDatumFactory().createPrimeMeridian(properties(names[PRIMEM]), longitude, unit);
                }
                break;                      // Default to Greenwich.
            }
            default: {
                /*
                 * Prime meridian defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also provide the longitude value, verify that the value is consistent
                 * with what we would expect for a prime meridian of the given EPSG code.
                 */
                final PrimeMeridian pm = getDatumAuthorityFactory().createPrimeMeridian(String.valueOf(epsg));
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
        verify(pm, ReferencingUtilities.getGreenwichLongitude(pm, unit), GeoKeys.PrimeMeridianLongitude, unit);
    }

    /**
     * Creates an ellipsoid from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#Ellipsoid} tag.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#GeodeticCitation},</li>
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
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
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
                    ellipsoid = getDatumFactory().createFlattenedSphere(properties, semiMajor, inverseFlattening, unit);
                } else {
                    /*
                     * If the inverse flattening factory was not defined, fallback on semi-major axis length.
                     * This is a less common way to define ellipsoid (the most common way uses flattening).
                     */
                    final double semiMinor = getMandatoryDouble(GeoKeys.SemiMinorAxis);
                    ellipsoid = getDatumFactory().createEllipsoid(properties, semiMajor, semiMinor, unit);
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
                final Ellipsoid ellipsoid = getDatumAuthorityFactory().createEllipsoid(String.valueOf(epsg));
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
     * Creates a geodetic reference frame from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#GeodeticDatum}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#GeodeticCitation},</li>
     *     <li>all values required by {@link #createPrimeMeridian(String[], Unit)} (optional),</li>
     *     <li>all values required by {@link #createEllipsoid(String[], Unit)}.</li>
     *   </ul></li>
     * </ul>
     *
     * @param  names        the component names to use if the geodetic reference frame is user-defined.
     * @param  angularUnit  the angular unit of the longitude value relative to Greenwich.
     * @param  linearUnit   the linear unit of the ellipsoid semi-axis lengths.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
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
                String name = getOrDefault(names, DATUM);
                if (name == null) {
                    // TODO: see https://issues.apache.org/jira/browse/SIS-536
                    throw new NoSuchElementException(missingValue(GeoKeys.GeodeticCitation));
                }
                final Ellipsoid     ellipsoid = createEllipsoid(names, linearUnit);
                final PrimeMeridian meridian  = createPrimeMeridian(names, angularUnit);
                final GeodeticDatum datum     = getDatumFactory().createGeodeticDatum(properties(name), ellipsoid, meridian);
                name = Strings.toUpperCase(name, Characters.Filter.LETTERS_AND_DIGITS, true);
                lastName = datum.getName();
                try {
                    final CommonCRS predefined = CommonCRS.valueOf(name);
                    if (equalsIgnoreMetadata(predefined.ellipsoid(), ellipsoid) &&
                        equalsIgnoreMetadata(predefined.primeMeridian(), meridian))
                    {
                        return predefined.datum();
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
                final GeodeticDatum datum = getDatumAuthorityFactory().createGeodeticDatum(String.valueOf(epsg));
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




    //  ╔════════════════════════════════════════════════════════════╗
    //  ║                                                            ║
    //  ║          Geodetic CRS (geographic and geocentric)          ║
    //  ║                                                            ║
    //  ╚════════════════════════════════════════════════════════════╝

    /**
     * Splits the {@link GeoKeys#GeodeticCitation} value into its prime meridian, ellipsoid, datum and CRS name components.
     * This method is intended to parse geographic CRS names written like below:
     *
     * <pre class="text">
     *   GCS Name = wgs84|Datum = unknown|Ellipsoid = WGS_1984|Primem = Greenwich|</pre>
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
        final String[] components = (String[]) CharSequences.split(name, GeoCodes.STRING_SEPARATOR);
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
     * Returns the name at the given index if non-null. If that name is null, searches for a name in a sister element
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
     * Creates the main CRS in the case where that CRS is geographic.
     */
    private GeographicCRS createGeographicCRS() throws FactoryException {
        return createGeographicCRS(true, createAngularUnit(UnitKey.ANGULAR));
    }

    /**
     * Creates a geographic CRS from an EPSG code or from user-defined parameters.
     * The GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#GeodeticCRS}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a prime meridian value given by {@link GeoKeys#PrimeMeridianLongitude},</li>
     *     <li>a CRS name given by {@link GeoKeys#GeodeticCitation},</li>
     *     <li>a datum definition.</li>
     *   </ul></li>
     *   <li>A unit code given by {@link GeoKeys#GeogAngularUnits} (optional).</li>
     *   <li>A unit scale factor given by {@link GeoKeys#GeogAngularUnitSize} (optional).</li>
     * </ul>
     *
     * @param  isImageCRS   whether to force longitude before latitude axis.
     * @param  angularUnit  the angular unit of the latitude and longitude values.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createGeodeticDatum(String[], Unit, Unit)
     */
    private GeographicCRS createGeographicCRS(final boolean isImageCRS, final Unit<Angle> angularUnit) throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.GeodeticCRS);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.GeodeticCRS));
            }
            case GeoCodes.userDefined: {
                /*
                 * Creates the geodetic reference frame, then a geographic CRS assuming (longitude, latitude) axis order.
                 * We use the coordinate system of CRS:84 as a template and modify its unit of measurement if needed.
                 */
                final String[] names = splitName(getAsString(GeoKeys.GeodeticCitation));
                final Unit<Length> linearUnit = createLinearUnit(UnitKey.LINEAR);
                final GeodeticDatum datum = createGeodeticDatum(names, angularUnit, linearUnit);
                EllipsoidalCS cs = CommonCRS.defaultGeographic().getCoordinateSystem();
                if (!Units.DEGREE.equals(angularUnit)) {
                    cs = replaceAngularUnit(cs, angularUnit);
                }
                final GeographicCRS crs = getCRSFactory().createGeographicCRS(properties(getOrDefault(names, GCRS)), datum, cs);
                lastName = crs.getName();
                return crs;
            }
            default: {
                /*
                 * Geographic CRS defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are consistent
                 * with what we would expect for a CRS of the given EPSG code.
                 */
                GeographicCRS crs = getCRSAuthorityFactory().createGeographicCRS(String.valueOf(epsg));
                if (isImageCRS) {
                    crs = DefaultGeographicCRS.castOrCopy(crs).forConvention(AxesConvention.DISPLAY_ORIENTED);
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
    private void verify(final GeodeticCRS crs, final Unit<Angle> angularUnit) throws FactoryException {
        /*
         * Note: current createUnit(…) implementation does not allow us to distinguish whether METRE ou DEGREE units
         * were specified in the GeoTIFF file or if we got the default values. We do not compare units for that reason.
         */
        final Unit<Length> linearUnit = createLinearUnit(UnitKey.LINEAR);
        final GeodeticDatum datum = DatumOrEnsemble.asDatum(crs);
        verifyIdentifier(crs, datum, GeoKeys.GeodeticDatum);
        verify(datum, angularUnit, linearUnit);
        geoKeys.remove(GeoKeys.GeodeticCitation);
    }

    /**
     * Creates a geocentric CRS from user-defined parameters.
     * The GeoTIFF values used by this method are the same as the ones used by {@code createGeographicCRS(…)}.
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createGeodeticDatum(String[], Unit, Unit)
     */
    private GeodeticCRS createGeocentricCRS() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.GeodeticCRS);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.GeodeticCRS));
            }
            case GeoCodes.userDefined: {
                /*
                 * Creates the geodetic reference frame, then a geocentric CRS. We use the coordinate system of
                 * the WGS84 geocentric CRS as a template and modify its unit of measurement if needed.
                 */
                final String[] names = splitName(getAsString(GeoKeys.GeodeticCitation));
                final Unit<Length> linearUnit = createLinearUnit(UnitKey.LINEAR);
                final Unit<Angle> angularUnit = createAngularUnit(UnitKey.ANGULAR);
                final GeodeticDatum datum = createGeodeticDatum(names, angularUnit, linearUnit);
                CartesianCS cs = (CartesianCS) CommonCRS.WGS84.geocentric().getCoordinateSystem();
                if (!Units.METRE.equals(linearUnit)) {
                    cs = replaceLinearUnit(cs, linearUnit);
                }
                // TODO: datum should be DatumEnsemble for some case such as EPSG:4326.
                final GeodeticCRS crs = getCRSFactory().createGeodeticCRS(properties(getOrDefault(names, GCRS)), datum, null, cs);
                lastName = crs.getName();
                return crs;
            }
            default: {
                /*
                 * Geocentric CRS defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are consistent
                 * with what we would expect for a CRS of the given EPSG code.
                 */
                final GeodeticCRS crs = getCRSAuthorityFactory().createGeodeticCRS(String.valueOf(epsg));
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
    private void verify(final GeodeticCRS crs) throws FactoryException {
        /*
         * Note: current createUnit(…) implementation does not allow us to distinguish whether METRE ou DEGREE units
         * were specified in the GeoTIFF file or if we got the default values. We do not compare units for that reason.
         */
        final Unit<Length> linearUnit = createLinearUnit(UnitKey.LINEAR);
        final Unit<Angle> angularUnit = createAngularUnit(UnitKey.ANGULAR);
        final GeodeticDatum datum = DatumOrEnsemble.asDatum(crs);
        verifyIdentifier(crs, datum, GeoKeys.GeodeticDatum);
        verify(datum, angularUnit, linearUnit);
    }




    //  ╔════════════════════════════════════════════════════════════╗
    //  ║                                                            ║
    //  ║                       Projected CRS                        ║
    //  ║                                                            ║
    //  ╚════════════════════════════════════════════════════════════╝

    /**
     * Map projection parameters to be considered as aliases. This table is used for reading GeoTIFF files
     * that are not really well-formed, but for which we can reasonably guess what was the producer intent
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
     * but we sometimes see {@code "NatOriginLong"} parameter used for the {@code "CT_LambertConfConic_2SP"} projection.
     * Semantically those two parameters are for two different things but mathematically they are used in the same way.
     * Those "synonymous" will be invisible to the user; the map projection that (s)he will get uses the names defined
     * in the descriptor (not in the GeoTIFF file).
     */
    private static void aliases(final Map<Integer,String> mapping) {
        for (final short[] codes : PARAMETER_ALIASES) {
            for (int i=0; i<codes.length; i++) {
                final String name = mapping.get(Short.toUnsignedInt(codes[i]));
                if (name != null) {
                    for (int j=0; j<codes.length; j++) {
                        if (j != i) {
                            mapping.putIfAbsent(Short.toUnsignedInt(codes[j]), name);
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
     *   <li>A code given by {@link GeoKeys#ProjectedCRS}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#ProjectedCitation},</li>
     *     <li>a {@link CoordinateOperation} given by {@link GeoKeys#Projection},</li>
     *     <li>an {@link OperationMethod} given by {@link GeoKeys#ProjMethod}.</li>
     *   </ul></li>
     *   <li>A unit code given by {@link GeoKeys#ProjLinearUnits} (optional).</li>
     *   <li>A unit scale factor given by {@link GeoKeys#ProjLinearUnitSize} (optional).</li>
     * </ul>
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     *
     * @see #createGeographicCRS(boolean, Unit)
     * @see #createConversion(String, Unit, Unit)
     */
    private ProjectedCRS createProjectedCRS() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.ProjectedCRS);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.ProjectedCRS));
            }
            case GeoCodes.userDefined: {
                /*
                 * If the CRS is user-defined, we have to parse many components (base CRS, datum, unit, etc.)
                 * and build the projected CRS from them. Note that some GeoTIFF files put the projection name
                 * in the Citation key instead of ProjectedCitation.
                 */
                String name = getAsString(GeoKeys.ProjectedCitation);
                if (name == null) {
                    name = getAsString(GeoKeys.Citation);
                    // Note that Citation has been removed from the map, so it will not be used by `complete(MetadataBuilder)`.
                }
                final Unit<Length>  linearUnit  = createLinearUnit(UnitKey.PROJECTED);
                final Unit<Angle>   angularUnit = createAngularUnit(UnitKey.ANGULAR);
                final GeographicCRS baseCRS     = createGeographicCRS(false, angularUnit);
                final Conversion    projection  = createConversion(name, angularUnit, linearUnit);
                CartesianCS cs = getStandardProjectedCS();
                if (!Units.METRE.equals(linearUnit)) {
                    cs = replaceLinearUnit(cs, linearUnit);
                }
                final ProjectedCRS crs = getCRSFactory().createProjectedCRS(properties(name), baseCRS, projection, cs);
                lastName = crs.getName();
                return crs;
            }
            default: {
                /*
                 * Projected CRS defined by an EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are consistent
                 * with what we would expect for a CRS of the given EPSG code.
                 */
                ProjectedCRS crs = getCRSAuthorityFactory().createProjectedCRS(String.valueOf(epsg));
                crs = DefaultProjectedCRS.castOrCopy(crs).forConvention(AxesConvention.DISPLAY_ORIENTED);
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
        final Unit<Length> linearUnit  = createLinearUnit(UnitKey.PROJECTED);
        final Unit<Angle>  angularUnit = createAngularUnit(UnitKey.ANGULAR);
        final GeodeticCRS  baseCRS = crs.getBaseCRS();
        verifyIdentifier(crs, baseCRS, GeoKeys.GeodeticCRS);
        verify(baseCRS, angularUnit);
        final Conversion projection = crs.getConversionFromBase();
        verifyIdentifier(crs, projection, GeoKeys.Projection);
        verify(projection, angularUnit, linearUnit);
        getAsString(GeoKeys.ProjectedCitation);
    }

    /**
     * Returns the code of the operation method to request.
     * This method tries to resolves some ambiguities in the way operation methods are defined.
     * For example there is an ambiguity between Polar Stereographic (variant A) and (variant B).
     *
     * @param  code  value of {@code getMandatoryString(GeoKeys.ProjMethod)}.
     * @return the method code in the GeoTIFF or EPSG name space.
     */
    private String methodCode(final String code) {
        try {
            switch (Integer.parseInt(code)) {
                case GeoCodes.PolarStereographic: {
                    /*
                     * Some GeoTIFF producers wrongly interpreted GeoTIFF projection #15
                     * as "Polar Stereographic (Variant A)" while it should be variant B.
                     * In those files, the "Latitude of true scale" parameter is wrongly
                     * named "Latitude of natural origin" because the former is a member
                     * of variant A while the latter is a member of variant B. This code
                     * does the substitution.
                     *
                     * https://issues.apache.org/jira/browse/SIS-572
                     */
                    if (geoKeys.containsKey(GeoKeys.StdParallel1)) {
                        break;      // Assume a valid map projection.
                    }
                    Object value = geoKeys.get(GeoKeys.ScaleAtNatOrigin);
                    if (value instanceof Number && ((Number) value).doubleValue() != 1) {
                        return Constants.EPSG + ':' + PolarStereographicA.IDENTIFIER;
                    }
                    moveParameter(PolarStereographicB.NAME, GeoKeys.NatOriginLat, GeoKeys.StdParallel1);
                    moveParameter(PolarStereographicB.NAME, GeoKeys.ScaleAtNatOrigin, (short) 0);
                    break;
                }
                // More cases may be added in the future.
            }
        } catch (NumberFormatException e) {
            invalidValue(GeoKeys.ProjMethod, code);
            return code;
        }
        return Constants.GEOTIFF + ':' + code;
    }

    /**
     * Creates a defining conversion from an EPSG code or from user-defined parameters.
     *
     * @param  angularUnit  the angular unit of the latitude and longitude values.
     * @param  linearUnit   the linear unit of easting and northing values.
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
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
                final Unit<Angle>         azimuthUnit = createAngularUnit(UnitKey.AZIMUTH);
                final String              code        = methodCode(getMandatoryString(GeoKeys.ProjMethod));
                final OperationMethod     method      = findOperationMethod(code);
                final ParameterValueGroup parameters  = method.getParameters().createValue();
                final Map<Integer,String> toNames     = ReferencingUtilities.identifierToName(parameters.getDescriptor(), Citations.GEOTIFF);
                final Map<Object,Number>  paramValues = new HashMap<>();    // Keys: [String|Short] instances for [known|unknown] parameters.
                final Map<Short,Unit<?>>  deferred    = new HashMap<>();    // Only unknown parameters.
                final Iterator<Map.Entry<Short,Object>> it = geoKeys.entrySet().iterator();
                while (it.hasNext()) {
                    final Unit<?> unit;
                    final Map.Entry<Short,?> entry = it.next();
                    final Short key = entry.getKey();
                    switch (UnitKey.ofProjectionParameter(key)) {
                        case RATIO:     unit = Units.UNITY; break;
                        case PROJECTED: unit = linearUnit;  break;
                        case ANGULAR:   unit = angularUnit; break;
                        case AZIMUTH:   unit = azimuthUnit; break;
                        default: continue;
                    }
                    final Number value = (Number) entry.getValue();
                    it.remove();
                    final String paramName = toNames.get(Short.toUnsignedInt(key));
                    if (paramName != null) {
                        paramValues.put(paramName, value);
                        parameters.parameter(paramName).setValue(value.doubleValue(), unit);
                    } else {
                        paramValues.put(key, value);
                        deferred.put(key, unit);
                    }
                }
                /*
                 * At this point we finished to set all known map projection parameters. Sometimes GeoTIFF files
                 * set the same parameter many times using different names as a safety for GeoTIFF readers that
                 * expect wrong parameters. If this is the case, verify that the parameter values are consistent.
                 * It is also possible that we found new parameters (actually parameters using the wrong names).
                 */
                if (!deferred.isEmpty()) {
                    aliases(toNames);
                    for (final Map.Entry<Short,Unit<?>> entry : deferred.entrySet()) {
                        final Short key = entry.getKey();
                        String paramName = toNames.get(Short.toUnsignedInt(key));
                        if (paramName == null) {
                            paramName = GeoKeys.name(key);
                            throw new ParameterNotFoundException(Errors.forLocale(getLocale())
                                        .getString(Errors.Keys.UnexpectedParameter_1, paramName), paramName);
                        }
                        final Number value  = paramValues.get(key);
                        final Number actual = paramValues.putIfAbsent(paramName, value);
                        if (actual == null) {
                            parameters.parameter(paramName).setValue(value.doubleValue(), entry.getValue());
                        } else if (!actual.equals(value)) {
                            warning(Resources.Keys.InconsistentMapProjParameter_4, paramName, actual, GeoKeys.name(key), value);
                        }
                    }
                }
                final Conversion c = getCoordinateOperationFactory().createDefiningConversion(properties(name), method, parameters);
                lastName = c.getName();
                return c;
            }
            default: {
                /*
                 * Conversion defined by EPSG code. In principle we should just use the EPSG code.
                 * But if the file also defines the components, verify that those components are
                 * consistent with what we would expect for a conversion of the given EPSG code.
                 */
                final Conversion projection = (Conversion)
                        getCoordinateOperationAuthorityFactory().createCoordinateOperation(String.valueOf(epsg));
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
        final Unit<Angle> azimuthUnit = createAngularUnit(UnitKey.AZIMUTH);
        final String code = getAsString(GeoKeys.ProjMethod);
        if (code != null) {
            /*
             * Compare the identifier of the map projection declared in the GeoTIFF file with the identifier
             * of the projection used by the EPSG geodetic dataset. The two cases may use different variants
             * of the same projection (e.g. Mercator variant A, B and C), so the GeoTIFF code may be missing
             * in the projection given in argument.
             */
            OperationMethod method = projection.getMethod();
            Identifier id = IdentifiedObjects.getIdentifier(method, Citations.GEOTIFF);
            final boolean isSameProjection = (id != null) && code.equals(id.getCode());
            if (!isSameProjection) {
                if (id != null) {
                    final String key    = GeoKeys.name(GeoKeys.ProjMethod);
                    final String parent = IdentifiedObjects.getIdentifierOrName(projection);
                    warning(Resources.Keys.NotTheEpsgValue_5, parent, id.getCode(), key, code, "");
                }
                method = findOperationMethod(methodCode(code));
            }
            /*
             * Compare the parameter values with the ones declared in the EPSG geodetic dataset.
             */
            final Parameters parameters = Parameters.castOrWrap(projection.getParameterValues());
            for (final GeneralParameterDescriptor desc : method.getParameters().descriptors()) {
                id = IdentifiedObjects.getIdentifier(desc, Citations.GEOTIFF);
                if (id != null) {
                    final short key;
                    try {
                        key = Short.parseShort(id.getCode());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    final Unit<?> unit;
                    switch (UnitKey.ofProjectionParameter(key)) {
                        case RATIO:     unit = Units.UNITY; break;
                        case PROJECTED: unit = linearUnit;  break;
                        case ANGULAR:   unit = angularUnit; break;
                        case AZIMUTH:   unit = azimuthUnit; break;
                        default: continue;
                    }
                    double value = Double.NaN;
                    try {
                        @SuppressWarnings("unchecked")                          // Because we catch ClassCastException.
                        var p = (ParameterDescriptor<? extends Number>) desc;
                        value = parameters.doubleValue(p, unit);
                    } catch (ClassCastException | ParameterNotFoundException e) {
                        if (isSameProjection) {
                            warning(Resources.Keys.UnexpectedParameter_2, code, GeoKeys.name(key));
                        }
                    }
                    verify(projection, value, key, unit);
                }
            }
        }
    }




    //  ╔════════════════════════════════════════════════════════════╗
    //  ║                                                            ║
    //  ║                        Vertical CRS                        ║
    //  ║                                                            ║
    //  ╚════════════════════════════════════════════════════════════╝

    /**
     * Creates a vertical datum.
     */
    private VerticalDatum createVerticalDatum() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.VerticalDatum);
        switch (epsg) {
            case GeoCodes.undefined: {
                alreadyReported = true;
                throw new NoSuchElementException(missingValue(GeoKeys.VerticalDatum));
            }
            case GeoCodes.userDefined: {
                return CommonCRS.Vertical.OTHER_SURFACE.datum();
            }
            default: {
                return getDatumAuthorityFactory().createVerticalDatum(String.valueOf(epsg));
            }
        }
    }

    /**
     * Creates an optional vertical CRS, or returns {@code null} if no vertical CRS definition is found.
     * This method is different from the other {@code createFooCRS()} methods in that the vertical CRS
     * may be defined <em>in addition</em> of another CRS. Some GeoTIFF values used by this method are:
     *
     * <ul>
     *   <li>A code given by {@link GeoKeys#Vertical}.</li>
     *   <li>If above code is {@link GeoCodes#userDefined}, then:<ul>
     *     <li>a name given by {@link GeoKeys#VerticalCitation},</li>
     *     <li>a {@link VerticalDatum} given by {@link GeoKeys#VerticalDatum}.</li>
     *   </ul></li>
     *   <li>A unit code given by {@link GeoKeys#VerticalUnits} (optional).</li>
     * </ul>
     *
     * @throws NoSuchElementException if a mandatory value is missing.
     * @throws NumberFormatException if a numeric value was stored as a string and cannot be parsed.
     * @throws ClassCastException if an object defined by an EPSG code is not of the expected type.
     * @throws FactoryException if an error occurred during objects creation with the factories.
     */
    private VerticalCRS createVerticalCRS() throws FactoryException {
        final int epsg = getAsInteger(GeoKeys.Vertical);
        switch (epsg) {
            case GeoCodes.undefined: {
                return null;
            }
            case GeoCodes.userDefined: {
                final String name = getAsString(GeoKeys.VerticalCitation);
                final VerticalDatum datum = createVerticalDatum();
                final Unit<Length> unit = createLinearUnit(UnitKey.VERTICAL);
                VerticalCS cs = CommonCRS.Vertical.OTHER_SURFACE.crs().getCoordinateSystem();
                if (!Units.METRE.equals(unit)) {
                    cs = (VerticalCS) CoordinateSystems.replaceLinearUnit(cs, unit);
                }
                return getCRSFactory().createVerticalCRS(properties(name), datum, cs);
            }
            default: {
                return getCRSAuthorityFactory().createVerticalCRS(String.valueOf(epsg));
            }
        }
    }

    /**
     * Returns the locale to use for error messages.
     */
    @Override
    public final Locale getLocale() {
        return listeners.getLocale();
    }

    /**
     * Returns a string representation of the keys and associated values in this {@code CRSBuilder}.
     */
    @Override
    public final String toString() {
        final var buffer = new StringBuilder("GeoTIFF keys ").append(majorRevision).append('.')
                .append(minorRevision).append(" in ").append(listeners.getSourceName()).append(System.lineSeparator());
        final var table = new TableAppender(buffer, " ");
        for (Map.Entry<Short,Object> entry : geoKeys.entrySet()) {
            final short key = entry.getKey();
            table.append(String.valueOf(key)).nextColumn();
            table.append(GeoKeys.name(key)).nextColumn();
            table.append(" = ").append(String.valueOf(entry.getValue())).nextLine();
        }
        try {
            table.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);          // Should never happen since we wrote to a StringBuffer.
        }
        return buffer.toString();
    }
}
