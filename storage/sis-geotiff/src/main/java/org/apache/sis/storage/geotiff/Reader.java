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

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.io.IOException;
import java.nio.ByteOrder;
import org.opengis.util.NameFactory;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.resources.Errors;


/**
 * An image reader for GeoTIFF files. This reader duplicates the implementations performed by other libraries, but we
 * nevertheless provide our own reader in Apache SIS for better control on the process of decoding geospatial metadata.
 * We also measured better performance with this reader at least for uncompressed files, and added support for some
 * unusual data layout not supported by other libraries.
 *
 * <p>This image reader can also process <cite>Big TIFF</cite> images.</p>
 *
 * <p>The TIFF format specification version 6.0 (June 3, 1992) is available
 * <a href="https://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf">here</a>.</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   0.8
 * @module
 */
final class Reader extends GeoTIFF {
    /**
     * The stream from which to read the data.
     */
    final ChannelDataInput input;

    /**
     * Stream position of the first byte of the GeoTIFF file. This is usually zero.
     */
    final long origin;

    /**
     * A multiplication factor for the size of pointers, expressed as a power of 2.
     * The pointer size in bytes is given by {@code Integer.BYTES << pointerExpansion}.
     *
     * <p>Values can be:</p>
     * <ul>
     *   <li>0 for the classical TIFF format, which uses 4 bytes.</li>
     *   <li>1 for the BigTIFF format, which uses 8 bytes.</li>
     *   <li>2 for 16 bytes (not yet used, but the BigTIFF specification makes provision for it).
     * </ul>
     *
     * Those values are defined that way for making easier (like a boolean flag) to test if
     * the file is a BigTIFF format, with statement like {@code if (intSizeExpansion != 0)}.
     */
    final byte intSizeExpansion;

    /**
     * The last <cite>Image File Directory</cite> (IFD) read, or {@code null} if none.
     * This is used when we detected the end of a pyramid and the beginning of next one.
     */
    private ImageFileDirectory lastIFD;

    /**
     * Offset (relative to the beginning of the TIFF file) of the next Image File Directory (IFD)
     * to read, or 0 if we have finished to read all of them.
     *
     * @see #readNextImageOffset()
     */
    private long nextIFD;

    /**
     * Offsets of all <cite>Image File Directory</cite> (IFD) that have been read so far.
     * This field is used only as a protection against infinite recursivity, by preventing
     * the same offset to appear twice.
     *
     * @see #readNextImageOffset()
     */
    private final Set<Long> doneIFD;

    /**
     * Information about each (potentially pyramided) image in this file.
     * Those objects are created when first needed.
     *
     * @see #getImage(int)
     */
    private final List<GridCoverageResource> images = new ArrayList<>();

    /**
     * Entries having a value that can not be read immediately, but instead have a pointer
     * to a value stored elsewhere in the file. Those values will be read only when needed.
     *
     * <div class="note"><b>Implementation note:</b>
     * we use a {@code LinkedList} because we will perform frequent additions and removals,
     * but no random access.</div>
     */
    private final LinkedList<DeferredEntry> deferredEntries = new LinkedList<>();

    /**
     * Whether {@link #deferredEntries} needs to be stored. This flag is set to {@code true} when
     * at least one new deferred entry has been added, and cleared after the sort has been done.
     */
    private boolean deferredNeedsSort;

    /**
     * The factory to use for creating image identifiers.
     */
    final NameFactory nameFactory;

