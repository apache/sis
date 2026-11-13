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
package org.apache.sis.storage.geojson;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.geojson.binding.GeoJSONFeature;
import org.apache.sis.storage.geojson.binding.GeoJSONFeatureCollection;
import org.apache.sis.storage.geojson.binding.GeoJSONObject;
import org.apache.sis.storage.geojson.binding.JSONMDDecoder;
import org.apache.sis.storage.geojson.binding.JSONMDEncoder;
import org.apache.sis.storage.geojson.binding.JSONMDMetadata;
import org.opengis.metadata.Metadata;

/**
 * Reads and writes SIS metadata in the ISO/TR 19115-4 JSON encoding.
 *
 * <p>A metadata record is a GeoJSON feature whose {@code properties} member holds the {@code MD_Metadata} content.
 * The encoding is defined by the JSON schemas published at <a href="https://schemas.isotc211.org/19115/-4/">https://schemas.isotc211.org/19115/-4/</a>.
 * The {@link #toFeature(Metadata, Locale)} and {@link #fromFeature(GeoJSONFeature)} methods convert between
 * SIS metadata and {@link GeoJSONFeature}, for including records in other GeoJSON documents.
 *
 * <p>When writing, the feature {@code id} is a copy of the metadata identifier code, and the {@code geometry} and {@code bbox}
 * are copies of the geographic bounding boxes. When reading, {@code geometry} and {@code bbox} are ignored since they
 * are derived copies, and {@code id} is used only if the metadata identifier is absent.
 *
 * <p>Constraints:
 * <ul>
 *   <li>a record is written as a single GeoJSON {@code Feature} with a {@code conformsTo} member,</li>
 *   <li>a GeoJSON {@code Feature} or {@code FeatureCollection} can be read,</li>
 *   <li>texts are written in a single locale, the default locale of the metadata if none is given,</li>
 *   <li>values which cannot be encoded or decoded are skipped and logged as warnings
 *       in the {@link JSONMDEncoder#LOGGER_NAME} logger,</li>
 *   <li>streams given as argument are not closed.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 * @see "ISO-19115-4:2026 section 4.3"
 * @see "ISO-19115-4:2026 section 5.1"
 * @see "ISO-19115-4:2026 section 5.1.3 i)"
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/19115-4.json">19115-4.json#MetadataRecord</a>
 */
public final class MetadataJSON {

    /**
     * Conformance class of the ISO 19115-1 core profile.
     */
    public static final String CONFORMANCE_CORE = "https://standards.iso.org/iso/19115/-4/0.1/core";
    /**
     * Conformance class of the ISO 19157-1 core profile, declared when data quality information is present.
     */
    public static final String CONFORMANCE_DATA_QUALITY = "https://standards.iso.org/iso/19115/-4/0.1/data-quality-core";
    /**
     * Conformance class of documents without references, always declared since references are never written.
     */
    public static final String CONFORMANCE_NO_REFERENCE = "https://standards.iso.org/iso/19115/-4/0.1/no-ref";

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
            .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
            .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
            .build();

    private MetadataJSON() {
    }

