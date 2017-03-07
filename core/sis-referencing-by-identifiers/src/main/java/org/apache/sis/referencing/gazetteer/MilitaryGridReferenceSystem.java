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
package org.apache.sis.referencing.gazetteer;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.ConcurrentModificationException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.TransverseMercator;
import org.apache.sis.internal.referencing.provider.PolarStereographicA;
import org.apache.sis.internal.gazetteer.Resources;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.sql.MetadataSource;
import org.apache.sis.metadata.sql.MetadataStoreException;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.Latitude;

// Branch-dependent imports
import org.opengis.metadata.citation.Party;
import org.opengis.referencing.gazetteer.Location;
import org.opengis.referencing.gazetteer.LocationType;


/**
 * The Military Grid Reference System (MGRS).
 * The MGRS is the geocoordinate standard used by NATO militaries for locating points on the earth.
 * It is based on the Universal Transverse Mercator (UTM) and the Universal Polar Stereographic (UPS) projections.
 * Despite its name, MGRS is used not only for military purposes; it is used also for organizing Earth Observation
 * data in directory trees for example.
 *
 * <p>MGRS references are sequences of digits and letters like “4Q FJ 12345 67890” (a reference with 1 metre accuracy),
 * optionally written with reduced resolution as in “4Q FJ 123 678” (a reference with 100 metres accuracy).
 * Those references form a hierarchy of 3 {@linkplain ModifiableLocationType location types}:</p>
 *
 * <blockquote>
 *   <b>Grid zone designator</b>         (example: “4Q”)<br>
 *     └─<b>100 km square identifier</b> (example: “FJ”)<br>
 *         └─<b>Grid coordinate</b>      (example: “12345 67890”)<br>
 * </blockquote>
 *
 * <p>Conversions between MGRS references and spatial coordinates can be performed by the {@link Coder Coder} inner class.
 * The result of decoding a MGRS reference is an envelope rather than a point, but a representative point can be obtained.
 * The encoding and decoding processes take in account Norway and Svalbard special cases (they have wider UTM zones for
 * historical reasons).</p>
 *
 * <div class="note"><b>Example:</b>
 * the following code:
 *
 * {@preformat java
 *     MilitaryGridReferenceSystem system = new MilitaryGridReferenceSystem();
 *     MilitaryGridReferenceSystem.Coder coder = system.createCoder();
 *     Location loc = coder.decode("32TNL83");
 *     System.out.println(loc);
 * }
 *
 * should display (locale may vary):
 *
 * {@preformat text
 *     ┌─────────────────────────────────────────────────────────────────┐
 *     │ Location type:               Grid coordinate                    │
 *     │ Geographic identifier:       32TNL83                            │
 *     │ West bound:                    580,000 m    —     9°57′00″E     │
 *     │ Representative value:          585,000 m    —    10°00′36″E     │
 *     │ East bound:                    590,000 m    —    10°04′13″E     │
 *     │ South bound:                 4,530,000 m    —    40°54′58″N     │
 *     │ Representative value:        4,535,000 m    —    40°57′42″N     │
 *     │ North bound:                 4,540,000 m    —    41°00′27″N     │
 *     │ Coordinate reference system: WGS 84 / UTM zone 32N              │
 *     │ Administrator:               North Atlantic Treaty Organization │
 *     └─────────────────────────────────────────────────────────────────┘
 * }
 * </div>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus thread-safe.
 * However the {@link Coder Coder} instances performing conversions between references and coordinates
 * are not thread-safe; it is recommended to create a new {@code Coder} instance for each thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see CommonCRS#universal(double, double)
 * @see <a href="https://en.wikipedia.org/wiki/Military_Grid_Reference_System">Military Grid Reference System on Wikipedia</a>
 */
public class MilitaryGridReferenceSystem extends ReferencingByIdentifiers {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8337394374656125471L;

    /**
     * Height of latitude bands, in degrees.
     * Those bands are labeled from {@code 'C'} to {@code 'X'} inclusive, excluding {@code 'I'} and {@code 'O'}.
     */
    static final double LATITUDE_BAND_HEIGHT = 8;

    /**
     * Size of the 100 kilometres squares, in metres.
     */
    static final double GRID_SQUARE_SIZE = 100_000;

    /**
     * Number of letters available for grid rows. Those letters are "ABCDEFGHJKLMNPQRSTUV" (starting at letter
     * F for zones of even number), repeated in a cycle. Each row is {@value #GRID_SQUARE_SIZE} metres height.
     */
    static final int GRID_ROW_COUNT = 20;

    /**
     * The number of digits in a one-meter precision when formatting MGRS references.
     *
     * <p><b>Invariant:</b> the following relationship must hold:
     * {@code GRID_SQUARE_SIZE == Math.pow(10, METRE_PRECISION_DIGITS)}
     */
    static final int METRE_PRECISION_DIGITS = 5;

    /**
     * The first of the two letters ({@code 'I'} and {@code 'O'}) excluded in MGRS notation.
     * This letter and all following letters shall be shifted by one character. Example:
     *
     * {@preformat java
     *     char band = ...;
     *     if (band >= EXCLUDE_I) {
     *         band++;
     *         if (band >= EXCLUDE_O) band++;
     *     }
     * }
     *
     * or equivalently:
     *
     * {@preformat java
     *     char band = ...;
     *     if (band >= EXCLUDE_I && ++band >= EXCLUDE_O) band++;
     * }
     */
    private static final char EXCLUDE_I = 'I';

    /**
     * The second of the two letters ({@code 'I'} and {@code 'O'}) excluded in MGRS notation.
     */
    private static final char EXCLUDE_O = 'O';

    /**
     * The column letters used in Polar Stereographic Projections.
     * They are letters A to Z but omitting I, O, D, E, M, N, V, W.
     */
    private static final byte[] POLAR_COLUMNS = {
        'A','B','C','F','G','H','J','K','L','P','Q','R','S','T','U','X','Y','Z'
    };

    /**
     * The object to use for computing zone number and central meridian. This is a static final
     * field in current Apache SIS version, but could become configurable in a future version.
     */
    private static final TransverseMercator.Zoner ZONER = TransverseMercator.Zoner.UTM;

    /**
     * The datum to which to transform the coordinate before formatting the MGRS reference.
     * Only the datums enumerated in {@link CommonCRS} are currently supported.
     */
    final CommonCRS datum;

    /**
     * Whether {@link Encoder} should infer the datum from the given coordinates
     * instead than using {@link #datum}.
     */
    final boolean avoidDatumChange;

    /**
     * Value to add to the row number in order to have the "A" letter on the northernmost value on Greenwich meridian of
     * the Universal Polar Stereographic (UPS) South projection. Value is initially zero and computed when first needed.
     * This is derived from the bottom of the 100 kilometres square labeled "A" in Grid Zone Designations A and B.
     */
    private transient short southOffset;

    /**
     * Value to add to the row number in order to have the "A" letter on the southernmost value on Greenwich meridian of
     * the Universal Polar Stereographic (UPS) North projection. Value is initially zero and computed when first needed.
     * This is derived from the bottom of the 100 kilometres square labeled "A" in Grid Zone Designations Y and Z.
     */
    private transient short northOffset;

    /**
     * Creates a new Military Grid Reference System (MGRS) using the default datum.
     * The current Apache SIS version uses the {@linkplain CommonCRS#WGS84 WGS84} datum,
     * but this choice may change in the future if there is a need to adapt to new MGRS specifications.
     */
    public MilitaryGridReferenceSystem() {
        super(properties(), types());
        datum = CommonCRS.WGS84;
        avoidDatumChange = false;
    }

