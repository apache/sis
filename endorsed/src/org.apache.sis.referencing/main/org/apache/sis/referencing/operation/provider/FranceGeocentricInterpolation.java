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
package org.apache.sis.referencing.operation.provider;

import java.net.URI;
import java.util.Map;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.concurrent.Callable;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import static java.lang.Float.parseFloat;
import jakarta.xml.bind.annotation.XmlTransient;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.operation.gridded.GridFile;
import org.apache.sis.referencing.operation.gridded.LoadedGrid;
import org.apache.sis.referencing.operation.gridded.GridLoader;
import org.apache.sis.referencing.operation.gridded.CompressedGrid;
import org.apache.sis.referencing.operation.transform.InterpolatedGeocentricTransform;
import org.apache.sis.referencing.privy.NilReferencingObject;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.measure.Units;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;


/**
 * The provider for <q>Geocentric translations (geog2D domain) by grid (IGN)</q> (EPSG:1087).
 * This method replaces the deprecated <q>France geocentric interpolation</q> (ESPG:9655).
 * This operation requires a grid file provided by the French mapping agency.
 *
 * <p><b>Source:</b> IGN document {@code NTG_88.pdf},
 * <q>Grille de paramètres de transformation de coordonnées</q>
 * at <a href="http://www.ign.fr">http://www.ign.fr</a>.</p>
 *
 * In principle, this operation method is designed specifically for the French mapping
 * (e.g. EPSG:1053 <q>NTF to RGF93 (1)</q>) using the following hard-coded parameters:
 * <ul>
 *   <li>Source ellipsoid: Clarke 1880</li>
 *   <li>Target ellipsoid: RGF93</li>
 *   <li>Initial X-axis translation: {@value #TX} (sign reversed)</li>
 *   <li>Initial Y-axis translation: {@value #TY} (sign reversed)</li>
 *   <li>Initial Z-axis translation: {@value #TZ} (sign reversed)</li>
 * </ul>
 *
 * However, the Apache SIS implementation is designed in such a way that this operation method
 * could be used for other areas.
 *
 * @author  Simon Reynard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
public final class FranceGeocentricInterpolation extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -298193260915837911L;

    /**
     * Geocentric translation parameters to use as a first guess before to use the grid in France.
     * The values of those parameters are specified by the NTG_88 document and apply only for France.
     * If the geocentric interpolation is used for other area, other parameter values will be needed.
     *
     * <p>The values used by SIS are from source (RGF93) to target (NTF). This is the opposite of the
     * direction defined in NTG_88. Consequently, the signs need to be the opposite of NTG_88 values.</p>
     */
    public static final double TX = 168, TY = 60, TZ = -320;

    /**
     * Precision of offset values in the grid file. The "GR3DF97A.txt" file uses a precision of 0.001.
     * But we define here one more digit in case a user gives a more accurate grid.
     *
     * Note that value of {@code ulp((float) max(|TX|, |TY|, |TZ|))} is about 3E-5. Consequently, the
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
     * @see #isRecognized(URI)
     */
    private static final String DEFAULT = "gr3df97a.txt";

    /**
     * The operation parameter descriptor for the <cite>Geocentric translation file</cite> parameter value.
     *
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> Geocentric translation file </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>Default value: {@code gr3df97a.txt}</li>
     * </ul>
     */
    public static final ParameterDescriptor<URI> FILE;

    /**
     * The operation parameter descriptor for the <cite>EPSG code for Interpolation CRS</cite> parameter value.
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> EPSG code for Interpolation CRS </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Integer> INTERPOLATION_CRS;

    /**
     * The operation parameter descriptor for the <cite>EPSG code for "standard" CT</cite> parameter value.
     * <!-- Generated by ParameterNameTableGenerator -->
     * <table class="sis">
     *   <caption>Parameter names</caption>
     *   <tr><td> EPSG:    </td><td> EPSG code for standard transformation T0 </td></tr>
     *   <tr><td> EPSG:    </td><td> EPSG code for "standard" CT </td></tr>
     * </table>
     * <b>Notes:</b>
     * <ul>
     *   <li>No default value</li>
     * </ul>
     */
    public static final ParameterDescriptor<Integer> STANDARD_CT;

    /**
     * The group of all parameters expected by this coordinate operation. The only parameter formally defined by EPSG
     * is {@link #FILE}. All other parameters have been taken from {@link Molodensky} since geocentric interpolations
     * can be though as a Molodensky operations with non-constant (ΔX, ΔY, ΔZ) geocentric translation terms.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        FILE = builder
                .addIdentifier("8727")
                .addName("Geocentric translation file")
                .create(URI.class, URI.create(DEFAULT));
        INTERPOLATION_CRS = builder
                .addIdentifier("1048")
                .addName("EPSG code for Interpolation CRS")
                .create(Integer.class, null);
        STANDARD_CT = builder
                .addIdentifier("1062")
                .addName("EPSG code for standard transformation T0")
                .addName("EPSG code for \"standard\" CT")
                .create(Integer.class, null);
        PARAMETERS = builder
                .addIdentifier("1087").addName("Geocentric translations (geog2D domain) by grid (IGN)")
                .addName("Geocentric translation by Grid Interpolation (IGN)")
                .setDeprecated(true).addIdentifier("9655").addName("France geocentric interpolation")
                .setDeprecated(false)
                .createGroup(Molodensky.DIMENSION,              // Not an EPSG parameter.
                             Molodensky.SRC_SEMI_MAJOR,
                             Molodensky.SRC_SEMI_MINOR,
                             Molodensky.TGT_SEMI_MAJOR,
                             Molodensky.TGT_SEMI_MINOR,
                             FILE, INTERPOLATION_CRS, STANDARD_CT);
    }

    /**
     * Creates a new provider.
     */
    public FranceGeocentricInterpolation() {
        super(Transformation.class, PARAMETERS,
              EllipsoidalCS.class, true,
              EllipsoidalCS.class, true,
              (byte) 2);
    }

    /**
     * Returns {@code true} if the given path seems to be a grid published by the French mapping agency for France.
     * In principle this <q>France geocentric interpolation</q> is designed specifically for use with the
     * {@code "gr3df97a.txt"} grid, but in fact the Apache SIS implementation should be flexible enough
     * for use with other area.
     *
     * @param  file  the grid file.
     * @return {@code true} if the given file looks like a fie from the French mapping agency.
     */
    public static boolean isRecognized(final GridFile file) {
        final String filename = file.parameter.getPath();
        final int s = filename.lastIndexOf('/') + 1;
        return filename.regionMatches(true, s, DEFAULT, 0, 5);
    }

    /**
     * Creates the source or the target ellipsoid. This is a temporary ellipsoid
     * used only at {@link InterpolatedGeocentricTransform} time, then discarded.
     *
     * @param  values     the parameter group from which to get the axis lengths.
     * @param  semiMajor  the descriptor for locating the semi-major axis parameter.
     * @param  semiMinor  the descriptor for locating the semi-minor axis parameter.
     * @param  candidate  an ellipsoid to return if the axis lengths match the lengths found in the parameters,
     *                    or {@code null} if none. The intent is to use the predefined "GRS 1980" ellipsoid if
     *                    we can, because that ellipsoid is defined by inverse flattening factor instead of by
     *                    semi-minor axis length.
     * @return a temporary ellipsoid encapsulating the axis lengths found in the parameters.
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
        return DefaultEllipsoid.createEllipsoid(Map.of(Ellipsoid.NAME_KEY, NilReferencingObject.UNNAMED),
                                                semiMajorAxis, semiMinorAxis, Units.METRE);
    }

    /**
     * Creates a transform from the specified group of parameter values.
     * This method creates the transform from <em>target</em> to <em>source</em>
     * (which is the direction that use the interpolation grid directly without iteration),
     * then inverts the transform.
     *
     * @param  context  the parameter values together with its context.
     * @return the created math transform.
     * @throws ParameterNotFoundException if a required parameter was not found.
     * @throws FactoryException if an error occurred while loading the grid.
     */
    @Override
    public MathTransform createMathTransform(final Context context) throws FactoryException {
        final Parameters pg = Parameters.castOrWrap(context.getCompletedParameters());
        final int dim = pg.getValue(Molodensky.DIMENSION);
        final GridFile file = new GridFile(pg, FILE);
        final LoadedGrid<Angle,Length> grid;
        try {
            grid = getOrLoad(file, isRecognized(file) ? new double[] {TX, TY, TZ} : null, PRECISION);
        } catch (FactoryException e) {
            throw e;
        } catch (Exception e) {
            // NumberFormatException, ArithmeticException, NoSuchElementException, and more.
            throw file.canNotLoad(FranceGeocentricInterpolation.class, HEADER, e);
        }
        MathTransform tr = InterpolatedGeocentricTransform.createGeodeticTransformation(
                context.getFactory(),
                createEllipsoid(pg, Molodensky.TGT_SEMI_MAJOR,
                                    Molodensky.TGT_SEMI_MINOR,
                                    CommonCRS.ETRS89.ellipsoid()),      // GRS 1980 ellipsoid
                context.getTargetDimensions().orElse(dim) >= 3,
                createEllipsoid(pg, Molodensky.SRC_SEMI_MAJOR,
                                    Molodensky.SRC_SEMI_MINOR,
                                    null),                              // Clarke 1880 (IGN) ellipsoid
                context.getSourceDimensions().orElse(dim) >= 3,
                grid);
        try {
            tr = tr.inverse();
        } catch (NoninvertibleTransformException e) {
            throw new FactoryException(e);                  // Should never happen.
        }
        return tr;
    }

    /**
     * Returns the grid of the given name. This method returns the cached instance if it still exists,
     * or load the grid otherwise.
     *
     * @param  file      an absolute or relative reference to the datum shift grid file to load.
     * @param  averages  an "average" value for the offset in each dimension, or {@code null} if unknown.
     * @param  scale     the factor by which to multiply each compressed value before to add to the average value.
     * @throws Exception if an error occurred while loading the grid.
     *         Caller should handle the exception with {@code canNotLoad(…)}.
     *
     * @see GridLoader#canNotLoad(Class, String, URI, Exception)
     */
    static LoadedGrid<Angle,Length> getOrLoad(final GridFile file, final double[] averages, final double scale)
            throws Exception
    {
        return LoadedGrid.getOrLoad(file, null, new Loader(file, averages, scale))
                                 .castTo(Angle.class, Length.class);
    }

    /**
     * Temporary object created for loading gridded data if not present in the cache.
     * The data are provided in a text file, which is read with {@link BufferedReader}.
     */
    static final class Loader implements Callable<LoadedGrid<?,?>> {
        /** The file to load. */
        private final GridFile file;

        /** An "average" value for the offset in each dimension, or {@code null} if unknown. */
        private final double[] averages;

        /** The factor by which to multiply each compressed value before to add to the average value. */
        private final double scale;

        /** Creates a new loader for the given file. */
        Loader(final GridFile file, final double[] averages, final double scale) {
            this.file     = file;
            this.averages = averages;
            this.scale    = scale;
        }

        /**
         * Invoked when the gridded data are not in the cache.
         * This method load grid data from the file specified at construction time.
         *
         * @return the loaded grid data.
         * @throws Exception if an error occurred while loading the grid data.
         *         May be {@link IOException}, {@link NumberFormatException}, {@link ArithmeticException},
         *         {@link NoSuchElementException}, {@link NoninvertibleTransformException}, <i>etc</i>.
         */
        @Override
        public LoadedGrid<?,?> call() throws Exception {
            final LoadedGrid<?,?> grid;
            try (BufferedReader in = file.newBufferedReader()) {
                file.startLoading(FranceGeocentricInterpolation.class);
                final LoadedGrid.Float<Angle,Length> g = load(in, file);
                grid = CompressedGrid.compress(g, averages, scale);
            }
            return grid.useSharedData();
        }

        /**
         * Unconditionally loads the grid for the given file without in-memory compression.
         *
         * @param  in    reader of the RGF93 datum shift file.
         * @param  file  path to the file being read, used for parameter declaration and error reporting.
         * @throws IOException if an I/O error occurred.
         * @throws NumberFormatException if a number cannot be parsed.
         * @throws NoSuchElementException if a data line is missing a value.
         * @throws FactoryException if an problem is found with the file content.
         * @throws ArithmeticException if the width or the height exceed the integer capacity.
         */
        static LoadedGrid.Float<Angle,Length> load(final BufferedReader in, final GridFile file)
                throws IOException, FactoryException, NoninvertibleTransformException
        {
            LoadedGrid.Float<Angle,Length> grid = null;
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
                                nx = Math.toIntExact(Math.round((xf - x0) / Δx + 1));
                                ny = Math.toIntExact(Math.round((yf - y0) / Δy + 1));
                                grid = new LoadedGrid.Float<>(3,
                                        Units.DEGREE, Units.METRE, false,
                                        x0, y0, Δx, Δy, nx, ny, PARAMETERS, file);
                                grid.accuracy = Double.NaN;
                                for (final float[] data : grid.offsets) {
                                    Arrays.fill(data, Float.NaN);
                                }
                            }
                            break;
                        }
                        case '2': {
                            final String interp = line.substring(p, length);
                            if (!interp.matches("(?i)INTERPOLATION[^A-Z]+BILINEAIRE")) {
                                final LogRecord record = Errors.forLocale(null).createLogRecord(
                                        Level.WARNING, Errors.Keys.UnsupportedInterpolation_1, interp);

                                // We declare `createMathTransform(…)` method because it is closer to public API.
                                Logging.completeAndLog(LOGGER, FranceGeocentricInterpolation.class,
                                                       "createMathTransform", record);
                            }
                            break;
                        }
                    }
                }
            }
            if (grid == null) {
                throw new FactoryException(Resources.format(Resources.Keys.FileNotFound_2, HEADER, file));
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
                final String[] tokens = line.split("\\s+");
                final double x = Double.parseDouble(tokens[1]);                     // Longitude in degrees
                final double y = Double.parseDouble(tokens[2]);                     // Latitude in degrees
                final int    i = Math.toIntExact(Math.round((x - x0) / Δx));        // Column index
                final int    j = Math.toIntExact(Math.round((y - y0) / Δy));        // Row index
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
                tX[p] = -parseFloat(tokens[3]);     // See javadoc for the reason why we reverse the sign.
                tY[p] = -parseFloat(tokens[4]);
                tZ[p] = -parseFloat(tokens[5]);
                int accuracyCode = Math.max(0, Integer.parseInt(tokens[6]) - 1);
                double accuracy = ACCURACY[Math.min(ACCURACY.length - 1, accuracyCode)];
                if (!(accuracy >= grid.accuracy)) {     // Use `!` for replacing the initial NaN.
                    grid.accuracy = accuracy;
                }
            } while ((line = in.readLine()) != null);
            return grid;
        }
    }
}
