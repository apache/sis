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

import java.util.Collections;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import javax.xml.bind.annotation.XmlTransient;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.system.DataDirectory;
import org.apache.sis.internal.referencing.NilReferencingObject;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform;

import static java.lang.Float.parseFloat;

// Branch-specific imports
import java.io.File;
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.Paths;
import org.apache.sis.internal.jdk8.JDK8;


/**
 * The provider for <cite>"France geocentric interpolation"</cite> (ESPG:9655).
 * This operation requires a grid file provided by the French mapping agency.
 *
 * <p><b>Source:</b> IGN document {@code NTG_88.pdf},
 * <cite>"Grille de paramètres de transformation de coordonnées"</cite>
 * at <a href="http://www.ign.fr">http://www.ign.fr</a>.</p>
 *
 * In principle, this operation method is designed specifically for the French mapping
 * (e.g. EPSG:1053 <cite>"NTF to RGF93 (1)"</cite>) using the following hard-coded parameters:
 * <ul>
 *   <li>Source ellipsoid: Clarke 1880</li>
 *   <li>Target ellipsoid: RGF93</li>
 *   <li>Initial X-axis translation: {@value #TX} (sign reversed)</li>
 *   <li>Initial Y-axis translation: {@value #TY} (sign reversed)</li>
 *   <li>Initial Z-axis translation: {@value #TZ} (sign reversed)</li>
 * </ul>
 *
 * However the Apache SIS implementation is designed in such a way that this operation method
 * could be used for other areas.
 *
 * @author  Simon Reynard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public class FranceGeocentricInterpolation extends GeodeticOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4707304160205218546L;

    /**
     * Geocentric translation parameters to use as a first guess before to use the grid in France.
     * The values of those parameters are specified by the NTG_88 document and apply only for France.
     * If the geocentric interpolation is used for other area, other parameter values will be needed.
     *
     * <p>The values used by SIS are from source (RGF93) to target (NTF). This is the opposite of the
     * direction defined in NTG_88. Consequently the signs need to be the opposite of NTG_88 values.</p>
     */
    public static final double TX = 168, TY = 60, TZ = -320;

    /**
     * Precision of offset values in the grid file. The "GR3DF97A.txt" file uses a precision of 0.001.
     * But we define here one more digit in case a user gives a more accurate grid.
     *
     * Note that value of {@code ulp((float) max(|TX|, |TY|, |TZ|))} is about 3E-5. Consequently the
     * value of {@code PRECISION} should not be lower than 1E-4 (assuming that we want a power of 10).
     */
    static final double PRECISION = 0.0001;

    /**
     * Accuracies of offset values. Accuracies in GR3D files are defined by standard-deviations rounded
     * to integer values. The mapping given in NTG_88 document is:
     *
     *   01 =  5 cm,
     *   02 = 10 cm,
     *   03 = 20 cm,
     *   04 = 50 cm and
     *   99 &gt; 1 m.
     */
    private static final double[] ACCURACY = {0.05, 0.1, 0.2, 0.5, 1};

    /**
     * The keyword expected at the beginning of every lines in the header.
     */
    private static final String HEADER = "GR3D";

    /**
     * Name of the default grid file, as mentioned in the NTG_88 document.
     * We use the 5 first characters ({@code "gr3df"}) as a sentinel value for French grid file.
     *
     * @see #isRecognized(Path)
     */
    private static final String DEFAULT = "gr3df97a.txt";

    /**
     * The operation parameter descriptor for the <cite>Geocentric translation file</cite> parameter value.
     */
    public static final ParameterDescriptor<File> FILE;

    /**
     * The group of all parameters expected by this coordinate operation. The only parameter formally defined by EPSG
     * is {@link #FILE}. All other parameters have been taken from {@link Molodensky} since geocentric interpolations
     * can be though as a Molodensky operations with non-constant (ΔX, ΔY, ΔZ) geocentric translation terms.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        FILE = builder
                .addIdentifier("8727")
                .addName("Geocentric translation file")
                .setRemarks(NTv2.WARNING).create(File.class, Paths.get(DEFAULT));
        PARAMETERS = builder
                .addIdentifier("9655")
                .addName("France geocentric interpolation")
                .createGroup(Molodensky.DIMENSION,       // Not an EPSG parameter.
                             Molodensky.SRC_SEMI_MAJOR,
                             Molodensky.SRC_SEMI_MINOR,
                             Molodensky.TGT_SEMI_MAJOR,
                             Molodensky.TGT_SEMI_MINOR,
                             FILE);
    }

    /**
     * Constructs a provider.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public FranceGeocentricInterpolation() {
        this(2, 2, PARAMETERS, new FranceGeocentricInterpolation[4]);
        redimensioned[0] = this;
        redimensioned[1] = new FranceGeocentricInterpolation(2, 3, PARAMETERS, redimensioned);
        redimensioned[2] = new FranceGeocentricInterpolation(3, 2, PARAMETERS, redimensioned);
        redimensioned[3] = new FranceGeocentricInterpolation(3, 3, PARAMETERS, redimensioned);
    }

    /**
     * Constructs a provider for the given number of dimensions.
     *
     * @param sourceDimensions  number of dimensions in the source CRS of this operation method.
     * @param targetDimensions  number of dimensions in the target CRS of this operation method.
     * @param parameters        description of parameters expected by this operation.
     * @param redimensioned     providers for all combinations between 2D and 3D cases, or {@code null}.
     */
    FranceGeocentricInterpolation(final int sourceDimensions,
                                  final int targetDimensions,
                                  final ParameterDescriptorGroup parameters,
                                  final GeodeticOperation[] redimensioned)
    {
        super(sourceDimensions, targetDimensions, parameters, redimensioned);
    }

    /**
     * Returns {@code true} if the given path seems to be a grid published by the French mapping agency for France.
     * In principle this <cite>"France geocentric interpolation"</cite> is designed specifically for use with the
     * {@code "gr3df97a.txt"} grid, but in fact the Apache SIS implementation should be flexible enough for use
     * with other area.
     *
     * @param file The grid file.
     * @return {@code true} if the given file looks like a fie from the French mapping agency.
     */
    public static boolean isRecognized(final Path file) {
        return file.getFileName().toString().regionMatches(true, 0, DEFAULT, 0, 5);
    }

    /**
     * The inverse of {@code FranceGeocentricInterpolation} is a different operation.
     *
     * @return {@code false}.
     */
    @Override
    public final boolean isInvertible() {
        return false;
    }

    /**
     * Notifies {@code DefaultMathTransformFactory} that map projections require values for the
     * {@code "src_semi_major"}, {@code "src_semi_minor"} , {@code "tgt_semi_major"} and
     * {@code "tgt_semi_minor"} parameters.
     *
     * @return 3, meaning that the operation requires source and target ellipsoids.
     */
    @Override
    public int getEllipsoidsMask() {
        return 3;
    }

    /**
     * Creates the source or the target ellipsoid. This is a temporary ellipsoid
     * used only at {@link InterpolatedGeocentricTransform} time, then discarded.
     *
     * @param values     The parameter group from which to get the axis lengths.
     * @param semiMajor  The descriptor for locating the semi-major axis parameter.
     * @param semiMinor  The descriptor for locating the semi-minor axis parameter.
     * @param candidate  An ellipsoid to return if the axis lengths match the lengths found in the parameters,
     *                   or {@code null} if none. The intend is to use the pre-defined "GRS 1980" ellipsoid if
     *                   we can, because that ellipsoid is defined by inverse flattening factor instead than by
     *                   semi-minor axis length.
     * @return A temporary ellipsoid encapsulating the axis lengths found in the parameters.
     */
    private static Ellipsoid createEllipsoid(final Parameters values,
                                             final ParameterDescriptor<Double> semiMajor,
                                             final ParameterDescriptor<Double> semiMinor,
                                             final Ellipsoid candidate)
    {
        final double semiMajorAxis = values.doubleValue(semiMajor);     // Converted to metres.
        final double semiMinorAxis = values.doubleValue(semiMinor);     // Converted to metres.
        if (candidate != null && Math.abs(candidate.getSemiMajorAxis() - semiMajorAxis) < 1E-6
                              && Math.abs(candidate.getSemiMinorAxis() - semiMinorAxis) < 1E-6)
        {
            return candidate;
        }
        return DefaultEllipsoid.createEllipsoid(Collections.singletonMap(Ellipsoid.NAME_KEY,
                NilReferencingObject.UNNAMED), semiMajorAxis, semiMinorAxis, SI.METRE);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * This method creates the transform from <em>target</em> to <em>source</em>
     * (which is the direction that use the interpolation grid directly without iteration),
     * then inverts the transform.
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
        boolean withHeights = false;
        final Parameters pg = Parameters.castOrWrap(values);
        final Integer dim = pg.getValue(Molodensky.DIMENSION);
        if (dim != null) switch (dim) {
            case 2:  break;
            case 3:  withHeights = true; break;
            default: throw new InvalidParameterValueException(Errors.format(
                            Errors.Keys.IllegalArgumentValue_2, "dim", dim), "dim", dim);
        }
        final Path file = Path.castOrCopy(pg.getMandatoryValue(FILE));
        final DatumShiftGridFile<Angle,Length> grid = getOrLoad(file,
                isRecognized(file) ? new double[] {TX, TY, TZ} : null, PRECISION);
        MathTransform tr = createGeodeticTransformation(factory,
                createEllipsoid(pg, Molodensky.TGT_SEMI_MAJOR,
                                    Molodensky.TGT_SEMI_MINOR, CommonCRS.ETRS89.ellipsoid()),   // GRS 1980 ellipsoid
                createEllipsoid(pg, Molodensky.SRC_SEMI_MAJOR,
                                    Molodensky.SRC_SEMI_MINOR, null),                           // Clarke 1880 (IGN) ellipsoid
                withHeights, grid);
        try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happen.
        }
        return tr;
    }

    /**
     * Creates the actual math transform. The default implementation delegates to the static method defined in
     * {@link InterpolatedGeocentricTransform}, but the {@link MolodenskyInterpolation} subclass will rather
     * delegate to {@link org.apache.sis.referencing.operation.transform.InterpolatedMolodenskyTransform}.
     */
    MathTransform createGeodeticTransformation(final MathTransformFactory factory,
            final Ellipsoid source, final Ellipsoid target, final boolean withHeights,
            final DatumShiftGridFile<Angle,Length> grid) throws FactoryException
    {
        return InterpolatedGeocentricTransform.createGeodeticTransformation(
                factory, source, withHeights, target, withHeights, grid);
    }

    /**
     * Returns the grid of the given name. This method returns the cached instance if it still exists,
     * or load the grid otherwise.
     *
     * @param  file      Name of the datum shift grid file to load.
     * @param  averages  An "average" value for the offset in each dimension, or {@code null} if unknown.
     * @param  scale     The factor by which to multiply each compressed value before to add to the average value.
     */
    @SuppressWarnings("null")
    static DatumShiftGridFile<Angle,Length> getOrLoad(final Path file, final double[] averages, final double scale)
            throws FactoryException
    {
        final Path resolved = DataDirectory.DATUM_CHANGES.resolve(file).toAbsolutePath();
        DatumShiftGridFile<?,?> grid = DatumShiftGridFile.CACHE.peek(resolved);
        if (grid == null) {
            final Cache.Handler<DatumShiftGridFile<?,?>> handler = DatumShiftGridFile.CACHE.lock(resolved);
            try {
                grid = handler.peek();
                if (grid == null) {
                    try {
                        final BufferedReader in = JDK8.newBufferedReader(resolved);
                        try {
                            DatumShiftGridLoader.log(FranceGeocentricInterpolation.class, file);
                            final DatumShiftGridFile.Float<Angle,Length> g = load(in, file);
                            grid = DatumShiftGridCompressed.compress(g, averages, scale);
                        } finally {
                            in.close();
                        }
                    } catch (Exception e) {     // Multi-catch on the JDK7 branch.
                        // NumberFormatException, ArithmeticException, NoSuchElementException, possibly other.
                        throw DatumShiftGridLoader.canNotLoad(HEADER, file, e);
                    }
                    grid = grid.useSharedData();
                }
            } finally {
                handler.putAndUnlock(grid);
            }
        }
        return grid.castTo(Angle.class, Length.class);
    }

    /**
     * Unconditionally loads the grid for the given file without in-memory compression.
     *
     * @param  in Reader of the RGF93 datum shift file.
     * @param  file Path to the file being read, used only for error reporting.
     * @throws IOException if an I/O error occurred.
     * @throws NumberFormatException if a number can not be parsed.
     * @throws NoSuchElementException if a data line is missing a value.
     * @throws FactoryException if an problem is found with the file content.
     * @throws ArithmeticException if the width or the height exceed the integer capacity.
     */
    static DatumShiftGridFile.Float<Angle,Length> load(final BufferedReader in, final Path file)
            throws IOException, FactoryException, NoninvertibleTransformException
    {
        DatumShiftGridFile.Float<Angle,Length> grid = null;
        double x0 = 0;
        double xf = 0;
        double y0 = 0;
        double yf = 0;
        double Δx = 0;
        double Δy = 0;
        int    nx = 0;
        int    ny = 0;
        /*
         * The header should be like below, but the only essential line for this class is the one
         * starting with "GR3D1". We also check that "GR3D2" declares the expected interpolation.
         *
         *     GR3D  002024 024 20370201
         *     GR3D1   -5.5000  10.0000  41.0000  52.0000    .1000    .1000
         *     GR3D2 INTERPOLATION BILINEAIRE
         *     GR3D3 PREC CM 01:5 02:10 03:20 04:50 99>100
         */
        String line;
        while (true) {
            line = in.readLine();
            if (line == null) {
                throw new EOFException(Errors.format(Errors.Keys.UnexpectedEndOfFile_1, file));
            }
            final int length = CharSequences.skipTrailingWhitespaces(line, 0, line.length());
            if (length <= 0) {
                continue;   // Skip empty lines.
            }
            int p = CharSequences.skipLeadingWhitespaces(line, 0, length);
            if (line.charAt(p) == '#') {
                continue;   // Skip comment lines (not officially part of the format).
            }
            if (!line.regionMatches(true, p, HEADER, 0, HEADER.length())) {
                break;      // End of header.
            }
            if ((p += HEADER.length()) < length) {
                final char c = line.charAt(p);
                p = CharSequences.skipLeadingWhitespaces(line, p+1, length);
                switch (c) {
                    case '1': {
                        if (grid != null) {
                            throw new FactoryException(Errors.format(Errors.Keys.DuplicatedElement_1, HEADER));
                        }
                        final double[] gridGeometry = CharSequences.parseDoubles(line.substring(p, length), ' ');
                        if (gridGeometry.length == 6) {
                            x0 = gridGeometry[0];
                            xf = gridGeometry[1];
                            y0 = gridGeometry[2];
                            yf = gridGeometry[3];
                            Δx = gridGeometry[4];
                            Δy = gridGeometry[5];
                            nx = JDK8.toIntExact(Math.round((xf - x0) / Δx + 1));
                            ny = JDK8.toIntExact(Math.round((yf - y0) / Δy + 1));
                            grid = new DatumShiftGridFile.Float<Angle,Length>(3,
                                    NonSI.DEGREE_ANGLE, SI.METRE, false,
                                    x0, y0, Δx, Δy, nx, ny, PARAMETERS, file);
                        }
                        break;
                    }
                    case '2': {
                        final String interp = line.substring(p, length);
                        if (!interp.matches("(?i)INTERPOLATION[^A-Z]+BILINEAIRE")) {
                            final LogRecord record = Errors.getResources((Locale) null).getLogRecord(
                                    Level.WARNING, Errors.Keys.UnsupportedInterpolation_1, interp);
                            record.setLoggerName(Loggers.COORDINATE_OPERATION);
                            Logging.log(FranceGeocentricInterpolation.class, "createMathTransform", record);
                            // We declare 'createMathTransform' method because it is closer to public API.
                        }
                        break;
                    }
                }
            }
        }
        if (grid == null) {
            throw new FactoryException(Errors.format(Errors.Keys.CanNotParseFile_2, HEADER, file));
        }
        /*
         * Loads the data with the sign of all offsets reversed. Data columns are
         *
         *     (unknown), longitude, latitude, tX, tY, tZ, accuracy code, data sheet (ignored)
         *
         * where the longitude and latitude values are in RGF93 system.
         * Example:
         *
         *     00002   -5.500000000   41.000000000  -165.027   -67.100   315.813  99  -0158
         *     00002   -5.500000000   41.100000000  -165.169   -66.948   316.007  99  -0157
         *     00002   -5.500000000   41.200000000  -165.312   -66.796   316.200  99  -0157
         *
         * Translation values in the IGN file are from NTF to RGF93, but Apache SIS implementation needs
         * the opposite direction (from RGF93 to NTF). The reason is that SIS expect the source datum to
         * be the datum in which longitude and latitude values are expressed.
         */
        final float[] tX = grid.offsets[0];
        final float[] tY = grid.offsets[1];
        final float[] tZ = grid.offsets[2];
        do {
            final StringTokenizer t = new StringTokenizer(line.trim());
            t.nextToken();                                                // Ignored
            final double x = Double.parseDouble(t.nextToken());           // Longitude in degrees
            final double y = Double.parseDouble(t.nextToken());           // Latitude in degrees
            final int    i = JDK8.toIntExact(Math.round((x - x0) / Δx));  // Column index
            final int    j = JDK8.toIntExact(Math.round((y - y0) / Δy));  // Row index
            if (i < 0 || i >= nx) {
                throw new FactoryException(Errors.format(Errors.Keys.ValueOutOfRange_4, "x", x, x0, xf));
            }
            if (j < 0 || j >= ny) {
                throw new FactoryException(Errors.format(Errors.Keys.ValueOutOfRange_4, "y", y, y0, yf));
            }
            final int p = j*nx + i;
            if (!Double.isNaN(tX[p]) || !Double.isNaN(tY[p]) || !Double.isNaN(tZ[p])) {
                throw new FactoryException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, x + ", " + y));
            }
            tX[p] = -parseFloat(t.nextToken());  // See javadoc for the reason why we reverse the sign.
            tY[p] = -parseFloat(t.nextToken());
            tZ[p] = -parseFloat(t.nextToken());
            final double accuracy = ACCURACY[Math.min(ACCURACY.length-1,
                    Math.max(0, Integer.parseInt(t.nextToken()) - 1))];
            if (!(accuracy >= grid.accuracy)) {   // Use '!' for replacing the initial NaN.
                grid.accuracy = accuracy;
            }
        } while ((line = in.readLine()) != null);
        return grid;
    }
}
