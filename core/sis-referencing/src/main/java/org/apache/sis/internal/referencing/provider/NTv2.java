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

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;

// Branch-dependent imports
import java.io.File;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.internal.jdk7.Files;
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.StandardCharsets;
import org.apache.sis.internal.jdk8.JDK8;


/**
 * The provider for <cite>"National Transformation version 2"</cite> (EPSG:9615).
 * This transform requires data that are not bundled by default with Apache SIS.
 *
 * @author  Simon Reynard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class NTv2 extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4027618007780159180L;

    /**
     * Warns the user that the parameter type will change from {@link File}
     * to {@link java.nio.file.Path} when Apache SIS will upgrade to Java 7.
     */
    static final InternationalString WARNING = new SimpleInternationalString(
            "The parameter type will change from ‘java.io.File’ to ‘java.nio.file.Path’ " +
            "when Apache SIS will upgrade to Java 7.");

    /**
     * The operation parameter descriptor for the <cite>"Latitude and longitude difference file"</cite> parameter value.
     * The file extension is typically {@code ".gsb"}. There is no default value.
     */
    private static final ParameterDescriptor<File> FILE;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        FILE = builder
                .addIdentifier("8656")
                .addName("Latitude and longitude difference file")
                .setRemarks(WARNING).create(File.class, null);
        PARAMETERS = builder
                .addIdentifier("9615")
                .addName("NTv2")
                .createGroup(FILE);
    }

    /**
     * Creates a new provider.
     */
    public NTv2() {
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
        final Parameters pg = Parameters.castOrWrap(values);
        return InterpolatedTransform.createGeodeticTransformation(factory,
                getOrLoad(Path.castOrCopy(pg.getMandatoryValue(FILE))));
    }

    /**
     * Returns the grid of the given name. This method returns the cached instance if it still exists,
     * or load the grid otherwise.
     *
     * @param file Name of the datum shift grid file to load.
     */
    @SuppressWarnings("null")
    static DatumShiftGridFile<Angle,Angle> getOrLoad(final Path file) throws FactoryException {
        final Path resolved = DataDirectory.DATUM_CHANGES.resolve(file).toAbsolutePath();
        DatumShiftGridFile<?,?> grid = DatumShiftGridFile.CACHE.peek(resolved);
        if (grid == null) {
            final Cache.Handler<DatumShiftGridFile<?,?>> handler = DatumShiftGridFile.CACHE.lock(resolved);
            try {
                grid = handler.peek();
                if (grid == null) {
                    try {
                        final ReadableByteChannel in = Files.newByteChannel(resolved);
                        try {
                            DatumShiftGridLoader.log(NTv2.class, file);
                            final Loader loader = new Loader(in, file);
                            grid = loader.readGrid();
                            loader.reportWarnings();
                        } finally {
                            in.close();
                        }
                    } catch (Exception e) {     // Multi-catch on the JDK7 branch.
                        throw DatumShiftGridLoader.canNotLoad("NTv2", file, e);
                    }
                    grid = grid.useSharedData();
                }
            } finally {
                handler.putAndUnlock(grid);
            }
        }
        return grid.castTo(Angle.class, Angle.class);
    }




    /**
     * Loaders of NTv2 data. Instances of this class exist only at loading time.
     *
     * @author  Simon Reynard (Geomatys)
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    private static final class Loader extends DatumShiftGridLoader {
        /**
         * Size of a record. This value applies to both the header records and the data records.
         * In the case of header records, this is the size of the key plus the size of the value.
         */
        private static final int RECORD_LENGTH = 16;

        /**
         * Maximum number of characters of a key in a header record.
         */
        private static final int KEY_LENGTH = 8;

        /**
         * Type of data allowed in header records.
         */
        private static final int STRING_TYPE = 0, INTEGER_TYPE = 1, DOUBLE_TYPE = 2;

        /**
         * Some known keywords that may appear in NTv2 header records.
         */
        private static final Map<String,Integer> TYPES;
        static {
            final Map<String,Integer> types = new HashMap<String,Integer>(32);
            final Integer string  = STRING_TYPE;    // Autoboxing
            final Integer integer = INTEGER_TYPE;
            final Integer real    = DOUBLE_TYPE;
            types.put("NUM_OREC", integer);         // Number of records in the header - usually 11
            types.put("NUM_SREC", integer);         // Number of records in the header of sub-grids - usually 11
            types.put("NUM_FILE", integer);         // Number of sub-grids
            types.put("GS_TYPE",  string);          // Units: "SECONDS", "MINUTES" or "DEGREES"
            types.put("VERSION",  string);          // Grid version
            types.put("SYSTEM_F", string);          // Source CRS
            types.put("SYSTEM_T", string);          // Target CRS
            types.put("MAJOR_F",  real);            // Semi-major axis of source ellipsoid (in metres)
            types.put("MINOR_F",  real);            // Semi-minor axis of source ellipsoid (in metres)
            types.put("MAJOR_T",  real);            // Semi-major axis of target ellipsoid (in metres)
            types.put("MINOR_T",  real);            // Semi-minor axis of target ellipsoid (in metres)
            types.put("SUB_NAME", string);          // Sub-grid identifier
            types.put("PARENT",   string);          // Parent grid
            types.put("CREATED",  string);          // Creation time
            types.put("UPDATED",  string);          // Update time
            types.put("S_LAT",    real);            // Southmost φ value
            types.put("N_LAT",    real);            // Northmost φ value
            types.put("E_LONG",   real);            // Eastmost λ value - west is positive, east is negative
            types.put("W_LONG",   real);            // Westmost λ value - west is positive, east is negative
            types.put("LAT_INC",  real);            // Increment on φ axis
            types.put("LONG_INC", real);            // Increment on λ axis - positive toward west
            types.put("GS_COUNT", integer);         // Number of sub-grid records following
            TYPES = types;
        }

        /**
         * The header content. Keys are strings like {@code "VERSION"}, {@code "SYSTEM_F"},
         * <var>etc.</var>. Values are {@link String}, {@link Integer} or {@link Double}.
         * If some keys are unrecognized, they will be put in this map with the {@code null} value
         * and the {@link #hasUnrecognized} field will be set to {@code true}.
         */
        private final Map<String,Object> header;

        /**
         * {@code true} if the {@code header} map contains at least one key associated to a null value.
         */
        private boolean hasUnrecognized;

        /**
         * Number of grids remaining in the file. This value is set in the constructor,
         * then decremented at every call to {@link #readGrid()}.
         */
        private int remainingGrids;

        /**
         * Creates a new reader for the given channel.
         * This constructor parses the header immediately, but does not read any grid.
         *
         * @param  channel Where to read data from.
         * @param  file Path to the longitude and latitude difference file. Used only for error reporting.
         * @throws FactoryException if a data record can not be parsed.
         */
        Loader(final ReadableByteChannel channel, final Path file) throws IOException, FactoryException {
            super(channel, ByteBuffer.allocate(4096), file);
            this.header = new LinkedHashMap<String,Object>();
            ensureBufferContains(RECORD_LENGTH);
            if (isLittleEndian(buffer.getInt(KEY_LENGTH))) {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }
            /*
             * Read the overview header. It is normally made of the first 11 records documented in TYPES map:
             * NUM_OREC, NUM_SREC, NUM_FILE, GS_TYPE, VERSION, SYSTEM_F, SYSTEM_T, MAJOR_F, MINOR_F, MAJOR_T,
             * MINOR_T.
             */
            readHeader(11, "NUM_OREC");
            remainingGrids = (Integer) get("NUM_FILE");
            if (remainingGrids < 1) {
                throw new FactoryException(Errors.format(Errors.Keys.UnexpectedValueInElement_2, "NUM_FILE", remainingGrids));
            }
        }

        /**
         * Reads the next grid, starting at the current position. A NTv2 file can have many grids.
         * This can be used for grids having different resolutions depending on the geographic area.
         * The first grid can cover a large area with a coarse resolution, and next grids cover smaller
         * areas overlapping the first grid but with finer resolution.
         *
         * Current SIS implementation does not yet handle the above-cited hierarchy of grids.
         * For now we just take the first one.
         *
         * <p>NTv2 grids contain also information about shifts accuracy. This is not yet handled by SIS.</p>
         */
        final DatumShiftGridFile<Angle,Angle> readGrid() throws IOException, FactoryException, NoninvertibleTransformException {
            if (--remainingGrids < 0) {
                throw new FactoryException(Errors.format(Errors.Keys.CanNotRead_1, file));
            }
            final Object[] overviewKeys = header.keySet().toArray();
            readHeader((Integer) get("NUM_SREC"), "NUM_SREC");
            /*
             * Extract the geographic bounding box and cell size. While different units are allowed,
             * in practice we usually have seconds of angle. This units has the advantage of allowing
             * all floating-point values to be integers.
             *
             * Note that the longitude values in NTv2 files are positive WEST.
             */
            final Unit<Angle> unit;
            final double precision;
            final String name = (String) get("GS_TYPE");
            if (name.equalsIgnoreCase("SECONDS")) {         // Most common value
                unit = NonSI.SECOND_ANGLE;
                precision = SECOND_PRECISION;                       // Used only as a hint; will not hurt if wrong.
            } else if (name.equalsIgnoreCase("MINUTES")) {
                unit = NonSI.MINUTE_ANGLE;
                precision = SECOND_PRECISION / 60;                  // Used only as a hint; will not hurt if wrong.
            } else if (name.equalsIgnoreCase("DEGREES")) {
                unit = NonSI.DEGREE_ANGLE;
                precision = SECOND_PRECISION / DEGREES_TO_SECONDS;  // Used only as a hint; will not hurt if wrong.
            } else {
                throw new FactoryException(Errors.format(Errors.Keys.UnexpectedValueInElement_2, "GS_TYPE", name));
            }
            final double  ymin     = (Double)  get("S_LAT");
            final double  ymax     = (Double)  get("N_LAT");
            final double  xmin     = (Double)  get("E_LONG");    // Sign reversed compared to usual convention.
            final double  xmax     = (Double)  get("W_LONG");    // Idem.
            final double  dy       = (Double)  get("LAT_INC");
            final double  dx       = (Double)  get("LONG_INC");  // Positive toward west.
            final Integer declared = (Integer) header.get("GS_COUNT");
            final int     width    = JDK8.toIntExact(Math.round((xmax - xmin) / dx + 1));
            final int     height   = JDK8.toIntExact(Math.round((ymax - ymin) / dy + 1));
            final int     count    = JDK8.multiplyExact(width, height);
            if (declared != null && count != declared) {
                throw new FactoryException(Errors.format(Errors.Keys.UnexpectedValueInElement_2, "GS_COUNT", declared));
            }
            /*
             * Construct the grid with the sign of longitude values reversed, in order to have longitude values
             * increasing toward East. We set isCellValueRatio = true (by the arguments given to the constructor)
             * because this is required by our InterpolatedTransform implementation. This setting implies that we
             * divide translation values by dx or dy at reading time. Note that this free us from reversing the
             * sign of longitude translations; instead, this reversal will be handled by grid.coordinateToGrid
             * MathTransform and its inverse.
             */
            final DatumShiftGridFile.Float<Angle,Angle> grid = new DatumShiftGridFile.Float<Angle,Angle>(2,
                    unit, unit, true, -xmin, ymin, -dx, dy, width, height, PARAMETERS, file);
            @SuppressWarnings("MismatchedReadAndWriteOfArray") final float[] tx = grid.offsets[0];
            @SuppressWarnings("MismatchedReadAndWriteOfArray") final float[] ty = grid.offsets[1];
            for (int i=0; i<count; i++) {
                ensureBufferContains(4 * (Float.SIZE / Byte.SIZE));
                ty[i] = (float) (buffer.getFloat() / dy);   // Division by dx and dy because isCellValueRatio = true.
                tx[i] = (float) (buffer.getFloat() / dx);
                final double accuracy = Math.min(buffer.getFloat() / dy, buffer.getFloat() / dx);
                if (accuracy > 0 && !(accuracy >= grid.accuracy)) {   // Use '!' for replacing the initial NaN.
                    grid.accuracy = accuracy;
                }
            }
            header.keySet().retainAll(Arrays.asList(overviewKeys));   // Keep only overview records.
            return DatumShiftGridCompressed.compress(grid, null, precision / Math.max(dx, dy));
        }

        /**
         * Returns {@code true} if the given value seems to be stored in little endian order.
         */
        private static boolean isLittleEndian(final int n) {
            return JDK8.compareUnsigned(n, Integer.reverseBytes(n)) > 0;
        }

        /**
         * Reads a string at the given position in the buffer.
         */
        private String readString(final int position, int length) {
            final byte[] array = buffer.array();
            while (length > position && array[position + length - 1] <= ' ') length--;
            return new String(array, position, length, StandardCharsets.US_ASCII).trim();
        }

        /**
         * Reads all records found in the header, starting from the current buffer position.
         * It may be the overview header (in which case we expect {@code NUM_OREC} records)
         * or a sub-grid header (in which case we expect {@code NUM_SREC} records).
         *
         * @param numRecords Default number of expected records (usually 11).
         * @param numkey Key of the record giving the number of records: {@code "NUM_OREC"} or {@code "NUM_SREC"}.
         */
        private void readHeader(int numRecords, final String numkey) throws IOException, FactoryException {
            int position = buffer.position();
            for (int i=0; i < numRecords; i++) {
                ensureBufferContains(RECORD_LENGTH);
                final String key = readString(position, KEY_LENGTH).toUpperCase(Locale.US);
                position += KEY_LENGTH;
                final Integer type = TYPES.get(key);
                final Comparable<?> value;
                if (type == null) {
                    value = null;
                    hasUnrecognized = true;
                } else switch (type) {
                    case STRING_TYPE: {
                        value = readString(position, RECORD_LENGTH - KEY_LENGTH);
                        break;
                    }
                    case INTEGER_TYPE: {
                        final int n = buffer.getInt(position);
                        if (key.equals(numkey)) {
                            numRecords = n;
                        }
                        value = n;
                        break;
                    }
                    case DOUBLE_TYPE: {
                        value = buffer.getDouble(position);
                        break;
                    }
                    default: throw new AssertionError(type);
                }
                final Object old = header.put(key, value);
                if (old != null && !old.equals(value)) {
                    throw new FactoryException(Errors.format(Errors.Keys.DuplicatedElement_1, key));
                }
                buffer.position(position += RECORD_LENGTH - KEY_LENGTH);
            }
        }

        /**
         * Returns the value for the given key, or thrown an exception if the value is not found.
         */
        private Object get(final String key) throws FactoryException {
            final Object value = header.get(key);
            if (value != null) {
                return value;
            }
            throw new FactoryException(Errors.format(Errors.Keys.PropertyNotFound_2, file, key));
        }

        /**
         * If we had any warnings during the loading process, report them now.
         */
        void reportWarnings() {
            if (hasUnrecognized) {
                final StringBuilder keywords = new StringBuilder();
                for (final Map.Entry<String,Object> entry : header.entrySet()) {
                    if (entry.getValue() == null) {
                        if (keywords.length() != 0) {
                            keywords.append(", ");
                        }
                        keywords.append(entry.getKey());
                    }
                }
                final LogRecord record = Messages.getResources(null).getLogRecord(Level.WARNING,
                        Messages.Keys.UnknownKeywordInRecord_2, file, keywords.toString());
                record.setLoggerName(Loggers.COORDINATE_OPERATION);
                Logging.log(NTv2.class, "createMathTransform", record);
            }
        }
    }
}
