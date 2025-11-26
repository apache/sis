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
package org.apache.sis.storage.geotiff.base;

import java.lang.reflect.Field;
import java.util.function.Supplier;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;


/**
 * Numerical values of GeoTIFF tags, as <strong>unsigned</strong> short integers.
 * This class provides only the tags that are not already provided by {@link TIFFTagSet}.
 *
 * <p>In this class, field names are identical to TIFF tag names.
 * For that reason, some field names do not follow usual Java convention for constants.</p>
 *
 * <p>A useful (but unofficial) reference is the
 * <a href="http://www.awaresystems.be/imaging/tiff/tifftags.html">TIFF Tag Reference</a> page.</p>
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class Tags {
    /**
     * XML packet containing metadata such as descriptions, titles, keywords, author and copyright information.
     *
     * @see <a href="https://www.adobe.com/products/xmp.html">Adobe XMP technote 9-14-02</a>
     * @see <a href="https://www.iso.org/standard/75163.html">ISO 16684-1:2019 Graphic technology — Extensible metadata platform (XMP)</a>
     */
    public static final short XML_Packet = 0x02BC;

    /**
     * Collection of Photoshop "Image Resource Blocks".
     *
     * @see <a href="https://www.awaresystems.be/imaging/tiff/tifftags/docs/photoshopthumbnail.html">PhotoShop private TIFF Tag</a>
     */
    public static final short PhotoshopImageResources = (short) 0x8649;

    /**
     * Embedded XML-encoded instance documents prepared using 19139-based schema.
     * This is an <abbr>OGC</abbr> <abbr>DGIWG</abbr> extension tag.
     */
    public static final short GEO_METADATA = (short) 0xC6DD;

    /**
     * Holds an XML list of name=value 'metadata' values about the image as a whole, and about specific samples.
     *
     * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags/gdal_metadata.html">TIFF Tag GDAL_METADATA</a>
     */
    public static final short GDAL_METADATA = (short) 0xA480;             // 42112

    /**
     * Contains an ASCII encoded nodata or background pixel value.
     *
     * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags/gdal_nodata.html">TIFF Tag GDAL_NODATA</a>
     */
    public static final short GDAL_NODATA = (short) 0xA481;               // 42113

    /**
     * Supplier of TIFF tag sets, in preference order.
     * The sets that are most likely to be used (for the kind of data handled by SIS) should be first.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Supplier<TIFFTagSet>[] TAG_SETS = new Supplier[] {
        javax.imageio.plugins.tiff.BaselineTIFFTagSet::getInstance,
        javax.imageio.plugins.tiff.GeoTIFFTagSet::getInstance,
        javax.imageio.plugins.tiff.ExifGPSTagSet::getInstance,
        javax.imageio.plugins.tiff.ExifParentTIFFTagSet::getInstance,
        javax.imageio.plugins.tiff.ExifTIFFTagSet::getInstance,
        javax.imageio.plugins.tiff.ExifInteroperabilityTagSet::getInstance,
        javax.imageio.plugins.tiff.FaxTIFFTagSet::getInstance
    };

    /**
     * Do not allow instantiation of this class.
     */
    private Tags() {
    }

    /**
     * Returns the name of the given tag.
     * This method should be rarely invoked (mostly for formatting error messages).
     */
    public static String name(final short tag) {
        final int ti = Short.toUnsignedInt(tag);
        for (final Supplier<TIFFTagSet> s : TAG_SETS) {
            final TIFFTag t = s.get().getTag(ti);
            if (t != null) return t.getName();
        }
        try {
            for (final Field field : Tags.class.getFields()) {
                if (field.getType() == Short.TYPE) {
                    if (field.getShort(null) == tag) {
                        return field.getName();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);        // Should never happen because we asked only for public fields.
        }
        return "Tag #" + Integer.toHexString(ti);
    }
}
