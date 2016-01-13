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

import org.apache.sis.internal.system.DataDirectory;

// Branch-specific imports
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk7.Files;


/**
 * Assumption methods used by the SIS project in addition of the JUnit ones.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class Assume extends org.junit.Assume {
    /**
     * Do not allow instantiation.
     */
    private Assume() {
    }

    /**
     * Assumes that the {@code SIS_DATA} environment variable is defined, that the directory
     * exists and contains the given file. If this condition fails, then the test is skipped.
     *
     * <p>This is used for tests that require data not distributed with SIS.
     * Examples of data not distributed with SIS are datum shift grids.
     * If desired, those grids need to be downloaded by the user and stored in the directory
     * identified by the {@code SIS_DATA} environment variable.</p>
     *
     * @param  type The directory where to search for the given file.
     * @param  file The file that needs to exist.
     * @return The path to the given file.
     */
    public static Path assumeDataExists(final DataDirectory type, final String file) {
        assumeNotNull(System.getenv(DataDirectory.ENV));
        Path path = type.getDirectory();
        assumeNotNull(path);
        path = path.resolve(file);
        assumeTrue(Files.isReadable(path));
        return path;
    }
}
