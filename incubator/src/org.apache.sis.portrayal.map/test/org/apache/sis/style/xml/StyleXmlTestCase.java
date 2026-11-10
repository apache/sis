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
package org.apache.sis.style.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;


/**
 * Base class of the tests of XML reading and writing of styles.
 * The test files are in the {@code filter}, {@code se1} and {@code sld1} sub-directories.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class StyleXmlTestCase {
    /**
     * Creates a new test case for styles in XML.
     */
    StyleXmlTestCase() {
    }

    /**
     * Opens the test file of the given name.
     *
     * @param  file  path to the test file, relative to this package.
     * @return input stream for the test file.
     */
    static InputStream open(final String file) {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        final InputStream in = StyleXmlTestCase.class.getResourceAsStream(file);
        assertNotNull(in, file);
        return in;
    }

    /**
     * Reads the test file of the given name and verifies that it contains an object of the given type.
     *
     * @param  <T>   compile-time value of {@code type}.
     * @param  file  path to the test file, relative to this package.
     * @param  type  expected type of the object.
     * @return the object read from the test file.
     * @throws Exception if an error occurred while reading the file.
     */
    static <T> T read(final String file, final Class<T> type) throws Exception {
        try (InputStream in = open(file); StyleReader reader = new StyleReader(in)) {
            return reader.read(type);
        }
    }

    /**
     * Reads an object from the given XML document.
     *
     * @param  xml  the XML document.
     * @return the object read from the document.
     * @throws Exception if an error occurred while reading the document.
     */
    static Object parse(final String xml) throws Exception {
        try (StyleReader reader = new StyleReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))) {
            return reader.read();
        }
    }

    /**
     * Writes the given object as a XML document in the given version.
     *
     * @param  object   the object to write.
     * @param  version  the version of the standards to target.
     * @return the XML document.
     * @throws Exception if an error occurred while writing the document.
     */
    static String write(final Object object, final StyleVersion version) throws Exception {
        final var out = new ByteArrayOutputStream();
        try (StyleWriter writer = new StyleWriter(out, version)) {
            writer.write(object);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Returns the value of the given expression, which shall be a literal.
     *
     * @param  expression  the expression.
     * @return the literal value.
     */
    static Object value(final Expression<?,?> expression) {
        return assertInstanceOf(Literal.class, expression).getValue();
    }

    /**
     * Returns the XPath of the given expression, which shall be a value reference.
     *
     * @param  expression  the expression.
     * @return the XPath of the value reference.
     */
    static String xpath(final Expression<?,?> expression) {
        return assertInstanceOf(ValueReference.class, expression).getXPath();
    }
}