    /**
     * Creates a new Military Grid Reference System (MGRS) using the specified datum.
     * Only the datums enumerated in {@link CommonCRS} are currently supported.
     *
     * @param  properties  the properties to be given to the reference system.
     * @param  datum       the datum to which to transform coordinates before formatting the MGRS references,
     *                     or {@code null} for inferring the datum from the CRS associated to each coordinate.
     */
    public MilitaryGridReferenceSystem(final Map<String,?> properties, final CommonCRS datum) {
        super(properties, types());
        this.datum = (datum != null) ? datum : CommonCRS.WGS84;
        avoidDatumChange = (datum == null);
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static Map<String,?> properties() {
        Party party;
        try {
            party = MetadataSource.getProvided().lookup(Party.class, "NATO");
        } catch (MetadataStoreException e) {
            party = null;
            Logging.unexpectedException(Logging.getLogger(Modules.REFERENCING_BY_IDENTIFIERS),
                    MilitaryGridReferenceSystem.class, "<init>", e);
        }
        final Map<String,Object> properties = new HashMap<>(6);
        properties.put(NAME_KEY, new NamedIdentifier(null, "NATO", Resources.formatInternational(Resources.Keys.MGRS), null, null));
        properties.put(DOMAIN_OF_VALIDITY_KEY, Extents.WORLD);
        properties.put(THEME_KEY, Vocabulary.formatInternational(Vocabulary.Keys.Mapping));
        properties.put(OVERALL_OWNER_KEY, party);
        return properties;
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static LocationType[] types() {
        final ModifiableLocationType gzd    = new ModifiableLocationType(Resources.formatInternational(Resources.Keys.GridZoneDesignator));
        final ModifiableLocationType square = new ModifiableLocationType(Resources.formatInternational(Resources.Keys.SquareIdentifier100));
        final ModifiableLocationType coord  = new ModifiableLocationType(Resources.formatInternational(Resources.Keys.GridCoordinates));
        gzd   .addIdentification(Vocabulary.formatInternational(Vocabulary.Keys.Code));
        coord .addIdentification(Vocabulary.formatInternational(Vocabulary.Keys.Coordinate));
        square.addParent(gzd);
        coord .addParent(square);
        return new LocationType[] {gzd};
    }

    /**
     * Returns the first location types, which should be the grid zone identifier.
     */
    final LocationType rootType() {
        return super.getLocationTypes().get(0);     // Use super.… for protecting from method overriding.
    }

    /**
     * Returns the value to add to the row number in order to have the "A" letter on the southernmost or
     * northernmost value on Greenwich meridian of the Universal Polar Stereographic (UPS) projection.
     * If {@code south} is {@code true}, then this is computed from the northernmost value of UPS South;
     * otherwise this is computed from the southernmost value of UPS North. This value is derived from
     * the bottom of the 100 kilometres square labeled "A" in Grid Zone Designations A, B, Y and Z.
     */
    final int polarOffset(final boolean south) throws TransformException {
        // No need to synchronized; not a big deal if computed twice.
        short origin = south ? southOffset : northOffset;
        if (origin == 0) {
            final DirectPosition2D position = new DirectPosition2D(
                    south ? TransverseMercator.Zoner.SOUTH_BOUNDS
                          : TransverseMercator.Zoner.NORTH_BOUNDS, 0);
            double northing = datum.universal(position.x * 1.01, position.y).getConversionFromBase()
                                   .getMathTransform().transform(position, position).getOrdinate(1);
            if (south) {
                northing = 2*PolarStereographicA.UPS_SHIFT - northing;
            }
            northing = Math.floor(northing / GRID_SQUARE_SIZE);
            origin = (short) northing;
            if (origin != northing) {                       // Paranoiac check (should never happen).
                throw new GazetteerException();
            }
            if (south) {
                southOffset = origin;
            } else {
                northOffset = origin;
            }
        }
        return origin;
    }

    /**
     * Returns a new object performing conversions between {@code DirectPosition} and MGRS references.
     * The returned object is <strong>not</strong> thread-safe; a new instance must be created for
     * each thread, or synchronization must be applied by the caller.
     *
     * @return a new object performing conversions between {@link DirectPosition} and MGRS references.
     */
    public Coder createCoder() {
        return new Coder();
    }

    /**
     * Conversions between direct positions and references in the Military Grid Reference System (MGRS).
     * Each {@code Coder} instance can read references at arbitrary precision, but formats at the
     * {@linkplain #setPrecision specified precision}.
     * The same {@code Coder} instance can be reused for reading or writing many MGRS references.
     *
     * <p>See the {@link MilitaryGridReferenceSystem} enclosing class for usage example.</p>
     *
     * <div class="section">Immutability and thread safety</div>
     * This class is <strong>not</strong> thread-safe. A new instance must be created for each thread,
     * or synchronization must be applied by the caller.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    public class Coder {
        /**
         * Number of digits to use for formatting the numerical part of a MGRS reference.
         *
         * @see #getPrecision()
         */
        private byte digits;

        /**
         * The separator to insert between each component of the MGRS identifier, or an empty string if none.
         */
        private String separator;

        /**
         * Same separator, but without leading and trailing spaces.
         */
        private String trimmedSeparator;

        /**
         * Cached information needed for building a MGRS reference from a direct position in the given CRS.
         */
        private final Map<CoordinateReferenceSystem,Encoder> encoders;

        /**
         * Temporary positions used for encoding. References are kept for avoiding
         * to recreate those temporary objects for every reference to parse or format.
         */
        transient DirectPosition normalized, geographic;

        /**
         * A buffer where to create reference, to be reused for each new reference.
         */
        final StringBuilder buffer;

        /**
         * Creates a new coder initialized to the default precision.
         */
        protected Coder() {
            digits     = METRE_PRECISION_DIGITS;     // 1 metre precision.
            separator  = trimmedSeparator = "";
            buffer     = new StringBuilder(18);      // Length of "4 Q FJ 12345 67890" sample value.
            encoders   = new IdentityHashMap<>();
        }

        /**
         * Returns the reference system for which MGRS references will be encoded or decoded.
         */
        final MilitaryGridReferenceSystem getReferenceSystem() {
            return MilitaryGridReferenceSystem.this;
        }

        /**
         * Returns the precision of the references formatted by this coder.
         * This method returns one of the following values:
         *
         * <table class="sis">
         *   <caption>MGRS reference precisions</caption>
         *   <tr><th>Precision (m)</th>             <th>Reference example</th></tr>
         *   <tr><td style="text-align:right">1</td> <td>4 Q FJ 12345 67890</td></tr>
         *   <tr><td style="text-align:right">10</td> <td>4 Q FJ 1234 6789</td></tr>
         *   <tr><td style="text-align:right">100</td> <td>4 Q FJ 123 678</td></tr>
         *   <tr><td style="text-align:right">1000</td> <td>4 Q FJ 12 67</td></tr>
         *   <tr><td style="text-align:right">10 000</td> <td>4 Q FJ 1 6</td></tr>
         *   <tr><td style="text-align:right">100 000</td> <td>4 Q FJ</td></tr>
         *   <tr><td style="text-align:right">(approximative) 1 000 000</td> <td>4 Q</td></tr>
         * </table>
         *
         * Values smaller than 1 (e.g. 0.01 for a centimetre precision) may also be returned
         * if that value has been {@linkplain #setPrecision(double) explicitely set},
         * but sub-metric precision are usually not used with MGRS.
         *
         * @return precision of formatted references in metres.
         */
        public double getPrecision() {
            return MathFunctions.pow10(METRE_PRECISION_DIGITS - digits);
        }

        /**
         * Sets the desired precision of the references formatted by this coder.
         * This method rounds the given precision to one of the power of 10
         * documented in the {@link #getPrecision()} method.
         *
         * @param  precision  the desired precision in metres.
         */
        public void setPrecision(final double precision) {
            final double p = Math.floor(Math.log10(precision));
            if (!Double.isFinite(p)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "precision", precision));
            }
            // The -3 is an arbitrary limit to millimetre precision.
            int n = Math.max(-3, Math.min(METRE_PRECISION_DIGITS + 1, (int) p));
            digits = (byte) (METRE_PRECISION_DIGITS - n);
        }

