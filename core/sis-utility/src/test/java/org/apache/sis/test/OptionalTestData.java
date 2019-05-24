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
package org.apache.sis.test;

import java.io.LineNumberReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.sis.internal.system.DataDirectory;

import static org.junit.Assume.assumeNotNull;


/**
 * All optional test data used by Apache SIS. Those data are not present on the source code repository.
 * They must be downloaded and installed by the developer in the {@code $SIS_DATA/Tests} directory in
 * order to enable the tests requiring those data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public enum OptionalTestData {
    /**
     * Geodesic distances, rhumb line length and azimuths on WGS84 ellipsoid computed from a set of points.
     *
     * <dl>
     *   <dt>File:</dt>
     *   <dd>{@code GeodTest.dat}</dd>
     *   <dt>Size:</dt>
     *   <dd>86558916 bytes (83 Mb)</dd>
     *   <dt>MD5 sum:</dt>
     *   <dd>{@code 3461c4dc2500a8bad9394cd530b13dbe}</dd>
     *   <dt>Source:</dt>
     *   <dd><a href="http://doi.org/10.5281/zenodo.32156">Karney, C. F. F. (2010). Test set for geodesics [Data set]. Zenodo.</a></dd>
     * </dl>
     *
     * Each line in the test file gives the following numbers (space delimited):
     *
     * <ol>
     *   <li>φ₁ — latitude at point 1 (degrees)</li>
     *   <li>λ₁ — longitude at point 1 (degrees)</li>
     *   <li>α₁ — azimuth at point 1 (degrees, clockwise from north)</li>
     *   <li>φ₂ — latitude at point 2 (degrees)</li>
     *   <li>λ₂ — longitude at point 2 (degrees)</li>
     *   <li>α₂ — azimuth at point 2 (degrees, clockwise from north)</li>
     *   <li>s₁₂ — geodesic distance from point 1 to point 2 (metres)</li>
     *   <li>σ₁₂ — arc distance on the auxiliary sphere (degrees)</li>
     *   <li>m₁₂ — reduced length of the geodesic (meters)</li>
     *   <li>S₁₂ — the area between the geodesic and the equator (m²)</li>
     * </ol>
     */
    GEODESIC("GeodTest.dat");

    /**
     * The filename in {@code $SIS_DATA/Tests} directory.
     */
    private final String filename;

    /**
     * Creates a new enumeration for the given file.
     */
    private OptionalTestData(final String filename) {
        this.filename = filename;
    }

    /**
     * Returns the path to the test file if {@code $SIS_DATA} is defined an the file exists, or {@code null} otherwise.
     *
     * @return path to the test file, or {@code null} if none.
     */
    private Path path() {
        Path path = DataDirectory.TESTS.getDirectory();
        if (path != null) {
            path = path.resolve(filename);
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        return null;
    }

    /**
     * If the test file represented by this enumeration exists, opens it as an input stream.
     * If the file does not exist, throws {@link org.junit.AssumptionViolatedException} as
     * by {@link org.junit.Assume} methods.
     *
     * @return an input stream for the test file represented by this enumeration.
     * @throws IOException if an error occurred while opening the test file.
     */
    public InputStream open() throws IOException {
        final Path path = path();
        assumeNotNull("File “$SIS_DATA/Tests/" + filename + "” has not been found.", path);
        return Files.newInputStream(path);
    }

    /**
     * If the test file represented by this enumeration exists, opens it as a UTF-8 character reader.
     * If the file does not exist, throws {@link org.junit.AssumptionViolatedException} as by
     * {@link org.junit.Assume} methods.
     *
     * @return an UTF-8 character reader for the test file represented by this enumeration.
     * @throws IOException if an error occurred while opening the test file.
     */
    public LineNumberReader reader() throws IOException {
        final InputStream in = open();
        return new LineNumberReader(new InputStreamReader(in, "UTF-8"));
    }
}
