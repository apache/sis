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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Collection;
import java.util.Arrays;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.io.TabularFormat;
import org.apache.sis.io.TableAppender;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the main branch:
import org.apache.sis.metadata.iso.citation.AbstractParty;


/**
 * Formats {@code Location} instances in a tabular format.
 * This format assumes a monospaced font and an encoding supporting drawing box characters (e.g. UTF-8).
 *
 * <h2>Example</h2>
 * The location identified by "32TNL83" in the {@linkplain MilitaryGridReferenceSystem military grid reference system}
 * can be represented by the following string formatted using {@link Locale#ENGLISH}:
 *
 * <pre class="text">
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │ Location type:               Grid zone designator           │
 *   │ Geographic identifier:       32TNL83                        │
 *   │ West bound:                    580,000 m    —     9°57′00″E │
 *   │ Representative value:          585,000 m    —    10°00′36″E │
 *   │ East bound:                    590,000 m    —    10°04′13″E │
 *   │ South bound:                 4,530,000 m    —    40°54′58″N │
 *   │ Representative value:        4,535,000 m    —    40°57′42″N │
 *   │ North bound:                 4,540,000 m    —    41°00′27″N │
 *   │ Coordinate reference system: WGS 84 / UTM zone 32N          │
 *   └─────────────────────────────────────────────────────────────┘</pre>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The current implementation can only format features — parsing is not yet implemented.</li>
 *   <li>{@code LocationFormat}, like most {@code java.text.Format} subclasses, is not thread-safe.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 */