    /**
     * Reads the single metadata record of a file.
     *
     * @throws IOException if the file cannot be read, is not valid JSON, or does not contain exactly one record.
     */
    public static DefaultMetadata read(final Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return read(in);
        }
    }

    /**
     * Reads the single metadata record of a stream.
     *
     * <p>The document can be a {@code Feature}, or a {@code FeatureCollection} with exactly one feature.
     *
     * @throws IOException if the stream cannot be read, is not valid JSON, or does not contain exactly one record.
     */
    public static DefaultMetadata read(final InputStream in) throws IOException {
        final List<DefaultMetadata> records = readAll(in);
        if (records.size() != 1) {
            throw new IOException("Expected one metadata record but found " + records.size() + ", use readAll(…) instead.");
        }
        return records.get(0);
    }

    /**
     * Reads all metadata records of a stream.
     *
     * <p>The document can be a {@code Feature} (one record) or a {@code FeatureCollection} (any number of records).
     *
     * @throws IOException if the stream cannot be read or is not a valid ISO 19115-4 document.
     */
    public static List<DefaultMetadata> readAll(final InputStream in) throws IOException {
        final JsonNode root = MAPPER.readTree(in);
        if (root == null || !root.isObject()) {
            throw new IOException("Expected a JSON object as root of the document.");
        }
        final String type = root.path(GeoJSONObject.PROPERTY_TYPE).asText();
        final List<GeoJSONFeature> features;
        if (GeoJSONObject.TYPE_FEATURE.equals(type)) {
            features = List.of(MAPPER.treeToValue(root, GeoJSONFeature.class));
        } else if (GeoJSONObject.TYPE_FEATURE_COLLECTION.equals(type)) {
            features = MAPPER.treeToValue(root, GeoJSONFeatureCollection.class).getFeatures();
        } else {
            throw new IOException("Expected a GeoJSON Feature or FeatureCollection, found type \"" + type + "\".");
        }
        final JSONMDDecoder decoder = new JSONMDDecoder();
        final List<DefaultMetadata> records = new ArrayList<>(features.size());
        for (final GeoJSONFeature feature : features) {
            if (feature == null) {
                throw new IOException("Expected a GeoJSON Feature object, found null.");
            }
            records.add(fromFeature(feature, decoder));
        }
        return records;
    }

    /**
     * Writes the metadata to a file, with texts in the default locale of the metadata.
     *
     * @throws IOException if the file cannot be written.
     */
    public static void write(final Metadata metadata, final Path path) throws IOException {
        try (OutputStream out = Files.newOutputStream(path)) {
            write(metadata, out);
        }
    }

    /**
     * Writes the metadata to a stream, with texts in the default locale of the metadata.
     *
     * <p>The default locale is the first locale of {@link Metadata#getLocalesAndCharsets()}, or {@code Locale.ROOT} if none.
     *
     * @throws IOException if the stream cannot be written.
     */
    public static void write(final Metadata metadata, final OutputStream out) throws IOException {
        write(metadata, defaultLocale(metadata), out);
    }

    /**
     * Writes the metadata to a stream, with texts in the given locale.
     *
     * @param locale locale of the JSON representation, typically the HTTP {@code Content-Language}.
     * @throws IOException if the stream cannot be written.
     * @see "ISO-19115-4:2026 section 4.8"
     */
    public static void write(final Metadata metadata, final Locale locale, final OutputStream out) throws IOException {
        final GeoJSONFeature feature = toFeature(metadata, locale);
        final List<String> conformsTo = new ArrayList<>(3);
        conformsTo.add(CONFORMANCE_CORE);
        if (feature.getProperties().get(JSONMDMetadata.PROPERTY_DATA_QUALITY_INFO) instanceof Collection<?> c && !c.isEmpty()) {
            conformsTo.add(CONFORMANCE_DATA_QUALITY);
        }
        conformsTo.add(CONFORMANCE_NO_REFERENCE);
        feature.setConformsTo(conformsTo);
        MAPPER.writeValue(out, feature);
    }

    /**
     * Converts the metadata to a metadata record, with texts in the given locale.
     *
     * <p>The returned feature has no {@code conformsTo} member, as required for a feature nested in a collection.
     * Callers writing the feature as a standalone document should declare the conformance classes.
     *
     * @param locale locale of the JSON representation, or {@code null} for the default locale of the metadata.
     * @return the metadata record.
     */
    public static GeoJSONFeature toFeature(final Metadata metadata, Locale locale) {
        if (locale == null) {
            locale = defaultLocale(metadata);
        }
        final JSONMDMetadata properties = new JSONMDMetadata(metadata, new JSONMDEncoder(locale));
        final GeographicFootprint footprint = new GeographicFootprint(metadata);
        final GeoJSONFeature feature = new GeoJSONFeature();
        if (properties.getMetadataIdentifier() != null) {
            feature.setId(properties.getMetadataIdentifier().getCode());
        }
        feature.setGeometry(footprint.toGeometry());
        final List<Double> bbox = footprint.toBbox();
        if (bbox != null) {
            feature.setBbox(new ArrayList<>(bbox));
        }
        feature.setProperties(MAPPER.convertValue(properties, MAP_TYPE));
        return feature;
    }

    /**
     * Converts a metadata record to SIS metadata.
     *
     * @param feature the metadata record.
     * @return the metadata of the record.
     * @throws IOException if the {@code properties} member cannot be parsed.
     */
    public static DefaultMetadata fromFeature(final GeoJSONFeature feature) throws IOException {
        return fromFeature(feature, new JSONMDDecoder());
    }

    /**
     * Converts a metadata record to SIS metadata using the given decoder.
     */
    private static DefaultMetadata fromFeature(final GeoJSONFeature feature, final JSONMDDecoder decoder) throws IOException {
        final Map<String, Object> content = feature.getProperties();
        final JSONMDMetadata properties = (content == null) ? new JSONMDMetadata()
                : MAPPER.treeToValue(MAPPER.valueToTree(content), JSONMDMetadata.class);
        final DefaultMetadata metadata = properties.toMetadata(decoder);
        final Object id = feature.getId();
        if (metadata.getMetadataIdentifier() == null && id != null) {
            metadata.setMetadataIdentifier(new DefaultIdentifier(id.toString()));
        }
        return metadata;
    }

    private static Locale defaultLocale(final Metadata metadata) {
        final Map<Locale, ?> locales = metadata.getLocalesAndCharsets();
        if (locales != null) {
            final Iterator<Locale> it = locales.keySet().iterator();
            if (it.hasNext()) {
                final Locale locale = it.next();
                if (locale != null) {
                    return locale;
                }
            }
        }
        return Locale.ROOT;
    }
}
