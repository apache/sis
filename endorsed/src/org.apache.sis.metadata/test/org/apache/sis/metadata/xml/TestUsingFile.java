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
package org.apache.sis.metadata.xml;

import java.net.URL;
import java.io.InputStream;
import org.apache.sis.util.Version;

// Test dependencies
import org.apache.sis.test.xml.TestCase;


/**
 * Base class of tests which contain some XML (un)marshalling of metadata as ISO 19115-3 compliant documents.
 * Tests use the files provided in the {@code "2007/"} or {@code "2016/"} sub-directories, depending on
 * whether ISO 19139:2007 or ISO 19115-3:2016 schema is used.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
public abstract class TestUsingFile extends TestCase {
    /**
     * Identification of the data to use for a test.
     */
    protected enum Format {
        /** A document in the sub-directory of XML files encoded according the ISO 19115-3:2016 schema. */
        XML2016(VERSION_2014, "2016/"),

        /** A document in the sub-directory of XML files encoded according the ISO 19139:2007 schema. */
        XML2007(VERSION_2007, "2007/");

        /** Version of the XML schema used by this format. */
        public final Version schemaVersion;

        /** The directory (relative to the {@code TestUsingFile.class} file) of the XML document. */
        private final String directory;

        /** Creates a new enumeration for documents in the specified sub-directory. */
        private Format(final Version schemaVersion, final String directory) {
            this.schemaVersion = schemaVersion;
            this.directory = directory;
        }

        /**
         * Returns the URL to the specified XML file.
         *
         * @param  filename  the XML file in the directory represented by this enumeration.
         * @return URL to the specified file.
         */
        public final URL getURL(final String filename) {
            // Call to `getResource(…)` is caller sensitive: it must be in the same module.
            return TestUsingFile.class.getResource(directory.concat(filename));
        }

        /**
         * Opens the stream to the specified XML file.
         *
         * @param  filename  the XML file in the directory represented by this enumeration.
         * @return stream opened on the XML document to use for testing purpose.
         */
        public final InputStream openTestFile(final String filename) {
            // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
            return TestUsingFile.class.getResourceAsStream(directory.concat(filename));
        }
    }

    /**
     * For sub-class constructors only.
     */
    protected TestUsingFile() {
    }
}