    /**
     * Creates a new GeoTIFF reader which will read data from the given input.
     * The input must be at the beginning of the GeoTIFF file.
     *
     * @throws IOException if an error occurred while reading first bytes from the stream.
     * @throws DataStoreException if the file is not encoded in the TIFF or BigTIFF format.
     */
    Reader(final GeoTiffStore store, final ChannelDataInput input) throws IOException, DataStoreException {
        super(store);
        this.input       = input;
        this.origin      = input.getStreamPosition();
        this.doneIFD     = new HashSet<>();
        this.nameFactory = DefaultFactories.forBuildin(NameFactory.class);
        /*
         * A TIFF file begins with either "II" (0x4949) or "MM" (0x4D4D) characters.
         * Those characters identify the byte order. Note that we do not need to care
         * about the byte order for this flag since the two bytes shall have the same value.
         */
        final short order = input.readShort();
        final boolean isBigEndian = (order == BIG_ENDIAN);
        if (isBigEndian || order == LITTLE_ENDIAN) {
            input.buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            /*
             * Magic number of TIFF file is 42, followed by nothing else.
             * The pointer type is implicitely the Java Integer type (4 bytes).
             *
             * Magic number of BigTIFF file is 43, followed by the pointer size.
             * Currently, that pointer type can only be the Java Long type (8 bytes),
             * but a future BigTIFF version may allow 16 bytes wide pointers.
             */
            switch (input.readShort()) {
                case CLASSIC: {                                     // Magic number of classical format.
                    intSizeExpansion = 0;
                    readNextImageOffset();
                    return;
                }
                case BIG_TIFF: {                                    // Magic number of BigTIFF format.
                    final int numBits  = input.readUnsignedShort();
                    final int powerOf2 = Integer.numberOfTrailingZeros(numBits);    // In the [0 … 32] range.
                    if (numBits == (1L << powerOf2) && input.readShort() == 0) {
                        intSizeExpansion = (byte) (powerOf2 - 2);
                        if (intSizeExpansion == 1) {
                            /*
                             * Above `intSizeExpension` calculation was a little bit useless since we accept only
                             * one result in the end, but we did that generic computation anyway for keeping the
                             * code almost ready if the BigTIFF specification adds support for 16 bytes pointer.
                             */
                            readNextImageOffset();
                            return;
                        }
                    }
                }
            }
        }
        // Do not invoke this.errors() yet because GeoTiffStore construction may not be finished. Owner.error() is okay.
        throw new DataStoreContentException(store.errors().getString(Errors.Keys.UnexpectedFileFormat_2, "TIFF", input.filename));
    }

    /**
     * Sets {@link #nextIFD} to the next offset read from the TIFF file
     * and makes sure that it will not cause an infinite loop.
     */
    private void readNextImageOffset() throws IOException, DataStoreException {
        nextIFD = readUnsignedInt();
        if (!doneIFD.add(nextIFD)) {
            throw new DataStoreContentException(resources().getString(
                    Resources.Keys.CircularImageReference_1, input.filename));
        }
    }

    /**
     * Reads the {@code int} or {@code long} value (depending if the file is
     * a standard of big TIFF) at the current {@linkplain #input} position.
     *
     * @return the next pointer value.
     */
    private long readUnsignedInt() throws IOException, DataStoreException {
        if (intSizeExpansion == 0) {
            return input.readUnsignedInt();         // Classical format.
        }
        final long pointer = input.readLong();      // BigTIFF format.
        if (pointer >= 0) {
            return pointer;
        }
        throw new DataStoreContentException(store.getLocale(), "TIFF", input.filename, null);
    }

    /**
     * Reads the {@code short} or {@code long} value (depending if the file is
     * standard of big TIFF) at the current {@linkplain #input} position.
     *
     * @return the next directory entry value.
     */
    private long readUnsignedShort() throws IOException, DataStoreException {
        if (intSizeExpansion == 0) {
            return input.readUnsignedShort();       // Classical format.
        }
        final long entry = input.readLong();        // BigTIFF format.
        if (entry >= 0) {
            return entry;
        }
        throw new DataStoreContentException(store.getLocale(), "TIFF", input.filename, null);
    }