        /**
         * Returns the separator to insert between each component of the MGRS identifier.
         * Components are zone number, latitude band, 100 000-metres square identifier and numerical values.
         * By default the separator is an empty string, which produce references like "4QFJ12345678".
         *
         * @return the separator to insert between each component of the MGRS identifier, or an empty string if none.
         */
        public String getSeparator() {
            return separator;
        }

        /**
         * Sets the separator to insert between each component of the MGRS identifier.
         * Components are zone number, latitude band, 100 000-metres square identifier and numerical values.
         * By default the separator is an empty string, which produce references like "4QFJ12345678".
         * If the separator is set to a space, then the references will be formatted like "4 Q FJ 1234 5678".
         *
         * <p>Note that a MGRS reference is normally written as an entity without spaces, parentheses, dashes,
         * or decimal points. Invoking this method with a non-empty separator produces non-conform MGRS, but
         * is sometime convenient for readability or for use in file systems (with the {@code '/'} separator).</p>
         *
         * @param  separator  the separator to insert between each component of the MGRS identifier.
         */
        public void setSeparator(final String separator) {
            ArgumentChecks.ensureNonNull("separator", separator);
            this.separator = separator;
            trimmedSeparator = CharSequences.trimWhitespaces(separator);
        }

        /**
         * Bridge to {@link MilitaryGridReferenceSystem#datum}
         * for the {@link Encoder} and {@link Decoder} classes.
         */
        final ProjectedCRS projection(final double latitude, final double longitude) {
            return MilitaryGridReferenceSystem.this.datum.universal(latitude, longitude);
        }

        /**
         * Returns the encoder for the given coordinate reference system.
         *
         * @throws IllegalArgumentException if the given CRS do not use one of the supported datums.
         * @throws FactoryException if the creation of a coordinate operation failed.
         * @throws TransformException if the creation of an inverse operation failed.
         */
        private Encoder encoder(final CoordinateReferenceSystem crs) throws FactoryException, TransformException {
            if (crs == null) {
                throw new GazetteerException(Errors.format(Errors.Keys.UnspecifiedCRS));
            }
            // We can not use encoders.computeIfAbsent(crs, ...) because of checked exceptions.
            Encoder encoder = encoders.get(crs);
            if (encoder == null) {
                encoder = new Encoder(avoidDatumChange ? null : datum, crs);
                if (encoders.put(crs, encoder) != null) {
                    throw new ConcurrentModificationException();            // Opportunistic check.
                }
            }
            return encoder;
        }

        /**
         * Encodes the given position into a MGRS reference.
         * The given position must have a Coordinate Reference System (CRS) associated to it.
         *
         * @param  position  the coordinate to encode.
         * @return MGRS encoding of the given position.
         * @throws TransformException if an error occurred while transforming the given coordinate to a MGRS reference.
         */
        public String encode(final DirectPosition position) throws TransformException {
            ArgumentChecks.ensureNonNull("position", position);
            try {
                return encoder(position.getCoordinateReferenceSystem()).encode(this, position, separator, digits);
            } catch (IllegalArgumentException | FactoryException e) {
                throw new GazetteerException(e.getLocalizedMessage(), e);
            }
        }