public class LocationFormat extends TabularFormat<AbstractLocation> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3440801316594167279L;

    /**
     * Default instance for {@link AbstractLocation#toString()}.
     * Use of this instance must be synchronized on {@code INSTANCE}.
     */
    static final LocationFormat INSTANCE = new LocationFormat(Locale.getDefault(), TimeZone.getDefault());

    /**
     * {@link Vocabulary.Keys} constants for the east, west, south and north bounds, in that order.
     */
    private static final short[] BOUND_KEY = {
        Vocabulary.Keys.WestBound,
        Vocabulary.Keys.RepresentativeValue,
        Vocabulary.Keys.EastBound,
        Vocabulary.Keys.SouthBound,
        Vocabulary.Keys.RepresentativeValue,
        Vocabulary.Keys.NorthBound
    };

    /**
     * Creates a new format for the given locale. The given locale can be {@code null}
     * or {@link Locale#ROOT} if this format shall format "unlocalized" strings.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     */
    public LocationFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
    }

    /**
     * Returns the type of values formatted by this {@code Format} instance.
     *
     * @return the type of values formatted by this {@code Format} instance.
     */
    @Override
    public Class<AbstractLocation> getValueType() {
        return AbstractLocation.class;
    }

    /**
     * Returns a CRS equivalent to the given one but with (longitude, latitude) or (easting, northing) axis order.
     * This method does not change the units of measurement. If the given CRS is already normalized, then it is
     * returned unchanged.
     */
    private static CoordinateReferenceSystem normalize(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            AbstractCRS normalized = AbstractCRS.castOrCopy(crs);
            if (normalized != (normalized = normalized.forConvention(AxesConvention.DISPLAY_ORIENTED))) {
                return normalized;
            }
        }
        return crs;
    }

    /**
     * Transforms the given position from the given source to the given target CRS.
     * If the source and target CRS are the same, then this method returns the position unchanged.
     */
    private static DirectPosition transform(DirectPosition position,
                                            CoordinateReferenceSystem sourceCRS,
                                            CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException
    {
        if (sourceCRS != targetCRS) {
            position = CRS.findOperation(sourceCRS, targetCRS, null).getMathTransform().transform(position, null);
        }
        return position;
    }

    /**
     * Returns a localized version of the given international string, or {@code null} if none.
     */
    private static String toString(final InternationalString i18n, final Locale locale) {
        return (i18n != null) ? i18n.toString(locale) : null;
    }

    /**
     * Returns a localized version of the given date, or {@code null} if none.
     */
    private String toString(final Date date) {
        return (date != null) ? getFormat(Date.class).format(date) : null;
    }

    /**
     * Writes a textual representation of the given location in the given stream or buffer.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * in a future SIS version, the type of {@code location} parameter may be generalized
     * to the {@code org.opengis.referencing.gazetteer.Location} interface.
     * This change is pending GeoAPI revision.</div>
     *
     * @param  location    the location to format.
     * @param  toAppendTo  where to format the location.
     * @throws IOException if an error occurred while writing to the given appendable.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public void format(final AbstractLocation location, final Appendable toAppendTo) throws IOException {
        ArgumentChecks.ensureNonNull("location", location);
        final Locale locale = getLocale(Locale.Category.DISPLAY);
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        final var table = new TableAppender(toAppendTo, "│ ", columnSeparator, " │");
        table.setMultiLinesCells(true);
        /*
         * Location type.
         */
        table.appendHorizontalSeparator();
        final AbstractLocationType type = location.type();
        if (type != null) {
            append(table, vocabulary, Vocabulary.Keys.LocationType, toString(type.getName(), locale));
        }
        /*
         * Geographic identifier and alternative identifiers, if any.
         */
        append(table, vocabulary, Vocabulary.Keys.GeographicIdentifier, toString(location.getGeographicIdentifier(), locale));
        final Collection<? extends InternationalString> alt = location.getAlternativeGeographicIdentifiers();
        if (alt != null && !alt.isEmpty()) {
            boolean isFirst = true;
            vocabulary.appendLabel(Vocabulary.Keys.AlternativeIdentifiers, table);
            nextColumn(table);
            for (final InternationalString id : alt) {
                if (!isFirst) {
                    isFirst = false;
                    table.append(lineSeparator);
                }
                table.append(id);
            }
            table.nextLine();
        }
        /*
         * Extents (temporal and geographic). If an envelope exists and the CRS is not geographic,
         * then the envelope bounds will be appended on the same lines as the geographic bounds.
         * But before writing the bounding box and/or the envelope, check if they are redundant.
         * We may also need to change axis order (but not unit) of the envelope in order to match
         * the axis order of the geographic bounding box.
         */
        final var extent = new DefaultExtent(null, location.getGeographicExtent(), null, location.getTemporalExtent());
        final Range<Date> time = Extents.getTimeRange(extent);
        if (time != null) {
            append(table, vocabulary, Vocabulary.Keys.StartDate, toString(time.getMinValue()));
            append(table, vocabulary, Vocabulary.Keys.EndDate,   toString(time.getMaxValue()));
        }
        GeographicBoundingBox     bbox     = Extents.getGeographicBoundingBox(extent);
        Envelope                  envelope = location.getEnvelope();
        DirectPosition            position = location.getPosition();
        DirectPosition            geopos   = null;                      // Position in geographic CRS.
        CoordinateReferenceSystem crs      = null;                      // Envelope Coordinate Reference System.
        CoordinateReferenceSystem normCRS  = null;                      // CRS in conventional (x,y) axis order.
        Exception                 warning  = null;                      // If failed to transform envelope.
        try {
            if (envelope != null) {
                normCRS = normalize(crs = envelope.getCoordinateReferenceSystem());
                if (normCRS != crs) {
                    envelope = Envelopes.transform(envelope, normCRS);      // Should only change order and sign.
                }
            }
            if (position != null) {
                /*
                 * If only one of the envelope or the position objects specify a CRS, assume that the other object
                 * use the same CRS. If both the envelope and the position objects specify a CRS, the envelope CRS
                 * will have precedence and the "representative position" will be projected to that CRS.
                 */
                final CoordinateReferenceSystem posCRS = position.getCoordinateReferenceSystem();
                if (normCRS == null) {
                    normCRS = normalize(crs = posCRS);
                    if (normCRS != crs) {
                        envelope = Envelopes.transform(envelope, normCRS);  // Should only change order and sign.
                    }
                }
                if (bbox != null) {     // Compute geographic position only if there is a geographic bounding box.
                    GeographicCRS geogCRS = ReferencingUtilities.toNormalizedGeographicCRS(posCRS, false, false);
                    if (geogCRS != null) {
                        geopos = transform(position, posCRS, geogCRS);
                    }
                }
                position = transform(position, posCRS, normCRS);
            }
        } catch (FactoryException | TransformException e) {
            envelope = null;
            position = null;
            warning  = e;
        }
        /*
         * At this point we got the final geographic bounding box and/or envelope to write.
         * Since we will write the projected and geographic coordinates side-by-side in the same cells,
         * we need to format them in advance so we can compute their width for internal right-alignment.
         * We do the alignment ourselves instead of using TableAppender.setCellAlignment(ALIGN_RIGHT)
         * because we do not want (projected geographic) tuple to appear far on the right side if other
         * cells have long texts.
         */
        if (bbox != null || envelope != null) {
            final CoordinateSystem cs = (crs != null) ? crs.getCoordinateSystem() : null;
            String[]     geographic = null;
            String[]     projected  = null;
            String[]     unitSymbol = null;
            AngleFormat  geogFormat = null;
            NumberFormat projFormat = null;
            UnitFormat   unitFormat = null;
            int          maxGeogLength = 0;
            int          maxProjLength = 0;
            int          maxUnitLength = 0;
            boolean      showProj  = false;
            if (bbox != null || geopos != null) {
                geogFormat = (AngleFormat) getFormat(Angle.class);
                geographic = new String[BOUND_KEY.length];
                Arrays.fill(geographic, "");
            }
            if (envelope != null || position != null) {
                projFormat = (NumberFormat) getFormat(Number.class);
                unitFormat = (UnitFormat)   getFormat(Unit.class);
                projected  = new String[BOUND_KEY.length];
                unitSymbol = new String[BOUND_KEY.length];
                Arrays.fill(projected,  "");
                Arrays.fill(unitSymbol, "");
            }
            for (int i=0; i<BOUND_KEY.length; i++) {
                RoundingMode rounding = RoundingMode.FLOOR;
                double g = Double.NaN;
                double p = Double.NaN;
                int dimension = 0;
                switch (i) {
                    case 0: if (bbox     != null) g = bbox.getWestBoundLongitude();
                            if (envelope != null) p = envelope.getMinimum(0);
                            break;
                    case 2: if (bbox     != null) g = bbox.getEastBoundLongitude();
                            if (envelope != null) p = envelope.getMaximum(0);
                            rounding = RoundingMode.CEILING;
                            break;
                    case 3: if (bbox     != null) g = bbox.getSouthBoundLatitude();
                            if (envelope != null) p = envelope.getMinimum(1);
                            dimension = 1;
                            break;
                    case 5: if (bbox     != null) g = bbox.getNorthBoundLatitude();
                            if (envelope != null) p = envelope.getMaximum(1);
                            rounding = RoundingMode.CEILING;
                            dimension = 1;
                            break;
                    case 4: dimension = 1;                            // Fall through
                    case 1: if (geopos   != null) g = geopos  .getOrdinate(dimension);
                            if (position != null) p = position.getOrdinate(dimension);
                            rounding = RoundingMode.HALF_EVEN;
                            break;
                }
                if (!Double.isNaN(p)) {
                    showProj |= (g != p);
                    if (cs != null) {
                        final Unit<?> unit = cs.getAxis(dimension).getUnit();
                        if (unit != null) {
                            final int length = (unitSymbol[i] = unitFormat.format(unit)).length();
                            if (length > maxUnitLength) {
                                maxUnitLength = length;
                            }
                        }
                    }
                    try {
                        projFormat.setRoundingMode(rounding);
                    } catch (UnsupportedOperationException e) {
                        // Ignore.
                    }
                    final int length = (projected[i] = projFormat.format(p)).length();
                    if (length > maxProjLength) {
                        maxProjLength = length;
                    }
                }
                if (!Double.isNaN(g)) {
                    geogFormat.setRoundingMode(rounding);
                    final Angle angle = (dimension == 0) ? new Longitude(g) : new Latitude(g);
                    final int length = (geographic[i] = geogFormat.format(angle)).length();
                    if (length > maxGeogLength) {
                        maxGeogLength = length;
                    }
                }
            }
            if (!showProj) {
                projected  = null;          // All projected coordinates are identical to geographic ones.
                unitSymbol = null;
                maxProjLength = 0;
                maxUnitLength = 0;
            } else if (maxProjLength != 0) {
                if (maxUnitLength != 0) {
                    maxUnitLength++;
                }
                maxGeogLength += 4;         // Arbitrary space between projected and geographic coordinates.
            }
            /*
             * At this point all coordinates have been formatted in advance.
             */
            final String separator = (projected != null && geographic != null) ? "    —" : "";
            for (int i=0; i<BOUND_KEY.length; i++) {
                final String p = (projected  != null) ? projected [i] : "";
                final String u = (unitSymbol != null) ? unitSymbol[i] : "";
                final String g = (geographic != null) ? geographic[i] : "";
                if (!p.isEmpty() || !g.isEmpty()) {
                    vocabulary.appendLabel(BOUND_KEY[i], table);
                    nextColumn(table);
                    table.append(CharSequences.spaces(maxProjLength - p.length())).append(p);
                    table.append(CharSequences.spaces(maxUnitLength - u.length())).append(u).append(separator);
                    table.append(CharSequences.spaces(maxGeogLength - g.length())).append(g);
                    table.nextLine();
                }
            }
        }
        if (crs != null) {
            append(table, vocabulary, Vocabulary.Keys.CoordinateRefSys, IdentifiedObjects.getDisplayName(crs, locale));
        }
        /*
         * Organization responsible for defining the characteristics of the location instance.
         */
        final AbstractParty administrator = location.getAdministrator();
        if (administrator != null) {
            append(table, vocabulary, Vocabulary.Keys.Administrator, toString(administrator.getName(), locale));
        }
        table.appendHorizontalSeparator();
        table.flush();
        if (warning != null) {
            vocabulary.appendLabel(Vocabulary.Keys.Warnings, toAppendTo);
            toAppendTo.append(warning.toString()).append(lineSeparator);
        }
    }

    /**
     * Creates the format to use for formatting a latitude, longitude or projected coordinate.
     * This method is invoked by {@code format(Location, Appendable)} when first needed.
     *
     * @param  valueType  {@code Angle.class}. {@code Number.class} or {@code Unit.class}.
     * @return a new {@link AngleFormat}, {@link NumberFormat} or {@link UnitFormat} instance
     *         depending on the argument value.
     */
    @Override
    protected Format createFormat(final Class<?> valueType) {
        final Format f = super.createFormat(valueType);
        if (f instanceof NumberFormat) {
            final var nf = (NumberFormat) f;
            nf.setMinimumFractionDigits(0);
            nf.setMaximumFractionDigits(0);                     // 1 metre accuracy, assuming lengths in metres.
        } else if (f instanceof AngleFormat) {
            ((AngleFormat) f).applyPattern("D°MM′SS″");         // 30 metres accuracy.
        }
        return f;
    }

    /**
     * Appends the given value in the given table if it is not null.
     *
     * @param table        the table where to append the value.
     * @param vocabulary   localized resources for the labels.
     * @param key          key of the label to append.
     * @param value        value to append, or {@code null} if none.
     */
    private void append(final TableAppender table, final Vocabulary vocabulary, final short key, final String value)
            throws IOException
    {
        if (value != null) {
            vocabulary.appendLabel(key, table);
            nextColumn(table);
            table.append(value).nextLine();
        }
    }

    /**
     * Moves to the next column.
     */
    private void nextColumn(final TableAppender table) {
        table.append(beforeFill).nextColumn(fillCharacter);
    }

    /**
     * Unsupported operation.
     *
     * @param  text  the character sequence for the location to parse.
     * @param  pos   the position where to start the parsing.
     * @return the parsed location, or {@code null} if the text is not recognized.
     * @throws ParseException if an error occurred while parsing the location.
     */
    @Override
    public AbstractLocation parse(CharSequence text, ParsePosition pos) throws ParseException {
        throw new ParseException(Errors.format(Errors.Keys.UnsupportedOperation_1, "parse"), pos.getIndex());
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    public LocationFormat clone() {
        return (LocationFormat) super.clone();
    }
}