    /**
     * Returns the next <cite>Image File Directory</cite> (IFD), or {@code null} if we reached the last IFD.
     * The IFD consists of a 2 (classical) or 8 (BigTiff)-bytes count of the number of directory entries,
     * followed by a sequence of 12-byte field entries, followed by a pointer to the next IFD (or 0 if none).
     *
     * @param  index  index of the (potentially pyramided) image, not counting reduced-resolution (overview) images.
     * @return the IFD if we found it, or {@code null} if there are no more IFDs.
     * @throws ArithmeticException if the pointer to a next IFD is too far.
     *
     * @see #getImage(int)
     */
    private ImageFileDirectory getImageFileDirectory(final int index) throws IOException, DataStoreException {
        if (nextIFD == 0) {
            return null;
        }
        resolveDeferredEntries(null, nextIFD);
        input.seek(Math.addExact(origin, nextIFD));
        nextIFD = 0;               // Prevent trying other IFD if we fail to read this one.
        /*
         * Design note: we parse the Image File Directory entry now because even if we were
         * not interrested in that IFD, we need to go anyway after its last record in order
         * to get the pointer to the next IFD.
         */
        final int offsetSize = Integer.BYTES << intSizeExpansion;
        final ImageFileDirectory dir = new ImageFileDirectory(this, index);
        for (long remaining = readUnsignedShort(); --remaining >= 0;) {
            /*
             * Each entry in the Image File Directory has the following format:
             *   - The tag that identifies the field (see constants in the Tags class).
             *   - The field type (see constants inherited from the GeoTIFF class).
             *   - The number of values of the indicated type.
             *   - The value, or the file offset to the value elswhere in the file.
             */
            final short tag  = (short) input.readUnsignedShort();
            final Type type  = Type.valueOf(input.readShort());        // May be null.
            final long count = readUnsignedInt();
            final long size  = (type != null) ? Math.multiplyExact(type.size, count) : 0;
            if (size <= offsetSize) {
                /*
                 * If the value can fit inside the number of bytes given by `offsetSize`, then the value is
                 * stored directly at that location. This is the most common way TIFF tag values are stored.
                 */
                final long position = input.getStreamPosition();
                if (size != 0) {
                    Object error;
                    try {
                        /*
                         * A size of zero means that we have an unknown type, in which case the TIFF specification
                         * recommends to ignore it (for allowing them to add new types in the future), or an entry
                         * without value (count = 0) - in principle illegal but we make this reader tolerant.
                         */
                        error = dir.addEntry(tag, type, count);
                    } catch (IOException | DataStoreException e) {
                        throw e;
                    } catch (Exception e) {
                        error = e;
                    }
                    if (error != null) {
                        warning(tag, error);
                    }
                }
                input.seek(position + offsetSize);      // Usually just move the buffer position by a few bytes.
            } else {
                // Offset from beginning of TIFF file where the values are stored.
                deferredEntries.add(new DeferredEntry(dir, tag, type, count, readUnsignedInt()));
                dir.hasDeferredEntries = true;
                deferredNeedsSort = true;
            }
        }
        /*
         * At this point we got the requested IFD. But maybe some deferred entries need to be read.
         * The values of those entries may be anywhere in the TIFF file, in any order. Given that
         * seek operations in the input stream may be costly or even not possible, we try to read
         * all values in sequential order, including values of other IFD if there is some before
         * our IFD of interest. This is the purpose of `resolveDeferredEntries(…)`.
         */
        readNextImageOffset();                          // Zero if the IFD that we just read was the last one.
        return dir;
    }

    /**
     * Reads all entries that were deferred.
     *
     * @param dir  the IFD for which to resolve deferred entries regardless stream position or {@code ignoreAfter} value.
     */
    final void resolveDeferredEntries(final ImageFileDirectory dir) throws IOException, DataStoreException {
        if (dir.hasDeferredEntries) {
            resolveDeferredEntries(dir, Long.MAX_VALUE);
            dir.hasDeferredEntries = false;
        }
        dir.validateMandatoryTags();
    }

