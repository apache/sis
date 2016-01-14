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
package org.apache.sis.internal.referencing.provider;

import java.util.AbstractMap;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.ReadableByteChannel;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.quantity.Angle;
import javax.measure.unit.NonSI;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.system.DataDirectory;

// Branch-dependent imports
import java.io.File;
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.Paths;
import org.apache.sis.internal.jdk7.Files;


/**
 * The provider for <cite>"North American Datum Conversion"</cite> (EPSG:9613).
 * This transform requires data that are not bundled by default with Apache SIS.
 *
 * <p>The files given in parameters are theoretically binary files. However this provider accepts also ASCII files.
 * Those two kinds of files are recognized automatically; there is no need to specify whether the files are ASCII
 * or binary.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see <a href="http://www.ngs.noaa.gov/cgi-bin/nadcon.prl">NADCON on-line computation</a>
 */
@XmlTransient
public final class NADCON extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4707304160205218546L;

    /**
     * The operation parameter descriptor for the <cite>"Latitude difference file"</cite> parameter value.
     * The default value is {@code "conus.las"}.
     */
    private static final ParameterDescriptor<File> LATITUDE;

    /**
     * The operation parameter descriptor for the <cite>"Longitude difference file"</cite> parameter value.
     * The default value is {@code "conus.los"}.
     */
    private static final ParameterDescriptor<File> LONGITUDE;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LATITUDE = builder
                .addIdentifier("8657")
                .addName("Latitude difference file")
                .setRemarks(NTv2.WARNING).create(File.class, Paths.get("conus.las"));
        LONGITUDE = builder
                .addIdentifier("8658")
                .addName("Longitude difference file")
                .setRemarks(NTv2.WARNING).create(File.class, Paths.get("conus.los"));
        PARAMETERS = builder
                .addIdentifier("9613")
                .addName("NADCON")
                .createGroup(LATITUDE, LONGITUDE);
    }

    /**
     * Creates a new provider.
     */
    public NADCON() {
        super(2, 2, PARAMETERS);
    }

    /**
     * Returns the base interface of the {@code CoordinateOperation} instances that use this method.
     *
     * @return Fixed to {@link Transformation}.
     */
    @Override
    public Class<Transformation> getOperationType() {
        return Transformation.class;
    }

    /**
     * Creates a transform from the specified group of parameter values.
     *
     * @param  factory The factory to use if this constructor needs to create other math transforms.
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws FactoryException if an error occurred while loading the grid.
     */
    @Override
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws ParameterNotFoundException, FactoryException
    {
        final Parameters pg  = Parameters.castOrWrap(values);
        return InterpolatedTransform.createGeodeticTransformation(factory,
                getOrLoad(Path.castOrCopy(pg.getMandatoryValue(LATITUDE)),
                          Path.castOrCopy(pg.getMandatoryValue(LONGITUDE))));
    }

    /**
     * Returns the grid of the given name. This method returns the cached instance if it still exists,
     * or load the grid otherwise.
     *
     * @param latitudeShifts   Name of the grid file for latitude shifts.
     * @param longitudeShifts  Name of the grid file for longitude shifts.
     */
    @SuppressWarnings("null")
    static DatumShiftGridFile<Angle,Angle> getOrLoad(final Path latitudeShifts, final Path longitudeShifts)
            throws FactoryException
    {
        final Path rlat = DataDirectory.DATUM_CHANGES.resolve(latitudeShifts).toAbsolutePath();
        final Path rlon = DataDirectory.DATUM_CHANGES.resolve(longitudeShifts).toAbsolutePath();
        final Object key = new AbstractMap.SimpleImmutableEntry<Path,Path>(rlat, rlon);
        DatumShiftGridFile<?,?> grid = DatumShiftGridFile.CACHE.peek(key);
        if (grid == null) {
            final Cache.Handler<DatumShiftGridFile<?,?>> handler = DatumShiftGridFile.CACHE.lock(key);
            try {
                grid = handler.peek();
                if (grid == null) {
                    final Loader loader;
                    Path file = latitudeShifts;
                    try {
                        // Note: buffer size must be divisible by the size of 'float' data type.
                        final ByteBuffer buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
                        final FloatBuffer fb = buffer.asFloatBuffer();
                        ReadableByteChannel in = Files.newByteChannel(rlat);
                        try {
                            DatumShiftGridLoader.log(NADCON.class, CharSequences.commonPrefix(
                                    latitudeShifts.toString(), longitudeShifts.toString()).toString() + '…');
                            loader = new Loader(in, buffer, file);
                            loader.readGrid(fb, null, longitudeShifts);
                        } finally {
                            in.close();
                        }
                        buffer.clear();
                        file = longitudeShifts;
                        in = Files.newByteChannel(rlon);
                        try {
                            new Loader(in, buffer, file).readGrid(fb, loader, null);
                        } finally {
                            in.close();
                        }
                    } catch (Exception e) {     // Multi-catch on the JDK7 branch.
                        throw DatumShiftGridLoader.canNotLoad("NADCON", file, e);
                    }
                    grid = DatumShiftGridCompressed.compress(loader.grid, null, loader.grid.accuracy);
                    grid = grid.useSharedData();
                }
            } finally {
                handler.putAndUnlock(grid);
            }
        }
        return grid.castTo(Angle.class, Angle.class);
    }




    /**
     * Loaders of NADCON data. Instances of this class exist only at loading time.
     * This class can read both binary and ASCII grid files.
     *
     * <div class="section">Binary format</div>
     * NADCON binary files ({@code "*.las"} and {@code "*.los"}) are organized into records
     * with the first record containing the header information, followed by the shift data.
     * The length of each record (including header) depends on the number of columns.
     *
     * <p>Records are ordered from South to North. Each record (except the header) is an entire row of grid points,
     * with values ordered from West to East. Each value is a {@code float} encoded in little endian byte order.
     * Each record ends with a 4 byte separator.</p>
     *
     * <p>Record data use a different convention than the record header. The header uses degrees of angle with
     * positive values East. But the offset values after the header are in seconds of angle with positive values
     * West. The {@code DatumShiftGrid} returned by this loader uses the header convention, which also matches
     * the order in which offset values appear in each row.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @author  Rueben Schulz (UBC)
     * @since   0.7
     * @version 0.7
     * @module
     */
    private static final class Loader extends DatumShiftGridLoader {
        /**
         * The length of the description in the header, in bytes.
         * <ul>
         *   <li>56 bytes of NADCON's identification: {@code "NADCON EXTRACTED REGION"}.</li>
         *   <li>8 bytes of "PGM" (unknown meaning).</li>
         * </ul>
         */
        private static final int DESCRIPTION_LENGTH = 64;

        /**
         * The first bytes that we expect to find the header (both binary and ASCII files).
         * We use that as the format signature.
         */
        private static final String NADCON = "NADCON";

        /**
         * The size of data in the binary file, in bytes.
         */
        private static final int DATA_SIZE = Float.SIZE / Byte.SIZE;

        /**
         * Longitude and latitude of of the first value in the first record, in degrees.
         */
        private final float x0, y0;

        /**
         * The difference between longitude (<var>Δx</var>) and latitude (<var>Δy</var>) cells, in degrees.
         */
        private final float Δx, Δy;

        /**
         * Number of longitude (<var>nx</var>) and latitude (<var>ny</var>) values.
         */
        private final int nx, ny, nz;

        /**
         * Temporary buffer for ASCII characters, or {@code null} if the file is a binary file.
         */
        private final StringBuilder ascii;

        /**
         * The grid created by {@link #readGrid(Loader)}.
         */
        DatumShiftGridFile.Float<Angle,Angle> grid;

        /**
         * Creates a new reader for the given channel. The file can be binary or ASCII.
         * This constructor parses the header immediately, but does not read any grid.
         *
         * @param  channel Where to read data from.
         * @param  buffer  The buffer to use. That buffer must use little endian byte order
         *                 and have a capacity divisible by the size of the {@code float} type.
         * @param  file    Path to the longitude or latitude difference file. Used only for error reporting.
         */
        Loader(final ReadableByteChannel channel, final ByteBuffer buffer, final Path file)
                throws IOException, FactoryException
        {
            super(channel, buffer, file);
            // After the description we need (3 int + 5 float) = 32 bytes
            // for a binary header, or 74 characters for an ASCII header.
            ensureBufferContains(DESCRIPTION_LENGTH + 80);
            for (int i=0; i<NADCON.length(); i++) {
                if (buffer.get() != NADCON.charAt(i)) {
                    throw unexpectedFormat();
                }
            }
            if (isASCII(buffer)) {
                ascii = new StringBuilder();
                nx = Integer.parseInt(nextWord());
                ny = Integer.parseInt(nextWord());
                nz = Integer.parseInt(nextWord());
                x0 = Float.parseFloat(nextWord());
                Δx = Float.parseFloat(nextWord());
                y0 = Float.parseFloat(nextWord());
                Δy = Float.parseFloat(nextWord());
                     Float.parseFloat(nextWord());
            } else {
                ascii = null;
                buffer.position(DESCRIPTION_LENGTH);
                nx = buffer.getInt();     // Number of data elements in each record.
                ny = buffer.getInt();     // Number of records in the file.
                nz = buffer.getInt();     // Not used.
                x0 = buffer.getFloat();   // Longitude of first record (westmost).
                Δx = buffer.getFloat();   // Cell size in degrees of longitude.
                y0 = buffer.getFloat();   // Latitude of first record (southmost).
                Δy = buffer.getFloat();   // Cell size in degrees of latitude.
                                          // One more float at this position, which we ignore.
            }
            if (nx < 8 || ny < 1 || nz < 1 || !(Δx > 0) || !(Δy > 0) || Float.isNaN(x0) || Float.isNaN(y0)) {
                throw unexpectedFormat();
            }
            if (ascii == null) {
                skip((nx + 1) * DATA_SIZE - buffer.position());
            }
        }

        /**
         * Returns {@code true} if all remaining characters in the buffer are US-ASCII characters for real numbers,
         * except the characters on the first line which may be any printable US-ASCII characters. If the content is
         * assumed ASCII, then the buffer position is set to the first EOL character.
         *
         * @return {@code true} if the file seems to be an ASCII one.
         */
        private static boolean isASCII(final ByteBuffer buffer) {
            int newLine = 0;
            while (buffer.hasRemaining()) {
                final char c = (char) buffer.get();
                if (c != ' ' && !(c >= '+' && c <= '9' && c != ',' && c != '/')) {  // (space) + - . [0-9]
                    if (c == '\r' || c == '\n') {
                        if (newLine == 0) {
                            newLine = buffer.position();
                        }
                    } else {
                        if (newLine == 0 && c >= 32 & c <= 126) {
                            continue;   // Accept other US-ASCII characters ony on the first line.
                        }
                        return false;
                    }
                }
            }
            if (newLine == 0) {
                return false;   // If it was an ASCII file, we would have found at least one EOL character.
            }
            buffer.position(newLine);
            return true;
        }

        /**
         * Returns the next word from an ASCII file. This method is invoked only for parsing ASCII files,
         * in which case the {@link #ascii} string builder is non-null.
         */
        private String nextWord() throws IOException {
            char c;
            do {
                ensureBufferContains(1);
                c = (char) buffer.get();
            } while (Character.isWhitespace(c));
            ascii.setLength(0);
            do {
                ascii.append(c);
                ensureBufferContains(1);
                c = (char) buffer.get();
            } while (!Character.isWhitespace(c));
            return ascii.toString();
        }

        /**
         * Returns the exception to thrown in the file does not seems to be a NADCON format.
         */
        private FactoryException unexpectedFormat() {
            return new FactoryException(Errors.format(Errors.Keys.UnexpectedFileFormat_2, NADCON, file));
        }

        /**
         * Loads latitude or longitude shifts data. This method should be invoked twice:
         *
         * <ol>
         *   <li>On an instance created for the latitude shifts file with a {@code latitude} argument set to null.</li>
         *   <li>On an instance created for the longitude shifts file with a {@code latitude} argument set to the
         *       instance created in the previous step.</li>
         * </ol>
         *
         * The result is stored in the {@link #grid} field.
         *
         * @param fb              A {@code FloatBuffer} view over the full {@link #buffer} range.
         * @param latitudeShifts  The previously loaded latitude shifts, or {@code null} if not yet loaded.
         * @param longitudeShifts The file for the longitude grid, or {@code null} if identical to {@link #file}.
         */
        final void readGrid(final FloatBuffer fb, final Loader latitudeShifts, final Path longitudeShifts)
                throws IOException, FactoryException, NoninvertibleTransformException
        {
            final int dim;
            final double scale;
            if (latitudeShifts == null) {
                dim   = 1;                          // Dimension of latitudes.
                scale = DEGREES_TO_SECONDS * Δy;    // NADCON shifts are positive north.
                grid  = new DatumShiftGridFile.Float<Angle,Angle>(2, NonSI.DEGREE_ANGLE, NonSI.DEGREE_ANGLE,
                        true, x0, y0, Δx, Δy, nx, ny, PARAMETERS, file, longitudeShifts);
                grid.accuracy = SECOND_PRECISION / DEGREES_TO_SECONDS;
            } else {
                if (x0 != latitudeShifts.x0 || Δx != latitudeShifts.Δx || nx != latitudeShifts.nx ||
                    y0 != latitudeShifts.y0 || Δy != latitudeShifts.Δy || ny != latitudeShifts.ny || nz != latitudeShifts.nz)
                {
                    throw new FactoryException(Errors.format(Errors.Keys.MismatchedGridGeometry_2,
                            latitudeShifts.file.getFileName(), file.getFileName()));
                }
                dim   = 0;                          // Dimension of longitudes
                scale = -DEGREES_TO_SECONDS * Δx;   // NADCON shifts are positive west.
                grid  = latitudeShifts.grid;        // Continue writing in existing grid.
            }
            final float[] array = grid.offsets[dim];
            if (ascii != null) {
                for (int i=0; i<array.length; i++) {
                    array[i] = (float) (Double.parseDouble(nextWord()) / scale);
                }
            } else {
                /*
                 * Transfer all data from the FloatBuffer to the float[] array, except one float at the beginning
                 * of every row which must be skipped. That skipped float value is not a translation value and is
                 * expected to be always zero.
                 */
                syncView(fb);
                int forCurrentRow = 0;
                for (int i=0; i<array.length;) {
                    if (forCurrentRow == 0) {
                        if (!fb.hasRemaining()) {
                            fillBuffer(fb);
                        }
                        if (fb.get() != 0) {
                            throw unexpectedFormat();
                        }
                        forCurrentRow = nx;
                    }
                    int remaining = fb.remaining();
                    if (remaining == 0) {
                        fillBuffer(fb);
                        remaining = fb.remaining();
                    }
                    final int n = Math.min(forCurrentRow, remaining);
                    fb.get(array, i, n);
                    forCurrentRow -= n;
                    i += n;
                }
                /*
                 * Convert seconds to degrees for consistency with the unit declared at the beginning of this method,
                 * then divide by cell size for consistency with the 'isCellRatio = true' configuration.
                 */
                for (int i=0; i<array.length; i++) {
                    array[i] /= scale;
                }
            }
        }

        /**
         * Invoked when the given {@code FloatBuffer} buffer is empty. This method requests one {@code float}
         * from the channel, but the channel will usually give us as many data as the buffer can contain.
         */
        private void fillBuffer(final FloatBuffer fb) throws IOException {
            buffer.position(fb.position() * DATA_SIZE).limit(fb.limit() * DATA_SIZE);
            ensureBufferContains(DATA_SIZE);    // Require at least one float, but usually get many.
            syncView(fb);
        }

        /**
         * Sets the position and limit of the given {@code FloatBuffer} to the same position and limit
         * than the underlying {@code ByteBuffer}, converted to units of {@code float} data type.
         */
        private void syncView(final FloatBuffer fb) {
            if ((buffer.position() % DATA_SIZE) != 0) {
                buffer.compact();   // For bytes alignment with FloatBuffer.
            }
            fb.limit(buffer.limit() / DATA_SIZE).position(buffer.position() / DATA_SIZE);
        }
    }
}
