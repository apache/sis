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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.IntFunction;
import java.io.IOException;
import static java.lang.Math.addExact;
import static javax.imageio.plugins.tiff.GeoTIFFTagSet.*;
import static javax.imageio.plugins.tiff.BaselineTIFFTagSet.*;
import org.apache.sis.math.Vector;
import org.apache.sis.math.NumberType;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.geotiff.base.CompressionMethod;
import org.apache.sis.storage.geotiff.base.Predictor;
import org.apache.sis.storage.geotiff.base.GeoKeys;
import org.apache.sis.storage.geotiff.base.Tags;
import org.apache.sis.storage.geotiff.reader.Type;
import org.apache.sis.storage.geotiff.reader.GeoKeysLoader;
import org.apache.sis.storage.geotiff.reader.XMLMetadata;


/**
 * View over GeoTIFF tags and GeoTIFF keys in their "raw" form (without interpretation).
 * Used only when showing {@linkplain GeoTiffStore#getNativeMetadata() native metadata}.
 *
 * <p>This implementation is inefficient because it performs a lot of "seek" operations.
 * This class does not make any effort for reading data in a more sequential way.
 * The performance penalty should not matter because this class should not be used except
 * for debugging purposes (the normal use is to interpret tags as they are read).</p>
 *
 * <p>This class is thread-safe if the user does not try to write in the tree.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class NativeMetadata extends GeoKeysLoader {
    /**
     * Column for the tag number or GeoKey number.
     */
    private static final TableColumn<Integer> CODE  = new TableColumn<>(Integer.class,
            Vocabulary.formatInternational(Vocabulary.Keys.Code));

    /**
     * Column for the name associated to the tag.
     * Value may be null if the name is unknown.
     */
    private static final TableColumn<CharSequence> NAME = TableColumn.NAME;

    /**
     * Column for the value associated to the tag.
     */
    private static final TableColumn<Object> VALUE = TableColumn.VALUE;

    /**
     * The stream from which to read the data.
     */
    private ChannelDataInput input;

    /**
     * {@code true} if reading classic TIFF file, or {@code false} for BigTIFF.
     */
    private boolean isClassic;

    /**
     * The node for GeoKeys, or {@code null} if none.
     */
    private TreeTable.Node geoNode;

    /**
     * Resources for vocabulary.
     */
    private final Vocabulary vocabulary;

    /**
     * Creates a reader for a tree table of native metadata.
     */
    NativeMetadata(final Locale locale) {
        vocabulary = Vocabulary.forLocale(locale);
    }

    /**
     * Reads the tree table content. This method assumes that the caller already verified that the
     * file is a GeoTIFF file. Tags are keys are added in the order they are declared in the file.
     */
    final DefaultTreeTable read(final Reader reader) throws IOException {
        input     = reader.input;
        isClassic = reader.intSizeExpansion == 0;
        final int offsetSize = Integer.BYTES << reader.intSizeExpansion;
        final var table = new DefaultTreeTable(CODE, NAME, VALUE);
        final TreeTable.Node root = table.getRoot();
        root.setValue(NAME, "TIFF");
        input.mark();
        try {
            input.seek(isClassic ? 2*Short.BYTES : 4*Short.BYTES);
            final Set<Long> doneIFD = new HashSet<>();
            long nextIFD;
            /*
             * Following loop is a simplified copy of `Reader.getImageFileDirectory(int)` method,
             * without the "deferred entries" mechanism. Instead, we seek immediately.
             */
            int imageNumber = 0;
            while ((nextIFD = readInt(false)) != 0) {
                if (!doneIFD.add(nextIFD)) {
                    // Safety against infinite recursion.
                    break;
                }
                final TreeTable.Node image = root.newChild();
                image.setValue(NAME, vocabulary.getString(Vocabulary.Keys.Image_1, imageNumber));
                input.seek(nextIFD);
                for (long remaining = readInt(true); --remaining >= 0;) {
                    final short tag  = (short) input.readUnsignedShort();
                    final Type type  = Type.valueOf(input.readShort());        // May be null.
                    final long count = readInt(false);
                    final long size  = (type != null) ? Math.multiplyExact(type.size, count) : 0;
                    final long next  = addExact(input.getStreamPosition(), offsetSize);
                    boolean visible;
                    /*
                     * Exclude the tags about location of tiles in the GeoTIFF files.
                     * Values of those tags are potentially large and rarely useful for human reading.
                     * This switch is only about tags to skip; special handlings of some tags are done later.
                     */
                    switch (tag) {
                        case TAG_TILE_OFFSETS:
                        case TAG_STRIP_OFFSETS:
                        case TAG_TILE_BYTE_COUNTS:
                        case TAG_STRIP_BYTE_COUNTS: visible = false; break;
                        default: visible = (size != 0); break;
                    }
                    if (visible) {
                        if (size > offsetSize) {
                            final long offset = readInt(false);
                            input.seek(offset);
                        }
                        /*
                         * Some tags need to be handle in a special way. The main cases are GeoTIFF keys.
                         * But other cases exist (e.g. GEO_METADATA and GDAL_METADATA).
                         */
                        Object value = null;
                        XMLMetadata children = null;
                        switch (tag) {
                            case (short) TAG_GEO_KEY_DIRECTORY: {
                                writeGeoKeys();             // Flush previous keys if any (should never happen).
                                keyDirectory = type.readAsVector(input, count);
                                value = "GeoTIFF";
                                break;
                            }
                            case (short) TAG_GEO_DOUBLE_PARAMS: {
                                numericParameters = type.readAsVector(input, count);
                                visible = false;
                                break;
                            }
                            case (short) TAG_GEO_ASCII_PARAMS: {
                                setAsciiParameters(type.readAsStrings(input, count, reader.store.encoding));
                                visible = false;
                                break;
                            }
                            case Tags.GDAL_METADATA:
                            case Tags.GEO_METADATA: {
                                children = reader.readXML(type, count, tag);
                                if (children.isEmpty()) {
                                    // Fallback on showing array of numerical values.
                                    value = type.readAsVector(input, count);
                                }
                                break;
                            }
                            default: {
                                value = type.readAsObject(input, count);
                                if (value instanceof Vector) {
                                    final Vector v = (Vector) value;
                                    switch (v.size()) {
                                        case 0: value = null; break;
                                        case 1: value = v.get(0); break;
                                    }
                                }
                                /*
                                 * Replace a few numerical values by a more readable string when available.
                                 * The first items are public enumerations. Other items are tag values that
                                 * we continue to handle as integers for now.
                                 */
                                switch (tag) {
                                    case TAG_COMPRESSION: value = toName(value, CompressionMethod::valueOf); break;
                                    case TAG_PREDICTOR:   value = toName(value, Predictor::valueOf); break;
                                    case TAG_PLANAR_CONFIGURATION: {
                                        value = toString(value, (code) -> {
                                            switch (code) {
                                                case PLANAR_CONFIGURATION_CHUNKY: return "Chunky";
                                                case PLANAR_CONFIGURATION_PLANAR: return "Planar";
                                                default: return "Unknown";
                                            }
                                        });
                                        break;
                                    }
                                    case TAG_FILL_ORDER: {
                                        value = toString(value, (code) -> {
                                            switch (code) {
                                                case FILL_ORDER_LEFT_TO_RIGHT: return "Left to right";
                                                case FILL_ORDER_RIGHT_TO_LEFT: return "Right to left";
                                                default: return "Unknown";
                                            }
                                        });
                                        break;
                                    }
                                    case TAG_SAMPLE_FORMAT: {
                                        value = toString(value, (code) -> {
                                            switch (code) {
                                                case SAMPLE_FORMAT_UNSIGNED_INTEGER: return "Unsigned integer";
                                                case SAMPLE_FORMAT_SIGNED_INTEGER:   return "Signed integer";
                                                case SAMPLE_FORMAT_FLOATING_POINT:   return "Floating point";
                                                case SAMPLE_FORMAT_UNDEFINED:        return "Undefined";
                                                default: return "Unknown";
                                            }
                                        });
                                        break;
                                    }
                                    case TAG_PHOTOMETRIC_INTERPRETATION: {
                                        value = toString(value, (code) -> {
                                            switch (code) {
                                                case PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO:     return "Black is zero";
                                                case PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO:     return "White is zero";
                                                case PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR:     return "Palette color";
                                                case PHOTOMETRIC_INTERPRETATION_RGB:               return "RGB";
                                                case PHOTOMETRIC_INTERPRETATION_CMYK:              return "CMYK";
                                                case PHOTOMETRIC_INTERPRETATION_CIELAB:            return "CIELAB";
                                                case PHOTOMETRIC_INTERPRETATION_ICCLAB:            return "ICCLAT";
                                                case PHOTOMETRIC_INTERPRETATION_Y_CB_CR:           return "YCbCr";
                                                case PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK: return "Transparency";
                                                default: return "Unknown";
                                            }
                                        });
                                        break;
                                    }
                                    case TAG_SUBFILE_TYPE: {
                                        value = toString(value, (code) -> {
                                            switch (code) {
                                                case SUBFILE_TYPE_FULL_RESOLUTION:    return "Full resolution";
                                                case SUBFILE_TYPE_REDUCED_RESOLUTION: return "Reduced resolution";
                                                case SUBFILE_TYPE_SINGLE_PAGE:        return "Single page";
                                                default: return "Unknown";
                                            }
                                        });
                                        break;
                                    }
                                    case TAG_NEW_SUBFILE_TYPE: {
                                        value = toString(value, (code) -> {
                                            final var items = new ArrayList<String>();
                                            while (code != 0) {
                                                final int bit = Integer.lowestOneBit(code);
                                                switch (bit) {
                                                    case NEW_SUBFILE_TYPE_REDUCED_RESOLUTION: items.add("Reduced resolution"); break;
                                                    case NEW_SUBFILE_TYPE_SINGLE_PAGE:        items.add("Single page"); break;
                                                    case NEW_SUBFILE_TYPE_TRANSPARENCY:       items.add("Transparency"); break;
                                                }
                                                code &= ~bit;
                                            }
                                            return String.join(", ", items);
                                        });
                                    }
                                }
                            }
                        }
                        if (visible) {
                            final String name = Tags.name(tag);
                            final TreeTable.Node node;
                            if (children != null) {
                                node = new XMLMetadata.Root(children, (DefaultTreeTable.Node) image, name);
                            } else {
                                node = image.newChild();
                                node.setValue(NAME,  name);
                                node.setValue(VALUE, value);
                            }
                            node.setValue(CODE, Short.toUnsignedInt(tag));
                            if (tag == (short) TAG_GEO_KEY_DIRECTORY) {
                                geoNode = node;
                            }
                        }
                    }
                    input.seek(next);
                }
                imageNumber++;
            }
        } catch (ArithmeticException e) {
            throw new IOException(e);           // Cannot seek that far.
        } finally {
            input.reset();
        }
        writeGeoKeys();
        return table;
    }

    /**
     * Reads the {@code short}, {@code int} or {@code long} value (depending if the
     * file is standard or big TIFF) at the current {@linkplain Reader#input} position.
     */
    private long readInt(final boolean isShort) throws IOException {
        if (isClassic) {
            return isShort ? input.readUnsignedShort() : input.readUnsignedInt();
        } else {
            final long entry = input.readLong();
            if (entry < 0) {
                throw new ArithmeticException();
            }
            return entry;
        }
    }

    /**
     * Completes an integer code with a string representation of the corresponding enumeration name.
     */
    private static Object toName(final Object value, final IntFunction<Enum<?>> valueOf) {
        return toString(value, (code) -> CharSequences.upperCaseToSentence(valueOf.apply(code).name()));
    }

    /**
     * Completes an integer code with a string representation provided by the given function.
     */
    private static Object toString(final Object value, final IntFunction<CharSequence> valueOf) {
        if (value != null && NumberType.isInteger(value.getClass())) {
            final int code = ((Number) value).intValue();
            return valueOf.apply(code) + " (#" + code + ')';
        }
        return value;
    }

    /**
     * Write child values in {@link #geoNode} if non-null.
     */
    private void writeGeoKeys() {
        if (geoNode != null) {
            final var geoKeys = new LinkedHashMap<Short ,Object>(32);
            load(geoKeys);
            for (final Map.Entry<Short, Object> entry : geoKeys.entrySet()) {
                final TreeTable.Node node = geoNode.newChild();
                final short code = entry.getKey();
                node.setValue(CODE,  Short.toUnsignedInt(code));
                node.setValue(NAME,  GeoKeys.name(code));
                node.setValue(VALUE, entry.getValue());
            }
            geoNode = null;
        }
    }
}