    /**
     * Reads some of the entries that has been deferred. If the given {@code dir} argument is non-null,
     * then this method resolves all entries needed by this IFD no matter where the entry value is located.
     * For other entries, this method may opportunistically resolve some values but make no guarantees.
     * Generally, values of IFD other than {@code this} will not be resolved if they are located before
     * the current stream position or after the {@code ignoreAfter} value.
     *
     * @param dir  the IFD for which to resolve deferred entries regardless stream position or {@code ignoreAfter} value.
     * @param ignoreAfter  offset relative to the beginning of TIFF file at which entries should be ignored.
     *        This hint does not apply to the IFD specified by the {@code dir} argument.
     */
    private void resolveDeferredEntries(final ImageFileDirectory dir, final long ignoreAfter)
            throws IOException, DataStoreException
    {
        if (deferredNeedsSort) {
            deferredEntries.sort(null);                                 // Sequential order in input stream.
            deferredNeedsSort = false;
        }
        final long ignoreBefore = input.getStreamPosition() - origin;   // Avoid seeking back, unless we need to.
        DeferredEntry stopAfter = null;                                 // Avoid reading more entries than needed.
        if (dir != null) {
            for (final Iterator<DeferredEntry> it = deferredEntries.descendingIterator(); it.hasNext();) {
                stopAfter = it.next();
                if (stopAfter.owner == dir) break;
            }
        }
        for (final Iterator<DeferredEntry> it = deferredEntries.iterator(); it.hasNext();) {
            final DeferredEntry entry = it.next();
            if (entry.owner == dir || (entry.offset >= ignoreBefore && entry.offset <= ignoreAfter)) {
                input.seek(Math.addExact(origin, entry.offset));
                Object error;
                try {
                    error = entry.owner.addEntry(entry.tag, entry.type, entry.count);
                } catch (IOException | DataStoreException e) {
                    throw e;
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    warning(entry.tag, error);
                }
                it.remove();            // Remove only on success, but before we try to read other entries.
            }
            if (entry == stopAfter) break;
        }
    }

    /**
     * Returns the potentially pyramided <cite>Image File Directories</cite> (IFDs) at the given index.
     * If the pyramid has already been initialized, then it is returned.
     * Otherwise this method initializes the pyramid now and returns it.
     *
     * <p>This method assumes that the first IFD is the full resolution image and all following IFDs having
     * {@link ImageFileDirectory#isReducedResolution()} flag set are the same image at lower resolutions.
     * This is the <cite>cloud optimized GeoTIFF</cite> convention.</p>
     *
     * @return the pyramid if we found it, or {@code null} if there is no more pyramid at the given index.
     * @throws ArithmeticException if the pointer to a next IFD is too far.
     */
    final GridCoverageResource getImage(final int index) throws IOException, DataStoreException {
        while (index >= images.size()) {
            int imageIndex = images.size();
            ImageFileDirectory fullResolution = lastIFD;
            if (fullResolution == null) {
                fullResolution = getImageFileDirectory(imageIndex);
                if (fullResolution == null) {
                    return null;
                }
            }
            lastIFD = null;     // Clear now in case of error.
            imageIndex++;       // In case next image is full-resolution.
            ImageFileDirectory image;
            final List<ImageFileDirectory> overviews = new ArrayList<>();
            while ((image = getImageFileDirectory(imageIndex)) != null) {
                if (image.isReducedResolution()) {
                    overviews.add(image);
                } else {
                    lastIFD = image;
                    break;
                }
            }
            /*
             * All pyramid levels have been read. If there is only one level,
             * use the image directly. Otherwise create the pyramid.
             */
            if (overviews.isEmpty()) {
                images.add(fullResolution);
            } else {
                overviews.add(0, fullResolution);
                images.add(new MultiResolutionImage(overviews));
            }
        }
        final GridCoverageResource image = images.get(index);
        if (image instanceof ImageFileDirectory) {
            resolveDeferredEntries((ImageFileDirectory) image);
        }
        return image;
    }

    /**
     * Logs a warning about a tag that can not be read, but does not interrupt the TIFF reading.
     *
     * @param tag    the tag than can not be read.
     * @param error  the value than can not be understand, or the exception that we got while trying to parse it.
     */
    private void warning(final short tag, final Object error) {
        final short key;
        final Object[] args;
        final Exception exception;
        if (error instanceof Exception) {
            key = Errors.Keys.CanNotSetPropertyValue_1;
            args = new Object[1];
            exception = (Exception) error;
        } else {
            key = Errors.Keys.UnknownEnumValue_2;
            args = new Object[2];
            args[1] = error;
            exception = null;
        }
        args[0] = Tags.name(tag);
        store.listeners().warning(errors().getString(key, args), exception);
    }

    /**
     * Closes this reader.
     *
     * @throws IOException if an error occurred while closing this reader.
     */
    @Override
    public void close() throws IOException {
        input.channel.close();
    }
}
