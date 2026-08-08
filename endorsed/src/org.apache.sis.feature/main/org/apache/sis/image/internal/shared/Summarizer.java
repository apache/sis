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
package org.apache.sis.image.internal.shared;

import java.util.Locale;
import java.text.NumberFormat;
import java.awt.Image;
import java.awt.image.RenderedImage;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Localized;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.feature.internal.Resources;


/**
 * Helper class for formatting a description of an image.
 * Used mostly for implementation of {@code toString()} methods for debugging purposes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Summarizer implements Localized {
    /**
     * Root of the tree where to add a branch.
     */
    private final TreeTable.Node root;

    /**
     * Localized resources for vocabulary.
     */
    private final Vocabulary vocabulary;

    /**
     * The single column where to write texts.
     */
    private final TableColumn<CharSequence> column;

    /**
     * Creates a new formatter for appending the image description in an existing tree.
     *
     * @param  root        root of the tree where to add a branch.
     * @param  vocabulary  localized resources for vocabulary.
     * @param  column      the single column where to write texts.
     */
    public Summarizer(final TreeTable.Node root, final Vocabulary vocabulary, final TableColumn<CharSequence> column) {
        this.root       = root;
        this.vocabulary = vocabulary;
        this.column     = column;
    }

    /**
     * Returns the locale used by this formatter.
     *
     * @return the locale.
     */
    @Override
    public final Locale getLocale() {
        return vocabulary.getLocale();
    }

    /**
     * A row to format.
     */
    private static final class Row {
        /** A {@link Vocabulary.Keys} constant, or 0 if none. */
        private final short key;

        /** Label created from the vocabulary key. */
        private String label;

        /** Description to write after the label. */
        private final String desc;

        /** Optional note as a child of the description. */
        String note;

        /** Prepares a new node. */
        Row(final short key, final CharSequence desc) {
            this.key  = key;
            this.desc = desc.toString();
        }

        /** Resolves {@code #label} and returns its length. */
        final int formatLabel(final Vocabulary vocabulary) {
            if (key == 0) return 0;
            label = vocabulary.getLabel(key);
            return label.length();
        }

        /** Returns a string representation using the given buffer as a temporary buffer. */
        final String toString(final StringBuilder buffer, final int margin) {
            if (label != null) {
                buffer.append(label).append(CharSequences.spaces(margin - label.length()));
            }
            return buffer.append(desc).toString();
        }
    }

    /**
     * Appends a "data layout" branch to the tree representation of an image description.
     * Contains information about image location, image size, tile size, sample model, color model.
     *
     * @param  data  the image to describe.
     */
    public void layout(final RenderedImage data) {
        final int width     = data.getWidth();
        final int height    = data.getHeight();
        final var model     = data.getSampleModel();
        final var nf        = NumberFormat.getIntegerInstance(getLocale());
        final var resources = Resources.forLocale(getLocale());
        final var buffer    = new StringBuilder();
        final var rows      = new Row[8];
        int count = 0;
        for (int c = 0; c < rows.length; c++) {
            final Row row;
            switch (c) {
                /*
                 * Image name defined by a property (optional).
                 */
                case 0: {
                    final Object name = data.getProperty(PlanarImage.SOURCE_NAME_KEY);
                    if (name == null || name == Image.UndefinedProperty) continue;
                    row = new Row(Vocabulary.Keys.Name,
                                  vocabulary.getString(Vocabulary.Keys.Quoted_1, name));
                    break;
                }
                /*
                 * Image location, omitted in the common case of image starting at (0, 0).
                 */
                case 1: {
                    final int x = data.getMinX();
                    final int y = data.getMinY();
                    if ((x | y) == 0) continue;
                    row = new Row(Vocabulary.Keys.Origin,
                                  buffer.append(nf.format(x)).append(", ")
                                        .append(nf.format(y)));
                    break;
                }
                /*
                 * Image size and number of bands, always shown.
                 * The sample model should never be null, but we check anyway for safety.
                 */
                case 2: {
                    appendCount(buffer, nf, width, height).append(" pixels");
                    if (model != null) {
                        buffer.append(" × ")
                              .append(resources.getString(Resources.Keys.BandCount_1, model.getNumBands()));
                    }
                    row = new Row(Vocabulary.Keys.ImageSize, buffer);
                    break;
                }
                /*
                 * Image tiling, omitted if the properties are consistent with an untiled image.
                 */
                case 3: {
                    final int nx = data.getNumXTiles();
                    final int ny = data.getNumYTiles();
                    final int tx = data.getTileWidth();
                    final int ty = data.getTileHeight();
                    if (nx == 1 && ny == 1 && tx == width && ty == height) continue;
                    final String n = appendCount(buffer, nf, nx, ny).toString(); buffer.setLength(0);
                    final String s = appendCount(buffer, nf, tx, ty).toString();
                    row = new Row(Vocabulary.Keys.Tiling, resources.getString(Resources.Keys.TileCountAndSize_2, n, s));
                    break;
                }
                /*
                 * Number of bits for each band and the type of number where they are stored.
                 */
                case 4: {
                    if (model == null) continue;
                    int[] size = model.getSampleSize();
                    buffer.append('{');
                    for (int i=0; i < size.length; i++) {
                        if (i != 0) buffer.append(", ");
                        buffer.append(nf.format(size[i]));
                    }
                    buffer.append("} bits");
                    String type = ImageUtilities.getDataTypeName(model);
                    if (type != null) {
                        buffer.append(vocabulary.getString(Vocabulary.Keys.InBetweenWords))
                              .append('‘').append(type).append('’');
                    }
                    row = new Row(Vocabulary.Keys.DataType, buffer);
                    break;
                }
                /*
                 * Sample model (mandatory). If the sample model is nevertheless missing,
                 * some placeholder will be shown for suggesting that there is a problem.
                 */
                case 5: {
                    row = new Row(Vocabulary.Keys.Layout,
                                  CharSequences.camelCaseToSentence(Classes.getShortClassName(model)));
                    break;
                }
                /*
                 * Color model (optional) with a note about transparency,
                 * which is an important information for images overlay.
                 */
                case 6: {
                    final var cm = data.getColorModel();
                    CharSequence desc = ColorModelFactory.formatDescription(cm, resources, buffer);
                    String note = ColorModelFactory.describeTransparency(cm, resources);
                    if (desc == null) {
                        if (note == null) continue;
                        desc = note;
                        note = null;
                    }
                    row = new Row(Vocabulary.Keys.Colors, desc);
                    row.note = note;
                    break;
                }
                /*
                 * Warning about potential inconsistency.
                 */
                case 7: {
                    if (!(data instanceof PlanarImage)) continue;
                    String warning = ((PlanarImage) data).verify();
                    if (warning == null) continue;
                    row = new Row((short) 0,
                            Messages.forLocale(getLocale()).getString(Messages.Keys.PossibleInconsistency_1, warning));
                    break;
                }
                /*
                 * End of rows.
                 */
                default: continue;
            }
            rows[count++] = row;
            buffer.setLength(0);
        }
        int margin = 0;
        for (int i=0; i<count; i++) {
            margin = Math.max(margin, rows[i].formatLabel(vocabulary));
        }
        margin++;
        for (int i=0; i<count; i++) {
            final Row row = rows[i];
            final TreeTable.Node child = root.newChild();
            child.setValue(column, row.toString(buffer, margin));
            buffer.setLength(0);
            if (row.note != null) {
                child.newChild().setValue(column, row.note);
            }
        }
    }

    /**
     * Appends an image size, tile size or number of tiles.
     */
    private static StringBuilder appendCount(StringBuilder buffer, NumberFormat nf, int nx, int ny) {
        return buffer.append(  '(').append(nf.format(nx))
                     .append(" × ").append(nf.format(ny)).append(')');
    }

    /**
     * Returns a summary of the given image as a tree.
     *
     * @param  image  the image to summarize.
     * @return a tree presentation of the properties of the image.
     */
    public static String toString(final RenderedImage image) {
        final TableColumn<CharSequence> column = TableColumn.VALUE_AS_TEXT;
        final var tree = new DefaultTreeTable(column);
        final var c = new Summarizer(tree.getRoot(), Vocabulary.forLocale(null), column);
        c.root.setValue(column, Classes.getShortClassName(image));
        c.layout(image);
        return tree.toString();
    }
}
