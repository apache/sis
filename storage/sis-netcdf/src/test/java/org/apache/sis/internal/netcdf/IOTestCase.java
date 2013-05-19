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
package org.apache.sis.internal.netcdf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import ucar.nc2.NetcdfFile;

import static org.junit.Assume.*;


/**
 * Placeholder for a GeoAPI 3.1 class which is missing in GeoAPI 3.0.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract strictfp class IOTestCase {
    /**
     * GeoAPI 3.1 constants copied here since this SIS branch depends on GeoAPI 3.0.
     */
    public static final String NCEP = "NCEP-SST.nc", CIP = "CIP.nc";

    /**
     * For subclass constructors only.
     */
    protected IOTestCase() {
    }

    /**
     * Opens the given NetCDF file.
     *
     * @param  file The file name, typically one of the {@link #NCEP} or {@link #CIP} constants.
     * @return The NetCDF file.
     * @throws IOException If an error occurred while opening the file.
     */
    protected NetcdfFile open(final String file) throws IOException {
        final URL url = getClass().getResource(file);
        assumeNotNull(url);
        assumeTrue("file".equals(url.getProtocol()));
        final File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            throw (IOException) new MalformedURLException(e.getLocalizedMessage()).initCause(e);
        }
        return NetcdfFile.open(f.getPath());
    }
}
