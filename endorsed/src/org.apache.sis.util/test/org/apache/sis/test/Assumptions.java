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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Files;
import org.apache.sis.system.DataDirectory;

// Test dependencies
import static org.junit.jupiter.api.Assumptions.*;


/**
 * Assumption methods used by the SIS project in addition of the JUnit ones.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Assumptions {
    /**
     * Do not allow instantiation.
     */
    private Assumptions() {
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
     * @param  type  the directory where to search for the given file.
     * @param  file  the file that needs to exist.
     * @return the path to the given file.
     */
    public static URI assumeDataExists(final DataDirectory type, final String file) {
        assumeTrue(System.getenv(DataDirectory.ENV) != null, "$SIS_DATA environment variable not set.");
        Path path = type.getDirectory();
        assumeTrue(path != null, () -> "$SIS_DATA/" + type + " directory not found.");
        path = path.resolve(file);
        assumeTrue(Files.exists(path), "Specified file or directory not found.");
        assumeTrue(Files.isReadable(path), "Specified directory not readable.");
        return path.toUri();
    }
}
