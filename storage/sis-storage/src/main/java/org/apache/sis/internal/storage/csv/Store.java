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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Duration;
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
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.IndexedResourceBundle;

// Branch-dependent imports
import java.time.Instant;
import java.time.DateTimeException;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.apache.sis.setup.OptionKey;


/**
 * A data store which creates feature instances from a CSV file using the OGC Moving Features specification.
 * See package javadoc for more information on the syntax.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.8
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
     * The character encoding, or {@code null} if unspecified (in which case the platform default is assumed).
     */
    private final Charset encoding;

    /**
     * The metadata object, or {@code null} if not yet created.
     */
    private transient DefaultMetadata metadata;

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
    private final List<Feature> features;

    /**
     * {@code true} if {@link #featureType} contains a trajectory column.
     */
    private boolean hasTrajectories;

    /**
     * Appearing order of trajectories, or {@code null} if unspecified.
     *
     * @see #parseFoliation(List)
     */
    private final Foliation foliation;

    /**
     * Specifies how time is encoded in the CSV file, or {@code null} if there is no time.
     */
    private TimeEncoding timeEncoding;

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
        Foliation       foliation   = null;
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
                            throw new DataStoreContentException(duplicated("@stboundedby"));
                        }
                        envelope = parseEnvelope(elements);
                        break;
                    }
                    case "@columns": {
                        if (featureType != null) {
                            throw new DataStoreContentException(duplicated("@columns"));
                        }
                        featureType = parseFeatureType(elements);
                        if (foliation == null) {
                            foliation = Foliation.TIME;
                        }
                        break;
                    }
                    case "@foliation": {
                        if (foliation != null) {
                            throw new DataStoreContentException(duplicated("@foliation"));
                        }
                        foliation = parseFoliation(elements);
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
        this.encoding    = connector.getOption(OptionKey.ENCODING);
        this.envelope    = envelope;
        this.featureType = featureType;
        this.foliation   = foliation;
        this.features    = new ArrayList<>();
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
                        case "day":      timeUnit = NonSI.DAY;    break;
                        case "absolute": isTimeAbsolute = true;   break;
                        default: throw new DataStoreContentException(errors().getString(Errors.Keys.UnknownUnit_1, unit));
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
                        default: throw new DataStoreContentException(errors().getString(
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
                final TemporalCRS temporal;
                if (isTimeAbsolute) {
                    temporal = TimeEncoding.DEFAULT.crs();
                    timeEncoding = TimeEncoding.ABSOLUTE;
                } else {
                    temporal = builder.createTemporalCRS(Date.from(startTime), timeUnit);
                    timeEncoding = new TimeEncoding(temporal.getDatum(), timeUnit);
                }
                components[count++] = temporal;
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
                    throw new DataStoreContentException(errors().getString(
                            Errors.Keys.MismatchedDimension_2, dim, spatialDimension));
                }
                for (int i=0; i<spatialDimension; i++) {
                    envelope.setRange(i, lowerCorner[i], upperCorner[i]);
                }
            }
            if (startTime != null && endTime != null) {
                envelope.setRange(spatialDimension, timeEncoding.toCRS(startTime.toEpochMilli()),
                                                    timeEncoding.toCRS(endTime.toEpochMilli()));
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
                        default: throw new DataStoreContentException(errors().getString(Errors.Keys.UnknownType_1, tn));
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
                            hasTrajectories = true;
                            if (timeEncoding != null) {
                                properties.add(createProperty("startTime", Instant.class, 1));
                                properties.add(createProperty(  "endTime", Instant.class, 1));
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
    private Foliation parseFoliation(final List<String> elements) {
        if (elements.size() >= 2) {
            return Foliation.valueOf(elements.get(1).toUpperCase(Locale.US));
        }
        return Foliation.TIME;      // Default value.
    }

    /**
     * Returns the metadata associated to the CSV file, or {@code null} if none.
     *
     * @return The metadata associated to the CSV file, or {@code null} if none.
     * @throws DataStoreException if an error occurred during the parsing process.
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final MetadataBuilder builder = new MetadataBuilder();
            builder.add(encoding);
            try {
                builder.add(envelope);
            } catch (TransformException e) {
                throw new DataStoreContentException(errors().getString(Errors.Keys.CanNotParseFile_2, "CSV", name), e);
            }
            metadata = builder.result();
        }
        return metadata;
    }

    /**
     * Returns an iterator over the features.
     *
     * @todo THIS IS AN EXPERIMENTAL API. We may change the return type to {@link java.util.stream.Stream} later.
     * @todo Current implementation is inefficient. We should not parse all features immediately.
     *
     * @return An iterator over all features in the CSV file.
     * @throws DataStoreException if an error occurred while creating the iterator.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "fallthrough"})
    public Iterator<Feature> getFeatures() throws DataStoreException {
        if (features.isEmpty()) try {
            final Collection<? extends PropertyType> properties = featureType.getProperties(false);
            final ObjectConverter<String,?>[] converters = new ObjectConverter[properties.size()];
            final String[]     propertyNames   = new String[converters.length];
            final boolean      hasTrajectories = this.hasTrajectories;
            final TimeEncoding timeEncoding    = this.timeEncoding;
            final List<String> values          = new ArrayList<>();
            int i = -1;
            for (final PropertyType p : properties) {
                propertyNames[++i] = p.getName().tip().toString();
                switch (i) {    // This switch shall follow the same cases than the swith in the loop.
                    case 1:
                    case 2: if (timeEncoding != null) continue;     // else fall through
                    case 3: if (hasTrajectories) continue;
                }
                converters[i] = ObjectConverters.find(String.class, ((AttributeType) p).getValueClass());
            }
            /*
             * Above lines prepared the constants. Now parse all lines.
             * TODO: We should move the code below this point in a custom Iterator implementation.
             */
            String line;
            while ((line = source.readLine()) != null) {
                split(line, values);
                final int length = Math.min(propertyNames.length, values.size());
                if (length != 0) {
                    final Feature feature = featureType.newInstance();
                    for (i=0; i<length; i++) {
                        final String text = values.get(i);
                        final String name = propertyNames[i];
                        final Object value;
                        /*
                         * According Moving Features specification:
                         *   Column 0 is the feature identifier (mfidref). There is nothing special to do here.
                         *   Column 1 is the start time.
                         *   Column 2 is the end time.
                         *   Column 3 is the trajectory.
                         *   Columns 4+ are custom attributes.
                         *
                         * TODO: we should replace that switch case by custom ObjectConverter.
                         */
                        switch (i) {
                            case 1:
                            case 2: {
                                if (timeEncoding != null) {
                                    if (timeEncoding == TimeEncoding.ABSOLUTE) {
                                        value = Instant.parse(text).toEpochMilli();
                                    } else {
                                        value = Instant.ofEpochMilli(timeEncoding.toMillis(Double.parseDouble(text)));
                                    }
                                    break;
                                }
                                /*
                                 * If there is no time columns, then this column may the trajectory (note that allowing
                                 * CSV files without time is obviously a departure from Moving Features specification.
                                 * The intend is to have a CSV format applicable to other features than moving ones).
                                 * Fall through in order to process trajectory.
                                 */
                            }
                            case 3: {
                                if (hasTrajectories) {
                                    value = CharSequences.parseDoubles(text, ORDINATE_SEPARATOR);
                                    break;
                                }
                                /*
                                 * If there is no trajectory columns, than this column is a custum attribute.
                                 * CSV files without trajectories are not compliant with Moving Feature spec.,
                                 * but we try to keep this reader a little bit more generic.
                                 */
                            }
                            default: {
                                value = converters[i].apply(text);
                                break;
                            }
                        }
                        feature.setPropertyValue(name, value);
                    }
                    features.add(feature);
                }
                values.clear();
            }
        } catch (IOException | IllegalArgumentException | DateTimeException e) {
            throw new DataStoreException(errors().getString(Errors.Keys.CanNotParseFile_2, "CSV", name), e);
        }
        return features.iterator();
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
                        elements.add(decode(line, startAt, i, hasQuotes));
                        startAt = i+1;
                        hasQuotes = false;
                    }
                    break;
                }
            }
        }
        elements.add(decode(line, startAt, length, hasQuotes));
    }

    /**
     * Extracts a substring from the given line and replaces double quotes by single quotes.
     *
     * @todo Needs also to check escape characters {@code \s \t \b &lt; &gt; &amp; &quot; &apos;}.
     * @todo Should modify double quote policy: process only if the text had quotes at the beginning and end.
     *       Those "todo" should be done only when we detected that the CSV file is a moving features file.
     */
    private static String decode(CharSequence text, final int lower, final int upper, final boolean hasQuotes) {
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
