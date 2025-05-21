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
package org.apache.sis.storage.isobmff;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.OptionalLong;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.isobmff.base.MovieHeader;
import org.apache.sis.storage.isobmff.mpeg.ComponentDefinition;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * Reader of boxes in a GeoHEIF file, together with contextual information.
 * The context contains sibling or parent boxes that may be needed for resolving a new box.
 * For example, some boxes contain an array, with subsequent boxes containing indexes to elements in that array.
 * This {@code Reader} class contains the boxes that are used as arrays, so that subsequent boxes can read them.
 *
 * <h2>Stack of contexts</h2>
 * A context is valid inside a box and all children of that box.
 * When the reader moves to the parent box, it resets the context of that parent box.
 * For making that possible, the {@code Reader} is cloned (together with its context)
 * every times that a container box is about to parse its children.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class Reader implements Cloneable {
    /**
     * The stream from which to read the data.
     *
     * @todo Should be an implementation that blocks reading after {@link #endOfCurrentBox}.
     *       This is not easy to implement. Maybe a {@code ReadableByteChannel} adjusting
     *       the {@code ByteBuffer} limit after read operations, keeping in mind that there
     *       is valid bytes between the old and the new limits.
     */
    public final ChannelDataInput input;

    /**
     * Position after the last byte of the current box, or -1 if unknown.
     * This is also the position where the next box begins.
     *
     * @see #endOfCurrentBox()
     */
    private long endOfCurrentBox;

    /**
     * The last {@code MovieHeader} encountered.
     * Stored because {@code TrackHeader} needs this information.
     *
     * This information is contained in {@code Movie} box.
     * {@code MovieHeader} is a sibling of {@code Track}, which is the parent of {@code TrackHeader}.
     */
    MovieHeader movieHeader;

    /**
     * The last {@code ComponentDefinition} encountered.
     * Stored because {@code UncompressedFrameConfig} needs this information.
     */
    ComponentDefinition componentDefinition;

    /**
     * A map of objects that may be duplicated in different boxes.
     * This map can be used for saving a little bit of memory.
     * All keys and values should be treated as unmodifiable.
     *
     * <p>The {@linkplain #clone cloned()} instances share the same map
     * in order to make the sharing more effective.</p>
     *
     * @see #unique(Object)
     */
    public final Map<Object,Object> sharedObjects;

    /**
     * Arbitrary objects identifying warnings that have already been reported.
     * The keys are usually {@link Box#type()} value. However the keys can be any compound objects
     * if the caller wants finer grain separation. Since this map is used for avoiding repetitions,
     * not for providing data, the exact key and value types do not matter.
     * The map values are optional causes for finer grain separation.
     *
     * @see #isNewWarning(Object, Object)
     */
    private final Map<Object, Set<Object>> alreadyReported;

    /**
     * The listeners when to send warnings. Warnings are sent as soon as they occur,
     * after omitting repetitions, in order to have the warnings in a meaningful order.
     */
    private final StoreListeners listeners;

    /**
     * Creates a new GeoHEIF reader which will read data from the given input.
     * The input must be at the beginning of the GeoHEIF file.
     *
     * @param  input      the stream from which to read the data.
     * @param  listeners  the listeners where to send warnings.
     * @throws IOException if an error occurred with the given input.
     */
    public Reader(final ChannelDataInput input, final StoreListeners listeners) throws IOException {
        input.buffer.order(ByteOrder.BIG_ENDIAN);
        this.input      = input;
        this.listeners  = listeners;
        endOfCurrentBox = input.length();
        sharedObjects   = new HashMap<>();
        alreadyReported = new HashMap<>();
    }

    /**
     * Reads one box instantiated with the given registry.
     * This method reads the box header (size and type) from the stream,
     * then instantiates a box of the Java class identified by the type.
     * A custom registry is specified for filtering the type of boxes to accept.
     *
     * @param  registry  the registry to use for box instantiations.
     * @return the next box, or {@code null} if the box type is not recognized.
     * @throws IOException if an error occurred while reading the box content.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     * @throws ArithmeticException if an integer overflow occurred for another reason.
     * @throws ArrayStoreException if a child box is not of the type expected by its container box.
     * @throws ArrayIndexOutOfBoundsException if a box does not have the expected number of elements.
     * @throws UnsupportedVersionException if the box declare an unsupported version number.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the box creation failed for another reason.
     */
    public final Box readBox(final BoxRegistry registry) throws IOException, DataStoreException {
        return readBox(registry, input.getStreamPosition());
    }

    /**
     * Reads one box starting at the given index. The caller must ensure that {@code startOfCurrentBox}
     * is the current stream position, either by setting that value to {@code input.getStreamPosition()}
     * or by invoking {@code input.seek(startOfCurrentBox)} before this method call.
     * On return, {@link #endOfCurrentBox} must be the position of the next box.
     *
     * @param  registry           the registry to use for box instantiations.
     * @param  startOfCurrentBox  the current stream position, which must be the beginning of the box to read.
     * @return the next box, or {@code null} if the box type is not recognized.
     */
    private Box readBox(final BoxRegistry registry, final long startOfCurrentBox) throws IOException, DataStoreException {
        assert startOfCurrentBox == input.getStreamPosition();
        /*
         * Number of bytes of the next box, including all its field and contained boxes.
         * If the size is 1, then the actual size is in a `largeSize` field after the type.
         * If the size is 0, then the box is a top-level container extending to the end of file.
         */
        long end = input.readUnsignedInt();     // For now this is actually the box size. Will become the box end later.
        final int type = input.readInt();
        if (end == 0) {
            end = input.length();               // -1 if unknown size.
        } else {
            int min = 2*Integer.BYTES;
            if (end == 1) {
                end = input.readLong();
                min += Long.BYTES;
            }
            if (end < min) {
                throw new DataStoreContentException("Malformed HEIF file: invalid box size: " + end + " bytes");
            }
            end = Math.addExact(startOfCurrentBox, end);    // Now, it becomes the real box end
        }
        /*
         * If the box type is 'uuid', then the next 128 bits are the UUID and will be read
         * by `MainBoxRegistry`. The rest of the payload should be read at least partially
         * by the box constructor. Remaining bytes will be skipped.
         */
        endOfCurrentBox = end;
        Box box;
        try {
            box = registry.create(this, type);
            // Do not log a warning if null, because it should be done by the registry.
        } catch (DataStoreException cause) {
            box = null;
            if (isNewWarning(type, Classes.getClass(cause))) {
                var record = new LogRecord(Level.WARNING, "Cannot read the \"" + Box.formatFourCC(type) + "\" box.");
                record.setThrown(cause);
                listeners.warning(record);
            }
        }
        assert endOfCurrentBox == end;      // Ensure that the value has not been modified.
        assert (end < 0) || input.getStreamPosition() <= end : Box.formatFourCC(box.type());
        return box;
    }

    /**
     * Reads the remaining of the current box as children.
     * A custom registry is specified for filtering the type of boxes to accept.
     * Boxes of unknown types are ignored and skipped.
     *
     * @param  registry  the registry to use for box instantiations, or {@code null} for the main registry.
     * @param  indexed   whether index matter. If {@code true}, then the returned array may contain null elements.
     * @return children boxes. May contain null elements if {@code indexed} is true.
     * @throws IOException if an error occurred while reading the box.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     * @throws ArithmeticException if an integer overflow occurred for another reason.
     * @throws ArrayStoreException if a child box is not of the type expected by its container box.
     * @throws DataStoreContentException if the <abbr>HEIF</abbr> file is malformed.
     * @throws DataStoreException if the reading failed for another reason.
     */
    public final List<Box> readChildren(BoxRegistry registry, final boolean indexed) throws IOException, DataStoreException {
        if (registry == null) {
            registry = MainBoxRegistry.INSTANCE;
        }
        final var children = new ArrayList<Box>();
        final long end = endOfCurrentBox - 2*Integer.BYTES;     // With a margin of at least the space of a box header.
        final boolean toEnd = (end < 0);
        final var reader = clone();
        long startOfCurrentBox = input.getStreamPosition();
        while (toEnd ? input.hasRemaining() : startOfCurrentBox <= end) {
            input.seek(startOfCurrentBox);
            Box child = reader.readBox(registry, startOfCurrentBox);
            if (child != null || indexed) {
                children.add(child);
            }
            startOfCurrentBox = reader.endOfCurrentBox;
        }
        return children;
    }

    /**
     * Reads all remaining integers in the current box.
     *
     * @return remaining integers, or an empty array if none.
     * @throws IOException if an error occurred while reading the integers.
     */
    public final int[] readRemainingInts() throws IOException {
        if (endOfCurrentBox < 0) {
            throw new IOException("Stream of unknown length.");
        }
        int n = Math.toIntExact((endOfCurrentBox - input.getStreamPosition()) / Integer.BYTES);
        return (n != 0) ? input.readInts(n) : ArraysExt.EMPTY_INT;
    }

    /**
     * Reads null-terminated string encoded in UTF-8 or UTF-16.
     * If blank or empty, returns {@code null}.
     *
     * @param  allowUTF16  whether to allow UTF-16 encoding.
     * @return the null-terminated string, or {@code null} if blank.
     * @throws IOException if an error occurred while reading the string.
     */
    public final String readNullTerminatedString(final boolean allowUTF16) throws IOException {
        String value = input.readNullTerminatedString(allowUTF16 ? null : StandardCharsets.UTF_8);
        return value.isBlank() ? null : unique(value);
    }

    /**
     * Reads an optional <abbr>URI</abbr>. If the <abbr>URI</abbr> cannot be parsed,
     * logs a warning and returns {@code null}.
     *
     * @return the <abbr>URI</abbr>, or {@code null}.
     * @throws IOException if an error occurred while reading the string.
     */
    public final URI readURI() throws IOException {
        final String uri = readNullTerminatedString(false);
        if (uri != null) try {
            return unique(new URI(uri));
        } catch (URISyntaxException e) {
            cannotParse(e, uri, false);
        }
        return null;
    }

    /**
     * Returns a unique instance of the given object, if possible.
     *
     * @param  <T>      type of object.
     * @param  element  the object for which to get a shared instance, or {@code null}.
     * @return a shared instance of the given object, or {@code element} if none.
     */
    @SuppressWarnings("unchecked")
    public final <T> T unique(final T element) {
        if (element != null) {
            Object previous = sharedObjects.putIfAbsent(element, element);
            if (element.equals(previous)) {
                return (T) previous;
            }
        }
        return element;
    }

    /**
     * Returns the stream position after the last byte of the current box.
     *
     * @return stream position after the last byte of the current box.
     */
    public final OptionalLong endOfCurrentBox() {
        return (endOfCurrentBox >= 0) ? OptionalLong.of(endOfCurrentBox) : OptionalLong.empty();
    }

    /**
     * Returns whether warning identified by the given ({@code key}, {@code cause}) pair has not already been reported.
     *
     * @param  key    an arbitrary key such as the box type or an exception class.
     * @param  cause  optional value for finer-grain separation, or {@code null} if none.
     * @return whether the given ({@code key}, {@code cause}) pair is a new warning to report.
     */
    private boolean isNewWarning(final Object key, final Object cause) {
        return alreadyReported.computeIfAbsent(key, (k) -> new HashSet<>()).add(cause);
    }

    /**
     * Reports that a type of box is unknown to this implementation.
     *
     * @param type  the type of the unknown box. It may be a four-characters code or an <abbr>UUID</abbr>.
     */
    final void unknownBoxType(Object type) {
        if (isNewWarning(type, null)) {
            if (type instanceof Integer fourCC) {
                type = Box.formatFourCC(fourCC);
            }
            var record = new LogRecord(Level.WARNING, "The \"" + type + "\" type of box is unrecognized.");
            listeners.warning(record);
        }
    }

    /**
     * Prepares a warning message for a box of unexpected type found inside a container box.
     * "Unexpected" in this context does not mean "unknown". This method is for cases where
     * the type of children is constrained by the specification, but another type was found.
     *
     * @param container  the type of the container box.
     * @param child      the type of the unexpected child.
     */
    public final void unexpectedChildType(final int container, final int child) {
        if (isNewWarning(container, child)) {
            final var message = new StringBuilder("Container box “").append(Box.formatFourCC(container))
                    .append("” cannot contain children of type “").append(Box.formatFourCC(child)).append("\".");
            listeners.warning(message.toString());
        }
    }

    /**
     * Logs a warning for an object that cannot be parsed, if no similar warning has been logged before.
     *
     * @param error       the error that occurred.
     * @param value       the value that cannot be parsed.
     * @param ignoreable  {@code true} for {@link Level#WARNING}, {@code false} for {@link Level#FINE}.
     */
    public final void cannotParse(final Exception error, final String value, final boolean ignoreable) {
        if (isNewWarning(error.getClass(), value)) {
            final LogRecord record = Errors.forLocale(listeners.getLocale())
                    .getLogRecord(ignoreable ? Level.FINE : Level.WARNING, Errors.Keys.CanNotParse_1, value);
            record.setThrown(error);
            listeners.warning(record);
        }
    }

    /**
     * Returns a clone of this reader. Used for inheriting all the context
     * and making changes to it without impacting the parent context.
     *
     * @return a clone of this reader.
     */
    @Override
    protected final Reader clone() {
        try {
            return (Reader) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
