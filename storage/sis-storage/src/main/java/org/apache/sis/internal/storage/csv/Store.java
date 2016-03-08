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
package org.apache.sis.internal.storage.csv;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.DateTimeException;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Duration;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.metadata.Metadata;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.internal.storage.MetadataHelper;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.IndexedResourceBundle;


/**
 * A data store which creates feature instances from a CSV file using the OGC Moving Features specification.
 * See package javadoc for more information on the syntax.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class Store extends DataStore {
    /**
     * The character at the beginning of lines to ignore in the header.
     * Note that this is not part of OGC Moving Feature Specification.
     */
    private static final char COMMENT = '#';

    /**
     * The character at the beginning of metadata lines.
     */
    private static final char METADATA = '@';

    /**
     * The quote character. Quotes inside quoted texts must be doubled.
     */
    private static final char QUOTE = '"';

    /**
     * The column separator.
     */
    private static final char SEPARATOR = ',';

    /**
     * The separator between ordinate values in a coordinate.
     */
    private static final char ORDINATE_SEPARATOR = ' ';

    /**
     * The prefix for elements in the {@code @columns} line that specify the data type.
     * Examples: {@code xsd:boolean}, {@code xsd:decimal}, {@code xsd:integer}, <i>etc</i>.
     */
    private static final String TYPE_PREFIX = "xsd:";

    /**
     * The file name.
     */
    private final String name;

    /**
     * The reader, set by the constructor and cleared when no longer needed.
     */
    private BufferedReader source;

    /**
     * The metadata object. Initialized to a minimal amount of information, then completed when first needed.
     */
    private final DefaultMetadata metadata;

    /**
     * Values for {@link #order} specifying the appearing order of trajectories.
     */
    private static final byte ORDER_TIME = 1, ORDER_SEQUENTIAL = 2;

    /**
     * Appearing order of trajectories: 0 = undefined, 1 = ordered by time,
     * 2 = elements which are parts of one track are ordered by time.
     *
     * @see #parseFoliation(List)
     */
    private final byte order;

    /**
     * Values for {@link #timeEncoding} specifying how time is encoded in the CSV file.
     */
    private static final byte TIME_RELATIVE = 1, TIME_ABSOLUTE = 2;

    /**
     * A code for the time encoding: 0 = no time, 1 = relative time, 2 = absolute time.
     * Relative times are formatted as number of seconds or minutes since an epoch.
     * Absolute times are formatted as ISO dates.
     */
    private byte timeEncoding;

    /**
     * Date of value zero on the time axis, in milliseconds since January 1st 1970 at midnight UTC.
     */
    private long timeOrigin;

    /**
     * Number of milliseconds between two consecutive integer values on the time axis.
     */
    private double timeInterval;

    /**
     * The three- or four-dimensional envelope together with the CRS.
     * This envelope contains a vertical component if the feature trajectories are 3D,
     * and a temporal component if the CSV file contains a start time and end time.
     *
     * @see #parseEnvelope(List)
     */
    private final GeneralEnvelope envelope;

    /**
     * Description of the columns found in the CSV file.
     *
     * @see #parseFeatureType(List)
     */
    private final FeatureType featureType;

    /**
     * The features created while parsing the CSV file.
     *
     * @todo We should not keep them in memory, but instead use some kind of iterator or stream.
     */
    private final Map<String,Feature> features;

    /**
     * Creates a new CSV store from the given file, URL or stream.
     *
     * <p>If the CSV file is known to be a Moving Feature file, then the given connector should
     * have an {@link org.apache.sis.setup.OptionKey#ENCODING} associated to the UTF-8 value.</p>
     *
     * @param  connector Information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    public Store(final StorageConnector connector) throws DataStoreException {
        name = connector.getStorageName();
        final Reader r = connector.getStorageAs(Reader.class);
        connector.closeAllExcept(r);
        if (r == null) {
            throw new DataStoreException(Errors.format(Errors.Keys.CanNotOpen_1, name));
        }
        source = (r instanceof BufferedReader) ? (BufferedReader) r : new LineNumberReader(r);
        GeneralEnvelope envelope    = null;
        FeatureType     featureType = null;
        byte            order       = 0;
        try {
            final List<String> elements = new ArrayList<>();
            source.mark(1024);
            String line;
            while ((line = source.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                final char c = line.charAt(0);
                if (c == COMMENT) continue;
                if (c != METADATA) break;
                split(line, elements);
                final String keyword = elements.get(0);
                switch (keyword.toLowerCase(Locale.US)) {
                    case "@stboundedby": {
                        if (envelope != null) {
                            throw new DataStoreException(duplicated("@stboundedby"));
                        }
                        envelope = parseEnvelope(elements);
                        break;
                    }
                    case "@columns": {
                        if (featureType != null) {
                            throw new DataStoreException(duplicated("@columns"));
                        }
                        featureType = parseFeatureType(elements);
                        break;
                    }
                    case "@foliation": {
                        if (order != 0) {
                            throw new DataStoreException(duplicated("@foliation"));
                        }
                        order = parseFoliation(elements);
                        break;
                    }
                    default: {
                        final LogRecord record = errors().getLogRecord(Level.WARNING, Errors.Keys.UnknownKeyword_1, keyword);
                        record.setSourceClassName(Store.class.getName());
                        record.setSourceMethodName("parseHeader");
                        listeners.warning(record);
                        break;
                    }
                }
                elements.clear();
                source.mark(1024);
            }
            source.reset();
        } catch (IOException | FactoryException | IllegalArgumentException | DateTimeException e) {
            throw new DataStoreException(errors().getString(Errors.Keys.CanNotParseFile_2, "CSV", name), e);
        }
        this.envelope    = envelope;
        this.featureType = featureType;
        this.order       = order;
        this.metadata    = MetadataHelper.createForTextFile(connector);
        this.features    = new LinkedHashMap<>();
    }

    /**
     * Parses the envelope described by the header line starting with {@code @stboundedby}.
     * The envelope returned by this method will be stored in the {@link #envelope} field.
     *
     * <p>Example:</p>
     * {@preformat text
     *   &#64;stboundedby, urn:ogc:def:crs:CRS:1.3:84, 2D, 50.23 9.23, 50.31 9.27, 2012-01-17T12:33:41Z, 2012-01-17T12:37:00Z, sec
     * }
     *
     * @param  elements The line elements. The first elements should be {@code "@stboundedby"}.
     * @return The envelope, or {@code null} if the given list does not contain enough elements.
     */
    @SuppressWarnings("fallthrough")
    private GeneralEnvelope parseEnvelope(final List<String> elements) throws DataStoreException, FactoryException {
        double[]       lowerCorner    = null;
        double[]       upperCorner    = null;
        Instant        startTime      = null;
        Instant        endTime        = null;
        Unit<Duration> timeUnit       = SI.SECOND;
        boolean        isTimeAbsolute = false;
        boolean        is3D           = false;
        CoordinateReferenceSystem crs = null;
        GeneralEnvelope envelope      = null;
        switch (elements.size()) {
            default:final String unit = elements.get(7);
                    switch (unit.toLowerCase(Locale.US)) {
                        case "":
                        case "sec":
                        case "second":   /* Already SI.SECOND. */ break;
                        case "minute":   timeUnit = NonSI.MINUTE; break;
                        case "hour":     timeUnit = NonSI.HOUR;   break;
                        case "absolute": isTimeAbsolute = true;   // Fall through
                        case "day":      timeUnit = NonSI.DAY;    break;
                        default: throw new DataStoreException(errors().getString(Errors.Keys.UnknownUnit_1, unit));
                    }
                    // Fall through
            case 7: endTime     = Instant      .parse(       elements.get(6));
            case 6: startTime   = Instant      .parse(       elements.get(5));
            case 5: upperCorner = CharSequences.parseDoubles(elements.get(4), ORDINATE_SEPARATOR);
            case 4: lowerCorner = CharSequences.parseDoubles(elements.get(3), ORDINATE_SEPARATOR);
            case 3: final String dimension = elements.get(2);
                    switch (dimension.toUpperCase(Locale.US)) {
                        case "":   // Default to 2D.
                        case "2D": break;
                        case "3D": is3D = true; break;
                        default: throw new DataStoreException(errors().getString(
                                        Errors.Keys.IllegalCoordinateSystem_1, dimension));
                    }
                    // Fall through
            case 2: crs = CRS.forCode(elements.get(1));
            case 1:
            case 0:
        }
        /*
         * Complete the CRS by adding a vertical component if needed, then a temporal component.
         * Only after the CRS has been completed we can create the envelope.
         *
         * Vertical component:
         *   Ideally, should be part of the CRS created from the authority code. But if the authority
         *   code is only for a two-dimensional CRS, we default to an arbitrary height component.
         *
         * Temporal component:
         *   Assumed never part of the authority code. We need to build the temporal component ourselves
         *   in order to set the origin to the start time.
         */
        if (crs != null) {
            final CoordinateReferenceSystem[] components = new CoordinateReferenceSystem[3];
            final int spatialDimension = crs.getCoordinateSystem().getDimension();
            int count = 0;
            components[count++] = crs;
            if (is3D && spatialDimension == 2) {
                components[count++] = CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
            }
            final GeodeticObjectBuilder builder = new GeodeticObjectBuilder();
            if (startTime != null) {
                TemporalCRS temporal = null;
                if (isTimeAbsolute) {
                    /*
                     * If time is absolute (i.e. is formatted as an ISO date), then the origin does not matter.
                     * In such case we favor the first pre-defined TemporalCRS with same unit of measurement.
                     */
                    for (final CommonCRS.Temporal c : CommonCRS.Temporal.values()) {
                        final TemporalCRS candidate = c.crs();
                        if (timeUnit.equals(candidate.getCoordinateSystem().getAxis(0).getUnit())) {
                            temporal = candidate;
                            break;
                        }
                    }
                }
                if (temporal == null) {
                    temporal = builder.createTemporalCRS(Date.from(startTime), timeUnit);
                }
                components[count++] = temporal;
                timeOrigin   = temporal.getDatum().getOrigin().getTime();
                timeInterval = timeUnit.getConverterTo(Units.MILLISECOND).convert(1);
                timeEncoding = isTimeAbsolute ? TIME_ABSOLUTE : TIME_RELATIVE;
            }
            crs = builder.addName(name).createCompoundCRS(ArraysExt.resize(components, count));
            /*
             * At this point we got the three- or four-dimensional spatio-temporal CRS.
             * We can now set the envelope coordinate values.
             */
            envelope = new GeneralEnvelope(crs);
            if (lowerCorner != null && upperCorner != null) {
                int dim;
                if ((dim = lowerCorner.length) != spatialDimension ||
                    (dim = upperCorner.length) != spatialDimension)
                {
                    throw new DataStoreException(errors().getString(
                            Errors.Keys.MismatchedDimension_2, dim, spatialDimension));
                }
                for (int i=0; i<spatialDimension; i++) {
                    envelope.setRange(i, lowerCorner[i], upperCorner[i]);
                }
            }
            if (startTime != null && endTime != null) {
                envelope.setRange(spatialDimension, (startTime.toEpochMilli() - timeOrigin) / timeInterval,
                                                      (endTime.toEpochMilli() - timeOrigin) / timeInterval);
            }
        }
        return envelope;
    }

    /**
     * Parses the columns metadata described by the header line starting with {@code @columns}.
     * The feature type returned by this method will be stored in the {@link #featureType} field.
     *
     * <p>Example:</p>
     * {@preformat text
     *   &#64;columns, mfidref, trajectory, state,xsd:token, "type code",xsd:integer
     * }
     *
     * @param  elements The line elements. The first elements should be {@code "@columns"}.
     * @return The column metadata, or {@code null} if the given list does not contain enough elements.
     */
    private FeatureType parseFeatureType(final List<String> elements) throws DataStoreException {
        final int size = elements.size();
        final List<PropertyType> properties = new ArrayList<>();
        for (int i=1; i<size; i++) {
            final String name = elements.get(i);
            Class<?> type = null;
            if (++i < size) {
                String tn = elements.get(i);
                if (!tn.isEmpty() && tn.regionMatches(true, 0, TYPE_PREFIX, 0, TYPE_PREFIX.length())) {
                    String st = tn.substring(TYPE_PREFIX.length()).toLowerCase(Locale.US);
                    switch (st) {
                        case "boolean":  type = Boolean.class; break;
                        case "decimal":  type = Double .class; break;
                        case "integer":  type = Integer.class; break;
                        case "string":   type = String .class; break;
                        case "datetime": type = Instant.class; break;
                        case "anyuri":   type = URI    .class; break;
                        default: throw new DataStoreException(errors().getString(Errors.Keys.UnknownType_1, tn));
                    }
                }
            }
            int minOccurrence = 0;
            if (type == null) {
                /*
                 * If the column name was not followed by a type, default to a String type except in the special
                 * case of trajectory. Note that according the Moving Feature specification, only the two first
                 * columns are not followed by a type. Those columns are:
                 *
                 *   1) mfidref     - used in order to identify the moving feature.
                 *   2) trajectory  - defines the spatio-temporal geometry of moving features.
                 *                    Contains implicit "start time" and "end time" columns.
                 *
                 * Those two columns are mandatory in Moving Feature specification. All other ones are optional.
                 */
                type = String.class;
                switch (--i) {
                    case 1: minOccurrence = 1; break;
                    case 2: {
                        if (name.equalsIgnoreCase("trajectory")) {
                            if (timeEncoding != 0) {
                                properties.add(createProperty("time", long[].class, 1));
                            }
                            type = double[].class;
                            minOccurrence = 1;
                        }
                        break;
                    }
                }
            }
            properties.add(createProperty(name, type, minOccurrence));
        }
        return new DefaultFeatureType(Collections.singletonMap(DefaultFeatureType.NAME_KEY, name),
                false, null, properties.toArray(new PropertyType[properties.size()]));
    }

    /**
     * Creates a property type for the given name and type.
     */
    private static PropertyType createProperty(final String name, final Class<?> type, final int minOccurrence) {
        return new DefaultAttributeType<>(Collections.singletonMap(DefaultAttributeType.NAME_KEY, name), type, minOccurrence, 1, null);
    }

    /**
     * Parses the metadata described by the header line starting with {@code @foliation}.
     * The value returned by this method will be stored in the {@link #order} field.
     *
     * <p>Example:</p>
     * {@preformat text
     *   &#64;foliation,Sequential
     * }
     *
     * @param  elements The line elements. The first elements should be {@code "@foliation"}.
     * @return The foliation metadata.
     */
    private byte parseFoliation(final List<String> elements) throws DataStoreException {
        if (elements.size() >= 2) {
            final String value = elements.get(1);
            switch (value.toLowerCase(Locale.US)) {
                case "time":       break;
                case "sequential": return ORDER_SEQUENTIAL;
                default: throw new DataStoreException(errors().getString(Errors.Keys.UnknownEnumValue_2, "order", value));
            }
        }
        return ORDER_TIME;      // Default value.
    }

    /**
     * Returns the metadata associated to the CSV file, or {@code null} if none.
     *
     * @return The metadata associated to the CSV file, or {@code null} if none.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        if (metadata.isModifiable()) {
            try {
                MetadataHelper.add(metadata, envelope);
            } catch (TransformException e) {
                throw new DataStoreException(errors().getString(Errors.Keys.CanNotParseFile_2, "CSV", name), e);
            }
            metadata.freeze();
        }
        return metadata;
    }

    /**
     * Splits the content of the given line around the column separator.
     * Quotes are taken in account. The elements are added in the given list.
     *
     * @param line the line to parse.
     * @param elements an initially empty list where to add elements.
     */
    private static void split(final String line, final List<String> elements) {
        int startAt = 0;
        boolean hasQuotes = false;
        boolean isQuoting = false;
        final int length = line.length();
        for (int i=0; i<length; i++) {
            switch (line.charAt(i)) {
                case QUOTE: {
                    hasQuotes = true;
                    if (isQuoting && i+1 < length && line.charAt(i+1) == QUOTE) {
                        i++;
                    } else {
                        isQuoting = !isQuoting;
                    }
                    break;
                }
                case SEPARATOR: {
                    if (!isQuoting) {
                        elements.add(extract(line, startAt, i, hasQuotes));
                        startAt = i+1;
                        hasQuotes = false;
                    }
                    break;
                }
            }
        }
        elements.add(extract(line, startAt, length, hasQuotes));
    }

    /**
     * Extracts a substring from the given line and replaces double quotes by single quotes.
     */
    private static String extract(CharSequence text, final int lower, final int upper, final boolean hasQuotes) {
        if (hasQuotes) {
            final StringBuilder buffer = new StringBuilder(upper - lower).append(text, lower, upper);
            for (int i=0; i<buffer.length(); i++) {
                if (buffer.charAt(i) == QUOTE) {
                    buffer.deleteCharAt(i);
                    // If the deleted char was followed by another quote, that second quote will be preserved.
                }
            }
            text = CharSequences.trimWhitespaces(buffer);
        } else {
            text = CharSequences.trimWhitespaces(text, lower, upper).toString();
        }
        return text.toString();
    }

    /**
     * Returns an error message for a duplicated element.
     */
    private String duplicated(final String name) {
        return errors().getString(Errors.Keys.DuplicatedElement_1, name);
    }

    /**
     * Returns the resources to use for producing error messages.
     */
    private IndexedResourceBundle errors() {
        return Errors.getResources(getLocale());
    }

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException If an error occurred while closing this data store.
     */
    @Override
    public void close() throws DataStoreException {
        final BufferedReader s = source;
        source = null;                  // Cleared first in case of failure.
        features.clear();
        if (s != null) try {
            s.close();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }
}