        /**
         * Decodes the given MGRS reference into a position and an envelope.
         * The Coordinate Reference System (CRS) associated to the returned position depends on the given reference.
         *
         * @param  reference  MGRS string to decode.
         * @return a new position with the longitude at ordinate 0 and latitude at ordinate 1.
         * @throws TransformException if an error occurred while parsing the given string.
         */
        public Location decode(final CharSequence reference) throws TransformException {
            ArgumentChecks.ensureNonNull("reference", reference);
            return new Decoder(this, reference);
        }
    }




    /**
     * Conversions from direct positions to Military Grid Reference System (MGRS) references.
     * Each {@code Encoder} instance is configured for one {@code DirectPosition} CRS.
     * If a position is given in another CRS, another {@code Encoder} instance must be created.
     *
     * <div class="section">Immutability and thread safety</div>
     * This class is <strong>not</strong> thread-safe. A new instance must be created for each thread,
     * or synchronization must be applied by the caller.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     *
     * @see <a href="https://en.wikipedia.org/wiki/Military_Grid_Reference_System">Military Grid Reference System on Wikipedia</a>
     */
    static final class Encoder {
        /**
         * Special {@link #crsZone} value for the UPS (Universal Polar Stereographic) projection.
         * Positive value is used for North pole and negative value for South pole.
         */
        private static final int POLE = 100;

        /**
         * The datum to which to transform the coordinate before formatting the MGRS reference.
         * Only the datums enumerated in {@link CommonCRS} are currently supported.
         */
        private final CommonCRS datum;

        /**
         * UTM zone of position CRS (negative for South hemisphere), or {@value #POLE} (negative of positive)
         * if the CRS is a Universal Polar Stereographic projection, or 0 if the CRS is not a recognized projection.
         * Note that this is not necessarily the same zone than the one to use for formatting any given coordinate in
         * that projected CRS, since the {@link #zone(double, char)} method has special rules for some latitudes.
         */
        private final int crsZone;

        /**
         * Coordinate conversion from the position CRS to a CRS of the same type but with normalized axes,
         * or {@code null} if not needed. After conversion, axis directions will be (East, North) and axis
         * units will be metres.
         *
         * <p>This transform should perform only simple operations like swapping axis order and unit conversions.
         * It should not perform more complex operations that would require to go back to geographic coordinates.</p>
         */
        private final MathTransform toNormalized;

        /**
         * Coordinate conversion or transformation from the <em>normalized</em> CRS to geographic CRS.
         * Axis directions are (North, East) as in EPSG geodetic dataset and axis units are degrees.
         * This transform is never {@code null}.
         *
         * <p>This transform may contain datum change from the position datum to the target {@link #datum}.</p>
         */
        private final MathTransform toGeographic;

        /**
         * A transform from a position in the CRS given at construction time to a position in the CRS identified by
         * {@link #actualZone}. This field is updated only when a given position is not located in the zone of the
         * CRS given at construction time.
         */
        private MathTransform toActualZone;

        /**
         * The actual zone where the position to encode is located. Legal values are the same than {@link #crsZone}.
         * If non-zero, then this is the zone of the {@link #toActualZone} transform. This field is updated only when
         * a given position is not located in the zone of the CRS given at construction time.
         */
        private int actualZone;

        /**
         * Creates a new converter from direct positions to MGRS references.
         *
         * @param  datum  the datum to which to transform the coordinate before formatting the MGRS reference,
         *                or {@code null} for inferring the datum from the given {@code crs}.
         * @param  crs    the coordinate reference system of the coordinates for which to create MGRS references.
         * @throws IllegalArgumentException if the given CRS do not use one of the supported datums.
         * @throws FactoryException if the creation of a coordinate operation failed.
         * @throws TransformException if the creation of an inverse operation failed.
         */
        Encoder(CommonCRS datum, CoordinateReferenceSystem crs) throws FactoryException, TransformException {
            CoordinateReferenceSystem horizontal = CRS.getHorizontalComponent(crs);
            if (horizontal == null) {
                horizontal = crs;
            }
            if (datum == null) {
                datum = CommonCRS.forDatum(horizontal);
            }
            this.datum = datum;
            if (horizontal instanceof ProjectedCRS) {
                ProjectedCRS  projCRS = (ProjectedCRS) horizontal;
                Projection projection = projCRS.getConversionFromBase();
                final OperationMethod method = projection.getMethod();
                if (IdentifiedObjects.isHeuristicMatchForName(method, TransverseMercator.NAME)) {
                    crsZone = ZONER.zone(projection.getParameterValues());
                } else if (IdentifiedObjects.isHeuristicMatchForName(method, PolarStereographicA.NAME)) {
                    crsZone = POLE * PolarStereographicA.isUPS(projection.getParameterValues());
                } else {
                    crsZone = 0;                                    // Neither UTM or UPS projection.
                }
                if (crsZone != 0) {
                    /*
                     * Usually, the projected CRS already has (E,N) axis orientations with metres units,
                     * so we let 'toNormalized' to null. In the rarer cases where the CRS axes do not
                     * have the expected orientations and units, then we build a normalized version of
                     * that CRS and compute the transformation to that CRS.
                     */
                    final DefaultProjectedCRS userAxisOrder = DefaultProjectedCRS.castOrCopy(projCRS);
                    projCRS = userAxisOrder.forConvention(AxesConvention.NORMALIZED);
                    if (crs != horizontal || projCRS != userAxisOrder) {
                        toNormalized = CRS.findOperation(crs, projCRS, null).getMathTransform();
                        projection   = projCRS.getConversionFromBase();
                        horizontal   = projCRS;
                        crs          = projCRS;         // Next step in the chain of transformations.
                    } else {
                        toNormalized = null;            // ProjectedCRS (UTM or UPS) is already normalized.
                    }
                } else {
                    toNormalized = null;    // ProjectedCRS is neither UTM or UPS — will need full reprojection.
                }
                /*
                 * We will also need the transformation from the normalized projected CRS to latitude and
                 * longitude (in that order) in degrees. We can get this transform directly from the
                 * projected CRS if its base CRS already has the expected axis orientations and units.
                 */
                if (crs == horizontal && Utilities.equalsIgnoreMetadata(projCRS.getBaseCRS(), datum.geographic())) {
                    toGeographic = projection.getMathTransform().inverse();
                    return;
                }
            } else {
                crsZone      = 0;
                toNormalized = null;
            }
            toGeographic = CRS.findOperation(crs, datum.geographic(), null).getMathTransform();
        }

        /**
         * Returns the band letter for the given latitude. It is caller responsibility to ensure that the
         * given latitude is between {@value #UTM_SOUTH_BOUNDS} and {@value #UTM_NORTH_BOUNDS} inclusive.
         * The returned letter will be one of {@code "CDEFGHJKLMNPQRSTUVWX"} (note that I and O letters
         * are excluded). All bands are 8° height except the X band which is 12° height.
         *
         * @param  φ  the latitude in degrees for which to get the band letter.
         * @return the band letter for the given latitude.
         */
        static char latitudeBand(final double φ) {
            int band = 'C' + (int) ((φ - TransverseMercator.Zoner.SOUTH_BOUNDS) / LATITUDE_BAND_HEIGHT);
            if (band >= EXCLUDE_I && ++band >= EXCLUDE_O && ++band == 'Y') {
                band = 'X';         // Because the last latitude band ('X') is 12° height instead of 8°.
            }
            assert band >= 'C' && band <= 'X' : band;
            return (char) band;
        }

        /**
         * Encodes the given position into a MGRS reference. It is caller responsibility to ensure that
         * the position CRS is the same than the CRS specified at this {@code Encoder} creation time.
         *
         * @param  owner      the {@code Coder} which own this {@code Encoder}.
         * @param  position   the direct position to format as a MGRS reference.
         * @param  separator  the separator to insert between each component of the MGRS identifier.
         * @param  digits     number of digits to use for formatting the numerical part of a MGRS reference.
         * @return the value of {@code buffer.toString()}.
         */
        String encode(final Coder owner, DirectPosition position, final String separator, final int digits)
                throws FactoryException, TransformException
        {
            final StringBuilder buffer = owner.buffer;
            if (toNormalized != null) {
                owner.normalized = position = toNormalized.transform(position, owner.normalized);
            }
            final DirectPosition geographic = toGeographic.transform(position, owner.geographic);
            owner.geographic     = geographic;                  // For reuse in next method calls.
            final double  λ      = geographic.getOrdinate(1);
            final double  φ      = geographic.getOrdinate(0);
            final boolean isUTM  = φ >= TransverseMercator.Zoner.SOUTH_BOUNDS &&
                                   φ <  TransverseMercator.Zoner.NORTH_BOUNDS;
            final int zone       = isUTM ? ZONER.zone(φ, λ) : POLE;
            final int signedZone = MathFunctions.isNegative(φ) ? -zone : zone;
            if (signedZone == 0) {
                // Zero value at this point is the result of NaN of infinite ordinate value.
                throw new GazetteerException(Errors.format(Errors.Keys.NotANumber_1, "longitude"));
            }
            /*
             * If the DirectPosition given to this method is not in the expected Coordinate Reference System,
             * transform it now. This may happen because the UTM zone computed above is not the same UTM zone
             * than the coordinate one, or because the coordinate is geographic instead than projected.
             */
            if (signedZone != crsZone) {
                if (signedZone != actualZone) {
                    actualZone   = 0;                           // In case an exception is thrown on the next line.
                    toActualZone = CRS.findOperation(datum.geographic(), datum.universal(φ, λ), null).getMathTransform();
                    actualZone   = signedZone;
                }
                owner.normalized = position = toActualZone.transform(geographic, owner.normalized);
            }
            /*
             * Grid Zone Designator (GZD).
             */
            buffer.setLength(0);
            if (isUTM) {
                buffer.append(zone).append(separator).append(latitudeBand(φ));
            } else {
                char z = (signedZone < 0) ? 'A' : 'Y';
                if (λ >= 0) z++;
                buffer.append(z);
            }
            /*
             * 100 kilometres square identification.
             */
            if (digits >= 0) {
                final double  x = position.getOrdinate(0);
                final double  y = position.getOrdinate(1);
                final double cx = Math.floor(x / GRID_SQUARE_SIZE);
                final double cy = Math.floor(y / GRID_SQUARE_SIZE);
                int col = (int) cx;
                int row = (int) cy;
                if (isUTM) {
                    /*
                     * Specification said that 100,000-meters columns are lettered from A through Z (omitting I and O)
                     * starting at the 180° meridian, proceeding easterly for 18°, and repeating for each 18° intervals.
                     * Since a UTM zone is 6° width, a 18° interval is exactly 3 standard UTM zones. Columns in zone 1
                     * are A-H, zone 2 are J-R (skipping O), zone 3 are S-Z, then repeating every 3 zones.
                     */
                    if (col < 1 || col > 8) {
                        /*
                         * UTM northing values at the equator range from 166021 to 833979 meters approximatively
                         * (WGS84 ellipsoid). Consequently 'cx' ranges from approximatively 1.66 to 8.34, so 'c'
                         * should range from 1 to 8 inclusive.
                         */
                        throw new GazetteerException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
                    }
                    switch (zone % 3) {                          // First A-H sequence starts at zone number 1.
                        case 1: col += ('A' - 1); break;
                        case 2: col += ('J' - 1); if (col >= EXCLUDE_O) col++; break;
                        case 0: col += ('S' - 1); break;
                    }
                    /*
                     * Rows in odd  zones are ABCDEFGHJKLMNPQRSTUV
                     * Rows in even zones are FGHJKLMNPQRSTUVABCDE
                     * Those 20 letters are repeated in a cycle.
                     */
                    if ((zone & 1) == 0) {
                        row += ('F' - 'A');
                    }
                    row %= GRID_ROW_COUNT;
                    // Row calculation to be completed after the 'else' block.
                } else {
                    /*
                     * Universal Polar Stereographic (UPS) case. Row letters go from A to Z, omitting I and O.
                     * The column letters go from A to Z, omitting I, O, D, E, M, N, V, W. Rightmost column in
                     * grid zones A and Y has column letter Z, and the next column in grid zones B and Z starts
                     * over with column letter A.
                     */
                    final byte[] columns = POLAR_COLUMNS;
                    col -= (int) (PolarStereographicA.UPS_SHIFT / GRID_SQUARE_SIZE);
                    if (!(λ >= 0)) {                    // Same condition than in GZD block. Use of ! is for NaN.
                        col += columns.length;          // Letters Z to A from right to left.
                    }
                    if (col < 0 || col >= columns.length) {
                        throw new GazetteerException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
                    }
                    col  = columns[col];
                    row -= owner.getReferenceSystem().polarOffset(signedZone < 0);
                }
                row += 'A';
                if (row >= EXCLUDE_I && ++row >= EXCLUDE_O) row++;
                buffer.append(separator).append(letter(col)).append(letter(row));
                /*
                 * Numerical location at the given precision.
                 * The specification requires us to truncate the number, not to round it.
                 */
                if (digits > 0) {
                    final double precision = MathFunctions.pow10(METRE_PRECISION_DIGITS - digits);
                    append(buffer.append(separator), (int) ((x - cx * GRID_SQUARE_SIZE) / precision), digits);
                    append(buffer.append(separator), (int) ((y - cy * GRID_SQUARE_SIZE) / precision), digits);
                }
            }
            return buffer.toString();
        }

        /**
         * Returns the given character as a {@code char} if it is a letter, or throws an exception otherwise.
         * The exception should never happen, unless the the encoder is used for a planet larger than Earth
         * for which we do not have enough letters.
         */
        private static char letter(final int c) throws GazetteerException {
            if (c >= 'A' && c <= 'Z') return (char) c;
            throw new GazetteerException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
        }

        /**
         * Appends the given value in the given buffer, padding with zero digits in order to get
         * the specified total amount of digits.
         */
        private static void append(final StringBuilder buffer, final int value, int digits) throws GazetteerException {
            if (value >= 0) {
                final int p = buffer.length();
                digits -= (buffer.append(value).length() - p);
                if (digits >= 0) {
                    StringBuilders.repeat(buffer, p, '0', digits);
                    return;
                }
            }
            throw new GazetteerException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
        }
    }




    /**
     * The result of decoding a MGRS reference. The {@linkplain #getPosition() position}
     * represents the lower-left corner (not the centroid) of the decoded MGRS reference.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.8
     * @version 0.8
     * @module
     */
    static final class Decoder extends SimpleLocation.Projected {
        /**
         * Number of bits reserved for storing the minimal northing value of latitude bands in the
         * {@link #ROW_RESOLVER} table.
         */
        static final int NORTHING_BITS_COUNT = 4;

        /**
         * Mask over the {@value #NORTHING_BITS_COUNT} lowest bits.
         */
        static final int NORTHING_BITS_MASK = (1 << NORTHING_BITS_COUNT) - 1;

        /**
         * Mapping from latitude bands to the minimal northing values, together with the set of valid rows.
         * There is 20 latitude bands identified by letters C to X inclusively, with letters I and O skipped.
         * First, the band letter must be converted to an index <var>i</var> in the [0 … 19] range.
         * Then, the {@code ROW_RESOLVER[i]} integer values provides the following information:
         *
         * <ul>
         *   <li>
         *     The lowest {@value #NORTHING_BITS_COUNT} bits gives the minimal northing value of all valid
         *     coordinates in the latitude band, as a multiple of the number of metres in a full cycle of
         *     {@value #GRID_ROW_COUNT} rows. That northing value can be computed in metre as below:
         *
         *     {@preformat java
         *         double northing = (ROW_RESOLVER[i] & NORTHING_BITS_MASK) * (GRID_SQUARE_SIZE * GRID_ROW_COUNT);
         *     }
         *   </li><li>
         *     Given a row number <var>row</var> in the [0 … 19] range, the following expression tells
         *     if that row can be inside the latitude band:
         *
         *     {@preformat java
         *         boolean isValidRow = (ROW_RESOLVER[i] & (1 << (row + NORTHING_BITS_COUNT))) != 0;
         *     }
         *
         *     Note that the same row may be valid in two consecutive latitude bands.
         *     The trailing {@code _0000} parts make room for {@value #NORTHING_BITS_COUNT} bits.
         *   </li>
         * </ul>
         *
         * The content of this table is verified by {@code MilitaryGridReferenceSystemTest.verifyDecoderTables()}.
         * The same method with minor edition can be used for generating this table.
         */
        @SuppressWarnings("PointlessBitwiseExpression")
        private static final int[] ROW_RESOLVER = {
            /*
             * First column is the minimal northing value as a multiple of 2000 km.
             * Second column enumerates the valid rows in that latitude band.
             * Trailing _0000 are for making room for NORTHING_BITS_COUNT bits.
             */
            /* Latitude band C (from 80°S) */   0  |  0b11111111100000000001_0000,
            /* Latitude band D (from 72°S) */   1  |  0b00000000001111111111_0000,
            /* Latitude band E (from 64°S) */   1  |  0b00111111111100000000_0000,
            /* Latitude band F (from 56°S) */   1  |  0b11100000000001111111_0000,
            /* Latitude band G (from 48°S) */   2  |  0b00001111111111000000_0000,
            /* Latitude band H (from 40°S) */   2  |  0b11111000000000011111_0000,
            /* Latitude band J (from 32°S) */   3  |  0b00000011111111110000_0000,
            /* Latitude band K (from 24°S) */   3  |  0b11111110000000000111_0000,
            /* Latitude band L (from 16°S) */   4  |  0b00000000111111111100_0000,
            /* Latitude band M (from  8°S) */   4  |  0b11111111100000000000_0000,
            /* Latitude band N (from  0° ) */   0  |  0b00000000000111111111_0000,
            /* Latitude band P (from  8°N) */   0  |  0b00111111111100000000_0000,
            /* Latitude band Q (from 16°N) */   0  |  0b11100000000001111111_0000,
            /* Latitude band R (from 24°N) */   1  |  0b00001111111111000000_0000,
            /* Latitude band S (from 32°N) */   1  |  0b11111000000000011111_0000,
            /* Latitude band T (from 40°N) */   2  |  0b00000011111111110000_0000,
            /* Latitude band U (from 48°N) */   2  |  0b11111110000000000111_0000,
            /* Latitude band V (from 56°N) */   3  |  0b00000000111111111100_0000,
            /* Latitude band W (from 64°N) */   3  |  0b11111111110000000000_0000,
            /* Latitude band X (from 72°N) */   3  |  0b10000011111111111111_0000
        };

        /**
         * The Coordinate Reference System of the decoded MGRS reference.
         * This is an Universal Transverse Mercator (UTM) or Universal Polar Stereographic (UPS) projection.
         *
         * @see #getCoordinateReferenceSystem()
         */
        private final ProjectedCRS crs;

        /**
         * Decodes the given MGRS reference.
         *
         * @param owner  the {@code Coder} which is creating this {@code Decoder}.
         */
        Decoder(final Coder owner, final CharSequence reference) throws TransformException {
            super(owner.getReferenceSystem().rootType(), reference);
            final int zone;                     // UTM zone, or 0 if UPS.
            boolean hasSquareIdentification;    // Whether a square identification is present.
            final double φs;                    // Southernmost bound of latitude band (UTM only).
            final double λ0;                    // Central meridian of UTM zone (ignoring Norway and Svalbard).
            boolean isValid = true;             // Whether the given reference passes consistency checks.

            final int end  = CharSequences.skipTrailingWhitespaces(reference, 0, reference.length());
            final int base = CharSequences.skipLeadingWhitespaces (reference, 0, end);
            int i = endOfDigits(reference, base, end);
            if (i == base && i < end) {
                /*
                 * Universal Polar Stereographic (UPS) case. The reference has no zone number
                 * and begins directly with 3 parts, where each part is exactly one letter:
                 *
                 *   part 0  —  A zone indicator: A or B for South pole, or Y or Z for North pole.
                 *   part 1  —  column letter:    A to Z, omitting I, O, D, E, M, N, V, W.
                 *   part 2  —  row letter:       A to Z, omitting I, O.
                 */
                zone          = 0;                  // Not used in UPS case.
                boolean south = false;              // True for A and B zones, false for Y and Z zones.
                boolean west  = false;              // True for A and Y zones, false for B and Z zones.
                int col = 0, row = 0;               // Parsed row and column indices.
                for (int part = 0; part <= 2; part++) {
                    int c = Character.codePointAt(reference, i);
                    final int ni = i + Character.charCount(c);
                    if (isLetter(c) || isLetter(c -= ('a' - 'A'))) {
parse:                  switch (part) {
                            case 0: {
                                switch (c) {
                                    case 'A': south = true; west = true; break;
                                    case 'B': south = true;              break;
                                    case 'Y':               west = true; break;
                                    case 'Z':                            break;
                                    default : break parse;                              // Invalid UPS zone.
                                }
                                i = nextComponent(owner, reference, base, ni, end);
                                continue;
                            }
                            case 1: {
                                col = Arrays.binarySearch(POLAR_COLUMNS, (byte) c);
                                if (col < 0) break;                                     // Invalid column letter.
                                if (west) col -= POLAR_COLUMNS.length;
                                col += (int) (PolarStereographicA.UPS_SHIFT / GRID_SQUARE_SIZE);
                                i = nextComponent(owner, reference, base, ni, end);
                                continue;
                            }
                            case 2: {
                                if (c >= EXCLUDE_O) c--;
                                if (c >= EXCLUDE_I) c--;
                                row = (c - 'A') + owner.getReferenceSystem().polarOffset(south);
                                i = ni;
                                continue;
                            }
                        }
                    }
                    /*
                     * We reach this point only if the current character is invalid (not a letter,
                     * or not one of the letters expected for the current part).
                     */
                    final short key;
                    final CharSequence token;
                    if (part == 0) {
                        key = Resources.Keys.IllegalUPSZone_1;
                        token = reference.subSequence(i, ni);
                    } else {
                        key = Resources.Keys.IllegalSquareIdentification_1;
                        token = CharSequences.token(reference, i);
                    }
                    throw new GazetteerException(Resources.format(key, token));
                }
                crs  = owner.projection(φs = (south ? Latitude.MIN_VALUE : Latitude.MAX_VALUE), 0);
                minX = col * GRID_SQUARE_SIZE;
                minY = row * GRID_SQUARE_SIZE;
                hasSquareIdentification = true;
                λ0 = 0;
            } else {
                /*
                 * Universal Transverse Mercator (UTM) case.
                 */
                zone = parseInt(reference, base, i, Resources.Keys.IllegalUTMZone_1);
                if (zone < 1 || zone > 60) {
                    throw new GazetteerException(Resources.format(Resources.Keys.IllegalUTMZone_1, zone));
                }
                /*
                 * Parse the sub-sequence made of letters. That sub-sequence can have one or three parts.
                 * The first part is mandatory and the two other parts are optional, but if the two last
                 * parts are omitted, then they must be omitted together.
                 *
                 *   part 0  —  latitude band: C-X (excluding I and O) for UTM. Other letters (A, B, Y, Z) are for UPS.
                 *   part 1  —  column letter: A-H in zone 1, J-R (skipping O) in zone 2, S-Z in zone 3, then repeat.
                 *   part 2  —  row letter:    ABCDEFGHJKLMNPQRSTUV in odd zones, FGHJKLMNPQRSTUVABCDE in even zones.
                 */
                int latitudeBand = -1;                              // The latitude band in [0 … 19] range.
                int col = 1, row = 0;
                hasSquareIdentification = true;
                for (int part = 0; part <= 2; part++) {
                    if (part == 1 && i >= end) {
                        hasSquareIdentification = false;
                        break;                                      // Allow to stop parsing only after part 1.
                    }
                    i = nextComponent(owner, reference, base, i, end);
                    int c = Character.codePointAt(reference, i);
                    final int ni = i + Character.charCount(c);
                    if (!isLetter(c) && !isLetter(c -= ('a' - 'A'))) {
                        final short key;
                        final CharSequence token;
                        if (part == 0) {
                            key = Resources.Keys.IllegalLatitudeBand_1;
                            token = reference.subSequence(i, ni);
                        } else {
                            key = Resources.Keys.IllegalSquareIdentification_1;
                            token = CharSequences.token(reference, i);
                        }
                        throw new GazetteerException(Resources.format(key, token));
                    }
                    /*
                     * At this point, 'c' is a valid letter. First, applies a correction for the fact that 'I' and 'O'
                     * letters were excluded. Next, the conversion to latitude or 100 000 meters grid indices depends
                     * on which part we are parsing. The formulas used below are about the same than in Encoder class,
                     * with terms moved on the other side of the equations.
                     */
                    if (c >= EXCLUDE_O) c--;
                    if (c >= EXCLUDE_I) c--;
                    switch (part) {
                        case 0: {
                            latitudeBand = (c - 'C');
                            break;
                        }
                        case 1: {
                            switch (zone % 3) {                         // First A-H sequence starts at zone number 1.
                                case 1: col = c - ('A' - 1); break;
                                case 2: col = c - ('J' - 2); break;     // -2 because 'I' has already been excluded.
                                case 0: col = c - ('S' - 3); break;     // -3 because 'I' and 'O' have been excluded.
                            }
                            break;
                        }
                        case 2: {
                            if ((zone & 1) != 0) {
                                row = c - 'A';
                            } else {
                                row = c - 'F';
                                if (row < 0) {
                                    row += GRID_ROW_COUNT;
                                }
                            }
                            break;
                        }
                    }
                    i = ni;
                }
                /*
                 * Create a UTM projection for exactly the zone specified in the MGRS reference, regardless the
                 * Norway and Svalbard special cases.  Then fetch the smallest northing value that the latitude
                 * band may contain in that UTM zone. This is the projection of the geographic coordinate on the
                 * central meridian in the North hemisphere, or on the UTM zone border in the South hemisphere
                 * (because the zone center is always closer to equator and the zone borders closer to poles).
                 * This estimation is needed because the 100 kilometres square identification is insufficient;
                 * we may need to add some multiple of 2000 kilometres (20 squares).
                 */
                φs = latitudeBand * LATITUDE_BAND_HEIGHT + TransverseMercator.Zoner.SOUTH_BOUNDS;
                if (latitudeBand < 0 || latitudeBand >= ROW_RESOLVER.length) {
                    throw new GazetteerException(Resources.format(Resources.Keys.IllegalLatitudeBand_1, Encoder.latitudeBand(φs)));
                }
                λ0  = ZONER.centralMeridian(zone);
                crs = owner.projection(Math.signum(φs), λ0);
                final int info = ROW_RESOLVER[latitudeBand];        // Contains the above-cited northing value.
                if (hasSquareIdentification) {
                    int rowBit = 1 << (row + NORTHING_BITS_COUNT);  // Bit mask of the row to check for existence.
                    isValid = (info & rowBit) != 0;                 // Whether the row exists in the latitude band.
                    if (isValid) {
                        /*
                         * Get the bit mask of the first invalid row north of current row. For example if 'info' has
                         * the following value and the current 'rowBit' mask has its bit set at the position of (1),
                         * then after this line of code the 'rowBit' bit will be set at the position of (2).
                         *
                         *     11111000000000011111
                         *         ^         ^   ^
                         *        (3)       (2) (1)
                         *
                         * This line works by forcing to 1 all bits on the right side of (1) then searching the lowest
                         * zero bit (actually inverting everything and searching the lowest one bit, since there is no
                         * "lowestZeroBit" method in java.lang.Integer).
                         *
                         * The purpose is to verify if there another, distinct, sequence of 1 values on the left of
                         * current sequence. We will check that (after this block) by verifying if there is any bit
                         * set to 1 on the left side of zero bits, like the bit at position (3) in above figure. If
                         * such distinct sequence exists, then that sequence is the current cycle of rows while the
                         * sequence on the right side is a new cycle of rows.
                         *
                         * Note that if there is no zero bit on the left side, then rowBit = 0.
                         * This implies ~(rowBit - 1) == 0, which is okay for next line of code.
                         * Note: ~(rowBit - 1)  ==  -rowBit
                         */
                        rowBit = Integer.lowestOneBit(~(info | (rowBit - 1)));
                    }
                    if ((info & -rowBit) != 0) {    // Test if there is valid rows on the left side of sequence of zero bits.
                        row += GRID_ROW_COUNT;      // Left bits were from previous cycle, which means that we started a new cycle.
                    }
                }
                row += (info & NORTHING_BITS_MASK) * GRID_ROW_COUNT;        // Add the pre-computed northing value.
                minX = col * GRID_SQUARE_SIZE;
                minY = row * GRID_SQUARE_SIZE;
            }
            /*
             * If we have not yet reached the end of string, parse the numerical location.
             * That location is normally encoded as a single number with an even number of digits.
             * The first half is the easting and the second half is the northing, both relative to the
             * 100 kilometer square. However some variants of MGRS use a separator, in which case we get
             * two distinct numbers. In both cases, the resolution is determined by the amount of digits.
             */
            final double sx, sy;    // Scale factors for converting MGRS values in to easting and northing in metres.
            if (i < end) {
                i = nextComponent(owner, reference, base, i, end);
                int s = endOfDigits(reference, i, end);
                final double x, y;
                if (s >= end) {
                    int length = s - i;
                    if ((length & 1) != 0) {
                        throw new GazetteerException(Resources.format(Resources.Keys.OddGridCoordinateLength_1,
                                reference.subSequence(i, s)));
                    }
                    final int h = i + (length >>>= 1);
                    sx = sy = MathFunctions.pow10(METRE_PRECISION_DIGITS - length);
                    x  = parseCoordinate(reference, i, h, sx);
                    y  = parseCoordinate(reference, h, s, sy);
                } else {
                    sx = MathFunctions.pow10(METRE_PRECISION_DIGITS - (s - i));
                    x  = parseCoordinate(reference, i, s, sx);
                    i  = nextComponent(owner, reference, base, s, end);
                    s  = endOfDigits(reference, i, end);
                    sy = MathFunctions.pow10(METRE_PRECISION_DIGITS - (s - i));
                    y = parseCoordinate(reference, i, s, sy);
                    if (s < end) {
                        throw new GazetteerException(Errors.format(Errors.Keys.UnexpectedCharactersAfter_2,
                                reference.subSequence(base, s), CharSequences.trimWhitespaces(reference, s, end)));
                    }
                }
                minX += x;
                minY += y;
            } else if (hasSquareIdentification) {
                sx = sy = GRID_SQUARE_SIZE;
            } else {
                /*
                 * Not used for scaling anymore. Choose value that will cause the point to be in zone zenter.
                 * The '- GRID_SQUARE_SIZE' in 'sx' is because the westernmost 100-km grid squares are in the
                 * column at index 1 (the column at index 0 is outside the UTM zone).
                 */
                sx = (ZONER.easting - GRID_SQUARE_SIZE) * 2;
                sy =  ZONER.northing;
            }
            maxX = minX + sx;
            maxY = minY + sy;
            /*
             * At this point the non-clipped projected envelope has been computed. Now compute the geographic envelope.
             * We need this information for clipping the projected envelope to the domain of validity of UTM zone.
             */
            if (!hasSquareIdentification) {
                if (zone != 0) {
                    if (φs < 0) {
                        southBoundLatitude = TransverseMercator.Zoner.SOUTH_BOUNDS;
                        northBoundLatitude = 0;
                    } else {
                        southBoundLatitude = 0;
                        northBoundLatitude = TransverseMercator.Zoner.NORTH_BOUNDS;
                    }
                    westBoundLongitude = λ0 - ZONER.width / 2;
                    eastBoundLongitude = λ0 + ZONER.width / 2;
                } else {
                    if (φs < 0) {
                        southBoundLatitude = Latitude.MIN_VALUE;
                        northBoundLatitude = TransverseMercator.Zoner.SOUTH_BOUNDS;
                    } else {
                        southBoundLatitude = TransverseMercator.Zoner.NORTH_BOUNDS;
                        northBoundLatitude = Latitude.MAX_VALUE;
                    }
                    westBoundLongitude = Longitude.MIN_VALUE;
                    eastBoundLongitude = Longitude.MAX_VALUE;
                }
            } else {
                final MathTransform projection = crs.getConversionFromBase().getMathTransform();
                computeGeographicBoundingBox(projection.inverse());
                /*
                 * Update the LocationType according the precision.
                 * This update is mostly for documentation purpose.
                 * There is three levels:
                 *
                 *   - Grid zone designator         (if hasSquareIdentification == false)
                 *   - 100 km square identifier     (if resolution == 100 km)
                 *   - Grid coordinate              (if resolution < 100 km)
                 */
                setTypeToChild();                   // Replace "Grid zone designator" by "100 km square identifier".
                if (sx < GRID_SQUARE_SIZE || sy < GRID_SQUARE_SIZE) {
                    setTypeToChild();               // Replace "100 km square identifier" by "Grid coordinate".
                }
                /*
                 * At this point we finished computing the position. Now perform error detection, by verifying
                 * if the given 100 kilometres square identification is consistent with grid zone designation.
                 * We verify both φ and λ, but the verification of φ is actually redundant with the check of
                 * 100 km square validity that we did previously with the help of ROW_RESOLVER bitmask.
                 * We check φ anyway in case of bug, but we have to allow a tolerance threshold on the south
                 * bound because the 100 km square may overlap two latitude bands. We do not need equivalent
                 * tolerance threshold for the upper bound because the coordinate that we are testing is the
                 * lower-left corner of the cell area.
                 */
                if (isValid && zone != 0) {
                    final double λ = (westBoundLongitude + eastBoundLongitude) / 2;
                    final double φ = (southBoundLatitude + northBoundLatitude) / 2;
                    isValid = (φ >= φs - LATITUDE_BAND_HEIGHT/2) && (φ < upperBound(φs));   // See above comment.
                    if (isValid) {
                        /*
                         * Verification of UTM zone. We allow a tolerance for latitudes close to a pole because
                         * not all users may apply the UTM special rules for Norway and Svalbard. Anyway, using
                         * the neighbor zone at those high latitudes is less significant. For other latitudes,
                         * we allow a tolerance if the point is close to a line of zone change.
                         */
                        int zoneError = ZONER.zone(φ, λ) - zone;
                        if (zoneError != 0) {
                            final int zc = ZONER.zoneCount();
                            if (zoneError > zc/2) zoneError -= zc;
                            if (ZONER.isSpecialCase(zone, φ)) {
                                isValid = Math.abs(zoneError) == 1;         // Tolerance in zone numbers for high latitudes.
                            } else {
                                final double rλ = Math.IEEEremainder(λ - ZONER.origin, ZONER.width);    // Distance to closest zone change, in degrees of longitude.
                                final double cv = (minX - ZONER.easting) / (λ - λ0);                    // Approximative conversion factor from degrees to metres.
                                isValid = (Math.abs(rλ) * cv <= sx);                                    // Be tolerant if distance in metres is less than resolution.
                                if (isValid) {
                                    isValid = (zoneError == (rλ < 0 ? -1 : +1));                        // Verify also that the error is on the side of the zone change.
                                }
                            }
                        }
                    }
                }
                /*
                 * At this point we finished verifying the cell validity using the coordinates specified by the
                 * MGRS reference. If the cell is valid, we can now check for cells that are on a zone border.
                 * Those cells will be clipped to the zone valid area.
                 */
                if (isValid) {
                    final boolean changed;
                    if (zone != 0) {
                        double width = ZONER.width;
                        if (!ZONER.isSpecialCase(zone, φs)) width /= 2;       // Be strict only if not Norway or Svalbard.
                        changed = clipGeographicBoundingBox(λ0 - width, φs,
                                                            λ0 + width, upperBound(φs));
                    } else if (φs < 0) {
                        changed = clipGeographicBoundingBox(Longitude.MIN_VALUE, Latitude.MIN_VALUE,
                                                            Longitude.MAX_VALUE, TransverseMercator.Zoner.SOUTH_BOUNDS);
                    } else {
                        changed = clipGeographicBoundingBox(Longitude.MIN_VALUE, TransverseMercator.Zoner.NORTH_BOUNDS,
                                                            Longitude.MAX_VALUE, Latitude.MAX_VALUE);
                    }
                    if (changed) {
                        clipProjectedEnvelope(projection, sx / 100, sy / 100);
                    }
                }
            }
            if (!isValid) {
                final String gzd;
                try {
                    gzd = owner.encoder(crs).encode(owner, getDirectPosition(), "", 0);
                } catch (IllegalArgumentException | FactoryException e) {
                    throw new GazetteerException(e.getLocalizedMessage(), e);
                }
                final CharSequence ref = reference.subSequence(base, end);
                throw new ReferenceVerifyException(Resources.format(Resources.Keys.InconsistentWithGZD_2, ref, gzd));
            }
        }

        /**
         * Skips spaces, then the separator if present (optional).
         *
         * @param  reference  the reference to parse.
         * @param  base       index where the parsing began. Used for formatting error message only.
         * @param  start      current parsing position.
         * @param  end        where the parsing is expected to end.
         * @return position where to continue parsing (with spaces skipped).
         * @throws GazetteerException if this method unexpectedly reached the end of string.
         */
        private static int nextComponent(final Coder owner, final CharSequence reference,
                final int base, int start, final int end) throws GazetteerException
        {
            start = CharSequences.skipLeadingWhitespaces(reference, start, end);
            if (start < end) {
                if (!CharSequences.regionMatches(reference, start, owner.trimmedSeparator)) {
                    return start;               // Separator not found, but it was optional.
                }
                start += owner.trimmedSeparator.length();
                start = CharSequences.skipLeadingWhitespaces(reference, start, end);
                if (start < end) {
                    return start;
                }
            }
            throw new GazetteerException(Errors.format(Errors.Keys.UnexpectedEndOfString_1, reference.subSequence(base, end)));
        }

        /**
         * Returns {@code true} if the given character is a valid upper case ASCII letter, excluding I and O.
         */
        private static boolean isLetter(final int c) {
            return (c >= 'A' && c <= 'Z') && c != EXCLUDE_I && c != EXCLUDE_O;
        }

        /**
         * Returns the index after the last digit in a sequence of ASCII characters.
         * Leading whitespaces must have been skipped before to invoke this method.
         */
        private static int endOfDigits(final CharSequence reference, int i, final int end) {
            while (i < end) {
                final char c = reference.charAt(i);     // Code-point API not needed here because we restrict to ASCII.
                if (c < '0' || c > '9') {               // Do not use Character.isDigit(…) because we restrict to ASCII.
                    break;
                }
                i++;
            }
            return i;
        }

        /**
         * Parses part of the given character sequence as an integer.
         *
         * @param  reference  the MGRS reference to parse.
         * @param  start      index of the first character to parse as an integer.
         * @param  end        index after the last character to parse as an integer.
         * @param  errorKey   {@link Resources.Keys} value to use in case of error.
         *                    The error message string shall accept exactly one argument.
         * @return the parsed integer.
         * @throws GazetteerException if the string can not be parsed as an integer.
         */
        private static int parseInt(final CharSequence reference, final int start, final int end, final short errorKey)
                throws GazetteerException
        {
            NumberFormatException cause = null;
            final CharSequence part;
            if (start == end) {
                part = CharSequences.token(reference, start);
            } else {
                part = reference.subSequence(start, end);
                try {
                    return Integer.parseInt(part.toString());
                } catch (NumberFormatException e) {
                    cause = e;
                }
            }
            throw new GazetteerException(Resources.format(errorKey, part), cause);
        }

        /**
         * Parses part of the given character sequence as a grid coordinate.
         * The resolution is determined by the amount of digits.
         *
         * @param  reference  the MGRS reference to parse.
         * @param  start      index of the first character to parse as a grid coordinate.
         * @param  end        index after the last character to parse as a grid coordinate.
         * @param  scale      value of {@code MathFunctions.pow10(METRE_PRECISION_DIGITS - (end - start))}.
         * @return the parsed grid coordinate (also referred to as rectangular coordinates).
         * @throws GazetteerException if the string can not be parsed as a grid coordinate.
         */
        private static double parseCoordinate(final CharSequence reference,
                final int start, final int end, final double scale) throws GazetteerException
        {
            return parseInt(reference, start, end, Resources.Keys.IllegalGridCoordinate_1) * scale;
        }

        /**
         * Returns the upper bound of the latitude band specified by the given lower bound.
         */
        static double upperBound(final double φ) {
            return φ < TransverseMercator.Zoner.SVALBARD_BOUNDS ? φ + LATITUDE_BAND_HEIGHT
                     : TransverseMercator.Zoner.NORTH_BOUNDS;
        }

        /**
         * Returns the Coordinate Reference System of the decoded MGRS reference.
         * This is an Universal Transverse Mercator (UTM) or Universal Polar Stereographic (UPS) projection.
         */
        @Override
        public CoordinateReferenceSystem getCoordinateReferenceSystem() {
            return crs;
        }
    }
}
